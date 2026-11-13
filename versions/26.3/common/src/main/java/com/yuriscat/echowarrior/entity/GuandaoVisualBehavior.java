package com.yuriscat.echowarrior.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared server-authoritative attention and facial presentation for echo warriors.
 * The timings and priorities mirror the established Roman/Aztec visual contract,
 * while model-specific bone axes remain entirely in each client renderer.
 */
final class GuandaoVisualBehavior<T extends PathfinderMob & GuandaoVisualBehavior.Host> {
	private static final double HEAD_GAZE_RADIUS = 0.35;
	private static final double VISUAL_HEAD_CENTER_HEIGHT = 27.5 / 16.0;
	private static final double INVISIBLE_GAZE_RANGE = 4.0;
	private static final int GAZE_MISS_TOLERANCE_TICKS = 2;
	private static final int COMBAT_GAZE_SUPPRESSION_TICKS = 20 * 3;
	private static final int MUTUAL_GAZE_PRIORITY = 790;
	private static final int EYE_STICKY_TICKS = 5;
	private static final int HEAD_STICKY_TICKS = 10;
	private static final int LOCOMOTION_ATTENTION_PRIORITY = 320;
	private static final int LOCOMOTION_ATTENTION_TICKS = 6;
	private static final int CAUGHT_PREWATCH_TICKS = 8;
	private static final int CAUGHT_GLANCE_START_TICKS = 18;
	private static final double CAUGHT_MAX_OWNER_DISTANCE_SQR = 16.0 * 16.0;
	private static final double CAUGHT_EXIT_WALK_ELIGIBLE_OWNER_DISTANCE_SQR = 9.0 * 9.0;
	private static final double CAUGHT_EXIT_MAX_OWNER_DISTANCE_SQR = 12.0 * 12.0;
	private static final double CAUGHT_EXIT_FOLLOW_CANCEL_DISTANCE_SQR = 15.0 * 15.0;
	private static final float CAUGHT_EXIT_MIN_OWNER_ANGLE = 70.0F;
	private static final float CAUGHT_EXIT_MAX_WALK_ANGLE = 130.0F;

	private final T entity;
	private final RandomSource random = RandomSource.create();
	private final Map<UUID, PlayerGazeProgress> playerGazeProgress = new HashMap<>();

	private @Nullable LivingEntity eyeAttentionTarget;
	private int eyeAttentionPriority;
	private long eyeAttentionExpiresAt;
	private long eyeStickyUntil;
	private AttentionKind eyeAttentionKind = AttentionKind.NORMAL;
	private @Nullable LivingEntity attentionTarget;
	private int attentionPriority;
	private long attentionStartedAt;
	private long attentionExpiresAt;
	private long headStickyUntil;
	private AttentionKind headAttentionKind = AttentionKind.NORMAL;
	private @Nullable LivingEntity pendingHeadTarget;
	private long pendingHeadSince;
	private AttentionKind pendingHeadKind = AttentionKind.NORMAL;
	private @Nullable LivingEntity bodyAttentionTarget;
	private long bodyAttentionStartedAt;
	private long bodyAttentionExpiresAt;
	private AttentionKind bodyAttentionKind = AttentionKind.NORMAL;
	private long nextBlinkAt;

	private @Nullable UUID mutualGazePlayerUuid;
	private Vec3 mutualGazeLastSeenPoint = Vec3.ZERO;
	private int mutualGazeHoldTicksRemaining;
	private long mutualGazeCooldownUntil;
	private long mutualGazeLostSightAt = -1L;
	private long mutualGazeDistractionStartedAt = -1L;
	private boolean mutualGazeAligned;

	private long caughtReactionScheduledAt = -1L;
	private long caughtReactionStartedAt = -1L;
	private long caughtReactionGlanceEndAt = -1L;
	private long caughtReactionFinishAt = -1L;
	private long caughtReactionCooldownUntil;
	private int caughtReactionGazeMissTicks;
	private Vec3 caughtReactionAwayPoint = Vec3.ZERO;

	private CaughtExitMode caughtExitMode = CaughtExitMode.NONE;
	private long caughtExitEndsAt = -1L;
	private long caughtExitWalkStartsAt = -1L;
	private boolean caughtExitWalkStarted;
	private boolean caughtExitWalkArrived;
	private Vec3 caughtExitWalkTarget = Vec3.ZERO;
	private Vec3 caughtExitFocusPoint = Vec3.ZERO;
	private float caughtExitBodyTargetYaw;
	private boolean caughtExitSecondaryPlanned;
	private boolean caughtExitSecondaryDone;
	private long caughtExitSecondaryScheduledAt = -1L;
	private long caughtExitSecondaryStartedAt = -1L;
	private long caughtExitSecondaryGlanceEndAt = -1L;
	private long caughtExitSecondaryReturnEndAt = -1L;
	private long caughtExitOwnerAvoidUntil = -1L;
	private Vec3 caughtExitOwnerAvoidPoint = Vec3.ZERO;

	GuandaoVisualBehavior(T entity) {
		this.entity = entity;
	}

	void bindTo(LivingEntity owner) {
		Vec3 point = owner.getEyePosition();
		this.eyeAttentionTarget = owner;
		this.attentionTarget = owner;
		this.eyeAttentionPriority = 220;
		this.attentionPriority = 220;
		this.entity.setVisualEyeAttentionPoint(point);
		this.entity.setVisualAttentionPoint(point);
	}

