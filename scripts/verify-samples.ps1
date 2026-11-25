$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$sampleRoot = Join-Path $projectRoot "samples"
$manifestPath = (Get-ChildItem -LiteralPath $sampleRoot -File -Filter "*.json" |
    Where-Object Name -ne "demo-rubric.json" |
    Select-Object -First 1 -ExpandProperty FullName)
if (-not $manifestPath) {
    throw "Sample manifest JSON was not found."
}
$manifest = Get-Content -Raw -LiteralPath $manifestPath -Encoding UTF8 | ConvertFrom-Json

if (@($manifest.answers).Count -ne 10) {
    throw "Expected 10 answer sheets, got $(@($manifest.answers).Count)."
}
$actualFour = @($manifest.answers | Where-Object contentHitCount -eq 4).Count
$actualThree = @($manifest.answers | Where-Object contentHitCount -eq 3).Count
$actualZero = @($manifest.answers | Where-Object contentHitCount -eq 0).Count
if ($actualFour -ne 2 -or $actualThree -ne 7 -or $actualZero -ne 1) {
    throw "Unexpected hit distribution: four=$actualFour three=$actualThree zero=$actualZero"
}

Add-Type -AssemblyName System.Drawing
foreach ($answer in $manifest.answers) {
    $path = Join-Path $sampleRoot $answer.file
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Missing sample: $path"
    }
    $image = [System.Drawing.Image]::FromFile($path)
    try {
        if ($image.Width -ne 1721 -or $image.Height -ne 2435) {
            throw "$($answer.file) is $($image.Width)x$($image.Height), expected 1721x2435"
        }
    } finally {
        $image.Dispose()
    }
}

Write-Host "PASS: 10 samples, 1721x2435, distribution 20% four-point / 70% three-point / 10% zero-point."
