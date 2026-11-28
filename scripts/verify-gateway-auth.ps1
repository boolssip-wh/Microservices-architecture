param(
    [string]$GatewayUrl = "http://127.0.0.1:8080"
)

$ErrorActionPreference = "Stop"
$login = Invoke-RestMethod -Method Post -Uri "$GatewayUrl/api/auth/login" `
    -ContentType "application/json" -Body '{"username":"teacher1","password":"Teach2026"}' `
    -TimeoutSec 10
$valid = Invoke-WebRequest -Uri "$GatewayUrl/api/batches?page=1&size=5" `
    -Headers @{ Authorization = "Bearer $($login.data.accessToken)" } -UseBasicParsing -TimeoutSec 10
# 合法 JWT 应返回 200，并带网关标记头。
$marker = $valid.Headers["X-SmartGrade-Gateway"]

$invalidStatus = $null
try {
    # 故意发送无效 JWT，预期 Gateway 在进入业务服务前返回 401。
    $null = Invoke-WebRequest -Uri "$GatewayUrl/api/batches?page=1&size=5" `
        -Headers @{ Authorization = "Bearer not-a-valid-token" } -UseBasicParsing -TimeoutSec 10
} catch {
    $invalidStatus = $_.Exception.Response.StatusCode.value__
}

[PSCustomObject]@{
    ValidStatus = $valid.StatusCode
    GatewayMarker = $marker
    InvalidStatus = $invalidStatus
} | Format-List

if ($valid.StatusCode -ne 200 -or $marker -ne "smartgrade-gateway" -or $invalidStatus -ne 401) {
    throw "Gateway auth verification failed."
}
Write-Host "PASS: valid token is forwarded, invalid token is rejected, and the gateway marker is present."