	void tick(ServerLevel level, LivingEntity owner) {
		long now = level.getGameTime();
		tickBlinkClock(now);
		if (this.entity.getVisualReactionUntil() <= now
				&& this.entity.getVisualReaction() != Host.VISUAL_NORMAL) {
			this.entity.setVisualReaction(Host.VISUAL_NORMAL, now);
			this.entity.setVisualCuriousTilt((byte)0);
		}

		boolean combatSuppressed = isMutualGazeCombatSuppressed(owner, now);
		boolean scanTick = this.entity.tickCount % 2 == 0;
		AttentionCandidate candidate = scanTick ? findBestAttentionCandidate(level, owner, now) : null;
		Player acquiredPlayer = this.mutualGazePlayerUuid == null && now >= this.caughtExitOwnerAvoidUntil
				? tickPlayerGazeAcquisition(level, owner, combatSuppressed)
				: null;

		if (now < this.caughtExitOwnerAvoidUntil && (combatSuppressed
				|| candidate != null && isHardInterrupt(candidate))) {
			this.caughtExitOwnerAvoidUntil = -1L;
		}
		if (this.mutualGazePlayerUuid != null) {
			if (combatSuppressed || candidate != null && (isHardInterrupt(candidate)
					|| isCaughtReactionPendingOrActive() && isCaughtInterrupt(candidate))) {
				endMutualGaze(now);
			} else if (tickMutualGaze(level, now, candidate, scanTick)) {
				return;
			}
		}
		if (isCaughtExitActive()) {
			if (combatSuppressed || candidate != null && (isHardInterrupt(candidate) || isCaughtInterrupt(candidate))) {
				endCaughtExit(now, true);
			} else if (tickCaughtExit(level, owner, now)) {
				return;
			}
		}
		if (acquiredPlayer != null && now >= this.mutualGazeCooldownUntil
				&& !combatSuppressed && (candidate == null || !isHardInterrupt(candidate))) {
			beginMutualGaze(acquiredPlayer, owner, now);
			return;
		}
		if (candidate != null) considerAttentionCandidate(candidate, now);
	}

	void onHurt(long now, @Nullable LivingEntity attacker) {
		endCaughtExit(now, true);
		this.entity.setVisualBlink(now, (byte)1);
		Vec3 point = attacker != null
				? attacker.getEyePosition()
				// Environmental damage such as poison has no attacker. Keep the current
				// gaze level and forward instead of snapping toward a fake low rear point.
				: this.entity.getEyePosition().add(this.entity.getLookAngle().scale(4.0));
		applyAttention(new AttentionCandidate(attacker, point, 1100,
				Host.VISUAL_HURT, 16, false, AttentionKind.DAMAGE_SOURCE), now);
	}

	boolean ownsMovement() {
		return this.mutualGazePlayerUuid != null || isCaughtExitActive();
	}

	private @Nullable Player tickPlayerGazeAcquisition(ServerLevel level, LivingEntity owner, boolean suppressed) {
		if (suppressed || level.getGameTime() < this.mutualGazeCooldownUntil) {
			this.playerGazeProgress.clear();
			return null;
		}
		Player acquired = null;
		int acquiredTicks = -1;
		Set<UUID> seen = new HashSet<>();
		for (Player player : level.players()) {
			if (!player.isAlive() || player.isSpectator()) continue;
			seen.add(player.getUUID());
			PlayerGazeProgress progress = this.playerGazeProgress.computeIfAbsent(
					player.getUUID(), ignored -> new PlayerGazeProgress());
			GazeSample sample = samplePlayerHeadGaze(player);
			if (sample.state() == GazeState.VALID) {
				progress.validTicks++;
				progress.missedTicks = 0;
			} else if (sample.state() == GazeState.MISSED && progress.validTicks > 0
					&& progress.missedTicks < GAZE_MISS_TOLERANCE_TICKS) {
				progress.missedTicks++;
			} else {
				progress.reset();
			}
			if (progress.validTicks < requiredGazeTicks(sample.distance())) continue;
			boolean ownerPriority = player == owner;
			boolean currentOwner = acquired == owner;
			if (acquired == null || ownerPriority && !currentOwner
					|| ownerPriority == currentOwner && progress.validTicks > acquiredTicks) {
				acquired = player;
				acquiredTicks = progress.validTicks;
			}
		}
		this.playerGazeProgress.keySet().removeIf(uuid -> !seen.contains(uuid));
		return acquired;
	}

