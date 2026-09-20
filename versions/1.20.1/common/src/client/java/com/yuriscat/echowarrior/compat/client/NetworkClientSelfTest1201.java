package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.network.EchoNetwork1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertRequest1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertResult1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertResult1201.Outcome;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Opt-in local client test of the actual C2S/S2C transport. Never a test of creative GUI clicks. */
public final class NetworkClientSelfTest1201 {
    private static CompletableFuture<Fixture> setup;
    private static CompletableFuture<Void> cleanup;
    private static Fixture fixture;
    private static SummonerInsertResult1201 response;
    private static int phase;
    private static int round;
    private static long sentAt;

    private NetworkClientSelfTest1201() {}

    public static boolean tick(Minecraft client) {
        if (round == 2) return true;
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("Network smoke test needs an integrated server");
        if (phase == 0) {
            UUID playerId = client.player.getUUID();
            GameType mode = round == 0 ? GameType.SURVIVAL : GameType.CREATIVE;
            int requestId = 120101 + round;
            setup = server.submit(() -> prepare(server, playerId, mode, requestId));
            phase = 1;
        } else if (phase == 1 && setup.isDone()) {
            fixture = setup.join();
            EchoNetwork1201.setClientReceiver(result -> {
                if (result.requestId() == fixture.request().requestId()) response = result;
            });
            send(2);
        } else if (phase == 2 || phase == 3) {
            if (response == null) {
                if (System.nanoTime() - sentAt > 20_000_000_000L) finish(server, "Timed out waiting for server response");
                return false;
            }
            if (phase == 2) {
                if (response.outcome() != Outcome.INSERTED || response.inserted() != 5
                        || response.revision() != fixture.request().expectedRevision() + 1) {
                    finish(server, "Wrong insertion response: " + response);
                } else send(3); // Exact same packet/revision: must not consume again.
            } else {
                finish(server, response.outcome() == Outcome.STALE_REVISION && response.inserted() == 0
                        ? null : "Replay was not rejected: " + response);
            }
        } else if (phase == 4 && cleanup.isDone()) {
            cleanup.join();
            EchoWarrior1201.LOGGER.info("[Compat1201] NETWORK CLIENT SELFTEST PASSED mode={} insert=5 replay=rejected inventory=restored",
                    round == 0 ? "survival" : "creative");
            round++;
            phase = 0;
            EchoNetwork1201.setClientReceiver(result -> {});
        }
        return round == 2;
    }

    private static void send(int nextPhase) {
        response = null;
        phase = nextPhase;
        sentAt = System.nanoTime();
        EchoNetwork1201.send(fixture.request());
    }

    private static void finish(MinecraftServer server, String failure) {
        Fixture current = fixture;
        cleanup = server.submit(() -> {
            try {
                if (failure != null) throw new IllegalStateException(failure);
                var binding = EchoBindingSavedData1201.get(server).get(current.request().summonerId());
                if (binding == null || binding.contents().get(6).getCount() != 5
                        || current.player().getInventory().getItem(35).getCount() != 7
                        || binding.stateRevision() != current.request().expectedRevision() + 1) {
                    throw new IllegalStateException("Network transfer did not conserve server inventory");
                }
            } finally { current.restore(server); }
        });
        phase = 4;
    }

    private static Fixture prepare(MinecraftServer server, UUID playerId, GameType mode, int requestId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null || player.containerMenu != player.inventoryMenu) {
            throw new IllegalStateException("Test player is unavailable or has an open container");
        }
        ItemStack previousTarget = player.getInventory().getItem(34).copy();
        ItemStack previousSource = player.getInventory().getItem(35).copy();
        GameType previousMode = player.gameMode.getGameModeForPlayer();
        ItemStack summoner = new ItemStack(ModContent1201.ECHO_SUMMONER);
        var binding = EchoBindingSystem1201.synchronize(player.serverLevel(), summoner);
        var request = new SummonerInsertRequest1201(requestId, player.inventoryMenu.containerId,
                34, 35, binding.summonerId(), binding.stateRevision(), 5);
        Fixture fixture = new Fixture(player, previousTarget, previousSource, previousMode, request);
        try {
            player.getInventory().setItem(34, summoner);
            player.getInventory().setItem(35, new ItemStack(Items.ROTTEN_FLESH, 12));
            player.setGameMode(mode);
            player.inventoryMenu.broadcastChanges();
            return fixture;
        } catch (RuntimeException error) {
            fixture.restore(server);
            throw error;
        }
    }

    private record Fixture(ServerPlayer player, ItemStack previousTarget, ItemStack previousSource,
                           GameType previousMode, SummonerInsertRequest1201 request) {
        private void restore(MinecraftServer server) {
            player.getInventory().setItem(34, previousTarget.copy());
            player.getInventory().setItem(35, previousSource.copy());
            EchoBindingSavedData1201.get(server).remove(request.summonerId());
            player.setGameMode(previousMode);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
    }
}
