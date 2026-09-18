package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.item.EchoSummonerItem1211;
import com.yuriscat.echowarrior.compat.item.SummonerFuel1211;
import com.yuriscat.echowarrior.compat.mixin.CreativeModeInventoryScreenInvoker1211;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.GameType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Runs the client-only creative inventory regression and then releases the mouse. */
public final class AutomatedTestPauseController1211 {
    private static final boolean ENABLED = Boolean.getBoolean("echo_warrior.auto_pause_after_quick_play");
    private static Stage stage = Stage.WAIT_WORLD;
    private static int stageTicks;
    private static ItemStack originalFirstHotbarStack = ItemStack.EMPTY;
    private static ItemStack originalSecondHotbarStack = ItemStack.EMPTY;
    private static ItemStack originalThirdHotbarStack = ItemStack.EMPTY;
    private static ItemStack originalFourthHotbarStack = ItemStack.EMPTY;
    private static volatile UUID deletionSummonerId;
    private static volatile UUID deletionSpiritId;
    private static volatile UUID catalogDeletionSummonerId;
    private static volatile UUID catalogDeletionSpiritId;
    private static GameType originalGameType = GameType.SURVIVAL;
    private static CreativeModeInventoryScreen creativeScreen;
    private static volatile boolean serverStepComplete;
    private static volatile String serverFailure;
    private static volatile boolean serverVerificationPassed;
    private static volatile String serverVerificationSnapshot;
    private static int nextServerVerificationTick;

    private AutomatedTestPauseController1211() {
    }

    public static void tick(Minecraft client) {
        if (!ENABLED || stage == Stage.DONE || client.player == null || client.level == null) return;
        stageTicks++;
        try {
            switch (stage) {
                case WAIT_WORLD -> {
                    if (client.screen == null && stageTicks >= 20) requestCreativeMode(client);
                }
                case WAIT_CREATIVE -> {
                    checkServerFailure();
                    if (serverStepComplete && client.gameMode != null && client.gameMode.hasInfiniteItems()
                            && client.player.getInventory().getItem(0).is(ModContent1211.ECHO_SUMMONER)) {
                        openCreativeTestScreen(client);
                    }
                }
                case WAIT_SCREEN -> {
                    if (client.screen == creativeScreen && stageTicks >= 2) insertCategoryRelicThroughScreen();
                }
                case INSERT_CATEGORY_ACCESSORY -> {
                    if (stageTicks >= 2) insertCategoryAccessoryThroughScreen();
                }
                case WAIT_INVENTORY_TAB -> {
                    if (creativeScreen.isInventoryOpen() && stageTicks >= 2) insertInventoryRelicThroughScreen();
                }
                case INSERT_INVENTORY_ACCESSORY -> {
                    if (stageTicks >= 2) insertInventoryAccessoryThroughScreen();
                }
                case WAIT_DELETE_CATEGORY_TAB -> {
                    if (!creativeScreen.isInventoryOpen() && stageTicks >= 2) deleteActiveSummonerThroughScreen();
                }
                case PICKUP_CATALOG_DELETE_SUMMONER -> {
                    if (stageTicks >= 2) pickupCatalogDeleteSummoner();
                }
                case DISCARD_SUMMONER_IN_CATALOG -> {
                    if (stageTicks >= 2) discardCarriedSummonerInCatalog();
                }
                case WAIT_SERVER_VERIFY -> {
                    checkServerFailure();
                    if (serverVerificationPassed) {
                        restoreHotbar(client);
                    } else if (serverStepComplete && stageTicks >= nextServerVerificationTick) {
                        if (stageTicks >= 200) {
                            throw new IllegalStateException(serverVerificationSnapshot == null
                                    ? "server inventory verification timed out"
                                    : serverVerificationSnapshot);
                        }
                        serverStepComplete = false;
                        nextServerVerificationTick = stageTicks + 10;
                        requestServerInventoryVerification(client);
                    }
                }
                case WAIT_SLOT_RESTORE -> {
                    if (stageTicks >= 8) requestOriginalGameMode(client);
                }
                case WAIT_GAME_MODE_RESTORE -> {
                    checkServerFailure();
                    if (serverStepComplete) finish(client);
                }
                case DONE -> {
                }
            }
        } catch (Throwable throwable) {
            fail(client, throwable.getMessage() == null ? throwable.getClass().getName() : throwable.getMessage(), throwable);
        }
    }

