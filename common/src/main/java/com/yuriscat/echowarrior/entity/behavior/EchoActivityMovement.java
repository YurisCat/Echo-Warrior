package com.yuriscat.echowarrior.entity.behavior;

import com.yuriscat.echowarrior.item.EchoRelicState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.util.BrainUtil;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shared idle movement controller for terrestrial echoes.
 *
 * <p>Following and combat retain ownership of SBL's walk-target memory. This
 * controller owns direct navigation only while an echo is waiting or wandering,
 * and releases it immediately when combat, special movement, visual interaction,
 * or water recovery takes priority.</p>
 */
public final class EchoActivityMovement {
	private static final double WAIT_RETURN_RADIUS = 2.0;
	private static final double RETURN_ARRIVAL_RADIUS = 1.0;
	private static final double WANDER_SOFT_RADIUS = 12.0;
	private static final double WANDER_TARGET_RADIUS = 14.5;
	private static final double WANDER_HARD_RADIUS = 16.0;
	private static final double WANDER_ARRIVAL_RADIUS = 1.0;
	private static final double MIN_WANDER_STEP = 3.0;
	private static final int TARGET_ATTEMPTS = 8;
	private static final int PROGRESS_CHECK_TICKS = 20;
	private static final double MIN_PROGRESS = 0.25;
	private static final Map<PathfinderMob, MovementState> MOVEMENT_STATES = new WeakHashMap<>();

	private EchoActivityMovement() {
	}

	public static void reset(PathfinderMob echo) {
		MOVEMENT_STATES.remove(echo);
		echo.getNavigation().stop();
		BrainUtil.clearMemory(echo, MemoryModuleType.WALK_TARGET);
	}

	public static void tick(ServerLevel level, PathfinderMob echo, EchoRelicState.ActivityMode mode,
			Vec3 anchor, boolean higherPriorityMovement) {
		long now = level.getGameTime();
		MovementState state = MOVEMENT_STATES.computeIfAbsent(echo,
				ignored -> MovementState.entering(mode, echo.position(), initialPause(echo), now));
		if (state.mode != mode) {
			if (state.ownsNavigation && !higherPriorityMovement) {
				echo.getNavigation().stop();
			}
			BrainUtil.clearMemory(echo, MemoryModuleType.WALK_TARGET);
			state.enter(mode, echo.position(), initialPause(echo), now);
		}

		if (mode == EchoRelicState.ActivityMode.FOLLOW) {
			state.higherPriorityActive = false;
			state.release(echo.position(), now, 0);
			return;
		}

		if (higherPriorityMovement) {
			// Combat or a special/visual movement may already have replaced our path
			// during this tick, so relinquish ownership without stopping navigation.
			state.release(echo.position(), now, 40);
			state.higherPriorityActive = true;
			return;
		}

		if (state.higherPriorityActive) {
			state.higherPriorityActive = false;
			echo.getNavigation().stop();
			BrainUtil.clearMemory(echo, MemoryModuleType.WALK_TARGET);
			state.release(echo.position(), now, 40);
			return;
		}

		if (echo.isEyeInFluid(FluidTags.WATER)) {
			if (state.ownsNavigation) {
				echo.getNavigation().stop();
			}
			state.release(echo.position(), now, 40);
			return;
		}

		if (mode == EchoRelicState.ActivityMode.WAIT) {
			tickWaiting(level, echo, anchor, state, now);
		} else {
			tickWandering(level, echo, anchor, state, now);
		}
	}

	private static void tickWaiting(ServerLevel level, PathfinderMob echo, Vec3 anchor,
			MovementState state, long now) {
		double distanceSqr = echo.position().distanceToSqr(anchor);
		if (distanceSqr <= RETURN_ARRIVAL_RADIUS * RETURN_ARRIVAL_RADIUS) {
			finishPath(echo, state, now, Integer.MAX_VALUE / 2);
			return;
		}
		if (distanceSqr <= WAIT_RETURN_RADIUS * WAIT_RETURN_RADIUS && state.phase != MovementPhase.RETURNING) {
			state.release(echo.position(), now, Integer.MAX_VALUE / 2);
			return;
		}
		if (state.phase != MovementPhase.RETURNING || echo.getNavigation().isDone()) {
			if (now >= state.nextDecisionAt && !startReturnPath(level, echo, anchor, state, now, 1.0)) {
				state.release(echo.position(), now, 20);
			}
			return;
		}
		if (isStuck(echo, state, now)) {
			finishPath(echo, state, now, 20);
		}
	}

