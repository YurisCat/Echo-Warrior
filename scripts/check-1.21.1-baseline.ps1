[CmdletBinding()]
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$compatibilityRoot = Join-Path $repositoryRoot 'versions\1.21.1'
$propertiesPath = Join-Path $compatibilityRoot 'gradle.properties'

if (-not $SkipBuild) {
    & (Join-Path $PSScriptRoot 'build-1.21.1.ps1') -Loader Dual
    if ($LASTEXITCODE -ne 0) {
        throw "Minecraft 1.21.1 dual build failed with exit code $LASTEXITCODE."
    }
}

$properties = @{}
foreach ($line in Get-Content -LiteralPath $propertiesPath) {
    if ($line -match '^\s*([^#=]+?)\s*=\s*(.*?)\s*$') {
        $properties[$matches[1]] = $matches[2]
    }
}

$version = $properties['mod_version']
$baseName = $properties['archives_base_name']
$fabricJar = Join-Path $compatibilityRoot "fabric\build\libs\$baseName-fabric-1.21.1-$version.jar"
$neoForgeJar = Join-Path $compatibilityRoot "neoforge\build\libs\$baseName-neoforge-1.21.1-$version.jar"

Add-Type -AssemblyName System.IO.Compression.FileSystem

function Assert-Condition {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

foreach ($languageFile in @('zh_cn.json', 'en_us.json')) {
    $mainLanguagePath = Join-Path $repositoryRoot "common\src\main\resources\assets\echo_warrior\lang\$languageFile"
    $compatLanguagePath = Join-Path $compatibilityRoot "common\src\main\resources\assets\echo_warrior\lang\$languageFile"
    $mainLanguage = Get-Content -LiteralPath $mainLanguagePath -Raw | ConvertFrom-Json
    $compatLanguage = Get-Content -LiteralPath $compatLanguagePath -Raw | ConvertFrom-Json
    $skillKeyPattern = '^gui\.echo_warrior\.(?:summoner\.skill\.|tutorial\.skill\.roman\.)'
    $skillKeys = @(
        @($mainLanguage.PSObject.Properties.Name | Where-Object { $_ -match $skillKeyPattern })
        @($compatLanguage.PSObject.Properties.Name | Where-Object { $_ -match $skillKeyPattern })
    ) | Sort-Object -Unique
    foreach ($skillKey in $skillKeys) {
        $mainProperty = $mainLanguage.PSObject.Properties[$skillKey]
        $compatProperty = $compatLanguage.PSObject.Properties[$skillKey]
        Assert-Condition ($null -ne $mainProperty -and $null -ne $compatProperty) `
            "Skill localization key $skillKey must exist in both the 26.1.2 and 1.21.1 $languageFile sources."
        Assert-Condition ([string]$mainProperty.Value -ceq [string]$compatProperty.Value) `
            "Skill localization key $skillKey differs between the 26.1.2 and 1.21.1 $languageFile sources."
    }
}

$clientLauncherPath = Join-Path $repositoryRoot 'scripts\run-test-client.ps1'
$clientLauncher = Get-Content -LiteralPath $clientLauncherPath -Raw
Assert-Condition ($clientLauncher -match [regex]::Escape('onboardAccessibility:false')) `
    'The automated 1.21.1 client launcher must bypass the first-run accessibility screen.'
Assert-Condition ($clientLauncher -match 'Another Minecraft development client is already running') `
    'The automated client launcher must refuse to open a concurrent Minecraft client.'
Assert-Condition ($clientLauncher -match 'Starting integrated minecraft server version') `
    'The automated client launcher must require an integrated-server Quick Play checkpoint.'
Assert-Condition ($clientLauncher -match 'Closed every process launched by') `
    'The automated client launcher must retain explicit process cleanup reporting.'
Assert-Condition ($clientLauncher -match [regex]::Escape('-PautoPauseAfterQuickPlay=true')) `
    'Startup-only Quick Play must enable the automated pause-menu mouse release.'
Assert-Condition ($clientLauncher -match 'Automated client smoke test opened the pause menu to release the mouse') `
    'Startup-only Quick Play must wait for the pause-menu mouse-release checkpoint.'
Assert-Condition ($clientLauncher -match "if \(\`$null -eq \`$logText\) \{ \`$logText = '' \}") `
    'The automated client launcher must tolerate an empty latest.log while Fabric recreates it.'

$modelSourceText = Get-ChildItem -LiteralPath (Join-Path $compatibilityRoot 'common\src\client\java') `
    -Recurse -File -Filter '*Model1211.java' | Get-Content -Raw
Assert-Condition (-not ($modelSourceText -match 'geckolib/(?:models|animations)')) `
    'GeckoLib 4 model classes must use assets/<modid>/geo and assets/<modid>/animations paths.'

$summonerMenuSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\menu\SummonerMenu1211.java') -Raw
$summonerItemSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\item\EchoSummonerItem1211.java') -Raw
$knowledgeFragmentSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\item\KnowledgeFragmentItem1211.java') -Raw
$knowledgeTooltipSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\item\KnowledgeTooltip1211.java') -Raw
$relicItemSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\item\EchoRelicItem1211.java') -Raw
$summonerScreenSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\client\SummonerScreen1211.java') -Raw
$containerScreenMixinSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\mixin\AbstractContainerScreenMixin1211.java') -Raw
$tutorialScreenSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\client\TutorialManualScreen1211.java') -Raw
$creativeInventoryMixinSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\mixin\CreativeModeInventoryScreenMixin1211.java') -Raw
$creativeInsertionPayloadSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\network\CreativeSummonerInsertionPayload1211.java') -Raw
$creativeDestructionPayloadSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\network\CreativeSummonerDestructionPayload1211.java') -Raw
$creativeDestroyTrackerSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\binding\CreativeSummonerDestroyTracker1211.java') -Raw
$summonerStackContentsSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\item\SummonerStackContents1211.java') -Raw
$serverGamePacketMixinSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\mixin\ServerGamePacketListenerMixin1211.java') -Raw
$bindingSystemSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\binding\EchoBindingSystem1211.java') -Raw
$bindingSavedDataSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\binding\EchoBindingSavedData1211.java') -Raw
$bindingConfigSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\binding\EchoBindingConfig1211.java') -Raw
$safeTeleportSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\entity\behavior\EchoSafeTeleport1211.java') -Raw
$bindingCommandSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\command\EchoBindingCommands1211.java') -Raw
$gameplayCommandSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\command\GameplayTestCommands1211.java') -Raw
$selfTestCommandSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\command\CompatSelfTestCommand1211.java') -Raw
$itemInHandMixinSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\mixin\ItemInHandRendererMixin1211.java') -Raw
$tutorialStackDataSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\tutorial\TutorialManualStackData1211.java') -Raw
$knowledgeStackDataSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\knowledge\KnowledgeStackData1211.java') -Raw
$summonerRelicIconSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\client\SummonerRelicIconProperty1211.java') -Raw
$mainCreativeInventoryMixinSource = Get-Content -LiteralPath (Join-Path $repositoryRoot `
    'common\src\client\java\com\yuriscat\echowarrior\mixin\CreativeModeInventoryScreenMixin.java') -Raw
$mainCreativeDestroyTrackerSource = Get-Content -LiteralPath (Join-Path $repositoryRoot `
    'common\src\main\java\com\yuriscat\echowarrior\binding\CreativeSummonerDestroyTracker.java') -Raw
$mainItemInHandMixinSource = Get-Content -LiteralPath (Join-Path $repositoryRoot `
    'common\src\client\java\com\yuriscat\echowarrior\mixin\ItemInHandRendererMixin.java') -Raw
$mainKnowledgeStackDataSource = Get-Content -LiteralPath (Join-Path $repositoryRoot `
    'common\src\main\java\com\yuriscat\echowarrior\knowledge\KnowledgeStackData.java') -Raw
$compassItemSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\item\EchoCompassItem1211.java') -Raw
$compassPulseSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\client\EchoCompassPulseHud1211.java') -Raw
$compassTooltipSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\client\EchoCompassTooltipTitle1211.java') -Raw
$guiMixinSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\mixin\GuiMixin1211.java') -Raw
$guiGraphicsMixinSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\mixin\GuiGraphicsMixin1211.java') -Raw
$guiGraphicsInvokerSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\mixin\GuiGraphicsInvoker1211.java') -Raw
$fabricMainSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'fabric\src\main\java\com\yuriscat\echowarrior\compat\fabric\EchoWarrior1211Fabric.java') -Raw
$neoForgeMainSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'neoforge\src\main\java\com\yuriscat\echowarrior\compat\neoforge\EchoWarrior1211NeoForge.java') -Raw
$egyptianSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\entity\EgyptianArcherEchoEntity1211.java') -Raw
$egyptianArrowSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\entity\EgyptianArcherArrowEntity1211.java') -Raw
$egyptianModelSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\client\EgyptianArcherModel1211.java') -Raw
$summonerFeedbackSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\item\SummonerFuelInsertFeedback1211.java') -Raw
$mixinConfigSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\resources\echo_warrior_1211.mixins.json') -Raw
$suspiciousTextureGeneratorSource = Get-Content -LiteralPath (Join-Path $repositoryRoot `
    'scripts\asset-tools\GenerateSuspiciousBlockTextures.java') -Raw
$colorProviderSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\client\CompatColorProviders1211.java') -Raw
$recyclerItemRendererSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\client\RecyclerChestItemRenderer1211.java') -Raw
$recyclerRendererSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\client\RecyclerChestRenderer1211.java') -Raw
$fabricClientSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'fabric\src\main\java\com\yuriscat\echowarrior\compat\fabric\EchoWarrior1211FabricClient.java') -Raw
$neoForgeClientSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'neoforge\src\main\java\com\yuriscat\echowarrior\compat\neoforge\EchoWarrior1211NeoForgeClient.java') -Raw
$automatedPauseSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\client\java\com\yuriscat\echowarrior\compat\client\AutomatedTestPauseController1211.java') -Raw
$legacySource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\item\LegacyItem1211.java') -Raw
$romanSource = Get-Content -LiteralPath (Join-Path $compatibilityRoot `
    'common\src\main\java\com\yuriscat\echowarrior\compat\entity\RomanLegionaryEchoEntity1211.java') -Raw
Assert-Condition ($summonerMenuSource -match 'reconcileMenuRelicForSave') `
    'The summoner menu must reconcile stale relic state before saving.'
Assert-Condition ($summonerMenuSource -match 'insertIntoOpenSummoner') `
    'The open summoner menu must support direct relic and accessory insertion.'
Assert-Condition ($summonerMenuSource -match 'owner\.level\(\)\.isClientSide' `
    -and $summonerMenuSource -match 'commitMenuContents' `
    -and $summonerItemSource -match 'openMenu\.insertIntoOpenSummoner\(other\)' `
    -and $bindingSystemSource -match 'public static void commitMenuContents' `
    -and $bindingSystemSource -match 'binding\.setRelic\(relic\)' `
    -and $bindingSystemSource -match 'binding\.setAccessories\(accessories\)') `
    'Summoner equipment removal must commit relic and accessories atomically without a client-side stale mirror write.'
Assert-Condition ($summonerItemSource -match 'allowsDirectInsertionIntoSource') `
    'The summoner item must route direct insertion through an open locked menu slot.'