	private GazeSample samplePlayerHeadGaze(Player player) {
		Vec3 headCenter = this.entity.position().add(0.0, VISUAL_HEAD_CENTER_HEIGHT, 0.0);
		Vec3 from = player.getEyePosition();
		Vec3 towardHead = headCenter.subtract(from);
		double distance = towardHead.length();
		if (distance < 1.0E-4 || player.isInvisible() && distance > INVISIBLE_GAZE_RANGE) {
			return new GazeSample(GazeState.BLOCKED, distance);
		}
		Vec3 look = player.getLookAngle().normalize();
		double projection = look.dot(towardHead);
		if (projection <= 0.0) return new GazeSample(GazeState.MISSED, distance);
		double closestDistanceSqr = towardHead.lengthSqr() - projection * projection;
		if (closestDistanceSqr > HEAD_GAZE_RADIUS * HEAD_GAZE_RADIUS) {
			return new GazeSample(GazeState.MISSED, distance);
		}
		HitResult obstruction = this.entity.level().clip(new ClipContext(
				from, headCenter, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return new GazeSample(obstruction.getType() == HitResult.Type.MISS ? GazeState.VALID : GazeState.BLOCKED, distance);
	}

	private static int requiredGazeTicks(double distance) {
		return distance <= 12.0 ? 10 : Mth.ceil((0.5 + (distance - 12.0) / 40.0) * 20.0);
	}

	private boolean isMutualGazeCombatSuppressed(LivingEntity owner, long now) {
		return this.entity.isVisualCombatActive(now)
				|| isRecentWithin(this.entity, this.entity.getLastHurtByMobTimestamp(), COMBAT_GAZE_SUPPRESSION_TICKS)
				|| isRecentWithin(owner, owner.getLastHurtByMobTimestamp(), COMBAT_GAZE_SUPPRESSION_TICKS)
				|| isRecentWithin(owner, owner.getLastHurtMobTimestamp(), COMBAT_GAZE_SUPPRESSION_TICKS);
	}

	private void beginMutualGaze(Player player, LivingEntity owner, long now) {
		boolean caughtEligible = player == owner && now >= this.caughtReactionCooldownUntil
				&& this.eyeAttentionTarget == owner && this.attentionTarget == owner
				&& this.eyeAttentionKind == AttentionKind.NORMAL && this.headAttentionKind == AttentionKind.NORMAL
				&& now - this.attentionStartedAt >= CAUGHT_PREWATCH_TICKS;
		this.mutualGazePlayerUuid = player.getUUID();
		this.mutualGazeLastSeenPoint = player.getEyePosition();
		this.mutualGazeHoldTicksRemaining = 40 + this.random.nextInt(41);
		this.mutualGazeLostSightAt = -1L;
		this.mutualGazeDistractionStartedAt = -1L;
		this.mutualGazeAligned = false;
		this.caughtReactionScheduledAt = caughtEligible ? now + chooseCaughtReactionDelay() : -1L;
		this.playerGazeProgress.clear();
		applyAttention(new AttentionCandidate(player, player.getEyePosition(), MUTUAL_GAZE_PRIORITY,
				Host.VISUAL_MUTUAL_GAZE, 20 * 60, false, AttentionKind.MUTUAL_GAZE), now);
		this.entity.getNavigation().stop();
	}

	private boolean tickMutualGaze(ServerLevel level, long now, @Nullable AttentionCandidate candidate, boolean scanTick) {
		Player player = this.mutualGazePlayerUuid == null ? null : level.getPlayerByUUID(this.mutualGazePlayerUuid);
		if (player == null || !player.isAlive() || player.isSpectator()) {
			endMutualGaze(now);
			return false;
		}
		boolean visible = this.entity.hasLineOfSight(player);
		if (isCaughtReactionActive() && (!visible || this.entity.distanceToSqr(player) > CAUGHT_MAX_OWNER_DISTANCE_SQR)) {
			endMutualGaze(now);
			return false;
		}
		if (visible) {
			this.mutualGazeLastSeenPoint = player.getEyePosition();
			this.mutualGazeLostSightAt = -1L;
			setMutualGazePoints();
		} else {
			if (this.mutualGazeLostSightAt < 0L) this.mutualGazeLostSightAt = now;
			if (now - this.mutualGazeLostSightAt > 10) {
				endMutualGaze(now);
				return false;
			}
			this.entity.setVisualAttentionPoint(this.mutualGazeLastSeenPoint);
		}
		this.entity.getNavigation().stop();
		if (candidate != null && candidate.kind() == AttentionKind.CLOSE_CREEPER) {
			if (this.mutualGazeDistractionStartedAt < 0L) this.mutualGazeDistractionStartedAt = now;
			considerAttentionCandidate(candidate, now);
			if (now - this.mutualGazeDistractionStartedAt > 20) {
				endMutualGaze(now);
				return false;
			}
			return true;
		}
		if (this.mutualGazeDistractionStartedAt >= 0L && !scanTick) return true;
		if (this.mutualGazeDistractionStartedAt >= 0L) {
			this.mutualGazeDistractionStartedAt = -1L;
			applyAttention(new AttentionCandidate(player, this.mutualGazeLastSeenPoint, MUTUAL_GAZE_PRIORITY,
					Host.VISUAL_MUTUAL_GAZE, 20 * 60, false, AttentionKind.MUTUAL_GAZE), now);
		} else {
			setMutualGazePoints();
		}
		if (tickCaughtReaction(player, now)) return true;
		if (visible && this.mutualGazeAligned && --this.mutualGazeHoldTicksRemaining <= 0) {
			boolean stillLooking = samplePlayerHeadGaze(player).state() == GazeState.VALID;
			if (stillLooking && this.random.nextFloat() < 0.75F) {
				this.mutualGazeHoldTicksRemaining = 20 + this.random.nextInt(41);
			} else {
				endMutualGaze(now);
				if (stillLooking) startMutualGazeGlanceAway(now);
				return false;
			}
		}
		return true;
	}

	private void setMutualGazePoints() {
		this.entity.setVisualEyeAttentionPoint(this.mutualGazeLastSeenPoint);
		this.entity.setVisualAttentionPoint(this.mutualGazeLastSeenPoint);
	}

	private boolean tickCaughtReaction(Player owner, long now) {
		if (this.caughtReactionScheduledAt >= 0L && !isCaughtReactionActive()) {
			GazeState state = samplePlayerHeadGaze(owner).state();
			if (state == GazeState.VALID) {
				this.caughtReactionGazeMissTicks = 0;
			} else if (state == GazeState.MISSED && this.caughtReactionGazeMissTicks < GAZE_MISS_TOLERANCE_TICKS) {
				this.caughtReactionGazeMissTicks++;
				return true;
			} else {
				this.caughtReactionScheduledAt = -1L;
				this.caughtReactionGazeMissTicks = 0;
				return false;
			}
			if (now < this.caughtReactionScheduledAt) return true;
			startCaughtReaction(owner, now);
		}
		if (!isCaughtReactionActive()) return false;
		long elapsed = now - this.caughtReactionStartedAt;
		this.entity.getNavigation().stop();
		this.entity.setVisualReaction(Host.VISUAL_CAUGHT, now + 10);
		this.entity.setVisualCuriousTilt((byte)0);
		if (elapsed < 10) {
			this.entity.setVisualEyeAttentionPoint(owner.getEyePosition());
			this.entity.setVisualAttentionPoint(owner.getEyePosition());
			return true;
		}
		this.entity.setVisualEyeAttentionPoint(this.caughtReactionAwayPoint);
		if (elapsed < 12) {
			this.entity.setVisualAttentionPoint(owner.getEyePosition());
			return true;
		}
		this.entity.setVisualAttentionPoint(this.caughtReactionAwayPoint);
		if (elapsed < CAUGHT_GLANCE_START_TICKS) return true;
		if (this.caughtReactionGlanceEndAt < 0L) {
			boolean stillLooking = samplePlayerHeadGaze(owner).state() == GazeState.VALID;
			this.caughtReactionGlanceEndAt = now + (stillLooking ? 4 + this.random.nextInt(4) : 8 + this.random.nextInt(4));
			this.caughtReactionFinishAt = this.caughtReactionGlanceEndAt + 5 + this.random.nextInt(3);
		}
		if (now < this.caughtReactionGlanceEndAt) {
			this.entity.setVisualEyeAttentionPoint(owner.getEyePosition());
			return true;
		}
		this.entity.setVisualEyeAttentionPoint(this.caughtReactionAwayPoint);
		if (now < this.caughtReactionFinishAt) return true;
		Vec3 awayPoint = this.caughtReactionAwayPoint;
		endMutualGaze(now);
		startCaughtExit(owner, awayPoint, now);
		return true;
	}

	private void startCaughtReaction(Player owner, long now) {
		this.caughtReactionScheduledAt = -1L;
		this.caughtReactionStartedAt = now;
		this.caughtReactionGlanceEndAt = -1L;
		this.caughtReactionFinishAt = -1L;
		this.caughtReactionGazeMissTicks = 0;
		this.caughtReactionAwayPoint = createAwayPoint(owner, 35.0F, 55.0F, 6.0, 9.0);
		this.entity.setVisualCaughtReactionStart(now);
		this.entity.setVisualBlink(now + 3, (byte)2);
		this.entity.setVisualReaction(Host.VISUAL_CAUGHT, now + 60);
		this.entity.setVisualCuriousTilt((byte)0);
		this.entity.bumpVisualSequence();
	}

	private int chooseCaughtReactionDelay() {
		int roll = this.random.nextInt(100);
		if (roll < 30) return 0;
		if (roll < 75) return 20 + this.random.nextInt(21);
		if (roll < 95) return 40 + this.random.nextInt(41);
		return 80 + this.random.nextInt(41);
	}

	private void startCaughtExit(LivingEntity owner, Vec3 awayPoint, long now) {
		int roll = this.random.nextInt(100);
		this.caughtExitMode = roll < 15 ? CaughtExitMode.LOOK_AWAY
				: roll < 40 ? CaughtExitMode.TURN_AWAY : CaughtExitMode.WALK_AWAY;
		this.caughtExitEndsAt = now + 40 + this.random.nextInt(41);
		this.caughtExitWalkStartsAt = now + 5 + this.random.nextInt(6);
		this.caughtExitWalkStarted = false;
		this.caughtExitWalkArrived = false;
		this.caughtExitSecondaryPlanned = this.random.nextFloat() < 0.75F;
		this.caughtExitSecondaryDone = false;
		this.caughtExitSecondaryScheduledAt = now + 14 + this.random.nextInt(16);
		this.caughtExitSecondaryStartedAt = -1L;
		this.caughtExitSecondaryGlanceEndAt = -1L;
		this.caughtExitSecondaryReturnEndAt = -1L;
		this.caughtExitFocusPoint = isAwayFromOwner(owner, awayPoint) ? awayPoint : createExitFallbackPoint(owner);
		if (this.caughtExitMode == CaughtExitMode.WALK_AWAY && !chooseCaughtExitWalkTarget(owner)) {
			this.caughtExitMode = CaughtExitMode.TURN_AWAY;
		}
		this.caughtExitBodyTargetYaw = yawToward(this.entity.getX(), this.entity.getZ(),
				this.caughtExitMode == CaughtExitMode.WALK_AWAY ? this.caughtExitWalkTarget.x : this.caughtExitFocusPoint.x,
				this.caughtExitMode == CaughtExitMode.WALK_AWAY ? this.caughtExitWalkTarget.z : this.caughtExitFocusPoint.z);
		applyCaughtExitFocus(now);
	}

	private boolean chooseCaughtExitWalkTarget(LivingEntity owner) {
		if (this.entity.distanceToSqr(owner) > CAUGHT_EXIT_WALK_ELIGIBLE_OWNER_DISTANCE_SQR) return false;
		float ownerYaw = yawToward(this.entity.getX(), this.entity.getZ(), owner.getX(), owner.getZ());
		for (int attempt = 0; attempt < 8; attempt++) {
			float offset = CAUGHT_EXIT_MIN_OWNER_ANGLE
					+ this.random.nextFloat() * (CAUGHT_EXIT_MAX_WALK_ANGLE - CAUGHT_EXIT_MIN_OWNER_ANGLE);
			float yaw = ownerYaw + (this.random.nextBoolean() ? offset : -offset);
			double distance = 1.5 + this.random.nextDouble() * 1.5;
			Vec3 direction = directionFromYaw(yaw);
			Vec3 target = this.entity.position().add(direction.scale(distance));
			if (owner.distanceToSqr(target.x, this.entity.getY(), target.z) > CAUGHT_EXIT_MAX_OWNER_DISTANCE_SQR) continue;
			if (!this.entity.getNavigation().moveTo(target.x, target.y, target.z, 0.7)) continue;
			this.entity.getNavigation().stop();
			this.caughtExitWalkTarget = target;
			this.caughtExitFocusPoint = target.add(0.0, this.entity.getEyeHeight(), 0.0);
			return true;
		}
		return false;
	}

	private boolean tickCaughtExit(ServerLevel level, LivingEntity owner, long now) {
		if (!isCaughtExitActive()) return false;
		if (this.entity.distanceToSqr(owner) >= CAUGHT_EXIT_FOLLOW_CANCEL_DISTANCE_SQR) {
			endCaughtExit(now, true);
			return false;
		}
		if (this.caughtExitMode == CaughtExitMode.WALK_AWAY && !this.caughtExitWalkArrived) {
			this.entity.setVisualEyeAttentionPoint(this.caughtExitWalkTarget.add(0.0, this.entity.getEyeHeight(), 0.0));
			this.entity.setVisualAttentionPoint(this.caughtExitWalkTarget.add(0.0, this.entity.getEyeHeight(), 0.0));
			if (!this.caughtExitWalkStarted && now >= this.caughtExitWalkStartsAt) {
				this.caughtExitWalkStarted = this.entity.getNavigation().moveTo(
						this.caughtExitWalkTarget.x, this.caughtExitWalkTarget.y, this.caughtExitWalkTarget.z, 0.7);
				if (!this.caughtExitWalkStarted) this.caughtExitMode = CaughtExitMode.TURN_AWAY;
			}
			if (this.caughtExitWalkStarted && (this.entity.distanceToSqr(this.caughtExitWalkTarget) <= 0.45 * 0.45
					|| this.entity.getNavigation().isDone())) {
				this.caughtExitWalkArrived = true;
			}
		}
		tickCaughtExitSecondaryGlance(owner, now);
		if (now >= this.caughtExitEndsAt) {
			endCaughtExit(now, false);
			return false;
		}
		if (this.caughtExitSecondaryStartedAt < 0L) applyCaughtExitFocus(now);
		return true;
	}

	private void tickCaughtExitSecondaryGlance(LivingEntity owner, long now) {
		if (!this.caughtExitSecondaryPlanned || this.caughtExitSecondaryDone) return;
		if (this.caughtExitMode == CaughtExitMode.WALK_AWAY && !this.caughtExitWalkStarted) return;
		if (this.caughtExitSecondaryStartedAt < 0L) {
			if (now < this.caughtExitSecondaryScheduledAt) return;
			this.caughtExitSecondaryStartedAt = now;
			boolean ownerStillLooking = owner instanceof Player player
					&& samplePlayerHeadGaze(player).state() == GazeState.VALID;
			this.caughtExitSecondaryGlanceEndAt = now + (ownerStillLooking ? 3 + this.random.nextInt(3) : 7 + this.random.nextInt(5));
			this.caughtExitSecondaryReturnEndAt = this.caughtExitSecondaryGlanceEndAt + 3;
		}
		if (now < this.caughtExitSecondaryGlanceEndAt) {
			this.entity.setVisualEyeAttentionPoint(owner.getEyePosition());
			if (now - this.caughtExitSecondaryStartedAt >= 2) {
				Vec3 direction = owner.getEyePosition().subtract(this.entity.getEyePosition()).normalize();
				this.entity.setVisualAttentionPoint(this.entity.getEyePosition().add(direction.scale(4.0)));
			}
			return;
		}
		this.entity.setVisualEyeAttentionPoint(this.caughtExitFocusPoint);
		this.entity.setVisualAttentionPoint(this.caughtExitFocusPoint);
		if (now >= this.caughtExitSecondaryReturnEndAt) {
			this.caughtExitSecondaryDone = true;
			this.caughtExitSecondaryStartedAt = -1L;
		}
	}

	private void endCaughtExit(long now, boolean interrupted) {
		if (!isCaughtExitActive()) return;
		if (this.caughtExitMode == CaughtExitMode.WALK_AWAY) this.entity.getNavigation().stop();
		Vec3 finalFocus = this.caughtExitFocusPoint;
		this.caughtExitMode = CaughtExitMode.NONE;
		this.caughtExitEndsAt = -1L;
		this.caughtExitWalkStarted = false;
		this.caughtExitWalkArrived = false;
		this.caughtExitSecondaryPlanned = false;
		this.caughtExitSecondaryDone = false;
		this.caughtExitSecondaryStartedAt = -1L;
		if (!interrupted) {
			this.caughtReactionCooldownUntil = now + 160 + this.random.nextInt(141);
			this.mutualGazeCooldownUntil = Math.max(this.mutualGazeCooldownUntil, this.caughtReactionCooldownUntil);
			this.caughtExitOwnerAvoidUntil = now + 80 + this.random.nextInt(61);
			this.caughtExitOwnerAvoidPoint = finalFocus;
			applyAttention(new AttentionCandidate(null, finalFocus, 235,
					Host.VISUAL_NORMAL, 35 + this.random.nextInt(36), false, AttentionKind.NORMAL), now);
		}
	}

	private AttentionCandidate findBestAttentionCandidate(ServerLevel level, LivingEntity owner, long now) {
		if (now < this.caughtExitOwnerAvoidUntil && !isAwayFromOwner(owner, this.caughtExitOwnerAvoidPoint)) {
			this.caughtExitOwnerAvoidPoint = createExitFallbackPoint(owner);
		}
		LivingEntity combatTarget = this.entity.getTarget();
		boolean locomotion = shouldUseLocomotionAttention();
		AttentionCandidate best = isVisible(combatTarget)
				? new AttentionCandidate(combatTarget, combatTarget.getEyePosition(), 800,
						Host.VISUAL_ALERT, 30, false, AttentionKind.COMBAT_TARGET)
				: now < this.caughtExitOwnerAvoidUntil
						? new AttentionCandidate(null, this.caughtExitOwnerAvoidPoint, 220,
								Host.VISUAL_NORMAL, 35 + this.random.nextInt(36), false, AttentionKind.NORMAL)
						: locomotion
								? new AttentionCandidate(null, createLocomotionAttentionPoint(), LOCOMOTION_ATTENTION_PRIORITY,
										Host.VISUAL_LOCOMOTION, LOCOMOTION_ATTENTION_TICKS, false, AttentionKind.LOCOMOTION)
								: new AttentionCandidate(owner, owner.getEyePosition(), 220,
										Host.VISUAL_NORMAL, 35 + this.random.nextInt(36), false, AttentionKind.NORMAL);

		LivingEntity attacker = this.entity.getLastHurtByMob();
		if (isRecentWithin(this.entity, this.entity.getLastHurtByMobTimestamp(), 20) && isVisible(attacker)) {
			best = new AttentionCandidate(attacker, attacker.getEyePosition(), 1100,
					Host.VISUAL_HURT, 16, false, AttentionKind.DAMAGE_SOURCE);
		}
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isRecentWithin(owner, owner.getLastHurtByMobTimestamp(), 20) && isVisible(ownerAttacker) && best.priority() < 1050) {
			best = new AttentionCandidate(ownerAttacker, ownerAttacker.getEyePosition(), 1050,
					Host.VISUAL_ALERT, 24, false, AttentionKind.DAMAGE_SOURCE);
		}

		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
				this.entity.getBoundingBox().inflate(12.0), candidate -> candidate != this.entity
						&& candidate.isAlive() && !candidate.isSpectator() && this.entity.hasLineOfSight(candidate));
		for (LivingEntity living : nearby) {
			if (living == owner && now < this.caughtExitOwnerAvoidUntil) continue;
			double distanceSqr = this.entity.distanceToSqr(living);
			if (living instanceof Creeper creeper) {
				boolean primed = creeper.getSwellDir() > 0 || creeper.isIgnited();
				int score = primed ? 1200 : distanceSqr <= 64.0 ? 900 : 540;
				AttentionKind kind = primed ? AttentionKind.PRIMED_CREEPER
						: distanceSqr <= 64.0 ? AttentionKind.CLOSE_CREEPER : AttentionKind.NORMAL;
				byte reaction = primed || distanceSqr <= 64.0
						? Host.VISUAL_STARTLED : Host.VISUAL_ALERT;
				if (score > best.priority()) best = new AttentionCandidate(living, living.getEyePosition(), score,
						reaction, primed ? 30 : 22, false, kind);
				continue;
			}
			double closingSpeed = approachingSpeed(living);
			int score;
			byte reaction = Host.VISUAL_NORMAL;
			int duration = 30 + this.random.nextInt(51);
			if (distanceSqr <= 100.0 && closingSpeed > 0.22) {
				score = 620 + (int)(closingSpeed * 250.0);
				reaction = Host.VISUAL_STARTLED;
				duration = 18;
			} else if (living == owner) score = 220;
			else if (living instanceof Player) score = 170;
			else score = 115;
			score -= (int)(Math.sqrt(distanceSqr) * 4.0);
			score += this.random.nextInt(35);
			if (score > best.priority()) {
				boolean curious = reaction == Host.VISUAL_NORMAL
						&& isInSafeIdleState() && this.random.nextFloat() < 0.1F;
				best = new AttentionCandidate(living, living.getEyePosition(), score,
						curious ? Host.VISUAL_CURIOUS : reaction, duration, curious,
						reaction == Host.VISUAL_STARTLED ? AttentionKind.APPROACHING : AttentionKind.NORMAL);
			}
		}
		if (now >= this.caughtExitOwnerAvoidUntil && best.priority() <= 220 && this.random.nextFloat() < 0.3F) {
			float yaw = this.entity.getYRot() + this.random.nextFloat() * 130.0F - 65.0F;
			double distance = 4.0 + this.random.nextDouble() * 5.0;
			Vec3 point = this.entity.getEyePosition().add(directionFromYaw(yaw).scale(distance))
					.add(0.0, this.random.nextDouble() * 3.0 - 1.0, 0.0);
			boolean curious = isInSafeIdleState() && this.random.nextFloat() < 0.1F;
			return new AttentionCandidate(null, point, 230,
					curious ? Host.VISUAL_CURIOUS : Host.VISUAL_NORMAL,
					30 + this.random.nextInt(51), curious, AttentionKind.NORMAL);
		}
		return best;
	}

