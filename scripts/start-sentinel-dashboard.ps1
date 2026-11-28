param(
    [int]$Port = 8858,
    [string]$Username = "sentinel",
    [string]$Password = "sentinel",
    [switch]$PassThru
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$toolsRoot = if ($env:SMARTGRADE_TOOLS) { $env:SMARTGRADE_TOOLS } else { "D:\SmartGradeTools" }
$java = Join-Path $toolsRoot "jdk17\bin\java.exe"
$portableJar = Join-Path $toolsRoot "sentinel\sentinel-dashboard.jar"
$jar = if ($env:SENTINEL_DASHBOARD_JAR) {
    [IO.Path]::GetFullPath($env:SENTINEL_DASHBOARD_JAR)
} elseif (Test-Path -LiteralPath $portableJar) {
    $portableJar
} else {
    Join-Path $projectRoot "..\..\sentinel-dashboard.jar"
}
$logRoot = Join-Path $projectRoot ".runtime\sentinel-dashboard"
$pidFile = Join-Path $projectRoot ".runtime\sentinel-dashboard-pid.json"
$jdkTmp = Join-Path $logRoot "jdk-tmp"
$sentinelLogs = Join-Path $logRoot "sentinel-logs"

# C2 保护控制台：Sentinel Dashboard 监听 8858，登录后查看机器列表、簇点链路和规则。
if (-not (Test-Path -LiteralPath $java)) {
    throw "JDK 17 not found at $java"
}
if (-not (Test-Path -LiteralPath $jar)) {
    throw "Sentinel dashboard not found at $jar. Download it separately and set SENTINEL_DASHBOARD_JAR to its absolute path."
}

if (Test-Path -LiteralPath $pidFile) {
    $record = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
    $running = Get-Process -Id $record.pid -ErrorAction SilentlyContinue
    $sameStart = $running -and $running.ProcessName -eq "java" -and
        [Math]::Abs($running.StartTime.ToUniversalTime().Ticks - [long]$record.startTimeUtcTicks) -lt 5000000
    if ($sameStart) {
        try {
            $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/" -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 400) {
                Write-Host "Sentinel dashboard is already running (PID $($record.pid))."
                if ($PassThru) { Write-Output $record }
                exit 0
            }
        } catch {
            # The tracked process exists but the dashboard is not ready; fail instead of starting a duplicate.
        }
        throw "Tracked Sentinel dashboard process $($record.pid) is not responding on port $Port."
    }
    Remove-Item -LiteralPath $pidFile -Force
}

$occupied = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
if ($occupied) {
    throw "Port $Port is already in use by untracked PID $($occupied[0].OwningProcess). Stop it before starting Sentinel."
}

New-Item -ItemType Directory -Force -Path $logRoot, $jdkTmp, $sentinelLogs | Out-Null
$process = Start-Process -FilePath $java `
    -ArgumentList @(
        "-Djdk.net.unixdomain.tmpdir=`"$jdkTmp`"",
        "-Dcsp.sentinel.log.dir=`"$sentinelLogs`"",
        "-Dcsp.sentinel.log.use.pid=true",
        "-DEAGLEEYE.LOG.PATH=`"$sentinelLogs`"",
        "-Dserver.port=$Port",
        "-Dcsp.sentinel.dashboard.server=127.0.0.1:$Port",
        "-Dsentinel.dashboard.auth.username=$Username",
        "-Dsentinel.dashboard.auth.password=$Password",
        "-jar",
        "`"$jar`""
    ) `
    -WorkingDirectory $projectRoot `
    -RedirectStandardOutput (Join-Path $logRoot "dashboard.out.log") `
    -RedirectStandardError (Join-Path $logRoot "dashboard.err.log") `
    -WindowStyle Hidden `
    -PassThru

$record = [PSCustomObject]@{
    name = "sentinel-dashboard"
    pid = $process.Id
    port = $Port
    processName = "java"
    startTime = $process.StartTime.ToUniversalTime().ToString("o")
    startTimeUtcTicks = $process.StartTime.ToUniversalTime().Ticks
}
$record | ConvertTo-Json | Set-Content -LiteralPath $pidFile -Encoding UTF8

$deadline = (Get-Date).AddSeconds(45)
# C2 现场验收：45 秒内 HTTP 控制台可访问，才能证明 Sentinel Dashboard 已就绪。
$ready = $false
do {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/" -UseBasicParsing -TimeoutSec 2
        $ready = $response.StatusCode -ge 200 -and $response.StatusCode -lt 400
    } catch {
        Start-Sleep -Milliseconds 500
    }
} while (-not $ready -and (Get-Date) -lt $deadline)

if (-not $ready) {
    throw "Sentinel dashboard did not become ready on port $Port within 45 seconds."
}

Write-Host "Sentinel dashboard PID $($process.Id)"
Write-Host "URL: http://127.0.0.1:$Port"
Write-Host "Username: $Username"
Write-Host "Password: $Password"
if ($PassThru) { Write-Output $record }