Assert-Condition ($creativeInventoryMixinSource -match 'insertIntoInternalSlotForCreative' `
    -and $creativeInventoryMixinSource -match 'CreativeSummonerInsertionSender1211\.send' `
    -and $creativeInventoryMixinSource -notmatch 'handleCreativeModeItemAdd' `
    -and $creativeInventoryMixinSource -match 'slotId < firstHotbarSlotId' `
    -and $creativeInventoryMixinSource -match 'echoWarrior\$getTarget\(\)\.index' `
    -and $creativeInventoryMixinSource -match 'slot\.index - screen\.getMenu\(\)\.slots\.size\(\) \+ 45' `
    -and $creativeInventoryMixinSource -match 'cancellable\s*=\s*true' `
    -and $creativeInventoryMixinSource -match 'callback\.cancel\(\)' `
    -and $creativeInventoryMixinSource -notmatch 'pendingSummoner' `
    -and $summonerItemSource -match 'isDirectInsertionCandidate' `
    -and $summonerItemSource -notmatch 'player\.getAbilities\(\)\.instabuild') `
    'Creative direct insertion must commit once, cancel the vanilla click path, and use the loader bridge instead of vanilla creative slot replacement.'
Assert-Condition ($creativeInsertionPayloadSource -match 'player\.gameMode\.isCreative\(\)' `
    -and $creativeInsertionPayloadSource -match 'slot\.container != player\.getInventory\(\)' `
    -and $creativeInsertionPayloadSource -match 'insertIntoInternalSlotForCreative' `
    -and $creativeInsertionPayloadSource -match 'player\.inventoryMenu\.broadcastChanges\(\)' `
    -and $fabricMainSource -match 'PayloadTypeRegistry\.playC2S\(\)\.register' `
    -and $fabricMainSource -match 'ServerPlayNetworking\.registerGlobalReceiver' `
    -and $fabricClientSource -match 'ClientPlayNetworking\.send' `
    -and $neoForgeMainSource -match 'RegisterPayloadHandlersEvent' `
    -and $neoForgeMainSource -match 'playToServer' `
    -and $neoForgeClientSource -match 'PacketDistributor\.sendToServer') `
    'Creative insertion must be validated and applied on the server through registered Fabric and NeoForge payloads.'
Assert-Condition ($creativeInventoryMixinSource -match 'destroyItemSlot' `
    -and $creativeInventoryMixinSource -match 'CreativeSummonerDestructionSender1211\.send' `
    -and $creativeInventoryMixinSource -match 'echoWarrior\$visibleSummonersBefore' `
    -and $creativeInventoryMixinSource -match 'echoWarrior\$explicitDestructionCandidates' `
    -and $creativeInventoryMixinSource -match 'visibleBefore\.stream\(\)\.filter\(id -> !visibleAfter\.contains\(id\)\)' `
    -and $creativeInventoryMixinSource -match '@At\("RETURN"\)' `
    -and $creativeInventoryMixinSource -match 'ClickType\.QUICK_MOVE' `
    -and $creativeInventoryMixinSource -match 'slot != null && clickType == ClickType\.QUICK_MOVE' `
    -and $creativeInventoryMixinSource -match 'summonerIds\(slot\.getItem\(\)\)' `
    -and $creativeInventoryMixinSource -match 'clickType != ClickType\.THROW' `
    -and $creativeInventoryMixinSource -match 'destroyCarriedSummonerWhenScreenCloses' `
    -and $creativeInventoryMixinSource -match 'handleHotbarLoadOrSave' `
    -and $creativeInventoryMixinSource -match 'SummonerStackContents1211\.summonerIds' `
    -and $creativeDestructionPayloadSource -match 'List<UUID> summonerIds' `
    -and $creativeDestructionPayloadSource -match 'CreativeSummonerDestroyTracker1211\.requestCreativeTrash' `
    -and $creativeDestroyTrackerSource -match 'noteCreativeSlotUpdate' `
    -and $creativeDestroyTrackerSource -match 'REQUEST_NOT_BEFORE_TICK' `
    -and $creativeDestroyTrackerSource -match 'visibleInventorySummoners' `
    -and $creativeDestroyTrackerSource -match 'REMOVAL_WINDOW_TICKS = 20 \* 60 \* 30' `
    -and $creativeDestroyTrackerSource -match 'player\.containerMenu\.getItems\(\)' `
    -and $creativeDestroyTrackerSource -match 'player\.getEnderChestInventory\(\)' `
    -and $creativeDestroyTrackerSource -match 'entity instanceof ItemEntity' `
    -and $creativeDestroyTrackerSource -match 'entity instanceof Container' `
    -and $creativeDestroyTrackerSource -match 'EchoBindingSystem1211\.destroySummoner' `
    -and $summonerStackContentsSource -match 'DataComponents\.CONTAINER' `
    -and $summonerStackContentsSource -match 'nonEmptyStream\(\)' `
    -and $summonerStackContentsSource -match 'DataComponents\.BUNDLE_CONTENTS' `
    -and $summonerStackContentsSource -match 'itemCopyStream\(\)' `
    -and $serverGamePacketMixinSource -match 'CreativeSummonerDestroyTracker1211\.noteCreativeSlotUpdate' `
    -and $mixinConfigSource -match 'ServerGamePacketListenerMixin1211' `
    -and $fabricMainSource -match 'CreativeSummonerDestroyTracker1211\.tick' `
    -and $neoForgeMainSource -match 'CreativeSummonerDestroyTracker1211\.tick' `
    -and $fabricMainSource -match 'CreativeSummonerDestructionPayload1211' `
    -and $neoForgeMainSource -match 'CreativeSummonerDestructionPayload1211' `
    -and $fabricClientSource -match 'CreativeSummonerDestructionSender1211' `
    -and $neoForgeClientSource -match 'CreativeSummonerDestructionSender1211' `
    -and $bindingSystemSource -match 'public static boolean destroySummoner' `
    -and $summonerItemSource -match 'void onDestroyed\(ItemEntity' `
    -and $summonerItemSource -match 'EchoBindingSystem1211\.destroySummoner' `
    -and $selfTestCommandSource -match 'nested summoner discovery for confirmed creative destruction') `
    'Destroyed summoners must permanently remove their binding and dismiss the linked spirit.'
Assert-Condition ($mainCreativeInventoryMixinSource -match 'else if \(containerInput == ContainerInput\.QUICK_MOVE\)' `
    -and $mainCreativeInventoryMixinSource -match 'summonerIds\(slot\.getItem\(\)\)' `
    -and $mainCreativeInventoryMixinSource -match 'visibleBefore\.stream\(\)\.filter\(id -> !visibleAfter\.contains\(id\)\)' `
    -and $mainCreativeInventoryMixinSource -match 'containerInput != ContainerInput\.THROW' `
    -and $mainCreativeInventoryMixinSource -match 'destroyCarriedSummonerWhenScreenCloses' `
    -and $mainCreativeInventoryMixinSource -match 'handleHotbarLoadOrSave' `
    -and $mainCreativeDestroyTrackerSource -match 'REMOVAL_WINDOW_TICKS = 20 \* 60 \* 30' `
    -and $mainCreativeDestroyTrackerSource -match 'player\.containerMenu\.getItems\(\)' `
    -and $mainCreativeDestroyTrackerSource -match 'player\.getEnderChestInventory\(\)' `
    -and $mainCreativeDestroyTrackerSource -match 'entity instanceof ItemEntity' `
    -and $mainCreativeDestroyTrackerSource -match 'entity instanceof Container') `
    'Mainline and 1.21.1 must retain semantic creative destruction detection, preset overwrite coverage, drop exclusion, and world-visible safety checks.'
