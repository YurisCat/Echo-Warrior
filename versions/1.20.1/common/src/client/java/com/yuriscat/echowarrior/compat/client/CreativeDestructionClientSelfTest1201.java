package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.item.*;
import com.yuriscat.echowarrior.compat.mixin.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/** Actual creative-screen packets, world binding records and living hero lifetime. */
public final class CreativeDestructionClientSelfTest1201 {
    private static final String[] CASES = {"catalogue-shift-delete", "cursor-to-catalogue", "trash-single",
            "trash-shift-clear", "cursor-screen-close", "q-drop-retained", "inventory-shift-retained",
            "hotbar-preset-overwrite", "outside-drop-retained", "unproven-request-retained", "partial-scan-retained", "nested-container-delete"};
    private static int round, phase;
    private static long deadline;
    private static CompletableFuture<Fixture> setup;
    private static CompletableFuture<Void> cleanup;
    private static CompletableFuture<Boolean> verification;
    private static Fixture fixture;
    private static CreativeModeInventoryScreen screen;
    private static int verifyAfter;
    private CreativeDestructionClientSelfTest1201() {}

    public static boolean tick(Minecraft client) {
        if (round == CASES.length) return true;
        var server = client.getSingleplayerServer();
        if (phase != 0 && phase != 9 && System.nanoTime() > deadline)
            finish(client, server, "Creative destruction timed out: " + CASES[round] + " phase=" + phase);
        switch (phase) {
            case 0 -> {
                var id = client.player.getUUID();
                int testCase = round;
                setup = server.submit(() -> prepare(server, id, testCase));
                advance(1);
            }
            case 1 -> { if (setup.isDone()) { fixture = setup.join(); advance(2); } }
            case 2 -> {
                if (client.gameMode.hasInfiniteItems() && SummonerStackContents1201.scan(client.player.getInventory().getItem(0)).ids().contains(fixture.ids.get(0))) {
                    screen = new CreativeModeInventoryScreen(client.player, client.level.enabledFeatures(), false);
                    client.setScreen(screen);
                    boolean inventory = round == 2 || round == 3 || round == 6;
                    ((CreativeModeInventoryScreenInvoker1201)screen).echoWarrior$invokeSelectTab(BuiltInRegistries.CREATIVE_MODE_TAB.get(
                            new ResourceLocation("minecraft", inventory ? "inventory" : "building_blocks")));
                    advance(3);
                }
            }
            case 3 -> {
                switch (round) {
                    case 0, 6, 10, 11 -> click(inventorySlot(0), ClickType.QUICK_MOVE);
                    case 3 -> click(((CreativeModeInventoryScreenInvoker1201)screen).echoWarrior$getDestroyItemSlot(), ClickType.QUICK_MOVE);
                    case 5 -> click(inventorySlot(0), ClickType.THROW);
                    case 7 -> {
                        var preset = client.getHotbarManager().get(8);
                        var original = preset.stream().map(ItemStack::copy).toList();
                        try {
                            for (int index = 0; index < 9; index++) preset.set(index, ItemStack.EMPTY);
                            CreativeModeInventoryScreen.handleHotbarLoadOrSave(client, 8, true, false);
                        } finally { for (int index = 0; index < 9; index++) preset.set(index, original.get(index)); }
                    }
                    case 9 -> com.yuriscat.echowarrior.compat.network.InventoryNetwork1201.destructionSender.accept(
                            new com.yuriscat.echowarrior.compat.network.CreativeDestructionRequest1201(List.of(fixture.ids.get(0))));
                    default -> click(inventorySlot(0), ClickType.PICKUP);
                }
                advance(round == 1 || round == 2 || round == 4 || round == 8 ? 4 : 5);
            }
            case 4 -> {
                if (fixture.ids.get(0).equals(SummonerData1201.summonerId(screen.getMenu().getCarried()))) {
                    if (round == 1) click(screen.getMenu().getSlot(0), ClickType.PICKUP);
                    else if (round == 2) click(((CreativeModeInventoryScreenInvoker1201)screen).echoWarrior$getDestroyItemSlot(), ClickType.PICKUP);
                    else if (round == 8) {
                        // Exercise the actual outside hit-test too: slotClicked alone never sets hasClickedOutside.
                        screen.mouseClicked(-20, -20, 0);
                        screen.mouseReleased(-20, -20, 0);
                    }
                    else client.setScreen(null); // Automation's mouse guard still blocks capture; server must keep ticking.
                    advance(5);
                }
            }
            case 5 -> { verifyAfter = client.player.tickCount + 12; advance(6); }
            case 6 -> {
                if (client.player.tickCount >= verifyAfter) {
                    int tested = round;
                    var current = fixture;
                    verification = server.submit(() -> {
                        var data = EchoBindingSavedData1201.get(server);
                        boolean firstExists = data.get(current.ids.get(0)) != null;
                        boolean secondExists = data.get(current.ids.get(1)) != null;
                        if (firstExists == current.heroes.spirits.get(0).isRemoved()
                                || secondExists == current.heroes.spirits.get(1).isRemoved())
                            throw new IllegalStateException("Living entity does not match destruction authority");
                        if (tested == 3 || tested == 7) return !firstExists && !secondExists;
                        if (!secondExists) throw new IllegalStateException("Unrelated summoner was destroyed");
                        if (tested < 5 || tested == 11) return !firstExists;
                        if (!firstExists) throw new IllegalStateException("Ordinary move/drop destroyed binding");
                        if (tested == 9 || tested == 10) return true;
                        if (tested == 5 || tested == 8) {
                            int copies = (int)current.player.inventoryMenu.getItems().stream()
                                    .filter(stack -> current.ids.get(0).equals(SummonerData1201.summonerId(stack))).count();
                            for (var entity : current.player.serverLevel().getAllEntities()) {
                                if (entity instanceof ItemEntity dropped && current.ids.get(0).equals(SummonerData1201.summonerId(dropped.getItem()))) copies++;
                            }
                            // Vanilla may already have picked the nearby drop back up after its pickup delay.
                            if (copies != 1) throw new IllegalStateException("Drop must conserve exactly one physical summoner: copies=" + copies);
                            return true;
                        }
                        return current.player.getInventory().getItem(0).isEmpty() && current.player.inventoryMenu.getItems().stream()
                                .anyMatch(stack -> current.ids.get(0).equals(SummonerData1201.summonerId(stack)));
                    });
                    advance(7);
                }
            }
            case 7 -> {
                if (verification.isDone()) {
                    try {
                        if (!verification.join()) { finish(client, server, "Wrong creative destruction result: " + CASES[round]); break; }
                        finish(client, server, null);
                    } catch (RuntimeException error) { finish(client, server, error.toString()); }
                }
            }
            case 9 -> {
                if (cleanup.isDone()) {
                    cleanup.join();
                    EchoWarrior1201.LOGGER.info("[Compat1201] CREATIVE DESTRUCTION CLIENT SELFTEST PASSED case={} authority=verified unrelated=preserved inventory=restored entities=verified", CASES[round]);
                    round++; phase = 0; screen = null; fixture = null;
                }
            }
            default -> { }
        }
        return round == CASES.length;
    }