	private void considerAttentionCandidate(AttentionCandidate candidate, long now) {
		boolean urgent = candidate.kind() == AttentionKind.PRIMED_CREEPER || candidate.kind() == AttentionKind.DAMAGE_SOURCE;
		boolean sameEye = candidate.target() != null && this.eyeAttentionTarget == candidate.target()
				&& this.eyeAttentionKind == candidate.kind();
		boolean eyeExpired = now >= this.eyeAttentionExpiresAt
				|| this.eyeAttentionTarget != null && !this.eyeAttentionTarget.isAlive();
		if (urgent || eyeExpired || now >= this.eyeStickyUntil && candidate.priority() >= this.eyeAttentionPriority + 80) {
			this.eyeAttentionTarget = candidate.target();
			this.eyeAttentionPriority = candidate.priority();
			this.eyeAttentionExpiresAt = now + candidate.durationTicks();
			this.eyeStickyUntil = now + EYE_STICKY_TICKS;
			this.eyeAttentionKind = candidate.kind();
			this.entity.setVisualEyeAttentionPoint(candidate.point());
			this.entity.setVisualReaction(candidate.reaction(), now + candidate.durationTicks());
			this.entity.setVisualCuriousTilt((byte)0);
			this.entity.bumpVisualSequence();
		} else if (sameEye) {
			this.entity.setVisualEyeAttentionPoint(candidate.target().getEyePosition());
		}
		if (this.pendingHeadTarget != candidate.target() || this.pendingHeadKind != candidate.kind()) {
			this.pendingHeadTarget = candidate.target();
			this.pendingHeadKind = candidate.kind();
			this.pendingHeadSince = now;
		}
		boolean sameHead = candidate.target() != null && this.attentionTarget == candidate.target()
				&& this.headAttentionKind == candidate.kind();
		boolean headExpired = now >= this.attentionExpiresAt
				|| this.attentionTarget != null && !this.attentionTarget.isAlive();
		int delay = candidate.kind() == AttentionKind.CLOSE_CREEPER ? 4
				: candidate.kind() == AttentionKind.APPROACHING ? 2 : 0;
		if (now - this.pendingHeadSince >= delay && (urgent || headExpired
				|| now >= this.headStickyUntil && candidate.priority() >= this.attentionPriority + 80)) {
			this.attentionTarget = candidate.target();
			this.attentionPriority = candidate.priority();
			this.attentionStartedAt = now;
			this.attentionExpiresAt = now + candidate.durationTicks();
			this.headStickyUntil = now + HEAD_STICKY_TICKS;
			this.headAttentionKind = candidate.kind();
			this.entity.setVisualAttentionStartedAt(now);
			this.entity.setVisualAttentionPoint(candidate.point());
			this.entity.setVisualCuriousTilt(candidate.curious()
					? (byte)(this.random.nextBoolean() ? 1 : -1) : (byte)0);
			this.entity.bumpVisualSequence();
		} else if (sameHead) {
			this.entity.setVisualAttentionPoint(candidate.target().getEyePosition());
		}
		configureBodyAttention(candidate, now);
	}

