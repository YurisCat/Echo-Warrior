[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$ProjectRoot
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'tester-support-common.ps1')

try {
    $root = Resolve-TesterProjectRoot -Path $ProjectRoot
    Assert-TesterEnvironment -ProjectRoot $root -RequireWorld

    $setupMarker = Join-Path $root '.toolchains\tester-kit\setup-complete.txt'
    if (-not (Test-Path -LiteralPath $setupMarker -PathType Leaf)) {
        throw '尚未完成首次检查。请先双击“首次安装.bat”。'
    }

    $jdkRoot = Join-Path $root '.toolchains\jdk-25'
    $env:JAVA_HOME = $jdkRoot
    $env:GRADLE_USER_HOME = Join-Path $root '.toolchains\gradle-user-home'
    $env:Path = "$(Join-Path $jdkRoot 'bin');$env:Path"

    & (Join-Path $root 'scripts\playtest-now.ps1')
    exit $LASTEXITCODE
} catch {
    Write-Host ''
    Write-Host '测试环境无法启动：' -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
	Write-Host '请打开 TESTER_GUIDE.html 排查，或把本窗口截图发给开发者。'
	exit 1
}
