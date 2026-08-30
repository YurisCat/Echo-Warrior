[CmdletBinding()]
param(
    [string]$KitRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$ProjectRoot
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'modeler-support-common.ps1')

try {
    Write-ModelerStep -Number 1 -Title '定位模型项目'
    $root = Get-ModelerProjectRoot -KitRoot $KitRoot -PreferredProjectRoot $ProjectRoot
    Write-Host "项目目录：$root"

    if (Test-ModelerClientRunning -ProjectRoot $root) {
        Write-Host 'Echo Warrior 开发客户端已经在运行，不会重复启动。' -ForegroundColor Green
        exit 0
    }

    Write-ModelerStep -Number 2 -Title '检查 Java 25'
    $java = Find-ModelerJava25 -ProjectRoot $root
    Write-Host "使用 $($java.Label)：$($java.Executable)"

    $oldJavaHome = $env:JAVA_HOME
    $oldPath = $env:Path
    try {
        if ([string]::IsNullOrWhiteSpace($java.JavaHome)) {
            Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue
        } else {
            $env:JAVA_HOME = $java.JavaHome
        }
        $env:Path = "$(Split-Path -Parent $java.Executable);$oldPath"

        Write-ModelerStep -Number 3 -Title '编译并启动开发客户端'
        $gradleArguments = @('runClient', '--console=plain')
        $worldPath = Join-Path $root 'run\saves\CATTEST'
        if (Test-Path -LiteralPath (Join-Path $worldPath 'level.dat') -PathType Leaf) {
            $gradleArguments += '-PquickPlayWorld=CATTEST'
            Write-Host '检测到 CATTEST，将直接进入该测试世界。'
        } else {
            Write-Host '未检测到 CATTEST，将启动到 Minecraft 主菜单。' -ForegroundColor Yellow
        }

        $focusScript = Join-Path $root 'scripts\focus-test-client-window.ps1'
        if (Test-Path -LiteralPath $focusScript -PathType Leaf) {
            Start-Process -FilePath 'powershell.exe' `
                -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $focusScript, '-ProjectRoot', $root) `
                -WindowStyle Hidden
        }

        Push-Location $root
        try {
            & (Join-Path $root 'gradlew.bat') @gradleArguments
            if ($LASTEXITCODE -ne 0) {
                throw "Gradle 启动失败（退出代码 $LASTEXITCODE）。"
            }
        } finally {
            Pop-Location
        }
    } finally {
        $env:JAVA_HOME = $oldJavaHome
        $env:Path = $oldPath
    }

    exit 0
} catch {
    Write-Host ''
    Write-Host "启动未完成：$($_.Exception.Message)" -ForegroundColor Red
	Write-Host '如果提示缺少依赖或下载缓慢，请把窗口截图发给项目负责人。' -ForegroundColor Yellow
	exit 1
}
