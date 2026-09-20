package com.yuriscat.echowarrior.compat.binding;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.item.SummonerStackContents1201;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Confirms that a client-side creative trash request matches summoners that the
 * server actually observed leaving that player's inventory.
 */
public final class CreativeSummonerDestroyTracker1201 {
    private static final int REQUEST_WINDOW_TICKS = 40;
    private static final int REMOVAL_WINDOW_TICKS = 20 * 60 * 30;
    private static final Map<UUID, Map<UUID, Integer>> REMOVED_FROM_CREATIVE_INVENTORY = new HashMap<>();
    private static final Map<UUID, Map<UUID, Integer>> REQUESTED_CREATIVE_TRASH = new HashMap<>();
    private static final Map<UUID, Integer> REQUEST_NOT_BEFORE_TICK = new HashMap<>();
    private static final Map<UUID, Set<UUID>> WAITING_FOR_REMOVAL_LOGGED = new HashMap<>();

    private CreativeSummonerDestroyTracker1201() {
    }

    public static void noteCreativeSlotUpdate(ServerPlayer player, ItemStack previous, ItemStack replacement) {
        if (!player.gameMode.isCreative()) return;
        var before = SummonerStackContents1201.scan(previous);
        var after = SummonerStackContents1201.scan(replacement);
        if (!before.complete() || !after.complete()) return;
        Set<UUID> previousIds = new HashSet<>(before.ids());
        Set<UUID> replacementIds = after.ids();
        if (!previousIds.isEmpty() || !replacementIds.isEmpty()) {
            EchoWarrior1201.LOGGER.info(
                    "[CreativeSummonerDelete] server observed creative slot replacement: player={} previousIds={} replacementIds={} replacement={}",
                    player.getGameProfile().getName(), previousIds, replacementIds, replacement);
        }
        UUID playerId = player.getUUID();
        Map<UUID, Integer> removed = REMOVED_FROM_CREATIVE_INVENTORY.get(playerId);
        if (removed != null) {
            for (UUID replacementId : replacementIds) removed.remove(replacementId);
            if (removed.isEmpty()) REMOVED_FROM_CREATIVE_INVENTORY.remove(playerId);
        }
        previousIds.removeAll(replacementIds);
        if (previousIds.isEmpty()) return;

        int expiresAt = player.serverLevel().getServer().getTickCount() + REMOVAL_WINDOW_TICKS;
        removed = REMOVED_FROM_CREATIVE_INVENTORY.computeIfAbsent(playerId, ignored -> new HashMap<>());
        for (UUID previousId : previousIds) {
            if (removed.size() >= 4096) break;
            removed.put(previousId, expiresAt);
        }
        Set<UUID> waiting = WAITING_FOR_REMOVAL_LOGGED.get(playerId);
        if (waiting != null) waiting.removeAll(previousIds);
    }

