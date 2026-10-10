[CmdletBinding()]
param([string]$LauncherPath=(Join-Path $PSScriptRoot 'run-test-client.ps1'))
$ErrorActionPreference='Stop'
$errors=$null;$tokens=$null
$ast=[Management.Automation.Language.Parser]::ParseFile((Resolve-Path -LiteralPath $LauncherPath),[ref]$tokens,[ref]$errors)
if($errors.Count){throw 'Client launcher does not parse'}
$function=$ast.Find({param($node) $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Get-DescendantProcessIds'},$true)
if(-not $function){throw 'Descendant traversal is missing'}
$module=New-Module -ScriptBlock {
    param($definition)
    Invoke-Expression $definition
    function Get-CimInstance {
        param([string]$ClassName)
        if($ClassName -ne 'Win32_Process'){throw 'Unexpected fixture query'}
        @(
            [pscustomobject]@{ProcessId=100;ParentProcessId=50;Name='cmd.exe'},
            [pscustomobject]@{ProcessId=101;ParentProcessId=100;Name='java.exe'},
            [pscustomobject]@{ProcessId=102;ParentProcessId=101;Name='cmd.exe'},
            [pscustomobject]@{ProcessId=130;ParentProcessId=100;Name='conhost.exe'},
            [pscustomobject]@{ProcessId=120;ParentProcessId=101;Name='OpenConsole.exe'},
            [pscustomobject]@{ProcessId=103;ParentProcessId=120;Name='cmd.exe'},
            [pscustomobject]@{ProcessId=90;ParentProcessId=999;Name='java.exe'}
        )
    }
    Export-ModuleMember -Function Get-DescendantProcessIds
} -ArgumentList $function.Extent.Text
try{
    $owned=@(& $module {Get-DescendantProcessIds -RootProcessId 100}|Sort-Object)
    if(($owned -join ',') -cne '101,102,103'){throw 'Console hosts, unrelated processes or descendants were classified incorrectly'}
    $empty=@(& $module {Get-DescendantProcessIds -RootProcessId 678})
    if($empty.Count){throw 'Missing root must not claim unrelated processes'}
}finally{Remove-Module $module -ErrorAction SilentlyContinue}
$launch=@($ast.FindAll({param($node)
    $node -is [Management.Automation.Language.CommandAst] -and
    $node.GetCommandName() -eq 'Start-Process' -and $node.Extent.Text -like '*$launchExecutable*'
},$true))
if($launch.Count -ne 1){throw 'Automated client launch is ambiguous'}
$parameters=@($launch[0].CommandElements|Where-Object{$_ -is [Management.Automation.Language.CommandParameterAst]}|ForEach-Object{$_.ParameterName})
if($parameters -notcontains 'NoNewWindow' -or $parameters -contains 'WindowStyle'){throw 'Automated client must retain the launcher console'}
if($parameters -notcontains 'RedirectStandardOutput' -or $parameters -notcontains 'RedirectStandardError'){throw 'Client logs must remain redirected'}
[pscustomobject]@{Passed=$true;Checks=@('Console hosts excluded from forced cleanup','Descendants through console hosts retained','Unrelated processes excluded','Absent root stays empty','Automated launch retains console','Output remains logged');SyntheticOnly=$true;ProcessesStarted=0;LauncherSha256=(Get-FileHash -LiteralPath $LauncherPath).Hash}|ConvertTo-Json -Depth 4
