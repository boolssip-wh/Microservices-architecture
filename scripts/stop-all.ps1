param(
    [switch]$IncludeNacos,
    [string]$NacosHome = (Join-Path $env:USERPROFILE "ai-infra\nacos\standalone\nacos-3.3.0")
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$pidFiles = @(
    (Join-Path $projectRoot ".runtime\service-pids.json"),
    (Join-Path $projectRoot ".runtime\sentinel-dashboard-pid.json")
)
$existingPidFiles = @($pidFiles | Where-Object { Test-Path -LiteralPath $_ })

$items = @()
foreach ($pidFile in $existingPidFiles) {
    try {
        $records = Get-Content -LiteralPath $pidFile -Raw | ConvertFrom-Json
        foreach ($record in $records) {
            $items += $record
        }
    } catch {
        Write-Warning "Cannot read $pidFile; checking application paths instead."
    }
}

# PID files can be lost or overwritten by an interrupted/repeated start. Only
# discover this checkout's application jars/Vite, never all Java/Node processes.
$rootPattern = [regex]::Escape($projectRoot.Replace('/', '\'))
$jarPattern = $rootPattern + '\\smartgrade-(gateway|user-service|rubric-service|answer-service|grading-service|review-service|report-service|notification-service)\\target(?:-[^\\\s"'']+)?\\smartgrade-[^\\\s"'']+\.jar(?:["\s]|$)'
$vitePath = Join-Path $projectRoot "smartgrade-web\node_modules\vite\bin\vite.js"
$toolsRoot = if ($env:SMARTGRADE_TOOLS) { $env:SMARTGRADE_TOOLS } else { "D:\SmartGradeTools" }
$sentinelPaths = @(
    (Join-Path $projectRoot "..\..\sentinel-dashboard.jar"),
    [IO.Path]::GetFullPath((Join-Path $projectRoot "..\..\sentinel-dashboard.jar")),
    [IO.Path]::GetFullPath((Join-Path $toolsRoot "sentinel\sentinel-dashboard.jar"))
)
if ($env:SENTINEL_DASHBOARD_JAR) {
    $sentinelPaths += [IO.Path]::GetFullPath($env:SENTINEL_DASHBOARD_JAR)
}
$nacosPath = [IO.Path]::GetFullPath($NacosHome).TrimEnd('\') + '\'
try {
    $candidates = Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='node.exe'"
} catch {
    Write-Warning "CIM process discovery is unavailable; falling back to project ports and PID records."
    $candidates = @()
}
foreach ($candidate in $candidates) {
    # Nacos startup.cmd quotes only the home segment: "C:\...\nacos"\target\...
    $command = ([string]$candidate.CommandLine).Replace('/', '\').Replace('"', '')
    $kind = $null
    if ($candidate.Name -eq 'java.exe' -and $command -match '(?:^|\s)-jar\s' -and $command -match $jarPattern) {
        $kind = 'project-service'
    } elseif ($candidate.Name -eq 'node.exe' -and $command.IndexOf($vitePath, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
        $kind = 'web'
    } elseif ($candidate.Name -eq 'java.exe' -and $command -match '(?:^|\s)-jar\s') {
        foreach ($sentinelPath in $sentinelPaths) {
            if ($command.IndexOf($sentinelPath, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
                $kind = 'sentinel-dashboard'
            }
        }
    }
    if ($IncludeNacos -and $candidate.Name -eq 'java.exe' -and
        $command.IndexOf($nacosPath, [StringComparison]::OrdinalIgnoreCase) -ge 0 -and
        $command -match 'nacos-server\.jar') {
        $kind = 'nacos'
    }
    if ($kind) {
        $items = @($items | Where-Object { $_.pid -ne $candidate.ProcessId })
        $items += [PSCustomObject]@{
            name = $kind
            pid = $candidate.ProcessId
            processName = [IO.Path]::GetFileNameWithoutExtension($candidate.Name)
            startTimeUtcTicks = $candidate.CreationDate.ToUniversalTime().Ticks
        }
    }
}

# Some restricted terminals deny CIM access. The fixed project ports let the
# same checkout stop safely without terminating unrelated Java or Node apps.
$portMap = @{
    8080 = @{ name = "gateway"; processName = "java" }
    8081 = @{ name = "user"; processName = "java" }
    8082 = @{ name = "rubric"; processName = "java" }
    8083 = @{ name = "answer"; processName = "java" }
    8084 = @{ name = "grading"; processName = "java" }
    8085 = @{ name = "review"; processName = "java" }
    8086 = @{ name = "report"; processName = "java" }
    8087 = @{ name = "notification"; processName = "java" }
    5173 = @{ name = "web"; processName = "node" }
    8858 = @{ name = "sentinel-dashboard"; processName = "java" }
}
$listeners = & netstat.exe -ano -p tcp 2>$null
foreach ($line in @($listeners)) {
    if ($line -notmatch "^\s*TCP\s+\S+:(\d+)\s+\S+\s+LISTENING\s+(\d+)\s*$") { continue }
    $port = [int]$Matches[1]
    $processId = [int]$Matches[2]
    $descriptor = $portMap[$port]
    if (-not $descriptor) { continue }
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if (-not $process -or $process.ProcessName -ne $descriptor.processName) { continue }
    $items = @($items | Where-Object { $_.pid -ne $processId })
    $items += [PSCustomObject]@{
        name = $descriptor.name
        pid = $processId
        processName = $descriptor.processName
        startTimeUtcTicks = $process.StartTime.ToUniversalTime().Ticks
    }
}
$items = @($items | Sort-Object pid -Unique)
if (-not $items.Count) {
    Write-Host "No Tongbiao grading processes are running."
}
foreach ($item in @($items)) {
    $process = Get-Process -Id $item.pid -ErrorAction SilentlyContinue
    $sameName = $process -and $process.ProcessName -eq $item.processName
    $sameStart = $false
    if ($sameName -and $item.startTimeUtcTicks) {
        $differenceTicks = [Math]::Abs($process.StartTime.ToUniversalTime().Ticks - [long]$item.startTimeUtcTicks)
        $sameStart = $differenceTicks -lt 5000000
    } elseif ($sameName -and $item.startTime) {
        $expected = [DateTime]::Parse($item.startTime).ToUniversalTime()
        $sameStart = [Math]::Abs(($process.StartTime.ToUniversalTime() - $expected).TotalSeconds) -lt 5
    }
    if ($sameName -and $sameStart) {
        Stop-Process -InputObject $process -Force
        if (-not $process.WaitForExit(10000)) {
            throw "Timed out stopping $($item.name) (PID $($item.pid))."
        }
        Write-Host "Stopped $($item.name) (PID $($item.pid))."
    } else {
        Write-Warning "Skipped stale PID record for $($item.name): $($item.pid)"
    }
}
foreach ($pidFile in $existingPidFiles) {
    Remove-Item -LiteralPath $pidFile -Force
}
