param(
    [int]$Requests = 40,
    [string]$GatewayUrl = "http://127.0.0.1:8080",
    [string]$SentinelUrl = "http://127.0.0.1:8858",
    [string]$Username = "teacher1",
    [string]$Password = "Teach2026",
    [string]$SentinelUsername = "sentinel",
    [string]$SentinelPassword = "sentinel"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http
[System.Net.ServicePointManager]::DefaultConnectionLimit = [Math]::Max(64, $Requests)

$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/auth/login" -Body $loginBody `
    -ContentType "application/json" -TimeoutSec 8
$token = $login.data.accessToken
if (-not $token) {
    throw "Gateway login did not return an access token."
}

$handler = New-Object System.Net.Http.HttpClientHandler
$client = New-Object System.Net.Http.HttpClient($handler)
$client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $token)
$client.Timeout = [TimeSpan]::FromSeconds(15)

try {
    Write-Host "Sending $Requests concurrent GET requests through Gateway route answer-service..."
    # 并发请求用于触发 Gateway 的 answer-service 8 QPS 流控规则。
    $tasks = @()
    for ($index = 0; $index -lt $Requests; $index++) {
        $tasks += $client.GetAsync("$GatewayUrl/api/batches?page=1&size=1&burst=$index")
    }
    [System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]$tasks)
    $responses = @($tasks | ForEach-Object { $_.Result })
    # 按 HTTP 状态码汇总；预期前 8 个请求为 200，其余为 429。
    $responses | Group-Object { [int]$_.StatusCode } | Sort-Object Name | ForEach-Object {
        [PSCustomObject]@{ HttpStatus = $_.Name; Count = $_.Count }
    } | Format-Table -AutoSize

    $blocked = @($responses | Where-Object { [int]$_.StatusCode -eq 429 })
    if (-not $blocked.Count) {
        throw "No request was blocked. The Sentinel Gateway rule is not demonstrably active."
    }
    $sample = $blocked[0].Content.ReadAsStringAsync().Result
    # 输出被 Sentinel 阻断的真实响应体，证明 429 来自网关保护。
    Write-Host "Sample blocked response: $sample"
    try {
        $null = Invoke-WebRequest -Method Post -Uri "$SentinelUrl/auth/login" `
            -Body @{ username = $SentinelUsername; password = $SentinelPassword } `
            -ContentType "application/x-www-form-urlencoded" -SessionVariable sentinelSession -UseBasicParsing
        $apps = (Invoke-RestMethod -Uri "$SentinelUrl/app/briefinfos.json" -WebSession $sentinelSession).data
        $gateway = $apps | Where-Object app -eq "smartgrade-gateway" | Select-Object -First 1
        $machine = $gateway.machines[0]
        $resources = Invoke-RestMethod -Uri "$SentinelUrl/resource/machineResource.json?app=smartgrade-gateway&ip=$($machine.ip)&port=$($machine.port)&pid=&searchKey=answer-service&pageIndex=0&pageSize=100" `
            -WebSession $sentinelSession -TimeoutSec 30
        Write-Host "Sentinel live resource counters:"
        @($resources.data | Where-Object resource -eq "answer-service") | `
            Select-Object resource, oneMinutePass, oneMinuteBlock, oneMinuteTotal | Format-Table -AutoSize
    } catch {
        Write-Warning "The 429 proof succeeded, but the dashboard cannot query this client command port. Refresh the dashboard manually if its network route becomes available."
    }
    Write-Host "`nPASS: Sentinel returned real HTTP 429 responses through smartgrade-gateway."
    Write-Host "Refresh http://127.0.0.1:8858 and open smartgrade-gateway -> Real-time Monitoring / Gateway Flow Control."
} finally {
    $client.Dispose()
    $handler.Dispose()
}
