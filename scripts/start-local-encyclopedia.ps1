[CmdletBinding()]
param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [ValidateRange(1024, 65535)][int]$Port = 4173,
    [switch]$NoBrowser
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Write-ArchiveStep {
    param(
        [Parameter(Mandatory = $true)][int]$Number,
        [Parameter(Mandatory = $true)][string]$Title
    )

    Write-Host ''
    Write-Host ("[{0}] {1}" -f $Number, $Title) -ForegroundColor Cyan
}

function Get-ArchivePage {
    param([Parameter(Mandatory = $true)][string]$Url)

    try {
        return Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
    } catch {
        return $null
    }
}

function Test-EchoArchivePage {
    param([AllowNull()]$Response)

    return $null -ne $Response -and $Response.Content -match 'Echo Archive|回声档案馆'
}

function Find-NodeTooling {
    param([Parameter(Mandatory = $true)][string]$Root)

    $nodeCandidates = New-Object System.Collections.Generic.List[string]
    $projectNode = Join-Path $Root '.toolchains\node\node.exe'
    if (Test-Path -LiteralPath $projectNode -PathType Leaf) {
        $nodeCandidates.Add($projectNode)
    }

    $pathNode = Get-Command node.exe -ErrorAction SilentlyContinue
    if ($pathNode) {
        $nodeCandidates.Add($pathNode.Source)
    }

    if (-not [string]::IsNullOrWhiteSpace($env:ProgramFiles)) {
        $programFilesNode = Join-Path $env:ProgramFiles 'nodejs\node.exe'
        if (Test-Path -LiteralPath $programFilesNode -PathType Leaf) {
            $nodeCandidates.Add($programFilesNode)
        }
    }

    $seen = @{}
    foreach ($nodeExecutable in $nodeCandidates) {
        $key = $nodeExecutable.ToLowerInvariant()
        if ($seen.ContainsKey($key)) {
            continue
        }
        $seen[$key] = $true

        $versionText = (& $nodeExecutable --version 2>$null).Trim().TrimStart('v')
        $version = $null
        if (-not [version]::TryParse(($versionText -split '-')[0], [ref]$version)) {
            continue
        }
        if ($version -lt [version]'22.13.0') {
            continue
        }

        $npmExecutable = Join-Path (Split-Path -Parent $nodeExecutable) 'npm.cmd'
        if (-not (Test-Path -LiteralPath $npmExecutable -PathType Leaf)) {
            $pathNpm = Get-Command npm.cmd -ErrorAction SilentlyContinue
            if ($pathNpm) {
                $npmExecutable = $pathNpm.Source
            }
        }
        if (Test-Path -LiteralPath $npmExecutable -PathType Leaf) {
            return [pscustomobject]@{
                Node = $nodeExecutable
                Npm = $npmExecutable
                Version = $version
            }
        }
    }

    throw '没有找到 Node.js 22.13 或更高版本。请安装 Node.js LTS，安装后重新双击启动本地百科.bat。'
}

try {
    $root = (Resolve-Path -LiteralPath $ProjectRoot).Path.TrimEnd('\')
    $archiveRoot = Join-Path $root 'encyclopedia'
    $packageJson = Join-Path $archiveRoot 'package.json'
    $packageLock = Join-Path $archiveRoot 'package-lock.json'
    if (-not (Test-Path -LiteralPath $packageJson -PathType Leaf) -or
        -not (Test-Path -LiteralPath $packageLock -PathType Leaf)) {
        throw "所选目录中没有完整的本地百科：$archiveRoot"
    }

    $url = "http://127.0.0.1:$Port/"

    Write-ArchiveStep -Number 1 -Title '检查本地百科是否已经启动'
    $existingPage = Get-ArchivePage -Url $url
    if (Test-EchoArchivePage -Response $existingPage) {
        Write-Host "本地百科已经在运行：$url" -ForegroundColor Green
        if (-not $NoBrowser) {
            Start-Process $url
        }
        exit 0
    }
    if ($null -ne $existingPage) {
        throw "端口 $Port 已被其他程序占用。请关闭占用该端口的程序后重试。"
    }

    Write-ArchiveStep -Number 2 -Title '检查 Node.js'
    $tooling = Find-NodeTooling -Root $root
    Write-Host "Node.js $($tooling.Version)" -ForegroundColor Green

    Write-ArchiveStep -Number 3 -Title '检查百科依赖'
    $installedLock = Join-Path $archiveRoot 'node_modules\.package-lock.json'
    $needsInstall = -not (Test-Path -LiteralPath $installedLock -PathType Leaf)
    if (-not $needsInstall) {
        $installedAt = (Get-Item -LiteralPath $installedLock).LastWriteTimeUtc
        $needsInstall = (Get-Item -LiteralPath $packageLock).LastWriteTimeUtc -gt $installedAt -or
            (Get-Item -LiteralPath $packageJson).LastWriteTimeUtc -gt $installedAt
    }

    if ($needsInstall) {
        Write-Host '首次启动或依赖已经更新，正在自动安装。这个过程可能需要几分钟。' -ForegroundColor Yellow
        Push-Location $archiveRoot
        try {
            & $tooling.Npm ci --no-audit --no-fund
            if ($LASTEXITCODE -ne 0) {
                throw "百科依赖安装失败（退出代码 $LASTEXITCODE）。"
            }
        } finally {
            Pop-Location
        }
    } else {
        Write-Host '百科依赖已经准备好。' -ForegroundColor Green
    }

    Write-ArchiveStep -Number 4 -Title '启动本地百科'
    Write-Host "浏览器地址：$url" -ForegroundColor Green
    Write-Host '请保持这个黑色窗口开启；关闭窗口就会停止本地百科。' -ForegroundColor Yellow

    $browserJob = $null
    if (-not $NoBrowser) {
        $browserJob = Start-Job -ArgumentList $url -ScriptBlock {
            param($TargetUrl)
            for ($attempt = 0; $attempt -lt 120; $attempt++) {
                try {
                    $response = Invoke-WebRequest -Uri $TargetUrl -UseBasicParsing -TimeoutSec 2
                    if ($response.Content -match 'Echo Archive|回声档案馆') {
                        Start-Process $TargetUrl
                        return
                    }
                } catch {
                    # The development server is still starting.
                }
                Start-Sleep -Milliseconds 500
            }
        }
    }

    Push-Location $archiveRoot
    try {
        & $tooling.Npm run dev -- --host 127.0.0.1 --port $Port
        $serverExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
        if ($null -ne $browserJob) {
            Remove-Job -Job $browserJob -Force -ErrorAction SilentlyContinue
        }
    }

    if ($serverExitCode -ne 0) {
        throw "本地百科启动失败（退出代码 $serverExitCode）。"
    }
    exit 0
} catch {
    Write-Host ''
    Write-Host "本地百科启动失败：$($_.Exception.Message)" -ForegroundColor Red
    Write-Host '请把这个窗口的最后一屏截图发给项目负责人。' -ForegroundColor Yellow
    exit 1
}
