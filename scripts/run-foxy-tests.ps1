[CmdletBinding()]
param(
    [ValidateSet('Run', 'Status', 'Collect')][string]$Action = 'Run',
    [ValidateSet('All', '1.21.1', '1.20.1')][string]$Versions = 'All',
    [ValidatePattern('^[0-9]{8}T[0-9]{6}Z-[0-9]+$')][string]$RunId,
    [switch]$SkipBuild,
    [string]$ControllerWorkspace = 'D:\AI-Workshop\60_Infrastructure\desktop-ms-xamanfjjoqzl'
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$connect = Join-Path $ControllerWorkspace 'scripts\connect-desktop.ps1'
$verify = Join-Path $ControllerWorkspace 'scripts\test-desktop-connection.ps1'
if (-not (Test-Path -LiteralPath $connect)) { throw "Pinned SSH controller is unavailable: $connect" }

function Invoke-Node([string]$Command) {
    & $connect -PowerShellCommand ('$ErrorActionPreference = ''Stop''; ' + $Command)
}
function Copy-Node([string]$Source, [string]$Destination) {
    $identity = Join-Path $env:USERPROFILE '.ssh\desktop-ms-xamanfjjoqzl_ed25519\codex_desktop_ed25519'
    $known = Join-Path $ControllerWorkspace 'scripts\desktop_known_hosts'
    & "$env:WINDIR\System32\OpenSSH\scp.exe" -q -i $identity `
        -o IdentitiesOnly=yes -o BatchMode=yes -o PasswordAuthentication=no `
        -o StrictHostKeyChecking=yes -o HostKeyAlgorithms=ssh-ed25519 `
        -o "UserKnownHostsFile=$known" -o ConnectTimeout=8 $Source $Destination
    if ($LASTEXITCODE -ne 0) { throw "Pinned SCP failed with exit code $LASTEXITCODE" }
}

& $verify
if ($Action -eq 'Run') {
    if ($RunId) { throw 'RunId is generated for a new job; supply it only for Status/Collect.' }
    [string[]]$selected = if ($Versions -eq 'All') { @('1.21.1', '1.20.1') } else { @($Versions) }
    if (-not $SkipBuild) {
        foreach ($version in $selected) {
            & (Join-Path $PSScriptRoot "build-$version.ps1") -Loader Dual
            if ($LASTEXITCODE -ne 0) { throw "$version build failed" }
        }
    }
    $RunId = [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ') + "-$PID"
}
elseif (-not $RunId) { throw 'Supply the RunId printed by Run.' }

$local = Join-Path $repo "build\foxy-tests\$RunId"
$remoteSource = "D:\Workspace-Terminal\EchoWarrior\$RunId"
$remoteReports = "D:\Artifacts-Terminal\Reports\EchoWarrior\$RunId"
$remoteGames = "D:\Games-Terminal\EchoWarrior\$RunId"
$worker = "$remoteSource\scripts\foxy-test-worker.py"
$python = 'C:\Program Files\Python314\python.exe'

if ($Action -eq 'Run') {
    New-Item -ItemType Directory -Path $local | Out-Null
    $package = Join-Path $local 'source.zip'
    & python (Join-Path $PSScriptRoot 'foxy-test-worker.py') --package $package --versions @selected
    if ($LASTEXITCODE -ne 0) { throw 'Source snapshot packaging failed' }
    $hash = (Get-FileHash -LiteralPath $package -Algorithm SHA256).Hash.ToLowerInvariant()
    $incoming = "D:\Download-Terminal\Incoming\EchoWarrior\$RunId.zip"
    Invoke-Node @'
if (Test-Path -LiteralPath 'D:\Workspace-Terminal\EchoWarrior\active-test.lock') { throw 'An Echo Warrior test job already owns the node.' }
foreach ($version in @(17,21,25)) {
    if (-not (Test-Path -LiteralPath "D:\Tools-Terminal\EchoWarrior\jdk-$version\bin\java.exe")) { throw "Configure the node JDK $version first; see docs/FOXY_TEST_NODE.md." }
}
New-Item -ItemType Directory -Path 'D:\Download-Terminal\Incoming\EchoWarrior' -Force | Out-Null
'@
    Copy-Node $package "foxy@100.121.97.8:D:/Download-Terminal/Incoming/EchoWarrior/$RunId.zip"
    Invoke-Node @"
if ((Get-FileHash -LiteralPath '$incoming' -Algorithm SHA256).Hash.ToLowerInvariant() -ne '$hash') { throw 'Source archive transfer hash mismatch' }
if (Test-Path -LiteralPath '$remoteSource') { throw 'A fresh source directory is required' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::ExtractToDirectory('$incoming', '$remoteSource')
New-Item -ItemType Directory -Path '$remoteSource\.toolchains' | Out-Null
foreach (`$jdkVersion in @(17,21,25)) {
    New-Item -ItemType Junction -Path (Join-Path '$remoteSource\.toolchains' "jdk-`$jdkVersion") -Target "D:\Tools-Terminal\EchoWarrior\jdk-`$jdkVersion" | Out-Null
}
New-Item -ItemType Directory -Path '$remoteReports' -Force | Out-Null
# A direct SSH child belongs to sshd's job and is killed when SSH disconnects.
# WMI creates an independent hidden process under the authenticated FOXY account.
`$startup = New-CimInstance -ClassName Win32_ProcessStartup -ClientOnly -Property @{ShowWindow=[uint16]0}
`$created = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{CommandLine='"$python" -u "$worker" --execute --report-root "$remoteReports" --game-root "$remoteGames"';CurrentDirectory='$remoteSource';ProcessStartupInformation=`$startup}
if (`$created.ReturnValue -ne 0) { throw "Independent worker creation failed: `$(`$created.ReturnValue)" }
`$receipt = [pscustomobject]@{RunId='$RunId';Pid=`$created.ProcessId;Source='$remoteSource';Reports='$remoteReports';Games='$remoteGames'}
`$receipt | ConvertTo-Json | Set-Content -LiteralPath '$remoteReports\launch.json' -Encoding UTF8
`$receipt | ConvertTo-Json
"@
    [pscustomobject]@{RunId=$RunId;PackageSha256=$hash;Source=$remoteSource;Reports=$remoteReports;Games=$remoteGames;Versions=$selected} |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $local 'dispatch.json') -Encoding UTF8
    Write-Host "Dispatched $RunId. Check with -Action Status -RunId $RunId; retrieve with -Action Collect -RunId $RunId."
}
elseif ($Action -eq 'Status') {
    Invoke-Node "if (Test-Path -LiteralPath '$remoteReports\status.json') { Get-Content -LiteralPath '$remoteReports\status.json' -Raw -Encoding UTF8 } else { throw 'Worker has not written status; inspect the owned worker stderr log.' }"
}
else {
    $receipt = Invoke-Node "& '$python' '$worker' --collect --report-root '$remoteReports' --game-root '$remoteGames'; if (`$LASTEXITCODE -ne 0) { throw 'Evidence collection failed' }"
    $receipt | Write-Host
    $metadata = ($receipt -join "`n") | ConvertFrom-Json
    New-Item -ItemType Directory -Path $local -Force | Out-Null
    $archive = Join-Path $local 'evidence.zip'
    Copy-Node "foxy@100.121.97.8:D:/Artifacts-Terminal/Reports/EchoWarrior/$RunId/evidence.zip" $archive
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $metadata.sha256) { throw 'Evidence transfer hash mismatch' }
    $destination = Join-Path $local 'evidence'
    if (Test-Path -LiteralPath $destination) { throw "Evidence already extracted: $destination" }
    Expand-Archive -LiteralPath $archive -DestinationPath $destination
    Write-Host "Verified evidence: $destination"
}
