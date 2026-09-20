package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.menu.SummonerMenu1201;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.UUID;

/** Calls vanilla click dispatch, not a hand-written simulation of slot transfer. */
public final class SummonerMenuSelfTest1201 {
    private static int checks;
    private SummonerMenuSelfTest1201() {}

    public static void run(MinecraftServer server) {
        checks = 0;
        ServerPlayer player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "MenuSelfTest"));
        ItemStack summoner = new ItemStack(ModContent1201.ECHO_SUMMONER);
        player.getInventory().setItem(0, summoner);
        var binding = EchoBindingSystem1201.synchronize(server.overworld(), summoner);
        try {
            SummonerMenu1201 menu = new SummonerMenu1201(5, player.getInventory(), 0);
            player.containerMenu = menu;
            int previousFeedback = menu.actionFeedbackValue();
            for (int action = 0; action < 260; action++) {
                menu.reportFuelConsumed(new ItemStack(Items.ROTTEN_FLESH));
                int value = menu.actionFeedbackValue();
                var bytes = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try {
                    var packet = new net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket(5, 0, value);
                    packet.write(bytes);
                    int received = new net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket(bytes).getValue();
                    check(value == received && value > 0 && value != previousFeedback
                            && (received & 0xFF) == SummonerMenu1201.ACTION_FUEL_ROTTEN_FLESH,
                            "signed-short feedback survives two sequence wraps");
                } finally { bytes.release(); }
                previousFeedback = value;
            }
            check(menu.stillValid(player) && menu.slots.size() == 44, "original eight + 36 slots");
            check(menu.getSlot(6).x == 179 && menu.getSlot(6).y == 172
                    && menu.getSlot(7).x == 217 && menu.getSlot(43).y == 177, "authored coordinates");
            menu.setCarried(new ItemStack(Items.ROTTEN_FLESH, 12));
            long revision = binding.stateRevision();
            menu.clicked(6, 0, ClickType.PICKUP, player);
            check(menu.getCarried().isEmpty() && binding.contents().get(6).getCount() == 12
                    && binding.stateRevision() == revision + 1, "cursor insertion commits once");
            menu.clicked(6, 1, ClickType.PICKUP, player);
            check(menu.getCarried().getCount() == 6 && binding.contents().get(6).getCount() == 6, "right click half withdrawal");
            menu.clicked(8, 0, ClickType.PICKUP, player);
            check(player.getInventory().getItem(9).getCount() == 6 && menu.getCarried().isEmpty(), "withdrawn items reach inventory");
            menu.removed(player);
            menu = new SummonerMenu1201(6, player.getInventory(), 0);
            player.containerMenu = menu;
            check(menu.getSlot(6).getItem().getCount() == 6, "reopen does not resurrect withdrawn items");
            menu.clicked(8, 0, ClickType.QUICK_MOVE, player);
            check(player.getInventory().getItem(9).isEmpty() && binding.contents().get(6).getCount() == 12, "shift insert");
            menu.clicked(6, 0, ClickType.QUICK_MOVE, player);
            check(binding.contents().get(6).isEmpty() && player.getInventory().getItem(8).getCount() == 12, "shift withdraw");
            for (ClickType type : new ClickType[]{ClickType.PICKUP, ClickType.QUICK_MOVE, ClickType.THROW, ClickType.SWAP}) {
                menu.clicked(35, 0, type, player);
                check(player.getInventory().getItem(0) == summoner && menu.getCarried().isEmpty(), "summoner lock " + type);
            }
            menu.clicked(6, 0, ClickType.SWAP, player);
            check(player.getInventory().getItem(0) == summoner, "number key cannot swap locked summoner into another slot");
            menu.setCarried(new ItemStack(Items.SOUL_SAND, 3));
            menu.clicked(35, 0, ClickType.PICKUP, player);
            check(menu.getCarried().isEmpty() && binding.contents().get(6).getCount() == 3, "fuel onto locked summoner");
            menu.setCarried(new ItemStack(Items.DIAMOND, 2));
            menu.clicked(0, 0, ClickType.PICKUP, player);
            menu.clicked(7, 0, ClickType.PICKUP, player);
            menu.clicked(6, 0, ClickType.PICKUP, player);
            check(menu.getCarried().getCount() == 2 && binding.contents().get(7).isEmpty()
                    && binding.contents().get(0).isEmpty() && binding.contents().get(6).getCount() == 3, "reject unsupported slot contents");
            menu.setCarried(ItemStack.EMPTY);
            int oldState = menu.getStateId();
            menu.incrementStateId();
            check(!menu.acceptRemoteState(oldState), "reject old vanilla state ID");
            binding.setFuel(900);
            check(!menu.acceptRemoteState(menu.getStateId()), "reject newer authority before broadcast");
            menu.clicked(6, 0, ClickType.PICKUP, player);
            check(menu.getCarried().isEmpty() && menu.fuelAmount() == 900 && binding.contents().get(6).getCount() == 3,
                    "stale click reloads without transferring");
            check(menu.acceptRemoteState(menu.getStateId()), "current state accepted after resync");
            revision = binding.stateRevision();
            check(binding.convertOneFuel(120100) && binding.fuel() == 950 && binding.contents().get(6).getCount() == 2
                    && binding.stateRevision() == revision + 1, "fuel item and energy atomic revision");
            check(!binding.convertOneFuel(120100) && binding.fuel() == 950, "duplicate callback same tick cannot burn twice");
            check(binding.convertOneFuel(120105) && binding.fuel() == 1000, "fuel reaches capacity");
            check(!binding.convertOneFuel(120110) && binding.contents().get(6).getCount() == 1, "full fuel preserves item");
            binding.setFuel(980);
            check(!binding.convertOneFuel(120115) && binding.contents().get(6).getCount() == 1, "partial headroom preserves expensive fuel");
            menu.broadcastChanges();
            check(menu.getSlot(6).getItem().getCount() == 1 && menu.fuelAmount() == 980, "menu follows outside conversion");
            menu.clicked(6, 0, ClickType.PICKUP, player);
            check(menu.getCarried().getCount() == 1 && binding.contents().get(6).isEmpty(), "final withdrawal persisted");
            // Avoid connection-dependent vanilla cursor return in this disconnected fixture.
            menu.setCarried(ItemStack.EMPTY);
            menu.removed(player);
            check(new SummonerMenu1201(7, player.getInventory(), 0).getSlot(6).getItem().isEmpty(), "close never restores stale contents");
            player.getInventory().setItem(0, ItemStack.EMPTY);
            check(!menu.stillValid(player), "source removed invalidates menu");
            player.getInventory().setItem(40, summoner);
            menu = new SummonerMenu1201(8, player.getInventory(), 40);
            menu.clicked(8, 40, ClickType.SWAP, player);
            check(menu.stillValid(player) && player.getInventory().getItem(40) == summoner, "offhand source locked against swap");
        } finally {
            player.containerMenu = player.inventoryMenu;
            EchoBindingSavedData1201.get(server).remove(binding.summonerId());
        }
        EchoWarrior1201.LOGGER.info("[Compat1201] MENU SELFTEST PASSED checks={}", checks);
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("Menu test failed: " + label);
        checks++;
    }
}
