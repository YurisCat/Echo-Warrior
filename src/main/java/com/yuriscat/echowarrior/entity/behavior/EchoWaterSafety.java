package com.yuriscat.echowarrior.entity.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.util.BrainUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shared water recovery for terrestrial echoes.
 *
 * <p>The vanilla {@code FloatGoal} performs ordinary surface swimming. This
 * helper only takes over after an echo has remained fully submerged without
 * making useful positional progress, then steers it toward its owner and uses
 * a safe recovery teleport as a final fallback.</p>
 */
public final class EchoWaterSafety {
	private static final int SURVIVAL_OVERRIDE_TICKS = 40;
	private static final int STUCK_TELEPORT_TICKS = 100;
	private static final int TELEPORT_RETRY_TICKS = 20;
	private static final double MEANINGFUL_ASCENT = 0.5;
	private static final double MEANINGFUL_OWNER_APPROACH_SQR = 2.25;
	private static final Map<PathfinderMob, RecoveryState> RECOVERY_STATES = new WeakHashMap<>();

	private EchoWaterSafety() {
	}

	public static void tick(ServerLevel level, PathfinderMob echo, LivingEntity owner, boolean mayReturnToOwner) {
		RecoveryState state = RECOVERY_STATES.computeIfAbsent(echo, ignored -> new RecoveryState());
		long now = level.getGameTime();
		if (!echo.isInWater()) {
			state.reset(echo.position(), now);
			return;
		}

		if (!echo.isEyeInFluid(FluidTags.WATER)) {
			state.reset(echo.position(), now);
			return;
		}
		// FloatGoal normally supplies this impulse. Repeating it only while fully
		// submerged makes recovery deterministic without causing shallow-water hops.
		echo.getJumpControl().jump();

		Vec3 position = echo.position();
		double ownerDistanceSqr = position.distanceToSqr(owner.position());
		if (!state.submerged) {
			state.begin(position, ownerDistanceSqr, now);
		} else if (position.y >= state.highestY + MEANINGFUL_ASCENT
				|| ownerDistanceSqr <= state.bestOwnerDistanceSqr - MEANINGFUL_OWNER_APPROACH_SQR) {
			state.recordProgress(position, ownerDistanceSqr, now);
		}

		long stalledTicks = now - state.lastProgressAt;
		if (!mayReturnToOwner || stalledTicks < SURVIVAL_OVERRIDE_TICKS) {
			return;
		}

		if (echo.tickCount % 5 == 0) {
			echo.setTarget(null);
			BrainUtil.clearMemory(echo, MemoryModuleType.ATTACK_TARGET);
			echo.getNavigation().moveTo(owner, 1.15);
		}

		if (stalledTicks < STUCK_TELEPORT_TICKS || now < state.nextTeleportAttemptAt) {
			return;
		}

		Vec3 safePosition = findSafeRecoveryPosition(level, echo, owner);
		if (safePosition == null) {
			state.nextTeleportAttemptAt = now + TELEPORT_RETRY_TICKS;
			return;
		}

		teleportToSafety(level, echo, owner, safePosition);
		state.reset(safePosition, now);
		state.nextTeleportAttemptAt = now + STUCK_TELEPORT_TICKS;
	}

	private static Vec3 findSafeRecoveryPosition(ServerLevel level, PathfinderMob echo, LivingEntity owner) {
		List<Vec3> horizontalCandidates = horizontalCandidates(owner);
		int ownerY = owner.getBlockY();
		for (Vec3 horizontal : horizontalCandidates) {
			for (int offsetY : new int[] {0, 1, -1, 2, -2, 3, -3}) {
				Vec3 candidate = new Vec3(horizontal.x, ownerY + offsetY, horizontal.z);
				if (isSafeDryStandingPosition(level, echo, candidate)) {
					return candidate;
				}
			}
		}

		for (Vec3 horizontal : horizontalCandidates) {
			Vec3 surface = findWaterSurfacePosition(level, echo, horizontal, ownerY);
			if (surface != null) {
				return surface;
			}
		}

		// If the owner is also deep underwater, returning close to them is still
		// preferable to leaving the echo trapped far below. FloatGoal will resume
		// the ascent from this new position and drowning immunity prevents loss.
		if (!owner.isEyeInFluid(FluidTags.WATER)) {
			return null;
		}
		for (Vec3 horizontal : horizontalCandidates) {
			Vec3 candidate = new Vec3(horizontal.x, owner.getY(), horizontal.z);
			BlockPos feet = BlockPos.containing(candidate);
			if (!level.getFluidState(feet).is(FluidTags.LAVA)
					&& !level.getFluidState(feet.above()).is(FluidTags.LAVA)
					&& hasNoCollision(level, echo, candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private static List<Vec3> horizontalCandidates(LivingEntity owner) {
		List<Vec3> candidates = new ArrayList<>();
		Vec3 side = owner.getLookAngle().cross(new Vec3(0.0, 1.0, 0.0));
		if (side.lengthSqr() > 1.0E-4) {
			side = side.normalize().scale(1.5);
			candidates.add(new Vec3(owner.getX() + side.x, 0.0, owner.getZ() + side.z));
			candidates.add(new Vec3(owner.getX() - side.x, 0.0, owner.getZ() - side.z));
		}
		for (int radius = 1; radius <= 5; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
					candidates.add(new Vec3(owner.getX() + dx + 0.5, 0.0, owner.getZ() + dz + 0.5));
				}
			}
		}
		return candidates;
	}

	private static boolean isSafeDryStandingPosition(ServerLevel level, PathfinderMob echo, Vec3 candidate) {
		BlockPos feet = BlockPos.containing(candidate);
		BlockPos floor = feet.below();
		if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) {
			return false;
		}
		if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
			return false;
		}
		return hasNoCollision(level, echo, candidate);
	}

