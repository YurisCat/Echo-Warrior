[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^pet-acceptance-[A-Za-z0-9-]+$')][string]$RunId,
    [string[]]$Loaders=@('fabric','neoforge'),
    [ValidatePattern('^pet-acceptance-[A-Za-z0-9-]+$')][string]$LegacyFixtureRunId
)
$ErrorActionPreference='Stop'
if($env:COMPUTERNAME -ne 'FOXY-NODE'){throw 'Real multiplayer acceptance belongs on FOXY-NODE'}
$Loaders=@($Loaders|ForEach-Object{$_ -split ','})
if(-not $Loaders.Count -or @($Loaders|Where-Object{$_ -notin @('fabric','neoforge')}).Count){throw 'Invalid loader'}
$source=Split-Path -Parent $PSScriptRoot
$games="D:\Games-Terminal\EchoWarrior\$RunId"
$reports="D:\Artifacts-Terminal\Reports\EchoWarrior\$RunId"
$python='C:\Program Files\Python314\python.exe'
$pwsh='C:\Program Files\PowerShell\7\pwsh.exe'
$oldRuntime='D:\Games-Terminal\EchoWarrior\pet-main-20261113T013950Z-68344'
$newRuntime='D:\Games-Terminal\EchoWarrior\pet-main-20261113T062938Z-20980'
$script:clients=@{}
$script:serverProcess=$null
$script:serverRoot=''
$status=@{run=$RunId;state='running';started_utc=[DateTime]::UtcNow.ToString('o');cases=@();legacy_fixture_run=$LegacyFixtureRunId}
if(Test-Path -LiteralPath $games){throw 'Fresh acceptance run required'}
if(Test-Path -LiteralPath 'D:\Workspace-Terminal\EchoWarrior\active-test.lock'){throw 'Node is busy'}
New-Item -ItemType Directory -Path $games,$reports|Out-Null
function Save-Status {$status|ConvertTo-Json -Depth 12|Set-Content -LiteralPath "$reports\status.json" -Encoding UTF8}
function Start-Logged([string]$Executable,[string[]]$Arguments,[string]$Name) {
    $quoted=@($Arguments|ForEach-Object{'"'+$_.Replace('"','\"')+'"'})
    $p=Start-Process -FilePath $Executable -ArgumentList $quoted -WorkingDirectory $source -WindowStyle Hidden `
        -RedirectStandardOutput "$reports\$Name.out.log" -RedirectStandardError "$reports\$Name.err.log" -PassThru
    $null=$p.Handle
    return $p
}
function Wait-Logged($Process,[string]$Name,[int]$Seconds=900){
    if(-not $Process.WaitForExit($Seconds*1000)){throw "$Name timed out; PID $($Process.Id)"}
    if($Process.ExitCode -ne 0){throw "$Name exited $($Process.ExitCode); preserved logs in $reports"}
}
function Run-Logged([string]$Executable,[string[]]$Arguments,[string]$Name){
    $p=Start-Logged $Executable $Arguments $Name
    Wait-Logged $p $Name
}
function Start-Server([string]$Prefix,[string]$Mc,[string]$Loader,[string]$Jar,[string]$World=''){
    $status.current=$Prefix;$status.phase='server';Save-Status
    $script:serverRoot="$games\$Prefix-server"
    $runtime=if($Mc -eq '26.1.2'){"$oldRuntime\$Loader"}else{"$newRuntime\$Loader"}
    $args=@('-u',"$source\scripts\pet-acceptance-server.py",'--source-runtime',$runtime,
        '--root',$script:serverRoot,'--echo-jar',$Jar,'--java',"$source\.toolchains\jdk-25\bin\java.exe",
        '--minecraft',$Mc,'--loader',$Loader)
    if($World){$args+=@('--world',$World)}
    $script:serverProcess=Start-Logged $python $args "$Prefix-server"
    $until=[DateTime]::UtcNow.AddSeconds(180)
    do {
        if(Test-Path -LiteralPath "$script:serverRoot\acceptance-server-state.json"){
            $s=Get-Content -LiteralPath "$script:serverRoot\acceptance-server-state.json" -Raw|ConvertFrom-Json
            if($s.state -eq 'ready'){return}
            if($s.state -eq 'failed'){throw $s.worker_error}
        }
        if($script:serverProcess.HasExited){throw "Server worker exited before ready: $Prefix"}
        Start-Sleep -Milliseconds 250
    }while([DateTime]::UtcNow -lt $until)
    throw "Server not ready: $Prefix"
}
function Prepare-Client([string]$Prefix,[string]$Mc,[string]$Loader,[string]$Jar,[string]$Role,[string]$Suffix=''){
    $label="$Prefix-$Role$Suffix"
    Run-Logged $python @('-u',"$source\scripts\prepare-pet-acceptance-client.py",
        '--baseline','--minecraft',$Mc,'--loader',$Loader,'--mode','multiplayer',
        '--root',$games,'--mods',"$script:serverRoot\mods",'--echo-jar',$Jar,
        '--username',$(if($Role -eq 'owner'){'EchoPetOwner'}else{'EchoPetGuest'}),'--label',$label) "$label-prepare"
    return $label
}
function Write-Pair([string]$Prefix,[string]$Mc,[string]$OwnerLabel,[string]$GuestLabel,[string]$Suffix=''){
    $entries=@($OwnerLabel,$GuestLabel)|ForEach-Object{
        $p="$games\$_\launch.json";@{path=$p;sha256=(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash}
    }
    $path="$games\$Prefix$Suffix-pair.json"
    @{run_root=$games;minecraft=$Mc;server='127.0.0.1:25671';max_clients=2;launches=@($entries)}|
        ConvertTo-Json -Depth 8|Set-Content -LiteralPath $path -Encoding UTF8
    @{owner=$OwnerLabel;guest=$GuestLabel}|ConvertTo-Json|Set-Content -LiteralPath "$games\$Prefix-clients.json" -Encoding UTF8
    return $path
}
function Start-Client([string]$Label,[string]$Mc,[string]$Loader,[string]$Role,[string]$Pair){
    $target=if($Mc -eq '26.1.2'){'Current'}else{$Mc}
    $loaderName=if($Loader -eq 'fabric'){'Fabric'}else{'NeoForge'}
    $args=@('-NoProfile','-WindowStyle','Hidden','-File',"$source\scripts\run-test-client.ps1",
        '-TargetVersion',$target,'-Loader',$loaderName,'-Production','-PreparedProductionLaunch',"$games\$Label\launch.json",
        '-MultiplayerTestManifest',$Pair,'-AcceptancePlayer',$(if($Role -eq 'owner'){'EchoPetOwner'}else{'EchoPetGuest'}))
    $script:clients[$Role]=Start-Logged $pwsh $args "$Label-client"
    Start-Sleep -Seconds 2
}
function Start-Pair([string]$Prefix,[string]$Mc,[string]$Loader,[string]$Jar){
    $status.phase='prepare-clients';Save-Status
    $owner=Prepare-Client $Prefix $Mc $Loader $Jar 'owner'
    $guest=Prepare-Client $Prefix $Mc $Loader $Jar 'guest'
    $pair=Write-Pair $Prefix $Mc $owner $guest
    $status.phase='clients';Save-Status
    Start-Client $owner $Mc $Loader 'owner' $pair
    Start-Client $guest $Mc $Loader 'guest' $pair
}
function Run-Case([string]$Prefix,[string]$Loader,[string]$Phase){
    $status.phase=$Phase;Save-Status
    Run-Logged $python @('-u',"$source\scripts\acceptance_263.py",'--root',$games,'--prefix',$Prefix,
        '--loader',$Loader,'--phase',$Phase) "$Prefix-$Phase"
    $result=Get-Content -LiteralPath "$games\$Prefix-$Phase-evidence-1\result.json" -Raw|ConvertFrom-Json
    if($result.state -ne 'passed'){throw "Case result failed: $Prefix-$Phase"}
    $status.cases+=@{prefix=$Prefix;phase=$Phase;passed=$true;steps=$result.steps.Count}
    Save-Status
}
function Wait-Closed {
    foreach($role in @($script:clients.Keys)){Wait-Logged $script:clients[$role] "$role-client" 30}
    Wait-Logged $script:serverProcess 'server' 45
    $script:clients=@{};$script:serverProcess=$null
    if(Test-Path -LiteralPath 'D:\Workspace-Terminal\EchoWarrior\active-test.lock'){throw 'Previous server lock remains'}
}
try {
    Save-Status
    $manifest=Get-Content -LiteralPath "$source\foxy-source-manifest.json" -Raw|ConvertFrom-Json
    foreach($file in $manifest.files.PSObject.Properties){
        if((Get-FileHash -LiteralPath (Join-Path $source $file.Name) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $file.Value){
            throw "Staged source changed: $($file.Name)"
        }
    }
    foreach($loader in $Loaders){
        $legacyJar="$oldRuntime\$loader\mods\echo-warrior-$loader-26.1.2-0.3.0.jar"
        $newJar="$source\versions\26.3\$loader\build\libs\echo-warrior-$loader-26.3-0.3.1.jar"
        $prior=if($LegacyFixtureRunId){"D:\Games-Terminal\EchoWarrior\$LegacyFixtureRunId"}else{''}
        $priorEvidence="$prior\legacy-$loader-legacy-evidence-1"
        if($prior -and (Test-Path -LiteralPath "$priorEvidence\result.json")){
            $r=Get-Content -LiteralPath "$priorEvidence\result.json" -Raw|ConvertFrom-Json
            $s=Get-Content -LiteralPath "$prior\legacy-$loader-server\acceptance-server-state.json" -Raw|ConvertFrom-Json
            if($r.state -ne 'passed' -or $s.state -ne 'stopped' -or -not $s.normal_save -or $s.exit_code -ne 0){
                throw 'Only a passed normally saved legacy fixture may be reused'
            }
            $hashes=Get-Content -LiteralPath "$priorEvidence\world-sha256.json" -Raw|ConvertFrom-Json
            foreach($file in $hashes.PSObject.Properties){
                if((Get-FileHash -LiteralPath (Join-Path "$prior\legacy-$loader-server\acceptance-world" $file.Name) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $file.Value){
                    throw 'Legacy fixture source changed'
                }
            }
            New-Item -ItemType Directory -Path "$games\legacy-$loader-server"|Out-Null
            Copy-Item -LiteralPath "$prior\legacy-$loader-server\acceptance-world" -Destination "$games\legacy-$loader-server\acceptance-world" -Recurse
            Copy-Item -LiteralPath "$prior\legacy-$loader-server\acceptance-server-state.json" -Destination "$games\legacy-$loader-server"
            Copy-Item -LiteralPath $priorEvidence -Destination "$games\legacy-$loader-legacy-evidence-1" -Recurse
            $status.cases+=@{prefix="legacy-$loader";phase='legacy';passed=$true;reused_from=$LegacyFixtureRunId;steps=$r.steps.Count}
            Save-Status
        }else{
            Start-Server "legacy-$loader" '26.1.2' $loader $legacyJar
            Start-Pair "legacy-$loader" '26.1.2' $loader $legacyJar
            Run-Case "legacy-$loader" $loader 'legacy'
            Wait-Closed
        }
        $legacyWorld="$games\legacy-$loader-server\acceptance-world"
        Start-Server "upgrade-$loader" '26.3' $loader $newJar $legacyWorld
        Start-Pair "upgrade-$loader" '26.3' $loader $newJar
        Run-Case "upgrade-$loader" $loader 'upgrade'
        Wait-Logged $script:clients['guest'] 'disconnected-guest' 30
        $guest=Prepare-Client "upgrade-$loader" '26.3' $loader $newJar 'guest' '-reconnect'
        $pair=Write-Pair "upgrade-$loader" '26.3' "upgrade-$loader-owner" $guest '-reconnect'
        Start-Client $guest '26.3' $loader 'guest' $pair
        Run-Case "upgrade-$loader" $loader 'reconnected'
        Wait-Closed
        Start-Server "restart-$loader" '26.3' $loader $newJar "$games\upgrade-$loader-server\acceptance-world"
        Start-Pair "restart-$loader" '26.3' $loader $newJar
        Run-Case "restart-$loader" $loader 'restart'
        Wait-Closed
        $oldHashes=Get-Content -LiteralPath "$games\legacy-$loader-legacy-evidence-1\world-sha256.json" -Raw|ConvertFrom-Json
        foreach($file in $oldHashes.PSObject.Properties){
            if((Get-FileHash -LiteralPath (Join-Path $legacyWorld $file.Name) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $file.Value){
                throw 'Legacy source world was modified during upgrade'
            }
        }
    }
    $status.state='passed'
} catch {
    $status.state='failed';$status.error=$_.Exception.ToString()
} finally {
    # On failure, request graceful termination only of this run's registered clients/server.
    if($status.state -eq 'failed'){
        foreach($label in @(Get-ChildItem -LiteralPath $games -Directory|Where-Object{$_.Name -match '-(owner|guest)(-reconnect)?$'})){
            $q=Join-Path $label.FullName 'acceptance-queue'
            if(Test-Path -LiteralPath $q){
                $numbers=@(Get-ChildItem -LiteralPath $q -Filter '*.json'|Where-Object{$_.BaseName -match '^\d+$'}|ForEach-Object{[int]$_.BaseName})
                $next=if($numbers.Count){($numbers|Measure-Object -Maximum).Maximum+1}else{1}
                [IO.File]::WriteAllText((Join-Path $q ('{0:d5}.json' -f [int]$next)),'{"action":"stop"}',[Text.UTF8Encoding]::new($false))
            }
        }
        if($script:serverProcess -and -not $script:serverProcess.HasExited){
            $q=Join-Path $script:serverRoot 'acceptance-control'
            $numbers=@(Get-ChildItem -LiteralPath $q -Filter '*.json'|Where-Object{$_.BaseName -match '^\d+$'}|ForEach-Object{[int]$_.BaseName})
            $next=if($numbers.Count){($numbers|Measure-Object -Maximum).Maximum+1}else{1}
            [IO.File]::WriteAllText((Join-Path $q ('{0:d5}.json' -f [int]$next)),'{"commands":["stop"]}',[Text.UTF8Encoding]::new($false))
            $null=$script:serverProcess.WaitForExit(45000)
        }
    }
    $status.finished_utc=[DateTime]::UtcNow.ToString('o');Save-Status
}
