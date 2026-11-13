package com.yuriscat.echowarrior.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Shared permission gate for intentional Creeper targeting. Ambient scans are
 * denied by default, while a Creeper that actually provoked the echo/owner or
 * was explicitly attacked by the owner can remain a committed combat target.
 * Area attacks deliberately do not use this gate, so incidental splash damage
 * keeps its natural gameplay result.
 */
final class EchoCreeperTargeting {
	private @Nullable UUID reactiveCreeperUuid;
	private long lastSeenAt = Long.MIN_VALUE / 2L;

	void authorizeReactive(Mob observer, @Nullable LivingEntity target, long now) {
		if (!(target instanceof Creeper)) return;
		this.reactiveCreeperUuid = target.getUUID();
		this.lastSeenAt = now;
	}

	void validate(Mob observer, long now, Predicate<LivingEntity> baseEligibility) {
		if (this.reactiveCreeperUuid == null || !(observer.level() instanceof ServerLevel level)) return;
		Entity found = level.getEntity(this.reactiveCreeperUuid);
		if (!(found instanceof Creeper creeper) || !creeper.isAlive() || !baseEligibility.test(creeper)) {
			clear();
			return;
		}
		if (observer.hasLineOfSight(creeper)) this.lastSeenAt = now;
		else if (now - this.lastSeenAt > EchoTargetVisibilityMemory.GRACE_TICKS) clear();
	}

	boolean canTarget(Mob observer, @Nullable LivingEntity target, long now, boolean allowProactiveCreeper) {
		if (!(target instanceof Creeper)) return true;
		if (allowProactiveCreeper) return true;
		if (this.reactiveCreeperUuid == null || !this.reactiveCreeperUuid.equals(target.getUUID())) return false;
		if (observer.hasLineOfSight(target)) this.lastSeenAt = now;
		return now - this.lastSeenAt <= EchoTargetVisibilityMemory.GRACE_TICKS;
	}

	private void clear() {
		this.reactiveCreeperUuid = null;
		this.lastSeenAt = Long.MIN_VALUE / 2L;
	}
}
