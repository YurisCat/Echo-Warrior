[CmdletBinding()]
param(
    [ValidateSet('Dual', 'Fabric', 'NeoForge')]
    [string]$Loader = 'Dual',

    [switch]$Clean
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$compatibilityRoot = Join-Path $repositoryRoot 'versions\1.21.1'
$gradleWrapper = Join-Path $repositoryRoot 'gradlew.bat'

$javaCandidates = @(
    @(
        $env:ECHO_WARRIOR_JAVA21_HOME,
        (Join-Path $repositoryRoot '.toolchains\jdk-21'),
        'C:\Program Files\Java\jdk-21',
        'C:\Program Files\Java\jdk-21.0.10',
        'C:\Program Files\Eclipse Adoptium\jdk-21'
    ) | Where-Object { $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe')) }
)

if ($javaCandidates.Count -eq 0) {
    throw 'Java 21 was not found. Set ECHO_WARRIOR_JAVA21_HOME to a JDK 21 directory.'
}

$javaHome = (Resolve-Path -LiteralPath $javaCandidates[0]).Path
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path

try {
    $env:JAVA_HOME = $javaHome
    $env:Path = "$(Join-Path $javaHome 'bin');$previousPath"

    $tasks = @()
    if ($Clean) {
        $tasks += 'clean'
    }

    $tasks += switch ($Loader) {
        'Fabric' { 'fabricBuild' }
        'NeoForge' { 'neoForgeBuild' }
        default { 'dualBuild' }
    }

    & $gradleWrapper '-p' $compatibilityRoot @tasks
    if ($LASTEXITCODE -ne 0) {
        throw "Minecraft 1.21.1 $Loader build failed with exit code $LASTEXITCODE."
    }
}
finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
}
