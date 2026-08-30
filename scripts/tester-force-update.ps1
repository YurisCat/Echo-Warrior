[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$ProjectRoot
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'tester-support-common.ps1')

try {
    $root = Resolve-TesterProjectRoot -Path $ProjectRoot
    Write-Host 'Echo Warrior 强制更新' -ForegroundColor Green
    Write-Host '本操作会抛弃代码文件的本地修改，但保留测试世界、配置、截图、日志和离线工具。'

    Write-TesterStep -Number 1 -Title '检查 Minecraft 是否已关闭'
    if (Test-TesterClientRunning -ProjectRoot $root) {
        throw '检测到本项目的 Minecraft 开发客户端仍在运行。请先正常关闭游戏，再重新执行更新。'
    }
    Write-Host '未发现正在运行的开发客户端。'

    Write-TesterStep -Number 2 -Title '连接 GitHub 并获取 main 分支'
    Assert-TesterEnvironment -ProjectRoot $root
    $git = Get-TesterGitExecutable -ProjectRoot $root
    Set-TesterGitConfiguration -GitExecutable $git -ProjectRoot $root
    Write-Host '若浏览器弹出 GitHub 登录页面，请登录开发者已邀请为协作者的账号。'
    Invoke-TesterNative -FilePath $git -Arguments @('-C', $root, 'fetch', '--prune', 'origin', 'main') -FailureMessage '无法从 GitHub 获取更新。请确认已接受私有仓库邀请并完成登录'

    & $git -C $root rev-parse --verify --quiet 'refs/remotes/origin/main' *> $null
    if ($LASTEXITCODE -ne 0) {
        throw '获取完成，但找不到 origin/main。请把本窗口截图发给开发者。'
    }

    Write-TesterStep -Number 3 -Title '强制覆盖本地代码'
    Start-Sleep -Seconds 2
    Invoke-TesterNative -FilePath $git -Arguments @('-C', $root, 'reset', '--hard', 'refs/remotes/origin/main') -FailureMessage '无法将代码重置到 origin/main'
    Invoke-TesterNative -FilePath $git -Arguments @(
        '-C', $root, 'clean', '-fd',
        '-e', '/TESTER_GUIDE.html',
        '-e', '/首次安装.bat',
        '-e', '/强制更新.bat',
        '-e', '/启动测试.bat'
    ) -FailureMessage '无法清理多余的非忽略文件'

    $revision = (& $git -C $root rev-parse --short=12 HEAD | Out-String).Trim()
    Write-Host "代码已更新到：$revision"

    Write-TesterStep -Number 4 -Title '执行离线构建检查'
    try {
        Invoke-TesterOfflineBuildCheck -ProjectRoot $root
    } catch {
        Write-Host ''
        Write-Host '代码更新已经完成，但离线构建检查失败。' -ForegroundColor Yellow
        Write-Host '本次更新可能升级了 Minecraft、Gradle、Fabric 或模组依赖，需要开发者提供新版离线支持包。' -ForegroundColor Yellow
        throw $_
    }

    Write-Host ''
    Write-Host '更新成功。现在可以关闭本窗口，再双击“启动测试.bat”。' -ForegroundColor Green
    exit 0
} catch {
    Write-Host ''
    Write-Host '强制更新未完成：' -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
	Write-Host '请不要反复下载依赖；把本窗口截图发给开发者即可。'
	exit 1
}
