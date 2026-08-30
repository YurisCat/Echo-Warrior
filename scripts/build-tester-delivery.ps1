[CmdletBinding()]
param(
    [string]$PortableGitRoot = '',
    [string]$GradleUserHomeSource = '',
    [string]$OutputRoot = '',
    [string]$PackageDate = (Get-Date -Format 'yyyy-MM-dd'),
    [switch]$SkipArchive
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path -LiteralPath (Split-Path -Parent $PSScriptRoot)).Path

if ([string]::IsNullOrWhiteSpace($PortableGitRoot)) {
    $PortableGitRoot = Join-Path $projectRoot 'temporary-delivery\_staging\portable-git'
}
if ([string]::IsNullOrWhiteSpace($GradleUserHomeSource)) {
    $GradleUserHomeSource = Join-Path $projectRoot 'temporary-delivery\_staging\gradle-user-home'
}
if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $projectRoot 'temporary-delivery'
}

$portableRoot = (Resolve-Path -LiteralPath $PortableGitRoot).Path
$gradleSource = (Resolve-Path -LiteralPath $GradleUserHomeSource).Path
$output = (Resolve-Path -LiteralPath $OutputRoot).Path
$portableGit = Join-Path $portableRoot 'cmd\git.exe'
$sourceGit = (Get-Command git.exe).Source
$packageName = "Echo-Warrior-Tester-Kit-$PackageDate"
$packageRoot = Join-Path $output $packageName
$archivePath = Join-Path $output "$packageName.zip"
$checksumPath = "$archivePath.sha256.txt"

function Assert-SafeOutputTarget {
    param([Parameter(Mandatory = $true)][string]$Path)

    $full = [IO.Path]::GetFullPath($Path)
    if (-not $full.StartsWith($output, [StringComparison]::OrdinalIgnoreCase)) {
        throw "拒绝处理交付目录以外的路径：$full"
    }
    if ((Split-Path -Leaf $full) -notlike 'Echo-Warrior-Tester-Kit-*') {
        throw "拒绝处理名称不符合交付包规则的路径：$full"
    }
}

function Copy-Tree {
    param(
        [Parameter(Mandatory = $true)][string]$Source,
        [Parameter(Mandatory = $true)][string]$Destination,
        [string[]]$ExcludedDirectories = @(),
        [string[]]$ExcludedFiles = @()
    )

    if (-not (Test-Path -LiteralPath $Source)) {
        throw "复制源不存在：$Source"
    }
    New-Item -ItemType Directory -Path $Destination -Force | Out-Null

    $arguments = @($Source, $Destination, '/E', '/COPY:DAT', '/DCOPY:DAT', '/R:1', '/W:1', '/NFL', '/NDL', '/NP', '/NJH', '/NJS')
    if ($ExcludedDirectories.Count -gt 0) {
        $arguments += '/XD'
        $arguments += $ExcludedDirectories
    }
    if ($ExcludedFiles.Count -gt 0) {
        $arguments += '/XF'
        $arguments += $ExcludedFiles
    }

    & robocopy.exe @arguments | Out-Null
    if ($LASTEXITCODE -gt 7) {
        throw "Robocopy 失败：$Source → $Destination（退出代码 $LASTEXITCODE）"
    }
}

function Copy-CurrentTrackedTree {
    param([Parameter(Mandatory = $true)][string]$DestinationRoot)

    $trackedFiles = & $sourceGit -c core.quotepath=false -C $projectRoot ls-files
    if ($LASTEXITCODE -ne 0) {
        throw '无法读取当前仓库的受管理文件清单。'
    }

    foreach ($relativePath in $trackedFiles) {
        $sourcePath = Join-Path $projectRoot $relativePath
        $destinationPath = Join-Path $DestinationRoot $relativePath
        if (Test-Path -LiteralPath $sourcePath -PathType Leaf) {
            $destinationDirectory = Split-Path -Parent $destinationPath
            New-Item -ItemType Directory -Path $destinationDirectory -Force | Out-Null
            Copy-Item -LiteralPath $sourcePath -Destination $destinationPath -Force
        } elseif (Test-Path -LiteralPath $destinationPath) {
            Remove-Item -LiteralPath $destinationPath -Force
        }
    }
}

if (-not (Test-Path -LiteralPath $portableGit -PathType Leaf)) {
    throw "PortableGit 不完整：$portableGit"
}

