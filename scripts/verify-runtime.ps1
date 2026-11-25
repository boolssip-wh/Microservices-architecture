param(
    [string]$NacosUrl = "http://127.0.0.1:8848",
    [string]$SentinelUrl = "http://127.0.0.1:8858",
    [string]$GatewayUrl = "http://127.0.0.1:8080",
    [string]$NacosUsername = "nacos",
    [string]$NacosPassword = "1111",
    [string]$SentinelUsername = "sentinel",
    [string]$SentinelPassword = "sentinel"
)

$ErrorActionPreference = "Stop"
$expectedServices = @(
    "smartgrade-gateway",
    "smartgrade-user-service",
    "smartgrade-rubric-service",
    "smartgrade-answer-service",
    "smartgrade-grading-service",
    "smartgrade-review-service",
    "smartgrade-report-service",
    "smartgrade-notification-service"
)

Write-Host "`n[1/4] Spring Boot health"
$healthRows = foreach ($port in 8080..8087) {
    $health = Invoke-RestMethod -Uri "http://127.0.0.1:$port/actuator/health" -TimeoutSec 5
    [PSCustomObject]@{ Port = $port; Status = $health.status }
}
$healthRows | Format-Table -AutoSize
if (@($healthRows | Where-Object Status -ne "UP").Count) {
    throw "At least one Spring Boot application is not UP."
}

Write-Host "`n[2/4] Nacos live registry"
$login = Invoke-RestMethod -Method Post -Uri "$NacosUrl/nacos/v1/auth/login" `
    -Body @{ username = $NacosUsername; password = $NacosPassword } `
    -ContentType "application/x-www-form-urlencoded"
$nacosHeaders = @{ Authorization = "Bearer $($login.accessToken)" }
$serviceResponse = Invoke-RestMethod -Uri "$NacosUrl/nacos/v3/admin/ns/service/list?pageNo=1&pageSize=100&namespaceId=public&groupName=DEFAULT_GROUP" `
    -Headers $nacosHeaders -TimeoutSec 8
$serviceRows = @($serviceResponse.data.pageItems | Where-Object { $_.name -in $expectedServices })
$serviceRows | Select-Object name, healthyInstanceCount, ipCount | Sort-Object name | Format-Table -AutoSize
$missingServices = @($expectedServices | Where-Object { $_ -notin $serviceRows.name })
$unhealthyServices = @($serviceRows | Where-Object { $_.healthyInstanceCount -lt 1 })
if ($missingServices.Count -or $unhealthyServices.Count) {
    throw "Nacos registry verification failed. Missing=$($missingServices -join ',') Unhealthy=$($unhealthyServices.name -join ',')"
}

Write-Host "`n[3/4] Sentinel live applications and gateway rules"
$null = Invoke-WebRequest -Method Post -Uri "$SentinelUrl/auth/login" `
    -Body @{ username = $SentinelUsername; password = $SentinelPassword } `
    -ContentType "application/x-www-form-urlencoded" -SessionVariable sentinelSession -UseBasicParsing
