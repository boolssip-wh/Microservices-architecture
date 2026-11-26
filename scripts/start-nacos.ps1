param(
    [string]$NacosHome = (Join-Path $env:USERPROFILE "ai-infra\nacos\standalone\nacos-3.3.0"),
    [string]$JavaHome = "D:\SmartGradeTools\jdk17",
    [int]$ServerPort = 8848
)

$ErrorActionPreference = "Stop"
$startup = Join-Path $NacosHome "bin\startup.cmd"
$java = Join-Path $JavaHome "bin\java.exe"
if (-not (Test-Path -LiteralPath $startup)) { throw "Nacos startup.cmd not found: $startup" }
if (-not (Test-Path -LiteralPath $java)) { throw "JDK not found: $java" }

$listening = Test-NetConnection -ComputerName 127.0.0.1 -Port $ServerPort `
    -WarningAction SilentlyContinue
if ($listening.TcpTestSucceeded) {
    Write-Host "Nacos is already listening on $ServerPort."
    exit 0
}

$env:JAVA_HOME = $JavaHome
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Start-Process -FilePath "cmd.exe" `
    -ArgumentList @("/c", "`"$startup`" -m standalone") `
    -WorkingDirectory $NacosHome -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $projectRoot ".runtime\nacos-start.out.log") `
    -RedirectStandardError (Join-Path $projectRoot ".runtime\nacos-start.err.log")

$deadline = (Get-Date).AddSeconds(120)
do {
    $ready = Test-NetConnection -ComputerName 127.0.0.1 -Port $ServerPort `
        -WarningAction SilentlyContinue
    if (-not $ready.TcpTestSucceeded) { Start-Sleep -Seconds 2 }
} while (-not $ready.TcpTestSucceeded -and (Get-Date) -lt $deadline)

if (-not $ready.TcpTestSucceeded) {
    throw "Nacos did not become ready on $ServerPort."
}
Write-Host "Nacos server: http://127.0.0.1:8848"
Write-Host "Nacos console: http://127.0.0.1:8088"
