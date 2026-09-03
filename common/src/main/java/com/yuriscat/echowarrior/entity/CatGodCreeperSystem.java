package com.yuriscat.echowarrior.entity;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.item.EchoAccessorySystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class CatGodCreeperSystem {
	private static final double AURA_RADIUS = 30.0;
	private static final long PANIC_TICKS = 100L;
	private static final Map<UUID, PanicState> PANICKING = new HashMap<>();
	private static final Map<UUID, Long> DISORIENTED_UNTIL = new HashMap<>();

	private CatGodCreeperSystem() {
	}

	public static void initialize() {
	}

	public static void tickAura(ServerLevel level, EgyptianArcherEchoEntity archer) {
		if (!archer.isCatGodActive() || (archer.tickCount & 1) != 0) return;
		for (Creeper creeper : level.getEntitiesOfClass(Creeper.class,
				archer.getBoundingBox().inflate(AURA_RADIUS),
				candidate -> candidate.isAlive() && candidate.distanceToSqr(archer) <= AURA_RADIUS * AURA_RADIUS)) {
			if (creeper.getSwellDir() > 0 || creeper.isIgnited()) panic(level, creeper, archer.position());
			if (PANICKING.containsKey(creeper.getUUID())) {
				creeper.setSwellDir(-1);
				creeper.setTarget(null);
			}
		}
	}

	public static void onDirectlyDamaged(ServerLevel level, Creeper creeper) {
		EgyptianArcherEchoEntity protector = nearestProtector(level, creeper);
		if (protector != null) panic(level, creeper, protector.position());
	}

	public static boolean shouldCancelExplosion(Creeper creeper) {
		if (!(creeper.level() instanceof ServerLevel level)) return false;
		EgyptianArcherEchoEntity protector = nearestProtector(level, creeper);
		if (protector != null) panic(level, creeper, protector.position());
		else if (!hasAccessoryProtector(level, creeper)) return false;
		creeper.setSwellDir(-1);
		return true;
	}

	public static boolean shouldSuppressIgnition(Creeper creeper) {
		if (!(creeper.level() instanceof ServerLevel level)
				|| creeper.getSwellDir() <= 0 && !creeper.isIgnited()) return false;
		EgyptianArcherEchoEntity protector = nearestProtector(level, creeper);
		if (protector != null) panic(level, creeper, protector.position());
		else if (!hasAccessoryProtector(level, creeper)) return false;
		return true;
	}

	private static boolean hasAccessoryProtector(ServerLevel level, Creeper creeper) {
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof EchoWarriorEntity echo && echo.livingEntity().isAlive()
					&& creeper.distanceToSqr(echo.livingEntity()) <= 36.0
					&& EchoAccessorySystem.has(echo, ModItems.CAT_BELL_FISH_CHARM_ACCESSORY)) return true;
		}
		return false;
	}

	public static boolean isPanicking(Creeper creeper) {
		return PANICKING.containsKey(creeper.getUUID());
	}

	public static void disorientFromShieldCharge(ServerLevel level, Creeper creeper, long durationTicks) {
		long until = level.getGameTime() + Math.max(1L, durationTicks);
		DISORIENTED_UNTIL.merge(creeper.getUUID(), until, Math::max);
		creeper.setTarget(null);
		creeper.setSwellDir(-1);
	}

	private static EgyptianArcherEchoEntity nearestProtector(ServerLevel level, Creeper creeper) {
		EgyptianArcherEchoEntity best = null;
		double bestDistance = AURA_RADIUS * AURA_RADIUS;
		for (EgyptianArcherEchoEntity archer : level.getEntitiesOfClass(EgyptianArcherEchoEntity.class,
				creeper.getBoundingBox().inflate(AURA_RADIUS),
				candidate -> candidate.isAlive() && candidate.isCatGodActive())) {
			double distance = creeper.distanceToSqr(archer);
			if (distance <= bestDistance) {
				best = archer;
				bestDistance = distance;
			}
		}
		return best;
	}

	private static void panic(ServerLevel level, Creeper creeper, Vec3 source) {
		PANICKING.put(creeper.getUUID(), new PanicState(level.getGameTime() + PANIC_TICKS, source));
		creeper.setSwellDir(-1);
		creeper.setTarget(null);
		moveAway(creeper, source);
	}

	public static void tickPanickingCreepers(ServerLevel level) {
		long now = level.getGameTime();
		Iterator<Map.Entry<UUID, PanicState>> iterator = PANICKING.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, PanicState> entry = iterator.next();
			Entity found = level.getEntityInAnyDimension(entry.getKey());
			if (!(found instanceof Creeper creeper) || !creeper.isAlive() || now >= entry.getValue().until()) {
				if (found != null || now >= entry.getValue().until()) iterator.remove();
				continue;
			}
			creeper.setTarget(null);
			if (nearestProtector(level, creeper) != null) creeper.setSwellDir(-1);
			if (creeper.tickCount % 10 == 0 || creeper.getNavigation().isDone()) {
				moveAway(creeper, entry.getValue().source());
			}
		}
		tickDisorientedCreepers(level, now);
	}

	private static void tickDisorientedCreepers(ServerLevel level, long now) {
		Iterator<Map.Entry<UUID, Long>> iterator = DISORIENTED_UNTIL.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Long> entry = iterator.next();
			Entity found = level.getEntityInAnyDimension(entry.getKey());
			if (!(found instanceof Creeper creeper) || !creeper.isAlive() || now >= entry.getValue()) {
				if (found != null || now >= entry.getValue()) iterator.remove();
				continue;
			}
			creeper.setTarget(null);
			creeper.setSwellDir(-1);
		}
	}

	private static void moveAway(Creeper creeper, Vec3 source) {
		Vec3 escape = DefaultRandomPos.getPosAway(creeper, 12, 7, source);
		if (escape != null) creeper.getNavigation().moveTo(escape.x, escape.y, escape.z, 1.35);
	}

	private record PanicState(long until, Vec3 source) {
	}
}