Assert-SafeOutputTarget -Path $packageRoot
Assert-SafeOutputTarget -Path $archivePath

if (Test-Path -LiteralPath $packageRoot) {
    Remove-Item -LiteralPath $packageRoot -Recurse -Force
}
if (Test-Path -LiteralPath $archivePath) {
    Remove-Item -LiteralPath $archivePath -Force
}
if (Test-Path -LiteralPath $checksumPath) {
    Remove-Item -LiteralPath $checksumPath -Force
}

Write-Host '[1/7] 创建干净 Git 工作目录...' -ForegroundColor Cyan
New-Item -ItemType Directory -Path $packageRoot -Force | Out-Null
Copy-Tree -Source (Join-Path $projectRoot '.git') -Destination (Join-Path $packageRoot '.git') -ExcludedDirectories @('worktrees') -ExcludedFiles @('*.lock', '*.lck')
& $sourceGit -C $packageRoot remote set-url origin 'https://github.com/YurisCat/Echo-Warrior.git'
if ($LASTEXITCODE -ne 0) {
    throw '无法设置 GitHub 远程地址。'
}

Write-Host '[2/7] 复制当前受管理文件快照...' -ForegroundColor Cyan
Copy-CurrentTrackedTree -DestinationRoot $packageRoot
& $sourceGit -C $packageRoot config user.name 'Echo Warrior Offline Pack'
& $sourceGit -C $packageRoot config user.email 'offline-pack@local.invalid'
& $sourceGit -C $packageRoot add --update
& $sourceGit -C $packageRoot diff --cached --quiet
if ($LASTEXITCODE -eq 1) {
    & $sourceGit -C $packageRoot commit -m "Offline tester snapshot $PackageDate"
    if ($LASTEXITCODE -ne 0) {
        throw '无法创建交付包本地快照提交。'
    }
} elseif ($LASTEXITCODE -ne 0) {
    throw '无法检查交付包快照差异。'
}

Write-Host '[3/7] 复制测试员入口和离线工具...' -ForegroundColor Cyan
$rootSupportFiles = @('TESTER_GUIDE.html', '首次安装.bat', '强制更新.bat', '启动测试.bat')
foreach ($relativePath in $rootSupportFiles) {
    $destination = Join-Path $packageRoot $relativePath
    Copy-Item -LiteralPath (Join-Path $projectRoot $relativePath) -Destination $destination -Force
    if ([IO.Path]::GetExtension($destination) -ieq '.bat') {
        $batchText = (Get-Content -LiteralPath $destination -Raw) -replace "`r?`n", "`r`n"
        [IO.File]::WriteAllText($destination, $batchText, [Text.UTF8Encoding]::new($false))
    }
}

$testerKit = Join-Path $packageRoot '.toolchains\tester-kit'
New-Item -ItemType Directory -Path $testerKit -Force | Out-Null
Get-ChildItem -LiteralPath (Join-Path $projectRoot 'scripts') -Filter 'tester-*.ps1' -File |
    ForEach-Object { Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $testerKit $_.Name) -Force }

$excludePath = Join-Path $packageRoot '.git\info\exclude'
Add-Content -LiteralPath $excludePath -Encoding UTF8 -Value @(
    '',
    '# Echo Warrior tester kit files preserved across git clean',
    '/TESTER_GUIDE.html',
    '/首次安装.bat',
    '/强制更新.bat',
    '/启动测试.bat'
)

Write-Host '[4/7] 复制 Java、PortableGit、测试世界和 Gradle 缓存...' -ForegroundColor Cyan
Copy-Tree -Source (Join-Path $projectRoot '.toolchains\jdk-25') -Destination (Join-Path $packageRoot '.toolchains\jdk-25')
Copy-Tree -Source $portableRoot -Destination (Join-Path $packageRoot '.toolchains\portable-git') -ExcludedDirectories @('tmp') -ExcludedFiles @('*.lock', '*.lck')
Copy-Tree -Source $gradleSource -Destination (Join-Path $packageRoot '.toolchains\gradle-user-home') -ExcludedDirectories @('daemon', 'workers', '.tmp') -ExcludedFiles @('*.lock', '*.lck')
Copy-Tree -Source (Join-Path $projectRoot '.gradle\loom-cache') -Destination (Join-Path $packageRoot '.gradle\loom-cache') -ExcludedFiles @('*.lock', '*.lck')
Copy-Tree -Source (Join-Path $projectRoot 'run\saves\CATTEST') -Destination (Join-Path $packageRoot 'run\saves\CATTEST') -ExcludedFiles @('session.lock')