    private static void requestCreativeMode(Minecraft client) {
        MinecraftServer server = requireServer(client);
        UUID playerId = client.player.getUUID();
        originalFirstHotbarStack = client.player.getInventory().getItem(0).copy();
        originalSecondHotbarStack = client.player.getInventory().getItem(1).copy();
        originalThirdHotbarStack = client.player.getInventory().getItem(2).copy();
        originalFourthHotbarStack = client.player.getInventory().getItem(3).copy();
        serverStepComplete = false;
        serverFailure = null;
        setStage(Stage.WAIT_CREATIVE);
        server.execute(() -> {
            try {
                ServerPlayer player = requireServerPlayer(server, playerId);
                originalGameType = player.gameMode.getGameModeForPlayer();
                player.setGameMode(GameType.CREATIVE);
                player.getInventory().setItem(0, new ItemStack(ModContent1211.ECHO_SUMMONER));
                player.getInventory().setItem(1, new ItemStack(ModContent1211.ECHO_SUMMONER));
                ItemStack deletionSummoner = new ItemStack(ModContent1211.ECHO_SUMMONER);
                EchoSummonerItem1211.setRelicStack(
                        deletionSummoner, new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC));
                SummonerFuel1211.setAmount(deletionSummoner, SummonerFuel1211.CAPACITY);
                EchoBindingSystem1211.SpawnAttempt attempt = EchoBindingSystem1211.summonNew(
                        player.serverLevel(), player, deletionSummoner);
                if (!attempt.succeeded()) {
                    throw new IllegalStateException("cannot summon creative deletion test echo: " + attempt.failure());
                }
                deletionSummonerId = EchoSummonerItem1211.getSummonerId(deletionSummoner);
                deletionSpiritId = attempt.spirit().livingEntity().getUUID();
                player.getInventory().setItem(2, deletionSummoner);

                ItemStack catalogDeletionSummoner = new ItemStack(ModContent1211.ECHO_SUMMONER);
                EchoSummonerItem1211.setRelicStack(
                        catalogDeletionSummoner, new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC));
                SummonerFuel1211.setAmount(catalogDeletionSummoner, SummonerFuel1211.CAPACITY);
                EchoBindingSystem1211.SpawnAttempt catalogAttempt = EchoBindingSystem1211.summonNew(
                        player.serverLevel(), player, catalogDeletionSummoner);
                if (!catalogAttempt.succeeded()) {
                    throw new IllegalStateException(
                            "cannot summon creative catalog deletion test echo: " + catalogAttempt.failure());
                }
                catalogDeletionSummonerId = EchoSummonerItem1211.getSummonerId(catalogDeletionSummoner);
                catalogDeletionSpiritId = catalogAttempt.spirit().livingEntity().getUUID();
                player.getInventory().setItem(3, catalogDeletionSummoner);
                player.inventoryMenu.broadcastChanges();
            } catch (Throwable throwable) {
                serverFailure = "cannot enter creative mode: " + throwable.getMessage();
            } finally {
                serverStepComplete = true;
            }
        });
    }

    private static void openCreativeTestScreen(Minecraft client) {
        creativeScreen = new CreativeModeInventoryScreen(
                client.player, client.player.connection.enabledFeatures(), false);
        client.setScreen(creativeScreen);
        selectTab(CreativeModeTabs.BUILDING_BLOCKS);
        setStage(Stage.WAIT_SCREEN);
    }

    private static void insertCategoryRelicThroughScreen() {
        ItemStack before = creativeScreen.getMenu().getSlot(45).getItem();
        if (!before.is(ModContent1211.ECHO_SUMMONER)) {
            throw new IllegalStateException("hotbar slot 45 is not the prepared summoner: " + before);
        }
        creativeScreen.getMenu().setCarried(new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC));
        clickSlot(45, 45);
        ItemStack result = creativeScreen.getMenu().getSlot(45).getItem();
        if (!creativeScreen.getMenu().getCarried().isEmpty()
                || !EchoSummonerItem1211.relicStack(result).is(ModContent1211.ROMAN_LEGIONARY_RELIC)) {
            throw new IllegalStateException("relic click did not update both the summoner and cursor: result="
                    + result + " carried=" + creativeScreen.getMenu().getCarried()
                    + " storedRelic=" + EchoSummonerItem1211.relicStack(result));
        }
        setStage(Stage.INSERT_CATEGORY_ACCESSORY);
    }

    private static void insertCategoryAccessoryThroughScreen() {
        creativeScreen.getMenu().setCarried(new ItemStack(ModContent1211.PLATE_ARMOR_ACCESSORY));
        clickSlot(45, 45);
        ItemStack result = creativeScreen.getMenu().getSlot(45).getItem();
        boolean accessoryPresent = EchoSummonerItem1211.accessoryStacks(result).stream()
                .anyMatch(stack -> stack.is(ModContent1211.PLATE_ARMOR_ACCESSORY));
        if (!creativeScreen.getMenu().getCarried().isEmpty() || !accessoryPresent) {
            throw new IllegalStateException("category-tab accessory click did not update both the summoner and cursor");
        }
        CreativeModeTab inventoryTab = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.INVENTORY);
        ((CreativeModeInventoryScreenInvoker1211)creativeScreen).echoWarrior$invokeSelectTab(inventoryTab);
        setStage(Stage.WAIT_INVENTORY_TAB);
    }

    private static void insertInventoryRelicThroughScreen() {
        ItemStack before = creativeScreen.getMenu().getSlot(37).getItem();
        if (!before.is(ModContent1211.ECHO_SUMMONER)) {
            throw new IllegalStateException("inventory-tab hotbar slot 37 is not the prepared summoner: " + before);
        }
        creativeScreen.getMenu().setCarried(new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC));
        clickSlot(37, creativeScreen.getMenu().getSlot(37).index);
        ItemStack result = creativeScreen.getMenu().getSlot(37).getItem();
        if (!creativeScreen.getMenu().getCarried().isEmpty()
                || !EchoSummonerItem1211.relicStack(result).is(ModContent1211.ROMAN_LEGIONARY_RELIC)) {
            throw new IllegalStateException("inventory-tab relic click did not update both the summoner and cursor");
        }
        setStage(Stage.INSERT_INVENTORY_ACCESSORY);
    }

    private static void insertInventoryAccessoryThroughScreen() {
        creativeScreen.getMenu().setCarried(new ItemStack(ModContent1211.PLATE_ARMOR_ACCESSORY));
        clickSlot(37, creativeScreen.getMenu().getSlot(37).index);
        ItemStack result = creativeScreen.getMenu().getSlot(37).getItem();
        boolean accessoryPresent = EchoSummonerItem1211.accessoryStacks(result).stream()
                .anyMatch(stack -> stack.is(ModContent1211.PLATE_ARMOR_ACCESSORY));
        if (!creativeScreen.getMenu().getCarried().isEmpty() || !accessoryPresent) {
            throw new IllegalStateException("inventory-tab accessory click did not update both the summoner and cursor");
        }
        selectTab(CreativeModeTabs.BUILDING_BLOCKS);
        setStage(Stage.WAIT_DELETE_CATEGORY_TAB);
    }

    private static void deleteActiveSummonerThroughScreen() {
        ItemStack before = creativeScreen.getMenu().getSlot(47).getItem();
        UUID beforeId = EchoSummonerItem1211.getSummonerId(before);
        if (!before.is(ModContent1211.ECHO_SUMMONER) || !deletionSummonerId.equals(beforeId)) {
            throw new IllegalStateException(
                    "category-tab hotbar slot 47 is not the prepared active summoner: " + before);
        }
        ((CreativeModeInventoryScreenInvoker1211)creativeScreen).echoWarrior$invokeSlotClicked(
                creativeScreen.getMenu().getSlot(47), 47, 0, ClickType.QUICK_MOVE);
        ItemStack after = creativeScreen.getMenu().getSlot(47).getItem();
        if (!after.isEmpty()) {
            throw new IllegalStateException("creative Shift+left quick deletion did not clear hotbar slot 47: " + after);
        }
        setStage(Stage.PICKUP_CATALOG_DELETE_SUMMONER);
    }

    private static void pickupCatalogDeleteSummoner() {
        ItemStack before = creativeScreen.getMenu().getSlot(48).getItem();
        UUID beforeId = EchoSummonerItem1211.getSummonerId(before);
        if (!before.is(ModContent1211.ECHO_SUMMONER) || !catalogDeletionSummonerId.equals(beforeId)) {
            throw new IllegalStateException(
                    "category-tab hotbar slot 48 is not the prepared catalog-delete summoner: " + before);
        }
        ((CreativeModeInventoryScreenInvoker1211)creativeScreen).echoWarrior$invokeSlotClicked(
                creativeScreen.getMenu().getSlot(48), 48, 0, ClickType.PICKUP);
        ItemStack carried = creativeScreen.getMenu().getCarried();
        if (!creativeScreen.getMenu().getSlot(48).getItem().isEmpty()
                || !catalogDeletionSummonerId.equals(EchoSummonerItem1211.getSummonerId(carried))) {
            throw new IllegalStateException(
                    "creative catalog-delete pickup did not move the active summoner onto the cursor: carried="
                            + carried + " slot=" + creativeScreen.getMenu().getSlot(48).getItem());
        }
        setStage(Stage.DISCARD_SUMMONER_IN_CATALOG);
    }

    private static void discardCarriedSummonerInCatalog() {
        ItemStack catalogItem = creativeScreen.getMenu().getSlot(0).getItem();
        if (catalogItem.isEmpty()) {
            throw new IllegalStateException("creative catalog slot 0 is empty during cursor-discard regression");
        }
        ((CreativeModeInventoryScreenInvoker1211)creativeScreen).echoWarrior$invokeSlotClicked(
                creativeScreen.getMenu().getSlot(0), 0, 0, ClickType.PICKUP);
        if (!creativeScreen.getMenu().getCarried().isEmpty()) {
            throw new IllegalStateException(
                    "clicking the creative catalog did not discard the carried active summoner: "
                            + creativeScreen.getMenu().getCarried());
        }
        serverStepComplete = true;
        serverVerificationPassed = false;
        serverVerificationSnapshot = null;
        nextServerVerificationTick = 40;
        setStage(Stage.WAIT_SERVER_VERIFY);
    }

    private static void clickSlot(int menuSlot, int clickSlotId) {
        ((CreativeModeInventoryScreenInvoker1211)creativeScreen).echoWarrior$invokeSlotClicked(
                creativeScreen.getMenu().getSlot(menuSlot), clickSlotId, 0, ClickType.PICKUP);
    }

    private static void selectTab(net.minecraft.resources.ResourceKey<CreativeModeTab> tabKey) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(tabKey);
        ((CreativeModeInventoryScreenInvoker1211)creativeScreen).echoWarrior$invokeSelectTab(tab);
    }

    private static void requestServerInventoryVerification(Minecraft client) {
        MinecraftServer server = requireServer(client);
        UUID playerId = client.player.getUUID();
        server.execute(() -> {
            try {
                ServerPlayer player = requireServerPlayer(server, playerId);
                ItemStack categoryResult = player.getInventory().getItem(0);
                ItemStack inventoryResult = player.getInventory().getItem(1);
                ItemStack deletionResult = player.getInventory().getItem(2);
                ItemStack catalogDeletionResult = player.getInventory().getItem(3);
                boolean categoryRelicPresent = EchoSummonerItem1211.relicStack(categoryResult)
                        .is(ModContent1211.ROMAN_LEGIONARY_RELIC);
                boolean categoryAccessoryPresent = EchoSummonerItem1211.accessoryStacks(categoryResult).stream()
                        .anyMatch(stack -> stack.is(ModContent1211.PLATE_ARMOR_ACCESSORY));
                boolean inventoryRelicPresent = EchoSummonerItem1211.relicStack(inventoryResult)
                        .is(ModContent1211.ROMAN_LEGIONARY_RELIC);
                boolean inventoryAccessoryPresent = EchoSummonerItem1211.accessoryStacks(inventoryResult).stream()
                        .anyMatch(stack -> stack.is(ModContent1211.PLATE_ARMOR_ACCESSORY));
                boolean deletionBindingPresent = deletionSummonerId != null
                        && EchoBindingSavedData1211.get(server).get(deletionSummonerId) != null;
                boolean deletionSpiritPresent = deletionSpiritId != null
                        && EchoBindingSystem1211.findLoaded(server, deletionSpiritId) != null;
                boolean catalogDeletionBindingPresent = catalogDeletionSummonerId != null
                        && EchoBindingSavedData1211.get(server).get(catalogDeletionSummonerId) != null;
                boolean catalogDeletionSpiritPresent = catalogDeletionSpiritId != null
                        && EchoBindingSystem1211.findLoaded(server, catalogDeletionSpiritId) != null;
                if (!categoryRelicPresent || !categoryAccessoryPresent
                        || !inventoryRelicPresent || !inventoryAccessoryPresent
                        || !deletionResult.isEmpty() || deletionBindingPresent || deletionSpiritPresent
                        || !catalogDeletionResult.isEmpty()
                        || catalogDeletionBindingPresent || catalogDeletionSpiritPresent) {
                    serverVerificationSnapshot = "creative UI regression did not converge before timeout: categoryResult="
                            + categoryResult + " categoryRelic=" + EchoSummonerItem1211.relicStack(categoryResult)
                            + " categoryAccessories=" + EchoSummonerItem1211.accessoryStacks(categoryResult)
                            + " inventoryResult=" + inventoryResult
                            + " inventoryRelic=" + EchoSummonerItem1211.relicStack(inventoryResult)
                            + " inventoryAccessories=" + EchoSummonerItem1211.accessoryStacks(inventoryResult)
                            + " deletionResult=" + deletionResult
                            + " deletionBindingPresent=" + deletionBindingPresent
                            + " deletionSpiritPresent=" + deletionSpiritPresent
                            + " catalogDeletionResult=" + catalogDeletionResult
                            + " catalogDeletionBindingPresent=" + catalogDeletionBindingPresent
                            + " catalogDeletionSpiritPresent=" + catalogDeletionSpiritPresent;
                } else {
                    serverVerificationPassed = true;
                    serverVerificationSnapshot = null;
                }
            } catch (Throwable throwable) {
                serverFailure = "cannot verify server inventory: " + throwable.getMessage();
            } finally {
                serverStepComplete = true;
            }
        });
    }

    private static void restoreHotbar(Minecraft client) {
        checkServerFailure();
        creativeScreen.getMenu().setCarried(ItemStack.EMPTY);
        client.setScreen(null);
        client.player.getInventory().setItem(0, originalFirstHotbarStack.copy());
        client.player.getInventory().setItem(1, originalSecondHotbarStack.copy());
        client.player.getInventory().setItem(2, originalThirdHotbarStack.copy());
        client.player.getInventory().setItem(3, originalFourthHotbarStack.copy());
        client.gameMode.handleCreativeModeItemAdd(originalFirstHotbarStack.copy(), 36);
        client.gameMode.handleCreativeModeItemAdd(originalSecondHotbarStack.copy(), 37);
        client.gameMode.handleCreativeModeItemAdd(originalThirdHotbarStack.copy(), 38);
        client.gameMode.handleCreativeModeItemAdd(originalFourthHotbarStack.copy(), 39);
        creativeScreen = null;
        setStage(Stage.WAIT_SLOT_RESTORE);
    }

    private static void requestOriginalGameMode(Minecraft client) {
        MinecraftServer server = requireServer(client);
        UUID playerId = client.player.getUUID();
        serverStepComplete = false;
        serverFailure = null;
        setStage(Stage.WAIT_GAME_MODE_RESTORE);
        server.execute(() -> {
            try {
                requireServerPlayer(server, playerId).setGameMode(originalGameType);
            } catch (Throwable throwable) {
                serverFailure = "cannot restore the original game mode: " + throwable.getMessage();
            } finally {
                serverStepComplete = true;
            }
        });
    }

    private static void finish(Minecraft client) {
        EchoWarrior1211.LOGGER.info(
                "Automated creative summoner test passed for relic/accessory synchronization, active-echo Shift+left deletion, and carried-summoner catalog deletion.");
        client.setScreen(new PauseScreen(true));
        stage = Stage.DONE;
        EchoWarrior1211.LOGGER.info("Automated client smoke test opened the pause menu to release the mouse.");
    }

    private static void fail(Minecraft client, String message, Throwable throwable) {
        try {
            if (client.player != null && client.gameMode != null) {
                if (creativeScreen != null) creativeScreen.getMenu().setCarried(ItemStack.EMPTY);
                client.setScreen(null);
                client.player.getInventory().setItem(0, originalFirstHotbarStack.copy());
                client.player.getInventory().setItem(1, originalSecondHotbarStack.copy());
                client.player.getInventory().setItem(2, originalThirdHotbarStack.copy());
                client.player.getInventory().setItem(3, originalFourthHotbarStack.copy());
                if (client.gameMode.hasInfiniteItems()) {
                    client.gameMode.handleCreativeModeItemAdd(originalFirstHotbarStack.copy(), 36);
                    client.gameMode.handleCreativeModeItemAdd(originalSecondHotbarStack.copy(), 37);
                    client.gameMode.handleCreativeModeItemAdd(originalThirdHotbarStack.copy(), 38);
                    client.gameMode.handleCreativeModeItemAdd(originalFourthHotbarStack.copy(), 39);
                }
            }
            MinecraftServer server = client.getSingleplayerServer();
            if (server != null && client.player != null) {
                UUID playerId = client.player.getUUID();
                server.execute(() -> {
                    ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                    if (deletionSummonerId != null && player != null) {
                        EchoBindingSystem1211.destroySummoner(player.serverLevel(), deletionSummonerId);
                    }
                    if (catalogDeletionSummonerId != null && player != null) {
                        EchoBindingSystem1211.destroySummoner(player.serverLevel(), catalogDeletionSummonerId);
                    }
                    if (player != null) player.setGameMode(originalGameType);
                });
            }
        } catch (Throwable ignored) {
        }
        EchoWarrior1211.LOGGER.error("Automated creative summoner insertion test failed: {}", message, throwable);
        client.setScreen(new PauseScreen(true));
        stage = Stage.DONE;
        EchoWarrior1211.LOGGER.info("Automated client smoke test opened the pause menu to release the mouse.");
    }

    private static MinecraftServer requireServer(Minecraft client) {
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("Quick Play did not start an integrated server");
        return server;
    }

    private static ServerPlayer requireServerPlayer(MinecraftServer server, UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) throw new IllegalStateException("integrated-server player is unavailable");
        return player;
    }

    private static void checkServerFailure() {
        if (serverStepComplete && serverFailure != null) throw new IllegalStateException(serverFailure);
    }

    private static void setStage(Stage next) {
        stage = next;
        stageTicks = 0;
    }

    private enum Stage {
        WAIT_WORLD,
        WAIT_CREATIVE,
        WAIT_SCREEN,
        INSERT_CATEGORY_ACCESSORY,
        WAIT_INVENTORY_TAB,
        INSERT_INVENTORY_ACCESSORY,
        WAIT_DELETE_CATEGORY_TAB,
        PICKUP_CATALOG_DELETE_SUMMONER,
        DISCARD_SUMMONER_IN_CATALOG,
        WAIT_SERVER_VERIFY,
        WAIT_SLOT_RESTORE,
        WAIT_GAME_MODE_RESTORE,
        DONE
    }
}
