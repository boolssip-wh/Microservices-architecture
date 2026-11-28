param(
    [switch]$NoBrowser,
    [string]$MySqlHome = "D:\mysql-8.0.41",
    [string]$MySqlDataDir = "D:\SmartGradeTools\mysql-it",
    [int]$MySqlPort = 3307
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot ".runtime") | Out-Null
$mysqld = Join-Path $MySqlHome "bin\mysqld.exe"
$mysql = Join-Path $MySqlHome "bin\mysql.exe"
$mysqladmin = Join-Path $MySqlHome "bin\mysqladmin.exe"
foreach ($required in @($mysqld, $mysql, $mysqladmin)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "MySQL executable was not found: $required"
    }
}
try {
    $null = Invoke-WebRequest -Uri "http://127.0.0.1:8848/nacos/" -UseBasicParsing -TimeoutSec 3
} catch {
    throw "Nacos is not available at 127.0.0.1:8848. Start Nacos first."
}

# 备选的一键演示：初始化 3307 隔离 MySQL，让 D1 回滚测试不污染默认 H2/开发库。
if (-not (Test-Path -LiteralPath (Join-Path $MySqlDataDir "mysql"))) {
    New-Item -ItemType Directory -Force -Path $MySqlDataDir | Out-Null
    & $mysqld --no-defaults --initialize-insecure --basedir=$MySqlHome --datadir=$MySqlDataDir
    if ($LASTEXITCODE -ne 0) {
        throw "Isolated MySQL initialization failed."
    }
}

$mysqlListening = Test-NetConnection -ComputerName 127.0.0.1 -Port $MySqlPort -WarningAction SilentlyContinue
if (-not $mysqlListening.TcpTestSucceeded) {
    $argLine = "--no-defaults --basedir=`"$MySqlHome`" --datadir=`"$MySqlDataDir`" " +
        "--port=$MySqlPort --bind-address=127.0.0.1 --console"
    Start-Process -FilePath $mysqld -ArgumentList $argLine -WorkingDirectory $MySqlDataDir `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $projectRoot ".runtime\mysql-demo.out.log") `
        -RedirectStandardError (Join-Path $projectRoot ".runtime\mysql-demo.err.log")
    $deadline = (Get-Date).AddSeconds(60)
    do {
        $mysqlListening = Test-NetConnection -ComputerName 127.0.0.1 -Port $MySqlPort `
            -WarningAction SilentlyContinue
        if (-not $mysqlListening.TcpTestSucceeded) {
            Start-Sleep -Seconds 2
        }
    } while (-not $mysqlListening.TcpTestSucceeded -and (Get-Date) -lt $deadline)
}
if (-not $mysqlListening.TcpTestSucceeded) {
    throw "Isolated MySQL did not become ready. Check .runtime\mysql-demo.*.log."
}

$schemaReady = & $mysql --protocol=TCP --host=127.0.0.1 --port=$MySqlPort --user=root `
    --batch --skip-column-names -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='smartgrade_answer';"
if ([int]($schemaReady | Select-Object -First 1) -eq 0) {
    $schema = Get-Content -Raw -LiteralPath (Join-Path $projectRoot "deploy\mysql\init.sql") -Encoding UTF8
    $schema | & $mysql --protocol=TCP --host=127.0.0.1 --port=$MySqlPort --user=root
    if ($LASTEXITCODE -ne 0) {
        throw "Could not initialize the isolated MySQL schemas."
    }
} else {
    Write-Host "Isolated MySQL schemas already exist; skipping schema initialization."
}

& (Join-Path $PSScriptRoot "start-governance.ps1")
$toolsRoot = if ($env:SMARTGRADE_TOOLS) { $env:SMARTGRADE_TOOLS } else { "D:\SmartGradeTools" }
$env:SMARTGRADE_TOOLS = $toolsRoot
# D1/D2 环境：answer/review 开启 Seata；所有 Java 服务由 start-all 自动附加 SkyWalking Agent。
$env:SEATA_ENABLED = "true"
$env:SEATA_SERVER_ADDR = "127.0.0.1:8091"
$env:SKYWALKING_AGENT_JAR = Join-Path $toolsRoot "skywalking-agent\skywalking-agent.jar"
$env:SKYWALKING_BACKEND_SERVICE = "127.0.0.1:11800"
# A5/D1 数据隔离：answer 和 review 各自连接独立数据库，Seata 通过 undo_log 保证跨库回滚。
$env:ANSWER_DB_URL = "jdbc:mysql://127.0.0.1:$MySqlPort/smartgrade_answer?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:REVIEW_DB_URL = "jdbc:mysql://127.0.0.1:$MySqlPort/smartgrade_review?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:ANSWER_DB_DRIVER = "com.mysql.cj.jdbc.Driver"
$env:REVIEW_DB_DRIVER = "com.mysql.cj.jdbc.Driver"
$env:DB_USERNAME = "smartgrade"
$env:DB_PASSWORD = "smartgrade123"
$env:SPRING_FLYWAY_BASELINE_ON_MIGRATE = "true"
$env:SPRING_FLYWAY_BASELINE_VERSION = "0"

$startArguments = @()
if ($NoBrowser) {
    $startArguments += "-NoBrowser"
}
& powershell.exe -NoProfile -ExecutionPolicy Bypass `
    -File (Join-Path $PSScriptRoot "start-all.ps1") @startArguments
