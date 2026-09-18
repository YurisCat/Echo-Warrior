package com.yuriscat.echowarrior.compat.binding;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.item.SummonerStackContents1211;
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
public final class CreativeSummonerDestroyTracker1211 {
    private static final int REQUEST_WINDOW_TICKS = 40;
    private static final int REMOVAL_WINDOW_TICKS = 20 * 60 * 30;
    private static final Map<UUID, Map<UUID, Integer>> REMOVED_FROM_CREATIVE_INVENTORY = new HashMap<>();
    private static final Map<UUID, Map<UUID, Integer>> REQUESTED_CREATIVE_TRASH = new HashMap<>();
    private static final Map<UUID, Integer> REQUEST_NOT_BEFORE_TICK = new HashMap<>();
    private static final Map<UUID, Set<UUID>> WAITING_FOR_REMOVAL_LOGGED = new HashMap<>();

    private CreativeSummonerDestroyTracker1211() {
    }

    public static void noteCreativeSlotUpdate(ServerPlayer player, ItemStack previous, ItemStack replacement) {
        if (!player.gameMode.isCreative()) return;
        Set<UUID> previousIds = SummonerStackContents1211.summonerIds(previous);
        Set<UUID> replacementIds = SummonerStackContents1211.summonerIds(replacement);
        if (!previousIds.isEmpty() || !replacementIds.isEmpty()) {
            EchoWarrior1211.LOGGER.info(
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
        for (UUID previousId : previousIds) removed.put(previousId, expiresAt);
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
        for (UUID summonerId : requestedIds) requested.put(summonerId, expiresAt);
        Set<UUID> waiting = WAITING_FOR_REMOVAL_LOGGED.get(playerId);
        if (waiting != null) waiting.removeAll(requestedIds);
        REQUEST_NOT_BEFORE_TICK.merge(playerId, now + 1, Math::max);
        EchoWarrior1211.LOGGER.info(
                "[CreativeSummonerDelete] server queued confirmation: player={} now={} notBefore={} ids={} alreadyRemoved={}",
                player.getGameProfile().getName(), now, now + 1, requestedIds,
                REMOVED_FROM_CREATIVE_INVENTORY.getOrDefault(playerId, Map.of()).keySet());
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        pruneExpired(REMOVED_FROM_CREATIVE_INVENTORY, now);
        pruneExpired(REQUESTED_CREATIVE_TRASH, now);
        REQUEST_NOT_BEFORE_TICK.keySet().removeIf(playerId -> !REQUESTED_CREATIVE_TRASH.containsKey(playerId));
        if (REQUESTED_CREATIVE_TRASH.isEmpty()) return;

        Set<UUID> visibleIds = visibleInventorySummoners(server);
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
                    EchoWarrior1211.LOGGER.info(
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
                        EchoWarrior1211.LOGGER.info(
                                "[CreativeSummonerDelete] waiting for authoritative creative slot removal: player={} id={}",
                                player.getGameProfile().getName(), summonerId);
                    }
                    continue;
                }
                boolean destroyed = EchoBindingSystem1211.destroySummoner(player.serverLevel(), summonerId);
                EchoWarrior1211.LOGGER.info(
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

    private static Set<UUID> visibleInventorySummoners(MinecraftServer server) {
        Set<UUID> visible = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (ItemStack stack : player.inventoryMenu.getItems()) {
                visible.addAll(SummonerStackContents1211.summonerIds(stack));
            }
            visible.addAll(SummonerStackContents1211.summonerIds(player.inventoryMenu.getCarried()));
            if (player.containerMenu != player.inventoryMenu) {
                for (ItemStack stack : player.containerMenu.getItems()) {
                    visible.addAll(SummonerStackContents1211.summonerIds(stack));
                }
                visible.addAll(SummonerStackContents1211.summonerIds(player.containerMenu.getCarried()));
            }
            for (int slot = 0; slot < player.getEnderChestInventory().getContainerSize(); slot++) {
                visible.addAll(SummonerStackContents1211.summonerIds(
                        player.getEnderChestInventory().getItem(slot)));
            }
        }
        for (var level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity) {
                    visible.addAll(SummonerStackContents1211.summonerIds(itemEntity.getItem()));
                } else if (entity instanceof Container container) {
                    for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        visible.addAll(SummonerStackContents1211.summonerIds(container.getItem(slot)));
                    }
                }
            }
        }
        return visible;
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
