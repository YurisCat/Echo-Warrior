[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$launcher = Join-Path $projectRoot 'tools\windows\Launch 1.20.1 Forge Test Client.bat'
$world = Join-Path $projectRoot 'versions\1.20.1\run-forge\saves\CATTEST\level.dat'
if (-not (Test-Path -LiteralPath $launcher) -or -not (Test-Path -LiteralPath $world)) {
    throw 'The 1.20.1 Forge launcher or isolated CATTEST world is missing.'
}
$desktopDirectory = [Environment]::GetFolderPath('Desktop')
$shortcutPath = Join-Path $desktopDirectory 'Echo Warrior 1.20.1 Forge 测试端（进世界自动暂停）.lnk'
if (Test-Path -LiteralPath $shortcutPath) {
    throw "Shortcut already exists; inspect it before replacing: $shortcutPath"
}
$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $launcher
$shortcut.WorkingDirectory = $projectRoot
$shortcut.Description = 'Minecraft 1.20.1 Forge / Echo Warrior / CATTEST。进入世界自动暂停并释放鼠标；点击返回游戏后正常操作，不自动退出。内部移植测试端，尚非完整可玩版。'
$shortcut.WindowStyle = 7
$shortcut.Save()
$verified = $shell.CreateShortcut($shortcutPath)
if ($verified.TargetPath -ne $launcher -or $verified.WorkingDirectory -ne $projectRoot) {
    throw 'Shortcut verification failed.'
}
Write-Host "Created: $shortcutPath"
Write-Host "Target: $($verified.TargetPath)"
