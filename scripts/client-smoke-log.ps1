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
