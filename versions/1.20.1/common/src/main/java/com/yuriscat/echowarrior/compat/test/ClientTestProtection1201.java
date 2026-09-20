package com.yuriscat.echowarrior.compat.test;

import net.minecraft.server.MinecraftServer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Opt-in test fixture protection, restored before a normal stop/save even if an assertion fails. */
public final class ClientTestProtection1201 {
    private static final Map<UUID, Boolean> ORIGINAL = new HashMap<>();
    private ClientTestProtection1201() {}
    public static void begin(MinecraftServer server, UUID playerId) {
        if (!Boolean.getBoolean("echo_warrior.auto_pause_after_quick_play")) throw new IllegalStateException("Not an automated test");
        var player = server.getPlayerList().getPlayer(playerId);
        if (player == null) throw new IllegalStateException("No fixture player");
        ORIGINAL.putIfAbsent(playerId, player.isInvulnerable());
        player.setInvulnerable(true);
    }
    public static void restore(MinecraftServer server) {
        ORIGINAL.forEach((id, original) -> {
            var player = server.getPlayerList().getPlayer(id);
            if (player != null) player.setInvulnerable(original);
        });
        ORIGINAL.clear();
    }
    public static void restoreBeforeLogout(net.minecraft.server.level.ServerPlayer player) {
        Boolean original = ORIGINAL.remove(player.getUUID());
        if (original != null) player.setInvulnerable(original);
    }
}
