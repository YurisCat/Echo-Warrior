[CmdletBinding()]
param(
    [switch]$AllowDirty
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkRoot = Join-Path $projectRoot '.toolchains\jdk-25'
$javaExecutable = Join-Path $jdkRoot 'bin\java.exe'
$propertiesPath = Join-Path $projectRoot 'gradle.properties'
$deliveryRoot = Join-Path $projectRoot 'temporary-delivery'
$checklistSource = Join-Path $projectRoot 'docs\SUMMONER_TEST_CHECKLIST.html'

function Read-GradleProperties([string]$Path) {
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#') -or -not $trimmed.Contains('=')) {
            continue
        }
        $key, $value = $trimmed.Split('=', 2)
        $values[$key.Trim()] = $value.Trim()
    }
    return $values
}

function Require-Property([hashtable]$Properties, [string]$Name) {
    if (-not $Properties.ContainsKey($Name) -or -not $Properties[$Name]) {
        throw "Missing required Gradle property: $Name"
    }
    return [string]$Properties[$Name]
}

if (-not (Test-Path -LiteralPath $javaExecutable)) {
    throw "Project Java 25 runtime is missing: $javaExecutable"
}
if (-not (Test-Path -LiteralPath $checklistSource)) {
    throw "Test checklist is missing: $checklistSource"
}

$properties = Read-GradleProperties $propertiesPath
$modVersion = Require-Property $properties 'mod_version'
$minecraftVersion = Require-Property $properties 'minecraft_version'
$archiveBase = Require-Property $properties 'archives_base_name'
$javaVersion = Require-Property $properties 'java_version'
$fabricLoaderVersion = Require-Property $properties 'loader_version'
$fabricApiVersion = Require-Property $properties 'fabric_api_version'
$neoForgeVersion = Require-Property $properties 'neoforge_version'
$smartBrainLibVersion = Require-Property $properties 'smartbrainlib_version'
$geckoLibVersion = Require-Property $properties 'geckolib_version'

Push-Location $projectRoot
try {
    $commit = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the current Git commit.' }
    $branch = (& git branch --show-current).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Unable to resolve the current Git branch.' }
    if (-not $branch) { $branch = '(detached HEAD)' }
    $dirty = [bool](& git status --porcelain)
    if ($LASTEXITCODE -ne 0) { throw 'Unable to inspect the Git working tree.' }

    if ($dirty -and -not $AllowDirty) {
        throw 'The working tree is not clean. Commit the dual-loader source first, or use -AllowDirty for a local non-publishable verification package.'
    }

    $env:JAVA_HOME = $jdkRoot
    $env:Path = "$(Join-Path $jdkRoot 'bin');$env:Path"
    & (Join-Path $projectRoot 'gradlew.bat') 'clean' ':fabric:build' ':neoforge:build' '--console=plain'
    if ($LASTEXITCODE -ne 0) {
        throw "Dual-loader Gradle build failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

$fabricBuildName = "$archiveBase-fabric-$minecraftVersion-$modVersion.jar"
$neoForgeBuildName = "$archiveBase-neoforge-$minecraftVersion-$modVersion.jar"
$fabricBuildJar = Join-Path $projectRoot "fabric\build\libs\$fabricBuildName"
$neoForgeBuildJar = Join-Path $projectRoot "neoforge\build\libs\$neoForgeBuildName"
if (-not (Test-Path -LiteralPath $fabricBuildJar)) { throw "Fabric JAR was not produced: $fabricBuildJar" }
if (-not (Test-Path -LiteralPath $neoForgeBuildJar)) { throw "NeoForge JAR was not produced: $neoForgeBuildJar" }

$resolvedProjectRoot = [IO.Path]::GetFullPath($projectRoot).TrimEnd('\')
$resolvedDeliveryRoot = [IO.Path]::GetFullPath($deliveryRoot).TrimEnd('\')
if (-not $resolvedDeliveryRoot.StartsWith("$resolvedProjectRoot\", [StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to prepare delivery outside the project root: $resolvedDeliveryRoot"
}
New-Item -ItemType Directory -Path $deliveryRoot -Force | Out-Null
Get-ChildItem -LiteralPath $deliveryRoot -File | Remove-Item -Force

$fabricDeliveryName = "$archiveBase-$modVersion-fabric-mc$minecraftVersion.jar"
$neoForgeDeliveryName = "$archiveBase-$modVersion-neoforge-mc$minecraftVersion.jar"
$checklistName = "Echo-Warrior-dual-loader-test-checklist-$modVersion.html"
$fabricDeliveryJar = Join-Path $deliveryRoot $fabricDeliveryName
$neoForgeDeliveryJar = Join-Path $deliveryRoot $neoForgeDeliveryName
$checklistDelivery = Join-Path $deliveryRoot $checklistName

Copy-Item -LiteralPath $fabricBuildJar -Destination $fabricDeliveryJar
Copy-Item -LiteralPath $neoForgeBuildJar -Destination $neoForgeDeliveryJar
Copy-Item -LiteralPath $checklistSource -Destination $checklistDelivery

$fabricHash = (Get-FileHash -LiteralPath $fabricDeliveryJar -Algorithm SHA256).Hash.ToLowerInvariant()
$neoForgeHash = (Get-FileHash -LiteralPath $neoForgeDeliveryJar -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath "$fabricDeliveryJar.sha256" -Encoding utf8 -Value "$fabricHash  $fabricDeliveryName"
Set-Content -LiteralPath "$neoForgeDeliveryJar.sha256" -Encoding utf8 -Value "$neoForgeHash  $neoForgeDeliveryName"

$manifest = [ordered]@{
    schemaVersion = 1
    project = 'Echo Warrior'
    modId = Require-Property $properties 'mod_id'
    modVersion = $modVersion
    minecraftVersion = $minecraftVersion
    javaVersion = $javaVersion
    generatedAtUtc = (Get-Date).ToUniversalTime().ToString('o')
    source = [ordered]@{
        branch = $branch
        commit = $commit
        dirty = $dirty
    }
    publishReady = -not $dirty
    buildCommand = '.\gradlew.bat clean :fabric:build :neoforge:build'
    testChecklist = $checklistName
    packages = @(
        [ordered]@{
            loader = 'fabric'
            loaderVersion = $fabricLoaderVersion
            file = $fabricDeliveryName
            sha256 = $fabricHash
            requiredDependencies = @(
                "Fabric API $fabricApiVersion",
                "SmartBrainLib $smartBrainLibVersion",
                "GeckoLib $geckoLibVersion"
            )
        },
        [ordered]@{
            loader = 'neoforge'
            loaderVersion = $neoForgeVersion
            file = $neoForgeDeliveryName
            sha256 = $neoForgeHash
            requiredDependencies = @(
                "SmartBrainLib $smartBrainLibVersion",
                "GeckoLib $geckoLibVersion"
            )
        }
    )
}
$manifestPath = Join-Path $deliveryRoot 'release-manifest.json'
$manifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $manifestPath -Encoding utf8

Write-Host ''
Write-Host "Dual-loader delivery prepared at: $deliveryRoot"
Write-Host "Fabric:   $fabricDeliveryName"
Write-Host "NeoForge: $neoForgeDeliveryName"
if ($dirty) {
    Write-Warning 'This package was built from a dirty working tree and is not marked publish-ready.'
} else {
    Write-Host 'The source tree is clean; release-manifest.json marks this package publish-ready.'
}