	private void applyAttention(AttentionCandidate candidate, long now) {
		this.eyeAttentionTarget = candidate.target();
		this.eyeAttentionPriority = candidate.priority();
		this.eyeAttentionExpiresAt = now + candidate.durationTicks();
		this.eyeStickyUntil = now + EYE_STICKY_TICKS;
		this.eyeAttentionKind = candidate.kind();
		this.attentionTarget = candidate.target();
		this.attentionPriority = candidate.priority();
		this.attentionStartedAt = now;
		this.attentionExpiresAt = now + candidate.durationTicks();
		this.headStickyUntil = now + HEAD_STICKY_TICKS;
		this.headAttentionKind = candidate.kind();
		this.pendingHeadTarget = candidate.target();
		this.pendingHeadKind = candidate.kind();
		this.pendingHeadSince = now;
		this.entity.setVisualEyeAttentionPoint(candidate.point());
		this.entity.setVisualAttentionPoint(candidate.point());
		this.entity.setVisualAttentionStartedAt(now);
		this.entity.setVisualReaction(candidate.reaction(), now + candidate.durationTicks());
		this.entity.setVisualCuriousTilt(candidate.curious()
				? (byte)(this.random.nextBoolean() ? 1 : -1) : (byte)0);
		this.entity.bumpVisualSequence();
		configureBodyAttention(candidate, now);
	}