Write-Host '[5/7] 写入交付清单...' -ForegroundColor Cyan
$snapshotRevision = (& $sourceGit -C $packageRoot rev-parse HEAD | Out-String).Trim()
$sourceRevision = (& $sourceGit -C $projectRoot rev-parse HEAD | Out-String).Trim()
$sourceStatus = (& $sourceGit -C $projectRoot status --short | Out-String).TrimEnd()
$manifest = @(
    'Echo Warrior Windows x64 Tester Kit',
    "package_date=$PackageDate",
    "source_head=$sourceRevision",
    "package_snapshot=$snapshotRevision",
    'branch=main',
    'origin=https://github.com/YurisCat/Echo-Warrior.git',
    'portable_git=2.55.0.windows.5',
    'java=25',
    'gradle=9.5.1',
    'minecraft=26.1.2',
    'fabric_loader=0.19.3',
    'fabric_api=0.155.2+26.1.2',
    'smartbrainlib=2.0.0',
    'geckolib=5.5.2',
    'test_world=CATTEST',
    '',
    'Source working tree status at packaging time:',
    $(if ([string]::IsNullOrWhiteSpace($sourceStatus)) { '(clean)' } else { $sourceStatus })
)
Set-Content -LiteralPath (Join-Path $packageRoot 'OFFLINE_PACKAGE_MANIFEST.txt') -Encoding UTF8 -Value $manifest
Add-Content -LiteralPath $excludePath -Encoding UTF8 -Value '/OFFLINE_PACKAGE_MANIFEST.txt'

Write-Host '[6/7] 验证完全离线编译和客户端运行配置...' -ForegroundColor Cyan
$packageJdk = Join-Path $packageRoot '.toolchains\jdk-25'
$packageGradleHome = Join-Path $packageRoot '.toolchains\gradle-user-home'
$oldJavaHome = $env:JAVA_HOME
$oldGradleUserHome = $env:GRADLE_USER_HOME
$oldPath = $env:Path
try {
    $env:JAVA_HOME = $packageJdk
    $env:GRADLE_USER_HOME = $packageGradleHome
    $env:Path = "$(Join-Path $packageJdk 'bin');$oldPath"
    Push-Location $packageRoot
    try {
        & (Join-Path $packageRoot 'gradlew.bat') '--offline' '--no-daemon' 'classes' 'downloadAssets' 'configureClientLaunch'
        if ($LASTEXITCODE -ne 0) {
            throw "交付包离线验证失败（退出代码 $LASTEXITCODE）。"
        }
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_HOME = $oldJavaHome
    $env:GRADLE_USER_HOME = $oldGradleUserHome
    $env:Path = $oldPath
}

& $portableGit --version
if ($LASTEXITCODE -ne 0) {
    throw '包内 PortableGit 无法运行。'
}
& $portableGit -C $packageRoot status --short
if ($LASTEXITCODE -ne 0) {
    throw '交付包 Git 状态检查失败。'
}

if (-not $SkipArchive) {
    Write-Host '[7/7] 生成 ZIP 与 SHA-256...' -ForegroundColor Cyan
    Push-Location $output
    try {
        & tar.exe -a -c -f $archivePath $packageName
        if ($LASTEXITCODE -ne 0) {
            throw "ZIP 生成失败（退出代码 $LASTEXITCODE）。"
        }
    } finally {
        Pop-Location
    }

    $archiveHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
    Set-Content -LiteralPath $checksumPath -Encoding ASCII -Value ("{0}  {1}" -f $archiveHash, (Split-Path -Leaf $archivePath))
    Write-Host "ZIP：$archivePath"
    Write-Host "SHA-256：$archiveHash"
} else {
    Write-Host '[7/7] 已按参数跳过 ZIP。' -ForegroundColor Yellow
}

$packageBytes = (Get-ChildItem -LiteralPath $packageRoot -Recurse -File -Force -ErrorAction SilentlyContinue | Measure-Object Length -Sum).Sum
Write-Host ("交付目录大小：{0:N2} GiB" -f ($packageBytes / 1GB)) -ForegroundColor Green
Write-Host '测试员离线包构建完成。' -ForegroundColor Green
