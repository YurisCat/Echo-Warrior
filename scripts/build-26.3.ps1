[CmdletBinding()]
param(
    [ValidateSet('Dual', 'Fabric', 'NeoForge')][string]$Loader = 'Dual',
    [switch]$Clean
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$target = Join-Path $repo 'versions\26.3'
$jdk = Join-Path $repo '.toolchains\jdk-25'
if (-not (Test-Path -LiteralPath (Join-Path $jdk 'bin\java.exe'))) {
    throw 'Minecraft 26.3 requires the project Java 25 toolchain.'
}
$previousJavaHome = $env:JAVA_HOME
$previousPath = $env:Path
try {
    $env:JAVA_HOME = $jdk
    $env:Path = "$jdk\bin;$previousPath"
    $tasks = @()
    if ($Clean) { $tasks += 'clean' }
    $tasks += switch ($Loader) {
        'Fabric' { ':fabric:build' }
        'NeoForge' { ':neoforge:build' }
        default { 'dualBuild' }
    }
    # This line needs Gradle 9.6; other versions retain their frozen Wrapper.
    & (Join-Path $target 'gradlew.bat') '-p' $target @tasks
    if ($LASTEXITCODE -ne 0) { throw "Minecraft 26.3 $Loader build failed: $LASTEXITCODE" }
} finally {
    $env:JAVA_HOME = $previousJavaHome
    $env:Path = $previousPath
}
