[CmdletBinding()]
param(
    [string]$VersionDate = (Get-Date -Format 'yyyy-MM-dd'),
    [string]$PortableGitSource
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$deliveryRoot = Join-Path $projectRoot 'temporary-delivery'
$packageName = "Echo-Warrior-Modeler-Tools-$VersionDate"
$packageRoot = Join-Path $deliveryRoot $packageName
$zipPath = Join-Path $deliveryRoot "$packageName.zip"
$checksumPath = "$zipPath.sha256.txt"

function Assert-DeliveryChildPath {
    param([Parameter(Mandatory = $true)][string]$Path)

    $deliveryFull = [IO.Path]::GetFullPath($deliveryRoot).TrimEnd('\') + '\'
    $candidateFull = [IO.Path]::GetFullPath($Path)
    if (-not $candidateFull.StartsWith($deliveryFull, [StringComparison]::OrdinalIgnoreCase)) {
        throw "拒绝操作 temporary-delivery 之外的路径：$candidateFull"
    }
}

if ([string]::IsNullOrWhiteSpace($PortableGitSource)) {
    $candidates = @(
        (Join-Path $deliveryRoot '_staging\portable-git'),
        (Join-Path $deliveryRoot 'Echo-Warrior-Tester-Kit-2026-08-27\.toolchains\portable-git')
    )
    $PortableGitSource = $candidates | Where-Object {
        Test-Path -LiteralPath (Join-Path $_ 'cmd\git.exe') -PathType Leaf
    } | Select-Object -First 1
}

if ([string]::IsNullOrWhiteSpace($PortableGitSource) -or
    -not (Test-Path -LiteralPath (Join-Path $PortableGitSource 'cmd\git.exe') -PathType Leaf)) {
    throw '找不到已经解压的 PortableGit。请通过 -PortableGitSource 指定包含 cmd\git.exe 的目录。'
}

New-Item -ItemType Directory -Path $deliveryRoot -Force | Out-Null
foreach ($target in @($packageRoot, $zipPath, $checksumPath)) {
    Assert-DeliveryChildPath -Path $target
    if (Test-Path -LiteralPath $target) {
        Remove-Item -LiteralPath $target -Recurse -Force
    }
}

$kitRoot = Join-Path $packageRoot '.modeler-kit'
$scriptTarget = Join-Path $kitRoot 'scripts'
$portableGitTarget = Join-Path $kitRoot 'portable-git'
New-Item -ItemType Directory -Path $scriptTarget -Force | Out-Null

$rootFiles = [ordered]@{
    'MODELER_GUIDE.html' = '模型师工具说明.html'
    '安全更新项目.bat' = '安全更新项目.bat'
    '强制覆盖更新（自动备份）.bat' = '强制覆盖更新（自动备份）.bat'
    '启动开发客户端.bat' = '启动开发客户端.bat'
    '重新选择项目目录.bat' = '重新选择项目目录.bat'
}
foreach ($entry in $rootFiles.GetEnumerator()) {
    $source = Join-Path $projectRoot $entry.Key
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
        throw "缺少模型师工具源文件：$($entry.Key)"
    }
    $destination = Join-Path $packageRoot $entry.Value
    Copy-Item -LiteralPath $source -Destination $destination -Force
    if ([IO.Path]::GetExtension($destination) -ieq '.bat') {
        $batchText = (Get-Content -LiteralPath $destination -Raw) -replace "`r?`n", "`r`n"
        [IO.File]::WriteAllText($destination, $batchText, [Text.UTF8Encoding]::new($false))
    }
}

$scriptFiles = @(
    'modeler-support-common.ps1',
    'modeler-safe-update.ps1',
    'modeler-force-update.ps1',
    'modeler-launch.ps1',
    'modeler-select-project.ps1'
)
foreach ($name in $scriptFiles) {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot $name) -Destination (Join-Path $scriptTarget $name) -Force
}

Write-Host '复制 PortableGit...'
$null = & robocopy.exe $PortableGitSource $portableGitTarget /E /R:2 /W:1 /NFL /NDL /NJH /NJS /NP
if ($LASTEXITCODE -gt 7) {
    throw "PortableGit 复制失败（robocopy 退出代码 $LASTEXITCODE）。"
}

$manifest = @"
Echo Warrior 模型师便携工具包
生成日期：$VersionDate

用途：
- 为已经配置好项目与 Java 25 的模型师提供便携式 Git 更新。
- 提供安全更新、自动备份后强制覆盖、开发客户端启动和重新选择项目目录入口。

包含：
- PortableGit（含 Git Credential Manager）
- 模型师更新/启动 PowerShell 脚本
- 中文 HTML 使用说明
- 更新 BAT 为当前进程临时设置 http://127.0.0.1:7897，不写入系统或 Git 全局代理

不包含：
- 项目源码或 .git 仓库
- Java/JDK
- Gradle 离线缓存
- Minecraft 资源、依赖或测试世界

使用要求：
- 整个工具包必须放在 Echo Warrior Git 项目目录之外。
- 第一次运行时选择现有项目根目录。
- 私有 GitHub 仓库访问权限由项目负责人提前添加。
- 更新前需要启动本机 HTTP 代理，并确保监听 127.0.0.1:7897。
"@
Set-Content -LiteralPath (Join-Path $packageRoot 'PACKAGE_MANIFEST.txt') -Value $manifest -Encoding UTF8

$requiredFiles = @(
    '模型师工具说明.html',
    '安全更新项目.bat',
    '强制覆盖更新（自动备份）.bat',
    '启动开发客户端.bat',
    '重新选择项目目录.bat',
    '.modeler-kit\portable-git\cmd\git.exe',
    '.modeler-kit\portable-git\mingw64\bin\git-credential-manager.exe',
    '.modeler-kit\scripts\modeler-safe-update.ps1',
    '.modeler-kit\scripts\modeler-force-update.ps1',
    '.modeler-kit\scripts\modeler-launch.ps1'
)
foreach ($relativePath in $requiredFiles) {
    if (-not (Test-Path -LiteralPath (Join-Path $packageRoot $relativePath) -PathType Leaf)) {
        throw "生成的工具包缺少：$relativePath"
    }
}

Write-Host '压缩模型师工具包...'
& tar.exe -a -c -f $zipPath -C $deliveryRoot $packageName
if ($LASTEXITCODE -ne 0) {
    throw "ZIP 压缩失败（退出代码 $LASTEXITCODE）。"
}

$hash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath $checksumPath -Value "$hash  $packageName.zip" -Encoding ASCII

$zipSizeMiB = [math]::Round((Get-Item -LiteralPath $zipPath).Length / 1MB, 2)
Write-Host ''
Write-Host "模型师工具包已生成：$zipPath" -ForegroundColor Green
Write-Host "大小：$zipSizeMiB MiB"
Write-Host "SHA-256：$hash"
