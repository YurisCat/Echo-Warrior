[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$ProjectRoot
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'tester-support-common.ps1')

try {
    $root = Resolve-TesterProjectRoot -Path $ProjectRoot
    Write-Host 'Echo Warrior 测试环境首次检查' -ForegroundColor Green
    Write-Host "项目目录：$root"

    Write-TesterStep -Number 1 -Title '检查离线包完整性'
    Assert-TesterEnvironment -ProjectRoot $root -RequireWorld
    Write-Host 'PortableGit、Java 25、Gradle 离线缓存和 CATTEST 均已找到。'

    Write-TesterStep -Number 2 -Title '配置 GitHub 私有仓库'
    $git = Get-TesterGitExecutable -ProjectRoot $root
    Invoke-TesterNative -FilePath $git -Arguments @('--version') -FailureMessage 'PortableGit 无法运行'
    Set-TesterGitConfiguration -GitExecutable $git -ProjectRoot $root

    $branch = (& $git -C $root branch --show-current 2>$null | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $branch -ne 'main') {
        throw "当前 Git 分支不是 main，而是：$branch"
    }
    Write-Host 'Git 远程地址和 main 分支检查完成。'

    Write-TesterStep -Number 3 -Title '验证 Java 25'
    $java = Join-Path $root '.toolchains\jdk-25\bin\java.exe'
    Invoke-TesterNative -FilePath $java -Arguments @('-version') -FailureMessage 'Java 25 无法运行'

    Write-TesterStep -Number 4 -Title '执行离线构建检查'
    Invoke-TesterOfflineBuildCheck -ProjectRoot $root

    $marker = Join-Path $root '.toolchains\tester-kit\setup-complete.txt'
    $markerDirectory = Split-Path -Parent $marker
    New-Item -ItemType Directory -Path $markerDirectory -Force | Out-Null
    Set-Content -LiteralPath $marker -Encoding UTF8 -Value ("completed={0:o}" -f (Get-Date))

    Write-Host ''
    Write-Host '首次检查完成。以后更新请双击“强制更新.bat”，启动测试请双击“启动测试.bat”。' -ForegroundColor Green
    exit 0
} catch {
    Write-Host ''
    Write-Host '首次检查未完成：' -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
	Write-Host '请打开 TESTER_GUIDE.html 的“第一次使用”标签页排查，或把本窗口截图发给开发者。'
	exit 1
}
