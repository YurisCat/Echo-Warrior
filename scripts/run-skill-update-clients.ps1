[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$RunId,
    [Parameter(Mandatory)][string]$CompatRunId,
    [Parameter(Mandatory)][string]$MainRunId,
    [string[]]$Cases = @('1.20.1-fabric', '1.21.1-fabric', '26.1.2-fabric')
)
$ErrorActionPreference = 'Stop'
if ($env:COMPUTERNAME -ne 'FOXY-NODE') { throw 'Client acceptance belongs on FOXY-NODE.' }
foreach ($value in @($RunId, $CompatRunId, $MainRunId)) {
    if ($value -notmatch '^[A-Za-z0-9-]+$') { throw 'Invalid test run ID.' }
}
$Cases = @($Cases | ForEach-Object { $_ -split ',' })
$valid = @('1.20.1-fabric', '1.20.1-forge', '1.21.1-fabric', '1.21.1-neoforge', '26.1.2-fabric', '26.1.2-neoforge')
if (-not $Cases.Count -or @($Cases | Where-Object { $_ -notin $valid }).Count) { throw 'Unknown client case.' }
$source = Split-Path -Parent $PSScriptRoot
$games = "D:\Games-Terminal\EchoWarrior\$RunId"
$reports = "D:\Artifacts-Terminal\Reports\EchoWarrior\$RunId"
$lock = 'D:\Workspace-Terminal\EchoWarrior\active-test.lock'
$lease = $null
New-Item -ItemType Directory -Path $reports -Force | Out-Null
$status = @{state='running';run_id=$RunId;source=$source;results=@();started_utc=[DateTime]::UtcNow.ToString('o')}
function Save-Status { $status | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath "$reports\client-status.json" -Encoding UTF8 }
function Run-Logged([string]$Executable, [string[]]$Arguments, [string]$Name) {
    $quoted = @($Arguments | ForEach-Object { '"' + $_.Replace('"', '\"') + '"' })
    $process = Start-Process -FilePath $Executable -ArgumentList $quoted -WorkingDirectory $source `
        -RedirectStandardOutput "$reports\$Name.out.log" -RedirectStandardError "$reports\$Name.err.log" `
        -WindowStyle Hidden -PassThru
    $null = $process.Handle
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "$Name failed: $($process.ExitCode)" }
}
try {
    $lease = [IO.File]::Open($lock, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::Read)
    $bytes = [Text.Encoding]::UTF8.GetBytes("skill-client $RunId pid=$PID source=$source")
    $lease.Write($bytes,0,$bytes.Length); $lease.Flush()
    $manifest = Get-Content -LiteralPath "$source\foxy-source-manifest.json" -Raw | ConvertFrom-Json
    foreach ($file in $manifest.files.PSObject.Properties) {
        if ((Get-FileHash -LiteralPath (Join-Path $source $file.Name) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $file.Value) {
            throw "Client source input changed: $($file.Name)"
        }
    }
    $env:ECHO_WARRIOR_JAVA21_HOME = Join-Path $source '.toolchains\jdk-21'
    $env:Path = 'C:\Program Files\Python314;' + $env:Path
    foreach ($case in $Cases) {
        $mc, $loader = $case -split '-', 2
        $status.current = $case; $status.phase = 'prepare'; Save-Status
        $properties = if ($mc -eq '26.1.2') { "$source\gradle.properties" } else { "$source\versions\$mc\gradle.properties" }
        $version = ((Get-Content -LiteralPath $properties | Where-Object { $_ -match '^mod_version=' }) -split '=',2)[1].Trim()
        if ($mc -eq '26.1.2') {
            $world = "D:\Games-Terminal\EchoWarrior\$MainRunId\$loader\battlefield-generation-test"
            $jar = "$source\$loader\build\libs\echo-warrior-$loader-$mc-$version.jar"
        } else {
            $suite = if ($mc -eq '1.20.1') { 'compatibility-1.20.1-smoke' } else { 'compatibility-1.21.1-production-smoke' }
            $worldName = if ($mc -eq '1.20.1') { 'bootstrap-test-world' } else { 'performance-test-world' }
            $found = @(Get-ChildItem -LiteralPath "D:\Games-Terminal\EchoWarrior\$CompatRunId\$suite" -Recurse -Filter level.dat -File |
                Where-Object { $_.FullName -like "*\$loader\$worldName\level.dat" })
            if ($found.Count -ne 1) { throw "Expected one saved test world for $case." }
            $world = $found[0].Directory.FullName
            $jar = "$source\versions\$mc\$loader\build\libs\echo-warrior-$loader-$mc-$version.jar"
        }
        if (-not (Test-Path -LiteralPath "$world\level.dat")) { throw 'Saved fixture missing.' }
        $mods = Join-Path (Split-Path -Parent $world) 'mods'
        Run-Logged 'C:\Program Files\Python314\python.exe' @(
            '-u', "$source\scripts\prepare-pet-acceptance-client.py", '--baseline',
            '--minecraft', $mc, '--loader', $loader, '--mode', 'smoke', '--root', $games,
            '--world', $world, '--mods', $mods, '--echo-jar', $jar
        ) "$case-prepare"
        $status.phase = 'client'; Save-Status
        $target = if ($mc -eq '26.1.2') { 'Current' } else { $mc }
        $launchLoader = switch ($loader) { 'fabric' { 'Fabric' } 'forge' { 'Forge' } 'neoforge' { 'NeoForge' } }
        $instance = "$games\$mc-$loader-smoke"
        Run-Logged 'C:\Program Files\PowerShell\7\pwsh.exe' @(
            '-NoProfile', '-WindowStyle', 'Hidden', '-File', "$source\scripts\run-test-client.ps1",
            '-TargetVersion', $target, '-Loader', $launchLoader, '-Production',
            '-PreparedProductionLaunch', "$instance\launch.json", '-RequireExistingWorld',
            '-StartupOnly', '-TimeoutSeconds', '300'
        ) "$case-client"
        $result = Get-Content -LiteralPath "$instance\report.json" -Raw | ConvertFrom-Json
        $log = Get-Content -LiteralPath "$instance\logs\latest.log" -Raw
        if (-not $result.passed -or -not $result.cleanup_passed -or $log -notmatch 'SKILL TOOLTIP CLIENT SELFTEST PASSED') {
            throw "Required client acceptance missing: $case"
        }
        $status.results += @{case=$case;passed=$true;report="$instance\report.json";artifacts=$result.artifacts}
    }
    $status.state='passed'
} catch {
    $status.state='failed'; $status.error=$_.Exception.Message
} finally {
    $status.finished_utc=[DateTime]::UtcNow.ToString('o'); Save-Status
    if ($lease) { $lease.Dispose(); Remove-Item -LiteralPath $lock }
}
