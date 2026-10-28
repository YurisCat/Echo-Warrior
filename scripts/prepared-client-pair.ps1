function Get-PreparedClientPair {
    param(
        [string]$ManifestPath,
        [string]$DescriptorPath,
        [string]$MinecraftVersion,
        [string]$ComputerName = $env:COMPUTERNAME,
        [string]$AllowedBase = 'D:\Games-Terminal\EchoWarrior'
    )
    if ($ComputerName -ne 'FOXY-NODE') { throw 'Paired acceptance clients belong on FOXY-NODE.' }
    $manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
    $root = (Resolve-Path -LiteralPath $manifest.run_root).Path.TrimEnd('\')
    $allowedRoot = $AllowedBase.TrimEnd('\') + '\'
    if (-not $root.StartsWith($allowedRoot, [StringComparison]::OrdinalIgnoreCase) -or
            $manifest.minecraft -ne $MinecraftVersion -or $manifest.max_clients -ne 2 -or
            @($manifest.launches).Count -ne 2 -or $manifest.server -notmatch '^127\.0\.0\.1:[0-9]{4,5}$') {
        throw 'Invalid isolated two-client acceptance manifest.'
    }
    $current = (Resolve-Path -LiteralPath $DescriptorPath).Path
    $seen = @{}
    $arguments = @()
    $descriptors = @()
    foreach ($entry in $manifest.launches) {
        $path = (Resolve-Path -LiteralPath $entry.path).Path
        if (-not $path.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase) -or
                (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash -ne $entry.sha256) {
            throw 'Paired client descriptor is outside the test run or changed.'
        }
        $launch = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
        $argumentPath = (Resolve-Path -LiteralPath $launch.argument_file).Path
        if (-not $launch.production -or $launch.minecraft -ne $MinecraftVersion -or
                $launch.multiplayer_server -ne $manifest.server -or
                (Split-Path -Parent $argumentPath) -ne (Split-Path -Parent $path) -or
                (Get-FileHash -LiteralPath $argumentPath -Algorithm SHA256).Hash -ne $launch.argument_sha256 -or
                $seen.ContainsKey([string]$launch.username)) {
            throw 'Paired client arguments, version, endpoint or distinct identity are invalid.'
        }
        $raw = Get-Content -LiteralPath $argumentPath -Raw
        if (-not $raw.Contains('--quickPlayMultiplayer') -or -not $raw.Contains([string]$manifest.server) -or
                $raw.Contains('--quickPlaySingleplayer')) { throw 'Paired clients must quick-play the isolated loopback server.' }
        $seen[[string]$launch.username] = $true
        $arguments += $argumentPath
        $descriptors += $path
    }
    if ($current -notin $descriptors -or @($arguments | Select-Object -Unique).Count -ne 2) {
        throw 'Current launch is not one of two distinct registered clients.'
    }
    return @($arguments | Where-Object { $_ -ne
        (Get-Content -LiteralPath $current -Raw | ConvertFrom-Json).argument_file })
}

function Test-PreparedPeerProcess {
    param([string]$CommandLine, [string[]]$PeerArguments)
    foreach ($path in $PeerArguments) {
        if ($CommandLine -match ('@["]?' + [regex]::Escape($path) + '(?=["\s]|$)')) { return $true }
    }
    return $false
}
