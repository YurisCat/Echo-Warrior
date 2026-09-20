package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.item.*;
import com.yuriscat.echowarrior.compat.mixin.CreativeModeInventoryScreenInvoker1201;
import com.yuriscat.echowarrior.compat.mixin.CreativeModeSlotWrapperAccessor1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/** Exercises the actual creative catalogue, creative inventory wrappers and survival clicks. */
public final class InventoryInsertionClientSelfTest1201 {
    private static int phase, round, item;
    private static long deadline;
    private static CompletableFuture<Fixture> setup;
    private static CompletableFuture<Void> cleanup;
    private static Fixture fixture;
    private static AbstractContainerScreen<?> screen;
    private static long visiblePolishBefore;
    private static long fuelBefore;
    private static int insertSounds;
    private static final net.minecraft.client.sounds.SoundEventListener SOUND_LISTENER = (sound, event) -> {
        if (sound.getLocation().equals(net.minecraft.sounds.SoundEvents.BUNDLE_INSERT.getLocation())) {
            if (sound.getSound() == net.minecraft.client.sounds.SoundManager.EMPTY_SOUND)
                throw new IllegalStateException("Insertion sound has no resolved audio");
            insertSounds++;
        }
    };
    private InventoryInsertionClientSelfTest1201() {}

    public static boolean tick(Minecraft client) {
        if (round == 3) return true;
        MinecraftServer server = client.getSingleplayerServer();
        if (phase != 0 && phase != 8 && System.nanoTime() > deadline) {
            finish(client, server, "Inventory insertion timed out: round=" + round + " phase=" + phase + " item=" + item
                    + " screen=" + client.screen + " polish=" + (InsertionRenderAudit1201.visiblePolishFrames - visiblePolishBefore)
                    + " fuel=" + (InsertionRenderAudit1201.fuelBursts - fuelBefore));
        }
        switch (phase) {
            case 0 -> {
                UUID playerId = client.player.getUUID();
                GameType mode = round == 2 ? GameType.SURVIVAL : GameType.CREATIVE;
                setup = server.submit(() -> prepare(server, playerId, mode));
                item = 0;
                insertSounds = 0;
                client.getSoundManager().addListener(SOUND_LISTENER);
                visiblePolishBefore = InsertionRenderAudit1201.visiblePolishFrames;
                fuelBefore = InsertionRenderAudit1201.fuelBursts;
                advance(1);
            }
            case 1 -> { if (setup.isDone()) { fixture = setup.join(); advance(2); } }
            case 2 -> {
                if (fixture.id.equals(SummonerData1201.summonerId(client.player.getInventory().getItem(0)))
                        && client.gameMode.hasInfiniteItems() == (round != 2)) {
                    if (round == 2) screen = new InventoryScreen(client.player);
                    else screen = new CreativeModeInventoryScreen(client.player, client.level.enabledFeatures(), false);
                    client.setScreen(screen);
                    if (screen instanceof CreativeModeInventoryScreen creative) {
                        var tab = new net.minecraft.resources.ResourceLocation("minecraft", round == 0 ? "building_blocks" : "inventory");
                        ((CreativeModeInventoryScreenInvoker1201)creative).echoWarrior$invokeSelectTab(BuiltInRegistries.CREATIVE_MODE_TAB.get(tab));
                    }
                    advance(3);
                }
            }
            case 3 -> {
                if (round == 0) screen.getMenu().setCarried(fixture.sources.get(item).copy());
                else click(client, inventorySlot(9 + item));
                advance(4);
            }
            case 4 -> {
                if (ItemStack.matches(screen.getMenu().getCarried(), fixture.sources.get(item))) {
                    click(client, inventorySlot(0));
                    advance(5);
                }
            }
            case 5 -> {
                var slots = SummonerData1201.contents(client.player.getInventory().getItem(0));
                int target = item == 0 ? 7 : item == 1 ? 0 : 6;
                if (screen.getMenu().getCarried().isEmpty() && ItemStack.matches(slots.get(target), fixture.sources.get(item))) {
                    item++;
                    advance(item < 3 ? 3 : 6);
                }
            }
            case 6 -> {
                // Real rendering must have submitted visible sweep pixels, not only enqueued a request.
                if (InsertionRenderAudit1201.visiblePolishFrames > visiblePolishBefore
                        && InsertionRenderAudit1201.fuelBursts > fuelBefore) {
                    finish(client, server, insertSounds == 3 ? null : "Expected one resolved sound per accepted insertion, got " + insertSounds);
                }
            }
            case 8 -> {
                if (cleanup.isDone()) {
                    cleanup.join();
                    EchoWarrior1201.LOGGER.info("[Compat1201] INVENTORY INSERT CLIENT SELFTEST PASSED mode={} relic=preserved accessory=conserved fuel=conserved polish=rendered inventory=restored sound=3-resolved",
                            round == 0 ? "creative-catalogue" : round == 1 ? "creative-inventory" : "survival");
                    round++; phase = 0; fixture = null; screen = null;
                }
            }
            default -> { }
        }
        return round == 3;
    }

