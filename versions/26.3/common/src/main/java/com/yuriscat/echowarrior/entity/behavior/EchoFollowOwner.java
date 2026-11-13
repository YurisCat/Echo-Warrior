package com.yuriscat.echowarrior.entity.behavior;

import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.FollowOwner;

/**
 * Keeps terrestrial echoes moving toward their owner without ever selecting
 * unsupported air as a teleport destination.
 */
public final class EchoFollowOwner<E extends PathfinderMob & EchoWarriorEntity> extends FollowOwner<E> {
	private static final double SPEED_MODIFIER = 1.1;
	private static final double TELEPORT_DISTANCE_SQR = 32.0 * 32.0;

	public EchoFollowOwner() {
		this.speedModifier((float)SPEED_MODIFIER);
		this.closeEnoughDist(5.0F);
		this.startFollowingAfter(5.0F);
		this.teleportAfterDist(32.0F);
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
		LivingEntity owner = this.followingEntity;
		if (owner == null || !owner.isAlive() || owner.level() != entity.level()) {
			EchoSafeTeleport.stopFollowMovement(entity);
			return;
		}
		if (entity.distanceToSqr(owner) > TELEPORT_DISTANCE_SQR) {
			EchoSafeTeleport.stopFollowMovement(entity);
			if (!EchoSafeTeleport.isStableBelowSuspendedOwner(entity, owner)
					&& entity.level() instanceof ServerLevel level
					&& EchoSafeTeleport.mayAttemptAutomaticTeleport(level, entity)) {
				EchoSafeTeleport.teleportBesideOwner(entity, owner);
			}
			return;
		}
		if (EchoSafeTeleport.shouldSuspendGroundFollow(entity, owner)) {
			EchoSafeTeleport.stopFollowMovement(entity);
			return;
		}
		if (entity.tickCount % 5 == 0) {
			refreshPath(entity);
		}
	}

	@Override
	protected void stop(E entity) {
		EchoSafeTeleport.stopFollowMovement(entity);
		super.stop(entity);
	}

	private void refreshPath(E entity) {
		if (!entity.shouldFollowOwner() || entity.isFollowMovementSuppressed()) {
			EchoSafeTeleport.stopFollowMovement(entity);
		} else if (this.followingEntity != null && this.followingEntity.isAlive()) {
			if (entity.distanceToSqr(this.followingEntity) > TELEPORT_DISTANCE_SQR
					|| EchoSafeTeleport.shouldSuspendGroundFollow(entity, this.followingEntity)) {
				EchoSafeTeleport.stopFollowMovement(entity);
			} else {
				entity.getNavigation().moveTo(this.followingEntity, SPEED_MODIFIER);
			}
		}
	}
}
