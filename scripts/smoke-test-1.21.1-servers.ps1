[CmdletBinding()]
param(
    [ValidateSet('Dual', 'Fabric', 'NeoForge')]
    [string]$Loader = 'Dual',

    [ValidateRange(30, 600)]
    [int]$TimeoutSeconds = 180,

    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$compatibilityRoot = Join-Path $repositoryRoot 'versions\1.21.1'
$gradleWrapper = Join-Path $repositoryRoot 'gradlew.bat'

$javaCandidates = @(
    @(
        $env:ECHO_WARRIOR_JAVA21_HOME,
        (Join-Path $repositoryRoot '.toolchains\jdk-21'),
        'C:\Program Files\Java\jdk-21',
        'C:\Program Files\Java\jdk-21.0.10',
        'C:\Program Files\Eclipse Adoptium\jdk-21'
    ) | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe')) }
)

if ($javaCandidates.Count -eq 0) {
    throw 'Java 21 was not found. Set ECHO_WARRIOR_JAVA21_HOME to a JDK 21 directory.'
}

$javaHome = (Resolve-Path -LiteralPath $javaCandidates[0]).Path
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path

function Get-FreeTcpPort {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    try {
        $listener.Start()
        return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
    }
    finally {
        $listener.Stop()
    }
}

function Get-DescendantProcessIds {
    param([int]$RootProcessId)

    $all = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)
    $known = [System.Collections.Generic.HashSet[int]]::new()
    [void]$known.Add($RootProcessId)
    $changed = $true
    while ($changed) {
        $changed = $false
        foreach ($candidate in $all) {
            $candidateId = [int]$candidate.ProcessId
            if ($known.Contains($candidateId)) { continue }
            if ($known.Contains([int]$candidate.ParentProcessId)) {
                [void]$known.Add($candidateId)
                $changed = $true
            }
        }
    }
    return @($known | Where-Object { $_ -ne $RootProcessId })
}

function Send-RconPacket {
    param(
        [System.IO.Stream]$Stream,
        [int]$RequestId,
        [int]$PacketType,
        [string]$Payload
    )

    $payloadBytes = [System.Text.Encoding]::UTF8.GetBytes($Payload)
    $packetBuffer = [System.IO.MemoryStream]::new()
    $writer = [System.IO.BinaryWriter]::new($packetBuffer, [System.Text.Encoding]::UTF8, $true)
    try {
        $writer.Write([int](4 + 4 + $payloadBytes.Length + 2))
        $writer.Write($RequestId)
        $writer.Write($PacketType)
        $writer.Write($payloadBytes)
        $writer.Write([byte]0)
        $writer.Write([byte]0)
        $writer.Flush()

        # Minecraft 1.21.1's RCON implementation expects one complete packet
        # from each socket read. Assemble the bytes first and submit them with
        # one network write so NeoForge does not observe a partial header.
        $packetBytes = $packetBuffer.ToArray()
        $Stream.Write($packetBytes, 0, $packetBytes.Length)
        $Stream.Flush()
    }
    finally {
        $writer.Dispose()
        $packetBuffer.Dispose()
    }
}

function Receive-RconPacket {
    param([System.IO.Stream]$Stream)

    $reader = [System.IO.BinaryReader]::new($Stream, [System.Text.Encoding]::UTF8, $true)
    try {
        $length = $reader.ReadInt32()
        if ($length -lt 10 -or $length -gt 4MB) {
            throw "Invalid RCON packet length: $length"
        }
        $requestId = $reader.ReadInt32()
        $packetType = $reader.ReadInt32()
        $payloadLength = $length - 10
        $payload = [System.Text.Encoding]::UTF8.GetString($reader.ReadBytes($payloadLength))
        [void]$reader.ReadByte()
        [void]$reader.ReadByte()
        return [pscustomobject]@{
            RequestId = $requestId
            PacketType = $packetType
            Payload = $payload
        }
    }
    finally {
        $reader.Dispose()
    }
}

function Invoke-RconCommand {
    param(
        [int]$Port,
        [string]$Password,
        [string]$Command
    )

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $client.Connect([System.Net.IPAddress]::Loopback, $Port)
        $stream = $client.GetStream()
        $stream.ReadTimeout = 5000
        $stream.WriteTimeout = 5000

        $requestId = 1211
        Send-RconPacket $stream $requestId 3 $Password
        $response = Receive-RconPacket $stream
        if ($response.RequestId -ne $requestId) {
            throw 'RCON authentication failed.'
        }

        $commandRequestId = $requestId + 1
        Send-RconPacket $stream $commandRequestId 2 $Command
        $commandResponse = Receive-RconPacket $stream
        if ($commandResponse.RequestId -ne $commandRequestId) {
            throw "RCON command '$Command' returned an unexpected request id."
        }
        return $commandResponse.Payload
    }
    finally {
        $client.Dispose()
    }
}

