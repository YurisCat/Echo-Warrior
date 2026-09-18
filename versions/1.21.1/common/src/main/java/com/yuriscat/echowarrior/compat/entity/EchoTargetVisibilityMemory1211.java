package com.yuriscat.echowarrior.compat.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.UUID;

/**
 * Short line-of-sight memory shared by echoes when continuing an already
 * acquired combat target. New ambient targets must still be visible.
 */
final class EchoTargetVisibilityMemory1211 {
	static final int GRACE_TICKS = 40;

	private UUID targetUuid;
	private long lastSeenAt = Long.MIN_VALUE / 2L;

	boolean observe(Mob observer, LivingEntity target, long now) {
		if (target == null || !observer.hasLineOfSight(target)) return false;
		this.targetUuid = target.getUUID();
		this.lastSeenAt = now;
		return true;
	}

	boolean canRetain(Mob observer, LivingEntity target, long now) {
		if (target == null) return false;
		if (observe(observer, target, now)) return true;
		return target.getUUID().equals(this.targetUuid) && now - this.lastSeenAt <= GRACE_TICKS;
	}

	void clear() {
		this.targetUuid = null;
		this.lastSeenAt = Long.MIN_VALUE / 2L;
	}
}