	private static void tickWandering(ServerLevel level, PathfinderMob echo, Vec3 anchor,
			MovementState state, long now) {
		double anchorDistanceSqr = horizontalDistanceSqr(echo.position(), anchor);
		if (anchorDistanceSqr > WANDER_HARD_RADIUS * WANDER_HARD_RADIUS) {
			if (state.phase != MovementPhase.RETURNING || echo.getNavigation().isDone()) {
				if (now >= state.nextDecisionAt && !startReturnPath(level, echo, anchor, state, now, 0.95)) {
					state.release(echo.position(), now, 20);
				}
			} else if (isStuck(echo, state, now)) {
				finishPath(echo, state, now, 20);
			}
			return;
		}

		if (state.phase == MovementPhase.RETURNING) {
			if (anchorDistanceSqr <= WANDER_SOFT_RADIUS * WANDER_SOFT_RADIUS) {
				finishPath(echo, state, now, normalPause(echo));
			} else if (echo.getNavigation().isDone() || isStuck(echo, state, now)) {
				finishPath(echo, state, now, 20);
			}
			return;
		}

		if (state.phase == MovementPhase.STROLLING) {
			if (state.target != null
					&& horizontalDistanceSqr(echo.position(), state.target) <= WANDER_ARRIVAL_RADIUS * WANDER_ARRIVAL_RADIUS) {
				finishPath(echo, state, now, normalPause(echo));
			} else if (echo.getNavigation().isDone()) {
				finishPath(echo, state, now, retryPause(echo));
			} else if (isStuck(echo, state, now)) {
				finishPath(echo, state, now, retryPause(echo));
			}
			return;
		}

		if (now >= state.nextDecisionAt && !startWanderPath(level, echo, anchor, state, now)) {
			state.release(echo.position(), now, retryPause(echo));
		}
	}