    public static void requestCreativeTrash(ServerPlayer player, List<UUID> requestedIds) {
        if (!player.gameMode.isCreative() || requestedIds.isEmpty()) return;
        MinecraftServer server = player.serverLevel().getServer();
        int now = server.getTickCount();
        int expiresAt = now + REQUEST_WINDOW_TICKS;
        UUID playerId = player.getUUID();
        Map<UUID, Integer> requested = REQUESTED_CREATIVE_TRASH.computeIfAbsent(
                playerId, ignored -> new HashMap<>());
        for (UUID summonerId : requestedIds) {
            if (requested.size() >= 4096) break;
            requested.put(summonerId, expiresAt);
        }
        Set<UUID> waiting = WAITING_FOR_REMOVAL_LOGGED.get(playerId);
        if (waiting != null) waiting.removeAll(requestedIds);
        REQUEST_NOT_BEFORE_TICK.merge(playerId, now + 1, Math::max);
        EchoWarrior1201.LOGGER.info(
                "[CreativeSummonerDelete] server queued confirmation: player={} now={} notBefore={} ids={} alreadyRemoved={}",
                player.getGameProfile().getName(), now, now + 1, requestedIds,
                REMOVED_FROM_CREATIVE_INVENTORY.getOrDefault(playerId, Map.of()).keySet());
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        pruneExpired(REMOVED_FROM_CREATIVE_INVENTORY, now);
        pruneExpired(REQUESTED_CREATIVE_TRASH, now);
        REQUEST_NOT_BEFORE_TICK.keySet().removeIf(playerId -> !REQUESTED_CREATIVE_TRASH.containsKey(playerId));
        WAITING_FOR_REMOVAL_LOGGED.keySet().removeIf(playerId -> !REQUESTED_CREATIVE_TRASH.containsKey(playerId));
        if (REQUESTED_CREATIVE_TRASH.isEmpty()) return;

        var visibility = visibleInventorySummoners(server);
        // Exhausting a scan is not evidence of absence. Let the request expire rather than delete blindly.
        if (!visibility.complete()) return;
        Set<UUID> visibleIds = visibility.ids();
        Iterator<Map.Entry<UUID, Map<UUID, Integer>>> playerIterator =
                REQUESTED_CREATIVE_TRASH.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, Map<UUID, Integer>> playerEntry = playerIterator.next();
            UUID playerId = playerEntry.getKey();
            if (now < REQUEST_NOT_BEFORE_TICK.getOrDefault(playerId, now)) continue;

            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            Map<UUID, Integer> removed = REMOVED_FROM_CREATIVE_INVENTORY.get(playerId);
            if (player == null || !player.gameMode.isCreative()) {
                playerIterator.remove();
                REMOVED_FROM_CREATIVE_INVENTORY.remove(playerId);
                REQUEST_NOT_BEFORE_TICK.remove(playerId);
                continue;
            }

            Iterator<UUID> requestIterator = playerEntry.getValue().keySet().iterator();
            while (requestIterator.hasNext()) {
                UUID summonerId = requestIterator.next();
                if (visibleIds.contains(summonerId)) {
                    EchoWarrior1201.LOGGER.info(
                            "[CreativeSummonerDelete] rejected destruction because summoner remains visible: player={} id={}",
                            player.getGameProfile().getName(), summonerId);
                    requestIterator.remove();
                    if (removed != null) removed.remove(summonerId);
                    continue;
                }
                if (removed == null || !removed.containsKey(summonerId)) {
                    Set<UUID> waiting = WAITING_FOR_REMOVAL_LOGGED.computeIfAbsent(
                            playerId, ignored -> new HashSet<>());
                    if (waiting.add(summonerId)) {
                        EchoWarrior1201.LOGGER.info(
                                "[CreativeSummonerDelete] waiting for authoritative creative slot removal: player={} id={}",
                                player.getGameProfile().getName(), summonerId);
                    }
                    continue;
                }
                boolean destroyed = EchoBindingSystem1201.destroySummoner(player.serverLevel(), summonerId);
                EchoWarrior1201.LOGGER.info(
                        "[CreativeSummonerDelete] confirmed destruction: player={} id={} bindingRemoved={}",
                        player.getGameProfile().getName(), summonerId, destroyed);
                requestIterator.remove();
                removed.remove(summonerId);
                Set<UUID> waiting = WAITING_FOR_REMOVAL_LOGGED.get(playerId);
                if (waiting != null) waiting.remove(summonerId);
            }

            if (removed != null && removed.isEmpty()) REMOVED_FROM_CREATIVE_INVENTORY.remove(playerId);
            if (playerEntry.getValue().isEmpty()) {
                playerIterator.remove();
                REQUEST_NOT_BEFORE_TICK.remove(playerId);
            }
        }
    }

    private static SummonerStackContents1201.ScanResult visibleInventorySummoners(MinecraftServer server) {
        var visible = new SummonerStackContents1201.VisibilityScan();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (ItemStack stack : player.inventoryMenu.getItems()) {
                visible.accept(stack);
            }
            visible.accept(player.inventoryMenu.getCarried());
            if (player.containerMenu != player.inventoryMenu) {
                for (ItemStack stack : player.containerMenu.getItems()) {
                    visible.accept(stack);
                }
                visible.accept(player.containerMenu.getCarried());
            }
            for (int slot = 0; slot < player.getEnderChestInventory().getContainerSize(); slot++) {
                visible.accept(player.getEnderChestInventory().getItem(slot));
            }
        }
        for (var level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity) {
                    visible.accept(itemEntity.getItem());
                } else if (entity instanceof Container container) {
                    for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        visible.accept(container.getItem(slot));
                    }
                }
            }
        }
        return visible.result();
    }

    private static void pruneExpired(Map<UUID, Map<UUID, Integer>> tracked, int now) {
        Iterator<Map.Entry<UUID, Map<UUID, Integer>>> playerIterator = tracked.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map<UUID, Integer> ids = playerIterator.next().getValue();
            ids.entrySet().removeIf(entry -> entry.getValue() < now);
            if (ids.isEmpty()) playerIterator.remove();
        }
    }

    public static void clearPlayer(UUID playerId) {
        REMOVED_FROM_CREATIVE_INVENTORY.remove(playerId);
        REQUESTED_CREATIVE_TRASH.remove(playerId);
        REQUEST_NOT_BEFORE_TICK.remove(playerId);
        WAITING_FOR_REMOVAL_LOGGED.remove(playerId);
    }

    public static void clearAll() {
        REMOVED_FROM_CREATIVE_INVENTORY.clear();
        REQUESTED_CREATIVE_TRASH.clear();
        REQUEST_NOT_BEFORE_TICK.clear();
        WAITING_FOR_REMOVAL_LOGGED.clear();
    }
}
