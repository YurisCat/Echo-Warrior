[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^skill-client-[A-Za-z0-9-]+$')][string]$RunId,
    [string]$ControllerWorkspace = 'D:\AI-Workshop\60_Infrastructure\desktop-ms-xamanfjjoqzl'
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$connect = Join-Path $ControllerWorkspace 'scripts\connect-desktop.ps1'
& (Join-Path $ControllerWorkspace 'scripts\test-desktop-connection.ps1')
$report = "D:\Artifacts-Terminal\Reports\EchoWarrior\$RunId"
$game = "D:\Games-Terminal\EchoWarrior\$RunId"
$remoteArchive = "$report-client-evidence.zip"
$result = & $connect -PowerShellCommand @"
`$ErrorActionPreference = 'Stop'
`$status = Get-Content -LiteralPath '$report\client-status.json' -Raw | ConvertFrom-Json
if (`$status.state -notin @('passed','failed')) { throw 'Client evidence is not final yet' }
if (-not (Test-Path -LiteralPath '$remoteArchive')) {
    `$evidence = '$report\runtime-evidence'
    New-Item -ItemType Directory -Path `$evidence -Force | Out-Null
    Copy-Item -LiteralPath 'D:\Workspace-Terminal\EchoWarrior\$RunId\foxy-source-manifest.json' -Destination `$evidence
    foreach (`$loader in @('fabric','neoforge')) {
        `$instance = Join-Path '$game' "26.3-`$loader-smoke"
        foreach (`$name in @('report.json','logs\latest.log')) {
            `$file = Join-Path `$instance `$name
            if (Test-Path -LiteralPath `$file) {
                Copy-Item -LiteralPath `$file -Destination (Join-Path `$evidence ("`$loader-" + (Split-Path -Leaf `$file)))
            }
        }
        foreach (`$folder in @('crash-reports','screenshots')) {
            `$directory = Join-Path `$instance `$folder
            if (Test-Path -LiteralPath `$directory) {
                `$destination = Join-Path `$evidence "`$loader-`$folder"
                New-Item -ItemType Directory -Path `$destination -Force | Out-Null
                Get-ChildItem -LiteralPath `$directory -File | Where-Object { `$_.Extension -in @('.txt','.png') } |
                    ForEach-Object { Copy-Item -LiteralPath `$_.FullName -Destination `$destination }
            }
        }
    }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::CreateFromDirectory('$report', '$remoteArchive')
}
(Get-FileHash -LiteralPath '$remoteArchive' -Algorithm SHA256).Hash.ToLowerInvariant()
"@
$expected = ($result -join '').Trim()
if ($expected -notmatch '^[a-f0-9]{64}$') { throw 'Invalid node evidence hash' }
$local = Join-Path $repo "build\foxy-tests\$RunId"
New-Item -ItemType Directory -Path $local -Force | Out-Null
$archive = Join-Path $local 'client-evidence.zip'
$identity = Join-Path $env:USERPROFILE '.ssh\desktop-ms-xamanfjjoqzl_ed25519\codex_desktop_ed25519'
$known = Join-Path $ControllerWorkspace 'scripts\desktop_known_hosts'
& "$env:WINDIR\System32\OpenSSH\scp.exe" -q -i $identity -o IdentitiesOnly=yes -o BatchMode=yes `
    -o PasswordAuthentication=no -o StrictHostKeyChecking=yes -o HostKeyAlgorithms=ssh-ed25519 `
    -o "UserKnownHostsFile=$known" -o ConnectTimeout=8 `
    "foxy@100.121.97.8:D:/Artifacts-Terminal/Reports/EchoWarrior/$RunId-client-evidence.zip" $archive
if ($LASTEXITCODE -ne 0 -or (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) {
    throw 'Client evidence transfer/checksum failed'
}
$destination = Join-Path $local 'evidence'
if (-not (Test-Path -LiteralPath $destination)) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory($archive, $destination)
}
Write-Output "Verified client evidence: $destination"
