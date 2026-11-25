param(
    [string]$GatewayUrl = "http://127.0.0.1:8080",
    [string]$SkyWalkingUrl = "http://127.0.0.1:12800",
    [string[]]$ExpectedServices = @(
        "smartgrade-gateway",
        "smartgrade-user-service",
        "smartgrade-rubric-service",
        "smartgrade-answer-service",
        "smartgrade-grading-service",
        "smartgrade-review-service",
        "smartgrade-report-service",
        "smartgrade-notification-service"
    )
)

$ErrorActionPreference = "Stop"
$login = Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/auth/login" `
    -ContentType "application/json" `
    -Body '{"username":"teacher1","password":"Teach2026"}' `
    -TimeoutSec 10
if ($login.code -ne 0) {
    throw "Gateway login failed: $($login.message)"
}
$headers = @{ Authorization = "Bearer $($login.data.accessToken)" }
$null = Invoke-RestMethod -Uri "$GatewayUrl/api/batches?page=1&size=5" -Headers $headers -TimeoutSec 10
try {
    $null = Invoke-RestMethod -Uri "$GatewayUrl/api/reports/batches/UNKNOWN/insights" `
        -Headers $headers -TimeoutSec 10
} catch {
    # A traced 404 still proves that Gateway, Report and the downstream chain were exercised.
}
$null = Invoke-RestMethod -Uri "$GatewayUrl/api/notifications?page=1&size=5" -Headers $headers -TimeoutSec 10

Start-Sleep -Seconds 8
$now = [DateTimeOffset]::UtcNow
$start = $now.AddMinutes(-15).ToString("yyyy-MM-dd HH:mm:ss")
$end = $now.AddMinutes(1).ToString("yyyy-MM-dd HH:mm:ss")
$query = @"
query {
  getAllServices(duration: {start: "$start", end: "$end", step: MINUTE}) {
    name
  }
}
"@
$response = Invoke-RestMethod -Method Post -Uri "$SkyWalkingUrl/graphql" `
    -ContentType "application/json" `
    -Body (@{ query = $query } | ConvertTo-Json -Depth 5) `
    -TimeoutSec 15

$services = @($response.data.getAllServices | ForEach-Object name)
$services | Sort-Object | ForEach-Object { Write-Host "SkyWalking service: $_" }
$missing = @($ExpectedServices | Where-Object { $_ -notin $services })
if ($missing.Count) {
    throw "SkyWalking has not observed: $($missing -join ', '). Run the full contract flow and retry."
}
Write-Host "PASS: all expected services are visible in SkyWalking."
