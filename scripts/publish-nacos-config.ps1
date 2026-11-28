param(
    [string]$NacosUrl = $(if ($env:NACOS_ADDR) { "http://$env:NACOS_ADDR" } else { "http://127.0.0.1:8848" }),
    [string]$Username = $(if ($env:NACOS_USERNAME) { $env:NACOS_USERNAME } else { "nacos" }),
    [string]$Password = $(if ($env:NACOS_PASSWORD) { $env:NACOS_PASSWORD } else { "1111" }),
    [string]$Namespace = $(if ($env:NACOS_NAMESPACE) { $env:NACOS_NAMESPACE } else { "public" })
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$configRoot = Join-Path $projectRoot "deploy\nacos"

# 登录 Nacos，后续使用 Bearer Token 发布配置。
$login = Invoke-RestMethod -Method Post `
    -Uri "$NacosUrl/nacos/v1/auth/login" `
    -Body @{ username = $Username; password = $Password } `
    -ContentType "application/x-www-form-urlencoded"
$headers = @{ Authorization = "Bearer $($login.accessToken)" }

# DataId 与本地配置文件的映射：共享配置 + Sentinel 流控/降级/网关规则。
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
    # 每成功发布一份配置就输出 DataId，现场应看到 4 行 Published。
    Write-Host "Published $($config.DataId)"
}