Assert-Condition ($automatedPauseSource -match 'active-echo Shift\+left deletion' `
      -and $automatedPauseSource -match 'echoWarrior\$invokeSlotClicked' `
      -and $automatedPauseSource -match 'echoWarrior\$invokeSelectTab' `
      -and $automatedPauseSource -match 'getInventory\(\)\.getItem\(0\)' `
      -and $automatedPauseSource -match 'getInventory\(\)\.getItem\(1\)' `
      -and $automatedPauseSource -match 'getInventory\(\)\.getItem\(2\)' `
      -and $automatedPauseSource -match 'getInventory\(\)\.getItem\(3\)' `
      -and $automatedPauseSource -match 'ROMAN_LEGIONARY_RELIC' `
      -and $automatedPauseSource -match 'PLATE_ARMOR_ACCESSORY' `
      -and $automatedPauseSource -match 'ClickType\.QUICK_MOVE' `
      -and $automatedPauseSource -match 'DISCARD_SUMMONER_IN_CATALOG' `
      -and $automatedPauseSource -match 'creative catalog-delete pickup' `
      -and $automatedPauseSource -match 'EchoBindingSavedData1211\.get\(server\)\.get\(deletionSummonerId\)' `
      -and $automatedPauseSource -match 'EchoBindingSystem1211\.findLoaded\(server, deletionSpiritId\)' `
      -and $automatedPauseSource -match 'EchoBindingSavedData1211\.get\(server\)\.get\(catalogDeletionSummonerId\)' `
      -and $automatedPauseSource -match 'EchoBindingSystem1211\.findLoaded\(server, catalogDeletionSpiritId\)') `
    'Startup-only clients must exercise both creative insertion paths and prove Shift-delete plus carried-item catalog deletion remove their slots, bindings, and entities.'
Assert-Condition ($summonerScreenSource -match 'SKILL_DESCRIPTION_LINES') `
    'The summoner screen must render skill descriptions.'
Assert-Condition ($summonerScreenSource -match 'trait\.descriptionTranslationKey\(\)' `
    -and $summonerScreenSource -match 'renderAttributeTooltip' `
    -and $summonerScreenSource -match 'summoner\.experience\.progress' `
    -and $summonerScreenSource -match 'summoner\.fuel\.heal_cost' `
    -and $summonerScreenSource -match 'summoner\.fuel_input\.rotten_flesh' `
    -and $summonerScreenSource -match 'summoner\.accessory\.unique') `
    'The 1.21.1 summoner screen must expose the same concise talent, attribute, experience, fuel, and empty-slot guidance as the mainline screen.'
Assert-Condition ($summonerMenuSource -match 'DataSlot actionFeedback' `
    -and $summonerMenuSource -match 'ACTION_NO_SAFE_POSITION' `
    -and $summonerMenuSource -match 'ACTION_STALE_STATE' `
    -and $summonerMenuSource -match 'EchoSummonerItem1211\.SummonResult' `
    -and $summonerMenuSource -match 'reloadAuthoritativeStateIfChanged' `
    -and $summonerMenuSource -match 'public void clicked\(int slotId, int button, ClickType clickType' `
    -and $summonerScreenSource -match 'renderFeedbackToast' `
    -and $summonerScreenSource -match 'feedbackText\(\)' `
    -and $summonerScreenSource -match 'gui\.echo_warrior\.summoner\.feedback\.skill_changed' `
    -and $summonerScreenSource -match 'gui\.echo_warrior\.summoner\.feedback\.stale_state' `
    -and $bindingSavedDataSource -match 'StateRevision' `
    -and $bindingSavedDataSource -match 'markStateChanged\(\)') `
    'Server-confirmed summoner actions must produce translated in-screen feedback, while stale menus reload versioned authoritative state before accepting a click.'
Assert-Condition ($summonerItemSource -match 'TooltipShiftState1211\.isShiftDown\(\)' `
    -and $summonerItemSource -match 'test_echo_summoner\.tooltip\.summary' `
    -and $summonerItemSource -match 'test_echo_summoner\.tooltip\.detail\.current_echo' `
    -and $summonerItemSource -match 'test_echo_summoner\.tooltip\.detail\.healing' `
    -and $summonerItemSource -match 'test_echo_summoner\.tooltip\.detail\.quick_action' `
    -and $summonerItemSource -match 'test_echo_summoner\.tooltip\.detail\.direct_insert') `
    'The 1.21.1 summoner item must retain the mainline summary and Shift-expanded usage guidance.'
Assert-Condition ($knowledgeFragmentSource -match 'KnowledgeTooltip1211\.appendFragmentDetails\(tooltip\)' `
    -and $knowledgeTooltipSource -match 'TooltipShiftState1211\.isShiftDown\(\)' `
    -and $knowledgeTooltipSource -match 'knowledge_fragment\.detail\.craft' `
    -and $knowledgeTooltipSource -match 'knowledge_fragment\.detail\.recycle') `
    'The 1.21.1 knowledge fragment must retain the mainline Shift-expanded collection and recycling guidance.'
Assert-Condition ($relicItemSource -match 'if \(mask == 0\)' `
    -and $relicItemSource -match 'tooltip\.echo_warrior\.relic\.talents\.none') `
    'Relics without talents must keep the mainline concise no-talents tooltip instead of showing an empty header.'
Assert-Condition ($bindingConfigSource -match 'echo_warrior-bindings\.json' `
    -and $bindingConfigSource -match 'maxLivingEchoesPerController' `
    -and $fabricMainSource -match 'EchoBindingConfig1211\.load\(FabricLoader\.getInstance\(\)\.getConfigDir\(\)\)' `
    -and $neoForgeMainSource -match 'EchoBindingConfig1211\.load\(FMLPaths\.CONFIGDIR\.get\(\)\)' `
    -and $bindingSystemSource -match 'canAddControllerEcho' `
    -and $bindingSystemSource -match 'count <= 8' `
    -and $bindingSystemSource -match 'echo_count_performance_warning' `
    -and $bindingSavedDataSource -match 'WarningCooldowns') `
    'The compatibility line must preserve the configurable per-controller echo limit and rate-limited over-eight performance warning.'
Assert-Condition ($safeTeleportSource -match 'public static Vec3 findSafeDestination' `
    -and $bindingSystemSource -match 'EchoSafeTeleport1211\.findSafeDestination' `
    -and $bindingSystemSource -match 'SpawnFailure\.NO_SAFE_POSITION' `
    -and $summonerItemSource -match 'SummonResult\.NO_SAFE_POSITION' `
    -and $summonerItemSource -match 'EchoBindingSystem1211\.addFuel\(serverLevel, stack, summonCost\)') `
    'Initial summons must require a safe standing position, report the specific failure, and refund precommitted fuel.'
Assert-Condition ($gameplayCommandSource -match 'EchoBindingCommands1211\.command\(\)' `
    -and $bindingCommandSource -match 'Commands\.literal\("list"\)' `
    -and $bindingCommandSource -match 'Commands\.literal\("status"\)' `
    -and $bindingCommandSource -match 'Commands\.literal\("dismiss"\)' `
    -and $bindingCommandSource -match 'Commands\.literal\("repair"\)' `
    -and $bindingCommandSource -match 'EchoBindingSystem1211\.forceReconstruct' `
    -and $bindingSystemSource -match 'public static boolean dismiss\(MinecraftServer server, UUID summonerId\)' `
    -and $bindingSystemSource -match 'public static boolean forceReconstruct\(MinecraftServer server, UUID summonerId\)' `
    -and $selfTestCommandSource -match 'stateRevision\(\) > stateRevisionBeforeControls' `
    -and $selfTestCommandSource -match 'roundTripped\.stateRevision\(\) == binding\.stateRevision\(\)') `
    'The compatibility line must preserve mainline binding diagnostics, conservative recovery, and persistent state-revision self-tests.'
Assert-Condition ($summonerScreenSource -match 'this\.menu\.spiritHealth\(\) \+ "/" \+ this\.menu\.spiritMaximumHealth\(\)' `
    -and $summonerScreenSource -notmatch 'spiritPresent\(\) \? this\.menu\.spiritHealth') `
    'The summoner screen must show stored health instead of replacing it with a dash while dismissed.'
Assert-Condition ($summonerScreenSource -match 'blitScaled16') `
    'The summoner screen must scale full talent icons instead of cropping them.'
Assert-Condition ($summonerScreenSource -notmatch 'EGYPTIAN_NORMAL_ARROW_ICON' `
    -and $summonerScreenSource -match 'EGYPTIAN_CONE_ARROW_ICON' `
    -and $summonerScreenSource -match 'egyptian\.normal_arrow' `
    -and $summonerScreenSource -match 'SKILL_EMPTY' `
    -and $summonerScreenSource -match '20, 20, 20, 20' `
    -and $summonerScreenSource -match 'renderRadialCooldown' `
    -and $summonerScreenSource -match '0xFFE05050') `
    'The Egyptian arrow selector must cycle disabled/leaf/cone without inventing a normal-arrow skill icon.'
Assert-Condition ($tutorialScreenSource -match 'private List<Component> deferredTooltip' `
    -and $tutorialScreenSource -match 'super\.render\(graphics, mouseX, mouseY, partialTick\)' `
    -and $tutorialScreenSource -match 'graphics\.flush\(\)' `
    -and $tutorialScreenSource -match 'graphics\.renderComponentTooltip' `
    -and $tutorialScreenSource -match 'this\.deferredTooltip = List\.copyOf\(tooltip\)') `
    'Tutorial tooltips must be deferred until every page icon has rendered so they remain the top GUI layer.'
Assert-Condition ($egyptianSource -match 'setAnimationSpeedHandler' `
    -and $egyptianSource -match 'actionAnimationSpeed' `
    -and $egyptianSource -match 'getTriggeredAnimation\(\)' `
    -and $egyptianSource -match 'triggeredAnimation == FIRST_DRAW_SEQUENCE_UPPER' `
    -and $egyptianSource -match 'EgyptianArcherAnimationServer' `
    -and $egyptianSource -match 'animation\.egyptian_archer\.draw_bow_upper' `
    -and $egyptianSource -match 'animation\.egyptian_archer\.reload_bow_upper' `
    -and $egyptianSource -match 'rangedPhaseBudget\(archer\.attackInterval\(\)\)\.releaseTicks\(\)' `
    -and $egyptianSource -match 'UNNOCK_ANIMATION_SECONDS' `
    -and $egyptianSource -match 'ACTION_CONTROLLER, 0, this::selectActionAnimation' `
    -and $egyptianSource -notmatch 'FIRST_DRAW_TRIGGER' `
    -and $egyptianSource -notmatch 'BOW_AIM_TRIGGER' `
    -and $egyptianSource -notmatch 'bow_aim_upper') `
    'GeckoLib 4 bow animations must keep nock and draw in one held authored sequence, bind speed to that trigger, and avoid restarting a looping aim trigger.'
Assert-Condition ($egyptianModelSource -match 'EgyptianArcherAnimationClient' `
    -and $egyptianModelSource -match 'getAnimationSpeed\(\)' `
    -and $egyptianModelSource -match 'nextAnimationDiagnosticTick') `
    'The development client must emit rate-limited Egyptian archer controller diagnostics.'
Assert-Condition ($containerScreenMixinSource -match 'loadSummonerAlphaMask' `
    -and $containerScreenMixinSource -match 'for \(int pixelY = 0; pixelY < 16; pixelY\+\+\)' `
    -and $containerScreenMixinSource -match 'double diagonal = \(pixelX \+ pixelY\) \* 0\.5' `
    -and $containerScreenMixinSource -match 'slotLeft \+ pixelX' `
    -and $containerScreenMixinSource -match 'pose\(\)\.translate\(0\.0F, 0\.0F, 250\.0F\)' `
    -and ([regex]::Matches($containerScreenMixinSource, 'graphics\.flush\(\)').Count -ge 2) `
    -and $containerScreenMixinSource -match 'RenderType\.guiOverlay\(\)' `
    -and $containerScreenMixinSource -match 'hoveredSlot != null \? this\.hoveredSlot : slot' `
    -and $containerScreenMixinSource -match 'SummonerFuelInsertFeedback1211\.drain' `
    -and $containerScreenMixinSource -match 'stage=render effect=POLISH' `
    -and $summonerFeedbackSource -match 'ConcurrentLinkedQueue' `
    -and $summonerFeedbackSource -match 'MAX_PENDING_NANOS' `
    -and $summonerFeedbackSource -match 'enqueue\(slot, new Feedback\(Effect\.POLISH' `
    -and $fabricClientSource -notmatch 'setClientHandler' `
    -and $neoForgeClientSource -notmatch 'setClientHandler' `
    -and $containerScreenMixinSource -notmatch 'SummonerInsertionParticle1211\.polish') `
    'Direct insertion polish must use the screen-consumed pending queue, flush into a no-depth overlay, clip to the summoner texture, and anchor to the actual target slot.'
Assert-Condition ($egyptianArrowSource -match 'return Items\.ARROW\.getDefaultInstance\(\)' `
    -and $egyptianArrowSource -notmatch 'getDefaultPickupItem\(\)\s*\{\s*return ItemStack\.EMPTY') `
    'Disabled-pickup Egyptian arrows still need a non-empty vanilla pickup stack so chunk saving can encode them.'
Assert-Condition ($egyptianModelSource -match 'inheritedRotation' `
    -and $egyptianModelSource -match 'extractEulerZyx' `
    -and $egyptianModelSource -match 'exactHeadFrameBlend' `
    -and $egyptianModelSource -match 'isRangedHeadFrameStabilized') `
    'The Egyptian archer must keep combat gaze stable with an exact inverse parent rotation.'
Assert-Condition ($mixinConfigSource -match 'CreativeModeInventoryScreenMixin1211' `
    -and $mixinConfigSource -match 'ItemInHandRendererMixin1211' `
    -and $itemInHandMixinSource -match 'isSamePhysicalSummoner' `
    -and $itemInHandMixinSource -match 'isSamePhysicalManual' `
    -and $itemInHandMixinSource -match 'isSamePhysicalCollection' `
    -and $tutorialStackDataSource -match 'isSamePhysicalManual' `
    -and $knowledgeStackDataSource -match 'isSamePhysicalCollection' `
    -and $selfTestCommandSource -match 'tutorial page bookmarks do not create a false held-item replacement' `
    -and $selfTestCommandSource -match 'knowledge collection page bookmarks do not create a false held-item replacement' `
    -and $itemInHandMixinSource -match 'this\.mainHandItem = currentMainHand' `
    -and $itemInHandMixinSource -match 'this\.offHandItem = currentOffHand') `
    'Creative synchronization plus summoner/page-bearing item re-equip suppression Mixins must be enabled in the shared client config.'
