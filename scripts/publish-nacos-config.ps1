param(
    [string]$NacosUrl = $(if ($env:NACOS_ADDR) { "http://$env:NACOS_ADDR" } else { "http://127.0.0.1:8848" }),
    [string]$Username = $(if ($env:NACOS_USERNAME) { $env:NACOS_USERNAME } else { "nacos" }),
    [string]$Password = $(if ($env:NACOS_PASSWORD) { $env:NACOS_PASSWORD } else { "1111" }),
    [string]$Namespace = $(if ($env:NACOS_NAMESPACE) { $env:NACOS_NAMESPACE } else { "public" })
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$configRoot = Join-Path $projectRoot "deploy\nacos"

$login = Invoke-RestMethod -Method Post `
    -Uri "$NacosUrl/nacos/v1/auth/login" `
    -Body @{ username = $Username; password = $Password } `
    -ContentType "application/x-www-form-urlencoded"
$headers = @{ Authorization = "Bearer $($login.accessToken)" }

$configs = @(
    @{ DataId = "smartgrade-common.yaml"; File = "smartgrade-common.yaml"; Type = "yaml" },
    @{ DataId = "smartgrade-sentinel-flow-rules"; File = "smartgrade-sentinel-flow-rules.json"; Type = "json" },
    @{ DataId = "smartgrade-sentinel-degrade-rules"; File = "smartgrade-sentinel-degrade-rules.json"; Type = "json" },
    @{ DataId = "smartgrade-sentinel-gateway-flow-rules"; File = "smartgrade-sentinel-gateway-flow-rules.json"; Type = "json" }
)

foreach ($config in $configs) {
    $content = Get-Content -Raw -LiteralPath (Join-Path $configRoot $config.File)
    $body = @{
        dataId = $config.DataId
        groupName = "DEFAULT_GROUP"
        namespaceId = $Namespace
        content = $content
        type = $config.Type
    }
    $response = Invoke-RestMethod -Method Post `
        -Uri "$NacosUrl/nacos/v3/admin/cs/config" `
        -Headers $headers `
        -Body $body `
        -ContentType "application/x-www-form-urlencoded"
    if ($response.code -ne 0) {
        throw "发布 $($config.DataId) 失败：$($response.message)"
    }
    Write-Host "Published $($config.DataId)"
}
