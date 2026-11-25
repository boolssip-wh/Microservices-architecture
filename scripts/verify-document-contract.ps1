param([int]$BasePort = 18100, [switch]$KeepRunning, [string]$BuildDirectory = 'target')
$ErrorActionPreference = 'Stop'
$project = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runDir = Join-Path $project ('.runtime/contract-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $runDir -Force | Out-Null
$jdkTmp = Join-Path $project '.runtime/jdk-tmp'
New-Item -ItemType Directory -Path $jdkTmp -Force | Out-Null
$java = Join-Path $(if ($env:SMARTGRADE_TOOLS) {$env:SMARTGRADE_TOOLS} else {'D:\SmartGradeTools'}) 'jdk17/bin/java.exe'
$children = @()
$verified = $false
$services = @('user','rubric','answer','grading','review','report','notification')
function Assert-Contract($condition, [string]$message) { if (-not $condition) { throw $message }; Write-Output "PASS $message" }
function Call-Api([string]$method, [string]$path, $body = $null, $headers = $script:auth) {
    $args = @{ Method=$method; Uri="http://127.0.0.1:$BasePort$path"; Headers=$headers; TimeoutSec=60 }
    if ($null -ne $body) { $args.ContentType='application/json; charset=utf-8'; $args.Body=[Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json -Depth 20)) }
    (Invoke-RestMethod @args).data
}
function Upload([string]$batch, [string]$key, [string]$name, [byte[]]$bytes, [string]$type) {
    $client = [System.Net.Http.HttpClient]::new()
    $form = [System.Net.Http.MultipartFormDataContent]::new()
    try {
        $client.DefaultRequestHeaders.Add('Authorization', $script:auth.Authorization)
        $client.DefaultRequestHeaders.Add('Idempotency-Key', $key)
        $form.Add([System.Net.Http.ByteArrayContent]::new($bytes), 'files', $name)
        $form.Add([System.Net.Http.StringContent]::new($type), 'sourceType')
        $form.Add([System.Net.Http.StringContent]::new('filename'), 'numberingRule')
        $response = $client.PostAsync("http://127.0.0.1:$BasePort/api/batches/$batch/answers/import", $form).GetAwaiter().GetResult()
        $text = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) { throw "Upload $($response.StatusCode): $text" }
        ($text | ConvertFrom-Json).data
    } finally { $form.Dispose(); $client.Dispose() }
}
try {
    for ($i=0; $i -le 7; $i++) {
        $module = if ($i -eq 0) {'smartgrade-gateway'} else {"smartgrade-$($services[$i-1])-service"}
        $port = $BasePort + $i
        if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "Test port occupied: $port" }
        $jar = Join-Path $project "$module/$BuildDirectory/$module-1.0.0.jar"
        $args = @('-Xms64m','-Xmx192m',"-Djdk.net.unixdomain.tmpdir=`"$jdkTmp`"",'-jar',"`"$jar`"","--server.port=$port",'--spring.config.import=',
            '--spring.cloud.nacos.config.enabled=false','--spring.cloud.nacos.discovery.enabled=false',
            '--spring.cloud.sentinel.enabled=false','--spring.cloud.openfeign.circuitbreaker.enabled=false',
            '--management.health.redis.enabled=false','--spring.data.redis.repositories.enabled=false',
            '--app.ocr.api-key=','--spring.cloud.gateway.enabled=true',"--app.storage.root=`"$runDir/objects`"", "--NOTIFICATION_URL=http://127.0.0.1:$($BasePort+7)")
        for ($j=0; $j -lt $services.Count; $j++) {
            $servicePort = $BasePort + $j + 1
            $args += "--spring.cloud.openfeign.client.config.smartgrade-$($services[$j])-service.url=http://127.0.0.1:$servicePort"
            if ($i -eq 0) { $args += "--$($services[$j].ToUpper())_SERVICE_URI=http://127.0.0.1:$servicePort" }
        }
        $children += Start-Process -FilePath $java -ArgumentList $args -WorkingDirectory $project -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $runDir "$module.log") -RedirectStandardError (Join-Path $runDir "$module.err.log")
    }
    $deadline = [DateTime]::Now.AddMinutes(3)
    do {
        foreach($child in $children) {
            $child.Refresh()
            if ($child.HasExited) { throw "Application exited during startup (PID $($child.Id)). Inspect $runDir" }
        }
        $ready = 0
        for($i=0;$i -le 7;$i++) {
            try { $health=Invoke-RestMethod "http://127.0.0.1:$($BasePort+$i)/actuator/health" -TimeoutSec 2; if($health.status -eq 'UP'){$ready++} } catch {}
        }
        if ($ready -eq 8) { break }
        Start-Sleep -Seconds 2
    } while ([DateTime]::Now -lt $deadline)
    Assert-Contract ($ready -eq 8) "all 8 applications and Flyway migrations are healthy"
    $script:auth=@{}
    $login=Call-Api POST '/api/auth/login' @{username='teacher1';password='Teach2026'}
    $script:auth=@{Authorization="Bearer $($login.accessToken)"}
    $batch=Call-Api POST '/api/batches' @{examName='文档一致性验收';questionNo='Q1';questionTitle='主观题规则一致性';maxScore=20;baseScore=8}
    Assert-Contract ($batch.status -eq 'DRAFT') 'new batch starts DRAFT'
    Assert-Contract ($batch.baseScore -eq 8) 'custom base score is persisted'
    $source='概念理解：共同富裕、发展；联系实际：实践、改革。这是完整的评分细则内容。'
    $rubric=Call-Api POST '/api/rubrics' @{batchId=$batch.batchId;sourceText=$source;maxScore=20;baseScore=8}
    $points=@(@{pointName='概念理解';pointType='CONCEPT';keywords=@('共同富裕');variants=@();weight=50;maxScore=6},
        @{pointName='联系实际';pointType='APPLICATION';keywords=@('实践');variants=@();weight=50;maxScore=6})
    $null=Call-Api PUT "/api/rubrics/$($rubric.rubricId)/points" @{points=$points}
    $frozen=Call-Api POST "/api/rubrics/$($rubric.rubricId)/compile" @{expectedWeightTotal=100}
    Assert-Contract ($frozen.rubricVersion -eq 1) 'rule version frozen'
    $rubric2=Call-Api POST '/api/rubrics' @{batchId=$batch.batchId;sourceText=$source;maxScore=20;baseScore=8}
    $null=Call-Api PUT "/api/rubrics/$($rubric2.rubricId)/points" @{points=$points}
    $frozen2=Call-Api POST "/api/rubrics/$($rubric2.rubricId)/compile" @{expectedWeightTotal=100}
    Assert-Contract ($frozen2.rubricVersion -eq 2) 'a second rubric in the same batch freezes version 2'
    $bytes=[Text.Encoding]::UTF8.GetBytes('共同富裕需要持续发展，青年应结合实践，不断改革和奋斗。')
    $job=Upload $batch.batchId 'contract-import-001' '2026001.txt' $bytes 'TEXT'
    $repeat=Upload $batch.batchId 'contract-import-001' '2026001.txt' $bytes 'TEXT'
    Assert-Contract ($job.jobId -eq $repeat.jobId -and $job.acceptedCount -eq 1) 'import idempotency returns original job'
    $deadline=[DateTime]::Now.AddSeconds(45)
    do { $results=Call-Api GET "/api/results?batchId=$($batch.batchId)"; if($results.items.Count -gt 0){break}; Start-Sleep -Milliseconds 500 } while([DateTime]::Now -lt $deadline)
    Assert-Contract ($results.items.Count -eq 1) 'async grading creates one review projection'
    $answerId=$results.items[0].answerId
    $originalPath = Join-Path $runDir 'original-answer.txt'
    Invoke-WebRequest -Uri "http://127.0.0.1:$BasePort/api/batches/$($batch.batchId)/answers/$answerId/original" -Headers $script:auth -OutFile $originalPath
    Assert-Contract ([Convert]::ToBase64String([IO.File]::ReadAllBytes($originalPath)) -eq [Convert]::ToBase64String($bytes)) 'original answer download preserves the uploaded bytes'
    $otherLogin = Call-Api POST '/api/auth/login' @{username='assistant1';password='Teach2026'}
    $denied = $false
    try { $null = Invoke-WebRequest -Uri "http://127.0.0.1:$BasePort/api/batches/$($batch.batchId)/answers/$answerId/original" -Headers @{Authorization="Bearer $($otherLogin.accessToken)"} }
    catch { $denied = [int]$_.Exception.Response.StatusCode -eq 403 }
    Assert-Contract $denied 'another teacher cannot read the original answer'
    $resultDetail=Call-Api GET "/api/results/$answerId"
    Assert-Contract ($resultDetail.baseScore -eq 8 -and $resultDetail.suggestedScore -eq 20) 'custom base score reaches grading and review'
    $null=Call-Api PUT "/api/results/$answerId/final-score" @{finalScore=18.5;finalComment='概念准确，注意结合具体事例。';reviewNote='验收';commentProfileVersion=1}
    $detail=Call-Api GET "/api/batches/$($batch.batchId)"
    Assert-Contract ($detail.status -eq 'COMPLETED' -and $detail.progress.waitingReview -eq 0) 'confirmed scores synchronize batch completion'
    $confirmed=Call-Api GET "/api/results?batchId=$($batch.batchId)"
    Assert-Contract ($confirmed.items[0].finalScore -eq 18.5) 'final score preserves decimals'
    $stats=Call-Api GET "/api/reports/batches/$($batch.batchId)/insights"
    $export=Call-Api POST '/api/reports/exports' @{batchId=$batch.batchId;format='XLSX';questionNo='Q1'}
    $download=Call-Api GET "/api/reports/exports/$($export.exportId)"
    Assert-Contract ($null -ne $download.downloadUrl) 'confirmed score exports XLSX'
    $downloadPath = ([uri]$download.downloadUrl).PathAndQuery
    if (-not $downloadPath) { $downloadPath = $download.downloadUrl }
    $exportResponse = Invoke-WebRequest -Uri "http://127.0.0.1:$BasePort$downloadPath" -Headers $script:auth -TimeoutSec 30
    Assert-Contract ($exportResponse.RawContentLength -gt 0) 'export download returns a nonempty file'
    $notes=Call-Api GET '/api/notifications'
    Assert-Contract ($notes.items.Count -gt 0) 'completion events produce notifications'
    $verified = $true
    Write-Output "CONTRACT CHECK PASSED. Logs: $runDir"
} finally {
    if ($KeepRunning -and $verified) {
        $children | ForEach-Object { @{pid=$_.Id;startTimeUtcTicks=$_.StartTime.ToUniversalTime().Ticks} } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runDir 'qa-pids.json') -Encoding UTF8
        Write-Output "QA applications remain on $BasePort-$($BasePort+7); tracked processes: $runDir/qa-pids.json"
    } else {
        foreach($child in $children) { if (-not $child.HasExited) { Stop-Process -Id $child.Id -ErrorAction SilentlyContinue } }
    }
}