Assert-Condition ($mainItemInHandMixinSource -match 'KnowledgeStackData\.isSamePhysicalCollection' `
    -and $mainKnowledgeStackDataSource -match 'isSamePhysicalCollection') `
    'Mainline must suppress false held-item replacement when only a knowledge collection bookmark changes.'
Assert-Condition ($summonerRelicIconSource -match 'ItemProperties\.register' `
    -and $summonerRelicIconSource -match 'summoner_relic_icon' `
    -and $summonerRelicIconSource -match 'Minecraft\.getInstance\(\)\.screen == null' `
    -and $summonerRelicIconSource -match 'EchoSummonerItem1211\.relicStack' `
    -and $fabricClientSource -match 'SummonerRelicIconProperty1211\.register\(\)' `
    -and $neoForgeClientSource -match 'SummonerRelicIconProperty1211\.register\(\)') `
    'The 1.21.1 clients must register the GUI-only Shift summoner relic icon predicate.'
Assert-Condition ($summonerScreenSource -match 'gui\.echo_warrior\.summoner\.button\.summon' `
    -and $summonerScreenSource -match 'gui\.echo_warrior\.summoner\.button\.dismiss' `
    -and $summonerScreenSource -match 'gui\.echo_warrior\.summoner\.state\.selected' `
    -and $summonerScreenSource -notmatch 'gui\.echo_warrior\.compat_1211\.(?:summon|dismiss|activity|alert)' `
    -and $summonerItemSource -notmatch 'message\.echo_warrior\.compat_1211') `
    'Summoner controls and action feedback must use packaged translation keys instead of exposing raw compatibility keys.'
Assert-Condition ($fabricClientSource -match 'BlockRenderLayerMap\.INSTANCE\.putBlock' `
    -and $fabricClientSource -match 'RenderType\.cutoutMipped') `
    'Fabric must render the suspicious grass overlay through the cutout-mipped terrain layer.'
Assert-Condition ($suspiciousTextureGeneratorSource -match 'grass_block_side_overlay' `
    -and $suspiciousTextureGeneratorSource -match '"render_type": "minecraft:cutout_mipped"') `
    'Generated suspicious grass models must own a transparent side overlay and request NeoForge cutout-mipped rendering.'
Assert-Condition ($colorProviderSource -match 'BiomeColors\.getAverageGrassColor' `
    -and $colorProviderSource -match '0xFF5AEEFF') `
    'The shared color providers must restore biome grass tint and the cyan compass pointer.'
Assert-Condition ($compassItemSource -match 'style\.withColor\(0xE6E6E6\)' `
    -and $compassItemSource -match 'style\.withColor\(0xEEC39A\)') `
    'The Echo Compass tooltip must retain the mainline battlefield and brush highlight colors.'
Assert-Condition ($compassPulseSource -match 'MESSAGE_DURATION_NANOS\s*=\s*2_400_000_000L' `
    -and $compassPulseSource -match 'MESSAGE_SHAKE_DURATION_NANOS\s*=\s*650_000_000L' `
    -and $compassPulseSource -match 'MESSAGE_FADE_DURATION_NANOS\s*=\s*450_000_000L' `
    -and $compassPulseSource -match 'acceptOverlayMessage' `
    -and $compassPulseSource -match 'message\.echo_warrior\.echo_compass\.remaining_echoes' `
    -and $compassPulseSource -match 'renderMessage' `
    -and $compassPulseSource -match 'messageAlpha' `
    -and $compassPulseSource -match 'if \(compass\.isEmpty\(\)\) \{\s*clearPulse\(\)' `
    -and $compassPulseSource -match 'isTooltipShakeActive' `
    -and $compassPulseSource -match 'renderTooltipTitle' `
    -and $compassTooltipSource -match 'implements ClientTooltipComponent') `
    'All Echo Compass action-bar messages and pulse-time tooltip titles must use the mainline per-grapheme presentation.'
