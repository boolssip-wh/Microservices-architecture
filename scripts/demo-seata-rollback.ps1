param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$MysqlExe = "D:\mysql-8.0.41\bin\mysql.exe",
    [int]$MysqlPort = 3307,
    [string]$MysqlUser = "smartgrade",
    [string]$MysqlPassword = "smartgrade123",
    [string]$StudentNo = "RBTEST2026",
    [string]$ServiceToken = "local-service-token",
    [int]$HoldSeconds = 10
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http
$script:serviceToken = $ServiceToken

function Set-FailMode {
    param([bool]$Enabled)
    $value = $Enabled.ToString().ToLowerInvariant()
    $response = Invoke-RestMethod -Method Put `
        -Uri "http://127.0.0.1:8083/internal/demo/seata/fail-after-remote/$value" `
        -Headers @{ "X-Service-Token" = $script:serviceToken } -TimeoutSec 10
    if ($response.code -ne 0) {
        throw "Could not set Seata demo fault mode: $($response.message)"
    }
}

function Set-HoldMillis {
    param([long]$Millis)
    $response = Invoke-RestMethod -Method Put `
        -Uri "http://127.0.0.1:8083/internal/demo/seata/hold-millis/$Millis" `
        -Headers @{ "X-Service-Token" = $script:serviceToken } -TimeoutSec 10
    if ($response.code -ne 0) {
        throw "Could not set Seata demo hold time: $($response.message)"
    }
}

trap {
    try { Set-FailMode $false } catch { }
    try { Set-HoldMillis 0 } catch { }
    throw
}

function Call-Api {
    param([string]$Method, [string]$Path, $Body)
    $request = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        ContentType = "application/json; charset=utf-8"
        Headers = $script:auth
    }
    if ($null -ne $Body) {
        $request.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 20))
    }
    $response = Invoke-RestMethod @request -TimeoutSec 30
    if ($response.code -ne 0) {
        throw "$Method $Path failed: $($response.message)"
    }
    $response.data
}

function Review-Count {
    $sql = "SELECT COUNT(*) FROM smartgrade_review.review_result_projection WHERE student_no='$StudentNo';"
    $result = & $MysqlExe --protocol=TCP --host=127.0.0.1 --port=$MysqlPort `
        --user=$MysqlUser --password=$MysqlPassword --batch --skip-column-names -e $sql
    if ($LASTEXITCODE -ne 0) {
        throw "Could not query review database."
    }
    [int]($result | Select-Object -First 1)
}

$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" `
    -ContentType "application/json" `
    -Body '{"username":"teacher1","password":"Teach2026"}'
$script:auth = @{ Authorization = "Bearer $($login.data.accessToken)" }

Set-FailMode $true
$holdMillis = [Math]::Max(0, $HoldSeconds) * 1000L
Set-HoldMillis $holdMillis
if ($holdMillis -gt 0) {
    Write-Host "Seata transaction will stay open for $HoldSeconds seconds. Refresh the Seata console now."
}
$before = Review-Count
$nonce = [guid]::NewGuid().ToString("N").Substring(0, 8)
$batch = Call-Api POST "/api/batches" @{
    examName = "Seata rollback verification $nonce"
    questionNo = "Q1"
    questionTitle = "Rollback demo"
    questionText = "Verify the short transaction boundary."
    maxScore = 20
    baseScore = 10
}
$rubric = Call-Api POST "/api/rubrics" @{
    batchId = $batch.batchId
    sourceText = "Concept: clarity; Practice: action."
    maxScore = 20
    baseScore = 10
}
$points = @(
    @{pointName="Concept";pointType="CONCEPT";keywords=@("clarity");variants=@();weight=50;maxScore=5},
    @{pointName="Practice";pointType="APPLICATION";keywords=@("action");variants=@();weight=50;maxScore=5}
)
$null = Call-Api PUT "/api/rubrics/$($rubric.rubricId)/points" @{ points = $points }
$null = Call-Api POST "/api/rubrics/$($rubric.rubricId)/compile" @{ expectedWeightTotal = 100 }

$client = [System.Net.Http.HttpClient]::new()
$form = [System.Net.Http.MultipartFormDataContent]::new()
try {
    $client.DefaultRequestHeaders.Add("Authorization", $script:auth.Authorization)
    $client.DefaultRequestHeaders.Add("Idempotency-Key", "rollback-" + $nonce)
    $bytes = [Text.Encoding]::UTF8.GetBytes(
        "This answer demonstrates clarity and action with sufficient detail for the rollback test."
    )
    $form.Add([System.Net.Http.ByteArrayContent]::new($bytes), "files", "$StudentNo.txt")
    $form.Add([System.Net.Http.StringContent]::new("TEXT"), "sourceType")
    $form.Add([System.Net.Http.StringContent]::new("filename"), "numberingRule")
    $response = $client.PostAsync(
        "$BaseUrl/api/batches/$($batch.batchId)/answers/import", $form
    ).GetAwaiter().GetResult()
    $text = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if (-not $response.IsSuccessStatusCode) {
        throw "Upload failed ($($response.StatusCode)): $text"
    }
} finally {
    $form.Dispose()
    $client.Dispose()
}

$deadline = (Get-Date).AddSeconds(90)
do {
    $answers = Call-Api GET "/api/batches/$($batch.batchId)/answers?page=1&size=20"
    $answer = @($answers.items | Where-Object studentNo -eq $StudentNo) | Select-Object -First 1
    if ($answer -and $answer.processStatus -notin @("PENDING", "RUNNING")) {
        break
    }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)

if (-not $answer -or $answer.processStatus -in @("PENDING", "RUNNING")) {
    throw "Rollback test did not finish."
}
$after = Review-Count
if ($after -ne $before) {
    throw "Review projection count changed from $before to $after; rollback failed."
}
if ($answer.processStatus -ne "FAILED") {
    throw "Expected FAILED answer status after injected failure, got $($answer.processStatus)."
}
Set-FailMode $false
Set-HoldMillis 0

@{
    batchId = $batch.batchId
    answerId = $answer.answerId
    reviewCountBefore = $before
    reviewCountAfter = $after
    answerStatus = $answer.processStatus
    holdSeconds = $HoldSeconds
    xid = "See answer/review logs for SEATA_DEMO_FAIL_AFTER_REMOTE"
} | ConvertTo-Json | Tee-Object -FilePath (Join-Path $PSScriptRoot "..\.runtime\seata-rollback-report.json")
Write-Host "PASS: review projection rolled back and answer is reachable as FAILED."
