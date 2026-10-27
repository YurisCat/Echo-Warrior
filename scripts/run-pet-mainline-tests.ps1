[CmdletBinding()]
param(
    [ValidateSet('Run', 'Status', 'Collect')][string]$Action = 'Run',
    [string]$InputDirectory,
    [ValidatePattern('^pet-main-[0-9]{8}T[0-9]{6}Z-[0-9]+$')][string]$RunId,
    [ValidateSet('both', 'fabric', 'neoforge')][string]$Loader = 'both',
    [switch]$Baseline,
    [string]$ControllerWorkspace = 'D:\AI-Workshop\60_Infrastructure\desktop-ms-xamanfjjoqzl'
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$connect = Join-Path $ControllerWorkspace 'scripts\connect-desktop.ps1'
& (Join-Path $ControllerWorkspace 'scripts\test-desktop-connection.ps1')
function Invoke-Node([string]$Command) {
    & $connect -PowerShellCommand ('$ErrorActionPreference = ''Stop''; ' + $Command)
}
function Copy-Node([string]$Source, [string]$Destination) {
    $identity = Join-Path $env:USERPROFILE '.ssh\desktop-ms-xamanfjjoqzl_ed25519\codex_desktop_ed25519'
    $known = Join-Path $ControllerWorkspace 'scripts\desktop_known_hosts'
    & "$env:WINDIR\System32\OpenSSH\scp.exe" -q -i $identity -o IdentitiesOnly=yes -o BatchMode=yes `
        -o StrictHostKeyChecking=yes -o HostKeyAlgorithms=ssh-ed25519 -o "UserKnownHostsFile=$known" `
        -o ConnectTimeout=8 $Source $Destination
    if ($LASTEXITCODE -ne 0) { throw 'Pinned test transfer failed.' }
}
if ($Action -eq 'Run') {
    if ($RunId) { throw 'Run creates a fresh RunId.' }
    $inputRoot = (Resolve-Path -LiteralPath $InputDirectory).Path
    if (-not (Test-Path -LiteralPath (Join-Path $inputRoot 'inputs.json'))) { throw 'Prepared runtime manifest missing.' }
    $RunId = 'pet-main-' + [DateTime]::UtcNow.ToString('yyyyMMddTHHmmssZ') + "-$PID"
}
elseif (-not $RunId) { throw 'Status/Collect requires RunId.' }
$local = Join-Path $repo "build\foxy-tests\$RunId"
$remoteSource = "D:\Workspace-Terminal\EchoWarrior\$RunId"
$remoteReports = "D:\Artifacts-Terminal\Reports\EchoWarrior\$RunId"
$remoteGames = "D:\Games-Terminal\EchoWarrior\$RunId"
$python = 'C:\Program Files\Python314\python.exe'
if ($Action -eq 'Run') {
    New-Item -ItemType Directory -Path $local | Out-Null
    $package = Join-Path $local 'input.zip'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::CreateFromDirectory($inputRoot, $package)
    $hash = (Get-FileHash -LiteralPath $package -Algorithm SHA256).Hash.ToLowerInvariant()
    $petFlag = if ($Baseline) { '' } else { ' --pet-collars' }
    $incoming = "D:\Download-Terminal\Incoming\EchoWarrior\$RunId.zip"
    Invoke-Node @"
if (Test-Path -LiteralPath 'D:\Workspace-Terminal\EchoWarrior\active-test.lock') { throw 'Another test owns the node.' }
if (Test-Path -LiteralPath '$remoteSource') { throw 'Fresh test snapshot required.' }
"@
    Copy-Node $package "foxy@100.121.97.8:D:/Download-Terminal/Incoming/EchoWarrior/$RunId.zip"
    Invoke-Node @"
if ((Get-FileHash -LiteralPath '$incoming' -Algorithm SHA256).Hash.ToLowerInvariant() -ne '$hash') { throw 'Transfer hash mismatch.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::ExtractToDirectory('$incoming', '$remoteSource')
`$startup = New-CimInstance -ClassName Win32_ProcessStartup -ClientOnly -Property @{ShowWindow=[uint16]0}
`$command = '"$python" -u "$remoteSource\smoke-test-battlefield-mainline.py" --input "$remoteSource" --games "$remoteGames" --reports "$remoteReports" --java "D:\Tools-Terminal\EchoWarrior\jdk-25\bin\java.exe" --lock "D:\Workspace-Terminal\EchoWarrior\active-test.lock" --loader $Loader$petFlag'
`$created = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{CommandLine=`$command;CurrentDirectory='$remoteSource';ProcessStartupInformation=`$startup}
if (`$created.ReturnValue -ne 0) { throw 'Hidden test worker creation failed.' }
`$created | ConvertTo-Json
"@
    [pscustomobject]@{RunId=$RunId;PackageSha256=$hash;Source=$remoteSource;Reports=$remoteReports;Games=$remoteGames;Loader=$Loader;Baseline=[bool]$Baseline} |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $local 'dispatch.json') -Encoding UTF8
    Write-Host "Dispatched $RunId"
}
elseif ($Action -eq 'Status') {
    Invoke-Node "if (Test-Path -LiteralPath '$remoteReports\report.json') { Get-Content -LiteralPath '$remoteReports\report.json' -Raw | ConvertFrom-Json | Select-Object state,error,results | ConvertTo-Json -Depth 6 } else { Get-CimInstance Win32_Process | Where-Object { `$_.CommandLine -like '*$RunId*' -and `$_.Name -eq 'python.exe' } | Select-Object ProcessId,CreationDate | ConvertTo-Json }"
}
else {
    $hash = ((Invoke-Node "Get-Content -LiteralPath '$remoteReports\evidence-sha256.txt' -Raw") -join '').Trim()
    if ($hash -notmatch '^[a-f0-9]{64}$') { throw 'Completed evidence hash unavailable.' }
    New-Item -ItemType Directory -Path $local -Force | Out-Null
    $archive = Join-Path $local 'evidence.zip'
    Copy-Node "foxy@100.121.97.8:D:/Artifacts-Terminal/Reports/EchoWarrior/$RunId/evidence.zip" $archive
    if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $hash) { throw 'Evidence hash mismatch.' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory($archive, (Join-Path $local 'evidence'))
    Write-Host "Verified evidence: $local\evidence"
}
