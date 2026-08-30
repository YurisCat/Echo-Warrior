[CmdletBinding()]
param(
    [string]$KitRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$ProjectRoot,
    [string]$ProxyUrl = $env:ECHO_WARRIOR_GIT_PROXY
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'modeler-support-common.ps1')

try {
    Write-ModelerStep -Number 1 -Title '定位模型项目'
    $root = Get-ModelerProjectRoot -KitRoot $KitRoot -PreferredProjectRoot $ProjectRoot
    Write-Host "项目目录：$root"

    if (Test-ModelerClientRunning -ProjectRoot $root) {
        throw '开发客户端仍在运行。请先正常关闭 Minecraft，再执行更新。'
    }

    if (-not [string]::IsNullOrWhiteSpace($ProxyUrl)) {
        Set-ModelerGitProxy -ProxyUrl $ProxyUrl
    }

    Write-ModelerStep -Number 2 -Title '检查 Git 和本地模型文件'
    $git = Get-ModelerGitExecutable -KitRoot $KitRoot
    Set-ModelerGitConfiguration -GitExecutable $git -ProjectRoot $root -KitRoot $KitRoot

    $branch = Get-ModelerGitOutput -GitExecutable $git -ProjectRoot $root `
        -Arguments @('branch', '--show-current') -FailureMessage '无法读取当前 Git 分支。'
    if ($branch -ne 'main') {
        throw "安全更新只允许在 main 分支运行；当前分支是 '$branch'。请联系项目负责人处理。"
    }

    $changes = @(Get-ModelerWorkingTreeChanges -GitExecutable $git -ProjectRoot $root)
    if ($changes.Count -gt 0) {
        Write-Host ''
        Write-Host '发现尚未保存到 Git 的文件，安全更新已停止：' -ForegroundColor Yellow
        $changes | ForEach-Object { Write-Host "  $_" }
        Write-Host ''
        throw '请先提交/交付这些模型文件；如果确定全部不要了，才使用“强制覆盖更新（自动备份）”。'
    }

    Write-ModelerStep -Number 3 -Title '从 GitHub 获取最新版本'
    Invoke-ModelerGit -GitExecutable $git -ProjectRoot $root `
        -Arguments @('fetch', '--prune', 'origin', 'main') -FailureMessage '无法连接 GitHub 获取 main。首次使用时请在弹出的窗口中登录。'

    $countsText = Get-ModelerGitOutput -GitExecutable $git -ProjectRoot $root `
        -Arguments @('rev-list', '--left-right', '--count', 'HEAD...refs/remotes/origin/main') `
        -FailureMessage '无法比较本地与 GitHub 版本。'
    $counts = @($countsText -split '\s+')
    if ($counts.Count -lt 2) {
        throw "无法解析 Git 版本差异：$countsText"
    }
    $ahead = [int]$counts[0]
    $behind = [int]$counts[1]

    if ($ahead -gt 0) {
        throw "本地 main 有 $ahead 个尚未合入 GitHub 的提交。为避免覆盖，安全更新已停止，请联系项目负责人。"
    }

    if ($behind -gt 0) {
        Invoke-ModelerGit -GitExecutable $git -ProjectRoot $root `
            -Arguments @('merge', '--ff-only', 'refs/remotes/origin/main') -FailureMessage '无法以安全的快进方式更新项目。'
        Write-Host "已安全更新 $behind 个远程提交。" -ForegroundColor Green
    } else {
        Write-Host '项目已经是 GitHub 上的最新版本。' -ForegroundColor Green
    }

    Write-Host ''
    Write-Host '安全更新完成。本脚本没有删除或覆盖任何本地模型改动。' -ForegroundColor Green
    exit 0
} catch {
    Write-Host ''
    Write-Host "更新未完成：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
