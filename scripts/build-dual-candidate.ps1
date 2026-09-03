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
$checklistSource = Join-Path $projectRoot 'docs\ECHO_WARRIOR_TEST_CASES.html'

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

$fabricDeliveryName = $fabricBuildName
$neoForgeDeliveryName = $neoForgeBuildName
$checklistName = "Echo-Warrior-dual-loader-test-cases-$modVersion.html"
$handoffName = "Echo-Warrior-dual-loader-test-handoff-$modVersion.md"
$fabricDeliveryJar = Join-Path $deliveryRoot $fabricDeliveryName
$neoForgeDeliveryJar = Join-Path $deliveryRoot $neoForgeDeliveryName
$checklistDelivery = Join-Path $deliveryRoot $checklistName
$handoffDelivery = Join-Path $deliveryRoot $handoffName

Copy-Item -LiteralPath $fabricBuildJar -Destination $fabricDeliveryJar
Copy-Item -LiteralPath $neoForgeBuildJar -Destination $neoForgeDeliveryJar
Copy-Item -LiteralPath $checklistSource -Destination $checklistDelivery

$fabricHash = (Get-FileHash -LiteralPath $fabricDeliveryJar -Algorithm SHA256).Hash.ToLowerInvariant()
$neoForgeHash = (Get-FileHash -LiteralPath $neoForgeDeliveryJar -Algorithm SHA256).Hash.ToLowerInvariant()
$publishStatus = if ($dirty) {
    '否；该候选来自有未提交改动的工作区，只可用于本地验证。'
} else {
    '是；该候选来自干净工作区，但仍须根据本轮测试结果决定是否发布。'
}
$handoffLines = @(
    '# Echo Warrior 双端测试交接说明',
    '',
    '## 构筑信息',
    '',
    "- Echo Warrior：$modVersion",
    "- Minecraft：$minecraftVersion",
    "- Java：$javaVersion",
    "- Git 分支：$branch",
    "- Git 提交：$commit",
    "- 工作区有未提交内容：$dirty",
    "- 可作为发布候选：$publishStatus",
    '',
    '## 四个交付文件',
    '',
    '| 加载器 | 文件 | SHA-256 |',
    '| --- | --- | --- |',
    "| Fabric | $fabricDeliveryName | $fabricHash |",
    "| NeoForge | $neoForgeDeliveryName | $neoForgeHash |",
    "| 双端共用 | $checklistName | 在浏览器中打开并分别保存两端结果 |",
    "| 双端共用 | $handoffName | 当前说明 |",
    '',
    '不要安装 sources JAR，也不要把 Fabric 与 NeoForge 两个 Echo Warrior JAR 放进同一个实例。',
    '',
    '## 依赖',
    '',
    "- Fabric：Fabric Loader $fabricLoaderVersion、Fabric API $fabricApiVersion、SmartBrainLib $smartBrainLibVersion、GeckoLib $geckoLibVersion。",
    "- NeoForge：NeoForge $neoForgeVersion、SmartBrainLib $smartBrainLibVersion、GeckoLib $geckoLibVersion。",
    '- 依赖必须选择与 Minecraft 版本和当前加载器相匹配的独立 JAR；Echo Warrior 不内置这些依赖。',
    '',
    '## 本轮优先检查',
    '',
    '- 五种英灵的死亡变红、倾倒和最终消失时机。',
    '- 五种英灵受到中毒等无攻击者伤害时，头部、眼睛和身体姿态是否稳定。',
    '- 回声罗盘在有目标、无目标、完成目标和重进世界后的指针、文字与声音反馈。',
    '- 全部英灵饰品改变生命、攻击、护甲、移速或警戒范围时的红绿数值。',
    '- 五种文化各自两件常见、两件罕见和一件稀有饰品，以及物品、JEI、教程与百科的一致性。',
    '- 回收箱在自然午夜、跳时、倒拨时间和进入新的自然日时的结算行为。',
    '',
    '## 结果返还',
    '',
    '- 页面导出的双端测试结果 JSON；',
    '- 每个加载器的结果 HTML（如方便）；',
    '- 失败项的截图或录像、最短复现步骤与复现频率；',
    '- 异常实例的 logs/latest.log；发生崩溃时另附 crash-reports。',
    '',
    '本说明由构筑脚本生成，只记录候选身份和测试要求，不代表已经完成独立测试人员的完整回归。'
)
$handoffLines | Set-Content -LiteralPath $handoffDelivery -Encoding utf8

Write-Host ''
Write-Host "Dual-loader delivery prepared at: $deliveryRoot"
Write-Host "Fabric:   $fabricDeliveryName"
Write-Host "NeoForge: $neoForgeDeliveryName"
Write-Host "Checklist: $checklistName"
Write-Host "Handoff:   $handoffName"
if ($dirty) {
    Write-Warning 'This package was built from a dirty working tree and is not marked publish-ready.'
} else {
    Write-Host 'The source tree is clean; the generated handoff marks this package as a release candidate.'
}
