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
Write-Host 'Client smoke log classification: 10 checks passed.'
