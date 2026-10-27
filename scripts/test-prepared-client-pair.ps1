$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'prepared-client-pair.ps1')
$root = Join-Path (Split-Path -Parent $PSScriptRoot) ('build\client-pair-check-' + [guid]::NewGuid().ToString('N'))
$caseRoot = Join-Path $root 'run'
New-Item -ItemType Directory -Path $caseRoot -Force | Out-Null
$launches = @()
foreach ($name in @('Echo1211Fabric','Echo1211Neo')) {
    $directory = Join-Path $caseRoot $name
    New-Item -ItemType Directory -Path $directory | Out-Null
    $arguments = Join-Path $directory 'client-arguments.txt'
    [IO.File]::WriteAllText($arguments, '"--quickPlayMultiplayer" "127.0.0.1:25671"')
    $descriptor = Join-Path $directory 'launch.json'
    $launch = @{production=$true; minecraft='1.21.1'; username=$name; multiplayer_server='127.0.0.1:25671';
        argument_file=$arguments; argument_sha256=(Get-FileHash -LiteralPath $arguments).Hash}
    $launch | ConvertTo-Json | Set-Content -LiteralPath $descriptor -Encoding UTF8
    $launches += @{path=$descriptor; sha256=(Get-FileHash -LiteralPath $descriptor).Hash}
}
$manifest = @{run_root=$caseRoot; minecraft='1.21.1'; max_clients=2; server='127.0.0.1:25671'; launches=$launches}
$manifestPath = Join-Path $caseRoot 'pair.json'
function Save-Manifest { $manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding UTF8 }
function Read-Pair {
    Get-PreparedClientPair -ManifestPath $manifestPath -DescriptorPath $launches[0].path `
        -MinecraftVersion '1.21.1' -ComputerName 'FOXY-NODE' -AllowedBase $root
}
$checks = 0
function Reject([scriptblock]$Action) {
    $rejected = $false
    try { & $Action | Out-Null } catch { $rejected = $true }
    if (-not $rejected) { throw 'Unsafe paired-client input was accepted' }
    $script:checks++
}
Save-Manifest
$peer = @(Read-Pair)
if ($peer.Count -ne 1 -or -not $peer[0].Contains('Echo1211Neo')) { throw 'Valid peer was not selected' }
$checks++
if (-not (Test-PreparedPeerProcess ('java.exe "@' + $peer[0] + '"') $peer)) { throw 'Quoted owned arguments not recognized' }
if (Test-PreparedPeerProcess ('java.exe "@' + $peer[0] + '.foreign"') $peer) { throw 'Argument prefix collision accepted' }
if (Test-PreparedPeerProcess 'java.exe @D:\unrelated\client-arguments.txt' $peer) { throw 'Unrelated client accepted' }
$checks += 3
Reject { Get-PreparedClientPair -ManifestPath $manifestPath -DescriptorPath $launches[0].path -MinecraftVersion '1.21.1' -ComputerName 'OTHER' -AllowedBase $root }
$manifest.server='192.168.1.10:25671'; Save-Manifest; Reject { Read-Pair }
$manifest.server='127.0.0.1:25671'
$manifest.max_clients=3; Save-Manifest; Reject { Read-Pair }
$manifest.max_clients=2
$manifest.minecraft='26.1.2'; Save-Manifest; Reject { Read-Pair }
$manifest.minecraft='1.21.1'
$originalHash=$launches[1].sha256
$launches[1].sha256='0'*64; Save-Manifest; Reject { Read-Pair }
$launches[1].sha256=$originalHash
Save-Manifest
Add-Content -LiteralPath $peer[0] -Value 'changed'
Reject { Read-Pair }
Write-Host "PASS: $checks paired-client ownership/identity/hash/endpoint checks."
