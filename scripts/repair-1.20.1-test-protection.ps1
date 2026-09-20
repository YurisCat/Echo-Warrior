[CmdletBinding()]
param([switch]$Apply)
$ErrorActionPreference = 'Stop'
# One-time recovery of a test-only flag saved by the first experimental protection fixture.
# This deliberately targets ONLY the known Forge development test world/player, never arbitrary saves.
$repository = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$testWorld = Join-Path $repository 'versions\1.20.1\run-forge\saves\CATTEST'
$marker = Get-Content -LiteralPath (Join-Path $testWorld 'echo-warrior-smoke-world.json') -Raw | ConvertFrom-Json
if ($marker.minecraft -ne '1.20.1' -or $marker.loader -ne 'forge') { throw 'Unrecognized test world' }
$running = @(Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match '^javaw?\.exe$' -and $_.CommandLine -match 'run-forge|:forge:runClient'
})
if ($running.Count) { throw 'Close the Forge test client before inspecting save files' }
$targets = @('level.dat', 'playerdata\1a01f11e-3cd6-3164-a222-39a5e2d42147.dat')
$tagName = [System.Text.Encoding]::UTF8.GetBytes('Invulnerable')
[byte[]]$prefix = @(1, 0, $tagName.Length) + $tagName
$backup = Join-Path $repository ('build\test-fixture-recovery\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
foreach ($relative in $targets) {
    $target = [System.IO.Path]::GetFullPath((Join-Path $testWorld $relative))
    if (!$target.StartsWith($testWorld + '\', [System.StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe target' }
    $inputFile = [System.IO.File]::OpenRead($target)
    $gzip = [System.IO.Compression.GZipStream]::new($inputFile, [System.IO.Compression.CompressionMode]::Decompress)
    $decoded = [System.IO.MemoryStream]::new()
    try { $gzip.CopyTo($decoded); $bytes = $decoded.ToArray() } finally { $gzip.Dispose(); $inputFile.Dispose(); $decoded.Dispose() }
    $matchesAt = @()
    for ($position = 0; $position -le $bytes.Length - $prefix.Length - 1; $position++) {
        $equal = $true
        for ($offset = 0; $offset -lt $prefix.Length; $offset++) {
            if ($bytes[$position + $offset] -ne $prefix[$offset]) { $equal = $false; break }
        }
        if ($equal) { $matchesAt += $position + $prefix.Length }
    }
    if ($matchesAt.Count -ne 1 -or $bytes[$matchesAt[0]] -notin @(0, 1)) { throw "Ambiguous NBT flag: $relative" }
    Write-Host "$relative : Invulnerable=$($bytes[$matchesAt[0]])"
    if (!$Apply -or $bytes[$matchesAt[0]] -eq 0) { continue }
    New-Item -ItemType Directory -Path $backup -Force | Out-Null
    Copy-Item -LiteralPath $target -Destination (Join-Path $backup ($relative.Replace('\', '_') + '.before'))
    $bytes[$matchesAt[0]] = 0
    $outputFile = [System.IO.File]::Create($target + '.fixture-repair.tmp')
    $encoded = [System.IO.Compression.GZipStream]::new($outputFile, [System.IO.Compression.CompressionMode]::Compress)
    try { $encoded.Write($bytes, 0, $bytes.Length) } finally { $encoded.Dispose(); $outputFile.Dispose() }
    Move-Item -LiteralPath ($target + '.fixture-repair.tmp') -Destination $target -Force
    Write-Host "Restored the test-generated flag to false; original retained in $backup"
}