	private void configureBodyAttention(AttentionCandidate candidate, long now) {
		if (candidate.target() == null || candidate.kind() != AttentionKind.PRIMED_CREEPER
				&& candidate.kind() != AttentionKind.DAMAGE_SOURCE
				&& candidate.kind() != AttentionKind.CLOSE_CREEPER
				&& candidate.kind() != AttentionKind.APPROACHING) return;
		if (this.bodyAttentionTarget != candidate.target() || this.bodyAttentionKind != candidate.kind()) {
			this.bodyAttentionStartedAt = now;
		}
		this.bodyAttentionTarget = candidate.target();
		this.bodyAttentionKind = candidate.kind();
		this.bodyAttentionExpiresAt = now + candidate.durationTicks();
	}

	void tickBodyFacing(long now) {
		if (this.mutualGazePlayerUuid != null && this.mutualGazeDistractionStartedAt < 0L
				&& !isCaughtReactionActive()) {
			float desired = yawToward(this.entity.getX(), this.entity.getZ(),
					this.mutualGazeLastSeenPoint.x, this.mutualGazeLastSeenPoint.z);
			float difference = Math.abs(Mth.wrapDegrees(desired - this.entity.visualBodyYaw()));
			if (difference <= 5.0F) {
				this.mutualGazeAligned = true;
			} else {
				this.mutualGazeAligned = false;
				this.entity.turnVisualBodyToward(this.mutualGazeLastSeenPoint, 8.0F);
			}
			return;
		}
		if (isCaughtExitActive()) {
			if (this.caughtExitMode == CaughtExitMode.TURN_AWAY
					|| this.caughtExitMode == CaughtExitMode.WALK_AWAY && !this.caughtExitWalkStarted) {
				this.entity.turnVisualBodyToward(directionPoint(this.caughtExitBodyTargetYaw), 5.0F);
			}
			return;
		}
		LivingEntity target = this.bodyAttentionTarget;
		if (target == null || !target.isAlive() || now >= this.bodyAttentionExpiresAt
				|| this.entity.isVisualCombatActive(now)) return;
		double distance = this.entity.distanceTo(target);
		float desired = yawToward(this.entity.getX(), this.entity.getZ(), target.getX(), target.getZ());
		float difference = Math.abs(Mth.wrapDegrees(desired - this.entity.visualBodyYaw()));
		boolean turn = switch (this.bodyAttentionKind) {
			case PRIMED_CREEPER -> distance <= 6.0 || difference > 75.0F && now - this.bodyAttentionStartedAt >= 6;
			case DAMAGE_SOURCE -> now - this.bodyAttentionStartedAt >= 3;
			case CLOSE_CREEPER -> distance <= 4.0 || willApproachWithin(target, 3.0, 10.0);
			case APPROACHING -> willApproachWithin(target, 3.0, 10.0);
			default -> false;
		};
		if (turn) this.entity.turnVisualBodyToward(target.getEyePosition(), 10.0F);
	}

