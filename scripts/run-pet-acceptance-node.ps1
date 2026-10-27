[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$RunId,
    [Parameter(Mandatory)][string]$FixtureRunId,
    [Parameter(Mandatory)][string]$MainFixtureRunId,
    [string]$SourceRoot,
    [string]$ArtifactRoot,
    [string[]]$Cases = @('1.21.1-fabric','1.21.1-neoforge','26.1.2-fabric','26.1.2-neoforge')
)
$ErrorActionPreference = 'Stop'
if ($env:COMPUTERNAME -ne 'FOXY-NODE') { throw 'Acceptance belongs on FOXY-NODE.' }
$Cases = @($Cases | ForEach-Object { $_ -split ',' })
$validCases = @('1.20.1-fabric','1.20.1-forge','1.21.1-fabric','1.21.1-neoforge','26.1.2-fabric','26.1.2-neoforge')
if ($Cases.Count -eq 0 -or @($Cases | Where-Object { $_ -notin $validCases }).Count -gt 0) {
    throw 'Select at least one known production-client case.'
}
foreach ($value in @($RunId,$FixtureRunId,$MainFixtureRunId)) {
    if ($value -notmatch '^[a-z0-9T-]+$') { throw 'Invalid isolated run ID' }
}
$source = if ($SourceRoot) { (Resolve-Path -LiteralPath $SourceRoot).Path } else { Split-Path -Parent $PSScriptRoot }
if (-not $ArtifactRoot) { $ArtifactRoot = $source }
$reports = "D:\Artifacts-Terminal\Reports\EchoWarrior\$RunId"
$games = "D:\Games-Terminal\EchoWarrior\$RunId"
$lock = 'D:\Workspace-Terminal\EchoWarrior\active-test.lock'
$lease = $null
$status = @{state='running'; run_id=$RunId; source=$source; artifact_root=$ArtifactRoot; results=@(); started_utc=[DateTime]::UtcNow.ToString('o')}
New-Item -ItemType Directory -Path $reports -Force | Out-Null
function Save-Status { $status | ConvertTo-Json -Depth 9 | Set-Content -LiteralPath "$reports\client-status.json" -Encoding UTF8 }
function Run-Logged([string]$Executable,[string[]]$Arguments,[string]$Name) {
    $quoted = @($Arguments | ForEach-Object { '"' + $_.Replace('"','\"') + '"' })
    $process = Start-Process -FilePath $Executable -ArgumentList $quoted -WorkingDirectory $source `
        -RedirectStandardOutput "$reports\$Name.out.log" -RedirectStandardError "$reports\$Name.err.log" `
        -WindowStyle Hidden -PassThru
    $null=$process.Handle
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "$Name failed with exit code $($process.ExitCode)" }
}
try {
    $lease = [IO.File]::Open($lock,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::Read)
    $bytes=[Text.Encoding]::UTF8.GetBytes("pet-acceptance $RunId pid=$PID")
    $lease.Write($bytes,0,$bytes.Length); $lease.Flush()
    $env:ECHO_WARRIOR_JAVA21_HOME=Join-Path $source '.toolchains\jdk-21'
    $env:Path='C:\Program Files\Python314;' + $env:Path
    foreach ($mc in @('1.20.1','1.21.1','26.1.2')) {
        $properties = if ($mc -eq '26.1.2') { "$source\gradle.properties" } else { "$source\versions\$mc\gradle.properties" }
        $versionLine = @(Get-Content -LiteralPath $properties | Where-Object { $_ -match '^mod_version=' })
        if ($versionLine.Count -ne 1) { throw "Expected one mod_version for $mc" }
        $modVersion = $versionLine[0].Substring('mod_version='.Length).Trim()
        if ($modVersion -notmatch '^[0-9]+\.[0-9]+\.[0-9]+(?:-[A-Za-z0-9.-]+)?$') { throw 'Invalid artifact version' }
        $loaders = if ($mc -eq '1.20.1') { @('fabric','forge') } else { @('fabric','neoforge') }
        foreach ($loader in $loaders) {
            $name="$mc-$loader"
            if ($name -notin $Cases) { continue }
            $status.current=$name
            Save-Status
            if ($mc -eq '1.20.1') {
                $fixtureBase="D:\Games-Terminal\EchoWarrior\$FixtureRunId\compatibility-1.20.1-smoke"
                $worlds=@(Get-ChildItem -LiteralPath $fixtureBase -Recurse -Filter level.dat -File |
                    Where-Object {$_.FullName -like "*\$loader\bootstrap-test-world\level.dat"})
                if ($worlds.Count -ne 1) { throw "Expected one normally saved fixture for $name" }
                $world=$worlds[0].Directory.FullName
                $mods=Join-Path (Split-Path -Parent $world) 'mods'
                $jar=Join-Path $ArtifactRoot "versions\1.20.1\$loader\build\libs\echo-warrior-$loader-1.20.1-$modVersion.jar"
            } elseif ($mc -eq '1.21.1') {
                $fixtureBase="D:\Games-Terminal\EchoWarrior\$FixtureRunId\compatibility-1.21.1-production-smoke"
                $worlds=@(Get-ChildItem -LiteralPath $fixtureBase -Recurse -Filter level.dat -File |
                    Where-Object {$_.FullName -like "*\$loader\performance-test-world\level.dat"})
                if ($worlds.Count -ne 1) { throw "Expected one normally saved fixture for $name" }
                $world=$worlds[0].Directory.FullName
                $mods=Join-Path (Split-Path -Parent $world) 'mods'
                $jar=Join-Path $ArtifactRoot "versions\1.21.1\$loader\build\libs\echo-warrior-$loader-1.21.1-$modVersion.jar"
            } else {
                $world="D:\Games-Terminal\EchoWarrior\$MainFixtureRunId\$loader\battlefield-generation-test"
                $mods=Join-Path $source "acceptance-mods\26.1.2\$loader"
                $jar=Join-Path $ArtifactRoot "$loader\build\libs\echo-warrior-$loader-26.1.2-$modVersion.jar"
            }
            $status.phase='prepare'; Save-Status
            Run-Logged 'C:\Program Files\Python314\python.exe' @(
                '-u',"$source\scripts\prepare-pet-acceptance-client.py",'--minecraft',$mc,'--loader',$loader,
                '--mode','smoke','--root',$games,'--world',$world,'--mods',$mods,'--echo-jar',$jar
            ) "$name-prepare"
            $descriptor="$games\$mc-$loader-smoke\launch.json"
            $target=if ($mc -eq '26.1.2') {'Current'} else {$mc}
            # ValidateSet accepts lowercase but preserves it; the 1.20.1 launcher
            # also derives a case-sensitive offline username from this argument.
            $launchLoader = switch ($loader) {
                'fabric' { 'Fabric' }
                'forge' { 'Forge' }
                'neoforge' { 'NeoForge' }
            }
            $status.phase='client'; Save-Status
            Run-Logged 'C:\Program Files\PowerShell\7\pwsh.exe' @(
                '-NoProfile','-ExecutionPolicy','Bypass','-File',"$source\scripts\run-test-client.ps1",
                '-TargetVersion',$target,'-Loader',$launchLoader,'-Production','-PreparedProductionLaunch',$descriptor,
                '-RequireExistingWorld','-StartupOnly','-TimeoutSeconds','300'
            ) "$name-client"
            $report=Get-Content -LiteralPath "$games\$mc-$loader-smoke\report.json" -Raw | ConvertFrom-Json
            if (-not $report.passed -or -not $report.cleanup_passed) { throw "Client report missing acceptance: $name" }
            $clientLog = Get-Content -LiteralPath "$games\$mc-$loader-smoke\logs\latest.log" -Raw
            if ($clientLog -notmatch 'PET FEEDBACK LAYOUT SELFTEST PASSED cases=48') {
                throw "Client did not complete the real-font pet feedback guard: $name"
            }
            $status.results += @{case=$name;passed=$true;report="$games\$mc-$loader-smoke\report.json";artifacts=$report.artifacts}
        }
    }
    if ($status.results.Count -ne @($Cases | Select-Object -Unique).Count) { throw 'Not all selected clients completed.' }
    $status.state='passed'
} catch {
    $status.state='failed'
    $status.error=$_.Exception.Message
} finally {
    $status.finished_utc=[DateTime]::UtcNow.ToString('o'); Save-Status
    if ($lease) { $lease.Dispose(); Remove-Item -LiteralPath $lock }
}
