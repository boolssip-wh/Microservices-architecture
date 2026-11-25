param(
    [string]$SeataHome = "D:\SmartGradeTools\seata-2.1.0",
    [string]$JavaHome = "D:\SmartGradeTools\jdk17"
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runner = Join-Path $SeataHome "bin\seata-server.bat"
$java = Join-Path $JavaHome "bin\java.exe"
if (-not (Test-Path -LiteralPath $runner) -or -not (Test-Path -LiteralPath $java)) {
    throw "Seata or JDK was not found. Expected $runner and $java."
}
$existing = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object LocalPort -eq 8091
if ($existing) {
    Write-Host "Seata is already listening on port 8091."
    exit 0
}

$env:JAVA_HOME = $JavaHome
$env:JAVACMD = $java
$env:SEATA_IP = "127.0.0.1"
$env:SEATA_PORT = "8091"
$out = Join-Path $projectRoot ".runtime\seata.out.log"
$err = Join-Path $projectRoot ".runtime\seata.err.log"
Start-Process -FilePath $runner -WorkingDirectory $SeataHome -WindowStyle Hidden `
    -RedirectStandardOutput $out -RedirectStandardError $err

$deadline = (Get-Date).AddSeconds(120)
do {
    $ready = Test-NetConnection -ComputerName 127.0.0.1 -Port 8091 -WarningAction SilentlyContinue
    if (-not $ready.TcpTestSucceeded) {
        Start-Sleep -Seconds 2
    }
} while (-not $ready.TcpTestSucceeded -and (Get-Date) -lt $deadline)
if (-not $ready.TcpTestSucceeded) {
    throw "Seata did not start. Check .runtime\seata.out.log and .runtime\seata.err.log."
}
Write-Host "Seata ready: 127.0.0.1:8091 (console http://127.0.0.1:7091)"
