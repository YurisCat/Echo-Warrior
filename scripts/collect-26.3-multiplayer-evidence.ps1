[CmdletBinding()]
param([Parameter(Mandatory)][ValidatePattern('^pet-acceptance-[A-Za-z0-9-]+$')][string]$RunId)
$ErrorActionPreference='Stop'
$repo=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$controller='D:\AI-Workshop\60_Infrastructure\desktop-ms-xamanfjjoqzl'
$remote=@'
$ErrorActionPreference='Stop'
$run='__RUN__'
$report="D:\Artifacts-Terminal\Reports\EchoWarrior\$run"
$games="D:\Games-Terminal\EchoWarrior\$run"
$source="D:\Workspace-Terminal\EchoWarrior\$run"
$state=Get-Content -LiteralPath "$report\status.json" -Raw|ConvertFrom-Json
if($state.state -notin @('passed','failed')){throw 'Acceptance is not complete'}
$live=@(Get-CimInstance Win32_Process|Where-Object {$_.Name -in @('java.exe','javaw.exe') -and $_.CommandLine -like "*$run*"})
if($live.Count){throw 'Owned clients have not exited'}
$zip="$report-evidence.zip"
if(-not(Test-Path -LiteralPath $zip)){
 $out=New-Item -ItemType Directory -Path "$report\collected-evidence" -ErrorAction Stop
 $logs=New-Item -ItemType Directory -Path "$out\reports"
 Get-ChildItem -LiteralPath $report -File|Where-Object {$_.Extension -in @('.json','.log')}|ForEach-Object{
  Copy-Item -LiteralPath $_.FullName -Destination $logs
 }
 Copy-Item -LiteralPath "$source\foxy-source-manifest.json" -Destination $out
 foreach($directory in @(Get-ChildItem -LiteralPath $games -Directory)){
  $name=$directory.Name
  $base=$directory.FullName
  if($name -match '^(legacy|upgrade|restart)-(fabric|neoforge)-server$'){
   $to=New-Item -ItemType Directory -Path "$out\$name"
   foreach($file in @('acceptance-console.log','acceptance-server-state.json','acceptance-worker-error.log','logs\latest.log')){
    $path=Join-Path $directory.FullName $file
    if(Test-Path -LiteralPath $path){Copy-Item -LiteralPath $path -Destination $to}
   }
   if(Test-Path -LiteralPath "$base\acceptance-control"){
    Copy-Item -LiteralPath "$base\acceptance-control" -Destination "$to\acceptance-control" -Recurse
   }
  }elseif($name -match '^(legacy|upgrade|restart)-(fabric|neoforge)-(owner|guest)(-reconnect)?$'){
   $to=New-Item -ItemType Directory -Path "$out\$name"
   foreach($file in @('launch.json','logs\latest.log')){
    $path=Join-Path $directory.FullName $file
    if(Test-Path -LiteralPath $path){Copy-Item -LiteralPath $path -Destination $to}
   }
   foreach($folder in @('acceptance-queue','crash-reports')){
    if(Test-Path -LiteralPath "$base\$folder"){Copy-Item -LiteralPath "$base\$folder" -Destination "$to\$folder" -Recurse}
   }
  }elseif($name -match '^(legacy|upgrade|restart)-(fabric|neoforge)-[a-z]+-evidence-\d+$'){
   Copy-Item -LiteralPath $directory.FullName -Destination "$out\$name" -Recurse
  }
 }
 $hashes=@{}
 Get-ChildItem -LiteralPath $out -Recurse -File|ForEach-Object{
  $hashes[$_.FullName.Substring($out.FullName.Length+1)]=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
 }
 @{run=$run;collected_utc=[DateTime]::UtcNow.ToString('o');state=$state.state;files=$hashes}|
  ConvertTo-Json -Depth 8|Set-Content -LiteralPath "$out\evidence-manifest.json" -Encoding UTF8
 Add-Type -AssemblyName System.IO.Compression.FileSystem
 [IO.Compression.ZipFile]::CreateFromDirectory($out.FullName,$zip)
}
@{path=$zip;sha256=(Get-FileHash -LiteralPath $zip -Algorithm SHA256).Hash.ToLowerInvariant();state=$state.state}|ConvertTo-Json -Compress
'@
$local=Join-Path $repo "build\foxy-tests\$RunId"
New-Item -ItemType Directory -Path $local -Force|Out-Null
$helper=Join-Path $local 'collect-node.ps1'
[IO.File]::WriteAllText($helper,$remote.Replace('__RUN__',$RunId),[Text.UTF8Encoding]::new($false))
$remoteHelper="D:\Artifacts-Terminal\Reports\EchoWarrior\$RunId\collect-node.ps1"
& "$env:WINDIR\System32\OpenSSH\scp.exe" -q -i "$env:USERPROFILE\.ssh\desktop-ms-xamanfjjoqzl_ed25519\codex_desktop_ed25519" `
 -o IdentitiesOnly=yes -o BatchMode=yes -o PasswordAuthentication=no -o StrictHostKeyChecking=yes `
 -o "UserKnownHostsFile=$controller\scripts\desktop_known_hosts" $helper `
 ("foxy@100.121.97.8:"+$remoteHelper.Replace('\','/'))
if($LASTEXITCODE -ne 0){throw 'Collector transfer failed'}
$helperHash=(Get-FileHash -LiteralPath $helper -Algorithm SHA256).Hash
$raw=& "$controller\scripts\connect-desktop.ps1" -PowerShellCommand "if((Get-FileHash -LiteralPath '$remoteHelper' -Algorithm SHA256).Hash -ne '$helperHash'){throw 'Collector changed'};& 'C:\Program Files\PowerShell\7\pwsh.exe' -NoProfile -ExecutionPolicy Bypass -File '$remoteHelper';if(`$LASTEXITCODE -ne 0){throw 'Collector failed'}"
$receipt=($raw -join "`n")|ConvertFrom-Json
$archive=Join-Path $local 'evidence.zip'
& "$env:WINDIR\System32\OpenSSH\scp.exe" -q -i "$env:USERPROFILE\.ssh\desktop-ms-xamanfjjoqzl_ed25519\codex_desktop_ed25519" `
 -o IdentitiesOnly=yes -o BatchMode=yes -o PasswordAuthentication=no -o StrictHostKeyChecking=yes `
 -o "UserKnownHostsFile=$controller\scripts\desktop_known_hosts" `
 ("foxy@100.121.97.8:"+$receipt.path.Replace('\','/')) $archive
if($LASTEXITCODE -ne 0 -or (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $receipt.sha256){throw 'Evidence transfer checksum failed'}
$destination=Join-Path $local 'evidence'
if(-not(Test-Path -LiteralPath $destination)){Expand-Archive -LiteralPath $archive -DestinationPath $destination}
$manifest=Get-Content -LiteralPath "$destination\evidence-manifest.json" -Raw|ConvertFrom-Json
foreach($file in $manifest.files.PSObject.Properties){
 if((Get-FileHash -LiteralPath (Join-Path $destination $file.Name) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $file.Value){throw "Evidence file changed: $($file.Name)"}
}
$receipt|ConvertTo-Json|Set-Content -LiteralPath "$local\collection-receipt.json" -Encoding UTF8
Write-Output "Verified $(@($manifest.files.PSObject.Properties).Count) evidence files; run state $($receipt.state): $destination"