function Stop-ServerWithRcon {
    param(
        [int]$Port,
        [string]$Password
    )

    $deadline = [DateTime]::UtcNow.AddSeconds(10)
    $lastError = $null
    while ([DateTime]::UtcNow -lt $deadline) {
        $client = [System.Net.Sockets.TcpClient]::new()
        try {
            $client.Connect([System.Net.IPAddress]::Loopback, $Port)
            $stream = $client.GetStream()
            $stream.ReadTimeout = 5000
            $stream.WriteTimeout = 5000

            $requestId = 1211
            Send-RconPacket $stream $requestId 3 $Password
            $response = Receive-RconPacket $stream
            if ($response.RequestId -ne $requestId) {
                throw 'RCON authentication failed.'
            }

            $commandRequestId = $requestId + 1
            Send-RconPacket $stream $commandRequestId 2 'stop'
            try {
                $commandResponse = Receive-RconPacket $stream
            }
            catch {
                # NeoForge can close the RCON socket immediately after accepting
                # the stop command. Process exit and shutdown-log assertions below
                # still prove that the command was handled successfully.
                return
            }
            if ($commandResponse.RequestId -ne $commandRequestId) {
                throw 'RCON stop command returned an unexpected request id.'
            }
            return
        }
        catch {
            $lastError = $_
            Start-Sleep -Milliseconds 250
        }
        finally {
            $client.Dispose()
        }
    }

    throw "Could not stop the server through RCON on port $Port. $lastError"
}

function Assert-NoExistingServer {
    param(
        [string]$RunDirectory,
        [string]$Task
    )

    $escapedCompatibilityRoot = [regex]::Escape($compatibilityRoot)
    $escapedTask = [regex]::Escape($Task)
    $existing = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -match '^javaw?\.exe$' -and
            $_.CommandLine -match $escapedCompatibilityRoot -and
            ($_.CommandLine -match $escapedTask -or
                $_.CommandLine -match 'runServer|KnotServer|net\.minecraft\.server\.Main')
        } |
        Select-Object -First 1

    if ($existing) {
        throw "A Minecraft test server is already using $RunDirectory (PID $($existing.ProcessId))."
    }
}

