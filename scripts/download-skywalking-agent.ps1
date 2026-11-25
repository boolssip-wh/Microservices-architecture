param(
    [string]$Version = "9.4.0",
    [string]$Destination
)

$ErrorActionPreference = "Stop"
$toolsRoot = if ($env:SMARTGRADE_TOOLS) { $env:SMARTGRADE_TOOLS } else { "D:\SmartGradeTools" }
if (-not $Destination) {
    $Destination = Join-Path $toolsRoot "skywalking-agent"
}
$Destination = [IO.Path]::GetFullPath($Destination)
$jarPath = Join-Path $Destination "skywalking-agent.jar"
if (Test-Path -LiteralPath $jarPath) {
    Write-Host "SkyWalking agent already exists: $jarPath"
    exit 0
}

$archiveName = "apache-skywalking-java-agent-$Version.tgz"
$url = "https://archive.apache.org/dist/skywalking/java-agent/$Version/$archiveName"
$downloadRoot = Join-Path ([IO.Path]::GetTempPath()) ("smartgrade-skywalking-" + [guid]::NewGuid().ToString("N"))
$archive = Join-Path $downloadRoot $archiveName
$extractRoot = Join-Path $downloadRoot "extract"

New-Item -ItemType Directory -Force -Path $downloadRoot, $extractRoot | Out-Null
try {
    Write-Host "Downloading SkyWalking Java Agent $Version..."
    Invoke-WebRequest -Uri $url -OutFile $archive
    tar -xzf $archive -C $extractRoot
    $source = Get-ChildItem -LiteralPath $extractRoot -Recurse -File -Filter "skywalking-agent.jar" |
        Select-Object -First 1
    if (-not $source) {
        throw "skywalking-agent.jar was not found in the downloaded archive."
    }
    New-Item -ItemType Directory -Force -Path $Destination | Out-Null
    Copy-Item -LiteralPath $source.Directory.FullName -Destination $Destination -Recurse -Force
    Write-Host "SkyWalking agent installed: $(Join-Path $Destination 'skywalking-agent.jar')"
} finally {
    if (Test-Path -LiteralPath $downloadRoot) {
        $resolved = (Resolve-Path -LiteralPath $downloadRoot).Path
        $tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
        if ($resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) {
            Remove-Item -LiteralPath $resolved -Recurse -Force
        }
    }
}
