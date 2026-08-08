package com.yuriscat.echowarrior.entity.behavior;

import com.yuriscat.echowarrior.entity.RomanLegionaryEchoEntity;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.FollowOwner;

/**
 * SBL's owner-follow memory handles distance thresholds and safe teleporting.
 * Refreshing the vanilla navigation path here makes following a moving owner
 * reliable instead of waiting until the teleport threshold is reached.
 */
public final class EchoFollowOwner extends FollowOwner<RomanLegionaryEchoEntity> {
	private static final double SPEED_MODIFIER = 1.1;

	public EchoFollowOwner() {
		this.speedModifier((float)SPEED_MODIFIER);
		this.closeEnoughDist(5.0F);
		this.startFollowingAfter(15.0F);
		this.teleportAfterDist(32.0F);
		this.noTimeout();
	}

	@Override
	protected void start(RomanLegionaryEchoEntity entity) {
		super.start(entity);
		refreshPath(entity);
	}

	@Override
	protected void tick(RomanLegionaryEchoEntity entity) {
		if (entity.isVisualInteractionMovementOwned()) {
			entity.getNavigation().stop();
			return;
		}
		super.tick(entity);
		if (entity.tickCount % 5 == 0) {
			refreshPath(entity);
		}
	}

	@Override
	protected void stop(RomanLegionaryEchoEntity entity) {
		entity.getNavigation().stop();
		super.stop(entity);
	}

	private void refreshPath(RomanLegionaryEchoEntity entity) {
		if (entity.isVisualInteractionMovementOwned()) {
			entity.getNavigation().stop();
		} else if (this.followingEntity != null && this.followingEntity.isAlive()) {
			entity.getNavigation().moveTo(this.followingEntity, SPEED_MODIFIER);
		}
	}
}
