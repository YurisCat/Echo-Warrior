[CmdletBinding()]
param(
    [string]$KitRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'modeler-support-common.ps1')

try {
    $root = Get-ModelerProjectRoot -KitRoot $KitRoot -ForceSelection
    Write-Host ''
    Write-Host "已记住项目目录：$root" -ForegroundColor Green
    exit 0
} catch {
    Write-Host ''
	Write-Host "没有更改项目目录：$($_.Exception.Message)" -ForegroundColor Red
	exit 1
}
