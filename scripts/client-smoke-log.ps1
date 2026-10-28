function Get-1201ClientInitializationPattern {
    # Stage prose changes as the port progresses; the loader-specific entrypoint marker does not.
    return '\[Compat1201\] Initialized (?:Fabric|Forge)\b'
}

function Remove-Known1201OfflineAuthenticationError {
    param([string]$LogText)
    # Vanilla 1.20.1's offline test user (dev or installed runtime) produces this exact authlib 401 pair.
    # Do not suppress other statuses, exceptions, threads or mod failures.
    return $LogText -replace '(?m)^[^\r\n]*\[Render thread/ERROR\][^\r\n]*Failed to verify authentication\r?\n(?=com\.mojang\.authlib\.exceptions\.InvalidCredentialsException: Status: 401(?:\r?\n|$))', ''
}

function Remove-KnownPetHome1201ClientDataFixerErrors {
    param([string]$LogText, [string]$VerifiedArtifactSha256)
    # Pinned official Fabric 1.1.4 only. The production launcher already verifies
    # each installed artifact hash. Unknown versions/messages remain failures.
    if ($VerifiedArtifactSha256 -cne '545828042d151f8665bbd975832768ba458f18809428728514630e3c081c1ec2') {
        return $LogText
    }
    return $LogText -replace '(?m)^\[\d{2}:\d{2}:\d{2}\] \[Render thread/ERROR\]: No data fixer registered for (?:chain_lightning|recall_ball|feather|giant_bubble|following_jukebox|highlighted_block|psychic_wall|net)(?:\r?\n|$)', ''
}

function Remove-KnownPetHome1211ClientDataFixerErrors {
    param([string]$LogText, [string]$VerifiedArtifactSha256)
    # Official Fabric 1.1.4: seven exact registration diagnostics observed in the
    # production client. Keep the separate 1.20.1 eight-entity allowlist unchanged.
    if ($VerifiedArtifactSha256 -cne 'e42894ad07d7417637cf83858fa64a4854c9143f3c34618c9caba2792e98c45e') {
        return $LogText
    }
    return $LogText -replace '(?m)^\[\d{2}:\d{2}:\d{2}\] \[Render thread/ERROR\]: No data fixer registered for (?:chain_lightning|recall_ball|feather|giant_bubble|highlighted_block|psychic_wall|net)(?:\r?\n|$)', ''
}