	private static boolean startWanderPath(ServerLevel level, PathfinderMob echo, Vec3 anchor,
			MovementState state, long now) {
		Vec3 current = echo.position();
		boolean biasTowardAnchor = horizontalDistanceSqr(current, anchor) > WANDER_SOFT_RADIUS * WANDER_SOFT_RADIUS;
		for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
			double distance = 4.0 + echo.getRandom().nextDouble() * 4.0;
			if (echo.getRandom().nextFloat() < 0.2F) {
				distance += 2.0 + echo.getRandom().nextDouble();
			}
			Vec3 direction = wanderDirection(echo, current, anchor, biasTowardAnchor);
			Vec3 desired = current.add(direction.scale(distance));
			desired = clampToAnchorRadius(desired, anchor, WANDER_TARGET_RADIUS);
			Vec3 candidate = LandRandomPos.getPosTowards(echo, (int)Math.ceil(distance) + 2, 5, desired);
			if (candidate == null
					|| horizontalDistanceSqr(current, candidate) < MIN_WANDER_STEP * MIN_WANDER_STEP
					|| horizontalDistanceSqr(anchor, candidate) > WANDER_HARD_RADIUS * WANDER_HARD_RADIUS
					|| !isSafeLandCandidate(level, candidate)) {
				continue;
			}
			double speed = 0.75 + echo.getRandom().nextDouble() * 0.10;
			if (startPath(echo, candidate, speed, MovementPhase.STROLLING, state, now)) {
				return true;
			}
		}
		return false;
	}

	private static Vec3 wanderDirection(PathfinderMob echo, Vec3 current, Vec3 anchor, boolean biasTowardAnchor) {
		double angle;
		if (biasTowardAnchor) {
			Vec3 inward = anchor.subtract(current).multiply(1.0, 0.0, 1.0);
			angle = Math.atan2(inward.z, inward.x) + (echo.getRandom().nextDouble() - 0.5) * Math.PI / 2.0;
		} else {
			angle = echo.getRandom().nextDouble() * Math.PI * 2.0;
		}
		return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
	}

	private static Vec3 clampToAnchorRadius(Vec3 position, Vec3 anchor, double radius) {
		Vec3 offset = position.subtract(anchor).multiply(1.0, 0.0, 1.0);
		if (offset.lengthSqr() <= radius * radius) {
			return position;
		}
		Vec3 clamped = offset.normalize().scale(radius);
		return new Vec3(anchor.x + clamped.x, position.y, anchor.z + clamped.z);
	}

	private static boolean startReturnPath(ServerLevel level, PathfinderMob echo, Vec3 anchor,
			MovementState state, long now, double speed) {
		if (isSafeLandCandidate(level, anchor)
				&& startPath(echo, anchor, speed, MovementPhase.RETURNING, state, now)) {
			return true;
		}
		Vec3 candidate = LandRandomPos.getPosTowards(echo, 12, 5, anchor);
		return candidate != null
				&& isSafeLandCandidate(level, candidate)
				&& horizontalDistanceSqr(candidate, anchor) < horizontalDistanceSqr(echo.position(), anchor)
				&& startPath(echo, candidate, speed, MovementPhase.RETURNING, state, now);
	}

	private static boolean startPath(PathfinderMob echo, Vec3 target, double speed,
			MovementPhase phase, MovementState state, long now) {
		BrainUtil.clearMemory(echo, MemoryModuleType.WALK_TARGET);
		Path path = echo.getNavigation().createPath(BlockPos.containing(target), 0);
		if (path == null || !path.canReach() || !echo.getNavigation().moveTo(path, speed)) {
			return false;
		}
		state.phase = phase;
		state.target = target;
		state.ownsNavigation = true;
		state.lastProgressPosition = echo.position();
		state.lastProgressAt = now;
		return true;
	}

	private static boolean isSafeLandCandidate(ServerLevel level, Vec3 candidate) {
		BlockPos feet = BlockPos.containing(candidate);
		return level.getFluidState(feet).isEmpty()
				&& level.getFluidState(feet.above()).isEmpty()
				&& !level.getBlockState(feet).is(BlockTags.FIRE)
				&& !level.getBlockState(feet).is(Blocks.POWDER_SNOW);
	}

	private static boolean isStuck(PathfinderMob echo, MovementState state, long now) {
		if (now - state.lastProgressAt < PROGRESS_CHECK_TICKS) {
			return false;
		}
		Vec3 position = echo.position();
		boolean stuck = horizontalDistanceSqr(position, state.lastProgressPosition) < MIN_PROGRESS * MIN_PROGRESS;
		state.lastProgressPosition = position;
		state.lastProgressAt = now;
		return stuck;
	}

	private static void finishPath(PathfinderMob echo, MovementState state, long now, int pauseTicks) {
		if (state.ownsNavigation) {
			echo.getNavigation().stop();
		}
		state.release(echo.position(), now, pauseTicks);
	}

	private static int initialPause(PathfinderMob echo) {
		return 20 + echo.getRandom().nextInt(41);
	}

	private static int normalPause(PathfinderMob echo) {
		int pause = 40 + echo.getRandom().nextInt(61);
		if (echo.getRandom().nextFloat() < 0.15F) {
			pause += 60 + echo.getRandom().nextInt(41);
		}
		return pause;
	}

	private static int retryPause(PathfinderMob echo) {
		return 20 + echo.getRandom().nextInt(21);
	}

	private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
		double x = first.x - second.x;
		double z = first.z - second.z;
		return x * x + z * z;
	}

	private enum MovementPhase {
		RESTING,
		STROLLING,
		RETURNING
	}

	private static final class MovementState {
		private EchoRelicState.ActivityMode mode;
		private MovementPhase phase;
		private Vec3 target;
		private Vec3 lastProgressPosition;
		private long lastProgressAt;
		private long nextDecisionAt;
		private boolean ownsNavigation;
		private boolean higherPriorityActive;

		private static MovementState entering(EchoRelicState.ActivityMode mode, Vec3 position,
				int pauseTicks, long now) {
			MovementState state = new MovementState();
			state.enter(mode, position, pauseTicks, now);
			return state;
		}

		private void enter(EchoRelicState.ActivityMode mode, Vec3 position, int pauseTicks, long now) {
			this.mode = mode;
			this.higherPriorityActive = false;
			release(position, now, pauseTicks);
		}

		private void release(Vec3 position, long now, int pauseTicks) {
			this.phase = MovementPhase.RESTING;
			this.target = null;
			this.lastProgressPosition = position;
			this.lastProgressAt = now;
			this.nextDecisionAt = now + pauseTicks;
			this.ownsNavigation = false;
		}
	}
}
