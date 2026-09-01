package com.yuriscat.echowarrior.binding;

import com.yuriscat.echowarrior.item.SummonerStackContents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Confirms that a client-side creative trash action refers to a summoner that
 * was actually removed from that player's server inventory.
 */
public final class CreativeSummonerDestroyTracker {
	private static final int CONFIRMATION_WINDOW_TICKS = 40;
	private static final Map<UUID, Map<UUID, Integer>> REMOVED_FROM_CREATIVE_INVENTORY = new HashMap<>();
	private static final Map<UUID, Map<UUID, Integer>> REQUESTED_CREATIVE_TRASH = new HashMap<>();
	private static final Map<UUID, Integer> REQUEST_NOT_BEFORE_TICK = new HashMap<>();

	private CreativeSummonerDestroyTracker() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(CreativeSummonerDestroyTracker::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearAll());
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clearPlayer(handler.getPlayer().getUUID()));
	}

	public static void noteCreativeSlotUpdate(ServerPlayer player, ItemStack previous, ItemStack replacement) {
		if (!player.hasInfiniteMaterials()) return;
		Set<UUID> previousIds = SummonerStackContents.summonerIds(previous);
		Set<UUID> replacementIds = SummonerStackContents.summonerIds(replacement);
		UUID playerId = player.getUUID();
		Map<UUID, Integer> removed = REMOVED_FROM_CREATIVE_INVENTORY.get(playerId);
		if (removed != null) {
			for (UUID replacementId : replacementIds) removed.remove(replacementId);
			if (removed.isEmpty()) REMOVED_FROM_CREATIVE_INVENTORY.remove(playerId);
		}
		previousIds.removeAll(replacementIds);
		if (previousIds.isEmpty()) return;

		int expiresAt = player.level().getServer().getTickCount() + CONFIRMATION_WINDOW_TICKS;
		removed = REMOVED_FROM_CREATIVE_INVENTORY.computeIfAbsent(playerId, ignored -> new HashMap<>());
		for (UUID previousId : previousIds) removed.put(previousId, expiresAt);
	}

	public static void requestCreativeTrash(ServerPlayer player, List<UUID> requestedIds) {
		if (!player.hasInfiniteMaterials() || requestedIds.isEmpty()) return;
		MinecraftServer server = player.level().getServer();
		int now = server.getTickCount();
		int expiresAt = now + CONFIRMATION_WINDOW_TICKS;
		UUID playerId = player.getUUID();
		Map<UUID, Integer> requested = REQUESTED_CREATIVE_TRASH.computeIfAbsent(
				playerId, ignored -> new HashMap<>());
		for (UUID summonerId : requestedIds) {
			requested.put(summonerId, expiresAt);
		}
		REQUEST_NOT_BEFORE_TICK.merge(playerId, now + 1, Math::max);
	}

	private static void tick(MinecraftServer server) {
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
			if (player == null || !player.hasInfiniteMaterials()) {
				playerIterator.remove();
				REMOVED_FROM_CREATIVE_INVENTORY.remove(playerId);
				REQUEST_NOT_BEFORE_TICK.remove(playerId);
				continue;
			}

			Iterator<UUID> requestIterator = playerEntry.getValue().keySet().iterator();
			while (requestIterator.hasNext()) {
				UUID summonerId = requestIterator.next();
				if (visibleIds.contains(summonerId)) {
					requestIterator.remove();
					if (removed != null) removed.remove(summonerId);
					continue;
				}
				if (removed == null || !removed.containsKey(summonerId)) continue;
				EchoBindingSystem.dismiss(server, summonerId, "creative_destroy_slot");
				requestIterator.remove();
				removed.remove(summonerId);
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
				visible.addAll(SummonerStackContents.summonerIds(stack));
			}
			visible.addAll(SummonerStackContents.summonerIds(player.inventoryMenu.getCarried()));
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

	private static void clearPlayer(UUID playerId) {
		REMOVED_FROM_CREATIVE_INVENTORY.remove(playerId);
		REQUESTED_CREATIVE_TRASH.remove(playerId);
		REQUEST_NOT_BEFORE_TICK.remove(playerId);
	}

	private static void clearAll() {
		REMOVED_FROM_CREATIVE_INVENTORY.clear();
		REQUESTED_CREATIVE_TRASH.clear();
		REQUEST_NOT_BEFORE_TICK.clear();
	}
}
