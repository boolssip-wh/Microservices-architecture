param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("gateway","user","rubric","answer","grading","review","report","notification")]
    [string]$ServiceName
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$java = "D:\SmartGradeTools\jdk17\bin\java.exe"
$agent = "D:\SmartGradeTools\skywalking-agent\skywalking-agent.jar"
# A1 单服务演示映射：ServiceName 对应项目模块、Nacos 服务名和固定端口。
# Gateway 是统一入口；其余 7 项才是架构图中的业务服务。
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

if ($ServiceName -eq "answer") {
    # D1 演示：answer-service 单独启动时连接 3307 隔离 MySQL，并开启 Seata 参与全局事务。
    $env:SEATA_ENABLED = "true"
    $env:SEATA_SERVER_ADDR = "127.0.0.1:8091"
    $env:ANSWER_DB_URL = "jdbc:mysql://127.0.0.1:3307/smartgrade_answer?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
    $env:ANSWER_DB_DRIVER = "com.mysql.cj.jdbc.Driver"
    $env:DB_USERNAME = "smartgrade"
    $env:DB_PASSWORD = "smartgrade123"
}
if ($ServiceName -eq "review") {
    # D1 演示：review-service 作为另一个事务参与方，同样连接 3307 的隔离数据库。
    $env:SEATA_ENABLED = "true"
    $env:SEATA_SERVER_ADDR = "127.0.0.1:8091"
    $env:REVIEW_DB_URL = "jdbc:mysql://127.0.0.1:3307/smartgrade_review?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
    $env:REVIEW_DB_DRIVER = "com.mysql.cj.jdbc.Driver"
    $env:DB_USERNAME = "smartgrade"
    $env:DB_PASSWORD = "smartgrade123"
}

$args = @(
    "-Dfile.encoding=UTF-8"
)
# 单服务调试也接入 SkyWalking，保证老师单独启动服务后仍能看到 Trace。
if (Test-Path -LiteralPath $agent) {
    $args += @(
        "-javaagent:`"$agent`"",
        "-Dskywalking.agent.service_name=$($service.Name)",
        "-Dskywalking.collector.backend_service=127.0.0.1:11800"
    )
}
$args += @(
    "-jar", "`"$jar`"",
    "--logging.file.name=`"$projectRoot\logs\$ServiceName.log`""
)
$p = Start-Process -FilePath $java -ArgumentList $args -WorkingDirectory $projectRoot `
    -RedirectStandardOutput (Join-Path $projectRoot "logs\$ServiceName.out.log") `
    -RedirectStandardError (Join-Path $projectRoot "logs\$ServiceName.err.log") `
    -WindowStyle Hidden -PassThru

$deadline = (Get-Date).AddSeconds(180)
# A1 验收：轮询 /actuator/health，health.status=UP 才表示端口和 Spring 上下文均已就绪。
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