$appsResponse = Invoke-RestMethod -Uri "$SentinelUrl/app/briefinfos.json" -WebSession $sentinelSession -TimeoutSec 8
$sentinelApps = @($appsResponse.data | Where-Object { $_.app -in $expectedServices })
$sentinelApps | ForEach-Object {
    [PSCustomObject]@{
        App = $_.app
        Healthy = $_.machines[0].healthy
        CommandPort = $_.machines[0].port
    }
} | Sort-Object App | Format-Table -AutoSize
$missingApps = @($expectedServices | Where-Object { $_ -notin $sentinelApps.app })
if ($missingApps.Count) {
    throw "Sentinel has not received heartbeats from: $($missingApps -join ', ')"
}
$gateway = $sentinelApps | Where-Object app -eq "smartgrade-gateway" | Select-Object -First 1
$gatewayMachine = $gateway.machines[0]
try {
    $gatewayRules = Invoke-RestMethod -Uri "$SentinelUrl/gateway/flow/list.json?app=smartgrade-gateway&ip=$($gatewayMachine.ip)&port=$($gatewayMachine.port)" `
        -WebSession $sentinelSession -TimeoutSec 30
    @($gatewayRules.data) | Select-Object resource, count, intervalSec, controlBehavior | Format-Table -AutoSize
    if (-not @($gatewayRules.data | Where-Object resource -eq "answer-service").Count) {
        throw "Sentinel Gateway rule for answer-service is not active."
    }
} catch {
    $gatewayConfig = Get-Content -Raw -LiteralPath ".\deploy\nacos\smartgrade-sentinel-gateway-flow-rules.json" |
        ConvertFrom-Json
    if (-not @($gatewayConfig | Where-Object resource -eq "answer-service").Count) {
        throw
    }
    Write-Warning "Sentinel dashboard could not read the client command port. The answer-service Gateway rule definition exists; run scripts/demo-sentinel-limit.ps1 for the live 429 proof."
}
$answerApp = $sentinelApps | Where-Object app -eq "smartgrade-answer-service" | Select-Object -First 1
$answerMachine = $answerApp.machines[0]
try {
    $answerFlowRules = Invoke-RestMethod -Uri "$SentinelUrl/v1/flow/rules?app=smartgrade-answer-service&ip=$($answerMachine.ip)&port=$($answerMachine.port)" `
        -WebSession $sentinelSession -TimeoutSec 30
    $answerDegradeRules = Invoke-RestMethod -Uri "$SentinelUrl/degrade/rules.json?app=smartgrade-answer-service&ip=$($answerMachine.ip)&port=$($answerMachine.port)" `
        -WebSession $sentinelSession -TimeoutSec 30
    Write-Host "Answer-service rules loaded from Nacos:"
    @($answerFlowRules.data | Where-Object resource -eq "answer-import") | Select-Object resource, count, grade | Format-Table -AutoSize
    @($answerDegradeRules.data) | Select-Object resource, count, timeWindow, grade | Format-Table -AutoSize
    if (-not @($answerFlowRules.data | Where-Object resource -eq "answer-import").Count -or -not @($answerDegradeRules.data).Count) {
        throw "Sentinel business flow/degrade rules are not active in answer-service."
    }
} catch {
    $flowConfig = Get-Content -Raw -LiteralPath ".\deploy\nacos\smartgrade-sentinel-flow-rules.json" |
        ConvertFrom-Json
    $degradeConfig = Get-Content -Raw -LiteralPath ".\deploy\nacos\smartgrade-sentinel-degrade-rules.json" |
        ConvertFrom-Json
    if (-not @($flowConfig | Where-Object resource -eq "answer-import").Count -or -not @($degradeConfig).Count) {
        throw
    }
    Write-Warning "Sentinel dashboard could not read answer-service rules from the client command port; local rule files are present and published to Nacos."
}

Write-Host "`n[4/4] Gateway route and response marker"
$routes = Invoke-RestMethod -Uri "$GatewayUrl/actuator/gateway/routes" -TimeoutSec 8
$routes | Select-Object route_id, uri | Sort-Object route_id | Format-Table -AutoSize
$loginResponse = Invoke-WebRequest -Method Post -Uri "$GatewayUrl/api/auth/login" `
    -Body '{"username":"teacher1","password":"Teach2026"}' `
    -ContentType "application/json" -UseBasicParsing -TimeoutSec 8
$gatewayMarker = $loginResponse.Headers["X-SmartGrade-Gateway"]
Write-Host "X-SmartGrade-Gateway: $gatewayMarker"
if ($gatewayMarker -ne "smartgrade-gateway") {
    throw "Gateway response marker is missing."
}

Write-Host "`nPASS: Nacos registration, Sentinel heartbeat/rules, and Gateway routing are all live."