    private static Slot inventorySlot(int index) {
        if (round != 2 && round != 3 && round != 6) return screen.getMenu().getSlot(screen.getMenu().slots.size() - 9 + index);
        return screen.getMenu().slots.stream().filter(slot -> slot instanceof CreativeModeSlotWrapperAccessor1201 wrapper
                && wrapper.echoWarrior$getTarget().container == Minecraft.getInstance().player.getInventory()
                && wrapper.echoWarrior$getTarget().getContainerSlot() == index).findFirst().orElseThrow();
    }
    private static void click(Slot slot, ClickType type) {
        ((CreativeModeInventoryScreenInvoker1201)screen).echoWarrior$invokeSlotClicked(slot, slot.index, 0, type);
    }
    private static void advance(int next) { phase = next; deadline = System.nanoTime() + 20_000_000_000L; }
    private static void finish(Minecraft client, MinecraftServer server, String failure) {
        if (screen != null) screen.getMenu().setCarried(ItemStack.EMPTY);
        client.player.closeContainer();
        client.setScreen(new PauseScreen(true));
        client.mouseHandler.releaseMouse();
        cleanup = setup.thenCompose(current -> server.submit(() -> {
            try { if (failure != null) throw new IllegalStateException(failure); }
            finally { current.restore(server); }
        }));
        phase = 9;
    }

