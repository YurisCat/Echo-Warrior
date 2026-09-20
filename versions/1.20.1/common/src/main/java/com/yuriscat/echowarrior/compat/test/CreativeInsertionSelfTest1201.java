package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.item.*;
import com.yuriscat.echowarrior.compat.network.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import java.util.UUID;

public final class CreativeInsertionSelfTest1201 {
    private static int checks;
    private CreativeInsertionSelfTest1201() {}
    public static void run(MinecraftServer server) {
        checks = 0;
        ServerPlayer player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "CreativeTest"));
        // A disconnected, test-owned connection records vanilla responses without adding a player to the world.
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player);
        ItemStack summoner = new ItemStack(ModContent1201.ECHO_SUMMONER);
        player.getInventory().setItem(0, summoner);
        var binding = EchoBindingSystem1201.synchronize(server.overworld(), summoner);
        try {
            var fuel = new ItemStack(Items.SOUL_SAND, 8);
            var request = request(binding, 36, fuel);
            reject(player, request, binding, "survival cannot forge creative cursor");
            player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            reject(player, request(binding, -1, fuel), binding, "negative slot");
            reject(player, request(binding, 46, fuel), binding, "outside inventory");
            reject(player, request(binding, 0, fuel), binding, "crafting slot");
            reject(player, request(binding, 37, fuel), binding, "empty target");
            reject(player, request(binding, 36, new ItemStack(Items.SOUL_SAND, 65)), binding, "oversized cursor");
            reject(player, request(binding, 36, ItemStack.EMPTY), binding, "empty cursor");
            reject(player, new CreativeInsertRequest1201(3, 36, UUID.randomUUID(), binding.stateRevision(), fuel), binding, "wrong UUID");
            reject(player, new CreativeInsertRequest1201(3, 36, binding.summonerId(), -1, fuel), binding, "negative revision");
            player.containerMenu = ChestMenu.threeRows(4, player.getInventory(), new SimpleContainer(27));
            reject(player, request, binding, "cannot insert while another container is open");
            player.containerMenu = player.inventoryMenu;
            long revision = binding.stateRevision();
            var reply = CreativeInsertRequest1201.handle(player, request);
            check(reply.inserted() == 8 && binding.stateRevision() == revision + 1 && binding.contents().get(6).getCount() == 8,
                    "one authoritative commit");
            check(request.carried().getCount() == 8 && fuel.getCount() == 8, "request snapshots are not consumed");
            reject(player, request, binding, "replayed request is not executed twice");
            reject(player, request(binding, 36, new ItemStack(Items.ROTTEN_FLESH)), binding, "different fuel");
            reject(player, request(binding, 36, new ItemStack(Items.DIAMOND)), binding, "unsupported item");
            ItemStack relic = new ItemStack(ModContent1201.relic(EchoHeroType1201.EGYPTIAN_ARCHER));
            EchoRelicState1201.ensureInitialized(relic, player.getRandom(), server.overworld().getGameTime());
            EchoRelicProgress1201.addExperience(relic, 100);
            check(CreativeInsertRequest1201.handle(player, request(binding, 36, relic)).inserted() == 1
                    && ItemStack.matches(relic, binding.contents().get(7)), "grown relic is preserved");
            reject(player, request(binding, 36, relic), binding, "occupied relic slot");
            ItemStack accessory = new ItemStack(ModContent1201.accessory(EchoAccessoryItem1201.AccessoryType.PLATE_ARMOR));
            check(CreativeInsertRequest1201.handle(player, request(binding, 36, accessory)).inserted() == 1, "accessory insertion");
            accessory.setHoverName(net.minecraft.network.chat.Component.literal("renamed"));
            reject(player, request(binding, 36, accessory), binding, "renamed duplicate accessory");
            var contents = new java.util.ArrayList<>(binding.contents());
            contents.set(6, new ItemStack(Items.SOUL_SAND, 63));
            binding.commitEquipment(binding.stateRevision(), contents);
            check(CreativeInsertRequest1201.handle(player, request(binding, 36, fuel)).inserted() == 1
                    && binding.contents().get(6).getCount() == 64, "partial virtual cursor insertion returns exact count");
            reject(player, request(binding, 36, fuel), binding, "full buffer");
            check(ItemStack.matches(SummonerData1201.contents(summoner).get(7), relic), "item mirror preserves equipment");
            codec(request(binding, 36, relic));
        } finally { EchoBindingSavedData1201.get(server).remove(binding.summonerId()); }
        EchoWarrior1201.LOGGER.info("[Compat1201] CREATIVE INSERT SELFTEST PASSED checks={}", checks);
    }

    private static CreativeInsertRequest1201 request(EchoBindingSavedData1201.Binding binding, int slot, ItemStack cursor) {
        return new CreativeInsertRequest1201(120105, slot, binding.summonerId(), binding.stateRevision(), cursor);
    }
    private static void reject(ServerPlayer player, CreativeInsertRequest1201 request, EchoBindingSavedData1201.Binding binding, String label) {
        long before = binding.stateRevision();
        check(CreativeInsertRequest1201.handle(player, request).inserted() == 0 && binding.stateRevision() == before, label);
    }
    private static void codec(CreativeInsertRequest1201 request) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            request.encode(buffer);
            var decoded = CreativeInsertRequest1201.decode(buffer);
            check(decoded.requestId() == request.requestId() && decoded.slot() == request.slot()
                    && decoded.revision() == request.revision() && decoded.summonerId().equals(request.summonerId())
                    && ItemStack.matches(decoded.carried(), request.carried()), "request wire preserves relic NBT");
            buffer.clear(); request.encode(buffer); buffer.writeByte(1);
            boolean rejected = false;
            try { CreativeInsertRequest1201.decode(buffer); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "trailing request bytes");
            buffer.clear(); buffer.writeZero(65537); rejected = false;
            try { CreativeInsertRequest1201.decode(buffer); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "oversized request rejected before item decode");
            buffer.clear(); new CreativeInsertReply1201(120105, 1).encode(buffer);
            check(CreativeInsertReply1201.decode(buffer).equals(new CreativeInsertReply1201(120105, 1)), "ACK wire round trip");
            buffer.clear(); buffer.writeInt(1).writeInt(65); rejected = false;
            try { CreativeInsertReply1201.decode(buffer); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "invalid ACK count");
            buffer.clear(); var message = new InsertionFeedback1201(0, 36, request.carried()); message.encode(buffer);
            var feedback = InsertionFeedback1201.decode(buffer);
            check(feedback.menuId() == 0 && feedback.slot() == 36 && ItemStack.matches(feedback.item(), message.item()), "feedback wire round trip");
            buffer.clear(); var deletion = new CreativeDestructionRequest1201(java.util.List.of(UUID.randomUUID(), UUID.randomUUID()));
            deletion.encode(buffer);
            check(CreativeDestructionRequest1201.decode(buffer).equals(deletion), "destruction candidate wire round trip");
            buffer.clear(); buffer.writeInt(1025); rejected = false;
            try { CreativeDestructionRequest1201.decode(buffer); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "oversized candidate count");
            buffer.clear(); buffer.writeInt(0).writeByte(1); rejected = false;
            try { CreativeDestructionRequest1201.decode(buffer); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "trailing destruction candidate bytes");
        } finally { buffer.release(); }
    }
    private static void check(boolean pass, String label) {
        if (!pass) throw new IllegalStateException("[Compat1201] Creative insertion failed: " + label);
        checks++;
    }
}
