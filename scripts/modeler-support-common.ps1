[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$script:ModelerExpectedOrigin = 'https://github.com/YurisCat/Echo-Warrior.git'

function Write-ModelerStep {
    param(
        [Parameter(Mandatory = $true)][int]$Number,
        [Parameter(Mandatory = $true)][string]$Title
    )

    Write-Host ''
    Write-Host ("[{0}] {1}" -f $Number, $Title) -ForegroundColor Cyan
}

function Test-ModelerProjectRoot {
    param([Parameter(Mandatory = $true)][string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return $false
    }

    return (
        (Test-Path -LiteralPath (Join-Path $Path '.git') -PathType Container) -and
        (Test-Path -LiteralPath (Join-Path $Path 'gradlew.bat') -PathType Leaf) -and
        (Test-Path -LiteralPath (Join-Path $Path 'settings.gradle') -PathType Leaf)
    )
}

function Select-ModelerProjectRoot {
    param([Parameter(Mandatory = $true)][string]$KitRoot)

    $selectedPath = $null
    try {
        $shell = New-Object -ComObject Shell.Application
        $folder = $shell.BrowseForFolder(
            0,
            '请选择 Echo Warrior 项目根目录（里面应有 .git、gradlew.bat 和 settings.gradle）',
            0,
            0
        )
        if ($folder) {
            $selectedPath = $folder.Self.Path
        }
    } catch {
        Write-Host '无法打开目录选择窗口，将改为手动输入路径。' -ForegroundColor Yellow
    }

    if ([string]::IsNullOrWhiteSpace($selectedPath)) {
        $selectedPath = Read-Host '请粘贴 Echo Warrior 项目根目录的完整路径'
    }

    if (-not (Test-ModelerProjectRoot -Path $selectedPath)) {
        throw "所选目录不是有效的 Echo Warrior Git 项目根目录：$selectedPath"
    }

    $resolved = (Resolve-Path -LiteralPath $selectedPath).Path.TrimEnd('\')
    $stateDirectory = Join-Path $KitRoot 'state'
    New-Item -ItemType Directory -Path $stateDirectory -Force | Out-Null
    Set-Content -LiteralPath (Join-Path $stateDirectory 'project-root.txt') -Value $resolved -Encoding UTF8
    return $resolved
}

function Get-ModelerProjectRoot {
    param(
        [Parameter(Mandatory = $true)][string]$KitRoot,
        [string]$PreferredProjectRoot,
        [switch]$ForceSelection
    )

    $resolvedKitRoot = (Resolve-Path -LiteralPath $KitRoot).Path.TrimEnd('\')

    if (-not [string]::IsNullOrWhiteSpace($PreferredProjectRoot)) {
        if (-not (Test-ModelerProjectRoot -Path $PreferredProjectRoot)) {
            throw "指定目录不是有效的 Echo Warrior Git 项目根目录：$PreferredProjectRoot"
        }
        $projectRoot = (Resolve-Path -LiteralPath $PreferredProjectRoot).Path.TrimEnd('\')
    } else {
        $pathFile = Join-Path $resolvedKitRoot 'state\project-root.txt'
        $projectRoot = $null

        if (-not $ForceSelection -and (Test-Path -LiteralPath $pathFile -PathType Leaf)) {
            $savedPath = (Get-Content -LiteralPath $pathFile -Raw).Trim()
            if (Test-ModelerProjectRoot -Path $savedPath) {
                $projectRoot = (Resolve-Path -LiteralPath $savedPath).Path.TrimEnd('\')
            }
        }

        if ([string]::IsNullOrWhiteSpace($projectRoot)) {
            $projectRoot = Select-ModelerProjectRoot -KitRoot $resolvedKitRoot
        }
    }

    $projectPrefix = $projectRoot.TrimEnd('\') + '\'
    if ($resolvedKitRoot.Equals($projectRoot, [StringComparison]::OrdinalIgnoreCase) -or
        $resolvedKitRoot.StartsWith($projectPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw '模型师工具包不能放在 Git 项目目录里面。请把整个工具包移动到桌面或其他目录后再运行。'
    }

    return $projectRoot
}

function Get-ModelerGitExecutable {
    param([Parameter(Mandatory = $true)][string]$KitRoot)

    $portableGit = Join-Path $KitRoot 'portable-git\cmd\git.exe'
    if (Test-Path -LiteralPath $portableGit -PathType Leaf) {
        return $portableGit
    }

    $systemGit = Get-Command git.exe -ErrorAction SilentlyContinue
    if ($systemGit) {
        return $systemGit.Source
    }

    throw '工具包内的 PortableGit 不完整，系统中也找不到 Git。请重新解压完整模型师工具包。'
}

function Set-ModelerGitProxy {
    param([Parameter(Mandatory = $true)][string]$ProxyUrl)

    $uri = $null
    if (-not [Uri]::TryCreate($ProxyUrl, [UriKind]::Absolute, [ref]$uri) -or
        $uri.Scheme -notin @('http', 'https') -or
        [string]::IsNullOrWhiteSpace($uri.Host)) {
        throw "GitHub 代理地址无效：$ProxyUrl"
    }

    $normalized = $uri.AbsoluteUri.TrimEnd('/')
    $env:HTTP_PROXY = $normalized
    $env:HTTPS_PROXY = $normalized
    Write-Host "GitHub 临时代理：$normalized" -ForegroundColor DarkCyan
}

function Invoke-ModelerGit {
    param(
        [Parameter(Mandatory = $true)][string]$GitExecutable,
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [string]$FailureMessage = 'Git 命令执行失败。'
    )

    $allArguments = @('-C', $ProjectRoot) + $Arguments
    & $GitExecutable @allArguments
    if ($LASTEXITCODE -ne 0) {
        throw ("{0}（退出代码 {1}）" -f $FailureMessage, $LASTEXITCODE)
    }
}

function Get-ModelerGitOutput {
    param(
        [Parameter(Mandatory = $true)][string]$GitExecutable,
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [string]$FailureMessage = 'Git 命令执行失败。'
    )

    $allArguments = @('-C', $ProjectRoot) + $Arguments
    $output = & $GitExecutable @allArguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        $detail = (($output | ForEach-Object { "$_" }) -join "`n").Trim()
        if ($detail) {
            throw ("{0}`n{1}" -f $FailureMessage, $detail)
        }
        throw $FailureMessage
    }

    return (($output | ForEach-Object { "$_" }) -join "`n").Trim()
}

function Set-ModelerGitConfiguration {
    param(
        [Parameter(Mandatory = $true)][string]$GitExecutable,
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string]$KitRoot
    )

    $originUrl = Get-ModelerGitOutput -GitExecutable $GitExecutable -ProjectRoot $ProjectRoot `
        -Arguments @('config', '--get', 'remote.origin.url') -FailureMessage '项目没有可用的 origin 远程仓库。'

    $normalizedOrigin = $originUrl.Trim().TrimEnd('/').ToLowerInvariant() -replace '\.git$', ''
    $acceptedOrigins = @(
        'https://github.com/yuriscat/echo-warrior',
        'git@github.com:yuriscat/echo-warrior',
        'ssh://git@github.com/yuriscat/echo-warrior'
    )
    if ($acceptedOrigins -notcontains $normalizedOrigin) {
        throw "当前 origin 不是 Echo Warrior 仓库：$originUrl"
    }

    Invoke-ModelerGit -GitExecutable $GitExecutable -ProjectRoot $ProjectRoot `
        -Arguments @('config', '--local', 'core.longpaths', 'true') -FailureMessage '无法启用 Git 长路径支持。'
    Invoke-ModelerGit -GitExecutable $GitExecutable -ProjectRoot $ProjectRoot `
        -Arguments @('config', '--local', 'fetch.prune', 'true') -FailureMessage '无法设置 Git 远程清理选项。'

    $credentialManager = Join-Path $KitRoot 'portable-git\mingw64\bin\git-credential-manager.exe'
    if (Test-Path -LiteralPath $credentialManager -PathType Leaf) {
        Invoke-ModelerGit -GitExecutable $GitExecutable -ProjectRoot $ProjectRoot `
            -Arguments @('config', '--local', 'credential.helper', 'manager') -FailureMessage '无法配置 GitHub 登录管理器。'
    }
}

function Get-ModelerWorkingTreeChanges {
    param(
        [Parameter(Mandatory = $true)][string]$GitExecutable,
        [Parameter(Mandatory = $true)][string]$ProjectRoot
    )

    $text = Get-ModelerGitOutput -GitExecutable $GitExecutable -ProjectRoot $ProjectRoot `
        -Arguments @('status', '--porcelain=v1', '--untracked-files=all') -FailureMessage '无法检查本地文件状态。'
    if ([string]::IsNullOrWhiteSpace($text)) {
        return @()
    }
    return @($text -split "`r?`n")
}

function Test-ModelerClientRunning {
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

function Get-JavaMajorVersion {
    param([Parameter(Mandatory = $true)][string]$JavaExecutable)

    $versionOutput = & $JavaExecutable -version 2>&1
    if ($LASTEXITCODE -ne 0) {
        return $null
    }
    $text = ($versionOutput | ForEach-Object { "$_" }) -join ' '
    $match = [regex]::Match($text, '(?:openjdk|java) version "?(?<major>\d+)')
    if (-not $match.Success) {
        return $null
    }
    return [int]$match.Groups['major'].Value
}

function Find-ModelerJava25 {
    param([Parameter(Mandatory = $true)][string]$ProjectRoot)

    $candidates = New-Object System.Collections.Generic.List[object]
    $projectJava = Join-Path $ProjectRoot '.toolchains\jdk-25\bin\java.exe'
    if (Test-Path -LiteralPath $projectJava -PathType Leaf) {
        $candidates.Add([pscustomobject]@{
            Executable = $projectJava
            JavaHome = (Split-Path -Parent (Split-Path -Parent $projectJava))
            Label = '项目 Java 25'
        })
    }

    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $javaHomeExecutable = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path -LiteralPath $javaHomeExecutable -PathType Leaf) {
            $candidates.Add([pscustomobject]@{
                Executable = $javaHomeExecutable
                JavaHome = $env:JAVA_HOME
                Label = 'JAVA_HOME'
            })
        }
    }

    $pathJava = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($pathJava) {
        $candidates.Add([pscustomobject]@{
            Executable = $pathJava.Source
            JavaHome = $null
            Label = '系统 PATH'
        })
    }

    $seen = @{}
    foreach ($candidate in $candidates) {
        $key = $candidate.Executable.ToLowerInvariant()
        if ($seen.ContainsKey($key)) {
            continue
        }
        $seen[$key] = $true

        $major = Get-JavaMajorVersion -JavaExecutable $candidate.Executable
        if ($major -eq 25) {
            return $candidate
        }
    }

    throw '没有找到 Java 25。请确认模型师电脑原有的 Java 25 环境仍然可用，或让项目负责人协助配置。'
}
