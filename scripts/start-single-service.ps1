param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("gateway","user","rubric","answer","grading","review","report","notification")]
    [string]$ServiceName
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$java = "D:\SmartGradeTools\jdk17\bin\java.exe"
$services = @{
    gateway      = @{ Name = "smartgrade-gateway"; Port = 8080 }
    user         = @{ Name = "smartgrade-user-service"; Port = 8081 }
    rubric       = @{ Name = "smartgrade-rubric-service"; Port = 8082 }
    answer       = @{ Name = "smartgrade-answer-service"; Port = 8083 }
    grading      = @{ Name = "smartgrade-grading-service"; Port = 8084 }
    review       = @{ Name = "smartgrade-review-service"; Port = 8085 }
    report       = @{ Name = "smartgrade-report-service"; Port = 8086 }
    notification = @{ Name = "smartgrade-notification-service"; Port = 8087 }
}
$service = $services[$ServiceName]
$jar = Join-Path $projectRoot "$($service.Name)\target\$($service.Name)-1.0.0.jar"
if (-not (Test-Path -LiteralPath $jar)) {
    throw "Missing jar: $jar. Run .\scripts\mvn17.cmd clean package first."
}
$occupied = Get-NetTCPConnection -State Listen -LocalPort $service.Port -ErrorAction SilentlyContinue
if ($occupied) { throw "Port $($service.Port) is already in use." }

$args = @(
    "-Dfile.encoding=UTF-8",
    "-jar", "`"$jar`"",
    "--logging.file.name=`"$projectRoot\logs\$ServiceName.log`""
)
$p = Start-Process -FilePath $java -ArgumentList $args -WorkingDirectory $projectRoot `
    -RedirectStandardOutput (Join-Path $projectRoot "logs\$ServiceName.out.log") `
    -RedirectStandardError (Join-Path $projectRoot "logs\$ServiceName.err.log") `
    -WindowStyle Hidden -PassThru

$deadline = (Get-Date).AddSeconds(180)
do {
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:$($service.Port)/actuator/health" -TimeoutSec 2
        if ($health.status -eq "UP") { break }
    } catch { Start-Sleep -Seconds 2 }
} while ((Get-Date) -lt $deadline)

if ($health.status -ne "UP") {
    throw "$($service.Name) did not become healthy."
}
Write-Host "$($service.Name) started on port $($service.Port), PID $($p.Id)."
