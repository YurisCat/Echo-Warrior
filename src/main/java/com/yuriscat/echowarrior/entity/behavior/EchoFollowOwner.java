package com.yuriscat.echowarrior.entity.behavior;

import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.FollowOwner;

/**
 * SBL's owner-follow memory handles distance thresholds and safe teleporting.
 * Refreshing the vanilla navigation path here makes following a moving owner
 * reliable instead of waiting until the teleport threshold is reached.
 */
public final class EchoFollowOwner<E extends PathfinderMob & EchoWarriorEntity> extends FollowOwner<E> {
	private static final double SPEED_MODIFIER = 1.1;

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
		super.tick(entity);
		if (entity.tickCount % 5 == 0) {
			refreshPath(entity);
		}
	}

	@Override
	protected void stop(E entity) {
		entity.getNavigation().stop();
		super.stop(entity);
	}

	private void refreshPath(E entity) {
		if (!entity.shouldFollowOwner() || entity.isFollowMovementSuppressed()) {
			entity.getNavigation().stop();
		} else if (this.followingEntity != null && this.followingEntity.isAlive()) {
			entity.getNavigation().moveTo(this.followingEntity, SPEED_MODIFIER);
		}
	}
}
