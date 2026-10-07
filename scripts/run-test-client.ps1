[CmdletBinding()]
param(
    [string]$TestWorldName = 'CATTEST',
    [ValidateSet('Current', '1.21.1', '1.20.1')]
    [string]$TargetVersion = 'Current',
    [ValidateSet('Fabric', 'NeoForge', 'Forge')]
    [string]$Loader = 'Fabric',
    [switch]$RequireExistingWorld,
    [switch]$StartupOnly,
    [switch]$PauseOnJoin,
    [switch]$Production,
    [ValidateRange(30, 300)]
    [int]$TimeoutSeconds = 120
)

$ErrorActionPreference = 'Stop'
if ($Production -and ($TargetVersion -ne '1.20.1' -or -not $StartupOnly -or $PauseOnJoin)) {
    throw 'Production currently requires 1.20.1 StartupOnly; it never replaces the manual development shortcut.'
}
if ($PauseOnJoin -and $TargetVersion -ne '1.20.1') {
    throw 'PauseOnJoin currently supports the isolated 1.20.1 client only.'
}
$projectRoot = Split-Path -Parent $PSScriptRoot
$focusScript = Join-Path $PSScriptRoot 'focus-test-client-window.ps1'
. (Join-Path $PSScriptRoot 'client-smoke-log.ps1')

function Get-DescendantProcessIds {
    param([int]$RootProcessId)

    $all = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)
    $known = [System.Collections.Generic.HashSet[int]]::new()
    [void]$known.Add($RootProcessId)
    $changed = $true
    while ($changed) {
        $changed = $false
        foreach ($process in $all) {
            $processId = [int]$process.ProcessId
            if ($known.Contains($processId)) { continue }
            if ($known.Contains([int]$process.ParentProcessId)) {
                [void]$known.Add($processId)
                $changed = $true
            }
        }
    }
    return @($known | Where-Object { $_ -ne $RootProcessId })
}

if (($TargetVersion -eq '1.20.1' -and $Loader -eq 'NeoForge') -or
    ($TargetVersion -ne '1.20.1' -and $Loader -eq 'Forge')) {
    throw "Unsupported loader/version pair: $TargetVersion $Loader. 1.20.1 uses Fabric or Forge."
}

if ($TargetVersion -in @('1.21.1', '1.20.1')) {
    $buildRoot = Join-Path $projectRoot "versions\$TargetVersion"
    $javaCandidates = @(
        @(
            $env:ECHO_WARRIOR_JAVA21_HOME,
            (Join-Path $projectRoot '.toolchains\jdk-21'),
            'C:\Program Files\Java\jdk-21',
            'C:\Program Files\Java\jdk-21.0.10',
            'C:\Program Files\Eclipse Adoptium\jdk-21'
        ) | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe')) }
    )
    if ($javaCandidates.Count -eq 0) {
        throw 'Java 21 was not found. Set ECHO_WARRIOR_JAVA21_HOME to a JDK 21 directory.'
    }
    $jdkRoot = (Resolve-Path -LiteralPath $javaCandidates[0]).Path
    if ($TargetVersion -eq '1.20.1') {
        # Gradle runs on 21; the isolated build selects a Java 17 client toolchain.
        $loaderId = $Loader.ToLowerInvariant()
        $runDirectory = Join-Path $buildRoot "run-$loaderId"
        $clientProjectDirectory = Join-Path $buildRoot $loaderId
        $task = ":${loaderId}:runClient"
        $quickPlayProperty = if ($Loader -eq 'Fabric') { 'quickPlayWorld' } else { 'quickPlayWorldForge' }
        $clientUsername = "Echo1201$Loader"
    } elseif ($Loader -eq 'Fabric') {
        $runDirectory = Join-Path $buildRoot 'run-fabric'
        $clientProjectDirectory = Join-Path $buildRoot 'fabric'
        $task = ':fabric:runClient'
        $quickPlayProperty = 'quickPlayWorld'
        $clientUsername = 'Echo1211Fabric'
    } else {
        $runDirectory = Join-Path $buildRoot 'run-neoforge'
        $clientProjectDirectory = Join-Path $buildRoot 'neoforge'
        $task = ':neoforge:runClient'
        $quickPlayProperty = 'quickPlayWorldNeoForge'
        $clientUsername = 'Echo1211Neo'
    }
}
else {
    $buildRoot = $projectRoot
    $jdkRoot = Join-Path $projectRoot '.toolchains\jdk-25'
    if ($Loader -eq 'Fabric') {
        $runDirectory = Join-Path $projectRoot 'run'
        $clientProjectDirectory = Join-Path $projectRoot 'fabric'
        $task = ':fabric:runClient'
        $quickPlayProperty = 'quickPlayWorld'
        $clientUsername = 'EchoWarriorDev'
    } else {
        $runDirectory = Join-Path $projectRoot 'run-neoforge'
        $clientProjectDirectory = Join-Path $projectRoot 'neoforge'
        $task = ':neoforge:runClient'
        $quickPlayProperty = 'quickPlayWorldNeoForge'
        $clientUsername = 'EchoNeoForge'
    }
}

