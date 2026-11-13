package com.yuriscat.echowarrior.entity.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.util.BrainUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Shared safe positioning rules for terrestrial echo recalls and long-distance following. */
public final class EchoSafeTeleport {
	private static final int AUTOMATIC_RETRY_TICKS = 20;
	private static final double SUSPENDED_VERTICAL_GAP = 8.0;
	private static final double STABLE_BELOW_HORIZONTAL_DISTANCE_SQR = 12.0 * 12.0;
	private static final Map<PathfinderMob, Long> NEXT_AUTOMATIC_ATTEMPT = new WeakHashMap<>();

	private EchoSafeTeleport() {
	}

	public static boolean shouldSuspendGroundFollow(PathfinderMob echo, LivingEntity owner) {
		return echo.level() == owner.level()
				&& !owner.onGround()
				&& !owner.isInWater()
				&& !owner.isInLava()
				&& owner.getY() - echo.getY() > SUSPENDED_VERTICAL_GAP;
	}

	public static boolean isStableBelowSuspendedOwner(PathfinderMob echo, LivingEntity owner) {
		if (!shouldSuspendGroundFollow(echo, owner)) return false;
		double dx = owner.getX() - echo.getX();
		double dz = owner.getZ() - echo.getZ();
		return dx * dx + dz * dz <= STABLE_BELOW_HORIZONTAL_DISTANCE_SQR;
	}

	public static boolean mayAttemptAutomaticTeleport(ServerLevel level, PathfinderMob echo) {
		long now = level.getGameTime();
		long nextAttempt = NEXT_AUTOMATIC_ATTEMPT.getOrDefault(echo, Long.MIN_VALUE);
		if (now < nextAttempt) return false;
		NEXT_AUTOMATIC_ATTEMPT.put(echo, now + AUTOMATIC_RETRY_TICKS);
		return true;
	}

	public static boolean teleportBesideOwner(PathfinderMob echo, LivingEntity owner) {
		if (!(echo.level() instanceof ServerLevel level) || owner.level() != level) return false;
		Vec3 destination = findSafeDestination(level, echo, owner);
		if (destination == null) {
			stopFollowMovement(echo);
			return false;
		}

		float yaw = yawToward(destination.x, destination.z, owner.getX(), owner.getZ());
		echo.snapTo(destination.x, destination.y, destination.z, yaw, 0.0F);
		echo.setYBodyRot(yaw);
		echo.setYHeadRot(yaw);
		echo.setDeltaMovement(Vec3.ZERO);
		echo.resetFallDistance();
		stopFollowMovement(echo);
		return true;
	}

	public static void stopFollowMovement(PathfinderMob echo) {
		echo.getNavigation().stop();
		echo.getMoveControl().setWait();
		echo.setSpeed(0.0F);
		echo.setXxa(0.0F);
		echo.setZza(0.0F);
		BrainUtil.clearMemory(echo, MemoryModuleType.WALK_TARGET);
	}

	private static Vec3 findSafeDestination(ServerLevel level, PathfinderMob echo, LivingEntity owner) {
		List<Vec3> horizontalCandidates = horizontalCandidates(owner);
		PathfindingContext pathfindingContext = new PathfindingContext(level, echo);
		int ownerY = owner.getBlockY();
		for (Vec3 horizontal : horizontalCandidates) {
			for (int offsetY : new int[] {0, 1, -1, 2, -2, 3, -3}) {
				Vec3 candidate = safeStandingPosition(
						level, echo, pathfindingContext, horizontal.x, ownerY + offsetY, horizontal.z);
				if (candidate != null) return candidate;
			}
		}

		if (!isClearlyAboveOpenTerrain(level, owner)) return null;
		for (Vec3 horizontal : horizontalCandidates) {
			int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(int)Math.floor(horizontal.x), (int)Math.floor(horizontal.z));
			Vec3 candidate = safeStandingPosition(
					level, echo, pathfindingContext, horizontal.x, surfaceY, horizontal.z);
			if (candidate != null) return candidate;
		}
		return null;
	}

	private static boolean isClearlyAboveOpenTerrain(ServerLevel level, LivingEntity owner) {
		if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) return false;
		int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				owner.getBlockX(), owner.getBlockZ());
		return owner.getY() >= surfaceY + SUSPENDED_VERTICAL_GAP;
	}

	private static List<Vec3> horizontalCandidates(LivingEntity owner) {
		List<Vec3> candidates = new ArrayList<>();
		Vec3 side = owner.getLookAngle().cross(new Vec3(0.0, 1.0, 0.0));
		if (side.lengthSqr() > 1.0E-4) {
			side = side.normalize().scale(1.5);
			candidates.add(new Vec3(owner.getX() + side.x, 0.0, owner.getZ() + side.z));
			candidates.add(new Vec3(owner.getX() - side.x, 0.0, owner.getZ() - side.z));
		}

		BlockPos center = owner.blockPosition();
		for (int radius = 1; radius <= 5; radius++) {
			addCandidate(candidates, center, radius, 0);
			addCandidate(candidates, center, -radius, 0);
			addCandidate(candidates, center, 0, radius);
			addCandidate(candidates, center, 0, -radius);
			addCandidate(candidates, center, radius, radius);
			addCandidate(candidates, center, radius, -radius);
			addCandidate(candidates, center, -radius, radius);
			addCandidate(candidates, center, -radius, -radius);
		}
		return candidates;
	}

	private static void addCandidate(List<Vec3> candidates, BlockPos center, int dx, int dz) {
		candidates.add(new Vec3(center.getX() + dx + 0.5, 0.0, center.getZ() + dz + 0.5));
	}

	private static Vec3 safeStandingPosition(
			ServerLevel level,
			PathfinderMob echo,
			PathfindingContext pathfindingContext,
			double x,
			int y,
			double z
	) {
		BlockPos feet = new BlockPos((int)Math.floor(x), y, (int)Math.floor(z));
		if (!level.isInWorldBounds(feet) || !level.isInWorldBounds(feet.above())
				|| !level.getWorldBorder().isWithinBounds(feet) || !level.hasChunkAt(feet)) return null;

		BlockPos floor = feet.below();
		if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
				|| !level.getFluidState(feet).isEmpty()
				|| !level.getFluidState(feet.above()).isEmpty()
				|| echo.getNavigation().getNodeEvaluator().getPathType(
						pathfindingContext, feet.getX(), feet.getY(), feet.getZ()) != PathType.WALKABLE) return null;

		Vec3 candidate = new Vec3(x, y, z);
		AABB movedBounds = echo.getBoundingBox().move(candidate.subtract(echo.position()));
		return level.noCollision(echo, movedBounds) ? candidate : null;
	}

	private static float yawToward(double fromX, double fromZ, double targetX, double targetZ) {
		return (float)(Math.atan2(targetZ - fromZ, targetX - fromX) * 180.0 / Math.PI) - 90.0F;
	}
}
