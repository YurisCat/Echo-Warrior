package com.yuriscat.echowarrior.world;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.item.EchoCompassItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
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
import java.util.Map;
import java.util.UUID;

public final class EchoCompassSystem {
	private static final Map<UUID, Tracking> TRACKING = new HashMap<>();
	private static final Holder<SoundEvent> HUM = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_RESONATE);

	private EchoCompassSystem() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(EchoCompassSystem::tick);
	}

	public static void forceReacquire(ServerPlayer player) {
		TRACKING.remove(player.getUUID());
	}

	public static void invalidateCompletedSite(BlockPos relic) {
		TRACKING.entrySet().removeIf(entry -> entry.getValue().relicPos == relic.asLong());
	}

	public static void playToggle(ServerPlayer player, boolean enabled) {
		Holder<SoundEvent> sound = enabled ? HUM : SoundEvents.UI_BUTTON_CLICK;
		player.connection.send(new ClientboundSoundPacket(sound, SoundSource.PLAYERS,
				player.getX(), player.getY() + 1.0, player.getZ(), 0.7F, enabled ? 0.72F : 0.55F,
				player.getRandom().nextLong()));
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Tracking>> iterator = TRACKING.entrySet().iterator();
		while (iterator.hasNext()) {
			if (server.getPlayerList().getPlayer(iterator.next().getKey()) == null) iterator.remove();
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) tickPlayer(player);
	}

	private static void tickPlayer(ServerPlayer player) {
		ItemStack compass = selectedEnabledCompass(player);
		if (compass.isEmpty() || !player.level().dimension().equals(Level.OVERWORLD)) {
			TRACKING.remove(player.getUUID());
			return;
		}
		BattlefieldSavedData data = BattlefieldSavedData.get(player.level());
		Tracking tracking = TRACKING.get(player.getUUID());
		BattlefieldSavedData.ActiveSite site = tracking == null ? null : data.findActiveByCenter(tracking.centerPos);
		long now = player.level().getGameTime();
		if (site != null) {
			double distance = horizontalDistance(player.position(), site.center().getCenter());
			if (distance > 384.0) {
				if (tracking.outOfRangeSince == 0L) tracking.outOfRangeSince = now;
				else if (now - tracking.outOfRangeSince >= 100L) site = null;
			} else {
				tracking.outOfRangeSince = 0L;
			}
		}
		if (site == null) {
			site = data.nearestActive(player.blockPosition(), 320.0);
			if (site == null) {
				TRACKING.remove(player.getUUID());
				return;
			}
			tracking = new Tracking(site.center().asLong(), site.relic().asLong(), now, 0L);
			TRACKING.put(player.getUUID(), tracking);
		}
		if (now < tracking.nextPulse) return;

		Vec3 center = site.center().getCenter();
		Vec3 relic = site.relic().getCenter();
		double centerDistance = horizontalDistance(player.position(), center);
		double focusBlend = centerDistance >= 48.0 ? 0.0 : smoothstep(1.0 - centerDistance / 48.0);
		Vec3 target = center.lerp(relic, focusBlend);
		Vec3 direction = target.subtract(player.position()).multiply(1.0, 0.0, 1.0);
		if (direction.lengthSqr() < 1.0E-6) direction = player.getLookAngle().multiply(1.0, 0.0, 1.0);
		direction = direction.normalize();
		double distance = horizontalDistance(player.position(), target);
		double closeness = Math.clamp(1.0 - distance / 320.0, 0.0, 1.0);
		Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
		double facing = look.lengthSqr() < 1.0E-6 ? 0.0 : Math.max(0.0, look.normalize().dot(direction));
		Vec3 source = player.position().add(direction.scale(4.0 + 4.0 * (1.0 - closeness))).add(0.0, 1.2, 0.0);
		float volume = (float)(0.22 + closeness * 0.43);
		float pitch = (float)(0.68 + closeness * 0.24 + facing * 0.16);
		player.connection.send(new ClientboundSoundPacket(HUM, SoundSource.AMBIENT,
				source.x, source.y, source.z, volume, pitch, player.getRandom().nextLong()));
		tracking.nextPulse = now + pulseInterval(distance);
	}

	private static ItemStack selectedEnabledCompass(ServerPlayer player) {
		ItemStack main = player.getMainHandItem();
		if (EchoCompassItem.isEnabled(main)) return main;
		ItemStack offhand = player.getOffhandItem();
		if (EchoCompassItem.isEnabled(offhand)) return offhand;
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < 9; slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (EchoCompassItem.isEnabled(stack)) return stack;
		}
		for (int slot = 9; slot < Math.min(36, inventory.getContainerSize()); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.is(ModItems.ECHO_COMPASS) && EchoCompassItem.isEnabled(stack)) return stack;
		}
		return ItemStack.EMPTY;
	}

	private static int pulseInterval(double distance) {
		if (distance <= 24.0) return lerpTicks(distance / 24.0, 16, 50);
		if (distance <= 48.0) return lerpTicks((distance - 24.0) / 24.0, 50, 70);
		if (distance <= 128.0) return lerpTicks((distance - 48.0) / 80.0, 70, 120);
		if (distance <= 224.0) return lerpTicks((distance - 128.0) / 96.0, 120, 240);
		return lerpTicks((distance - 224.0) / 96.0, 240, 320);
	}

	private static int lerpTicks(double value, int near, int far) {
		return (int)Math.round(near + Math.clamp(value, 0.0, 1.0) * (far - near));
	}

	private static double smoothstep(double value) {
		double t = Math.clamp(value, 0.0, 1.0);
		return t * t * (3.0 - 2.0 * t);
	}

	private static double horizontalDistance(Vec3 first, Vec3 second) {
		double dx = first.x - second.x;
		double dz = first.z - second.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static final class Tracking {
		private final long centerPos;
		private final long relicPos;
		private long nextPulse;
		private long outOfRangeSince;

		private Tracking(long centerPos, long relicPos, long nextPulse, long outOfRangeSince) {
			this.centerPos = centerPos;
			this.relicPos = relicPos;
			this.nextPulse = nextPulse;
			this.outOfRangeSince = outOfRangeSince;
		}
	}
}
