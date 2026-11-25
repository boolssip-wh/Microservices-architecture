param(
    [string]$SkyWalkingHome = "D:\SmartGradeTools\skywalking-9.7.0",
    [string]$JavaHome = "D:\SmartGradeTools\jdk17"
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$java = Join-Path $JavaHome "bin\java.exe"
if (-not (Test-Path -LiteralPath $java)) {
    throw "JDK was not found at $java."
}
$oapClassPath = "$SkyWalkingHome\config;$SkyWalkingHome\oap-libs\*;."
$webClassPath = "$SkyWalkingHome\webapp\skywalking-webapp.jar;$SkyWalkingHome\webapp"
if (-not (Test-Path -LiteralPath "$SkyWalkingHome\oap-libs")) {
    throw "SkyWalking APM distribution was not found at $SkyWalkingHome."
}

if (-not (Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object LocalPort -eq 12800)) {
    $env:SW_RECEIVER_AWS_FIREHOSE_HTTP_PORT = "12802"
    $env:JAVA_HOME = $JavaHome
    Start-Process -FilePath $java -ArgumentList @(
        "-Xms256m", "-Xmx512m", "-Doap.logDir=$SkyWalkingHome\logs",
        "-cp", "`"$oapClassPath`"",
        "org.apache.skywalking.oap.server.starter.OAPServerStartUp"
    ) -WorkingDirectory $SkyWalkingHome -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $projectRoot ".runtime\skywalking-oap.out.log") `
        -RedirectStandardError (Join-Path $projectRoot ".runtime\skywalking-oap.err.log")
}

if (-not (Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object LocalPort -eq 12801)) {
    $env:SW_SERVER_PORT = "12801"
    $env:SW_OAP_ADDRESS = "http://127.0.0.1:12800"
    Start-Process -FilePath $java -ArgumentList @(
        "-Dwebapp.logDir=$SkyWalkingHome\logs",
        "-cp", "`"$webClassPath`"",
        "org.apache.skywalking.oap.server.webapp.ApplicationStartUp"
    ) -WorkingDirectory $SkyWalkingHome -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $projectRoot ".runtime\skywalking-ui.out.log") `
        -RedirectStandardError (Join-Path $projectRoot ".runtime\skywalking-ui.err.log")
}

$deadline = (Get-Date).AddSeconds(180)
do {
    $oapReady = $false
    $uiReady = $false
    try {
        $body = @{ query = "{ version }" } | ConvertTo-Json
        $response = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:12800/graphql" `
            -ContentType "application/json" -Body $body -TimeoutSec 3
        $oapReady = $null -ne $response.data
    } catch { $oapReady = $false }
    try {
        $uiReady = (Invoke-WebRequest -Uri "http://127.0.0.1:12801" -UseBasicParsing -TimeoutSec 3).StatusCode -eq 200
    } catch { $uiReady = $false }
    if (-not ($oapReady -and $uiReady)) {
        Start-Sleep -Seconds 3
    }
} while (-not ($oapReady -and $uiReady) -and (Get-Date) -lt $deadline)

if (-not ($oapReady -and $uiReady)) {
    throw "SkyWalking did not become ready. Check .runtime\skywalking-*.log."
}
Write-Host "SkyWalking ready: UI http://127.0.0.1:12801, OAP 127.0.0.1:11800"
