[CmdletBinding()]
param(
    [string]$KitRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$ProjectRoot,
    [string]$ProxyUrl = $env:ECHO_WARRIOR_GIT_PROXY,
    [switch]$ConfirmOverwrite
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'modeler-support-common.ps1')

try {
    Write-ModelerStep -Number 1 -Title '定位模型项目'
    $root = Get-ModelerProjectRoot -KitRoot $KitRoot -PreferredProjectRoot $ProjectRoot
    Write-Host "项目目录：$root"

    if (Test-ModelerClientRunning -ProjectRoot $root) {
        throw '开发客户端仍在运行。请先正常关闭 Minecraft，再执行强制更新。'
    }

    if (-not [string]::IsNullOrWhiteSpace($ProxyUrl)) {
        Set-ModelerGitProxy -ProxyUrl $ProxyUrl
    }

    Write-ModelerStep -Number 2 -Title '获取 GitHub 最新版本'
    $git = Get-ModelerGitExecutable -KitRoot $KitRoot
    Set-ModelerGitConfiguration -GitExecutable $git -ProjectRoot $root -KitRoot $KitRoot
    Invoke-ModelerGit -GitExecutable $git -ProjectRoot $root `
        -Arguments @('fetch', '--prune', 'origin', 'main') -FailureMessage '无法连接 GitHub 获取 main；当前本地文件尚未被覆盖。'

    $changes = @(Get-ModelerWorkingTreeChanges -GitExecutable $git -ProjectRoot $root)
    if ($changes.Count -gt 0) {
        Write-Host ''
        Write-Host '以下未提交内容将从工作目录中移走，并保存到 Git stash：' -ForegroundColor Yellow
        $changes | ForEach-Object { Write-Host "  $_" }
    } else {
        Write-Host '当前没有未提交文件。'
    }

    $currentBranch = Get-ModelerGitOutput -GitExecutable $git -ProjectRoot $root `
        -Arguments @('branch', '--show-current') -FailureMessage '无法读取当前 Git 分支。'
    $localHead = Get-ModelerGitOutput -GitExecutable $git -ProjectRoot $root `
        -Arguments @('rev-parse', 'HEAD') -FailureMessage '无法读取本地提交。'
    $remoteHead = Get-ModelerGitOutput -GitExecutable $git -ProjectRoot $root `
        -Arguments @('rev-parse', 'refs/remotes/origin/main') -FailureMessage '无法读取 GitHub main 提交。'

    Write-Host ''
    Write-Host '警告：接下来会把工作目录切换为 GitHub main 的内容。' -ForegroundColor Red
    Write-Host '未提交文件会自动放入 stash；被覆盖的本地 main 提交会建立备份分支。' -ForegroundColor Yellow
    if (-not $ConfirmOverwrite) {
        $answer = Read-Host '如果确定继续，请输入 COVER（其他输入均取消）'
        if ($answer -cne 'COVER') {
            throw '用户已取消强制覆盖。'
        }
    }

    Write-ModelerStep -Number 3 -Title '自动备份本地工作'
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $backupBranch = $null
    if ($localHead -ne $remoteHead -and ([string]::IsNullOrWhiteSpace($currentBranch) -or $currentBranch -eq 'main')) {
        $backupBranch = "modeler-backup/$stamp"
        Invoke-ModelerGit -GitExecutable $git -ProjectRoot $root `
            -Arguments @('branch', $backupBranch, $localHead) -FailureMessage '无法为本地提交建立备份分支；已停止覆盖。'
        Write-Host "本地提交备份分支：$backupBranch" -ForegroundColor Green
    } elseif ($localHead -ne $remoteHead -and $currentBranch -ne 'main') {
        Write-Host "当前分支 '$currentBranch' 会保留原提交，不会删除该分支。" -ForegroundColor Green
    }

    $stashCreated = $false
    if ($changes.Count -gt 0) {
        Invoke-ModelerGit -GitExecutable $git -ProjectRoot $root `
            -Arguments @('stash', 'push', '--include-untracked', '-m', "modeler-auto-backup-$stamp") `
            -FailureMessage '无法自动备份未提交文件；已停止覆盖。'
        $remainingChanges = @(Get-ModelerWorkingTreeChanges -GitExecutable $git -ProjectRoot $root)
        if ($remainingChanges.Count -gt 0) {
            Write-Host '自动备份后仍存在以下文件：' -ForegroundColor Yellow
            $remainingChanges | ForEach-Object { Write-Host "  $_" }
            throw '工作目录仍不干净，为安全起见已停止覆盖。'
        }
        $stashCreated = $true
        Write-Host '未提交文件已保存到 Git stash。' -ForegroundColor Green
    }

    Write-ModelerStep -Number 4 -Title '强制切换到 GitHub main'
    Invoke-ModelerGit -GitExecutable $git -ProjectRoot $root `
        -Arguments @('checkout', '-B', 'main', 'refs/remotes/origin/main') -FailureMessage '无法切换到 GitHub main。自动备份仍然保留。'
    Invoke-ModelerGit -GitExecutable $git -ProjectRoot $root `
        -Arguments @('clean', '-fd') -FailureMessage '无法清理残留的非忽略文件。'

    Write-Host ''
    Write-Host '强制覆盖更新完成，当前项目已与 GitHub main 一致。' -ForegroundColor Green
    if ($stashCreated) {
        Write-Host '未提交文件备份：运行 git stash list 可以看到 modeler-auto-backup。' -ForegroundColor Cyan
    }
    if ($backupBranch) {
        Write-Host "本地提交备份：$backupBranch" -ForegroundColor Cyan
    }
    Write-Host '如需恢复备份，请联系项目负责人或让 AI 协助，不要自行反复强制更新。' -ForegroundColor Yellow
    exit 0
} catch {
    Write-Host ''
    Write-Host "强制更新未完成：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