$clientBuildPath = Join-Path $clientProjectDirectory 'build.gradle'
$clientBuild = Get-Content -LiteralPath $clientBuildPath -Raw
$configuredUsername = [regex]::Match($clientBuild, "--username=([^']+)'")
if (-not $configuredUsername.Success -or $configuredUsername.Groups[1].Value -cne $clientUsername) {
    throw "Client username differs between the launcher and $clientBuildPath."
}
if ($clientUsername -notmatch '^[A-Za-z0-9_]{1,16}$') {
    throw "Client username '$clientUsername' violates the Minecraft login protocol limit."
}

$javaExecutable = Join-Path $jdkRoot 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw "Project Java runtime is missing: $javaExecutable"
}

$escapedRunDirectory = [regex]::Escape($runDirectory)
$escapedClientProjectDirectory = [regex]::Escape($clientProjectDirectory)
$escapedClientUsername = [regex]::Escape($clientUsername)
$clientMainPattern = 'net\.minecraft\.client\.main\.Main|net\.fabricmc\.devlaunchinjector\.Main|net\.fabricmc\.loader\.impl\.launch\.knot\.KnotClient|net\.neoforged\.devlaunch\.Main|cpw\.mods\.bootstraplauncher\.BootstrapLauncher.*(?:forgeclient|client)|gradle-wrapper\.jar.*runClient\b|client-arguments\.txt'
$allRunningClients = @(Get-CimInstance Win32_Process -ErrorAction Stop |
    Where-Object { $_.Name -match '^javaw?\.exe$' -and $_.CommandLine -match $clientMainPattern })
$runningClient = $allRunningClients |
    Where-Object {
        $_.Name -match '^javaw?\.exe$' -and
        $_.CommandLine -match $clientMainPattern -and
        ($_.CommandLine -match $escapedRunDirectory -or
            $_.CommandLine -match $escapedClientProjectDirectory -or
            $_.CommandLine -match "--username(?:=|\s)$escapedClientUsername(?:\s|$)")
    } |
    Select-Object -First 1

if ($runningClient) {
    Write-Host "Echo Warrior $TargetVersion $Loader development client is already running (PID $($runningClient.ProcessId))."
    if ($StartupOnly) { throw 'Existing client is not evidence of a new automated test; close it before running StartupOnly.' }
    if (-not $StartupOnly -and -not $PauseOnJoin) {
        & $focusScript -ProjectRoot $buildRoot -ProcessId $runningClient.ProcessId -TimeoutSeconds 10
    }
    exit 0
}
if ($allRunningClients.Count -gt 0) {
    $otherClient = $allRunningClients | Select-Object -First 1
    throw "Another Minecraft development client is already running (PID $($otherClient.ProcessId)); refusing to launch a concurrent client."
}

$productionLaunch = $null
if ($Production) {
    & python (Join-Path $PSScriptRoot 'prepare-1.20.1-production-client.py') --loader $Loader.ToLowerInvariant()
    if ($LASTEXITCODE -ne 0) { throw 'Production client preparation failed.' }
    $descriptorPath = Join-Path $projectRoot "build\compatibility-1.20.1-production-client\latest-$($Loader.ToLowerInvariant())-launch.json"
    $productionLaunch = Get-Content -LiteralPath $descriptorPath -Raw | ConvertFrom-Json
    $runDirectory = $productionLaunch.run_directory
    $escapedRunDirectory = [regex]::Escape($runDirectory)
    $escapedClientProjectDirectory = $escapedRunDirectory
    foreach ($artifact in $productionLaunch.artifacts.PSObject.Properties) {
        $actualHash = (Get-FileHash -LiteralPath (Join-Path $runDirectory "mods\$($artifact.Name)") -Algorithm SHA256).Hash
        if ($actualHash -ne $artifact.Value) { throw "Production artifact changed after staging: $($artifact.Name)" }
    }
    $otherClient = Get-CimInstance Win32_Process -ErrorAction Stop |
        Where-Object { $_.Name -match '^javaw?\.exe$' -and $_.CommandLine -match $clientMainPattern } |
        Select-Object -First 1
    if ($otherClient) { throw "A client started during preparation (PID $($otherClient.ProcessId)); refusing concurrent launch." }
}