	private void tickBlinkClock(long now) {
		if (this.nextBlinkAt == 0L) this.nextBlinkAt = now + 50 + this.random.nextInt(71);
		byte reaction = this.entity.getVisualReaction();
		if (now < this.nextBlinkAt || reaction == Host.VISUAL_STARTLED
				|| reaction == Host.VISUAL_HURT
				|| reaction == Host.VISUAL_CAUGHT) return;
		this.entity.setVisualBlink(now, this.random.nextFloat() < 0.1F ? (byte)2 : (byte)1);
		this.nextBlinkAt = now + 50 + this.random.nextInt(71);
	}

	private boolean shouldUseLocomotionAttention() {
		Vec3 movement = this.entity.getDeltaMovement();
		double horizontalSpeedSqr = movement.x * movement.x + movement.z * movement.z;
		return !ownsMovement() && (!this.entity.getNavigation().isDone() || horizontalSpeedSqr > 2.5E-4);
	}

	private Vec3 createLocomotionAttentionPoint() {
		Vec3 movement = this.entity.getDeltaMovement();
		Vec3 direction = new Vec3(movement.x, 0.0, movement.z);
		if (direction.lengthSqr() > 1.0E-4) direction = direction.normalize();
		else direction = directionFromYaw(this.entity.visualBodyYaw());
		return this.entity.getEyePosition().add(direction.scale(6.0));
	}

	private void startMutualGazeGlanceAway(long now) {
		this.mutualGazeCooldownUntil = now + 10 + this.random.nextInt(21);
		float offset = 35.0F + this.random.nextFloat() * 65.0F;
		float yaw = this.entity.visualBodyYaw() + (this.random.nextBoolean() ? offset : -offset);
		Vec3 point = this.entity.getEyePosition().add(directionFromYaw(yaw).scale(4.0 + this.random.nextDouble() * 3.0))
				.add(0.0, this.random.nextDouble() * 1.5 - 0.5, 0.0);
		applyAttention(new AttentionCandidate(null, point, 235, Host.VISUAL_NORMAL,
				(int)(this.mutualGazeCooldownUntil - now), false, AttentionKind.NORMAL), now);
	}

	private void endMutualGaze(long now) {
		boolean caughtActive = isCaughtReactionActive();
		this.caughtReactionScheduledAt = -1L;
		this.caughtReactionStartedAt = -1L;
		this.caughtReactionGlanceEndAt = -1L;
		this.caughtReactionFinishAt = -1L;
		this.caughtReactionGazeMissTicks = 0;
		this.entity.setVisualCaughtReactionStart(-100L);
		if (caughtActive) {
			this.caughtReactionCooldownUntil = now + 160 + this.random.nextInt(141);
			this.mutualGazeCooldownUntil = Math.max(this.mutualGazeCooldownUntil, this.caughtReactionCooldownUntil);
		}
		this.mutualGazePlayerUuid = null;
		this.mutualGazeHoldTicksRemaining = 0;
		this.mutualGazeLostSightAt = -1L;
		this.mutualGazeDistractionStartedAt = -1L;
		this.mutualGazeAligned = false;
		this.playerGazeProgress.clear();
		this.attentionTarget = null;
		this.attentionPriority = 0;
		this.attentionExpiresAt = now;
		this.headAttentionKind = AttentionKind.NORMAL;
		this.eyeAttentionTarget = null;
		this.eyeAttentionPriority = 0;
		this.eyeAttentionExpiresAt = now;
		this.eyeAttentionKind = AttentionKind.NORMAL;
		if (this.entity.getVisualReaction() == Host.VISUAL_MUTUAL_GAZE
				|| this.entity.getVisualReaction() == Host.VISUAL_CAUGHT) {
			this.entity.setVisualReaction(Host.VISUAL_NORMAL, now);
			this.entity.setVisualCuriousTilt((byte)0);
		}
	}

	private boolean isVisible(@Nullable LivingEntity target) {
		return target != null && target.isAlive() && target.level() == this.entity.level()
				&& this.entity.distanceToSqr(target) <= 32.0 * 32.0 && this.entity.hasLineOfSight(target);
	}

	private boolean isInSafeIdleState() {
		return this.entity.getTarget() == null && !isRecentWithin(this.entity,
				this.entity.getLastHurtByMobTimestamp(), 100) && this.entity.getNavigation().isDone();
	}

