package com.yuriscat.echowarrior.world;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.item.EchoCompassItem;
import com.yuriscat.echowarrior.network.EchoCompassMessagePayload;
import com.yuriscat.echowarrior.network.EchoCompassStatePayload;
import com.yuriscat.echowarrior.network.EchoCompassPulsePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

public final class EchoCompassSystem {
	private static final double SEARCH_RADIUS = 2048.0;
	private static final double OUTSIDE_SOUND_RADIUS = 320.0;
	private static final double INNER_ENTER_RADIUS = 24.0;
	private static final double INNER_RELEASE_RADIUS = 48.0;
	private static final long SEARCH_INTERVAL = 40L;
	private static final long OUTSIDE_DETECTION_REARM_DELAY = 100L;
	private static final long SALVAGE_MESSAGE_INTERVAL = 60L;
	private static final long SALVAGE_MESSAGE_WINDOW = 200L;
	private static final long SALVAGE_MESSAGE_DISPLAY_DURATION = 48L;
	private static final long SALVAGE_RELEASE_DELAY = 60L;
	private static final Map<UUID, Tracking> TRACKING = new HashMap<>();
	private static final Map<UUID, EchoCompassStatePayload> LAST_RENDER_STATES = new HashMap<>();
	private static final Holder<SoundEvent> HUM = BuiltInRegistries.SOUND_EVENT
			.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_RESONATE);

	private EchoCompassSystem() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(EchoCompassSystem::tick);
	}

	public static void playToggle(ServerPlayer player, boolean enabled) {
		player.connection.send(new ClientboundSoundPacket(HUM, SoundSource.PLAYERS,
				player.getX(), player.getY() + 1.0, player.getZ(), 0.7F, enabled ? 1.28F : 0.68F,
				player.getRandom().nextLong()));
	}

	public static boolean isInsideBattlefieldMode(ServerPlayer player) {
		Tracking tracking = TRACKING.get(player.getUUID());
		return tracking != null
				&& (tracking.mode == Mode.INNER || tracking.mode == Mode.SALVAGE);
	}

	public static void sendMessage(ServerPlayer player, EchoCompassMessagePayload.Message message) {
		sendMessage(player, message, 0);
	}

	private static void sendMessage(
			ServerPlayer player,
			EchoCompassMessagePayload.Message message,
			int value
	) {
		EchoCompassMessagePayload payload = new EchoCompassMessagePayload(message, value);
		if (ServerPlayNetworking.canSend(player, EchoCompassMessagePayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		} else {
			player.sendOverlayMessage(payload.component());
		}
	}

	public static void onBattlefieldBlockRemoved(
			ServerLevel level,
			BattlefieldSavedData.RemovalResult result
	) {
		BattlefieldSavedData data = BattlefieldSavedData.get(level);
		if (result.relicCompleted()) {
			for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
				Tracking tracking = TRACKING.get(player.getUUID());
				if (tracking == null || tracking.centerPos != result.center().asLong()
						|| !hasCompass(player)) continue;
				if (result.remaining().isEmpty()) {
					data.clearSalvageTracker(player.getUUID());
					releaseToSearch(tracking, level.getGameTime());
					sendMessage(player, EchoCompassMessagePayload.Message.SITE_QUIET);
				} else {
					data.setSalvageTracker(player.getUUID(), result.center());
					enterSalvage(tracking, result.center(), level.getGameTime());
					sendRemainingMessage(player, tracking, result.remaining().size(), level.getGameTime());
				}
			}
		}
		if (result.remaining().isEmpty()) {
			data.clearSalvageTrackersAt(result.center());
			for (Map.Entry<UUID, Tracking> entry : TRACKING.entrySet()) {
				Tracking tracking = entry.getValue();
				if (tracking.mode != Mode.SALVAGE || tracking.centerPos != result.center().asLong()) continue;
				ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
				if (player != null) sendMessage(player, EchoCompassMessagePayload.Message.SITE_QUIET);
				releaseToSearch(tracking, level.getGameTime());
			}
		}
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Tracking>> iterator = TRACKING.entrySet().iterator();
		while (iterator.hasNext()) {
			if (server.getPlayerList().getPlayer(iterator.next().getKey()) == null) iterator.remove();
		}
		LAST_RENDER_STATES.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) tickPlayer(player);
	}

	private static void tickPlayer(ServerPlayer player) {
		if (!hasCompass(player) || !player.level().dimension().equals(Level.OVERWORLD)) {
			TRACKING.remove(player.getUUID());
			if (player.level().dimension().equals(Level.OVERWORLD)) {
				BattlefieldSavedData.get(player.level()).clearSalvageTracker(player.getUUID());
			}
			syncRenderState(player, EchoCompassStatePayload.inactive());
			return;
		}

		ServerLevel level = player.level();
		BattlefieldSavedData data = BattlefieldSavedData.get(level);
		long now = level.getGameTime();
		Tracking tracking = TRACKING.computeIfAbsent(player.getUUID(), ignored -> new Tracking(now));

		if (tracking.mode != Mode.SALVAGE) restoreSalvageIfValid(player, data, tracking, now);
		if (tracking.mode == Mode.SALVAGE) {
			tickSalvage(player, data, tracking, now);
			return;
		}

		BattlefieldSavedData.ActiveSite site = tracking.centerPos == 0L
				? null
				: data.findActiveByCenter(tracking.centerPos);
		if (site != null && horizontalDistance(player.position(), site.center().getCenter()) > SEARCH_RADIUS) {
			site = null;
			releaseToSearch(tracking, now);
		}
		if (site == null && tracking.centerPos != 0L) releaseToSearch(tracking, now);

		if (site == null && now >= tracking.nextSearch) {
			site = data.nearestActive(player.blockPosition(), SEARCH_RADIUS);
			tracking.nextSearch = now + SEARCH_INTERVAL;
			if (site != null) {
				tracking.centerPos = site.center().asLong();
				tracking.mode = Mode.OUTSIDE;
				tracking.noTargetNotified = false;
				data.clearSalvageTracker(player.getUUID());
			}
		}

		if (site == null) {
			updateOutsideDetection(player, tracking, false, now);
			syncRenderState(player, EchoCompassStatePayload.noTarget());
			if (!tracking.noTargetNotified) {
				sendMessage(player, EchoCompassMessagePayload.Message.NO_NEARBY_SITE);
				tracking.noTargetNotified = true;
			}
			return;
		}

		double centerDistance = horizontalDistance(player.position(), site.center().getCenter());
		updateOutsideDetection(player, tracking, centerDistance <= OUTSIDE_SOUND_RADIUS, now);
		if (tracking.mode == Mode.INNER && centerDistance > INNER_RELEASE_RADIUS) tracking.mode = Mode.OUTSIDE;
		else if (tracking.mode == Mode.OUTSIDE && centerDistance <= INNER_ENTER_RADIUS) tracking.mode = Mode.INNER;

		if (tracking.mode == Mode.INNER) {
			syncRenderState(player, new EchoCompassStatePayload(
					EchoCompassStatePayload.Mode.INNER, site.relic().asLong()));
			playDirectionalEcho(player, tracking, site.relic(), now);
		} else {
			syncRenderState(player, new EchoCompassStatePayload(
					EchoCompassStatePayload.Mode.OUTSIDE, site.center().asLong()));
			playOutsideReminder(player, tracking, centerDistance, now);
		}
	}

	private static void restoreSalvageIfValid(
			ServerPlayer player,
			BattlefieldSavedData data,
			Tracking tracking,
			long now
	) {
		OptionalLong savedCenter = data.salvageCenter(player.getUUID());
		if (savedCenter.isEmpty()) return;
		BattlefieldSavedData.SalvageSite salvage = data.findSalvageByCenter(savedCenter.getAsLong());
		if (salvage == null || horizontalDistance(player.position(), salvage.center().getCenter()) > INNER_RELEASE_RADIUS) {
			data.clearSalvageTracker(player.getUUID());
			return;
		}
		enterSalvage(tracking, salvage.center(), now);
	}

	private static void tickSalvage(
			ServerPlayer player,
			BattlefieldSavedData data,
			Tracking tracking,
			long now
	) {
		BattlefieldSavedData.SalvageSite salvage = data.findSalvageByCenter(tracking.centerPos);
		if (salvage == null || salvage.remaining().isEmpty()) {
			data.clearSalvageTracker(player.getUUID());
			sendMessage(player, EchoCompassMessagePayload.Message.SITE_QUIET);
			releaseToSearch(tracking, now);
			tickPlayer(player);
			return;
		}

		double centerDistance = horizontalDistance(player.position(), salvage.center().getCenter());
		if (centerDistance > INNER_RELEASE_RADIUS) {
			if (tracking.outOfRangeSince == 0L) tracking.outOfRangeSince = now;
			else if (now - tracking.outOfRangeSince >= SALVAGE_RELEASE_DELAY) {
				data.clearSalvageTracker(player.getUUID());
				releaseToSearch(tracking, now);
				tickPlayer(player);
				return;
			}
		} else {
			tracking.outOfRangeSince = 0L;
		}

		BlockPos target = lockedOrNearest(player, tracking, salvage.remaining());
		if (target != null) {
			syncRenderState(player, new EchoCompassStatePayload(
					EchoCompassStatePayload.Mode.SALVAGE, target.asLong()));
		} else {
			syncRenderState(player, EchoCompassStatePayload.noTarget());
		}
		playDirectionalEcho(player, tracking, target, now);
		if (now + SALVAGE_MESSAGE_DISPLAY_DURATION <= tracking.messageWindowEnds
				&& (tracking.lastRemainingCount != salvage.remaining().size() || now >= tracking.nextMessage)) {
			sendRemainingMessage(player, tracking, salvage.remaining().size(), now);
		}
	}

	private static BlockPos lockedOrNearest(ServerPlayer player, Tracking tracking, List<BlockPos> remaining) {
		if (tracking.lockedTarget != 0L) {
			for (BlockPos pos : remaining) {
				if (pos.asLong() == tracking.lockedTarget) return pos;
			}
		}
		BlockPos nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (BlockPos pos : remaining) {
			double distance = player.position().distanceToSqr(pos.getCenter());
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = pos;
			}
		}
		tracking.lockedTarget = nearest == null ? 0L : nearest.asLong();
		return nearest;
	}

	private static void sendRemainingMessage(ServerPlayer player, Tracking tracking, int count, long now) {
		sendMessage(player, EchoCompassMessagePayload.Message.REMAINING_ECHOES, count);
		tracking.lastRemainingCount = count;
		tracking.nextMessage = now + SALVAGE_MESSAGE_INTERVAL;
	}

	private static void playOutsideReminder(
			ServerPlayer player,
			Tracking tracking,
			double distance,
			long now
	) {
		if (distance > OUTSIDE_SOUND_RADIUS) return;
		double closeness = Math.clamp(1.0 - distance / OUTSIDE_SOUND_RADIUS, 0.0, 1.0);
		long interval = Math.round(80.0 + (1.0 - closeness) * 160.0);
		acceleratePendingPulse(tracking, interval);
		if (now < tracking.nextPulse) return;
		sendVisualPulse(player, closeness, false);
		if (!selectedSoundCompass(player).isEmpty()) {
			float volume = (float)(0.14 + closeness * 0.12);
			float pitch = (float)(0.66 + closeness * 0.18);
			player.connection.send(new ClientboundSoundPacket(HUM, SoundSource.AMBIENT,
					player.getX(), player.getY() + 1.0, player.getZ(), volume, pitch,
					player.getRandom().nextLong()));
		}
		scheduleNextPulse(tracking, now, interval);
	}

	private static void updateOutsideDetection(
			ServerPlayer player,
			Tracking tracking,
			boolean withinDetectionRange,
			long now
	) {
		if (withinDetectionRange) {
			tracking.outsideDetectionLostSince = -1L;
			if (!tracking.outsideDetectionLatched) {
				tracking.outsideDetectionLatched = true;
				sendMessage(player, EchoCompassMessagePayload.Message.ECHO_DETECTED);
			}
			return;
		}

		if (!tracking.outsideDetectionLatched) return;
		if (tracking.outsideDetectionLostSince < 0L) {
			tracking.outsideDetectionLostSince = now;
			return;
		}
		if (now - tracking.outsideDetectionLostSince >= OUTSIDE_DETECTION_REARM_DELAY) {
			tracking.outsideDetectionLatched = false;
			tracking.outsideDetectionLostSince = -1L;
		}
	}

	private static void playDirectionalEcho(
			ServerPlayer player,
			Tracking tracking,
			BlockPos target,
			long now
	) {
		if (target == null) return;
		Vec3 targetCenter = target.getCenter();
		Vec3 delta = targetCenter.subtract(player.position());
		double distance = delta.length();
		long interval = directionalPulseInterval(distance);
		acceleratePendingPulse(tracking, interval);
		if (now < tracking.nextPulse) return;
		Vec3 direction = delta.multiply(1.0, 0.25, 1.0);
		if (direction.lengthSqr() < 1.0E-6) direction = player.getLookAngle();
		direction = direction.normalize();
		double closeness = Math.clamp(1.0 - distance / 48.0, 0.0, 1.0);
		double response = smoothstep(closeness);
		sendVisualPulse(player, closeness, true);
		Vec3 source = player.position().add(direction.scale(4.0 + (1.0 - closeness) * 3.0)).add(0.0, 1.0, 0.0);
		float volume = (float)(0.20 + response * 0.60);
		float pitch = (float)(0.64 + response * 0.42);
		player.connection.send(new ClientboundSoundPacket(HUM, SoundSource.AMBIENT,
				source.x, source.y, source.z, volume, pitch, player.getRandom().nextLong()));
		scheduleNextPulse(tracking, now, interval);
	}

	private static void acceleratePendingPulse(Tracking tracking, long desiredInterval) {
		// A pulse that was scheduled while the player was farther away must not keep
		// the old, slower cadence after they approach the source or enter the ruin.
		// Anchor the new cadence to the time of the previous pulse: this preserves
		// its elapsed progress, while allowing an already-overdue faster pulse to
		// fire immediately on the current server tick.
		long acceleratedAt = tracking.lastPulseAt + desiredInterval;
		if (acceleratedAt < tracking.nextPulse) tracking.nextPulse = acceleratedAt;
	}

	private static void scheduleNextPulse(Tracking tracking, long now, long interval) {
		tracking.lastPulseAt = now;
		tracking.nextPulse = now + interval;
	}

	private static long directionalPulseInterval(double distance) {
		double clampedDistance = Math.clamp(distance, 0.0, 48.0);
		if (clampedDistance <= 4.0) return stagedInterval(clampedDistance, 0.0, 4.0, 14.0, 18.0);
		if (clampedDistance <= 10.0) return stagedInterval(clampedDistance, 4.0, 10.0, 18.0, 24.0);
		if (clampedDistance <= 20.0) return stagedInterval(clampedDistance, 10.0, 20.0, 24.0, 36.0);
		if (clampedDistance <= 32.0) return stagedInterval(clampedDistance, 20.0, 32.0, 36.0, 52.0);
		return stagedInterval(clampedDistance, 32.0, 48.0, 52.0, 72.0);
	}

	private static long stagedInterval(
			double value,
			double minimum,
			double maximum,
			double startTicks,
			double endTicks
	) {
		double progress = Math.clamp((value - minimum) / (maximum - minimum), 0.0, 1.0);
		double curvedProgress = smoothstep(progress);
		return Math.round(startTicks + (endTicks - startTicks) * curvedProgress);
	}

	private static double smoothstep(double value) {
		double clamped = Math.clamp(value, 0.0, 1.0);
		return clamped * clamped * (3.0 - 2.0 * clamped);
	}

	private static void sendVisualPulse(ServerPlayer player, double closeness, boolean directional) {
		if (!ServerPlayNetworking.canSend(player, EchoCompassPulsePayload.TYPE)) return;
		ServerPlayNetworking.send(player, new EchoCompassPulsePayload((float)closeness, directional));
	}

	private static void syncRenderState(ServerPlayer player, EchoCompassStatePayload state) {
		if (state.equals(LAST_RENDER_STATES.get(player.getUUID()))) return;
		if (!ServerPlayNetworking.canSend(player, EchoCompassStatePayload.TYPE)) return;
		ServerPlayNetworking.send(player, state);
		LAST_RENDER_STATES.put(player.getUUID(), state);
	}

	private static boolean hasCompass(ServerPlayer player) {
		// Moving an item between inventory slots temporarily places it in the active
		// menu's carried stack. Treat that stack as still being held so a normal
		// drag operation cannot erase the player's persisted salvage tracker.
		if (player.containerMenu.getCarried().is(ModItems.ECHO_COMPASS)) return true;
		if (player.getOffhandItem().is(ModItems.ECHO_COMPASS)) return true;
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < Math.min(36, inventory.getContainerSize()); slot++) {
			if (inventory.getItem(slot).is(ModItems.ECHO_COMPASS)) return true;
		}
		return false;
	}

	private static ItemStack selectedSoundCompass(ServerPlayer player) {
		ItemStack main = player.getMainHandItem();
		if (EchoCompassItem.isOutsideSoundEnabled(main)) return main;
		ItemStack offhand = player.getOffhandItem();
		if (EchoCompassItem.isOutsideSoundEnabled(offhand)) return offhand;
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (EchoCompassItem.isOutsideSoundEnabled(stack)) return stack;
		}
		for (int slot = 9; slot < Math.min(36, inventory.getContainerSize()); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (EchoCompassItem.isOutsideSoundEnabled(stack)) return stack;
		}
		return ItemStack.EMPTY;
	}

	private static void enterSalvage(Tracking tracking, BlockPos center, long now) {
		tracking.mode = Mode.SALVAGE;
		tracking.centerPos = center.asLong();
		tracking.lockedTarget = 0L;
		tracking.outOfRangeSince = 0L;
		tracking.lastPulseAt = now;
		tracking.nextPulse = now;
		tracking.nextMessage = now;
		tracking.messageWindowEnds = now + SALVAGE_MESSAGE_WINDOW;
		tracking.lastRemainingCount = -1;
		tracking.noTargetNotified = false;
	}

	private static void releaseToSearch(Tracking tracking, long now) {
		tracking.mode = Mode.DORMANT;
		tracking.centerPos = 0L;
		tracking.lockedTarget = 0L;
		tracking.outOfRangeSince = 0L;
		tracking.nextSearch = now;
		tracking.lastPulseAt = now;
		tracking.nextPulse = now;
		tracking.nextMessage = now;
		tracking.messageWindowEnds = 0L;
		tracking.lastRemainingCount = -1;
		tracking.noTargetNotified = false;
	}

	private static double horizontalDistance(Vec3 first, Vec3 second) {
		double dx = first.x - second.x;
		double dz = first.z - second.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	private enum Mode {
		DORMANT,
		OUTSIDE,
		INNER,
		SALVAGE
	}

	private static final class Tracking {
		private Mode mode = Mode.DORMANT;
		private long centerPos;
		private long lockedTarget;
		private long nextSearch;
		private long lastPulseAt;
		private long nextPulse;
		private long nextMessage;
		private long messageWindowEnds;
		private long outOfRangeSince;
		private long outsideDetectionLostSince = -1L;
		private int lastRemainingCount = -1;
		private boolean noTargetNotified;
		private boolean outsideDetectionLatched;

		private Tracking(long now) {
			this.nextSearch = now;
			this.lastPulseAt = now;
			this.nextPulse = now;
			this.nextMessage = now;
		}
	}
}
