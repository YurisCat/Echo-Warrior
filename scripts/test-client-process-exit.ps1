$ErrorActionPreference = 'Stop'
if ((Get-Content (Join-Path $PSScriptRoot 'run-test-client.ps1') -Raw) -notmatch '\$null = \$gradleProcess\.Handle') {
    throw 'Client launcher must retain the started process handle.'
}
$testDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) ('build\client-process-exit-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testDirectory | Out-Null
foreach ($retainHandle in @($false, $true)) {
    foreach ($expected in @(0, 7)) {
        $name = "retained-$retainHandle-code-$expected"
        $process = Start-Process -FilePath (Join-Path $PSScriptRoot 'tests\client-exit-fixture.bat') `
            -ArgumentList @($expected) -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput (Join-Path $testDirectory "$name.out.log") `
            -RedirectStandardError (Join-Path $testDirectory "$name.err.log")
        if ($retainHandle) { $null = $process.Handle }
        $deadline = [datetime]::UtcNow.AddSeconds(15)
        while (-not $process.HasExited -and [datetime]::UtcNow -lt $deadline) {
            Start-Sleep -Milliseconds 100
            $process.Refresh()
        }
        if (-not $process.HasExited) { throw "Exit probe timed out: $name" }
        Write-Host "$name exited=$($process.HasExited) code=$($process.ExitCode) null=$($null -eq $process.ExitCode)"
        if ($retainHandle -and ($null -eq $process.ExitCode -or $process.ExitCode -ne $expected)) {
            throw "Retained process reported wrong exit code: $name"
        }
        $process.Dispose()
    }
}
Write-Host 'Client process exit code checks passed.'