	private double approachingSpeed(LivingEntity target) {
		Vec3 toward = this.entity.position().subtract(target.position());
		return toward.lengthSqr() < 1.0E-4 ? 0.0 : target.getDeltaMovement().dot(toward.normalize());
	}

	private boolean willApproachWithin(LivingEntity target, double desiredDistance, double withinTicks) {
		double closingSpeed = approachingSpeed(target);
		double distanceToClose = Math.max(0.0, this.entity.distanceTo(target) - desiredDistance);
		return closingSpeed > 1.0E-4 && distanceToClose / closingSpeed <= withinTicks;
	}

	private boolean isAwayFromOwner(LivingEntity owner, Vec3 point) {
		float ownerYaw = yawToward(this.entity.getX(), this.entity.getZ(), owner.getX(), owner.getZ());
		float pointYaw = yawToward(this.entity.getX(), this.entity.getZ(), point.x, point.z);
		return Math.abs(Mth.wrapDegrees(pointYaw - ownerYaw)) >= CAUGHT_EXIT_MIN_OWNER_ANGLE;
	}

	private Vec3 createExitFallbackPoint(LivingEntity owner) {
		return createAwayPoint(owner, CAUGHT_EXIT_MIN_OWNER_ANGLE, CAUGHT_EXIT_MAX_WALK_ANGLE, 5.0, 8.0);
	}

	private Vec3 createAwayPoint(LivingEntity owner, float minAngle, float maxAngle, double minDistance, double maxDistance) {
		float ownerYaw = yawToward(this.entity.getX(), this.entity.getZ(), owner.getX(), owner.getZ());
		float offset = minAngle + this.random.nextFloat() * (maxAngle - minAngle);
		float yaw = ownerYaw + (this.random.nextBoolean() ? offset : -offset);
		double distance = minDistance + this.random.nextDouble() * (maxDistance - minDistance);
		return this.entity.getEyePosition().add(directionFromYaw(yaw).scale(distance))
				.add(0.0, 0.5 + this.random.nextDouble(), 0.0);
	}

	private void applyCaughtExitFocus(long now) {
		this.eyeAttentionTarget = null;
		this.attentionTarget = null;
		this.eyeAttentionKind = AttentionKind.NORMAL;
		this.headAttentionKind = AttentionKind.NORMAL;
		this.eyeAttentionPriority = 240;
		this.attentionPriority = 240;
		this.eyeAttentionExpiresAt = this.caughtExitEndsAt + 20;
		this.attentionExpiresAt = this.caughtExitEndsAt + 20;
		this.attentionStartedAt = now;
		this.entity.setVisualAttentionStartedAt(now);
		this.entity.setVisualEyeAttentionPoint(this.caughtExitFocusPoint);
		this.entity.setVisualAttentionPoint(this.caughtExitFocusPoint);
		this.entity.setVisualReaction(Host.VISUAL_NORMAL, this.caughtExitEndsAt + 20);
	}

	interface Host {
		byte VISUAL_NORMAL = 0;
		byte VISUAL_ALERT = 1;
		byte VISUAL_STARTLED = 2;
		byte VISUAL_HURT = 3;
		byte VISUAL_CURIOUS = 4;
		byte VISUAL_MUTUAL_GAZE = 5;
		byte VISUAL_CAUGHT = 6;
		byte VISUAL_LOCOMOTION = 7;

		byte getVisualReaction();
		long getVisualReactionUntil();
		void setVisualAttentionPoint(Vec3 point);
		void setVisualEyeAttentionPoint(Vec3 point);
		void setVisualReaction(byte reaction, long until);
		void setVisualBlink(long start, byte count);
		void setVisualCuriousTilt(byte tilt);
		void bumpVisualSequence();
		void setVisualAttentionStartedAt(long startedAt);
		void setVisualCaughtReactionStart(long startedAt);
		float visualBodyYaw();
		void turnVisualBodyToward(Vec3 point, float maxDegrees);
		boolean isVisualCombatActive(long now);
	}

	private Vec3 directionPoint(float yaw) {
		return this.entity.getEyePosition().add(directionFromYaw(yaw).scale(6.0));
	}

	private boolean isCaughtReactionPendingOrActive() {
		return this.caughtReactionScheduledAt >= 0L || isCaughtReactionActive();
	}

	private boolean isCaughtReactionActive() {
		return this.caughtReactionStartedAt >= 0L;
	}

	private boolean isCaughtExitActive() {
		return this.caughtExitMode != CaughtExitMode.NONE;
	}

	private static boolean isHardInterrupt(AttentionCandidate candidate) {
		return candidate.kind() == AttentionKind.PRIMED_CREEPER
				|| candidate.kind() == AttentionKind.DAMAGE_SOURCE
				|| candidate.kind() == AttentionKind.COMBAT_TARGET;
	}

	private static boolean isCaughtInterrupt(AttentionCandidate candidate) {
		return candidate.target() instanceof Creeper || candidate.kind() == AttentionKind.APPROACHING;
	}

	private static boolean isRecentWithin(LivingEntity source, int timestamp, int ticks) {
		return timestamp > 0 && source.tickCount - timestamp <= ticks;
	}

	private static Vec3 directionFromYaw(float yaw) {
		double radians = Math.toRadians(yaw);
		return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
	}

	private static float yawToward(double fromX, double fromZ, double targetX, double targetZ) {
		return (float)(Math.atan2(targetZ - fromZ, targetX - fromX) * 180.0 / Math.PI) - 90.0F;
	}

	private enum AttentionKind {
		PRIMED_CREEPER,
		DAMAGE_SOURCE,
		CLOSE_CREEPER,
		COMBAT_TARGET,
		MUTUAL_GAZE,
		APPROACHING,
		LOCOMOTION,
		NORMAL
	}

	private enum CaughtExitMode {
		NONE,
		LOOK_AWAY,
		TURN_AWAY,
		WALK_AWAY
	}

	private enum GazeState {
		VALID,
		MISSED,
		BLOCKED
	}

	private record AttentionCandidate(
			@Nullable LivingEntity target,
			Vec3 point,
			int priority,
			byte reaction,
			int durationTicks,
			boolean curious,
			AttentionKind kind
	) {
	}

	private record GazeSample(GazeState state, double distance) {
	}

	private static final class PlayerGazeProgress {
		private int validTicks;
		private int missedTicks;

		private void reset() {
			this.validTicks = 0;
			this.missedTicks = 0;
		}
	}
}
