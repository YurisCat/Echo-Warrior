package com.yuriscat.echowarrior.world;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.item.EchoCompassItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public final class EchoCompassSystem {
	private static final double SEARCH_RADIUS = 2048.0;
	private static final double OUTSIDE_SOUND_RADIUS = 320.0;
	private static final double INNER_ENTER_RADIUS = 24.0;
	private static final double INNER_RELEASE_RADIUS = 48.0;
	private static final double INNER_SPIN_DISTANCE = 32.0;
	private static final double INNER_SPIN_MIN_TURNS_PER_TICK = 1.0 / 32.0;
	private static final double INNER_SPIN_MAX_TURNS_PER_TICK = 1.0 / 8.0;
	private static final double NO_TARGET_TURNS_PER_TICK = 1.0 / 10.0;
	private static final double SPIN_TARGET_RADIUS = 1024.0;
	private static final long SEARCH_INTERVAL = 40L;
	private static final long SALVAGE_MESSAGE_INTERVAL = 60L;
	private static final long SALVAGE_RELEASE_DELAY = 60L;
	private static final Map<UUID, Tracking> TRACKING = new HashMap<>();
	private static final Holder<SoundEvent> HUM = BuiltInRegistries.SOUND_EVENT
			.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_RESONATE);

	private EchoCompassSystem() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(EchoCompassSystem::tick);
	}

	public static void playToggle(ServerPlayer player, boolean enabled) {
		Holder<SoundEvent> sound = enabled ? HUM : SoundEvents.UI_BUTTON_CLICK;
		player.connection.send(new ClientboundSoundPacket(sound, SoundSource.PLAYERS,
				player.getX(), player.getY() + 1.0, player.getZ(), 0.7F, enabled ? 0.72F : 0.55F,
				player.getRandom().nextLong()));
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
					player.sendOverlayMessage(Component.translatable(
							"message.echo_warrior.echo_compass.site_quiet"));
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
				if (player != null) player.sendOverlayMessage(Component.translatable(
						"message.echo_warrior.echo_compass.site_quiet"));
				releaseToSearch(tracking, level.getGameTime());
			}
		}
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Tracking>> iterator = TRACKING.entrySet().iterator();
		while (iterator.hasNext()) {
			if (server.getPlayerList().getPlayer(iterator.next().getKey()) == null) iterator.remove();
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) tickPlayer(player);
	}

	private static void tickPlayer(ServerPlayer player) {
		if (!hasCompass(player) || !player.level().dimension().equals(Level.OVERWORLD)) {
			TRACKING.remove(player.getUUID());
			if (player.level().dimension().equals(Level.OVERWORLD)) {
				BattlefieldSavedData.get(player.level()).clearSalvageTracker(player.getUUID());
			}
			clearCompassTargets(player);
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
			spinCompassTargets(player, tracking, null);
			if (!tracking.noTargetNotified) {
				player.sendOverlayMessage(Component.translatable(
						"message.echo_warrior.echo_compass.no_nearby_site"));
				tracking.noTargetNotified = true;
			}
			return;
		}

		double centerDistance = horizontalDistance(player.position(), site.center().getCenter());
		if (tracking.mode == Mode.INNER && centerDistance > INNER_RELEASE_RADIUS) tracking.mode = Mode.OUTSIDE;
		else if (tracking.mode == Mode.OUTSIDE && centerDistance <= INNER_ENTER_RADIUS) tracking.mode = Mode.INNER;

		if (tracking.mode == Mode.INNER) {
			spinCompassTargets(player, tracking, site.relic());
			playDirectionalEcho(player, tracking, site.relic(), now);
		} else {
			setCompassTargets(player, site.center());
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
			player.sendOverlayMessage(Component.translatable("message.echo_warrior.echo_compass.site_quiet"));
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
		spinCompassTargets(player, tracking, target);
		playDirectionalEcho(player, tracking, target, now);
		if (tracking.lastRemainingCount != salvage.remaining().size() || now >= tracking.nextMessage) {
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
		player.sendOverlayMessage(Component.translatable(
				"message.echo_warrior.echo_compass.remaining_echoes", count));
		tracking.lastRemainingCount = count;
		tracking.nextMessage = now + SALVAGE_MESSAGE_INTERVAL;
	}

	private static void playOutsideReminder(
			ServerPlayer player,
			Tracking tracking,
			double distance,
			long now
	) {
		if (distance > OUTSIDE_SOUND_RADIUS || selectedSoundCompass(player).isEmpty()
				|| now < tracking.nextPulse) return;
		double closeness = Math.clamp(1.0 - distance / OUTSIDE_SOUND_RADIUS, 0.0, 1.0);
		float volume = (float)(0.14 + closeness * 0.12);
		float pitch = (float)(0.66 + closeness * 0.18);
		player.connection.send(new ClientboundSoundPacket(HUM, SoundSource.AMBIENT,
				player.getX(), player.getY() + 1.0, player.getZ(), volume, pitch,
				player.getRandom().nextLong()));
		tracking.nextPulse = now + (long)Math.round(80.0 + (1.0 - closeness) * 160.0);
	}

	private static void playDirectionalEcho(
			ServerPlayer player,
			Tracking tracking,
			BlockPos target,
			long now
	) {
		if (target == null || now < tracking.nextPulse) return;
		Vec3 targetCenter = target.getCenter();
		Vec3 delta = targetCenter.subtract(player.position());
		double distance = delta.length();
		Vec3 direction = delta.multiply(1.0, 0.25, 1.0);
		if (direction.lengthSqr() < 1.0E-6) direction = player.getLookAngle();
		direction = direction.normalize();
		double closeness = Math.clamp(1.0 - distance / 48.0, 0.0, 1.0);
		Vec3 source = player.position().add(direction.scale(4.0 + (1.0 - closeness) * 3.0)).add(0.0, 1.0, 0.0);
		float volume = (float)(0.28 + closeness * 0.52);
		float pitch = (float)(0.70 + closeness * 0.32);
		player.connection.send(new ClientboundSoundPacket(HUM, SoundSource.AMBIENT,
				source.x, source.y, source.z, volume, pitch, player.getRandom().nextLong()));
		tracking.nextPulse = now + (long)Math.round(14.0 + (1.0 - closeness) * 56.0);
	}

	private static void setCompassTargets(ServerPlayer player, BlockPos target) {
		LodestoneTracker desired = target == null ? null : new LodestoneTracker(
				Optional.of(GlobalPos.of(Level.OVERWORLD, target)), false);
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < Math.min(36, inventory.getContainerSize()); slot++) {
			setCompassTarget(inventory.getItem(slot), desired);
		}
		setCompassTarget(player.getOffhandItem(), desired);
	}

	private static void spinCompassTargets(ServerPlayer player, Tracking tracking, BlockPos keyTarget) {
		double speed = NO_TARGET_TURNS_PER_TICK;
		if (keyTarget != null) {
			double distance = player.position().distanceTo(keyTarget.getCenter());
			double closeness = Math.clamp(1.0 - distance / INNER_SPIN_DISTANCE, 0.0, 1.0);
			speed = INNER_SPIN_MIN_TURNS_PER_TICK
					+ (INNER_SPIN_MAX_TURNS_PER_TICK - INNER_SPIN_MIN_TURNS_PER_TICK) * closeness;
		}
		tracking.spinPhase = (tracking.spinPhase + speed) % 1.0;

		// CompassAngleState renders increasing frame indices clockwise. Keeping the virtual
		// target aligned to player yaw makes the animation independent of where the player looks.
		double worldAngle = Math.toRadians(player.getYRot()) + tracking.spinPhase * Math.PI * 2.0;
		BlockPos origin = player.blockPosition();
		BlockPos virtualTarget = new BlockPos(
				origin.getX() + (int)Math.round(Math.cos(worldAngle) * SPIN_TARGET_RADIUS),
				origin.getY(),
				origin.getZ() + (int)Math.round(Math.sin(worldAngle) * SPIN_TARGET_RADIUS)
		);
		setCompassTargets(player, virtualTarget);
	}

	private static void clearCompassTargets(ServerPlayer player) {
		setCompassTargets(player, null);
	}

	private static void setCompassTarget(ItemStack stack, LodestoneTracker desired) {
		if (!stack.is(ModItems.ECHO_COMPASS)) return;
		LodestoneTracker current = stack.get(DataComponents.LODESTONE_TRACKER);
		if (Objects.equals(current, desired)) return;
		if (desired == null) stack.remove(DataComponents.LODESTONE_TRACKER);
		else stack.set(DataComponents.LODESTONE_TRACKER, desired);
	}

	private static boolean hasCompass(ServerPlayer player) {
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
		tracking.nextPulse = now;
		tracking.nextMessage = now;
		tracking.lastRemainingCount = -1;
		tracking.noTargetNotified = false;
	}

	private static void releaseToSearch(Tracking tracking, long now) {
		tracking.mode = Mode.DORMANT;
		tracking.centerPos = 0L;
		tracking.lockedTarget = 0L;
		tracking.outOfRangeSince = 0L;
		tracking.nextSearch = now;
		tracking.nextPulse = now;
		tracking.nextMessage = now;
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
		private long nextPulse;
		private long nextMessage;
		private long outOfRangeSince;
		private double spinPhase;
		private int lastRemainingCount = -1;
		private boolean noTargetNotified;

		private Tracking(long now) {
			this.nextSearch = now;
			this.nextPulse = now;
			this.nextMessage = now;
		}
	}
}