$env:JAVA_HOME = $jdkRoot
$env:Path = "$(Join-Path $jdkRoot 'bin');$env:Path"
$worldPath = Join-Path $runDirectory "saves\$TestWorldName"
$expectsQuickPlayWorld = Test-Path -LiteralPath $worldPath
if (($StartupOnly -or $PauseOnJoin) -and $TargetVersion -in @('1.21.1', '1.20.1')) {
    New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
    $optionsPath = Join-Path $runDirectory 'options.txt'
    $optionLines = @(if (Test-Path -LiteralPath $optionsPath) {
        @(Get-Content -LiteralPath $optionsPath)
    } else {
        if ($TargetVersion -eq '1.20.1') { @('version:3465') } else { @('version:3955') }
    })
    # WinPS unwraps one output line to a string; without the outer @(), += concatenates keys.
    # Repair only the exact malformed line produced by the older internal launcher.
    if ($optionLines.Count -eq 1 -and $optionLines[0] -match '^version:(\d+)(onboardAccessibility:false)+$') {
        $optionLines = @("version:$($Matches[1])", 'onboardAccessibility:false')
    }
    $onboardingIndex = -1
    for ($index = 0; $index -lt $optionLines.Count; $index++) {
        if ($optionLines[$index] -match '^onboardAccessibility:') {
            $onboardingIndex = $index
            break
        }
    }
    if ($onboardingIndex -ge 0) {
        $optionLines[$onboardingIndex] = 'onboardAccessibility:false'
    } else {
        $optionLines += 'onboardAccessibility:false'
    }
    [System.IO.File]::WriteAllLines(
        $optionsPath,
        $optionLines,
        [System.Text.UTF8Encoding]::new($false)
    )
}
$gradleArguments = @('-p', $buildRoot, $task, '--console=plain', '--no-daemon')
if ($expectsQuickPlayWorld) {
    $gradleArguments += "-P$quickPlayProperty=$TestWorldName"
    if ($StartupOnly) {
        $gradleArguments += '-PautoPauseAfterQuickPlay=true'
    }
    if ($PauseOnJoin) {
        $gradleArguments += '-PpauseOnJoin=true'
    }
    Write-Host "Launching Echo Warrior $TargetVersion $Loader and entering $TestWorldName..."
} elseif ($RequireExistingWorld -or $PauseOnJoin) {
    throw "The requested test world does not exist: $worldPath"
} else {
    Write-Host "Launching Echo Warrior $TargetVersion $Loader to the title screen."
}

if (-not $StartupOnly) {
    if (-not $PauseOnJoin) {
        Start-Process -FilePath 'powershell.exe' `
            -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $focusScript, '-ProjectRoot', $buildRoot) `
            -WindowStyle Hidden
    }
    Push-Location $projectRoot
    try {
        & (Join-Path $projectRoot 'gradlew.bat') @gradleArguments
        exit $LASTEXITCODE
    } finally {
        Pop-Location
    }
}

New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
$latestLog = Join-Path $runDirectory 'logs\latest.log'
$stdoutPath = Join-Path $runDirectory 'automated-client-smoke-gradle.out.log'
$stderrPath = Join-Path $runDirectory 'automated-client-smoke-gradle.err.log'
$crashDirectory = Join-Path $runDirectory 'crash-reports'
$startedAt = Get-Date
$logStartLength = 0L

