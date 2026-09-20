package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.item.SummonerData1201;
import com.yuriscat.echowarrior.compat.EchoHeroType1201;
import com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicProgress1201;
import com.yuriscat.echowarrior.compat.menu.SummonerMenu1201;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/** Actual item-use/open-menu/vanilla click packets. Only enabled by the automated launcher. */
public final class MenuClientSelfTest1201 {
    private static int phase;
    private static int round;
    private static int stateBeforeReplay;
    private static long deadline;
    private static CompletableFuture<Fixture> setup;
    private static CompletableFuture<Void> cleanup;
    private static Fixture fixture;

    private MenuClientSelfTest1201() {}

    public static boolean tick(Minecraft client) {
        if (round == 2) return true;
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("Menu test needs integrated server");
        if (phase != 0 && phase != 10 && System.nanoTime() > deadline) {
            finish(client, server, "Timed out in menu client phase " + phase);
        }
        SummonerMenu1201 menu = client.player.containerMenu instanceof SummonerMenu1201 current ? current : null;
        switch (phase) {
            case 0 -> {
                UUID playerId = client.player.getUUID();
                GameType mode = round == 0 ? GameType.SURVIVAL : GameType.CREATIVE;
                setup = server.submit(() -> prepare(server, playerId, mode));
                advance(1);
            }
            case 1 -> {
                if (setup.isDone()) { fixture = setup.join(); advance(2); }
            }
            case 2 -> {
                if (fixture.id().equals(SummonerData1201.summonerId(client.player.getInventory().getItem(0)))
                        && client.player.getInventory().getItem(9).getCount() == 12) {
                    client.player.getInventory().selected = 0;
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    advance(3);
                }
            }
            case 3 -> {
                if (menu != null && client.screen instanceof SummonerScreen1201 && menu.sourceInventorySlot() == 0
                        && menu.getSlot(8).getItem().getCount() == 12 && menu.fuelAmount() == 1000) {
                    click(client, menu, 8, ClickType.QUICK_MOVE);
                    advance(4);
                }
            }
            case 4 -> {
                if (menu != null && menu.getSlot(6).getItem().getCount() == 12 && menu.getSlot(8).getItem().isEmpty()) {
                    click(client, menu, 6, ClickType.PICKUP);
                    advance(5);
                }
            }
            case 5 -> {
                if (menu != null && menu.getCarried().getCount() == 12 && menu.getSlot(6).getItem().isEmpty()) {
                    click(client, menu, 8, ClickType.PICKUP);
                    advance(6);
                }
            }
            case 6 -> {
                if (menu != null && menu.getCarried().isEmpty() && menu.getSlot(8).getItem().getCount() == 12) {
                    client.player.closeContainer();
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    advance(7);
                }
            }
            case 7 -> {
                if (menu != null && client.screen instanceof SummonerScreen1201 && menu.sourceInventorySlot() == 0
                        && menu.getSlot(8).getItem().getCount() == 12) {
                    if (!menu.getSlot(6).getItem().isEmpty()) { finish(client, server, "Reopened menu resurrected fuel"); break; }
                    stateBeforeReplay = menu.getStateId();
                    client.getConnection().send(new ServerboundContainerClickPacket(menu.containerId,
                            (stateBeforeReplay - 1) & 32767, 8, 0, ClickType.QUICK_MOVE,
                            ItemStack.EMPTY, new Int2ObjectOpenHashMap<>()));
                    advance(8);
                }
            }
            case 8 -> {
                if (menu != null && menu.getStateId() != stateBeforeReplay) {
                    if (!menu.getSlot(6).getItem().isEmpty() || menu.getSlot(8).getItem().getCount() != 12) {
                        finish(client, server, "Stale vanilla click transferred items"); break;
                    }
                    click(client, menu, 9, ClickType.QUICK_MOVE);
                    advance(11);
                }
            }
            case 10 -> {
                if (cleanup.isDone()) {
                    cleanup.join();
                    EchoWarrior1201.LOGGER.info("[Compat1201] MENU CLIENT SELFTEST PASSED mode={} open=real-use transfer=conserved relic=preserved accessory=conserved reopen=empty stale=rejected inventory=restored",
                            round == 0 ? "survival" : "creative");
                    fixture = null;
                    round++;
                    phase = 0;
                }
            }
            case 11 -> {
                if (menu != null && ItemStack.matches(menu.getSlot(7).getItem(), fixture.relic()) && menu.getSlot(9).getItem().isEmpty()) {
                    click(client, menu, 10, ClickType.PICKUP); advance(12);
                }
            }
            case 12 -> {
                if (menu != null && menu.getCarried().is(ModContent1201.accessory(AccessoryType.PLATE_ARMOR))) {
                    click(client, menu, 35, ClickType.PICKUP); advance(13);
                }
            }
            case 13 -> {
                if (menu != null && menu.getCarried().isEmpty() && menu.getSlot(0).getItem().is(ModContent1201.accessory(AccessoryType.PLATE_ARMOR))) {
                    click(client, menu, 0, ClickType.PICKUP); advance(14);
                }
            }
            case 14 -> {
                if (menu != null && menu.getSlot(0).getItem().isEmpty() && menu.getCarried().is(ModContent1201.accessory(AccessoryType.PLATE_ARMOR))) {
                    click(client, menu, 10, ClickType.PICKUP); advance(15);
                }
            }
            case 15 -> {
                if (menu != null && menu.getCarried().isEmpty() && menu.getSlot(10).getItem().is(ModContent1201.accessory(AccessoryType.PLATE_ARMOR))) {
                    click(client, menu, 7, ClickType.PICKUP); advance(16);
                }
            }
            case 16 -> {
                if (menu != null && menu.getSlot(7).getItem().isEmpty() && ItemStack.matches(menu.getCarried(), fixture.relic())) {
                    click(client, menu, 9, ClickType.PICKUP); advance(17);
                }
            }
            case 17 -> {
                if (menu != null && menu.getCarried().isEmpty() && ItemStack.matches(menu.getSlot(9).getItem(), fixture.relic())) {
                    client.player.closeContainer();
                    client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND); advance(18);
                }
            }
            case 18 -> {
                if (menu != null && menu.sourceInventorySlot() == 0 && ItemStack.matches(menu.getSlot(9).getItem(), fixture.relic())) {
                    if (!menu.getSlot(0).getItem().isEmpty() || !menu.getSlot(7).getItem().isEmpty()) {
                        finish(client, server, "Reopened menu resurrected equipment"); break;
                    }
                    finish(client, server, null);
                }
            }
            default -> { }
        }
        return round == 2;
    }

    private static void click(Minecraft client, SummonerMenu1201 menu, int slot, ClickType type) {
        client.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0, type, client.player);
    }

    private static void advance(int next) { phase = next; deadline = System.nanoTime() + 20_000_000_000L; }

    private static void finish(Minecraft client, MinecraftServer server, String failure) {
        client.player.closeContainer();
        client.setScreen(new PauseScreen(true));
        client.mouseHandler.releaseMouse();
        CompletableFuture<Fixture> prepared = setup;
        cleanup = prepared.thenCompose(current -> server.submit(() -> {
            try {
                if (failure != null) throw new IllegalStateException(failure);
                var binding = EchoBindingSavedData1201.get(server).get(current.id());
                if (binding == null || !binding.contents().get(6).isEmpty()
                        || !binding.contents().get(0).isEmpty() || !binding.contents().get(7).isEmpty()
                        || !ItemStack.matches(current.player().getInventory().getItem(10), current.relic())
                        || !current.player().getInventory().getItem(11).is(ModContent1201.accessory(AccessoryType.PLATE_ARMOR))
                        || current.player().getInventory().getItem(11).getCount() != 1
                        || current.player().getInventory().getItem(9).getCount() != 12) {
                    throw new IllegalStateException("Menu client test failed server inventory conservation");
                }
            } finally { current.restore(server); }
        }));
        phase = 10;
    }

    private static Fixture prepare(MinecraftServer server, UUID id, GameType mode) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null || player.containerMenu != player.inventoryMenu || !player.inventoryMenu.getCarried().isEmpty()) {
            throw new IllegalStateException("Menu fixture needs closed empty-cursor inventory");
        }
        List<ItemStack> inventory = IntStream.range(0, player.getInventory().getContainerSize())
                .mapToObj(slot -> player.getInventory().getItem(slot).copy()).toList();
        ItemStack summoner = new ItemStack(ModContent1201.ECHO_SUMMONER);
        ItemStack relic = new ItemStack(ModContent1201.relic(EchoHeroType1201.ROMAN_LEGIONARY));
        EchoRelicState1201.ensureInitialized(relic, player.getRandom(), server.overworld().getGameTime());
        EchoRelicProgress1201.addExperience(relic, 100);
        EchoRelicState1201.setActivityMode(relic, EchoRelicState1201.ActivityMode.WAIT);
        EchoRelicState1201.toggleSkill(relic, 0);
        var binding = EchoBindingSystem1201.synchronize(player.serverLevel(), summoner);
        Fixture fixture = new Fixture(player, inventory, player.getInventory().selected,
                player.gameMode.getGameModeForPlayer(), binding.summonerId(), relic.copy());
        try {
            binding.setFuel(1000); // Freeze conversion for deterministic inventory conservation.
            binding.mirrorTo(summoner);
            player.getInventory().setItem(0, summoner);
            player.getInventory().setItem(9, new ItemStack(Items.ROTTEN_FLESH, 12));
            player.getInventory().setItem(10, relic);
            player.getInventory().setItem(11, new ItemStack(ModContent1201.accessory(AccessoryType.PLATE_ARMOR)));
            player.getInventory().selected = 0;
            player.setGameMode(mode);
            player.inventoryMenu.broadcastChanges();
            return fixture;
        } catch (RuntimeException error) { fixture.restore(server); throw error; }
    }

    private record Fixture(ServerPlayer player, List<ItemStack> inventory, int selected, GameType mode, UUID id, ItemStack relic) {
        private void restore(MinecraftServer server) {
            // Clear only the fixture's cursor before closing, so a failed test cannot spill test items.
            player.containerMenu.setCarried(ItemStack.EMPTY);
            if (player.containerMenu != player.inventoryMenu) player.closeContainer();
            for (int index = 0; index < inventory.size(); index++) player.getInventory().setItem(index, inventory.get(index).copy());
            player.getInventory().selected = selected;
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(selected));
            player.setGameMode(mode);
            EchoBindingSavedData1201.get(server).remove(id);
            player.inventoryMenu.broadcastChanges();
        }
    }
}
