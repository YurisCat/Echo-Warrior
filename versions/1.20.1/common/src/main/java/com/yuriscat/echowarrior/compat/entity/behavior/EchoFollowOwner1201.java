package com.yuriscat.echowarrior.compat.entity.behavior;

import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.FollowEntity;

/**
 * Keeps terrestrial echoes moving toward their owner without ever selecting
 * unsupported air as a teleport destination.
 */
public final class EchoFollowOwner1201<E extends PathfinderMob & EchoWarriorEntity1201>
		extends FollowEntity<E, LivingEntity> {
	private static final double SPEED_MODIFIER = 1.1;
	private static final double TELEPORT_DISTANCE_SQR = 32.0 * 32.0;

	public EchoFollowOwner1201() {
		this.following(entity -> entity.ownerEntity());
		this.speedMod((float)SPEED_MODIFIER);
		this.stopFollowingWithin(5.0);
		this.teleportToTargetAfter(Double.MAX_VALUE);
		this.startCondition(entity -> entity.shouldFollowOwner() && !entity.isFollowMovementSuppressed());
		this.stopIf(entity -> !entity.shouldFollowOwner() || entity.isFollowMovementSuppressed());
		this.noTimeout();
	}

	@Override
	protected void start(E entity) {
		super.start(entity);
		refreshPath(entity);
	}

	@Override
	protected void tick(E entity) {
		if (!entity.shouldFollowOwner() || entity.isFollowMovementSuppressed()) {
			return;
		}
		LivingEntity owner = this.followingEntityProvider.apply(entity);
		if (owner == null || !owner.isAlive() || owner.level() != entity.level()) {
			EchoSafeTeleport1201.stopFollowMovement(entity);
			return;
		}
		if (entity.distanceToSqr(owner) > TELEPORT_DISTANCE_SQR) {
			EchoSafeTeleport1201.stopFollowMovement(entity);
			if (!EchoSafeTeleport1201.isStableBelowSuspendedOwner(entity, owner)
					&& entity.level() instanceof ServerLevel level
					&& EchoSafeTeleport1201.mayAttemptAutomaticTeleport(level, entity)) {
				EchoSafeTeleport1201.teleportBesideOwner(entity, owner);
			}
			return;
		}
		if (EchoSafeTeleport1201.shouldSuspendGroundFollow(entity, owner)) {
			EchoSafeTeleport1201.stopFollowMovement(entity);
			return;
		}
		if (entity.tickCount % 5 == 0) {
			refreshPath(entity);
		}
	}

	@Override
	protected void stop(E entity) {
		EchoSafeTeleport1201.stopFollowMovement(entity);
		super.stop(entity);
	}

	private void refreshPath(E entity) {
		if (!entity.shouldFollowOwner() || entity.isFollowMovementSuppressed()) {
			EchoSafeTeleport1201.stopFollowMovement(entity);
		} else {
			LivingEntity owner = this.followingEntityProvider.apply(entity);
			if (owner == null || !owner.isAlive()) {
				EchoSafeTeleport1201.stopFollowMovement(entity);
			} else if (entity.distanceToSqr(owner) > TELEPORT_DISTANCE_SQR
					|| EchoSafeTeleport1201.shouldSuspendGroundFollow(entity, owner)) {
				EchoSafeTeleport1201.stopFollowMovement(entity);
			} else {
				entity.getNavigation().moveTo(owner, SPEED_MODIFIER);
			}
		}
	}
}
