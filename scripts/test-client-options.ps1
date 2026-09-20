$ErrorActionPreference = 'Stop'
$launcher = Join-Path $PSScriptRoot 'run-test-client.ps1'
$ast = [System.Management.Automation.Language.Parser]::ParseFile($launcher, [ref]$null, [ref]$null)
$block = $ast.Find({ param($node)
    $node -is [System.Management.Automation.Language.IfStatementAst] -and
    $node.Extent.Text.StartsWith('if (($StartupOnly -or $PauseOnJoin)')
}, $true)
if ($null -eq $block) { throw 'Options preparation block not found' }
$prepare = [scriptblock]::Create($block.Extent.Text)
$testRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('echo-client-options-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $testRoot | Out-Null
$StartupOnly = $true
$PauseOnJoin = $false
$checked = 0
try {
    foreach ($TargetVersion in @('1.20.1', '1.21.1')) {
        foreach ($fixture in @('absent', 'single', 'malformed', 'populated')) {
            $runDirectory = Join-Path $testRoot ($TargetVersion + '-' + $fixture)
            New-Item -ItemType Directory -Path $runDirectory | Out-Null
            $options = Join-Path $runDirectory 'options.txt'
            $dataVersion = if ($TargetVersion -eq '1.20.1') { '3465' } else { '3955' }
            switch ($fixture) {
                'single' { [System.IO.File]::WriteAllText($options, "version:$dataVersion") }
                'malformed' { [System.IO.File]::WriteAllText($options, "version:${dataVersion}onboardAccessibility:falseonboardAccessibility:false") }
                'populated' { [System.IO.File]::WriteAllLines($options, @("version:$dataVersion", 'lang:zh_cn', 'soundCategory_master:0.25', 'onboardAccessibility:true')) }
            }
            . $prepare
            . $prepare
            $lines = @(Get-Content -LiteralPath $options)
            if ($lines[0] -ne "version:$dataVersion" -or @($lines | Where-Object { $_ -eq 'onboardAccessibility:false' }).Count -ne 1) {
                throw "Malformed options after repeated preparation: $TargetVersion/$fixture"
            }
            if ($fixture -eq 'populated' -and ('lang:zh_cn' -notin $lines -or 'soundCategory_master:0.25' -notin $lines)) {
                throw 'Launcher modified unrelated preferences'
            }
            $checked++
        }
    }
    Write-Host "Client options checks passed: $checked cases; repeated runs preserve line boundaries and unrelated preferences."
} finally {
    $resolved = [System.IO.Path]::GetFullPath($testRoot)
    $temporary = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath()).TrimEnd('\') + '\'
    if (!$resolved.StartsWith($temporary, [System.StringComparison]::OrdinalIgnoreCase) -or
        !([System.IO.Path]::GetFileName($resolved).StartsWith('echo-client-options-'))) { throw 'Unsafe test cleanup path' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
