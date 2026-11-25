param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [int]$TimeoutSeconds = 420
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$sampleRoot = Join-Path $projectRoot "samples"
$manifestPath = (Get-ChildItem -LiteralPath $sampleRoot -File -Filter "*.json" |
    Where-Object Name -ne "demo-rubric.json" |
    Select-Object -First 1 -ExpandProperty FullName)
$manifest = Get-Content -Raw -LiteralPath $manifestPath -Encoding UTF8 | ConvertFrom-Json
$rubricConfig = Get-Content -Raw -LiteralPath (Join-Path $sampleRoot "demo-rubric.json") `
    -Encoding UTF8 | ConvertFrom-Json

function Call-Api {
    param([string]$Method, [string]$Path, $Body)
    $request = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        ContentType = "application/json"
        Headers = $script:auth
    }
    if ($null -ne $Body) {
        $jsonBody = $Body | ConvertTo-Json -Depth 20
        $request.Body = [Text.Encoding]::UTF8.GetBytes($jsonBody)
        $request.ContentType = "application/json; charset=utf-8"
    }
    $response = Invoke-RestMethod @request -TimeoutSec 30
    if ($response.code -ne 0) {
        throw "$Method $Path failed: $($response.message)"
    }
    $response.data
}

$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" `
    -ContentType "application/json" `
    -Body '{"username":"teacher1","password":"Teach2026"}' -TimeoutSec 10
$script:auth = @{ Authorization = "Bearer $($login.data.accessToken)" }

$batch = Call-Api POST "/api/batches" @{
    examName = "五四青年节 · 新时代好青年论述题"
    questionNo = "Q1"
    questionTitle = "如何争做新时代好青年，早日入党"
    questionText = @"
在今年五四青年节到来之际，中共中央总书记、国家主席、中央军委主席习近平代表党中央，向全国广大青年致以节日祝贺和诚挚问候。

习近平总书记指出，新时代新征程上，全国各族青年听从党和人民的召唤，在科技创新、乡村振兴、绿色发展、社会服务、卫国戍边等各领域各方面勇当排头兵和生力军，展现出自信自强、刚健有为的精神风貌。党中央对广大青年充分信任、寄予厚望！

习近平总书记强调，今年是新中国成立75周年，是五四运动105周年。广大青年要继承和发扬五四精神，坚定不移听党话、跟党走，争做有理想、敢担当能吃苦、肯奋斗的新时代好青年，在推进强国建设、民族复兴伟业中展现青春作为、彰显青春风采、贡献青春力量，奋力书写为中国式现代化挺膺担当的青春篇章。

作为一名入党积极分子，通过校史、院史和众多优秀校友的学习、了解，请你结合学习心得和自身实际，来谈谈如何争做新时代好青年，早日入党。
"@
    maxScore = 20
    baseScore = 10
}
$rubric = Call-Api POST "/api/rubrics" @{
    batchId = $batch.batchId
    sourceText = $rubricConfig.sourceText
    maxScore = 20
    baseScore = 10
}
$null = Call-Api PUT "/api/rubrics/$($rubric.rubricId)/points" @{ points = $rubricConfig.points }
$null = Call-Api POST "/api/rubrics/$($rubric.rubricId)/compile" @{ expectedWeightTotal = 100 }

$client = [System.Net.Http.HttpClient]::new()
$form = [System.Net.Http.MultipartFormDataContent]::new()
try {
    $client.DefaultRequestHeaders.Add("Authorization", $script:auth.Authorization)
    $client.DefaultRequestHeaders.Add("Idempotency-Key", "visual-set-" + [guid]::NewGuid().ToString("N"))
    foreach ($answer in $manifest.answers) {
        $path = Join-Path $sampleRoot $answer.file
        $content = [System.Net.Http.ByteArrayContent]::new([IO.File]::ReadAllBytes($path))
        $content.Headers.ContentType =
            [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("image/jpeg")
        $form.Add($content, "files", $answer.file)
    }
    $form.Add([System.Net.Http.StringContent]::new("IMAGE"), "sourceType")
    $form.Add([System.Net.Http.StringContent]::new("filename"), "numberingRule")
    $response = $client.PostAsync(
        "$BaseUrl/api/batches/$($batch.batchId)/answers/import", $form
    ).GetAwaiter().GetResult()
    $text = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if (-not $response.IsSuccessStatusCode) {
        throw "Upload failed ($($response.StatusCode)): $text"
    }
    $import = ($text | ConvertFrom-Json).data
    if ($import.acceptedCount -ne 10) {
        throw "Expected 10 accepted files, got $($import.acceptedCount)"
    }
} finally {
    $form.Dispose()
    $client.Dispose()
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
do {
    $answerPage = Call-Api GET "/api/batches/$($batch.batchId)/answers?page=1&size=100"
    $active = @($answerPage.items | Where-Object {
        $_.processStatus -in @("PENDING", "RUNNING")
    })
    if ($active.Count -eq 0) {
        break
    }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)
if ($active.Count -ne 0) {
    throw "Visual grading did not finish within $TimeoutSeconds seconds."
}

$results = Call-Api GET "/api/results?batchId=$($batch.batchId)&page=1&size=100"
$report = @()
foreach ($expected in $manifest.answers) {
    $item = @($results.items | Where-Object studentNo -eq $expected.studentNo)
    if ($item.Count -ne 1) {
        throw "Missing result for $($expected.studentNo)"
    }
    $detail = Call-Api GET "/api/results/$($item[0].answerId)"
    $contentHits = @($detail.evidence | Where-Object matchType -ne "LENGTH")
    if ($contentHits.Count -ne $expected.contentHitCount) {
        throw "Evidence mismatch for $($expected.studentNo): expected $($expected.contentHitCount) content hits, actual $($contentHits.Count)"
    }
    $report += [PSCustomObject]@{
        studentNo = $expected.studentNo
        expectedHitCount = $expected.contentHitCount
        actualContentHitCount = $contentHits.Count
        evidenceIds = @($contentHits | ForEach-Object evidenceId)
        coveragePointHit = @($detail.evidence | Where-Object matchType -eq "LENGTH").Count -gt 0
        suggestedScore = $detail.suggestedScore
        ocrConfidence = $detail.ocrConfidence
    }
}

$four = @($report | Where-Object actualContentHitCount -eq 4).Count
$three = @($report | Where-Object actualContentHitCount -eq 3).Count
$zero = @($report | Where-Object actualContentHitCount -eq 0).Count
if ($four -ne 2 -or $three -ne 7 -or $zero -ne 1) {
    throw "Unexpected visual verification distribution."
}

$runtime = Join-Path $projectRoot ".runtime"
New-Item -ItemType Directory -Force -Path $runtime | Out-Null
$reportPath = Join-Path $runtime "visual-sample-verification.json"
@{
    batchId = $batch.batchId
    jobId = $import.jobId
    verifiedAt = (Get-Date).ToString("o")
    results = $report
} | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $reportPath -Encoding UTF8

$report | Format-Table -AutoSize
Write-Host "PASS: visual grading completed for all 10 samples. Report: $reportPath"
