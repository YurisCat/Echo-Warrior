package com.yuriscat.echowarrior.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.yuriscat.echowarrior.entity.behavior.EchoFollowOwner;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import com.yuriscat.echowarrior.progress.EchoExperienceSystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetWalkTargetToAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.util.BrainUtil;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RomanLegionaryEchoEntity extends PathfinderMob
		implements OwnableEntity, SmartBrainOwner<RomanLegionaryEchoEntity>, GeoEntity {
	public static final byte VISUAL_NORMAL = 0;
	public static final byte VISUAL_ALERT = 1;
	public static final byte VISUAL_STARTLED = 2;
	public static final byte VISUAL_HURT = 3;
	public static final byte VISUAL_CURIOUS = 4;
	public static final byte VISUAL_MUTUAL_GAZE = 5;
	public static final byte VISUAL_CAUGHT = 6;

	private static final EntityDataAccessor<Float> ATTENTION_X = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ATTENTION_Y = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ATTENTION_Z = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> EYE_ATTENTION_X = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> EYE_ATTENTION_Y = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> EYE_ATTENTION_Z = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Byte> VISUAL_REACTION = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Long> VISUAL_REACTION_UNTIL = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> BLINK_START = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Byte> BLINK_COUNT = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> CURIOUS_TILT = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> VISUAL_SEQUENCE = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> ATTENTION_STARTED_AT = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> CAUGHT_REACTION_START = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.LONG);

	public static final int MAX_LIFETIME_TICKS = 20 * 120;
	public static final int SUMMONER_GRACE_TICKS = 20 * 5;
	private static final double HEAD_GAZE_RADIUS = 0.35;
	private static final double VISUAL_HEAD_CENTER_HEIGHT = 27.5 / 16.0;
	private static final double INVISIBLE_GAZE_RANGE = 4.0;
	private static final int GAZE_MISS_TOLERANCE_TICKS = 2;
	private static final int COMBAT_GAZE_SUPPRESSION_TICKS = 20 * 3;
	private static final int MUTUAL_GAZE_PRIORITY = 790;
	private static final int EYE_STICKY_TICKS = 5;
	private static final int HEAD_STICKY_TICKS = 10;
	private static final int CAUGHT_PREWATCH_TICKS = 8;
	private static final int CAUGHT_GLANCE_START_TICKS = 18;
	private static final double CAUGHT_MAX_OWNER_DISTANCE_SQR = 16.0 * 16.0;
	private static final double CAUGHT_EXIT_WALK_ELIGIBLE_OWNER_DISTANCE_SQR = 9.0 * 9.0;
	private static final double CAUGHT_EXIT_MAX_OWNER_DISTANCE_SQR = 12.0 * 12.0;
	private static final double CAUGHT_EXIT_FOLLOW_CANCEL_DISTANCE_SQR = 15.0 * 15.0;
	private static final float CAUGHT_EXIT_MIN_OWNER_ANGLE = 70.0F;
	private static final float CAUGHT_EXIT_MAX_WALK_ANGLE = 130.0F;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.roman_legionary.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.roman_legionary.walk");
	private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.roman_legionary.attack");
	private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.roman_legionary.hurt");
	private static final RawAnimation SHIELD_RAISE = RawAnimation.begin().thenPlayAndHold("animation.roman_legionary.shield_raise");
	private static final RawAnimation SHIELD_LOWER = RawAnimation.begin().thenPlay("animation.roman_legionary.shield_lower");
	private static final int MOVEMENT_ANIMATION_RELEASE_TICKS = 4;
	private static final String ACTION_CONTROLLER = "action";
	private static final String ATTACK_TRIGGER = "attack";
	private static final String HURT_TRIGGER = "hurt";
	private static final String SHIELD_RAISE_TRIGGER = "shield_raise";
	private static final String SHIELD_LOWER_TRIGGER = "shield_lower";
	private static final int ATTACK_ANIMATION_TICKS = 20;

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private boolean movementAnimationActive;
	private int movementAnimationLastMovingTick = Integer.MIN_VALUE;
	private @Nullable EntityReference<LivingEntity> ownerReference;
	private @Nullable UUID summonerUuid;
	private int remainingLifetime = MAX_LIFETIME_TICKS;
	private int missingSummonerTicks;
	private @Nullable LivingEntity attentionTarget;
	private Vec3 attentionPoint = Vec3.ZERO;
	private int attentionPriority;
	private long attentionStartedAt;
	private long attentionExpiresAt;
	private @Nullable LivingEntity eyeAttentionTarget;
	private Vec3 eyeAttentionPoint = Vec3.ZERO;
	private int eyeAttentionPriority;
	private long eyeAttentionExpiresAt;
	private AttentionKind eyeAttentionKind = AttentionKind.NORMAL;
	private long eyeStickyUntil;
	private long headStickyUntil;
	private AttentionKind headAttentionKind = AttentionKind.NORMAL;
	private @Nullable LivingEntity pendingHeadTarget;
	private AttentionKind pendingHeadKind = AttentionKind.NORMAL;
	private long pendingHeadSince;
	private @Nullable LivingEntity bodyAttentionTarget;
	private AttentionKind bodyAttentionKind = AttentionKind.NORMAL;
	private long bodyAttentionStartedAt;
	private long bodyAttentionExpiresAt;
	private long nextBlinkAt;
	private long forcedVisualUntil;
	private final Map<UUID, PlayerGazeProgress> playerGazeProgress = new HashMap<>();
	private @Nullable UUID mutualGazePlayerUuid;
	private Vec3 mutualGazeLastSeenPoint = Vec3.ZERO;
	private int mutualGazeHoldTicksRemaining;
	private long mutualGazeLostSightAt = -1L;
	private long mutualGazeCooldownUntil;
	private boolean mutualGazeBodyTurning;
	private boolean mutualGazeAligned;
	private long mutualGazeDistractionStartedAt = -1L;
	private long caughtReactionScheduledAt = -1L;
	private long caughtReactionStartedAt = -1L;
	private long caughtReactionGlanceEndAt = -1L;
	private long caughtReactionFinishAt = -1L;
	private long caughtReactionCooldownUntil;
	private int caughtReactionGazeMissTicks;
	private Vec3 caughtReactionAwayPoint = Vec3.ZERO;
	private CaughtExitMode caughtExitMode = CaughtExitMode.NONE;
	private long caughtExitStartedAt = -1L;
	private long caughtExitEndsAt = -1L;
	private @Nullable LivingEntity caughtExitFocusTarget;
	private Vec3 caughtExitFocusPoint = Vec3.ZERO;
	private Vec3 caughtExitWalkTarget = Vec3.ZERO;
	private float caughtExitBodyTargetYaw;
	private long caughtExitWalkStartsAt = -1L;
	private boolean caughtExitWalkStarted;
	private boolean caughtExitWalkArrived;
	private boolean caughtExitSecondaryPlanned;
	private boolean caughtExitSecondaryDone;
	private long caughtExitSecondaryScheduledAt = -1L;
	private long caughtExitSecondaryStartedAt = -1L;
	private long caughtExitSecondaryGlanceEndAt = -1L;
	private long caughtExitSecondaryReturnEndAt = -1L;
	private boolean caughtExitSecondaryOwnerStillLooking;
	private Vec3 caughtExitSecondaryHeadPoint = Vec3.ZERO;
	private long caughtExitOwnerAvoidUntil = -1L;
	private Vec3 caughtExitOwnerAvoidPoint = Vec3.ZERO;
	private long attackAnimationUntil;

	public RomanLegionaryEchoEntity(EntityType<? extends RomanLegionaryEchoEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder entityData) {
		super.defineSynchedData(entityData);
		entityData.define(ATTENTION_X, 0.0F);
		entityData.define(ATTENTION_Y, 0.0F);
		entityData.define(ATTENTION_Z, 0.0F);
		entityData.define(EYE_ATTENTION_X, 0.0F);
		entityData.define(EYE_ATTENTION_Y, 0.0F);
		entityData.define(EYE_ATTENTION_Z, 0.0F);
		entityData.define(VISUAL_REACTION, VISUAL_NORMAL);
		entityData.define(VISUAL_REACTION_UNTIL, 0L);
		entityData.define(BLINK_START, -100L);
		entityData.define(BLINK_COUNT, (byte)0);
		entityData.define(CURIOUS_TILT, (byte)0);
		entityData.define(VISUAL_SEQUENCE, 0);
		entityData.define(ATTENTION_STARTED_AT, 0L);
		entityData.define(CAUGHT_REACTION_START, -100L);
	}

	@Override
	protected void registerGoals() {
	}

	@Override
	public List<? extends ExtendedSensor<?>> getSensors(RomanLegionaryEchoEntity owner) {
		return List.of(new NearbyLivingEntitySensor<RomanLegionaryEchoEntity>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getAlwaysRunningBehaviours(RomanLegionaryEchoEntity owner) {
		return List.of(new MoveToWalkTarget<>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getIdleBehaviours(RomanLegionaryEchoEntity owner) {
		return List.of(new EchoFollowOwner());
	}

	@Override
	public List<? extends BehaviorControl<?>> getFightingBehaviours(RomanLegionaryEchoEntity owner) {
		return List.of(
				new InvalidateAttackTarget<RomanLegionaryEchoEntity>(),
				new SetWalkTargetToAttackTarget<RomanLegionaryEchoEntity>().speedModifier(1.15F).closeEnoughDist(1),
				new AnimatableMeleeAttack<RomanLegionaryEchoEntity>(6)
						.attackInterval(20)
						.canAttack((entity, target) -> entity.canAttack(target))
						.whenStarting(RomanLegionaryEchoEntity::startMeleeAttackAnimation)
		);
	}

	private void startMeleeAttackAnimation() {
		long now = this.level().getGameTime();
		this.attackAnimationUntil = now + ATTACK_ANIMATION_TICKS;
		this.triggerAnim(ACTION_CONTROLLER, ATTACK_TRIGGER);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		if (--this.remainingLifetime <= 0) {
			dismiss();
			return;
		}

		LivingEntity owner = this.getOwner();
		if (!(owner instanceof Player player) || !owner.isAlive() || owner.level() != this.level()) {
			dismiss();
			return;
		}

		boolean hasSummoner = player.getInventory().contains(stack -> TestEchoSummonerItem.hasSummoner(stack, this.summonerUuid));
		this.missingSummonerTicks = hasSummoner ? 0 : this.missingSummonerTicks + 1;
		if (this.missingSummonerTicks > SUMMONER_GRACE_TICKS) {
			dismiss();
			return;
		}

		if (this.tickCount % 5 == 0) {
			LivingEntity target = selectProtectiveTarget(owner);
			if (target != null && this.canAttack(target)) {
				BrainUtil.setTargetOfEntity(this, target);
			}
		}
		if (this.tickCount % 20 == 0 && this.getTarget() != null) {
			EchoExperienceSystem.markParticipation(this, this.getTarget());
		}

		tickVisualAwareness(serverLevel, owner);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) {
			if (this.mutualGazePlayerUuid != null && this.mutualGazeDistractionStartedAt < 0L
					&& !isCaughtReactionActive()) {
				tickMutualGazeBodyFacing();
			} else if (isCaughtExitActive()) {
				tickCaughtExitBodyFacing();
			} else {
				tickThreatBodyFacing();
			}
		}
	}

	private void tickVisualAwareness(ServerLevel level, LivingEntity owner) {
		long now = level.getGameTime();
		tickBlinkClock(now);

		if (this.forcedVisualUntil > now) {
			return;
		}

		if (this.entityData.get(VISUAL_REACTION_UNTIL) <= now && this.entityData.get(VISUAL_REACTION) != VISUAL_NORMAL) {
			this.entityData.set(VISUAL_REACTION, VISUAL_NORMAL);
			this.entityData.set(CURIOUS_TILT, (byte)0);
		}

		boolean combatSuppressed = isMutualGazeCombatSuppressed(owner);
		Player acquiredPlayer = this.mutualGazePlayerUuid == null && now >= this.caughtExitOwnerAvoidUntil
				? tickPlayerGazeAcquisition(level, owner, combatSuppressed, now)
				: null;
		boolean attentionScanTick = this.tickCount % 2 == 0;
		AttentionCandidate candidate = attentionScanTick ? findBestAttentionCandidate(level, owner, now) : null;
		if (now < this.caughtExitOwnerAvoidUntil && (combatSuppressed
				|| candidate != null && (isHardMutualGazeInterrupt(candidate) || isCaughtReactionInterrupt(candidate)))) {
			this.caughtExitOwnerAvoidUntil = -1L;
		}

		if (this.mutualGazePlayerUuid != null) {
			if (combatSuppressed || candidate != null && (isHardMutualGazeInterrupt(candidate)
					|| isCaughtReactionPendingOrActive() && isCaughtReactionInterrupt(candidate))) {
				endMutualGaze(now);
			} else if (tickMutualGaze(level, now, candidate, attentionScanTick)) {
				return;
			}
		}

		if (isCaughtExitActive()) {
			if (combatSuppressed || candidate != null && (isHardMutualGazeInterrupt(candidate)
					|| isCaughtReactionInterrupt(candidate))) {
				endCaughtExit(now, true);
			} else if (tickCaughtExit(level, owner, now)) {
				return;
			}
		}

		if (acquiredPlayer != null && now >= this.mutualGazeCooldownUntil
				&& !combatSuppressed && (candidate == null || !isHardMutualGazeInterrupt(candidate))) {
			beginMutualGaze(acquiredPlayer, now);
			return;
		}

		if (candidate == null) {
			return;
		}

		considerAttentionCandidate(candidate, now);
	}

	private @Nullable Player tickPlayerGazeAcquisition(ServerLevel level, LivingEntity owner, boolean combatSuppressed, long now) {
		if (combatSuppressed || now < this.mutualGazeCooldownUntil) {
			this.playerGazeProgress.clear();
			return null;
		}

		Set<UUID> presentPlayers = new HashSet<>();
		Player qualifiedOwner = null;
		Player longestGazePlayer = null;
		int longestGazeTicks = -1;

		for (Player player : level.players()) {
			if (!player.isAlive() || player.isSpectator()) {
				continue;
			}

			UUID playerUuid = player.getUUID();
			presentPlayers.add(playerUuid);
			PlayerGazeProgress progress = this.playerGazeProgress.computeIfAbsent(playerUuid, ignored -> new PlayerGazeProgress());
			GazeSample sample = samplePlayerHeadGaze(player);

			if (sample.state() == GazeState.VALID) {
				progress.validTicks++;
				progress.missedTicks = 0;
			} else if (sample.state() == GazeState.MISSED && progress.missedTicks < GAZE_MISS_TOLERANCE_TICKS) {
				progress.missedTicks++;
			} else {
				progress.reset();
			}

			if (progress.validTicks < requiredGazeTicks(sample.distance())) {
				continue;
			}

			if (player == owner) {
				qualifiedOwner = player;
			} else if (progress.validTicks > longestGazeTicks) {
				longestGazeTicks = progress.validTicks;
				longestGazePlayer = player;
			}
		}

		this.playerGazeProgress.keySet().removeIf(uuid -> !presentPlayers.contains(uuid));
		return qualifiedOwner != null ? qualifiedOwner : longestGazePlayer;
	}

	private GazeSample samplePlayerHeadGaze(Player player) {
		Vec3 playerEye = player.getEyePosition();
		Vec3 headCenter = getVisualHeadCenter();
		Vec3 towardHead = headCenter.subtract(playerEye);
		double distance = towardHead.length();
		if (distance < 0.1 || player.isInvisible() && distance > INVISIBLE_GAZE_RANGE || !hasClearViewFromPlayer(player, headCenter)) {
			return new GazeSample(GazeState.BLOCKED, distance);
		}

		Vec3 look = player.getLookAngle().normalize();
		double projection = look.dot(towardHead);
		if (projection <= 0.0) {
			return new GazeSample(GazeState.MISSED, distance);
		}

		double distanceFromRaySqr = Math.max(0.0, towardHead.lengthSqr() - projection * projection);
		return new GazeSample(distanceFromRaySqr <= HEAD_GAZE_RADIUS * HEAD_GAZE_RADIUS ? GazeState.VALID : GazeState.MISSED, distance);
	}

	private Vec3 getVisualHeadCenter() {
		return this.position().add(0.0, VISUAL_HEAD_CENTER_HEIGHT, 0.0);
	}

	private boolean hasClearViewFromPlayer(Player player, Vec3 headCenter) {
		return this.level().clip(new ClipContext(
				player.getEyePosition(),
				headCenter,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				player
		)).getType() == HitResult.Type.MISS;
	}

	private static int requiredGazeTicks(double distance) {
		return 10 + (int)Math.ceil(Math.max(0.0, distance - 12.0) / 2.0);
	}

	private boolean isMutualGazeCombatSuppressed(LivingEntity owner) {
		LivingEntity target = this.getTarget();
		return target != null && target.isAlive()
				|| isRecentWithin(this, this.getLastHurtByMobTimestamp(), COMBAT_GAZE_SUPPRESSION_TICKS)
				|| isRecentWithin(this, this.getLastHurtMobTimestamp(), COMBAT_GAZE_SUPPRESSION_TICKS)
				|| isRecentWithin(owner, owner.getLastHurtByMobTimestamp(), COMBAT_GAZE_SUPPRESSION_TICKS)
				|| isRecentWithin(owner, owner.getLastHurtMobTimestamp(), COMBAT_GAZE_SUPPRESSION_TICKS);
	}

	private void beginMutualGaze(Player player, long now) {
		LivingEntity owner = this.getOwner();
		boolean caughtEligible = player == owner
				&& now >= this.caughtReactionCooldownUntil
				&& this.attentionTarget == player
				&& this.eyeAttentionTarget == player
				&& this.headAttentionKind == AttentionKind.NORMAL
				&& this.eyeAttentionKind == AttentionKind.NORMAL
				&& now - this.attentionStartedAt >= CAUGHT_PREWATCH_TICKS;
		this.mutualGazePlayerUuid = player.getUUID();
		this.mutualGazeLastSeenPoint = player.getEyePosition();
		this.mutualGazeHoldTicksRemaining = 40 + this.random.nextInt(41);
		this.mutualGazeLostSightAt = -1L;
		this.mutualGazeBodyTurning = false;
		this.mutualGazeAligned = false;
		this.mutualGazeDistractionStartedAt = -1L;
		this.playerGazeProgress.clear();
		this.getNavigation().stop();
		applyAttention(new AttentionCandidate(player, this.mutualGazeLastSeenPoint, MUTUAL_GAZE_PRIORITY,
				VISUAL_MUTUAL_GAZE, 20 * 60, false, AttentionKind.MUTUAL_GAZE), now);
		this.caughtReactionScheduledAt = caughtEligible ? now + chooseCaughtReactionDelay() : -1L;
		this.caughtReactionStartedAt = -1L;
		this.caughtReactionGlanceEndAt = -1L;
		this.caughtReactionFinishAt = -1L;
		this.caughtReactionGazeMissTicks = 0;
	}

	private boolean tickMutualGaze(ServerLevel level, long now, @Nullable AttentionCandidate candidate, boolean attentionScanTick) {
		Player player = level.getPlayerByUUID(this.mutualGazePlayerUuid);
		if (player == null || !player.isAlive() || player.isSpectator()) {
			endMutualGaze(now);
			return false;
		}

		Vec3 headCenter = getVisualHeadCenter();
		double distance = player.getEyePosition().distanceTo(headCenter);
		boolean visible = (!player.isInvisible() || distance <= INVISIBLE_GAZE_RANGE) && hasClearViewFromPlayer(player, headCenter);
		if (isCaughtReactionActive() && (!visible || distance * distance > CAUGHT_MAX_OWNER_DISTANCE_SQR)) {
			endMutualGaze(now);
			return false;
		}
		if (visible) {
			this.mutualGazeLostSightAt = -1L;
			this.mutualGazeLastSeenPoint = player.getEyePosition();
			setAttentionPoint(this.mutualGazeLastSeenPoint);
		} else {
			if (this.mutualGazeLostSightAt < 0L) {
				this.mutualGazeLostSightAt = now;
			}
			if (now - this.mutualGazeLostSightAt > 10) {
				endMutualGaze(now);
				return false;
			}
			setAttentionPoint(this.mutualGazeLastSeenPoint);
		}

		this.getNavigation().stop();
		if (candidate != null && candidate.kind() == AttentionKind.CLOSE_CREEPER) {
			if (this.mutualGazeDistractionStartedAt < 0L) {
				this.mutualGazeDistractionStartedAt = now;
			}
			considerAttentionCandidate(candidate, now);
			if (now - this.mutualGazeDistractionStartedAt > 20) {
				endMutualGaze(now);
				return false;
			}
			return true;
		}

		if (this.mutualGazeDistractionStartedAt >= 0L && !attentionScanTick) {
			return true;
		}

		if (this.mutualGazeDistractionStartedAt >= 0L) {
			this.mutualGazeDistractionStartedAt = -1L;
			restoreMutualGazeAttention(player, now);
		} else {
			this.eyeAttentionTarget = player;
			this.attentionTarget = player;
			setEyeAttentionPoint(this.mutualGazeLastSeenPoint);
			setAttentionPoint(this.mutualGazeLastSeenPoint);
		}

		if (tickCaughtReaction(player, now)) {
			return true;
		}

		if (visible && this.mutualGazeAligned && --this.mutualGazeHoldTicksRemaining <= 0) {
			boolean stillLooking = samplePlayerHeadGaze(player).state() == GazeState.VALID;
			if (stillLooking && this.random.nextFloat() < 0.75F) {
				this.mutualGazeHoldTicksRemaining = 20 + this.random.nextInt(41);
			} else {
				endMutualGaze(now);
				if (stillLooking) {
					startMutualGazeGlanceAway(now);
				}
				return false;
			}
		}

		return true;
	}

	private boolean tickCaughtReaction(Player owner, long now) {
		if (this.caughtReactionScheduledAt >= 0L && !isCaughtReactionActive()) {
			GazeState gazeState = samplePlayerHeadGaze(owner).state();
			if (gazeState == GazeState.VALID) {
				this.caughtReactionGazeMissTicks = 0;
			} else if (gazeState == GazeState.MISSED && this.caughtReactionGazeMissTicks < GAZE_MISS_TOLERANCE_TICKS) {
				this.caughtReactionGazeMissTicks++;
				return true;
			} else {
				this.caughtReactionScheduledAt = -1L;
				this.caughtReactionGazeMissTicks = 0;
				return false;
			}
			if (now < this.caughtReactionScheduledAt) {
				return true;
			}
			startCaughtReaction(owner, now);
		}

		if (!isCaughtReactionActive()) {
			return false;
		}

		long elapsed = now - this.caughtReactionStartedAt;
		this.getNavigation().stop();
		setReaction(VISUAL_CAUGHT, now + 10);
		this.entityData.set(CURIOUS_TILT, (byte)0);

		if (elapsed < 10) {
			setEyeAttentionPoint(owner.getEyePosition());
			setAttentionPoint(owner.getEyePosition());
			return true;
		}

		setEyeAttentionPoint(this.caughtReactionAwayPoint);
		if (elapsed < 12) {
			setAttentionPoint(owner.getEyePosition());
			return true;
		}

		setAttentionPoint(this.caughtReactionAwayPoint);
		if (elapsed < CAUGHT_GLANCE_START_TICKS) {
			return true;
		}

		if (this.caughtReactionGlanceEndAt < 0L) {
			boolean ownerStillLooking = samplePlayerHeadGaze(owner).state() == GazeState.VALID;
			this.caughtReactionGlanceEndAt = now + (ownerStillLooking ? 4 + this.random.nextInt(4) : 8 + this.random.nextInt(4));
			this.caughtReactionFinishAt = this.caughtReactionGlanceEndAt + 5 + this.random.nextInt(3);
		}

		if (now < this.caughtReactionGlanceEndAt) {
			setEyeAttentionPoint(owner.getEyePosition());
			return true;
		}

		setEyeAttentionPoint(this.caughtReactionAwayPoint);
		if (now < this.caughtReactionFinishAt) {
			return true;
		}

		finishCaughtReaction(now);
		return false;
	}

	private void startCaughtReaction(Player owner, long now) {
		this.caughtReactionScheduledAt = -1L;
		this.caughtReactionStartedAt = now;
		this.caughtReactionGlanceEndAt = -1L;
		this.caughtReactionFinishAt = -1L;
		this.caughtReactionGazeMissTicks = 0;
		this.caughtReactionAwayPoint = createCaughtReactionAwayPoint(owner);
		this.entityData.set(CAUGHT_REACTION_START, now);
		this.entityData.set(BLINK_START, now + 3);
		this.entityData.set(BLINK_COUNT, (byte)2);
		setReaction(VISUAL_CAUGHT, now + 60);
		this.entityData.set(CURIOUS_TILT, (byte)0);
		this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
	}

	private Vec3 createCaughtReactionAwayPoint(Player owner) {
		float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
		float offset = 35.0F + this.random.nextFloat() * 20.0F;
		float targetYaw = ownerYaw + (this.random.nextBoolean() ? offset : -offset);
		float yawRadians = targetYaw * ((float)Math.PI / 180.0F);
		double distance = 6.0 + this.random.nextDouble() * 3.0;
		return new Vec3(
				this.getX() - Math.sin(yawRadians) * distance,
				this.getEyeY() + 0.5 + this.random.nextDouble(),
				this.getZ() + Math.cos(yawRadians) * distance
		);
	}

	private int chooseCaughtReactionDelay() {
		int roll = this.random.nextInt(100);
		if (roll < 30) {
			return 0;
		}
		if (roll < 75) {
			return 20 + this.random.nextInt(21);
		}
		if (roll < 95) {
			return 40 + this.random.nextInt(41);
		}
		return 80 + this.random.nextInt(41);
	}

	private boolean isCaughtReactionPendingOrActive() {
		return this.caughtReactionScheduledAt >= 0L || isCaughtReactionActive();
	}

	private boolean isCaughtReactionActive() {
		return this.caughtReactionStartedAt >= 0L;
	}

	private static boolean isCaughtReactionInterrupt(AttentionCandidate candidate) {
		return candidate.target() instanceof Creeper
				|| candidate.kind() == AttentionKind.APPROACHING;
	}

	private void finishCaughtReaction(long now) {
		Vec3 awayPoint = this.caughtReactionAwayPoint;
		endMutualGaze(now);
		LivingEntity owner = this.getOwner();
		if (this.level() instanceof ServerLevel serverLevel && owner != null) {
			startCaughtExit(serverLevel, owner, awayPoint, now);
		}
	}

	private void startCaughtExit(ServerLevel level, LivingEntity owner, Vec3 initialAwayPoint, long now) {
		int roll = this.random.nextInt(100);
		CaughtExitMode mode = roll < 15 ? CaughtExitMode.LOOK_AWAY : roll < 40 ? CaughtExitMode.TURN_AWAY : CaughtExitMode.WALK_AWAY;
		startCaughtExit(level, owner, initialAwayPoint, now, mode, this.random.nextFloat() < 0.75F);
	}

	private void startCaughtExit(ServerLevel level, LivingEntity owner, Vec3 initialAwayPoint, long now,
			CaughtExitMode mode, boolean secondaryPlanned) {
		this.caughtExitOwnerAvoidUntil = -1L;
		this.caughtExitMode = mode;
		this.caughtExitStartedAt = now;
		this.caughtExitEndsAt = now + 40 + this.random.nextInt(41);
		this.caughtExitWalkStartsAt = -1L;
		this.caughtExitWalkStarted = false;
		this.caughtExitWalkArrived = false;
		this.caughtExitSecondaryPlanned = secondaryPlanned;
		this.caughtExitSecondaryDone = false;
		this.caughtExitSecondaryScheduledAt = -1L;
		this.caughtExitSecondaryStartedAt = -1L;
		this.caughtExitSecondaryGlanceEndAt = -1L;
		this.caughtExitSecondaryReturnEndAt = -1L;
		this.caughtExitFocusTarget = null;
		this.caughtExitFocusPoint = initialAwayPoint;

		if (this.caughtExitMode == CaughtExitMode.WALK_AWAY && !chooseCaughtExitWalkTarget(owner)) {
			this.caughtExitMode = CaughtExitMode.TURN_AWAY;
		}

		if (this.caughtExitMode == CaughtExitMode.WALK_AWAY) {
			this.caughtExitFocusPoint = this.caughtExitWalkTarget.add(0.0, this.getEyeHeight(), 0.0);
			this.caughtExitWalkStartsAt = now + 5 + this.random.nextInt(6);
		} else {
			selectCaughtExitFocus(level, owner, initialAwayPoint);
			this.caughtExitSecondaryScheduledAt = this.caughtExitSecondaryPlanned
					? now + 16 + this.random.nextInt(15)
					: -1L;
		}

		this.caughtExitBodyTargetYaw = chooseCaughtExitBodyYaw(owner);
		applyCaughtExitFocus(now);
	}

	private boolean chooseCaughtExitWalkTarget(LivingEntity owner) {
		if (this.distanceToSqr(owner) > CAUGHT_EXIT_WALK_ELIGIBLE_OWNER_DISTANCE_SQR) {
			return false;
		}

		float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
		for (int attempt = 0; attempt < 8; attempt++) {
			float offset = CAUGHT_EXIT_MIN_OWNER_ANGLE
					+ this.random.nextFloat() * (CAUGHT_EXIT_MAX_WALK_ANGLE - CAUGHT_EXIT_MIN_OWNER_ANGLE);
			float targetYaw = ownerYaw + (this.random.nextBoolean() ? offset : -offset);
			float yawRadians = targetYaw * ((float)Math.PI / 180.0F);
			double distance = 1.5 + this.random.nextDouble() * 1.5;
			double targetX = this.getX() - Math.sin(yawRadians) * distance;
			double targetZ = this.getZ() + Math.cos(yawRadians) * distance;
			if (owner.distanceToSqr(targetX, this.getY(), targetZ) > CAUGHT_EXIT_MAX_OWNER_DISTANCE_SQR) {
				continue;
			}

			this.caughtExitWalkTarget = new Vec3(targetX, this.getY(), targetZ);
			return true;
		}

		return false;
	}

	private void selectCaughtExitFocus(ServerLevel level, LivingEntity owner, Vec3 fallbackPoint) {
		LivingEntity nearestCreature = null;
		Player nearestOtherPlayer = null;
		double creatureDistanceSqr = Double.MAX_VALUE;
		double playerDistanceSqr = Double.MAX_VALUE;
		for (LivingEntity entity : level.getEntitiesOfClass(
				LivingEntity.class,
				this.getBoundingBox().inflate(12.0),
				entity -> entity != this && entity != owner && entity.isAlive() && !entity.isSpectator()
						&& !(entity instanceof Monster) && this.hasLineOfSight(entity)
		)) {
			if (!isCaughtExitDirectionAwayFromOwner(owner, entity.getEyePosition())) {
				continue;
			}
			double distanceSqr = this.distanceToSqr(entity);
			if (entity instanceof Player player) {
				if (distanceSqr < playerDistanceSqr) {
					nearestOtherPlayer = player;
					playerDistanceSqr = distanceSqr;
				}
			} else if (distanceSqr < creatureDistanceSqr) {
				nearestCreature = entity;
				creatureDistanceSqr = distanceSqr;
			}
		}

		this.caughtExitFocusTarget = nearestCreature != null ? nearestCreature : nearestOtherPlayer;
		this.caughtExitFocusPoint = this.caughtExitFocusTarget == null
				? isCaughtExitDirectionAwayFromOwner(owner, fallbackPoint) ? fallbackPoint : createCaughtExitFallbackPoint(owner)
				: this.caughtExitFocusTarget.getEyePosition();
	}

	private boolean isCaughtExitDirectionAwayFromOwner(LivingEntity owner, Vec3 point) {
		float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
		float pointYaw = yawToward(this.getX(), this.getZ(), point.x, point.z);
		return Math.abs(net.minecraft.util.Mth.wrapDegrees(pointYaw - ownerYaw)) >= CAUGHT_EXIT_MIN_OWNER_ANGLE;
	}

	private float chooseCaughtExitBodyYaw(LivingEntity owner) {
		float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
		float focusYaw = yawToward(this.getX(), this.getZ(), this.caughtExitFocusPoint.x, this.caughtExitFocusPoint.z);
		float difference = net.minecraft.util.Mth.wrapDegrees(focusYaw - ownerYaw);
		float direction = Math.abs(difference) < 1.0F ? (this.random.nextBoolean() ? 1.0F : -1.0F) : Math.signum(difference);
		float offset = net.minecraft.util.Mth.clamp(Math.abs(difference), 45.0F,
				this.caughtExitMode == CaughtExitMode.WALK_AWAY ? CAUGHT_EXIT_MAX_WALK_ANGLE : 90.0F);
		return ownerYaw + direction * offset;
	}

	private boolean tickCaughtExit(ServerLevel level, LivingEntity owner, long now) {
		if (!isCaughtExitActive()) {
			return false;
		}
		if (this.distanceToSqr(owner) >= CAUGHT_EXIT_FOLLOW_CANCEL_DISTANCE_SQR) {
			endCaughtExit(now, true);
			return false;
		}

		if (this.caughtExitMode == CaughtExitMode.WALK_AWAY && !this.caughtExitWalkArrived) {
			if (now >= this.caughtExitEndsAt) {
				endCaughtExit(now, false);
				return true;
			}
			setEyeAttentionPoint(this.caughtExitWalkTarget.add(0.0, this.getEyeHeight(), 0.0));
			setAttentionPoint(this.caughtExitWalkTarget.add(0.0, this.getEyeHeight(), 0.0));
			if (!this.caughtExitWalkStarted && now >= this.caughtExitWalkStartsAt) {
				double speed = 0.715 + this.random.nextDouble() * 0.11;
				if (!this.getNavigation().moveTo(this.caughtExitWalkTarget.x, this.caughtExitWalkTarget.y,
						this.caughtExitWalkTarget.z, speed)) {
					fallbackCaughtExitWalkToTurn(level, owner, now);
					return true;
				}
				this.caughtExitWalkStarted = true;
				this.caughtExitSecondaryScheduledAt = this.caughtExitSecondaryPlanned
						? now + 16 + this.random.nextInt(15)
						: -1L;
			}
			tickCaughtExitSecondaryGlance(owner, now);
			boolean reachedWalkTarget = this.position().distanceToSqr(this.caughtExitWalkTarget) <= 0.75 * 0.75;
			if (this.caughtExitWalkStarted && reachedWalkTarget) {
				this.caughtExitWalkArrived = true;
				this.getNavigation().stop();
				selectCaughtExitFocus(level, owner, createCaughtExitFallbackPoint(owner));
				this.caughtExitBodyTargetYaw = chooseCaughtExitBodyYaw(owner);
				this.caughtExitEndsAt = Math.max(this.caughtExitEndsAt, now + 16 + this.random.nextInt(15));
				applyCaughtExitFocus(now);
			} else if (this.caughtExitWalkStarted && this.getNavigation().isDone()) {
				fallbackCaughtExitWalkToTurn(level, owner, now);
			}
			return true;
		}

		if (this.caughtExitFocusTarget != null) {
			if (this.caughtExitFocusTarget.isAlive() && this.hasLineOfSight(this.caughtExitFocusTarget)) {
				this.caughtExitFocusPoint = this.caughtExitFocusTarget.getEyePosition();
			} else {
				this.caughtExitFocusTarget = null;
				this.caughtExitFocusPoint = createCaughtExitFallbackPoint(owner);
				this.caughtExitBodyTargetYaw = chooseCaughtExitBodyYaw(owner);
			}
		}

		tickCaughtExitSecondaryGlance(owner, now);
		if (now >= this.caughtExitEndsAt && (this.caughtExitSecondaryStartedAt < 0L
				|| now >= this.caughtExitSecondaryReturnEndAt)) {
			endCaughtExit(now, false);
			return true;
		}

		if (this.caughtExitSecondaryStartedAt < 0L || now >= this.caughtExitSecondaryReturnEndAt) {
			applyCaughtExitFocus(now);
		}
		return true;
	}

	private void fallbackCaughtExitWalkToTurn(ServerLevel level, LivingEntity owner, long now) {
		this.getNavigation().stop();
		this.caughtExitMode = CaughtExitMode.TURN_AWAY;
		selectCaughtExitFocus(level, owner, createCaughtExitFallbackPoint(owner));
		this.caughtExitBodyTargetYaw = chooseCaughtExitBodyYaw(owner);
		if (this.caughtExitSecondaryPlanned && this.caughtExitSecondaryScheduledAt < 0L) {
			this.caughtExitSecondaryScheduledAt = now + 16 + this.random.nextInt(15);
		}
		applyCaughtExitFocus(now);
	}

	private void tickCaughtExitSecondaryGlance(LivingEntity owner, long now) {
		if (!this.caughtExitSecondaryPlanned || this.caughtExitSecondaryDone
				|| this.caughtExitSecondaryScheduledAt < 0L || now < this.caughtExitSecondaryScheduledAt) {
			return;
		}

		if (this.caughtExitSecondaryStartedAt < 0L) {
			this.caughtExitSecondaryStartedAt = now;
			this.caughtExitSecondaryOwnerStillLooking = owner instanceof Player player
					&& samplePlayerHeadGaze(player).state() == GazeState.VALID;
			this.caughtExitSecondaryGlanceEndAt = now + (this.caughtExitSecondaryOwnerStillLooking
					? 3 + this.random.nextInt(3)
					: 7 + this.random.nextInt(5));
			this.caughtExitSecondaryReturnEndAt = this.caughtExitSecondaryGlanceEndAt + 4;
			this.caughtExitSecondaryHeadPoint = createSecondaryGlanceHeadPoint(owner);
		}

		if (now < this.caughtExitSecondaryGlanceEndAt) {
			setEyeAttentionPoint(owner.getEyePosition());
			setAttentionPoint(now - this.caughtExitSecondaryStartedAt >= 2
					? this.caughtExitSecondaryHeadPoint
					: this.caughtExitFocusPoint);
			return;
		}

		setEyeAttentionPoint(this.caughtExitFocusPoint);
		setAttentionPoint(this.caughtExitFocusPoint);
		if (now >= this.caughtExitSecondaryReturnEndAt) {
			this.caughtExitSecondaryDone = true;
			if (this.caughtExitSecondaryOwnerStillLooking) {
				this.caughtExitEndsAt = Math.max(this.caughtExitEndsAt, now + 20 + this.random.nextInt(21));
			}
		}
	}

	private Vec3 createSecondaryGlanceHeadPoint(LivingEntity owner) {
		float focusYaw = yawToward(this.getX(), this.getZ(), this.caughtExitFocusPoint.x, this.caughtExitFocusPoint.z);
		float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
		float difference = net.minecraft.util.Mth.wrapDegrees(ownerYaw - focusYaw);
		float turn = Math.signum(difference) * Math.min(Math.abs(difference), 8.0F + this.random.nextFloat() * 7.0F);
		float yawRadians = (focusYaw + turn) * ((float)Math.PI / 180.0F);
		double distance = 8.0;
		return new Vec3(
				this.getX() - Math.sin(yawRadians) * distance,
				this.caughtExitFocusPoint.y,
				this.getZ() + Math.cos(yawRadians) * distance
		);
	}

	private Vec3 createCaughtExitFallbackPoint(LivingEntity owner) {
		float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
		float offset = CAUGHT_EXIT_MIN_OWNER_ANGLE
				+ this.random.nextFloat() * (CAUGHT_EXIT_MAX_WALK_ANGLE - CAUGHT_EXIT_MIN_OWNER_ANGLE);
		float yawRadians = (ownerYaw + (this.random.nextBoolean() ? offset : -offset)) * ((float)Math.PI / 180.0F);
		double distance = 6.0 + this.random.nextDouble() * 3.0;
		return new Vec3(
				this.getX() - Math.sin(yawRadians) * distance,
				this.getEyeY() + this.random.nextDouble(),
				this.getZ() + Math.cos(yawRadians) * distance
		);
	}

	private void applyCaughtExitFocus(long now) {
		this.eyeAttentionTarget = this.caughtExitFocusTarget;
		this.attentionTarget = this.caughtExitFocusTarget;
		this.eyeAttentionKind = AttentionKind.NORMAL;
		this.headAttentionKind = AttentionKind.NORMAL;
		this.eyeAttentionPriority = 240;
		this.attentionPriority = 240;
		this.eyeAttentionExpiresAt = this.caughtExitEndsAt + 20;
		this.attentionExpiresAt = this.caughtExitEndsAt + 20;
		setEyeAttentionPoint(this.caughtExitFocusPoint);
		setAttentionPoint(this.caughtExitFocusPoint);
		setReaction(VISUAL_NORMAL, now + 20);
	}

	private void tickCaughtExitBodyFacing() {
		if (this.caughtExitMode != CaughtExitMode.TURN_AWAY
				&& !(this.caughtExitMode == CaughtExitMode.WALK_AWAY
				&& (!this.caughtExitWalkStarted || this.caughtExitWalkArrived))) {
			return;
		}
		float yawRadians = this.caughtExitBodyTargetYaw * ((float)Math.PI / 180.0F);
		Vec3 target = new Vec3(this.getX() - Math.sin(yawRadians) * 6.0, this.getEyeY(), this.getZ() + Math.cos(yawRadians) * 6.0);
		turnBodyToward(target, 5.0F);
	}

	private void endCaughtExit(long now, boolean interrupted) {
		if (!isCaughtExitActive()) {
			return;
		}
		if (this.caughtExitMode == CaughtExitMode.WALK_AWAY) {
			this.getNavigation().stop();
		}
		Vec3 finalFocus = this.caughtExitFocusPoint;
		this.caughtExitMode = CaughtExitMode.NONE;
		this.caughtExitStartedAt = -1L;
		this.caughtExitEndsAt = -1L;
		this.caughtExitFocusTarget = null;
		this.caughtExitWalkStartsAt = -1L;
		this.caughtExitWalkStarted = false;
		this.caughtExitWalkArrived = false;
		this.caughtExitSecondaryPlanned = false;
		this.caughtExitSecondaryDone = false;
		this.caughtExitSecondaryScheduledAt = -1L;
		this.caughtExitSecondaryStartedAt = -1L;
		this.caughtExitSecondaryGlanceEndAt = -1L;
		this.caughtExitSecondaryReturnEndAt = -1L;
		if (!interrupted) {
			this.caughtReactionCooldownUntil = now + 160 + this.random.nextInt(141);
			this.mutualGazeCooldownUntil = Math.max(this.mutualGazeCooldownUntil, this.caughtReactionCooldownUntil);
			this.caughtExitOwnerAvoidUntil = now + 80 + this.random.nextInt(61);
			this.caughtExitOwnerAvoidPoint = finalFocus;
			applyAttention(new AttentionCandidate(null, finalFocus, 235, VISUAL_NORMAL,
					35 + this.random.nextInt(36), false, AttentionKind.NORMAL), now);
		}
	}

	private boolean isCaughtExitActive() {
		return this.caughtExitMode != CaughtExitMode.NONE;
	}

	private void restoreMutualGazeAttention(Player player, long now) {
		this.eyeAttentionTarget = player;
		this.eyeAttentionPriority = MUTUAL_GAZE_PRIORITY;
		this.eyeAttentionExpiresAt = now + 20 * 60;
		this.eyeAttentionKind = AttentionKind.MUTUAL_GAZE;
		this.eyeStickyUntil = now + EYE_STICKY_TICKS;
		this.attentionTarget = player;
		this.attentionPriority = MUTUAL_GAZE_PRIORITY;
		this.attentionExpiresAt = now + 20 * 60;
		this.headAttentionKind = AttentionKind.MUTUAL_GAZE;
		this.headStickyUntil = now + HEAD_STICKY_TICKS;
		setEyeAttentionPoint(this.mutualGazeLastSeenPoint);
		setAttentionPoint(this.mutualGazeLastSeenPoint);
		setReaction(VISUAL_MUTUAL_GAZE, now + 20 * 60);
		this.entityData.set(CURIOUS_TILT, (byte)0);
		this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
	}

	private void tickMutualGazeBodyFacing() {
		long now = this.level().getGameTime();
		if (now - this.attentionStartedAt < 4) {
			return;
		}

		float desiredYaw = yawToward(this.getX(), this.getZ(), this.mutualGazeLastSeenPoint.x, this.mutualGazeLastSeenPoint.z);
		float yawDifference = Math.abs(net.minecraft.util.Mth.wrapDegrees(desiredYaw - this.yBodyRot));
		if (!this.mutualGazeBodyTurning && yawDifference > 5.0F) {
			this.mutualGazeBodyTurning = true;
		}
		if (!this.mutualGazeBodyTurning) {
			this.mutualGazeAligned = true;
			return;
		}
		if (yawDifference <= 5.0F) {
			this.mutualGazeBodyTurning = false;
			this.mutualGazeAligned = true;
			return;
		}

		this.mutualGazeAligned = false;
		turnBodyToward(this.mutualGazeLastSeenPoint, 8.0F);
	}

	private void tickThreatBodyFacing() {
		long now = this.level().getGameTime();
		LivingEntity target = this.bodyAttentionTarget;
		if (target == null || !target.isAlive() || now >= this.bodyAttentionExpiresAt || isActivelyFighting()) {
			return;
		}

		double distance = this.distanceTo(target);
		float desiredYaw = yawToward(this.getX(), this.getZ(), target.getX(), target.getZ());
		float yawDifference = Math.abs(net.minecraft.util.Mth.wrapDegrees(desiredYaw - this.yBodyRot));
		boolean shouldTurn = switch (this.bodyAttentionKind) {
			case PRIMED_CREEPER -> distance <= 6.0 || yawDifference > 75.0F && now - this.bodyAttentionStartedAt >= 6;
			case DAMAGE_SOURCE -> now - this.bodyAttentionStartedAt >= 3;
			case CLOSE_CREEPER -> distance <= 4.0 || approachingSpeed(target) > 0.22;
			case APPROACHING -> willApproachWithin(target, 3.0, 10.0);
			default -> false;
		};
		if (shouldTurn) {
			turnBodyToward(target.getEyePosition(), 10.0F);
		}
	}

	private boolean isActivelyFighting() {
		LivingEntity target = this.getTarget();
		return target != null && target.isAlive();
	}

	private boolean willApproachWithin(LivingEntity target, double desiredDistance, double withinTicks) {
		double closingSpeed = approachingSpeed(target);
		double distanceToClose = Math.max(0.0, this.distanceTo(target) - desiredDistance);
		return closingSpeed > 1.0E-4 && distanceToClose / closingSpeed <= withinTicks;
	}

	private void startMutualGazeGlanceAway(long now) {
		this.mutualGazeCooldownUntil = now + 10 + this.random.nextInt(21);
		float offset = 35.0F + this.random.nextFloat() * 65.0F;
		float yaw = (this.yBodyRot + (this.random.nextBoolean() ? offset : -offset)) * ((float)Math.PI / 180.0F);
		double distance = 4.0 + this.random.nextDouble() * 3.0;
		Vec3 point = new Vec3(
				this.getX() - Math.sin(yaw) * distance,
				this.getEyeY() + this.random.nextDouble() * 1.5 - 0.5,
				this.getZ() + Math.cos(yaw) * distance
		);
		applyAttention(new AttentionCandidate(null, point, 235, VISUAL_NORMAL,
				(int)(this.mutualGazeCooldownUntil - now), false, AttentionKind.NORMAL), now);
	}

	private void endMutualGaze(long now) {
		boolean caughtReactionWasActive = isCaughtReactionActive();
		this.caughtReactionScheduledAt = -1L;
		this.caughtReactionStartedAt = -1L;
		this.caughtReactionGlanceEndAt = -1L;
		this.caughtReactionFinishAt = -1L;
		this.caughtReactionGazeMissTicks = 0;
		this.entityData.set(CAUGHT_REACTION_START, -100L);
		if (caughtReactionWasActive) {
			this.caughtReactionCooldownUntil = now + 160 + this.random.nextInt(141);
			this.mutualGazeCooldownUntil = Math.max(this.mutualGazeCooldownUntil, this.caughtReactionCooldownUntil);
		}
		this.mutualGazePlayerUuid = null;
		this.mutualGazeHoldTicksRemaining = 0;
		this.mutualGazeLostSightAt = -1L;
		this.mutualGazeBodyTurning = false;
		this.mutualGazeAligned = false;
		this.mutualGazeDistractionStartedAt = -1L;
		this.playerGazeProgress.clear();
		this.attentionTarget = null;
		this.attentionPriority = 0;
		this.attentionExpiresAt = now;
		this.headAttentionKind = AttentionKind.NORMAL;
		this.eyeAttentionTarget = null;
		this.eyeAttentionPriority = 0;
		this.eyeAttentionExpiresAt = now;
		this.eyeAttentionKind = AttentionKind.NORMAL;
		if (this.entityData.get(VISUAL_REACTION) == VISUAL_MUTUAL_GAZE
				|| this.entityData.get(VISUAL_REACTION) == VISUAL_CAUGHT) {
			setReaction(VISUAL_NORMAL, now);
			this.entityData.set(CURIOUS_TILT, (byte)0);
		}
	}

	private static boolean isHardMutualGazeInterrupt(AttentionCandidate candidate) {
		return candidate.kind() == AttentionKind.PRIMED_CREEPER
				|| candidate.kind() == AttentionKind.DAMAGE_SOURCE
				|| candidate.kind() == AttentionKind.COMBAT_TARGET;
	}

	private void tickBlinkClock(long now) {
		if (this.nextBlinkAt == 0L) {
			this.nextBlinkAt = now + 50 + this.random.nextInt(71);
		}
		if (now < this.nextBlinkAt || this.entityData.get(VISUAL_REACTION) == VISUAL_STARTLED
				|| this.entityData.get(VISUAL_REACTION) == VISUAL_HURT
				|| this.entityData.get(VISUAL_REACTION) == VISUAL_CAUGHT) {
			return;
		}

		this.entityData.set(BLINK_START, now);
		this.entityData.set(BLINK_COUNT, this.random.nextFloat() < 0.1F ? (byte)2 : (byte)1);
		this.nextBlinkAt = now + 50 + this.random.nextInt(71);
	}

	private AttentionCandidate findBestAttentionCandidate(ServerLevel level, LivingEntity owner, long now) {
		if (now < this.caughtExitOwnerAvoidUntil
				&& !isCaughtExitDirectionAwayFromOwner(owner, this.caughtExitOwnerAvoidPoint)) {
			this.caughtExitOwnerAvoidPoint = createCaughtExitFallbackPoint(owner);
		}
		LivingEntity combatTarget = this.getTarget();
		AttentionCandidate best = isVisibleAttentionTarget(combatTarget)
				? new AttentionCandidate(combatTarget, combatTarget.getEyePosition(), 800, VISUAL_ALERT, 30, false, AttentionKind.COMBAT_TARGET)
				: now < this.caughtExitOwnerAvoidUntil
						? new AttentionCandidate(null, this.caughtExitOwnerAvoidPoint, 220, VISUAL_NORMAL,
								35 + this.random.nextInt(36), false, AttentionKind.NORMAL)
						: new AttentionCandidate(owner, owner.getEyePosition(), 220, VISUAL_NORMAL,
								35 + this.random.nextInt(36), false, AttentionKind.NORMAL);

		LivingEntity attacker = this.getLastHurtByMob();
		if (isRecentWithin(this, this.getLastHurtByMobTimestamp(), 20) && isVisibleAttentionTarget(attacker)) {
			best = new AttentionCandidate(attacker, attacker.getEyePosition(), 1100, VISUAL_HURT, 16, false, AttentionKind.DAMAGE_SOURCE);
		}

		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isRecentWithin(owner, owner.getLastHurtByMobTimestamp(), 20) && isVisibleAttentionTarget(ownerAttacker) && best.priority() < 1050) {
			best = new AttentionCandidate(ownerAttacker, ownerAttacker.getEyePosition(), 1050, VISUAL_ALERT, 24, false, AttentionKind.DAMAGE_SOURCE);
		}

		List<LivingEntity> nearby = level.getEntitiesOfClass(
				LivingEntity.class,
				this.getBoundingBox().inflate(12.0),
				entity -> entity != this && entity.isAlive() && !entity.isSpectator() && this.hasLineOfSight(entity)
		);
		for (LivingEntity entity : nearby) {
			if (entity == owner && now < this.caughtExitOwnerAvoidUntil) {
				continue;
			}
			double distanceSqr = this.distanceToSqr(entity);
			int score;
			byte reaction = VISUAL_NORMAL;
			int duration = 30 + this.random.nextInt(51);

			if (entity instanceof Creeper creeper) {
				boolean primed = creeper.getSwellDir() > 0 || creeper.isIgnited();
				score = primed ? 1200 : distanceSqr <= 64.0 ? 900 : 540;
				reaction = primed || distanceSqr <= 64.0 ? VISUAL_STARTLED : VISUAL_ALERT;
				duration = primed ? 30 : 22;
				AttentionKind kind = primed ? AttentionKind.PRIMED_CREEPER
						: distanceSqr <= 64.0 ? AttentionKind.CLOSE_CREEPER : AttentionKind.NORMAL;
				if (score > best.priority()) {
					best = new AttentionCandidate(entity, entity.getEyePosition(), score, reaction, duration, false, kind);
				}
				continue;
			} else {
				double closingSpeed = approachingSpeed(entity);
				if (distanceSqr <= 100.0 && closingSpeed > 0.22) {
					score = 620 + (int)(closingSpeed * 250.0);
					reaction = VISUAL_STARTLED;
					duration = 18;
				} else if (entity == owner) {
					score = 220;
				} else if (entity instanceof Player) {
					score = 170;
				} else {
					score = 115;
				}
				score -= (int)(Math.sqrt(distanceSqr) * 4.0);
				score += this.random.nextInt(35);
			}

			if (score > best.priority()) {
				boolean curious = reaction == VISUAL_NORMAL && this.isInSafeIdleState() && this.random.nextFloat() < 0.1F;
				AttentionKind kind = reaction == VISUAL_STARTLED ? AttentionKind.APPROACHING : AttentionKind.NORMAL;
				best = new AttentionCandidate(entity, entity.getEyePosition(), score, curious ? VISUAL_CURIOUS : reaction, duration, curious, kind);
			}
		}

		if (now >= this.caughtExitOwnerAvoidUntil && best.priority() <= 220 && this.random.nextFloat() < 0.3F) {
			float yaw = (this.getYRot() + this.random.nextFloat() * 130.0F - 65.0F) * ((float)Math.PI / 180.0F);
			double distance = 4.0 + this.random.nextDouble() * 5.0;
			Vec3 point = new Vec3(
					this.getX() - Math.sin(yaw) * distance,
					this.getEyeY() + this.random.nextDouble() * 3.0 - 1.0,
					this.getZ() + Math.cos(yaw) * distance
			);
			boolean curious = this.isInSafeIdleState() && this.random.nextFloat() < 0.1F;
			return new AttentionCandidate(null, point, 230, curious ? VISUAL_CURIOUS : VISUAL_NORMAL,
					30 + this.random.nextInt(51), curious, AttentionKind.NORMAL);
		}

		return best;
	}

	private void considerAttentionCandidate(AttentionCandidate candidate, long now) {
		boolean urgent = isUrgentVisual(candidate.kind());
		boolean sameEyeTarget = candidate.target() != null
				&& sameTarget(this.eyeAttentionTarget, candidate.target())
				&& this.eyeAttentionKind == candidate.kind();
		boolean eyeExpired = now >= this.eyeAttentionExpiresAt || this.eyeAttentionTarget != null && !this.eyeAttentionTarget.isAlive();
		boolean switchEyes = urgent || eyeExpired || candidate.priority() >= this.eyeAttentionPriority + 80;
		if (switchEyes) {
			this.eyeAttentionTarget = candidate.target();
			this.eyeAttentionPriority = candidate.priority();
			this.eyeAttentionExpiresAt = now + candidate.durationTicks();
			this.eyeAttentionKind = candidate.kind();
			this.eyeStickyUntil = now + EYE_STICKY_TICKS;
			setEyeAttentionPoint(candidate.point());
			setReaction(candidate.reaction(), now + candidate.durationTicks());
			this.entityData.set(CURIOUS_TILT, (byte)0);
			this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
		} else if (sameEyeTarget) {
			setEyeAttentionPoint(candidate.target().getEyePosition());
		}

		boolean samePending = sameTarget(this.pendingHeadTarget, candidate.target()) && this.pendingHeadKind == candidate.kind();
		if (!samePending) {
			this.pendingHeadTarget = candidate.target();
			this.pendingHeadKind = candidate.kind();
			this.pendingHeadSince = now;
		}

		boolean sameHeadTarget = candidate.target() != null
				&& sameTarget(this.attentionTarget, candidate.target())
				&& this.headAttentionKind == candidate.kind();
		boolean headExpired = now >= this.attentionExpiresAt || this.attentionTarget != null && !this.attentionTarget.isAlive();
		boolean delayComplete = now - this.pendingHeadSince >= headDelayTicks(candidate.kind());
		boolean switchHead = delayComplete && (urgent || headExpired || candidate.priority() >= this.attentionPriority + 80);
		if (switchHead) {
			this.attentionTarget = candidate.target();
			this.attentionPoint = candidate.point();
			this.attentionPriority = candidate.priority();
			this.attentionStartedAt = now;
			this.attentionExpiresAt = now + candidate.durationTicks();
			this.headAttentionKind = candidate.kind();
			this.headStickyUntil = now + HEAD_STICKY_TICKS;
			this.entityData.set(ATTENTION_STARTED_AT, now);
			setAttentionPoint(candidate.point());
			this.entityData.set(CURIOUS_TILT, candidate.curious() ? (byte)(this.random.nextBoolean() ? 1 : -1) : (byte)0);
			this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
		} else if (sameHeadTarget && this.attentionTarget.isAlive()) {
			setAttentionPoint(this.attentionTarget.getEyePosition());
		}

		configureBodyAttention(candidate, now);
	}

	private static boolean sameTarget(@Nullable LivingEntity first, @Nullable LivingEntity second) {
		return first == second;
	}

	private static boolean isUrgentVisual(AttentionKind kind) {
		return kind == AttentionKind.PRIMED_CREEPER || kind == AttentionKind.DAMAGE_SOURCE;
	}

	private static int headDelayTicks(AttentionKind kind) {
		return switch (kind) {
			case CLOSE_CREEPER -> 4;
			case APPROACHING -> 2;
			default -> 0;
		};
	}

	private void configureBodyAttention(AttentionCandidate candidate, long now) {
		if (candidate.target() == null || candidate.kind() != AttentionKind.PRIMED_CREEPER
				&& candidate.kind() != AttentionKind.DAMAGE_SOURCE
				&& candidate.kind() != AttentionKind.CLOSE_CREEPER
				&& candidate.kind() != AttentionKind.APPROACHING) {
			return;
		}

		if (this.bodyAttentionTarget != candidate.target() || this.bodyAttentionKind != candidate.kind()) {
			this.bodyAttentionStartedAt = now;
		}
		this.bodyAttentionTarget = candidate.target();
		this.bodyAttentionKind = candidate.kind();
		this.bodyAttentionExpiresAt = now + candidate.durationTicks();
	}

	private boolean isVisibleAttentionTarget(@Nullable LivingEntity target) {
		return target != null && target.isAlive() && target.level() == this.level() && this.distanceToSqr(target) <= 32.0 * 32.0 && this.hasLineOfSight(target);
	}

	private double approachingSpeed(LivingEntity target) {
		Vec3 towardEcho = this.position().subtract(target.position());
		return towardEcho.lengthSqr() < 1.0E-4 ? 0.0 : target.getDeltaMovement().dot(towardEcho.normalize());
	}

	private boolean isInSafeIdleState() {
		return this.getTarget() == null
				&& !isRecent(this, this.getLastHurtByMobTimestamp())
				&& this.getNavigation().isDone();
	}

	private void applyAttention(AttentionCandidate candidate, long now) {
		this.eyeAttentionTarget = candidate.target();
		this.eyeAttentionPoint = candidate.point();
		this.eyeAttentionPriority = candidate.priority();
		this.eyeAttentionExpiresAt = now + candidate.durationTicks();
		this.eyeAttentionKind = candidate.kind();
		this.eyeStickyUntil = now + EYE_STICKY_TICKS;
		this.attentionTarget = candidate.target();
		this.attentionPoint = candidate.point();
		this.attentionPriority = candidate.priority();
		this.attentionStartedAt = now;
		this.entityData.set(ATTENTION_STARTED_AT, now);
		this.attentionExpiresAt = now + candidate.durationTicks();
		this.headAttentionKind = candidate.kind();
		this.headStickyUntil = now + HEAD_STICKY_TICKS;
		this.pendingHeadTarget = candidate.target();
		this.pendingHeadKind = candidate.kind();
		this.pendingHeadSince = now;
		setEyeAttentionPoint(candidate.point());
		setAttentionPoint(candidate.point());
		setReaction(candidate.reaction(), now + candidate.durationTicks());
		this.entityData.set(CURIOUS_TILT, candidate.curious() ? (byte)(this.random.nextBoolean() ? 1 : -1) : (byte)0);
		this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
		configureBodyAttention(candidate, now);
	}

	private void setAttentionPoint(Vec3 point) {
		this.attentionPoint = point;
		this.entityData.set(ATTENTION_X, (float)point.x);
		this.entityData.set(ATTENTION_Y, (float)point.y);
		this.entityData.set(ATTENTION_Z, (float)point.z);
	}

	private void setEyeAttentionPoint(Vec3 point) {
		this.eyeAttentionPoint = point;
		this.entityData.set(EYE_ATTENTION_X, (float)point.x);
		this.entityData.set(EYE_ATTENTION_Y, (float)point.y);
		this.entityData.set(EYE_ATTENTION_Z, (float)point.z);
	}

	private void setReaction(byte reaction, long until) {
		this.entityData.set(VISUAL_REACTION, reaction);
		this.entityData.set(VISUAL_REACTION_UNTIL, until);
	}

	private void turnBodyToward(Vec3 point, float maxDegrees) {
		float desiredYaw = yawToward(this.getX(), this.getZ(), point.x, point.z);
		float delta = net.minecraft.util.Mth.wrapDegrees(desiredYaw - this.yBodyRot);
		this.yBodyRot += net.minecraft.util.Mth.clamp(delta, -maxDegrees, maxDegrees);
		this.setYRot(this.yBodyRot);
	}

	public void forceVisualState(VisualTestMode mode) {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		long now = level.getGameTime();
		this.forcedVisualUntil = now + 60;
		switch (mode) {
			case BLINK -> {
				this.entityData.set(BLINK_START, now);
				this.entityData.set(BLINK_COUNT, (byte)1);
				this.forcedVisualUntil = now + 8;
			}
			case DOUBLE_BLINK -> {
				this.entityData.set(BLINK_START, now);
				this.entityData.set(BLINK_COUNT, (byte)2);
				this.forcedVisualUntil = now + 12;
			}
			case CURIOUS -> {
				LivingEntity owner = this.getOwner();
				Vec3 point = owner == null ? this.position().add(0, 1.5, 4) : owner.getEyePosition();
				setEyeAttentionPoint(point);
				setAttentionPoint(point);
				setReaction(VISUAL_CURIOUS, now + 60);
				this.entityData.set(CURIOUS_TILT, (byte)(this.random.nextBoolean() ? 1 : -1));
			}
			case STARTLED -> {
				LivingEntity owner = this.getOwner();
				Vec3 point = owner == null ? this.position().add(0, 1.5, 4) : owner.getEyePosition();
				setEyeAttentionPoint(point);
				setAttentionPoint(point);
				setReaction(VISUAL_STARTLED, now + 60);
				this.entityData.set(CURIOUS_TILT, (byte)0);
			}
			case EXIT_LOOK, EXIT_TURN, EXIT_WALK, EXIT_SECONDARY -> {
				LivingEntity owner = this.getOwner();
				if (owner == null) {
					return;
				}
				this.forcedVisualUntil = 0L;
				if (this.mutualGazePlayerUuid != null) {
					endMutualGaze(now);
				}
				endCaughtExit(now, true);
				CaughtExitMode exitMode = switch (mode) {
					case EXIT_LOOK -> CaughtExitMode.LOOK_AWAY;
					case EXIT_WALK -> CaughtExitMode.WALK_AWAY;
					default -> CaughtExitMode.TURN_AWAY;
				};
				startCaughtExit(level, owner, createCaughtExitFallbackPoint(owner), now,
						exitMode, mode == VisualTestMode.EXIT_SECONDARY);
			}
			case RESET -> {
				this.forcedVisualUntil = 0L;
				if (this.mutualGazePlayerUuid != null) {
					endMutualGaze(now);
				}
				endCaughtExit(now, true);
				this.eyeAttentionTarget = null;
				this.eyeAttentionExpiresAt = 0L;
				this.eyeAttentionPriority = 0;
				this.eyeAttentionKind = AttentionKind.NORMAL;
				this.attentionTarget = null;
				this.attentionExpiresAt = 0L;
				this.attentionPriority = 0;
				this.headAttentionKind = AttentionKind.NORMAL;
				this.bodyAttentionTarget = null;
				this.bodyAttentionExpiresAt = 0L;
				this.bodyAttentionKind = AttentionKind.NORMAL;
				setReaction(VISUAL_NORMAL, 0L);
				this.entityData.set(CURIOUS_TILT, (byte)0);
			}
		}
		this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
	}

	public Vec3 getSyncedAttentionPoint() {
		return new Vec3(this.entityData.get(ATTENTION_X), this.entityData.get(ATTENTION_Y), this.entityData.get(ATTENTION_Z));
	}

	public Vec3 getSyncedEyeAttentionPoint() {
		return new Vec3(this.entityData.get(EYE_ATTENTION_X), this.entityData.get(EYE_ATTENTION_Y), this.entityData.get(EYE_ATTENTION_Z));
	}

	public byte getVisualReaction() {
		return this.entityData.get(VISUAL_REACTION);
	}

	public long getVisualReactionUntil() {
		return this.entityData.get(VISUAL_REACTION_UNTIL);
	}

	public long getAttentionStartedAt() {
		return this.entityData.get(ATTENTION_STARTED_AT);
	}

	public long getCaughtReactionStart() {
		return this.entityData.get(CAUGHT_REACTION_START);
	}

	public long getBlinkStart() {
		return this.entityData.get(BLINK_START);
	}

	public byte getBlinkCount() {
		return this.entityData.get(BLINK_COUNT);
	}

	public byte getCuriousTilt() {
		return this.entityData.get(CURIOUS_TILT);
	}

	public int getVisualSequence() {
		return this.entityData.get(VISUAL_SEQUENCE);
	}

	public String describeGazeDebug(Player observer) {
		GazeSample sample = samplePlayerHeadGaze(observer);
		PlayerGazeProgress progress = this.playerGazeProgress.get(observer.getUUID());
		int validTicks = progress == null ? 0 : progress.validTicks;
		int missedTicks = progress == null ? 0 : progress.missedTicks;
		LivingEntity owner = this.getOwner();
		boolean combatSuppressed = owner != null && isMutualGazeCombatSuppressed(owner);
		return String.format(
				Locale.ROOT,
				"sample=%s distance=%.2f progress=%d/%d missed=%d combat=%s mutual=%s caughtPending=%s caughtActive=%s exit=%s secondary=%s avoid=%d distracted=%s aligned=%s hold=%d reaction=%d eye=%s head=%s body=%s bodyYaw=%.1f",
				sample.state().name().toLowerCase(Locale.ROOT),
				sample.distance(),
				validTicks,
				requiredGazeTicks(sample.distance()),
				missedTicks,
				combatSuppressed,
				this.mutualGazePlayerUuid != null,
				this.caughtReactionScheduledAt >= 0L,
				isCaughtReactionActive(),
				this.caughtExitMode.name().toLowerCase(Locale.ROOT),
				this.caughtExitSecondaryStartedAt >= 0L && !this.caughtExitSecondaryDone,
				Math.max(0L, this.caughtExitOwnerAvoidUntil - this.level().getGameTime()),
				this.mutualGazeDistractionStartedAt >= 0L,
				this.mutualGazeAligned,
				this.mutualGazeHoldTicksRemaining,
				this.entityData.get(VISUAL_REACTION),
				this.eyeAttentionKind.name().toLowerCase(Locale.ROOT),
				this.headAttentionKind.name().toLowerCase(Locale.ROOT),
				this.bodyAttentionKind.name().toLowerCase(Locale.ROOT),
				this.yBodyRot
		);
	}

	public boolean isMutualGazeActive() {
		return this.mutualGazePlayerUuid != null;
	}

	public boolean isVisualInteractionMovementOwned() {
		return isMutualGazeActive() || isCaughtExitActive();
	}

	public enum VisualTestMode {
		BLINK,
		DOUBLE_BLINK,
		CURIOUS,
		STARTLED,
		EXIT_LOOK,
		EXIT_TURN,
		EXIT_WALK,
		EXIT_SECONDARY,
		RESET
	}

	public enum AnimationTestMode {
		ATTACK,
		HURT,
		SHIELD_RAISE,
		SHIELD_LOWER,
		RESET
	}

	public void forceAnimationState(AnimationTestMode mode) {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		long now = level.getGameTime();
		switch (mode) {
			case ATTACK -> {
				this.attackAnimationUntil = now + ATTACK_ANIMATION_TICKS;
				this.triggerAnim(ACTION_CONTROLLER, ATTACK_TRIGGER);
			}
			case HURT -> {
				this.attackAnimationUntil = 0L;
				setReaction(VISUAL_HURT, now + 10);
				this.entityData.set(CURIOUS_TILT, (byte)0);
				this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
				triggerHurtPresentation(now, true);
			}
			case SHIELD_RAISE -> this.triggerAnim(ACTION_CONTROLLER, SHIELD_RAISE_TRIGGER);
			case SHIELD_LOWER -> this.triggerAnim(ACTION_CONTROLLER, SHIELD_LOWER_TRIGGER);
			case RESET -> {
				this.attackAnimationUntil = 0L;
				this.stopTriggeredAnim(ACTION_CONTROLLER, ATTACK_TRIGGER);
				this.stopTriggeredAnim(ACTION_CONTROLLER, HURT_TRIGGER);
				this.stopTriggeredAnim(ACTION_CONTROLLER, SHIELD_RAISE_TRIGGER);
				this.stopTriggeredAnim(ACTION_CONTROLLER, SHIELD_LOWER_TRIGGER);
			}
		}
	}

	private enum AttentionKind {
		PRIMED_CREEPER,
		DAMAGE_SOURCE,
		CLOSE_CREEPER,
		COMBAT_TARGET,
		MUTUAL_GAZE,
		APPROACHING,
		NORMAL
	}

	private enum CaughtExitMode {
		NONE,
		LOOK_AWAY,
		TURN_AWAY,
		WALK_AWAY
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

	private enum GazeState {
		VALID,
		MISSED,
		BLOCKED
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

	private @Nullable LivingEntity selectProtectiveTarget(LivingEntity owner) {
		LivingEntity ownAttacker = this.getLastHurtByMob();
		if (isRecent(this, this.getLastHurtByMobTimestamp()) && canProtectAgainst(ownAttacker)) {
			return ownAttacker;
		}

		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isRecent(owner, owner.getLastHurtByMobTimestamp()) && canProtectAgainst(ownerAttacker)) {
			return ownerAttacker;
		}

		LivingEntity ownerTarget = owner.getLastHurtMob();
		return isRecent(owner, owner.getLastHurtMobTimestamp()) && canProtectAgainst(ownerTarget) ? ownerTarget : null;
	}

	private static boolean isRecent(LivingEntity source, int timestamp) {
		return timestamp > 0 && source.tickCount - timestamp <= 100;
	}

	private static boolean isRecentWithin(LivingEntity source, int timestamp, int ticks) {
		return timestamp > 0 && source.tickCount - timestamp <= ticks;
	}

	private boolean canProtectAgainst(@Nullable LivingEntity target) {
		return target != null && target.isAlive() && this.distanceToSqr(target) <= 32.0 * 32.0 && this.canAttack(target);
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		LivingEntity owner = this.getOwner();
		if (target == this || target == owner || target.isAlliedTo(this) || owner != null && owner.isAlliedTo(target)) {
			return false;
		}
		if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
			return false;
		}
		if (target instanceof RomanLegionaryEchoEntity echo && owner != null && owner == echo.getOwner()) {
			return false;
		}
		if (target instanceof OwnableEntity ownable && owner != null && ownable.getRootOwner() == owner) {
			return false;
		}
		return super.canAttack(target);
	}

	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		LivingEntity owner = this.getOwner();
		if (other == owner) {
			return true;
		}
		if (other instanceof RomanLegionaryEchoEntity echo && owner != null && owner == echo.getOwner()) {
			return true;
		}
		return owner != null && owner.isAlliedTo(other) || super.considersEntityAsAlly(other);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		Entity attacker = source.getEntity();
		if (attacker == this.getOwner() || attacker instanceof RomanLegionaryEchoEntity echo && echo.getOwner() == this.getOwner()) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			Entity attackerEntity = source.getEntity();
			long now = level.getGameTime();
			endCaughtExit(now, true);
			triggerHurtPresentation(now, now >= this.attackAnimationUntil);
			if (attackerEntity instanceof LivingEntity living) {
				applyAttention(new AttentionCandidate(living, living.getEyePosition(), 1100,
						VISUAL_HURT, 16, false, AttentionKind.DAMAGE_SOURCE), now);
			} else {
				Vec3 point = this.position().add(this.getLookAngle().reverse());
				setEyeAttentionPoint(point);
				setAttentionPoint(point);
				setReaction(VISUAL_HURT, now + 16);
				this.entityData.set(CURIOUS_TILT, (byte)0);
				this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
			}
		}
		return hurt;
	}

	private void triggerHurtPresentation(long now, boolean playBodyAnimation) {
		this.entityData.set(BLINK_START, now);
		this.entityData.set(BLINK_COUNT, (byte)1);
		if (playBodyAnimation) {
			this.triggerAnim(ACTION_CONTROLLER, HURT_TRIGGER);
		}
	}

	public void bindTo(Player owner, UUID summonerUuid) {
		this.ownerReference = EntityReference.of(owner);
		this.summonerUuid = summonerUuid;
		this.remainingLifetime = MAX_LIFETIME_TICKS;
		this.missingSummonerTicks = 0;
		setEyeAttentionPoint(owner.getEyePosition());
		setAttentionPoint(owner.getEyePosition());
	}

	public @Nullable UUID getOwnerUuid() {
		LivingEntity owner = getOwner();
		return owner == null ? null : owner.getUUID();
	}

	public @Nullable UUID getSummonerUuid() {
		return this.summonerUuid;
	}

	@Override
	public @Nullable EntityReference<LivingEntity> getOwnerReference() {
		return this.ownerReference;
	}

	public void recallTo(Player player) {
		if (this.level() instanceof ServerLevel serverLevel) {
			long now = serverLevel.getGameTime();
			if (this.mutualGazePlayerUuid != null) {
				endMutualGaze(now);
			}
			endCaughtExit(now, true);
		}
		Vec3 side = player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(1.5);
		double targetX = player.getX() + side.x;
		double targetZ = player.getZ() + side.z;
		float facingYaw = yawToward(targetX, targetZ, player.getX(), player.getZ());
		this.snapTo(targetX, player.getY(), targetZ, facingYaw, 0.0F);
		this.setYBodyRot(facingYaw);
		this.setYHeadRot(facingYaw);
		this.getNavigation().stop();
		if (this.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.25, 0.5, 0.25, 0.01);
			serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.45F, 1.45F);
		}
	}

	public static float yawToward(double fromX, double fromZ, double targetX, double targetZ) {
		return (float)(Math.atan2(targetZ - fromZ, targetX - fromX) * 180.0 / Math.PI) - 90.0F;
	}

	public void dismiss() {
		if (this.isRemoved()) {
			return;
		}
		if (this.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 24, 0.35, 0.7, 0.35, 0.02);
			serverLevel.playSound(null, this.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7F, 0.75F);
		}
		this.discard();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		EntityReference.store(this.ownerReference, output, "EchoOwner");
		if (this.summonerUuid != null) {
			output.putString("SummonerUuid", this.summonerUuid.toString());
		}
		output.putInt("RemainingLifetime", this.remainingLifetime);
		output.putInt("MissingSummonerTicks", this.missingSummonerTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ownerReference = EntityReference.readWithOldOwnerConversion(input, "EchoOwner", this.level());
		try {
			this.summonerUuid = UUID.fromString(input.getStringOr("SummonerUuid", ""));
		} catch (IllegalArgumentException ignored) {
			this.summonerUuid = null;
		}
		this.remainingLifetime = input.getIntOr("RemainingLifetime", MAX_LIFETIME_TICKS);
		this.missingSummonerTicks = input.getIntOr("MissingSummonerTicks", 0);
	}

	@Override
	protected boolean shouldDropLoot(ServerLevel level) {
		return false;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<RomanLegionaryEchoEntity>("movement", 3, this::selectMovementAnimation));
		controllers.add(new AnimationController<RomanLegionaryEchoEntity>(ACTION_CONTROLLER, 2, test -> PlayState.STOP)
				.triggerableAnim(ATTACK_TRIGGER, ATTACK)
				.triggerableAnim(HURT_TRIGGER, HURT)
				.triggerableAnim(SHIELD_RAISE_TRIGGER, SHIELD_RAISE)
				.triggerableAnim(SHIELD_LOWER_TRIGGER, SHIELD_LOWER));
	}

	private PlayState selectMovementAnimation(AnimationTest<RomanLegionaryEchoEntity> test) {
		int currentTick = test.animatable().tickCount;
		if (test.isMoving()) {
			this.movementAnimationActive = true;
			this.movementAnimationLastMovingTick = currentTick;
		} else if (this.movementAnimationActive
				&& currentTick - this.movementAnimationLastMovingTick >= MOVEMENT_ANIMATION_RELEASE_TICKS) {
			this.movementAnimationActive = false;
		}

		return test.setAndContinue(this.movementAnimationActive ? WALK : IDLE);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animationCache;
	}
}
