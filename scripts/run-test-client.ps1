[CmdletBinding()]
param(
    [string]$TestWorldName = 'CFMJ-Test-World',
    [switch]$RequireExistingWorld
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkRoot = Join-Path $projectRoot '.toolchains\jdk-25'
$javaExecutable = Join-Path $jdkRoot 'bin\java.exe'
$worldPath = Join-Path $projectRoot "run\saves\$TestWorldName"

if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw "Project Java 25 runtime is missing: $javaExecutable"
}

$escapedProjectRoot = [regex]::Escape($projectRoot)
$runningClient = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
    Where-Object {
        $_.Name -match '^javaw?\.exe$' -and
        $_.CommandLine -match $escapedProjectRoot -and
        $_.CommandLine -match 'net\.minecraft\.client\.main\.Main'
    } |
    Select-Object -First 1

if ($runningClient) {
    Write-Host "Echo Warrior development client is already running (PID $($runningClient.ProcessId))."
    exit 0
}

$env:JAVA_HOME = $jdkRoot
$env:Path = "$(Join-Path $jdkRoot 'bin');$env:Path"

$gradleArguments = @('runClient', '--console=plain')
if (Test-Path -LiteralPath $worldPath) {
    $gradleArguments += "-PquickPlayWorld=$TestWorldName"
    Write-Host "Launching Echo Warrior and entering $TestWorldName..."
} elseif ($RequireExistingWorld) {
    throw "The requested test world does not exist: $worldPath"
} else {
    Write-Host "Launching Echo Warrior. Create the world '$TestWorldName' once; later launches will enter it automatically."
}

Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') @gradleArguments
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
