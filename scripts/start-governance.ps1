param(
    [string]$AgentVersion = "9.4.0"
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$toolsRoot = if ($env:SMARTGRADE_TOOLS) { $env:SMARTGRADE_TOOLS } else { "D:\SmartGradeTools" }
$standaloneSeata = Join-Path $toolsRoot "seata-2.1.0"
$standaloneSkyWalking = Join-Path $toolsRoot "skywalking-9.7.0"

Push-Location $projectRoot
try {
    # 优先使用老师提供的本地 Seata/SkyWalking 发行包；不存在时回退 Docker。
    if ((Test-Path -LiteralPath $standaloneSeata) -and
        (Test-Path -LiteralPath $standaloneSkyWalking)) {
        & (Join-Path $PSScriptRoot "start-seata-standalone.ps1") -SeataHome $standaloneSeata
        & (Join-Path $PSScriptRoot "start-skywalking-standalone.ps1") -SkyWalkingHome $standaloneSkyWalking
    } else {
        if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
            throw "Install the standalone distributions under $toolsRoot or install Docker Desktop."
        }
        & docker compose -f ".\deploy\docker-compose-infra.yml" --profile governance up -d `
            seata-server skywalking-oap skywalking-ui
        if ($LASTEXITCODE -ne 0) {
            throw "Governance containers failed to start."
        }
    }

    $deadline = (Get-Date).AddMinutes(3)
    # 分别检查 Seata 控制台 7091 和 SkyWalking OAP 12800。
    do {
        $seataReady = $false
        $skyWalkingReady = $false
        try {
            $response = Invoke-WebRequest -Uri "http://127.0.0.1:7091" -UseBasicParsing -TimeoutSec 3
            $seataReady = $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
        } catch {
            $seataReady = $false
        }
        try {
            $body = @{ query = "query { version }" } | ConvertTo-Json
            $response = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:12800/graphql" `
                -ContentType "application/json" -Body $body -TimeoutSec 3
            $skyWalkingReady = $null -ne $response.data
        } catch {
            $skyWalkingReady = $false
        }
        if (-not ($seataReady -and $skyWalkingReady)) {
            Start-Sleep -Seconds 3
        }
    } while (-not ($seataReady -and $skyWalkingReady) -and (Get-Date) -lt $deadline)

    if (-not $seataReady) {
        throw "Seata Server did not become ready. Check: docker logs smartgrade-seata"
    }
    if (-not $skyWalkingReady) {
        throw "SkyWalking OAP did not become ready. Check: docker logs smartgrade-skywalking-oap"
    }

    & (Join-Path $PSScriptRoot "download-skywalking-agent.ps1") -Version $AgentVersion
    # 这三行输出现场应看到的事务端口、Seata 控制台和 SkyWalking UI。
    Write-Host "Seata transaction port: 127.0.0.1:8091"
    Write-Host "Seata console:          http://127.0.0.1:7091"
    Write-Host "SkyWalking UI:          http://127.0.0.1:12801"
} finally {
    Pop-Location
}
