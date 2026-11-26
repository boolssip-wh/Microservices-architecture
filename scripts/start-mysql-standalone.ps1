param(
    [string]$MySqlHome = "D:\mysql-8.0.41",
    [string]$MySqlDataDir = "D:\SmartGradeTools\mysql-it",
    [int]$Port = 3307
)

$ErrorActionPreference = "Stop"
$mysqld = Join-Path $MySqlHome "bin\mysqld.exe"
if (-not (Test-Path -LiteralPath $mysqld)) { throw "mysqld.exe not found: $mysqld" }
if (-not (Test-Path -LiteralPath (Join-Path $MySqlDataDir "mysql"))) {
    New-Item -ItemType Directory -Force -Path $MySqlDataDir | Out-Null
    & $mysqld --no-defaults --initialize-insecure --basedir=$MySqlHome --datadir=$MySqlDataDir
    if ($LASTEXITCODE -ne 0) { throw "MySQL initialization failed." }
}

$listening = Test-NetConnection -ComputerName 127.0.0.1 -Port $Port -WarningAction SilentlyContinue
if ($listening.TcpTestSucceeded) {
    Write-Host "Isolated MySQL is already listening on $Port."
    exit 0
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$argLine = "--no-defaults --basedir=`"$MySqlHome`" --datadir=`"$MySqlDataDir`" " +
    "--port=$Port --bind-address=127.0.0.1 --console"
Start-Process -FilePath $mysqld -ArgumentList $argLine -WorkingDirectory $MySqlDataDir `
    -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $projectRoot ".runtime\mysql-demo.out.log") `
    -RedirectStandardError (Join-Path $projectRoot ".runtime\mysql-demo.err.log")

$deadline = (Get-Date).AddSeconds(60)
do {
    $listening = Test-NetConnection -ComputerName 127.0.0.1 -Port $Port -WarningAction SilentlyContinue
    if (-not $listening.TcpTestSucceeded) { Start-Sleep -Seconds 2 }
} while (-not $listening.TcpTestSucceeded -and (Get-Date) -lt $deadline)

if (-not $listening.TcpTestSucceeded) {
    throw "Isolated MySQL did not become ready on $Port."
}
Write-Host "Isolated MySQL ready: 127.0.0.1:$Port"
