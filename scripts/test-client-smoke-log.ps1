$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'client-smoke-log.ps1')
$expected = "[12:00:00] [Render thread/ERROR] (Minecraft) Failed to verify authentication`ncom.mojang.authlib.exceptions.InvalidCredentialsException: Status: 401`n"
if ((Remove-Known1201OfflineAuthenticationError $expected) -match '/ERROR\]') {
    throw 'Expected development-user 401 was not classified.'
}
foreach ($unexpected in @(
    ($expected -replace 'Status: 401', 'Status: 500'),
    ($expected -replace 'InvalidCredentialsException', 'OtherException'),
    ($expected -replace 'Render thread', 'Server thread'),
    ($expected + '[12:00:01] [Render thread/ERROR] (echo_warrior) Model load failed')
)) {
    if ((Remove-Known1201OfflineAuthenticationError $unexpected) -notmatch '/ERROR\]') {
        throw 'Unexpected client error was incorrectly suppressed.'
    }
}
foreach ($stage in @('bootstrap', 'internal compatibility build; full-port acceptance is pending')) {
    foreach ($loader in @('Fabric', 'Forge')) {
        if ("[Compat1201] Initialized $loader $stage" -notmatch (Get-1201ClientInitializationPattern)) {
            throw 'Changing stage prose broke initialization detection.'
        }
    }
}
if ('[Compat1211] Initialized NeoForge bootstrap' -match (Get-1201ClientInitializationPattern)) {
    throw 'A different version/loader was accepted.'
}
$petHash = '545828042d151f8665bbd975832768ba458f18809428728514630e3c081c1ec2'
foreach ($entity in @('chain_lightning','recall_ball','feather','giant_bubble','following_jukebox','highlighted_block','psychic_wall','net')) {
    $line = "[12:00:00] [Render thread/ERROR]: No data fixer registered for $entity`n"
    if (Remove-KnownPetHome1201ClientDataFixerErrors $line $petHash) { throw 'Known pinned diagnostic was not classified.' }
    if ((Remove-KnownPetHome1201ClientDataFixerErrors $line 'unknown') -ne $line) { throw 'Unknown artifact was suppressed.' }
}
foreach ($line in @(
    '[12:00:00] [Render thread/ERROR]: No data fixer registered for network',
    '[12:00:00] [Render thread/ERROR]: No data fixer registered for net extra failure',
    '[12:00:00] [Server thread/ERROR]: No data fixer registered for net',
    '[12:00:00] [Render thread/FATAL]: No data fixer registered for net',
    '[12:00:00] [Render thread/ERROR]: Pet Home entity failure'
)) {
    if ((Remove-KnownPetHome1201ClientDataFixerErrors $line $petHash) -ne $line) {
        throw 'Unreviewed client diagnostic was suppressed.'
    }
}
Write-Host 'Client smoke log classification: 31 checks passed.'
$pet1211Hash = 'e42894ad07d7417637cf83858fa64a4854c9143f3c34618c9caba2792e98c45e'
foreach ($entity in @('chain_lightning','recall_ball','feather','giant_bubble','highlighted_block','psychic_wall','net')) {
    $line = "[12:00:00] [Render thread/ERROR]: No data fixer registered for $entity`n"
    if (Remove-KnownPetHome1211ClientDataFixerErrors $line $pet1211Hash) { throw 'Pinned 1.21.1 diagnostic not classified.' }
    if ((Remove-KnownPetHome1211ClientDataFixerErrors $line $petHash) -ne $line) { throw 'Wrong version hash accepted.' }
}
foreach ($line in @(
    '[12:00:00] [Render thread/ERROR]: No data fixer registered for following_jukebox',
    '[12:00:00] [Render thread/ERROR]: No data fixer registered for net extra',
    '[12:00:00] [Server thread/ERROR]: No data fixer registered for net',
    '[12:00:00] [Render thread/FATAL]: No data fixer registered for net',
    '[12:00:00] [Render thread/ERROR]: Pet Home registration failed'
)) {
    if ((Remove-KnownPetHome1211ClientDataFixerErrors $line $pet1211Hash) -ne $line) {
        throw 'Unreviewed 1.21.1 diagnostic suppressed.'
    }
}
Write-Host 'Pinned 1.21.1 client log classification: 19 additional checks passed.'
