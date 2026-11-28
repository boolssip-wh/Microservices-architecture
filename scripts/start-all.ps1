param(
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
foreach ($environmentName in @(
    "VISION_OCR_PROVIDER",
    "VISION_API_BASE_URL",
    "VISION_MODEL",
    "VISION_API_KEY"
)) {
    if (-not [Environment]::GetEnvironmentVariable($environmentName, "Process")) {
        $storedValue = [Environment]::GetEnvironmentVariable($environmentName, "User")
        if ($storedValue) {
            [Environment]::SetEnvironmentVariable($environmentName, $storedValue, "Process")
        }
    }
}
$toolsRoot = if ($env:SMARTGRADE_TOOLS) { $env:SMARTGRADE_TOOLS } else { "D:\SmartGradeTools" }
$java = Join-Path $toolsRoot "jdk17\bin\java.exe"
$skyWalkingAgent = if ($env:SKYWALKING_AGENT_JAR) {
    $env:SKYWALKING_AGENT_JAR
} else {
    Join-Path $toolsRoot "skywalking-agent\skywalking-agent.jar"
}
$skyWalkingCollector = if ($env:SKYWALKING_BACKEND_SERVICE) {
    $env:SKYWALKING_BACKEND_SERVICE
} else {
    "127.0.0.1:11800"
}
$runtime = Join-Path $projectRoot ".runtime"
$logs = Join-Path $projectRoot "logs"
$jdkTmp = Join-Path $runtime "jdk-tmp"
$sentinelLogs = Join-Path $runtime "sentinel-logs"

if (-not (Test-Path -LiteralPath $java)) {
    throw "JDK 17 not found at $java. Set SMARTGRADE_TOOLS to the tools directory."
}
New-Item -ItemType Directory -Force -Path $runtime, $logs, $jdkTmp, $sentinelLogs | Out-Null

$startupLock = $null
try {
    $startupLock = [IO.File]::Open((Join-Path $runtime "startup.lock"), 'OpenOrCreate', 'ReadWrite', 'None')
} catch {
    throw "Another start is already in progress. Wait for that window to finish before running start.cmd again."
}
$started = @()
$pidFile = Join-Path $runtime "service-pids.json"
Push-Location $projectRoot
try {
    Write-Host "Stopping previous project processes before rebuilding..."
    & (Join-Path $PSScriptRoot "stop-all.ps1")

    $ports = @(8080,8081,8082,8083,8084,8085,8086,8087,5173,8858)
    $occupied = @(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -in $ports })
    if ($occupied.Count) {
        $details = ($occupied | ForEach-Object { "$($_.LocalPort) (PID $($_.OwningProcess))" }) -join ', '
        throw "Required ports are used by other applications: $details"
    }

    $buildLog = Join-Path $logs "build.log"
    Write-Host "Building project; log: $buildLog"
    $previousErrorAction = $ErrorActionPreference
    try {
        # Windows PowerShell turns native stderr into ErrorRecord objects;
        # use Maven's exit code to decide success, keeping all diagnostics.
        $ErrorActionPreference = 'Continue'
        & (Join-Path $PSScriptRoot "mvn17.cmd") -DskipTests clean package 2>&1 | ForEach-Object { "$_" } | Tee-Object -FilePath $buildLog
        $buildExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorAction
    }
    if ($buildExit -ne 0) {
        throw "Maven build failed (exit $buildExit). See $buildLog"
    }
    try {
        & (Join-Path $PSScriptRoot "publish-nacos-config.ps1")
    } catch {
        Write-Warning "Nacos shared config was not published: $($_.Exception.Message)"
    }

    & (Join-Path $PSScriptRoot "start-sentinel-dashboard.ps1")

    $services = @(
        @{ Name = "user"; AgentName = "smartgrade-user-service"; Jar = "smartgrade-user-service\target\smartgrade-user-service-1.0.0.jar"; Port = 8081 },
        @{ Name = "rubric"; AgentName = "smartgrade-rubric-service"; Jar = "smartgrade-rubric-service\target\smartgrade-rubric-service-1.0.0.jar"; Port = 8082 },
        @{ Name = "answer"; AgentName = "smartgrade-answer-service"; Jar = "smartgrade-answer-service\target\smartgrade-answer-service-1.0.0.jar"; Port = 8083 },
        @{ Name = "grading"; AgentName = "smartgrade-grading-service"; Jar = "smartgrade-grading-service\target\smartgrade-grading-service-1.0.0.jar"; Port = 8084 },
        @{ Name = "review"; AgentName = "smartgrade-review-service"; Jar = "smartgrade-review-service\target\smartgrade-review-service-1.0.0.jar"; Port = 8085 },
        @{ Name = "report"; AgentName = "smartgrade-report-service"; Jar = "smartgrade-report-service\target\smartgrade-report-service-1.0.0.jar"; Port = 8086 },
        @{ Name = "notification"; AgentName = "smartgrade-notification-service"; Jar = "smartgrade-notification-service\target\smartgrade-notification-service-1.0.0.jar"; Port = 8087 },
        @{ Name = "gateway"; AgentName = "smartgrade-gateway"; Jar = "smartgrade-gateway\target\smartgrade-gateway-1.0.0.jar"; Port = 8080 }
    )

    foreach ($service in $services) {
        $jar = Join-Path $projectRoot $service.Jar
        if (-not (Test-Path -LiteralPath $jar)) {
            throw "Missing application jar: $jar"
        }
        $stdout = Join-Path $logs ($service.Name + ".out.log")
        $stderr = Join-Path $logs ($service.Name + ".err.log")
        $applicationLog = Join-Path $logs ($service.Name + ".log")
        $javaArguments = @()
        # 检测到 SkyWalking Agent 时，为每个 Java 服务自动附加探针；
        # service_name 让控制台按服务名展示，backend_service 指定 OAP 上报地址。
        if (Test-Path -LiteralPath $skyWalkingAgent) {
            $javaArguments += @(
                "-javaagent:`"$skyWalkingAgent`"",
                "-Dskywalking.agent.service_name=$($service.AgentName)",
                "-Dskywalking.collector.backend_service=$skyWalkingCollector"
            )
        }
        $javaArguments += @(
            "-Djdk.net.unixdomain.tmpdir=`"$jdkTmp`"",
            "-Dcsp.sentinel.log.dir=`"$sentinelLogs`"",
            "-Dcsp.sentinel.log.use.pid=true",
            "-DEAGLEEYE.LOG.PATH=`"$sentinelLogs`"",
            "-Dfile.encoding=UTF-8",
            "-jar",
            "`"$jar`"",
            "--logging.file.name=`"$applicationLog`""
        )
        $process = Start-Process -FilePath $java `
            -ArgumentList $javaArguments `
            -WorkingDirectory $projectRoot -RedirectStandardOutput $stdout -RedirectStandardError $stderr `
            -WindowStyle Hidden -PassThru
        $started += [PSCustomObject]@{
            name = $service.Name
            pid = $process.Id
            port = $service.Port
            processName = "java"
            startTime = $process.StartTime.ToUniversalTime().ToString("o")
            startTimeUtcTicks = $process.StartTime.ToUniversalTime().Ticks
        }
        $started | ConvertTo-Json | Set-Content -LiteralPath $pidFile -Encoding UTF8
    }

    $webRoot = Join-Path $projectRoot "smartgrade-web"
    if (-not (Test-Path -LiteralPath (Join-Path $webRoot "node_modules"))) {
        Push-Location $webRoot
        try {
            & npm.cmd install
            if ($LASTEXITCODE -ne 0) { throw "npm install failed." }
        } finally {
            Pop-Location
        }
    }
    $node = (Get-Command node.exe -ErrorAction Stop).Source
    $vite = Join-Path $webRoot "node_modules\vite\bin\vite.js"
    $webProcess = Start-Process -FilePath $node -ArgumentList @("`"$vite`"", "--host", "127.0.0.1", "--port", "5173", "--strictPort") -WorkingDirectory $webRoot `
        -RedirectStandardOutput (Join-Path $logs "web.out.log") `
        -RedirectStandardError (Join-Path $logs "web.err.log") -WindowStyle Hidden -PassThru
    $started += [PSCustomObject]@{
        name = "web"
        pid = $webProcess.Id
        port = 5173
        processName = "node"
        startTime = $webProcess.StartTime.ToUniversalTime().ToString("o")
        startTimeUtcTicks = $webProcess.StartTime.ToUniversalTime().Ticks
    }
    $started | ConvertTo-Json | Set-Content -LiteralPath $pidFile -Encoding UTF8

    $deadline = (Get-Date).AddSeconds(420)
    do {
        $allUp = $true
        foreach ($service in $services) {
            try {
                $health = Invoke-RestMethod -Uri "http://127.0.0.1:$($service.Port)/actuator/health" -TimeoutSec 2
                if ($health.status -ne "UP") {
                    $allUp = $false
                }
            } catch {
                $allUp = $false
            }
        }
        try {
            $webResponse = Invoke-WebRequest -Uri "http://127.0.0.1:5173/" -UseBasicParsing -TimeoutSec 2
            if ($webResponse.StatusCode -ne 200) { $allUp = $false }
        } catch {
            $allUp = $false
        }
        if (-not $allUp) {
            Start-Sleep -Seconds 2
        }
    } while (-not $allUp -and (Get-Date) -lt $deadline)

    if ($allUp) {
        Write-Host "All Tongbiao grading services are UP."
    } else {
        throw "Some services did not become healthy within 420 seconds. Check logs in $logs."
    }
    Write-Host "Web console: http://127.0.0.1:5173"
    Write-Host "Gateway:     http://127.0.0.1:8080"
    Write-Host "Nacos:       http://127.0.0.1:8088"
    Write-Host "Sentinel:    http://127.0.0.1:8858"
    Write-Host "Logs:        $logs"
    Write-Host "Use scripts\stop-all.ps1 to stop all processes."
    if ($allUp -and -not $NoBrowser) {
        Start-Process "http://127.0.0.1:5173"
    }
} catch {
    $startupError = $_
    Write-Warning "Startup failed; stopping project processes."
    try { & (Join-Path $PSScriptRoot "stop-all.ps1") } catch { Write-Warning "Cleanup failed: $_" }
    throw $startupError
} finally {
    Pop-Location
    if ($startupLock) { $startupLock.Dispose() }
}