	private static Vec3 findWaterSurfacePosition(ServerLevel level, PathfinderMob echo, Vec3 horizontal, int ownerY) {
		int minimumY = Math.max(level.getMinY(), ownerY - 4);
		int maximumY = Math.min(level.getMaxY(), ownerY + 32);
		boolean foundWater = false;
		for (int y = minimumY; y <= maximumY; y++) {
			BlockPos position = BlockPos.containing(horizontal.x, y, horizontal.z);
			if (level.getFluidState(position).is(FluidTags.WATER)) {
				foundWater = true;
				continue;
			}
			if (!foundWater) continue;
			if (level.getBlockState(position).isFaceSturdy(level, position, Direction.UP)) {
				Vec3 top = new Vec3(horizontal.x, y + 1.0, horizontal.z);
				return isSafeDryStandingPosition(level, echo, top) ? top : null;
			}
			Vec3 candidate = new Vec3(horizontal.x, y, horizontal.z);
			if (level.getFluidState(position).isEmpty() && level.getFluidState(position.above()).isEmpty()
					&& hasNoCollision(level, echo, candidate)) {
				return candidate;
			}
			return null;
		}
		return null;
	}

	private static boolean hasNoCollision(ServerLevel level, PathfinderMob echo, Vec3 candidate) {
		AABB movedBounds = echo.getBoundingBox().move(candidate.subtract(echo.position()));
		return level.noCollision(echo, movedBounds);
	}

	private static void teleportToSafety(ServerLevel level, PathfinderMob echo, LivingEntity owner, Vec3 position) {
		level.sendParticles(ParticleTypes.SOUL, echo.getX(), echo.getY() + 1.0, echo.getZ(),
				10, 0.25, 0.45, 0.25, 0.01);
		float yaw = yawToward(position.x, position.z, owner.getX(), owner.getZ());
		echo.snapTo(position.x, position.y, position.z, yaw, 0.0F);
		echo.setYBodyRot(yaw);
		echo.setYHeadRot(yaw);
		echo.setDeltaMovement(Vec3.ZERO);
		echo.setAirSupply(echo.getMaxAirSupply());
		echo.getNavigation().stop();
		echo.setTarget(null);
		BrainUtil.clearMemory(echo, MemoryModuleType.ATTACK_TARGET);
		level.sendParticles(ParticleTypes.SOUL, position.x, position.y + 1.0, position.z,
				12, 0.25, 0.5, 0.25, 0.01);
		level.playSound(null, BlockPos.containing(position), SoundEvents.ENDERMAN_TELEPORT,
				SoundSource.PLAYERS, 0.4F, 1.5F);
	}

	private static float yawToward(double fromX, double fromZ, double targetX, double targetZ) {
		return (float)(Math.atan2(targetZ - fromZ, targetX - fromX) * 180.0 / Math.PI) - 90.0F;
	}

	private static final class RecoveryState {
		private boolean submerged;
		private double highestY;
		private double bestOwnerDistanceSqr = Double.POSITIVE_INFINITY;
		private long lastProgressAt;
		private long nextTeleportAttemptAt;

		private void begin(Vec3 position, double ownerDistanceSqr, long now) {
			this.submerged = true;
			this.highestY = position.y;
			this.bestOwnerDistanceSqr = ownerDistanceSqr;
			this.lastProgressAt = now;
		}

		private void recordProgress(Vec3 position, double ownerDistanceSqr, long now) {
			this.highestY = Math.max(this.highestY, position.y);
			this.bestOwnerDistanceSqr = Math.min(this.bestOwnerDistanceSqr, ownerDistanceSqr);
			this.lastProgressAt = now;
		}

		private void reset(Vec3 position, long now) {
			this.submerged = false;
			this.highestY = position.y;
			this.bestOwnerDistanceSqr = Double.POSITIVE_INFINITY;
			this.lastProgressAt = now;
		}
	}
}