$gradleProcess = $null
$clientAuditPassed = $false
$launchedProcessIds = [System.Collections.Generic.HashSet[int]]::new()
$launchExecutable = Join-Path $projectRoot 'gradlew.bat'
$launchArguments = $gradleArguments
if ($Production) {
    $launchExecutable = $productionLaunch.java
    $launchArguments = @("@`"$($productionLaunch.argument_file)`"")
    Write-Host 'Testing production JARs in an isolated installed client (no development classpath).'
}
try {
    $gradleProcess = Start-Process -FilePath $launchExecutable `
        -ArgumentList $launchArguments `
        -WorkingDirectory $(if ($Production) { $runDirectory } else { $projectRoot }) `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -PassThru
    # Windows PowerShell 5.1 can lose ExitCode for a .bat child after Refresh unless its
    # process handle was opened while alive. Keep the handle; never infer success from a null code.
    $null = $gradleProcess.Handle

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $ready = $false
    while ([DateTime]::UtcNow -lt $deadline) {
        $gradleProcess.Refresh()
        foreach ($processId in Get-DescendantProcessIds $gradleProcess.Id) {
            [void]$launchedProcessIds.Add($processId)
        }
        if ($gradleProcess.HasExited) { break }
        if ($Production -and (Test-Path -LiteralPath $stderrPath)) {
            $earlyFailure = Get-Content -LiteralPath $stderrPath -Raw -ErrorAction SilentlyContinue
            if ($earlyFailure -match "Minecraft game provider couldn't locate the game|Could not find or load main class|Unrecognized option") {
                throw "Production launcher failed before Minecraft initialized: $earlyFailure"
            }
        }
        $clientProcess = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
            Where-Object {
                $_.Name -match '^javaw?\.exe$' -and
                $_.CommandLine -match $clientMainPattern -and
                ($_.CommandLine -match $escapedRunDirectory -or
                    $_.CommandLine -match $escapedClientProjectDirectory -or
                    $_.CommandLine -match "--username(?:=|\s)$escapedClientUsername(?:\s|$)")
            } |
            Select-Object -First 1
        if ((Test-Path -LiteralPath $latestLog) -and
            (Get-Item -LiteralPath $latestLog).LastWriteTime -ge $startedAt) {
            $logText = Get-Content -LiteralPath $latestLog -Raw -ErrorAction SilentlyContinue
            if ($null -eq $logText) { $logText = '' }
            if ($logText.Length -ge $logStartLength) {
                $logText = $logText.Substring([int]$logStartLength)
            }
            if ($TargetVersion -eq '1.21.1') {
				$initializationPattern = 'Initializing Echo Warrior .*Minecraft 1\.21\.1'
            } elseif ($TargetVersion -eq '1.20.1') {
                $initializationPattern = Get-1201ClientInitializationPattern
            } else {
				$initializationPattern = 'Echo Warrior is awakening\.'
            }
			$clientInitialized = $logText -match $initializationPattern -and
                $logText -match 'Backend library: LWJGL version'
            $quickPlayReady = -not $expectsQuickPlayWorld -or (
                $logText -match 'Starting integrated minecraft server version' -and
                ($logText -match 'joined the game' -or
                    $logText -match 'logged in with entity id' -or
                    $logText -match 'Local game hosted on port')
            )
			$mouseReleased = -not ($StartupOnly -and $expectsQuickPlayWorld) -or
				$logText -match 'Automated client smoke test opened the pause menu to release the mouse\.'
			if ($clientInitialized -and $quickPlayReady -and $mouseReleased) {
                Start-Sleep -Seconds 5
                foreach ($processId in Get-DescendantProcessIds $gradleProcess.Id) {
                    [void]$launchedProcessIds.Add($processId)
                }
                $ready = $true
                break
            }
        }
        Start-Sleep -Milliseconds 500
    }

    if (-not $ready) {
        $stdoutTail = if (Test-Path -LiteralPath $stdoutPath) { (Get-Content -LiteralPath $stdoutPath -Tail 50) -join [Environment]::NewLine } else { '' }
        $stderrTail = if (Test-Path -LiteralPath $stderrPath) { (Get-Content -LiteralPath $stderrPath -Tail 50) -join [Environment]::NewLine } else { '' }
        throw "$TargetVersion $Loader client did not reach the startup checkpoint.`nSTDOUT:`n$stdoutTail`nSTDERR:`n$stderrTail"
    }

    if ($TargetVersion -eq '1.20.1' -and $expectsQuickPlayWorld) {
        # This controller requests vanilla shutdown after the pause checkpoint; allow save/exit.
        $exitDeadline = $deadline
        while (-not $gradleProcess.HasExited -and [DateTime]::UtcNow -lt $exitDeadline) {
            foreach ($processId in Get-DescendantProcessIds $gradleProcess.Id) {
                [void]$launchedProcessIds.Add($processId)
            }
            Start-Sleep -Milliseconds 500
            $gradleProcess.Refresh()
        }
        if (-not $gradleProcess.HasExited -or $gradleProcess.ExitCode -ne 0) {
            throw "1.20.1 client did not exit normally after its smoke test (exited=$($gradleProcess.HasExited), exitCode=$($gradleProcess.ExitCode))."
        }
    }

    $finalLog = Get-Content -LiteralPath $latestLog -Raw
    if ($null -eq $finalLog) { $finalLog = '' }
    if ($expectsQuickPlayWorld -and $TargetVersion -in @('Current', '1.21.1') -and
        $finalLog -notmatch 'GUANDAO PRESENTATION SELFTEST PASSED processor=geckolib5? root=idle-walk packet-order=three') {
        throw "$TargetVersion Guandao animation handoff checkpoint is missing."
    }
    if ($expectsQuickPlayWorld -and $TargetVersion -eq 'Current' -and $finalLog -notmatch '\[EchoProgressionSelfTest\] PASS') {
        throw 'Mainline progression runtime checkpoint is missing.'
    }
    if ($TargetVersion -eq '1.20.1' -and $expectsQuickPlayWorld) {
        if ($finalLog -notmatch 'Client smoke test requesting normal shutdown' -or
            $finalLog -notmatch 'Summoner item model and texture resolved' -or
            $finalLog -notmatch 'All 43 registered item models, textures and names resolved' -or
            $finalLog -notmatch 'EXPLORATION CLIENT SELFTEST PASSED compass=overrides-and-tint grass=biome recycler=72-vertices-and-atlas brushing=renderer' -or
            $finalLog -notmatch 'NETWORK CLIENT SELFTEST PASSED mode=survival' -or
            $finalLog -notmatch 'NETWORK CLIENT SELFTEST PASSED mode=creative' -or
            $finalLog -notmatch 'MENU CLIENT SELFTEST PASSED mode=survival[^\r\n]*relic=preserved accessory=conserved' -or
            $finalLog -notmatch 'MENU CLIENT SELFTEST PASSED mode=creative[^\r\n]*relic=preserved accessory=conserved' -or
            $finalLog -notmatch 'COMPASS HUD SELFTEST PASSED messages=6 pulse=orange-glyphs path=loader-frame inventory=restored' -or
            $finalLog -notmatch 'INVENTORY INSERT CLIENT SELFTEST PASSED mode=creative-catalogue[^\r\n]*sound=3-resolved' -or
            $finalLog -notmatch 'INVENTORY INSERT CLIENT SELFTEST PASSED mode=creative-inventory[^\r\n]*sound=3-resolved' -or
            $finalLog -notmatch 'INVENTORY INSERT CLIENT SELFTEST PASSED mode=survival[^\r\n]*sound=3-resolved' -or
            $finalLog -notmatch 'All dimensions are saved') {
            throw '1.20.1 client shutdown/save checkpoint is missing.'
        }
        foreach ($deletionCase in @('catalogue-shift-delete', 'cursor-to-catalogue', 'trash-single',
                'trash-shift-clear', 'cursor-screen-close', 'q-drop-retained', 'inventory-shift-retained',
                'hotbar-preset-overwrite', 'outside-drop-retained', 'unproven-request-retained',
                'partial-scan-retained', 'nested-container-delete')) {
            if ($finalLog -notmatch ('CREATIVE DESTRUCTION CLIENT SELFTEST PASSED case=' + $deletionCase + ' authority=verified[^\r\n]*entities=verified')) {
                throw "1.20.1 creative destruction checkpoint is missing: $deletionCase"
            }
        }
        foreach ($heroCase in @('ROMAN_LEGIONARY', 'AZTEC_WARRIOR', 'EGYPTIAN_ARCHER', 'GUANDAO_WARRIOR', 'JAPANESE_SAMURAI')) {
            if ($finalLog -notmatch ('HERO MENU CLIENT SELFTEST PASSED hero=' + $heroCase + ' summon=real[^\r\n]*preview-clock=40-ticks-detached[^\r\n]*combat=actual-ai-hit shift-icon=gui-only')) {
                throw "1.20.1 hero menu checkpoint is missing: $heroCase"
            }
        }
        if ($finalLog -notmatch 'GUANDAO PRESENTATION SELFTEST PASSED processor=geckolib root=idle-walk packet-order=three particles=per-entity-flame-and-sparks preview=no-world-emission') {
            throw '1.20.1 Guandao animation/particle regression checkpoint is missing.'
        }
        foreach ($bookCase in @('tutorial-main', 'tutorial-offhand', 'knowledge-main', 'knowledge-offhand')) {
            if ($finalLog -notmatch ('BOOKS CLIENT SELFTEST PASSED case=' + $bookCase + ' source=server-snapshot')) {
                throw "1.20.1 book checkpoint is missing: $bookCase"
            }
        }
    }
    if ($finalLog.Length -ge $logStartLength) {
        $finalLog = $finalLog.Substring([int]$logStartLength)
    }
    $unexpectedErrorLog = $finalLog
    if ($TargetVersion -eq '1.20.1') {
        $unexpectedErrorLog = Remove-Known1201OfflineAuthenticationError $unexpectedErrorLog
    } else {
        # Preserve the existing high-version policy; the isolated production 1.20.1
        # client only permits the exact offline-authlib 401 pair above.
        $unexpectedErrorLog = $unexpectedErrorLog `
            -replace '(?m)^.*No data fixer registered for echo_warrior:(?:roman_legionary_echo|aztec_warrior_echo|guandao_warrior_echo|japanese_samurai_echo|egyptian_archer_echo|egyptian_archer_arrow)\r?\n?', '' `
            -replace '(?m)^.*Failed to fetch user properties\r?\n?', '' `
            -replace '(?m)^.*Failed to fetch Realms feature flags\r?\n?', ''
    }
    if ($unexpectedErrorLog -match '\[(?:[^]]+/)?(?:ERROR|FATAL)\]') {
        throw "$TargetVersion $Loader client log contains an ERROR or FATAL entry."
    }
    $newCrash = Get-ChildItem -LiteralPath $crashDirectory -File -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -ge $startedAt } | Select-Object -First 1
    if ($newCrash) { throw "$TargetVersion $Loader client produced crash report $($newCrash.FullName)." }
    Write-Host "Echo Warrior $TargetVersion $Loader client startup smoke test passed."
    $clientAuditPassed = $true
}
finally {
    if ($gradleProcess) {
        foreach ($processId in Get-DescendantProcessIds $gradleProcess.Id) {
            [void]$launchedProcessIds.Add($processId)
        }
    }
    # Only descendants observed from our launch are owned. Matching a run directory
    # alone is not permission to stop a different client the author has just opened.
    foreach ($processId in @($launchedProcessIds) | Sort-Object -Descending) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
    if ($gradleProcess) {
        $gradleProcess.Refresh()
        if (-not $gradleProcess.HasExited -and -not $gradleProcess.WaitForExit(20000)) {
            Stop-Process -Id $gradleProcess.Id -Force -ErrorAction SilentlyContinue
        }
        $gradleProcess.Dispose()
    }
    $cleanupDeadline = [DateTime]::UtcNow.AddSeconds(10)
    do {
        $remaining = @($launchedProcessIds | Where-Object {
            Get-Process -Id $_ -ErrorAction SilentlyContinue
        })
        if ($remaining.Count -eq 0) { break }
        Start-Sleep -Milliseconds 200
    } while ([DateTime]::UtcNow -lt $cleanupDeadline)
    if ($remaining.Count -gt 0) {
        throw "Client cleanup failed; launched process IDs are still running: $($remaining -join ', ')"
    }
    Write-Host "Closed every process launched by the $TargetVersion $Loader client smoke test."
}
if ($Production -and $clientAuditPassed) {
    # A report is only successful after both gameplay/log auditing and owned-process cleanup.
    $productionLaunch | Add-Member -NotePropertyName passed -NotePropertyValue $true
    $productionLaunch | Add-Member -NotePropertyName cleanup_passed -NotePropertyValue $true
    $productionLaunch | Add-Member -NotePropertyName completed_at -NotePropertyValue ([DateTime]::UtcNow.ToString('o'))
    $productionLaunch | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $runDirectory 'report.json') -Encoding UTF8
}
