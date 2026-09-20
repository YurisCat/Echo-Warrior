[CmdletBinding()]
param(
    [ValidateSet('Dual', 'Fabric', 'Forge')]
    [string]$Loader = 'Dual',
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$compatibilityRoot = Join-Path $repositoryRoot 'versions\1.20.1'

# Gradle/Loom run on 21; the project's Java toolchain compiles/runs Minecraft on 17.
$buildJavaCandidates = @(
    $env:ECHO_WARRIOR_JAVA21_HOME,
    (Join-Path $repositoryRoot '.toolchains\jdk-21'),
    'C:\Program Files\Java\jdk-21.0.10',
    'C:\Program Files\Java\jdk-21'
) | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe')) }
if (@($buildJavaCandidates).Count -eq 0) {
    throw 'Set ECHO_WARRIOR_JAVA21_HOME to a JDK 21 installation for Gradle. Minecraft still requires a JDK 17 toolchain.'
}

$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
try {
    $env:JAVA_HOME = (Resolve-Path -LiteralPath @($buildJavaCandidates)[0]).Path
    $env:Path = "$(Join-Path $env:JAVA_HOME 'bin');$previousPath"
    $buildTasks = @()
    if ($Clean) { $buildTasks += 'clean' }
    $buildTasks += switch ($Loader) {
        'Fabric' { 'fabricBuild' }
        'Forge' { 'forgeBuild' }
        default { 'dualBuild' }
    }
    & (Join-Path $repositoryRoot 'gradlew.bat') '-p' $compatibilityRoot @buildTasks '--console=plain'
    if ($LASTEXITCODE -ne 0) { throw "Minecraft 1.20.1 $Loader build failed: $LASTEXITCODE" }
}
finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
}