Assert-Condition ($guiMixinSource -match '@Mixin\(Gui\.class\)' `
    -and $guiMixinSource -match 'setOverlayMessage' `
    -and $guiMixinSource -match 'acceptOverlayMessage' `
    -and $guiGraphicsMixinSource -match 'EchoCompassTooltipTitle1211' `
    -and $guiGraphicsMixinSource -match 'isTooltipShakeActive' `
    -and $guiGraphicsMixinSource -match 'renderTooltip\(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II\)V' `
    -and $guiGraphicsInvokerSource -match '@Invoker\("renderTooltipInternal"\)' `
    -and $mixinConfigSource -match 'GuiMixin1211' `
    -and $mixinConfigSource -match 'GuiGraphicsMixin1211' `
    -and $mixinConfigSource -match 'GuiGraphicsInvoker1211') `
    'The shared client Mixin config must intercept only Echo Compass overlays and replace only its pulsing tooltip title.'
Assert-Condition ($recyclerItemRendererSource -match 'RecyclerChestBlockEntity1211' `
    -and $recyclerItemRendererSource -match 'dispatcher\.renderItem\(this\.itemChest') `
    'The recycler item must reuse the placed block-entity renderer so the lid, body, and latch cannot diverge.'
Assert-Condition ($recyclerRendererSource -match 'blockEntity\.getLevel\(\) == null' `
    -and $recyclerRendererSource -match 'ChestBlock\.FACING, Direction\.SOUTH') `
    'The recycler item must use the vanilla chest item orientation so its front latch faces the item camera.'
Assert-Condition ($recyclerItemRendererSource -match 'static RecyclerChestItemRenderer1211 getInstance') `
    'The recycler item renderer must be constructed lazily after vanilla model layers are available.'
Assert-Condition ($fabricClientSource -match 'ColorProviderRegistry\.BLOCK' `
    -and $fabricClientSource -match 'ColorProviderRegistry\.ITEM' `
    -and $fabricClientSource -match 'BuiltinItemRendererRegistry' `
    -and $fabricClientSource -match 'RecyclerChestItemRenderer1211\.getInstance') `
    'Fabric must register shared block/item colors and the recycler item renderer.'
Assert-Condition ($neoForgeClientSource -match 'RegisterColorHandlersEvent\.Block' `
    -and $neoForgeClientSource -match 'RegisterColorHandlersEvent\.Item' `
    -and $neoForgeClientSource -match 'RegisterClientExtensionsEvent' `
    -and $neoForgeClientSource -match 'RecyclerChestItemRenderer1211\.getInstance' `
    -and $neoForgeClientSource -notmatch 'new RecyclerChestItemRenderer1211') `
    'NeoForge must register shared block/item colors and the recycler item renderer.'
Assert-Condition ($legacySource -match 'startUsingItem' -and $legacySource -match 'UseAnim\.BOW' `
    -and $legacySource -match 'finishUsingItem') `
    'Non-craft legacies must use the hold-to-charge presentation.'
Assert-Condition ($romanSource -match 'stopActionTriggers' -and $romanSource -match 'stopTriggeredAnim') `
    'Roman melee attacks must reset held GeckoLib action triggers between damage cycles.'

foreach ($loaderBuildPath in @(
    (Join-Path $compatibilityRoot 'fabric\build.gradle'),
    (Join-Path $compatibilityRoot 'neoforge\build.gradle')
)) {
    $loaderBuild = Get-Content -LiteralPath $loaderBuildPath -Raw
    Assert-Condition ($loaderBuild -match "--username=([^']+)'") `
        "The 1.21.1 loader build is missing its deterministic test username: $loaderBuildPath"
    Assert-Condition ($matches[1].Length -le 16) `
        "The Minecraft test username '$($matches[1])' exceeds the 16-character login protocol limit."
}

function Get-ArchiveEntryText {
    param(
        [System.IO.Compression.ZipArchive]$Archive,
        [string]$EntryName
    )

    $entry = $Archive.GetEntry($EntryName)
    Assert-Condition ($null -ne $entry) "Missing archive entry: $EntryName"
    $stream = $entry.Open()
    try {
        $reader = [System.IO.StreamReader]::new($stream, [System.Text.Encoding]::UTF8, $true, 1024, $false)
        try {
            return $reader.ReadToEnd()
        }
        finally {
            $reader.Dispose()
        }
    }
    finally {
        $stream.Dispose()
    }
}

function Assert-ClassMajorVersion {
    param(
        [System.IO.Compression.ZipArchive]$Archive,
        [string]$EntryName,
        [int]$ExpectedMajor
    )

    $entry = $Archive.GetEntry($EntryName)
    Assert-Condition ($null -ne $entry) "Missing class entry: $EntryName"
    $stream = $entry.Open()
    try {
        $header = [byte[]]::new(8)
        $read = $stream.Read($header, 0, $header.Length)
        Assert-Condition ($read -eq 8) "Class header is truncated: $EntryName"
        Assert-Condition (
            $header[0] -eq 0xCA -and $header[1] -eq 0xFE -and $header[2] -eq 0xBA -and $header[3] -eq 0xBE
        ) "Invalid class magic: $EntryName"
        $major = ([int]$header[6] -shl 8) -bor [int]$header[7]
        Assert-Condition ($major -eq $ExpectedMajor) "$EntryName uses class major $major; expected $ExpectedMajor (Java 21)."
    }
    finally {
        $stream.Dispose()
    }
}

function Assert-Archive {
    param(
        [string]$Path,
        [ValidateSet('Fabric', 'NeoForge')]
        [string]$Loader
    )

    Assert-Condition (Test-Path -LiteralPath $Path -PathType Leaf) "$Loader JAR was not built: $Path"
    $archive = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $Path).Path)
    try {
        $nestedCompatMixinClasses = @($archive.Entries | Where-Object {
            $_.FullName -like 'com/yuriscat/echowarrior/compat/mixin/*$*.class'
        })
        Assert-Condition ($nestedCompatMixinClasses.Count -eq 0) `
            "$Loader JAR must not contain nested classes in the compatibility Mixin package."
        $commonEntries = @(
            'pack.mcmeta',
            'echo_warrior_1211.mixins.json',
            'assets/echo_warrior/lang/en_us.json',
            'assets/echo_warrior/lang/zh_cn.json',
            'assets/echo_warrior/models/item/test_echo_summoner.json',
            'assets/echo_warrior/models/item/roman_legionary_relic.json',
            'assets/echo_warrior/models/item/plate_armor_accessory.json',
            'assets/echo_warrior/models/item/hawkeye_lens_accessory.json',
            'assets/echo_warrior/models/item/feast_ham_accessory.json',
            'assets/echo_warrior/models/item/light_gathering_magnet_accessory.json',
            'assets/echo_warrior/models/item/victors_laurel_accessory.json',
            'assets/echo_warrior/textures/item/test_echo_summoner.png',
            'assets/echo_warrior/textures/item/roman_legionary_relic.png',
            'assets/echo_warrior/textures/item/plate_armor_accessory.png',
            'assets/echo_warrior/textures/item/hawkeye_lens_accessory.png',
            'assets/echo_warrior/textures/item/feast_ham_accessory.png',
            'assets/echo_warrior/textures/item/light_gathering_magnet_accessory.png',
            'assets/echo_warrior/textures/item/victors_laurel_accessory.png',
            'assets/echo_warrior/textures/entity/roman_legionary_echo.png',
            'assets/echo_warrior/geo/roman_legionary_echo.geo.json',
            'assets/echo_warrior/animations/roman_legionary_echo.animation.json',
            'assets/echo_warrior/textures/mob_effect/soldier_formation.png',
            'assets/echo_warrior/textures/mob_effect/weapons_raised.png',
            'assets/echo_warrior/textures/mob_effect/shields_raised.png',
            'assets/echo_warrior/textures/mob_effect/huitzilopochtli_blessing.png',
            'assets/echo_warrior/textures/mob_effect/obsidian_wound.png',
            'assets/echo_warrior/textures/gui/summoner/summoner_screen.png',
            'assets/echo_warrior/textures/gui/summoner/summoner_widgets.png',
            'assets/echo_warrior/textures/gui/summoner/widgets/summon_default.png',
            'assets/echo_warrior/textures/gui/summoner/bars/fuel_fill.png',
            'assets/echo_warrior/textures/gui/summoner/modes/activity/follow.png',
            'assets/echo_warrior/textures/gui/summoner/modes/activity/wait.png',
            'assets/echo_warrior/textures/gui/summoner/modes/activity/wander.png',
            'assets/echo_warrior/textures/gui/summoner/modes/alert/aggressive.png',
            'assets/echo_warrior/textures/gui/summoner/modes/alert/defensive.png',
            'assets/echo_warrior/textures/gui/summoner/modes/alert/peaceful.png',
            'assets/echo_warrior/textures/gui/summoner/skills/roman_legionary/soldier_formation.png',
            'assets/echo_warrior/textures/gui/summoner/skills/roman_legionary/legionary_bulwark.png',
            'assets/echo_warrior/textures/gui/summoner/skills/roman_legionary/shield_charge.png',
            'assets/echo_warrior/textures/gui/summoner/skills/roman_legionary/legion_endures.png',
            'assets/echo_warrior/textures/gui/summoner/traits/bad_temper.png',
            'assets/echo_warrior/textures/gui/summoner/traits/biome_affinity_woodland.png',
            'data/echo_warrior/tags/worldgen/biome/talent_affinity/woodland.json',
            'data/echo_warrior/tags/item/summoner_accessories.json',
            'data/echo_warrior/tags/item/accessories/culture/roman.json',
            'data/echo_warrior/tags/item/accessories/rarity/common.json',
            'data/echo_warrior/tags/item/accessories/rarity/uncommon.json',
            'data/echo_warrior/tags/item/accessories/rarity/rare.json',
            'com/yuriscat/echowarrior/compat/EchoWarrior1211.class',
            'com/yuriscat/echowarrior/compat/ModContent1211.class',
            'com/yuriscat/echowarrior/compat/ModCreativeTabs1211.class',
            'com/yuriscat/echowarrior/compat/binding/EchoBindingSavedData1211.class',
            'com/yuriscat/echowarrior/compat/binding/EchoBindingSystem1211.class',
            'com/yuriscat/echowarrior/compat/binding/EchoBindingConfig1211.class',
            'com/yuriscat/echowarrior/compat/command/EchoBindingCommands1211.class',
            'com/yuriscat/echowarrior/compat/entity/behavior/EchoSafeTeleport1211.class',
            'com/yuriscat/echowarrior/compat/combat/FormationAura1211.class',
            'com/yuriscat/echowarrior/compat/item/EchoRelicProgress1211.class',
            'com/yuriscat/echowarrior/compat/item/EchoAccessoryItem1211.class',
            'com/yuriscat/echowarrior/compat/item/EchoAccessorySystem1211.class',
            'com/yuriscat/echowarrior/compat/item/EchoSummonerAccessory1211.class',
            'com/yuriscat/echowarrior/compat/item/EchoRelicState1211.class',
            'com/yuriscat/echowarrior/compat/item/EchoTalentSystem1211.class',
            'com/yuriscat/echowarrior/compat/item/EchoTrait1211.class',
            'com/yuriscat/echowarrior/compat/progress/EchoExperienceSystem1211.class',
            'com/yuriscat/echowarrior/compat/mixin/BlockTalentMixin1211.class',
            'com/yuriscat/echowarrior/compat/mixin/ExperienceOrbTalentMixin1211.class',
            'com/yuriscat/echowarrior/compat/mixin/FishingHookTalentMixin1211.class',
            'com/yuriscat/echowarrior/compat/mixin/LivingEntityMixin1211.class',
            'com/yuriscat/echowarrior/compat/mixin/MerchantTalentMixin1211.class',
            'com/yuriscat/echowarrior/compat/mixin/PlayerTalentMixin1211.class',
            'com/yuriscat/echowarrior/compat/entity/RomanLegionaryEchoEntity1211.class',
            'com/yuriscat/echowarrior/compat/entity/RomanVisualMath1211.class',
            'com/yuriscat/echowarrior/compat/item/EchoSummonerItem1211.class',
            'com/yuriscat/echowarrior/compat/item/SummonerStackContents1211.class',
            'com/yuriscat/echowarrior/compat/item/EchoCompassItem1211.class',
            'com/yuriscat/echowarrior/compat/item/KnowledgeFragmentItem1211.class',
            'com/yuriscat/echowarrior/compat/item/KnowledgeTooltip1211.class',
            'com/yuriscat/echowarrior/compat/item/KnowledgeFragmentCollectionItem1211.class',
            'com/yuriscat/echowarrior/compat/item/TutorialManualItem1211.class',
            'com/yuriscat/echowarrior/compat/item/SummonerFuelInsertFeedback1211.class',
            'com/yuriscat/echowarrior/compat/menu/SummonerMenu1211.class',
            'com/yuriscat/echowarrior/compat/network/CreativeSummonerInsertionPayload1211.class',
            'com/yuriscat/echowarrior/compat/network/CreativeSummonerDestructionPayload1211.class',
            'com/yuriscat/echowarrior/compat/binding/CreativeSummonerDestroyTracker1211.class',
            'com/yuriscat/echowarrior/compat/client/SummonerScreen1211.class',
            'com/yuriscat/echowarrior/compat/client/CreativeSummonerInsertionSender1211.class',
            'com/yuriscat/echowarrior/compat/client/CreativeSummonerDestructionSender1211.class',
            'com/yuriscat/echowarrior/compat/client/SummonerRelicIconProperty1211.class',
            'com/yuriscat/echowarrior/compat/client/KnowledgeReaderScreen1211.class',
            'com/yuriscat/echowarrior/compat/client/TutorialManualScreen1211.class',
            'com/yuriscat/echowarrior/compat/mixin/AbstractContainerScreenMixin1211.class',
            'com/yuriscat/echowarrior/compat/mixin/CreativeModeInventoryScreenMixin1211.class',
            'com/yuriscat/echowarrior/compat/mixin/ItemInHandRendererMixin1211.class',
            'com/yuriscat/echowarrior/compat/mixin/ServerGamePacketListenerMixin1211.class',
            'com/yuriscat/echowarrior/compat/client/SummonerInsertionParticle1211.class',
            'com/yuriscat/echowarrior/compat/client/SummonerInsertionPolish1211.class',
            'com/yuriscat/echowarrior/compat/client/RomanLegionaryRenderer1211.class',
            'com/yuriscat/echowarrior/compat/client/RomanLegionaryModel1211.class',
            'META-INF/LICENSE_ECHO_WARRIOR',
            'META-INF/LICENSE-CODE_ECHO_WARRIOR',
            'META-INF/LICENSE-ASSETS_ECHO_WARRIOR.md',
            'META-INF/NOTICE_ECHO_WARRIOR',
            'META-INF/CREDITS_ECHO_WARRIOR.md'
        )

        $heroIds = @(
            'roman_legionary',
            'aztec_warrior',
            'egyptian_archer',
            'guandao_warrior',
            'japanese_samurai'
        )
        $relicIds = $heroIds | ForEach-Object { "${_}_relic" }
        $legacyIds = @('courage_legacy', 'fortitude_legacy', 'purity_legacy', 'wisdom_legacy', 'craft_legacy')
        $accessoryIds = @(
            'plate_armor_accessory',
            'chainmail_armor_accessory',
            'spiked_armor_accessory',
            'battle_worn_whetstone_accessory',
            'mountain_burden_blade_accessory',
            'fractured_crystal_blade_accessory',
            'twin_oath_badge_accessory',
            'battle_blindfold_accessory',
            'crack_ring_hammer_charm_accessory',
            'victors_laurel_accessory',
            'blood_pact_fang_accessory',
            'memory_ritual_knife_accessory',
            'substitute_doll_accessory',
            'heart_sprout_amber_accessory',
            'feast_ham_accessory',
            'peacemaker_accessory',
            'sunwheel_garland_accessory',
            'moondew_bottle_accessory',
            'tomato_fish_accessory',
            'cat_bell_fish_charm_accessory',
            'light_gathering_magnet_accessory',
            'training_notes_accessory',
            'hawkeye_lens_accessory',
            'windchaser_feather_accessory',
            'hollow_bird_bone_accessory'
        )
        $simpleTextureItemIds = @(
            'test_echo_summoner',
            'knowledge_fragment',
            'knowledge_fragment_collection',
            'tutorial_manual'
        ) + $relicIds + $legacyIds + $accessoryIds
        $blockItemIds = @('echo_recycler', 'suspicious_grass_block', 'suspicious_dirt')
        $itemIds = @('echo_compass') + $simpleTextureItemIds + $blockItemIds
        Assert-Condition ($itemIds.Count -eq 43) 'The expected 1.21.1 item catalogue must contain 43 items.'
        foreach ($itemId in $itemIds) {
            $commonEntries += "assets/echo_warrior/models/item/$itemId.json"
        }
        foreach ($itemId in $simpleTextureItemIds) {
            $commonEntries += "assets/echo_warrior/textures/item/$itemId.png"
        }
        $commonEntries += @(
            'assets/echo_warrior/textures/item/echo_compass/echo_compass_background.png',
            'assets/echo_warrior/textures/item/echo_compass/echo_compass_pointer_16.png',
            'assets/echo_warrior/textures/item/echo_compass/echo_compass_highlight.png',
            'assets/echo_warrior/textures/item/echo_compass/echo_compass_frame_copper.png',
            'assets/echo_warrior/textures/item/echo_compass/echo_compass_frame_iron.png',
            'assets/echo_warrior/textures/item/echo_compass/echo_compass_frame_gold.png',
            'assets/echo_warrior/textures/gui/knowledge/knowledge_fragment.png',
            'assets/echo_warrior/textures/gui/knowledge/knowledge_collection.png',
            'assets/echo_warrior/textures/gui/knowledge/knowledge_previous_shadow.png',
            'assets/echo_warrior/textures/gui/knowledge/knowledge_next_shadow.png',
            'assets/echo_warrior/textures/gui/knowledge/knowledge_extract_shadow.png',
            'assets/echo_warrior/textures/gui/tutorial/chapter_tab.png',
            'assets/echo_warrior/textures/gui/tutorial/recipe.png',
            'assets/echo_warrior/textures/gui/tutorial/credits_portrait.png',
            'assets/echo_warrior/textures/gui/tutorial/paper.png',
            'assets/echo_warrior/textures/gui/recycler/recycler.png',
            'assets/echo_warrior/blockstates/echo_recycler.json',
            'assets/echo_warrior/blockstates/suspicious_grass_block.json',
            'assets/echo_warrior/blockstates/suspicious_dirt.json',
            'assets/echo_warrior/models/block/echo_recycler.json',
            'assets/echo_warrior/textures/entity/chest/recycler.png',
            'data/echo_warrior/knowledge/entries.json'
        )

        foreach ($cultureIndex in 0..9) {
            $cultureFrame = $cultureIndex.ToString('00')
            foreach ($dustStage in 0..3) {
                $commonEntries += "assets/echo_warrior/models/block/suspicious_dirt_c${cultureFrame}_s${dustStage}.json"
                $commonEntries += "assets/echo_warrior/models/block/suspicious_grass_block_c${cultureFrame}_s${dustStage}.json"
                $commonEntries += "assets/echo_warrior/textures/block/suspicious_dirt_c${cultureFrame}_s${dustStage}.png"
                $commonEntries += "assets/echo_warrior/textures/block/suspicious_grass_block_side_c${cultureFrame}_s${dustStage}.png"
                $commonEntries += "assets/echo_warrior/textures/block/suspicious_grass_block_side_overlay_c${cultureFrame}_s${dustStage}.png"
                $commonEntries += "assets/echo_warrior/textures/block/suspicious_grass_block_top_c${cultureFrame}_s${dustStage}.png"
            }
        }

        foreach ($index in 0..31) {
            $frame = $index.ToString('00')
            $commonEntries += "assets/echo_warrior/models/item/echo_compass_pointer_$frame.json"
            $commonEntries += "assets/echo_warrior/models/item/echo_compass_state_$frame.json"
            $commonEntries += "assets/echo_warrior/models/item/echo_compass_state_iron_$frame.json"
            $commonEntries += "assets/echo_warrior/models/item/echo_compass_state_gold_$frame.json"
            $commonEntries += "assets/echo_warrior/textures/item/echo_compass/echo_compass_pointer_$frame.png"
        }

        foreach ($heroId in $heroIds) {
            $commonEntries += "assets/echo_warrior/textures/entity/${heroId}_echo.png"
            $commonEntries += "assets/echo_warrior/geo/${heroId}_echo.geo.json"
            $commonEntries += "assets/echo_warrior/animations/${heroId}_echo.animation.json"
        }
        $commonEntries += @(
            'assets/echo_warrior/textures/entity/japanese_samurai_afterimage_detail.png',
            'assets/echo_warrior/textures/entity/japanese_samurai_afterimage_silhouette.png',
            'assets/echo_warrior/textures/effect/samurai_afterimage_dissolve.png',
            'assets/echo_warrior/shaders/core/samurai_afterimage.vsh',
            'assets/echo_warrior/shaders/core/samurai_afterimage.fsh'
        )

        $skillEntries = @(
            'roman_legionary/soldier_formation.png',
            'roman_legionary/legionary_bulwark.png',
            'roman_legionary/shield_charge.png',
            'roman_legionary/legion_endures.png',
            'aztec_warrior/huitzilopochtlis_blessing.png',
            'aztec_warrior/macuahuitl_mastery.png',
            'aztec_warrior/obsidian_wound.png',
            'aztec_warrior/pursuit.png',
            'aztec_warrior/quetzalcoatls_curse.png',
            'egyptian_archer/backstep.png',
            'egyptian_archer/cat_god.png',
            'egyptian_archer/chariot_volley.png',
            'egyptian_archer/cone_arrow.png',
            'egyptian_archer/leaf_arrow.png',
            'guandao_warrior/armor_clad.png',
            'guandao_warrior/crescent_blade.png',
            'guandao_warrior/growing_valor.png',
            'guandao_warrior/guandao_combo.png',
            'japanese_samurai/fumikomi.png',
            'japanese_samurai/stab.png',
            'japanese_samurai/zan.png',
            'japanese_samurai/zanshin.png'
        )
        foreach ($skillEntry in $skillEntries) {
            $commonEntries += "assets/echo_warrior/textures/gui/summoner/skills/$skillEntry"
        }

        foreach ($recipeId in @(
            'test_echo_summoner', 'craft_legacy_repair', 'echo_compass', 'echo_recycler',
            'knowledge_fragment_collection', 'tutorial_manual'
        ) + $accessoryIds) {
            $commonEntries += "data/echo_warrior/recipe/$recipeId.json"
        }
        foreach ($damageType in @(
            'armor_piercing_arrow', 'bleeding', 'obsidian_wound',
            'roman_first_strike', 'roman_followup', 'samurai_first_slash',
            'samurai_stab', 'spiked_armor_reflection'
        )) {
            $commonEntries += "data/echo_warrior/damage_type/$damageType.json"
        }
        foreach ($culture in @('roman', 'aztec', 'egyptian', 'chinese', 'japanese')) {
            $commonEntries += "data/echo_warrior/tags/item/accessories/culture/$culture.json"
            $commonEntries += "data/echo_warrior/loot_table/archaeology/battlefield_common_$culture.json"
            $commonEntries += "data/echo_warrior/loot_table/archaeology/battlefield_guaranteed_$culture.json"
            $commonEntries += "data/echo_warrior/loot_table/gameplay/knowledge_fragment/$culture.json"
        }
        foreach ($affinity in @('woodland', 'cold', 'underground', 'wasteland', 'waters')) {
            $commonEntries += "data/echo_warrior/tags/worldgen/biome/talent_affinity/$affinity.json"
        }
        $commonEntries += @(
            'data/echo_warrior/tags/worldgen/biome/aztec_favored_biomes.json',
            'data/echo_warrior/tags/worldgen/biome/has_battlefield_ruin.json',
            'data/echo_warrior/tags/block/battlefield_brushables.json',
            'data/echo_warrior/tags/item/battlefield_relics.json',
            'data/echo_warrior/tags/item/recycler/knowledge.json',
            'data/echo_warrior/tags/item/recycler/legacy.json',
            'data/echo_warrior/tags/item/recycler/relic.json',
            'data/echo_warrior/tags/item/recycler/accessory_common.json',
            'data/echo_warrior/tags/item/recycler/accessory_uncommon.json',
            'data/echo_warrior/tags/item/recycler/accessory_rare.json',
            'data/echo_warrior/loot_table/blocks/echo_recycler.json',
            'data/echo_warrior/loot_table/gameplay/knowledge_fragment/random.json',
            'data/echo_warrior/loot_table/gameplay/recycler/common.json',
            'data/echo_warrior/loot_table/gameplay/recycler/rare.json',
            'data/echo_warrior/loot_table/gameplay/recycler/super_rare.json',
            'data/echo_warrior/loot_table/archaeology/battlefield_guaranteed_relic.json',
            'data/echo_warrior/loot_table/archaeology/battlefield_common.json',
            'data/minecraft/tags/damage_type/bypasses_armor.json',
            'data/minecraft/tags/damage_type/bypasses_cooldown.json',
            'data/minecraft/tags/damage_type/no_knockback.json',
            'com/yuriscat/echowarrior/compat/entity/AztecWarriorEchoEntity1211.class',
            'com/yuriscat/echowarrior/compat/entity/EgyptianArcherEchoEntity1211.class',
            'com/yuriscat/echowarrior/compat/entity/EgyptianArcherArrowEntity1211.class',
            'com/yuriscat/echowarrior/compat/entity/GuandaoWarriorEchoEntity1211.class',
            'com/yuriscat/echowarrior/compat/entity/JapaneseSamuraiEchoEntity1211.class',
            'com/yuriscat/echowarrior/compat/entity/EchoAuraAuditSystem1211.class',
            'com/yuriscat/echowarrior/compat/entity/EchoCombatEvents1211.class',
            'com/yuriscat/echowarrior/compat/entity/CatGodCreeperSystem1211.class',
            'com/yuriscat/echowarrior/compat/item/LegacyItem1211.class',
            'com/yuriscat/echowarrior/compat/recipe/CraftLegacyRepairRecipe1211.class',
            'com/yuriscat/echowarrior/compat/recipe/KnowledgeFragmentCollectionRecipe1211.class',
            'com/yuriscat/echowarrior/compat/knowledge/KnowledgeLootSystem1211.class',
            'com/yuriscat/echowarrior/compat/command/GameplayTestCommands1211.class',
            'com/yuriscat/echowarrior/compat/tutorial/TutorialManualCatalog1211.class',
            'com/yuriscat/echowarrior/compat/tutorial/TutorialManualStackData1211.class',
            'com/yuriscat/echowarrior/compat/menu/RecyclerMenu1211.class',
            'com/yuriscat/echowarrior/compat/menu/KnowledgeReaderMenu1211.class',
            'com/yuriscat/echowarrior/compat/menu/TutorialManualMenu1211.class',
            'com/yuriscat/echowarrior/compat/block/StableBrushableBlock1211.class',
            'com/yuriscat/echowarrior/compat/block/RecyclerChestBlock1211.class',
            'com/yuriscat/echowarrior/compat/block/entity/RecyclerChestBlockEntity1211.class',
            'com/yuriscat/echowarrior/compat/item/SuspiciousBlockItem1211.class',
            'com/yuriscat/echowarrior/compat/item/RecyclerChestItem1211.class',
            'com/yuriscat/echowarrior/compat/recycler/RecyclerSystem1211.class',
            'com/yuriscat/echowarrior/compat/recycler/RecyclerClockData1211.class',
            'com/yuriscat/echowarrior/compat/world/BattlefieldCulture1211.class',
            'com/yuriscat/echowarrior/compat/world/BattlefieldSavedData1211.class',
            'com/yuriscat/echowarrior/compat/world/BattlefieldSystem1211.class',
            'com/yuriscat/echowarrior/compat/world/EchoCompassSystem1211.class',
            'com/yuriscat/echowarrior/compat/mixin/CreeperMixin1211.class',
            'com/yuriscat/echowarrior/compat/mixin/LivingEntityAccessoryMixin1211.class',
            'com/yuriscat/echowarrior/compat/client/AztecWarriorModel1211.class',
            'com/yuriscat/echowarrior/compat/client/AztecWarriorRenderer1211.class',
            'com/yuriscat/echowarrior/compat/client/EgyptianArcherModel1211.class',
            'com/yuriscat/echowarrior/compat/client/EgyptianArcherRenderer1211.class',
            'com/yuriscat/echowarrior/compat/client/EgyptianArcherArrowRenderer1211.class',
            'com/yuriscat/echowarrior/compat/client/GuandaoWarriorModel1211.class',
            'com/yuriscat/echowarrior/compat/client/GuandaoWarriorRenderer1211.class',
            'com/yuriscat/echowarrior/compat/client/JapaneseSamuraiModel1211.class',
            'com/yuriscat/echowarrior/compat/client/JapaneseSamuraiRenderer1211.class',
            'com/yuriscat/echowarrior/compat/client/RecyclerScreen1211.class',
            'com/yuriscat/echowarrior/compat/client/RecyclerChestRenderer1211.class',
            'com/yuriscat/echowarrior/compat/client/RecyclerChestItemRenderer1211.class',
            'com/yuriscat/echowarrior/compat/client/CompatColorProviders1211.class',
            'com/yuriscat/echowarrior/compat/client/EchoCompassClientProperties1211.class',
            'com/yuriscat/echowarrior/compat/client/EchoCompassPulseHud1211.class',
            'com/yuriscat/echowarrior/compat/client/EchoCompassTooltipTitle1211.class',
            'com/yuriscat/echowarrior/compat/mixin/GuiMixin1211.class',
            'com/yuriscat/echowarrior/compat/mixin/GuiGraphicsMixin1211.class',
            'com/yuriscat/echowarrior/compat/mixin/GuiGraphicsInvoker1211.class',
            'com/yuriscat/echowarrior/compat/client/TutorialRecipeCatalog1211.class',
            'com/yuriscat/echowarrior/compat/client/AutomatedTestPauseController1211.class'
        )
        foreach ($entryName in $commonEntries) {
            Assert-Condition ($null -ne $archive.GetEntry($entryName)) "$Loader JAR is missing $entryName"
        }

        foreach ($recipeId in @('test_echo_summoner', 'echo_recycler') + $accessoryIds) {
            $recipeEntry = "data/echo_warrior/recipe/$recipeId.json"
            $recipe = Get-ArchiveEntryText $archive $recipeEntry | ConvertFrom-Json
            Assert-Condition ($recipe.type -eq 'minecraft:crafting_shaped') `
                "$Loader $recipeEntry must be a shaped recipe"
            Assert-Condition ($recipe.result.id -eq "echo_warrior:$recipeId") `
                "$Loader $recipeEntry has the wrong result id"
            foreach ($ingredient in $recipe.key.PSObject.Properties) {
                $hasItem = $null -ne $ingredient.Value.item
                $hasTag = $null -ne $ingredient.Value.tag
                Assert-Condition ($hasItem -xor $hasTag) `
                    "$Loader $recipeEntry key '$($ingredient.Name)' must use a Minecraft 1.21.1 item/tag object"
            }
        }

        foreach ($languageEntry in @(
            'assets/echo_warrior/lang/en_us.json',
            'assets/echo_warrior/lang/zh_cn.json'
        )) {
            $language = Get-ArchiveEntryText $archive $languageEntry | ConvertFrom-Json
            $languageKeys = @($language.PSObject.Properties.Name)
            foreach ($itemId in @($itemIds | Where-Object { $_ -ne 'echo_recycler' })) {
                Assert-Condition ($languageKeys -contains "item.echo_warrior.$itemId") `
                    "$Loader $languageEntry is missing item.echo_warrior.$itemId"
            }
            foreach ($blockId in @('echo_recycler', 'suspicious_grass_block', 'suspicious_dirt')) {
                Assert-Condition ($languageKeys -contains "block.echo_warrior.$blockId") `
                    "$Loader $languageEntry is missing block.echo_warrior.$blockId"
            }
            foreach ($heroId in $heroIds) {
                Assert-Condition ($languageKeys -contains "hero.echo_warrior.$heroId") `
                    "$Loader $languageEntry is missing hero.echo_warrior.$heroId"
                Assert-Condition ($languageKeys -contains "entity.echo_warrior.${heroId}_echo") `
                    "$Loader $languageEntry is missing entity.echo_warrior.${heroId}_echo"
            }
            foreach ($effectId in @(
                'soldier_formation', 'weapons_raised', 'shields_raised',
                'huitzilopochtli_blessing', 'obsidian_wound', 'bleeding'
            )) {
                Assert-Condition ($languageKeys -contains "effect.echo_warrior.$effectId") `
                    "$Loader $languageEntry is missing effect.echo_warrior.$effectId"
            }
            Assert-Condition ($languageKeys -contains 'entity.echo_warrior.egyptian_archer_arrow') `
                "$Loader $languageEntry is missing entity.echo_warrior.egyptian_archer_arrow"
            foreach ($stateKey in @(
                'gui.echo_warrior.summoner.activity.follow.name',
                'gui.echo_warrior.summoner.activity.follow.description',
                'gui.echo_warrior.summoner.activity.wait.name',
                'gui.echo_warrior.summoner.activity.wait.description',
                'gui.echo_warrior.summoner.activity.wander.name',
                'gui.echo_warrior.summoner.activity.wander.description',
                'gui.echo_warrior.summoner.alert.aggressive.name',
                'gui.echo_warrior.summoner.alert.aggressive.description',
                'gui.echo_warrior.summoner.alert.defensive.name',
                'gui.echo_warrior.summoner.alert.defensive.description',
                'gui.echo_warrior.summoner.alert.peaceful.name',
                'gui.echo_warrior.summoner.alert.peaceful.description',
                'gui.echo_warrior.summoner.state.selected',
                'gui.echo_warrior.summoner.state.switch',
                'gui.echo_warrior.summoner.state.no_relic',
                'gui.echo_warrior.summoner.button.summon.tooltip.missing',
                'gui.echo_warrior.summoner.button.dismiss.tooltip',
                'gui.echo_warrior.summoner.button.summon.tooltip',
                'gui.echo_warrior.summoner.feedback.summoned',
                'gui.echo_warrior.summoner.feedback.dismissed',
                'gui.echo_warrior.summoner.feedback.no_relic',
                'gui.echo_warrior.summoner.feedback.not_enough_fuel',
                'gui.echo_warrior.summoner.skill.enabled',
                'gui.echo_warrior.summoner.skill.disabled'
            )) {
                Assert-Condition ($languageKeys -contains $stateKey) `
                    "$Loader $languageEntry is missing $stateKey"
            }
            $skillDescriptionCounts = [ordered]@{
                'roman.formation' = 2
                'roman.bulwark' = 1
                'roman.charge' = 2
                'roman.endures' = 2
                'aztec.quetzalcoatls_curse' = 1
                'aztec.huitzilopochtlis_blessing' = 2
                'aztec.obsidian_wound' = 1
                'aztec.pursuit' = 2
                'aztec.macuahuitl' = 2
                'egyptian.cat_god' = 2
                'egyptian.normal_arrow' = 2
                'egyptian.leaf_arrow' = 2
                'egyptian.cone_arrow' = 2
                'egyptian.chariot_volley' = 2
                'egyptian.backstep' = 2
                'guandao.armor_clad' = 1
                'guandao.growing_valor' = 2
                'guandao.crescent_blade' = 2
                'guandao.combo' = 3
                'samurai.zanshin' = 3
                'samurai.fumikomi' = 2
                'samurai.zan' = 2
                'samurai.stab' = 3
            }
            foreach ($skill in $skillDescriptionCounts.GetEnumerator()) {
                $baseKey = "gui.echo_warrior.summoner.skill.$($skill.Key)"
                Assert-Condition ($languageKeys -contains "$baseKey.name") `
                    "$Loader $languageEntry is missing $baseKey.name"
                foreach ($line in 1..$skill.Value) {
                    Assert-Condition ($languageKeys -contains "$baseKey.description.$line") `
                        "$Loader $languageEntry is missing $baseKey.description.$line"
                }
            }
            $skillDescriptionText = @($language.PSObject.Properties | Where-Object {
                $_.Name -match '^gui\.echo_warrior\.summoner\.skill\..*\.description\.\d+$'
            } | ForEach-Object { [string]$_.Value }) -join "`n"
            Assert-Condition ($skillDescriptionText -notmatch '完整演出|模型原速|血液?特效|鲜血粒子|箭矢尾迹|智能判断|full presentation|authored speed|blood particles|arrow trails|intelligently') `
                "$Loader $languageEntry contains development or purely visual wording in summoner skill descriptions."
        }

        $knowledge = Get-ArchiveEntryText $archive 'data/echo_warrior/knowledge/entries.json' | ConvertFrom-Json
        Assert-Condition (@($knowledge.entries).Count -eq 40) `
            "$Loader knowledge catalogue must contain exactly 40 entries."
        $illustratedEntries = @($knowledge.entries | Where-Object { $_.illustration_binding -eq 'high' })
        Assert-Condition ($illustratedEntries.Count -eq 27) `
            "$Loader knowledge catalogue must contain 27 illustrated entries."
        $illustrationCount = (@($knowledge.entries | ForEach-Object { @($_.illustrations).Count }) | Measure-Object -Sum).Sum
        foreach ($entry in $illustratedEntries) {
            foreach ($illustration in @($entry.illustrations)) {
                if ($illustration.type -eq 'item') {
                    $name = $illustration.resource.Replace(':', '_').Replace('/', '_')
                    foreach ($suffix in @('', '_faded')) {
                        $illustrationPath = "assets/echo_warrior/textures/gui/knowledge/illustrations/${name}${suffix}.png"
                        Assert-Condition ($null -ne $archive.GetEntry($illustrationPath)) `
                            "$Loader JAR is missing $illustrationPath"
                    }
                }
            }
        }
        Assert-Condition ($illustrationCount -eq 55) `
            "$Loader knowledge catalogue must contain 55 illustration bindings."

        $compassModel = Get-ArchiveEntryText $archive 'assets/echo_warrior/models/item/echo_compass.json' | ConvertFrom-Json
        Assert-Condition (@($compassModel.overrides).Count -eq 96) `
            "$Loader compass model must contain 32 copper, 32 iron, and 32 gold frame states."

        $summonerModel = Get-ArchiveEntryText $archive 'assets/echo_warrior/models/item/test_echo_summoner.json' | ConvertFrom-Json
        $summonerOverrides = @($summonerModel.overrides)
        Assert-Condition ($summonerOverrides.Count -eq 5) `
            "$Loader summoner model must contain five Shift relic icon overrides."
        $expectedRelicModels = @(
            'roman_legionary_relic',
            'aztec_warrior_relic',
            'egyptian_archer_relic',
            'guandao_warrior_relic',
            'japanese_samurai_relic'
        )
        for ($index = 0; $index -lt $expectedRelicModels.Count; $index++) {
            $override = $summonerOverrides[$index]
            $expectedPredicate = ($index + 1) / 10.0
            Assert-Condition ([double]$override.predicate.'echo_warrior:summoner_relic_icon' -eq $expectedPredicate) `
                "$Loader summoner relic icon override $index has the wrong predicate value."
            Assert-Condition ($override.model -eq "echo_warrior:item/$($expectedRelicModels[$index])") `
                "$Loader summoner relic icon override $index has the wrong model."
        }

        $grassModel = Get-ArchiveEntryText $archive `
            'assets/echo_warrior/models/block/suspicious_grass_block_c05_s0.json' | ConvertFrom-Json
        Assert-Condition ($grassModel.render_type -eq 'minecraft:cutout_mipped') `
            "$Loader suspicious grass model must use cutout-mipped rendering."
        Assert-Condition ($grassModel.textures.overlay -eq `
            'echo_warrior:block/suspicious_grass_block_side_overlay_c05_s0') `
            "$Loader suspicious grass model must use its generated transparent side overlay."

        $recyclerModel = Get-ArchiveEntryText $archive 'assets/echo_warrior/models/item/echo_recycler.json' | ConvertFrom-Json
        Assert-Condition ($recyclerModel.parent -eq 'minecraft:builtin/entity') `
            "$Loader recycler item model must use Minecraft's built-in entity renderer."

        foreach ($heroId in $heroIds) {
            $animations = Get-ArchiveEntryText $archive "assets/echo_warrior/animations/${heroId}_echo.animation.json"
            Assert-Condition ($animations -match [regex]::Escape("animation.$heroId.idle")) `
                "$Loader JAR is missing the $heroId idle animation"
        }

        $romanAnimations = Get-ArchiveEntryText $archive 'assets/echo_warrior/animations/roman_legionary_echo.animation.json'
        foreach ($animationName in @(
            'animation.roman_legionary.idle',
            'animation.roman_legionary.walk',
            'animation.roman_legionary.attack_first',
            'animation.roman_legionary.attack_recover',
            'animation.roman_legionary.attack_follow',
            'animation.roman_legionary.hurt',
            'animation.roman_legionary.shield_raise',
            'animation.roman_legionary.shield_lower'
        )) {
            Assert-Condition `
                ($romanAnimations -match [regex]::Escape($animationName)) `
                "$Loader JAR is missing Roman Legionary animation $animationName"
        }

        Assert-ClassMajorVersion $archive 'com/yuriscat/echowarrior/compat/EchoWarrior1211.class' 65

        if ($Loader -eq 'Fabric') {
            $metadata = Get-ArchiveEntryText $archive 'fabric.mod.json'
            Assert-Condition ($null -ne $archive.GetEntry('com/yuriscat/echowarrior/compat/fabric/EchoWarrior1211Fabric.class')) 'Fabric initializer class is missing.'
            Assert-ClassMajorVersion $archive 'com/yuriscat/echowarrior/compat/fabric/EchoWarrior1211Fabric.class' 65
            Assert-Condition ($null -eq $archive.GetEntry('META-INF/neoforge.mods.toml')) 'Fabric JAR unexpectedly contains NeoForge metadata.'
            Assert-Condition ($metadata -match '"id"\s*:\s*"echo_warrior"') 'Fabric mod id is incorrect.'
            Assert-Condition ($metadata -match '"minecraft"\s*:\s*"~1\.21\.1"') 'Fabric Minecraft version constraint is incorrect.'
            Assert-Condition ($metadata -match '"fabricloader"\s*:\s*">=0\.19\.5"') 'Fabric Loader version constraint is incorrect.'
        }
        else {
            $metadata = Get-ArchiveEntryText $archive 'META-INF/neoforge.mods.toml'
            Assert-Condition ($null -ne $archive.GetEntry('com/yuriscat/echowarrior/compat/neoforge/EchoWarrior1211NeoForge.class')) 'NeoForge initializer class is missing.'
            Assert-ClassMajorVersion $archive 'com/yuriscat/echowarrior/compat/neoforge/EchoWarrior1211NeoForge.class' 65
            Assert-Condition ($null -eq $archive.GetEntry('fabric.mod.json')) 'NeoForge JAR unexpectedly contains Fabric metadata.'
            Assert-Condition ($metadata -match 'modId\s*=\s*"echo_warrior"') 'NeoForge mod id is incorrect.'
            Assert-Condition ($metadata -match 'versionRange\s*=\s*"\[1\.21\.1,1\.21\.2\)"') 'NeoForge Minecraft version range is incorrect.'
            Assert-Condition ($metadata -match 'versionRange\s*=\s*"\[21\.1\.250,\)"') 'NeoForge version constraint is incorrect.'
        }
    }
    finally {
        $archive.Dispose()
    }

    $file = Get-Item -LiteralPath $Path
    $hash = Get-FileHash -LiteralPath $Path -Algorithm SHA256
    [pscustomobject]@{
        Loader = $Loader
        Path = $file.FullName
        Bytes = $file.Length
        SHA256 = $hash.Hash
    }
}

$results = @(
    Assert-Archive $fabricJar Fabric
    Assert-Archive $neoForgeJar NeoForge
)

$results | Format-Table -AutoSize
Write-Host 'Minecraft 1.21.1 dual-loader baseline validation passed.'