    private static Fixture prepare(MinecraftServer server, UUID playerId, int testCase) {
        var player = server.getPlayerList().getPlayer(playerId);
        if (player == null || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty())
            throw new IllegalStateException("Deletion fixture requires closed inventory");
        var inventory = IntStream.range(0, player.getInventory().getContainerSize()).mapToObj(i -> player.getInventory().getItem(i).copy()).toList();
        var oldMode = player.gameMode.getGameModeForPlayer();
        var heroes = com.yuriscat.echowarrior.compat.test.HeroClientFixture1201.prepare(player,
                EchoHeroType1201.values()[testCase % 5]);
        var firstSummoner = player.getInventory().getItem(0);
        var ids = heroes.ids;
        var fixture = new Fixture(player, inventory, oldMode, ids, heroes);
        try {
            for (int index = 0; index < inventory.size(); index++) player.getInventory().setItem(index, ItemStack.EMPTY);
            player.getInventory().setItem(0, firstSummoner);
            var spawn = EchoBindingSystem1201.summonNew(player.serverLevel(), player, firstSummoner);
            if (!spawn.succeeded()) throw new IllegalStateException("Deletion fixture spawn: " + spawn.failure());
            heroes.spirits.add(spawn.spirit().livingEntity());
            heroes.addSummoner(1, EchoHeroType1201.values()[(testCase + 1) % 5], true);
            if (testCase == 10) {
                var nested = new ItemStack(net.minecraft.world.item.Items.PAPER);
                for (int index = 0; index < 18; index++) {
                    var parent = new ItemStack(net.minecraft.world.item.Items.BUNDLE);
                    var list = new net.minecraft.nbt.ListTag();
                    list.add(nested.save(new net.minecraft.nbt.CompoundTag()));
                    parent.getOrCreateTag().put("Items", list); nested = parent;
                }
                player.getInventory().setItem(9, nested);
            }
            if (testCase == 11) {
                var box = new ItemStack(net.minecraft.world.item.Items.SHULKER_BOX);
                var list = new net.minecraft.nbt.ListTag();
                var entry = player.getInventory().getItem(0).save(new net.minecraft.nbt.CompoundTag());
                entry.putByte("Slot", (byte)0); list.add(entry);
                var contents = new net.minecraft.nbt.CompoundTag(); contents.put("Items", list);
                box.getOrCreateTag().put("BlockEntityTag", contents);
                player.getInventory().setItem(0, box);
            }
            player.setGameMode(GameType.CREATIVE);
            player.inventoryMenu.broadcastChanges();
            return fixture;
        } catch (RuntimeException error) { fixture.restore(server); throw error; }
    }
    private record Fixture(ServerPlayer player, List<ItemStack> inventory, GameType mode, List<UUID> ids,
            com.yuriscat.echowarrior.compat.test.HeroClientFixture1201 heroes) {
        void restore(MinecraftServer server) {
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            CreativeSummonerDestroyTracker1201.clearPlayer(player.getUUID());
            for (var level : server.getAllLevels()) {
                var ownedDrops = new ArrayList<ItemEntity>();
                for (var entity : level.getAllEntities()) if (entity instanceof ItemEntity dropped
                        && ids.contains(SummonerData1201.summonerId(dropped.getItem()))) ownedDrops.add(dropped);
                ownedDrops.forEach(ItemEntity::discard);
            }
            heroes.restore();
        }
    }
}