function Invoke-ServerSmokeTest {
    param(
        [ValidateSet('Fabric', 'NeoForge')]
        [string]$TargetLoader
    )

    $loaderKey = $TargetLoader.ToLowerInvariant()
    if ($TargetLoader -eq 'Fabric') {
        $task = ':fabric:runServer'
        $runDirectory = Join-Path $compatibilityRoot 'run-fabric-server'
    }
    else {
        $task = ':neoforge:runServer'
        $runDirectory = Join-Path $compatibilityRoot 'run-neoforge-server'
    }

    Assert-NoExistingServer $runDirectory $task
    New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null

    $serverPort = Get-FreeTcpPort
    do {
        $rconPort = Get-FreeTcpPort
    } while ($rconPort -eq $serverPort)

    $rconPassword = "echo-warrior-$loaderKey-$([guid]::NewGuid().ToString('N'))"
    $worldName = "AUTOTEST_1_21_1_$($TargetLoader.ToUpperInvariant())"
    $properties = @(
        'enable-command-block=false'
        'enable-query=false'
        'enable-rcon=true'
        'enforce-secure-profile=false'
        'level-name=' + $worldName
        'max-players=1'
        'motd=Echo Warrior 1.21.1 automated smoke test'
        'online-mode=false'
        'rcon.password=' + $rconPassword
        'rcon.port=' + $rconPort
        'server-ip=127.0.0.1'
        'server-port=' + $serverPort
        'simulation-distance=2'
        'spawn-protection=0'
        'sync-chunk-writes=true'
        'view-distance=2'
    )
    Set-Content -LiteralPath (Join-Path $runDirectory 'server.properties') -Value $properties -Encoding ASCII
    Set-Content -LiteralPath (Join-Path $runDirectory 'eula.txt') -Value 'eula=true' -Encoding ASCII

    $stdoutPath = Join-Path $runDirectory 'automated-smoke-gradle.out.log'
    $stderrPath = Join-Path $runDirectory 'automated-smoke-gradle.err.log'
    $latestLog = Join-Path $runDirectory 'logs\latest.log'

    if (Test-Path -LiteralPath $latestLog) {
        Remove-Item -LiteralPath $latestLog -Force
    }

    $process = $null
    try {
        Write-Host "Starting $TargetLoader Minecraft 1.21.1 dedicated server smoke test..."
        $process = Start-Process -FilePath $gradleWrapper `
            -ArgumentList @('-p', $compatibilityRoot, $task, '--console=plain', '--no-daemon') `
            -WorkingDirectory $repositoryRoot `
            -WindowStyle Hidden `
            -RedirectStandardOutput $stdoutPath `
            -RedirectStandardError $stderrPath `
            -PassThru

        $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
        $started = $false
        while ([DateTime]::UtcNow -lt $deadline) {
            $process.Refresh()
            if ($process.HasExited) {
                break
            }

            if (Test-Path -LiteralPath $latestLog) {
                $logText = Get-Content -LiteralPath $latestLog -Raw -ErrorAction SilentlyContinue
                if ($logText -match 'Failed to start the minecraft server' -or
                    $logText -match '\[(?:[^]]+/)?FATAL\]') {
                    throw "$TargetLoader server reported a fatal startup failure."
                }
                if ($logText -match 'Initializing Echo Warrior .*Minecraft 1\.21\.1' -and
                    $logText -match 'Done \([0-9.]+s\)!') {
                    $started = $true
                    break
                }
            }
            Start-Sleep -Milliseconds 500
        }

        if (-not $started) {
            $stdoutTail = if (Test-Path -LiteralPath $stdoutPath) { (Get-Content -LiteralPath $stdoutPath -Tail 40) -join [Environment]::NewLine } else { '' }
            $stderrTail = if (Test-Path -LiteralPath $stderrPath) { (Get-Content -LiteralPath $stderrPath -Tail 40) -join [Environment]::NewLine } else { '' }
            throw "$TargetLoader server did not reach the ready state within $TimeoutSeconds seconds.`nSTDOUT:`n$stdoutTail`nSTDERR:`n$stderrTail"
        }

        $selfTestResult = Invoke-RconCommand $rconPort $rconPassword 'echo_warrior_compat selftest'
        if ($selfTestResult -notmatch 'ECHO_WARRIOR_1_21_1_SELFTEST PASS') {
            throw "$TargetLoader compatibility self-test failed: $selfTestResult"
        }
        Write-Host "$TargetLoader five-hero registry, damage tags, SavedData authority, full entity snapshot, relic/accessory persistence, duplicate summoner/accessory rejection, modes, skills, growth, combat, visual math, projectile rehoming, creeper control, attributes, and entity save checks passed."

        Stop-ServerWithRcon $rconPort $rconPassword
        if (-not $process.WaitForExit(60000)) {
            throw "$TargetLoader Gradle server task did not exit within 60 seconds after the RCON stop command."
        }
        $process.WaitForExit()
        $process.Refresh()
        $serverExitCode = $process.ExitCode
        if ($null -ne $serverExitCode -and $serverExitCode -ne 0) {
            throw "$TargetLoader Gradle server task exited with code $serverExitCode."
        }
        if ($null -eq $serverExitCode) {
            $gradleOutput = if (Test-Path -LiteralPath $stdoutPath) {
                Get-Content -LiteralPath $stdoutPath -Raw
            } else { '' }
            if ($gradleOutput -notmatch 'BUILD SUCCESSFUL' -or $gradleOutput -match 'BUILD FAILED') {
                throw "$TargetLoader Gradle server task did not expose an exit code or a successful build result."
            }
        }

        $finalLog = Get-Content -LiteralPath $latestLog -Raw
        if ($finalLog -notmatch 'Stopping server' -or $finalLog -notmatch 'Saving worlds') {
            throw "$TargetLoader server did not record a graceful shutdown."
        }
        $unexpectedErrorLog = $finalLog -replace `
            '(?m)^.*No data fixer registered for echo_warrior:(?:roman_legionary_echo|aztec_warrior_echo|guandao_warrior_echo|japanese_samurai_echo|egyptian_archer_echo|egyptian_archer_arrow)\r?\n?', ''
        # Offline smoke tests do not depend on Mojang's rotating public keys.
        # NeoForge may finish the graceful shutdown before authlib's background
        # fetch times out, so ignore only this exact external-network error line.
        $unexpectedErrorLog = $unexpectedErrorLog -replace `
            '(?m)^.*\[Yggdrasil Key Fetcher/ERROR\].*Failed to request yggdrasil public key\r?\n?', ''
        if ($unexpectedErrorLog -match '\[(?:[^]]+/)?(?:ERROR|FATAL)\]') {
            throw "$TargetLoader server log contains an ERROR or FATAL entry."
        }

        Write-Host "$TargetLoader Minecraft 1.21.1 server startup and graceful shutdown passed."
    }
    finally {
        if ($process) {
            $process.Refresh()
            if (-not $process.HasExited) {
                $descendants = @(Get-DescendantProcessIds $process.Id)
                foreach ($processId in $descendants | Sort-Object -Descending) {
                    Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
                }
                Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
                [void]$process.WaitForExit(20000)
            }
            $process.Dispose()
        }
    }
}

try {
    $env:JAVA_HOME = $javaHome
    $env:Path = "$(Join-Path $javaHome 'bin');$previousPath"

    if (-not $SkipBuild) {
        & (Join-Path $PSScriptRoot 'check-1.21.1-baseline.ps1')
        if ($LASTEXITCODE -ne 0) {
            throw "Minecraft 1.21.1 baseline validation failed with exit code $LASTEXITCODE."
        }
    }

    if ($Loader -in @('Dual', 'Fabric')) {
        Invoke-ServerSmokeTest Fabric
    }
    if ($Loader -in @('Dual', 'NeoForge')) {
        Invoke-ServerSmokeTest NeoForge
    }
}
finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
}

Write-Host 'Minecraft 1.21.1 dedicated-server smoke test passed for every requested loader.'
