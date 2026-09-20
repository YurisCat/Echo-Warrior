package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.item.SummonerData1201;
import com.yuriscat.echowarrior.compat.menu.SummonerInsertion1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertRequest1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertResult1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertResult1201.Outcome;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.UUID;

/** Deterministic server checks; actual loader transport is tested separately in the integrated client. */
public final class NetworkTransactionSelfTest1201 {
    private static int checks;
    private NetworkTransactionSelfTest1201() {}

    public static void run(MinecraftServer server) {
        checks = 0;
        pausePolicy();
        codec();
        ServerPlayer player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "NetworkSelfTest"));
        ItemStack summoner = new ItemStack(ModContent1201.ECHO_SUMMONER);
        player.getInventory().setItem(0, summoner);
        player.getInventory().setItem(1, new ItemStack(Items.ROTTEN_FLESH, 12));
        var binding = EchoBindingSystem1201.synchronize(server.overworld(), summoner);
        UUID id = binding.summonerId();
        try {
            long revision = binding.stateRevision();
            var request = request(id, revision, 0, 1, 5);
            var result = SummonerInsertion1201.handle(player, request);
            check(result.outcome() == Outcome.INSERTED && result.inserted() == 5
                    && player.getInventory().getItem(1).getCount() == 7
                    && binding.contents().get(6).getCount() == 5
                    && SummonerData1201.contents(summoner).get(6).getCount() == 5,
                    "server transfer conserves source + target, mirrors authority");
            check(result.revision() == revision + 1, "one revision for insertion");
            rejected(player, request, Outcome.STALE_REVISION, "replay cannot consume twice");
            revision = binding.stateRevision();
            rejected(player, request(UUID.randomUUID(), revision, 0, 1, 1), Outcome.WRONG_SUMMONER, "wrong UUID");
            rejected(player, request(id, revision, -1, 1, 1), Outcome.INVALID_REQUEST, "negative target slot");
            rejected(player, request(id, revision, 0, 41, 1), Outcome.INVALID_REQUEST, "outside source slot");
            rejected(player, request(id, revision, 0, 0, 1), Outcome.INVALID_REQUEST, "self insertion");
            rejected(player, request(id, revision, 0, 1, 0), Outcome.INVALID_REQUEST, "zero count");
            rejected(player, request(id, revision, 0, 1, 65), Outcome.INVALID_REQUEST, "oversized count");
            rejected(player, request(id, revision, 0, 1, 8), Outcome.INVALID_SOURCE, "insufficient physical source");
            rejected(player, new SummonerInsertRequest1201(1, 9, 0, 1, id, revision, 1), Outcome.WRONG_MENU, "wrong menu ID");
            player.containerMenu = ChestMenu.threeRows(3, player.getInventory(), new SimpleContainer(27));
            rejected(player, new SummonerInsertRequest1201(1, 3, 0, 1, id, revision, 1), Outcome.WRONG_MENU,
                    "container other than player inventory is not an authorized source");
            player.containerMenu = player.inventoryMenu;
            player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 4));
            rejected(player, request(id, revision, 0, 1, 1), Outcome.UNSUPPORTED_ITEM, "not-yet-ported categories fail closed");
            player.getInventory().setItem(1, new ItemStack(Items.SOUL_SAND, 4));
            rejected(player, request(id, revision, 0, 1, 1), Outcome.NO_SPACE, "different fuel cannot replace existing fuel");
            player.getInventory().setItem(1, new ItemStack(Items.ROTTEN_FLESH, 4));
            player.getInventory().getItem(1).getOrCreateTag().putString("Marker", "different");
            rejected(player, request(id, revision, 0, 1, 1), Outcome.NO_SPACE, "different metadata cannot be merged");
            var contents = new ArrayList<>(binding.contents());
            contents.set(6, new ItemStack(Items.ROTTEN_FLESH, 63));
            binding.commitContents(revision, contents);
            player.getInventory().setItem(1, new ItemStack(Items.ROTTEN_FLESH, 4));
            result = SummonerInsertion1201.handle(player, request(id, binding.stateRevision(), 0, 1, 4));
            check(result.outcome() == Outcome.INSERTED && result.inserted() == 1
                    && player.getInventory().getItem(1).getCount() == 3 && binding.contents().get(6).getCount() == 64,
                    "partial insertion conserves remainder");
            rejected(player, request(id, binding.stateRevision(), 0, 1, 1), Outcome.NO_SPACE, "full slot does not consume");
            // Selecting another player's binding UUID is insufficient; this player's physical slot must match it.
            ServerPlayer other = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "OtherSelfTest"));
            other.getInventory().setItem(1, new ItemStack(Items.ROTTEN_FLESH, 4));
            rejected(other, request(id, binding.stateRevision(), 0, 1, 1), Outcome.WRONG_SUMMONER, "cannot modify another inventory by UUID");
        } finally {
            EchoBindingSavedData1201.get(server).remove(id);
        }
        EchoWarrior1201.LOGGER.info("[Compat1201] NETWORK TRANSACTION SELFTEST PASSED checks={}", checks);
    }

    private static void rejected(ServerPlayer player, SummonerInsertRequest1201 request, Outcome expected, String label) {
        ItemStack source = player.getInventory().getItem(1).copy();
        var result = SummonerInsertion1201.handle(player, request);
        check(result.outcome() == expected && result.inserted() == 0
                && ItemStack.matches(source, player.getInventory().getItem(1)), label);
    }

    private static SummonerInsertRequest1201 request(UUID id, long revision, int target, int source, int count) {
        return new SummonerInsertRequest1201(120101, 0, target, source, id, revision, count);
    }

    private static void codec() {
        var request = request(UUID.randomUUID(), 42, 0, 1, 3);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            request.encode(buffer);
            check(buffer.readableBytes() == SummonerInsertRequest1201.WIRE_BYTES
                    && SummonerInsertRequest1201.decode(buffer).equals(request), "request wire round trip");
            buffer.clear();
            var response = new SummonerInsertResult1201(3, Outcome.INSERTED, 3, 43);
            response.encode(buffer);
            check(buffer.readableBytes() == SummonerInsertResult1201.WIRE_BYTES
                    && SummonerInsertResult1201.decode(buffer).equals(response), "response wire round trip");
            buffer.clear();
            request.encode(buffer);
            buffer.writeByte(0);
            boolean rejected = false;
            try { SummonerInsertRequest1201.decode(buffer); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "trailing bytes cannot hide extra item NBT");
            buffer.clear();
            buffer.writeInt(1);
            rejected = false;
            try { SummonerInsertRequest1201.decode(buffer); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "truncated request rejected");
            buffer.clear();
            buffer.writeInt(1).writeInt(999).writeInt(0).writeLong(0);
            rejected = false;
            try { SummonerInsertResult1201.decode(buffer); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "unknown response code rejected");
        } finally { buffer.release(); }
    }

    private static void pausePolicy() {
        JoinPausePolicy1201 manual = new JoinPausePolicy1201(false, true);
        check(manual.needsPause() && manual.blockMouseCapture() && !manual.shouldAutoClose(), "manual launch blocks initial grab");
        manual.onPauseShown();
        check(!manual.needsPause() && !manual.blockMouseCapture() && !manual.shouldAutoClose(),
                "manual launch permits explicit resume and never auto exits");
        manual.onDisconnected();
        check(manual.needsPause() && manual.blockMouseCapture(), "rejoin rearms pause protection");
        JoinPausePolicy1201 automated = new JoinPausePolicy1201(true, false);
        automated.onPauseShown();
        check(automated.blockMouseCapture() && automated.shouldAutoClose(), "automation stays mouse-free and exits");
        JoinPausePolicy1201 normal = new JoinPausePolicy1201(false, false);
        check(!normal.needsPause() && !normal.blockMouseCapture() && !normal.shouldAutoClose(), "ordinary game unchanged");
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new IllegalStateException("[Compat1201] Network transaction check failed: " + name);
        checks++;
    }
}
