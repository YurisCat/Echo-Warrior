[CmdletBinding()]
param(
    [string]$TestWorldName = 'CATTEST',
    [ValidateSet('Current', '1.21.1')]
    [string]$TargetVersion = 'Current',
    [ValidateSet('Fabric', 'NeoForge')]
    [string]$Loader = 'Fabric',
    [switch]$RequireExistingWorld,
    [switch]$StartupOnly,
    [ValidateRange(30, 300)]
    [int]$TimeoutSeconds = 120
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$focusScript = Join-Path $PSScriptRoot 'focus-test-client-window.ps1'

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

if ($TargetVersion -eq '1.21.1') {
    $buildRoot = Join-Path $projectRoot 'versions\1.21.1'
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
    if ($Loader -eq 'Fabric') {
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

$javaExecutable = Join-Path $jdkRoot 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw "Project Java runtime is missing: $javaExecutable"
}

$escapedRunDirectory = [regex]::Escape($runDirectory)
$escapedClientProjectDirectory = [regex]::Escape($clientProjectDirectory)
$escapedClientUsername = [regex]::Escape($clientUsername)
$clientMainPattern = 'net\.minecraft\.client\.main\.Main|net\.fabricmc\.devlaunchinjector\.Main|net\.neoforged\.devlaunch\.Main'
$allRunningClients = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
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
    if (-not $StartupOnly) {
        & $focusScript -ProjectRoot $buildRoot -ProcessId $runningClient.ProcessId -TimeoutSeconds 10
    }
    exit 0
}
if ($allRunningClients.Count -gt 0) {
    $otherClient = $allRunningClients | Select-Object -First 1
    throw "Another Minecraft development client is already running (PID $($otherClient.ProcessId)); refusing to launch a concurrent client."
}

$env:JAVA_HOME = $jdkRoot
$env:Path = "$(Join-Path $jdkRoot 'bin');$env:Path"
$worldPath = Join-Path $runDirectory "saves\$TestWorldName"
$expectsQuickPlayWorld = Test-Path -LiteralPath $worldPath
if ($StartupOnly -and $TargetVersion -eq '1.21.1') {
    New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null
    $optionsPath = Join-Path $runDirectory 'options.txt'
    $optionLines = if (Test-Path -LiteralPath $optionsPath) {
        @(Get-Content -LiteralPath $optionsPath)
    } else {
        @('version:3955')
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
    Write-Host "Launching Echo Warrior $TargetVersion $Loader and entering $TestWorldName..."
} elseif ($RequireExistingWorld) {
    throw "The requested test world does not exist: $worldPath"
} else {
    Write-Host "Launching Echo Warrior $TargetVersion $Loader to the title screen."
}

if (-not $StartupOnly) {
    Start-Process -FilePath 'powershell.exe' `
        -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $focusScript, '-ProjectRoot', $buildRoot) `
        -WindowStyle Hidden
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
$launchedProcessIds = [System.Collections.Generic.HashSet[int]]::new()
try {
    $gradleProcess = Start-Process -FilePath (Join-Path $projectRoot 'gradlew.bat') `
        -ArgumentList $gradleArguments `
        -WorkingDirectory $projectRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath `
        -PassThru

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    $ready = $false
    while ([DateTime]::UtcNow -lt $deadline) {
        $gradleProcess.Refresh()
        foreach ($processId in Get-DescendantProcessIds $gradleProcess.Id) {
            [void]$launchedProcessIds.Add($processId)
        }
        if ($gradleProcess.HasExited) { break }
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

    $finalLog = Get-Content -LiteralPath $latestLog -Raw
    if ($null -eq $finalLog) { $finalLog = '' }
    if ($finalLog.Length -ge $logStartLength) {
        $finalLog = $finalLog.Substring([int]$logStartLength)
    }
	$unexpectedErrorLog = $finalLog `
		-replace '(?m)^.*No data fixer registered for echo_warrior:(?:roman_legionary_echo|aztec_warrior_echo|guandao_warrior_echo|japanese_samurai_echo|egyptian_archer_echo|egyptian_archer_arrow)\r?\n?', '' `
		-replace '(?m)^.*Failed to fetch user properties\r?\n?', '' `
		-replace '(?m)^.*Failed to fetch Realms feature flags\r?\n?', ''
    if ($unexpectedErrorLog -match '\[(?:[^]]+/)?(?:ERROR|FATAL)\]') {
        throw "$TargetVersion $Loader client log contains an ERROR or FATAL entry."
    }
    $newCrash = Get-ChildItem -LiteralPath $crashDirectory -File -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -ge $startedAt } | Select-Object -First 1
    if ($newCrash) { throw "$TargetVersion $Loader client produced crash report $($newCrash.FullName)." }
    Write-Host "Echo Warrior $TargetVersion $Loader client startup smoke test passed."
}
finally {
    if ($gradleProcess) {
        foreach ($processId in Get-DescendantProcessIds $gradleProcess.Id) {
            [void]$launchedProcessIds.Add($processId)
        }
    }
    $clientProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -match '^javaw?\.exe$' -and
            $_.CommandLine -match $clientMainPattern -and
            ($_.CommandLine -match $escapedRunDirectory -or
                $_.CommandLine -match $escapedClientProjectDirectory -or
                $_.CommandLine -match "--username(?:=|\s)$escapedClientUsername(?:\s|$)")
        }
    foreach ($client in $clientProcesses) {
        Stop-Process -Id $client.ProcessId -Force -ErrorAction SilentlyContinue
    }
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