    private static Slot inventorySlot(int inventoryIndex) {
        if (round == 0) return screen.getMenu().getSlot(screen.getMenu().slots.size() - 9 + inventoryIndex);
        return screen.getMenu().slots.stream().filter(slot -> {
            Slot actual = slot instanceof CreativeModeSlotWrapperAccessor1201 wrapper ? wrapper.echoWarrior$getTarget() : slot;
            return actual.container == Minecraft.getInstance().player.getInventory() && actual.getContainerSlot() == inventoryIndex;
        }).findFirst().orElseThrow();
    }

    private static void click(Minecraft client, Slot slot) {
        if (screen instanceof CreativeModeInventoryScreen creative) {
            ((CreativeModeInventoryScreenInvoker1201)creative).echoWarrior$invokeSlotClicked(slot, slot.index, 0, ClickType.PICKUP);
        } else client.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slot.index, 0, ClickType.PICKUP, client.player);
    }

    private static void advance(int next) { phase = next; deadline = System.nanoTime() + 20_000_000_000L; }

    private static void finish(Minecraft client, MinecraftServer server, String failure) {
        client.getSoundManager().removeListener(SOUND_LISTENER);
        if (screen != null) screen.getMenu().setCarried(ItemStack.EMPTY);
        client.player.closeContainer();
        client.setScreen(new PauseScreen(true));
        client.mouseHandler.releaseMouse();
        int testedRound = round;
        cleanup = setup.thenCompose(current -> server.submit(() -> {
            try {
                if (failure != null) throw new IllegalStateException(failure);
                var binding = EchoBindingSavedData1201.get(server).get(current.id);
                if (binding == null || !ItemStack.matches(binding.contents().get(7), current.sources.get(0))
                        || !ItemStack.matches(binding.contents().get(0), current.sources.get(1))
                        || !ItemStack.matches(binding.contents().get(6), current.sources.get(2))) {
                    throw new IllegalStateException("Direct insertion did not persist authoritative contents");
                }
                if (testedRound != 0) for (int index = 9; index <= 11; index++) {
                    if (!current.player.getInventory().getItem(index).isEmpty()) throw new IllegalStateException("Direct insertion duplicated physical source " + index);
                }
            } finally { current.restore(server); }
        }));
        phase = 8;
    }

    private static Fixture prepare(MinecraftServer server, UUID playerId, GameType mode) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty())
            throw new IllegalStateException("Insertion fixture requires closed inventory");
        List<ItemStack> saved = IntStream.range(0, player.getInventory().getContainerSize()).mapToObj(i -> player.getInventory().getItem(i).copy()).toList();
        ItemStack summoner = new ItemStack(ModContent1201.ECHO_SUMMONER);
        var binding = EchoBindingSystem1201.synchronize(server.overworld(), summoner);
        ItemStack relic = new ItemStack(ModContent1201.relic(EchoHeroType1201.EGYPTIAN_ARCHER));
        EchoRelicState1201.ensureInitialized(relic, player.getRandom(), server.overworld().getGameTime());
        EchoRelicProgress1201.addExperience(relic, 100);
        List<ItemStack> sources = List.of(relic, new ItemStack(ModContent1201.accessory(EchoAccessoryItem1201.AccessoryType.PLATE_ARMOR)), new ItemStack(Items.SOUL_SAND, 8));
        Fixture result = new Fixture(player, saved, player.gameMode.getGameModeForPlayer(), binding.summonerId(), sources);
        try {
            binding.setFuel(1000);
            binding.mirrorTo(summoner);
            player.getInventory().setItem(0, summoner);
            for (int index = 0; index < 3; index++) player.getInventory().setItem(9 + index, sources.get(index).copy());
            player.setGameMode(mode);
            player.inventoryMenu.broadcastChanges();
            return result;
        } catch (RuntimeException error) { result.restore(server); throw error; }
    }

    private record Fixture(ServerPlayer player, List<ItemStack> inventory, GameType mode, UUID id, List<ItemStack> sources) {
        void restore(MinecraftServer server) {
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            for (int index = 0; index < inventory.size(); index++) player.getInventory().setItem(index, inventory.get(index).copy());
            player.setGameMode(mode);
            EchoBindingSavedData1201.get(server).remove(id);
            player.inventoryMenu.broadcastChanges();
        }
    }
}
