[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

& (Join-Path $PSScriptRoot 'run-test-client.ps1') `
    -TestWorldName 'CATTEST' `
    -RequireExistingWorld

exit $LASTEXITCODE
