[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:TesterOriginUrl = 'https://github.com/YurisCat/Echo-Warrior.git'

function Resolve-TesterProjectRoot {
    param([Parameter(Mandatory = $true)][string]$Path)

    $resolved = (Resolve-Path -LiteralPath $Path).Path.TrimEnd('\')
    if (-not (Test-Path -LiteralPath (Join-Path $resolved 'gradlew.bat') -PathType Leaf)) {
        throw "所选目录不是 Echo Warrior 项目根目录：$resolved"
    }
    return $resolved
}

function Write-TesterStep {
    param(
        [Parameter(Mandatory = $true)][int]$Number,
        [Parameter(Mandatory = $true)][string]$Title
    )

    Write-Host ''
    Write-Host ("[{0}] {1}" -f $Number, $Title) -ForegroundColor Cyan
}

function Get-TesterGitExecutable {
    param([Parameter(Mandatory = $true)][string]$ProjectRoot)

    $portableGit = Join-Path $ProjectRoot '.toolchains\portable-git\cmd\git.exe'
    if (Test-Path -LiteralPath $portableGit -PathType Leaf) {
        return $portableGit
    }

    $systemGit = Get-Command git.exe -ErrorAction SilentlyContinue
    if ($systemGit) {
        return $systemGit.Source
    }

    throw '找不到包内 PortableGit，也找不到系统 Git。请重新解压完整离线包。'
}

function Invoke-TesterNative {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [string]$FailureMessage = '命令执行失败。'
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw ("{0}（退出代码 {1}）" -f $FailureMessage, $LASTEXITCODE)
    }
}

function Set-TesterGitConfiguration {
    param(
        [Parameter(Mandatory = $true)][string]$GitExecutable,
        [Parameter(Mandatory = $true)][string]$ProjectRoot
    )

    $remoteUrl = & $GitExecutable -C $ProjectRoot remote get-url origin 2>$null
    if ($LASTEXITCODE -ne 0) {
        Invoke-TesterNative -FilePath $GitExecutable -Arguments @('-C', $ProjectRoot, 'remote', 'add', 'origin', $script:TesterOriginUrl) -FailureMessage '无法添加 GitHub 远程仓库'
    } elseif (($remoteUrl | Out-String).Trim() -ne $script:TesterOriginUrl) {
        Invoke-TesterNative -FilePath $GitExecutable -Arguments @('-C', $ProjectRoot, 'remote', 'set-url', 'origin', $script:TesterOriginUrl) -FailureMessage '无法修正 GitHub 远程仓库地址'
    }

    Invoke-TesterNative -FilePath $GitExecutable -Arguments @('-C', $ProjectRoot, 'config', '--local', 'core.longpaths', 'true') -FailureMessage '无法启用长路径支持'
    Invoke-TesterNative -FilePath $GitExecutable -Arguments @('-C', $ProjectRoot, 'config', '--local', 'fetch.prune', 'true') -FailureMessage '无法设置远程清理选项'

    $credentialManager = Join-Path $ProjectRoot '.toolchains\portable-git\mingw64\bin\git-credential-manager.exe'
    if (Test-Path -LiteralPath $credentialManager -PathType Leaf) {
        Invoke-TesterNative -FilePath $GitExecutable -Arguments @('-C', $ProjectRoot, 'config', '--local', 'credential.helper', 'manager') -FailureMessage '无法设置 GitHub 登录凭据管理器'
    }
}

function Assert-TesterEnvironment {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [switch]$RequireWorld
    )

    if (-not [Environment]::Is64BitOperatingSystem) {
        throw '本离线包仅支持 Windows 10/11 x64。'
    }

    $requiredFiles = @(
        '.git\HEAD',
        '.toolchains\jdk-25\bin\java.exe',
        '.toolchains\gradle-user-home\init.d\echo-warrior-offline.gradle',
        'gradlew.bat',
        'scripts\playtest-now.ps1'
    )

    foreach ($relativePath in $requiredFiles) {
        $fullPath = Join-Path $ProjectRoot $relativePath
        if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
            throw "离线包缺少必要文件：$relativePath"
        }
    }

    if ($RequireWorld) {
        $worldLevel = Join-Path $ProjectRoot 'run\saves\CATTEST\level.dat'
        if (-not (Test-Path -LiteralPath $worldLevel -PathType Leaf)) {
            throw '缺少 CATTEST 测试世界：run\saves\CATTEST\level.dat'
        }
    }
}

function Test-TesterClientRunning {
    param([Parameter(Mandatory = $true)][string]$ProjectRoot)

    $escapedRoot = [regex]::Escape($ProjectRoot)
    $process = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Name -match '^javaw?\.exe$' -and
            $_.CommandLine -match $escapedRoot -and
            $_.CommandLine -match 'net\.minecraft\.client\.main\.Main'
        } |
        Select-Object -First 1

    return $null -ne $process
}

function Invoke-TesterOfflineBuildCheck {
    param([Parameter(Mandatory = $true)][string]$ProjectRoot)

    $jdkRoot = Join-Path $ProjectRoot '.toolchains\jdk-25'
    $gradleHome = Join-Path $ProjectRoot '.toolchains\gradle-user-home'
    $gradleWrapper = Join-Path $ProjectRoot 'gradlew.bat'

    $oldJavaHome = $env:JAVA_HOME
    $oldGradleUserHome = $env:GRADLE_USER_HOME
    $oldPath = $env:Path

    try {
        $env:JAVA_HOME = $jdkRoot
        $env:GRADLE_USER_HOME = $gradleHome
        $env:Path = "$(Join-Path $jdkRoot 'bin');$oldPath"

        Push-Location $ProjectRoot
        try {
            & $gradleWrapper '--offline' '--no-daemon' 'classes'
            if ($LASTEXITCODE -ne 0) {
                throw "离线构建检查失败（退出代码 $LASTEXITCODE）。"
            }
        } finally {
            Pop-Location
        }
    } finally {
        $env:JAVA_HOME = $oldJavaHome
        $env:GRADLE_USER_HOME = $oldGradleUserHome
		$env:Path = $oldPath
	}
}
