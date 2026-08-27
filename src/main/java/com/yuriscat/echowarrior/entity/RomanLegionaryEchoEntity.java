package com.yuriscat.echowarrior.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.yuriscat.echowarrior.ModEffects;
import com.yuriscat.echowarrior.entity.behavior.EchoActivityMovement;
import com.yuriscat.echowarrior.entity.behavior.EchoFollowOwner;
import com.yuriscat.echowarrior.entity.behavior.EchoWaterSafety;
import com.yuriscat.echowarrior.item.EchoRelicState;
import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.SummonerFuel;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import com.yuriscat.echowarrior.progress.EchoExperienceSystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
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
		implements EchoWarriorEntity, SmartBrainOwner<RomanLegionaryEchoEntity>, GeoEntity {
	public static final byte VISUAL_NORMAL = 0;
	public static final byte VISUAL_ALERT = 1;
	public static final byte VISUAL_STARTLED = 2;
	public static final byte VISUAL_HURT = 3;
	public static final byte VISUAL_CURIOUS = 4;
	public static final byte VISUAL_MUTUAL_GAZE = 5;
	public static final byte VISUAL_CAUGHT = 6;
	public static final byte VISUAL_LOCOMOTION = 7;

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
	private static final EntityDataAccessor<Boolean> SHIELD_RAISED = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.BOOLEAN);

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
	private static final int ATTACK_ANIMATION_TICKS = 20;
	private static final Identifier LEGION_ARMOR_ID = Identifier.fromNamespaceAndPath("echo_warrior", "legion_endures_armor");
	private static final Identifier LEGION_KNOCKBACK_ID = Identifier.fromNamespaceAndPath("echo_warrior", "legion_endures_knockback");
	private static final AttributeModifier LEGION_ARMOR = new AttributeModifier(LEGION_ARMOR_ID, 12.0, AttributeModifier.Operation.ADD_VALUE);
	private static final AttributeModifier LEGION_KNOCKBACK = new AttributeModifier(LEGION_KNOCKBACK_ID, 1.0, AttributeModifier.Operation.ADD_VALUE);

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private boolean movementAnimationActive;
	private int movementAnimationLastMovingTick = Integer.MIN_VALUE;
	private boolean shieldAnimationWasRaised;
	private int shieldLowerAnimationUntil = Integer.MIN_VALUE;
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
	private EchoRelicState.ActivityMode activityMode = EchoRelicState.ActivityMode.FOLLOW;
	private EchoRelicState.AlertMode alertMode = EchoRelicState.AlertMode.DEFENSIVE;
	private int enabledSkills = EchoHeroType.ROMAN_LEGIONARY.allSkillsEnabledMask();
	private Vec3 activityAnchor = Vec3.ZERO;
	private boolean formationActive;
	private boolean shieldBondActive;
	private long legionEnduresUntil;
	private long lastNaturalHealAt;
	private @Nullable Entity shieldChargeTarget;
	private long shieldChargeStartedAt;
	private long shieldChargeUntil;
	private long shieldInternalCooldownUntil;
	private @Nullable UUID recentlyChargedTargetUuid;
	private long recentlyChargedTargetUntil;
	private final Set<UUID> reflectedProjectiles = new HashSet<>();
	private float legionAccumulatedDamage;

	public RomanLegionaryEchoEntity(EntityType<? extends RomanLegionaryEchoEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, EchoHeroType.ROMAN_LEGIONARY.baseMaximumHealth())
				.add(Attributes.ARMOR, EchoHeroType.ROMAN_LEGIONARY.baseArmor())
				.add(Attributes.ATTACK_DAMAGE, EchoHeroType.ROMAN_LEGIONARY.baseAttackDamage())
				.add(Attributes.MOVEMENT_SPEED, EchoHeroType.ROMAN_LEGIONARY.baseMovementSpeed())
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, EchoHeroType.ROMAN_LEGIONARY.baseKnockbackResistance());
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
		entityData.define(SHIELD_RAISED, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
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
		return List.of(new EchoFollowOwner<RomanLegionaryEchoEntity>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getFightingBehaviours(RomanLegionaryEchoEntity owner) {
		return List.of(
				new InvalidateAttackTarget<RomanLegionaryEchoEntity>(),
				new SetWalkTargetToAttackTarget<RomanLegionaryEchoEntity>().speedModifier(1.15F).closeEnoughDist(1),
				new AnimatableMeleeAttack<RomanLegionaryEchoEntity>(6)
						.attackInterval((entity, target) -> entity.meleeAttackInterval())
						.canAttack((entity, target) -> entity.canPerformMeleeHit(target))
						.whenStarting(RomanLegionaryEchoEntity::startMeleeAttackAnimation)
		);
	}

	private boolean canPerformMeleeHit(LivingEntity target) {
		return this.shieldChargeTarget == null
				&& !isLegionEnduresActive()
				&& target.isAlive()
				&& this.canAttack(target)
				&& this.hasLineOfSight(target)
				&& this.isWithinMeleeAttackRange(target);
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
		tickCombatSkills(serverLevel, owner);
		if (this.tickCount % 5 == 0) {
			ItemStack relic = currentRelic();
			if (!relic.isEmpty()) tickFormation(serverLevel, owner, relic);
		}

		if (this.tickCount % 5 == 0) {
			LivingEntity target = selectProtectiveTarget(owner);
			if (target != null && this.canAttack(target)) {
				BrainUtil.setTargetOfEntity(this, target);
			}
			enforceActivityBoundary(owner);
		}
		if (this.tickCount % 20 == 0) {
			ItemStack relic = currentRelic();
			if (!relic.isEmpty()) {
				applyRelicState(relic, false);
				tickNaturalHealing(serverLevel, relic);
			}
		}
		if (this.tickCount % 20 == 0 && this.getTarget() != null) {
			EchoExperienceSystem.markParticipation(this, this.getTarget());
		}

		EchoActivityMovement.tick(serverLevel, this, this.activityMode, this.activityAnchor,
				this.getTarget() != null || this.shieldChargeTarget != null || isLegionEnduresActive()
						|| serverLevel.getGameTime() < this.attackAnimationUntil || isVisualInteractionMovementOwned());
		tickVisualAwareness(serverLevel, owner);
		EchoWaterSafety.tick(serverLevel, this, owner,
				this.activityMode == EchoRelicState.ActivityMode.FOLLOW
						&& this.shieldChargeTarget == null && !isLegionEnduresActive());
	}

	private void tickCombatSkills(ServerLevel level, LivingEntity owner) {
		ItemStack relic = currentRelic();
		if (relic.isEmpty()) return;
		long now = level.getGameTime();
		if (isLegionEnduresActive()) {
			tickLegionEndures(level, owner, now);
			return;
		}
		if (this.legionEnduresUntil != 0L && now >= this.legionEnduresUntil) {
			finishLegionEndures(level);
		}
		if (this.shieldChargeTarget != null) {
			tickShieldCharge(level, owner, now);
		} else if (EchoRelicState.skillEnabled(relic, 1) && now >= this.shieldInternalCooldownUntil) {
			Entity target = findShieldChargeTarget(level, owner);
			if (target != null && EchoRelicState.consumeShieldCharge(relic, now)) {
				persistCurrentRelic(relic);
				this.shieldChargeTarget = target;
				this.shieldChargeStartedAt = now;
				this.shieldChargeUntil = now + 20L;
				raiseShield();
				this.getNavigation().stop();
			}
		}
		if (EchoRelicState.skillEnabled(relic, 2) && canStartLegionEndures(level, owner, relic, now)) {
			startLegionEndures(level, owner, relic, now);
		}
	}

	private @Nullable Entity findShieldChargeTarget(ServerLevel level, LivingEntity owner) {
		Projectile bestProjectile = null;
		double bestTime = Double.MAX_VALUE;
		for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, owner.getBoundingBox().inflate(12.0), Projectile::isAlive)) {
			if (isRecentlyChargedTarget(projectile, level.getGameTime()) || this.reflectedProjectiles.contains(projectile.getUUID()) || projectile.getOwner() == owner
					|| projectile.getOwner() == this || projectile.getOwner() instanceof LivingEntity living && !this.canAttack(living)) continue;
			Vec3 velocity = projectile.getDeltaMovement();
			double speedSqr = velocity.lengthSqr();
			if (speedSqr < 0.0025) continue;
			Vec3 toOwner = owner.getEyePosition().subtract(projectile.position());
			double time = Math.clamp(toOwner.dot(velocity) / speedSqr, 0.0, 20.0);
			if (time <= 0.0 || projectile.position().add(velocity.scale(time)).distanceToSqr(owner.getEyePosition()) > 2.25) continue;
			if (time < bestTime) {
				bestTime = time;
				bestProjectile = projectile;
			}
		}
		if (bestProjectile != null) return bestProjectile;
		return level.getEntitiesOfClass(Creeper.class, owner.getBoundingBox().inflate(8.0), creeper ->
				creeper.isAlive() && !isRecentlyChargedTarget(creeper, level.getGameTime())
						&& this.canAttack(creeper) && (creeper.isIgnited() || creeper.getSwellDir() > 0)
						&& !(owner.getLastHurtMob() == creeper && isRecentWithin(owner, owner.getLastHurtMobTimestamp(), 100)))
				.stream().min(java.util.Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
	}

	private void tickShieldCharge(ServerLevel level, LivingEntity owner, long now) {
		Entity target = this.shieldChargeTarget;
		if (target == null || !target.isAlive() || now > this.shieldChargeUntil) {
			stopShieldCharge();
			return;
		}
		long elapsed = Math.max(0L, now - this.shieldChargeStartedAt);
		double progress = Math.clamp(elapsed / 20.0, 0.0, 1.0);
		double chargeSpeed = 0.32 + Math.sin(progress * Math.PI) * 0.34;
		Vec3 direction = target.position().subtract(this.position());
		if (direction.lengthSqr() > 0.01) {
			Vec3 velocity = direction.normalize().scale(chargeSpeed);
			this.setDeltaMovement(velocity.x, Math.max(this.getDeltaMovement().y, velocity.y), velocity.z);
		}
		if (elapsed % 2L == 0L) {
			Vec3 trail = this.position().subtract(this.getDeltaMovement().normalize().scale(0.35));
			level.sendParticles(ParticleTypes.GUST, trail.x, this.getY() + 0.75, trail.z, 1, 0.08, 0.2, 0.08, 0.01);
		}
		if (elapsed % 6L == 0L) {
			level.playSound(null, this.blockPosition(), SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS, 0.28F,
					1.25F + this.getRandom().nextFloat() * 0.15F);
		}
		if (this.distanceToSqr(target) > 3.0 || now - this.shieldChargeStartedAt < 6L) return;
		boolean impacted = false;
		if (target instanceof Projectile projectile) {
			Vec3 normal = owner.getLookAngle().normalize();
			Vec3 incoming = projectile.getDeltaMovement();
			Vec3 reflected = incoming.subtract(normal.scale(2.0 * incoming.dot(normal)));
			if (reflected.dot(incoming) > 0.0) reflected = incoming.reverse();
			projectile.setDeltaMovement(reflected);
			projectile.setOwner(this);
			this.reflectedProjectiles.add(projectile.getUUID());
			level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(), 7, 0.15, 0.15, 0.15, 0.05);
			impacted = true;
		} else if (target instanceof Creeper creeper) {
			Vec3 away = creeper.position().subtract(owner.position()).multiply(1.0, 0.0, 1.0).normalize();
			Vec3 charge = creeper.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
			Vec3 combined = away.scale(0.7).add(charge.scale(0.3)).normalize();
			creeper.knockback(2.2, -combined.x, -combined.z);
			creeper.setDeltaMovement(creeper.getDeltaMovement().add(combined.scale(0.35)).add(0.0, 0.18, 0.0));
			level.sendParticles(ParticleTypes.GUST, creeper.getX(), creeper.getY() + 0.8, creeper.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
			impacted = true;
		}
		if (impacted) {
			this.recentlyChargedTargetUuid = target.getUUID();
			this.recentlyChargedTargetUntil = now + 40L;
			level.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 0.9F, 0.9F);
		}
		stopShieldCharge();
	}

	private boolean isRecentlyChargedTarget(Entity target, long now) {
		return this.recentlyChargedTargetUuid != null && now < this.recentlyChargedTargetUntil
				&& this.recentlyChargedTargetUuid.equals(target.getUUID());
	}

	private void stopShieldCharge() {
		this.shieldChargeTarget = null;
		this.shieldInternalCooldownUntil = Math.max(this.shieldInternalCooldownUntil, this.level().getGameTime() + 10L);
		Vec3 velocity = this.getDeltaMovement();
		this.setDeltaMovement(velocity.x * 0.15, velocity.y, velocity.z * 0.15);
		lowerShield();
	}

	private void raiseShield() {
		this.stopTriggeredAnim(ACTION_CONTROLLER, ATTACK_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, HURT_TRIGGER);
		this.entityData.set(SHIELD_RAISED, true);
	}

	private void lowerShield() {
		this.entityData.set(SHIELD_RAISED, false);
	}

	private boolean canStartLegionEndures(ServerLevel level, LivingEntity owner, ItemStack relic, long now) {
		if (now < EchoRelicState.legionCooldownEnd(relic)) return false;
		if (this.getHealth() >= this.getMaxHealth() * 0.6F && !isRecentWithin(this, this.getLastHurtByMobTimestamp(), 100)
				&& !isRecentWithin(owner, owner.getLastHurtByMobTimestamp(), 100)) return false;
		if (level.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(6.0), monster -> this.canAttack(monster)).size() < 2) return false;
		return level.getEntitiesOfClass(RomanLegionaryEchoEntity.class, owner.getBoundingBox().inflate(32.0),
				echo -> echo != this && echo.getOwner() == owner && echo.isLegionEnduresActive()).isEmpty();
	}

	private void startLegionEndures(ServerLevel level, LivingEntity owner, ItemStack relic, long now) {
		this.legionEnduresUntil = now + 100L;
		this.legionAccumulatedDamage = 0.0F;
		EchoRelicState.setLegionCooldownEnd(relic, now + 400L);
		persistCurrentRelic(relic);
		this.getAttribute(Attributes.ARMOR).addOrUpdateTransientModifier(LEGION_ARMOR);
		this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addOrUpdateTransientModifier(LEGION_KNOCKBACK);
		this.getNavigation().stop();
		this.setTarget(null);
		raiseShield();
		spawnLegionTauntParticle(level, now);
		for (Monster monster : level.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(12.0), monster -> this.canAttack(monster) && this.hasLineOfSight(monster))) {
			monster.setTarget(this);
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, monster.getX(), monster.getY() + monster.getBbHeight() + 0.2, monster.getZ(), 5, 0.22, 0.12, 0.22, 0.0);
		}
		level.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 0.9F, 0.65F);
	}

	private void tickLegionEndures(ServerLevel level, LivingEntity owner, long now) {
		this.getNavigation().stop();
		this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
		this.setTarget(null);
		if ((now - (this.legionEnduresUntil - 100L)) % 3L == 0L) {
			spawnLegionTauntParticle(level, now);
		}
		if (this.tickCount % 10 == 0) {
			for (Monster monster : level.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(12.0), monster -> this.canAttack(monster) && this.hasLineOfSight(monster))) {
				monster.setTarget(this);
				level.sendParticles(ParticleTypes.ANGRY_VILLAGER, monster.getX(), monster.getY() + monster.getBbHeight() + 0.2, monster.getZ(), 2, 0.16, 0.08, 0.16, 0.0);
			}
		}
	}

	private void spawnLegionTauntParticle(ServerLevel level, long now) {
		long elapsed = Math.max(0L, now - (this.legionEnduresUntil - 100L));
		double phase = (elapsed % 24L) / 23.0;
		double side = ((elapsed / 24L) & 1L) == 0L ? 1.0 : -1.0;
		double angle = Math.toRadians(this.getYRot() + side * 90.0);
		double horizontal = side * (0.08 + 0.42 * phase);
		double arcHeight = Math.sin(phase * Math.PI) * 1.15;
		double x = this.getX() + Math.cos(angle) * horizontal;
		double y = this.getY() + this.getBbHeight() + 0.12 + arcHeight;
		double z = this.getZ() + Math.sin(angle) * horizontal;
		level.sendParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 1, 0.015, 0.015, 0.015, 0.0);
	}

	private void finishLegionEndures(ServerLevel level) {
		this.legionEnduresUntil = 0L;
		this.getAttribute(Attributes.ARMOR).removeModifier(LEGION_ARMOR_ID);
		this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(LEGION_KNOCKBACK_ID);
		lowerShield();
		float damage = this.legionAccumulatedDamage;
		double scale = Math.max(1.0, Math.sqrt(Math.max(0.0, damage) / 20.0));
		for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(6.0), this::canAttack)) {
			if (damage > 0.0F) enemy.hurtServer(level, level.damageSources().mobAttack(this), damage);
			double distance = Math.max(0.5, this.distanceTo(enemy));
			double strength = 1.5 * scale * Math.max(0.2, 1.0 - distance / 8.0);
			enemy.knockback(strength, this.getX() - enemy.getX(), this.getZ() - enemy.getZ());
		}
		level.sendParticles(ParticleTypes.GUST, this.getX(), this.getY() + 0.8, this.getZ(), 20, 1.8, 0.5, 1.8, 0.12);
		level.playSound(null, this.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.9F, 0.9F);
		this.legionAccumulatedDamage = 0.0F;
	}

	private void enforceActivityBoundary(LivingEntity owner) {
		LivingEntity target = this.getTarget();
		Vec3 center = this.activityMode == EchoRelicState.ActivityMode.FOLLOW ? owner.position() : this.activityAnchor;
		double giveUp = this.activityMode == EchoRelicState.ActivityMode.WAIT ? 8.0
				: this.activityMode == EchoRelicState.ActivityMode.WANDER ? 24.0 : 32.0;
		if (target != null && target.position().distanceToSqr(center) > giveUp * giveUp) {
			BrainUtil.clearMemory(this, net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
			this.setTarget(null);
			target = null;
		}
	}

	private void tickFormation(ServerLevel level, LivingEntity owner, ItemStack relic) {
		this.formationActive = EchoRelicState.skillEnabled(relic, 0);
		this.shieldBondActive = this.formationActive && owner.distanceToSqr(this) <= 64.0 && owner instanceof Player player
				&& (player.getMainHandItem().getItem() instanceof ShieldItem || player.getOffhandItem().getItem() instanceof ShieldItem);

		updateFormationEffect(this, formationAmplifier(level, owner, this));
		updateFormationEffect(owner, formationAmplifier(level, owner, owner));
		for (RomanLegionaryEchoEntity echo : level.getEntitiesOfClass(RomanLegionaryEchoEntity.class,
				owner.getBoundingBox().inflate(24.0), candidate -> candidate.getOwner() == owner)) {
			updateFormationEffect(echo, formationAmplifier(level, owner, echo));
		}
	}

	private static int formationAmplifier(ServerLevel level, LivingEntity owner, LivingEntity beneficiary) {
		int state = -1;
		for (RomanLegionaryEchoEntity echo : level.getEntitiesOfClass(RomanLegionaryEchoEntity.class,
				beneficiary.getBoundingBox().inflate(8.0), candidate -> candidate.isAlive() && candidate.getOwner() == owner
						&& candidate.formationActive && candidate.distanceToSqr(beneficiary) <= 64.0)) {
			state = Math.max(state, echo.shieldBondActive ? 1 : 0);
		}
		return state;
	}

	private static void updateFormationEffect(LivingEntity entity, int state) {
		boolean weaponsRaised = entity.hasEffect(ModEffects.WEAPONS_RAISED);
		boolean shieldsRaised = entity.hasEffect(ModEffects.SHIELDS_RAISED);
		if (state < 0) {
			if (weaponsRaised) entity.removeEffect(ModEffects.WEAPONS_RAISED);
			if (shieldsRaised) entity.removeEffect(ModEffects.SHIELDS_RAISED);
			return;
		}
		if (state == 0) {
			if (shieldsRaised) entity.removeEffect(ModEffects.SHIELDS_RAISED);
			if (!weaponsRaised) entity.addEffect(new MobEffectInstance(ModEffects.WEAPONS_RAISED,
					MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
		} else {
			if (weaponsRaised) entity.removeEffect(ModEffects.WEAPONS_RAISED);
			if (!shieldsRaised) entity.addEffect(new MobEffectInstance(ModEffects.SHIELDS_RAISED,
					MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
		}
	}

	private void tickNaturalHealing(ServerLevel level, ItemStack relic) {
		long now = level.getGameTime();
		if (this.getHealth() >= this.getMaxHealth() || this.getTarget() != null
				|| this.tickCount - this.getLastHurtByMobTimestamp() < 100 || now - this.lastNaturalHealAt < 40L) {
			return;
		}
		LivingEntity owner = this.getOwner();
		if (!(owner instanceof Player player) || this.summonerUuid == null) return;
		ItemStack summoner = TestEchoSummonerItem.findSummonerStack(player, this.summonerUuid);
		if (summoner.isEmpty() || !SummonerFuel.consumeFractional(summoner, SummonerFuel.healCost(relic))) return;
		this.heal(1.0F);
		this.lastNaturalHealAt = now;
		level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 2, 0.15, 0.3, 0.15, 0.0);
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
		boolean locomotionAttention = shouldUseLocomotionAttention();
		AttentionCandidate best = isVisibleAttentionTarget(combatTarget)
				? new AttentionCandidate(combatTarget, combatTarget.getEyePosition(), 800, VISUAL_ALERT, 30, false, AttentionKind.COMBAT_TARGET)
				: now < this.caughtExitOwnerAvoidUntil
						? new AttentionCandidate(null, this.caughtExitOwnerAvoidPoint, 220, VISUAL_NORMAL,
								35 + this.random.nextInt(36), false, AttentionKind.NORMAL)
						: locomotionAttention
								? new AttentionCandidate(null, createLocomotionAttentionPoint(), LOCOMOTION_ATTENTION_PRIORITY,
										VISUAL_LOCOMOTION, LOCOMOTION_ATTENTION_TICKS, false, AttentionKind.LOCOMOTION)
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

	private boolean shouldUseLocomotionAttention() {
		Vec3 movement = this.getDeltaMovement();
		double horizontalSpeedSqr = movement.x * movement.x + movement.z * movement.z;
		return !isVisualInteractionMovementOwned()
				&& (!this.getNavigation().isDone() || horizontalSpeedSqr > 2.5E-4);
	}

	private Vec3 createLocomotionAttentionPoint() {
		Vec3 movement = this.getDeltaMovement();
		Vec3 direction = new Vec3(movement.x, 0.0, movement.z);
		if (direction.lengthSqr() > 1.0E-4) {
			direction = direction.normalize();
		} else {
			float yaw = this.yBodyRot * ((float)Math.PI / 180.0F);
			direction = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		}
		return this.getEyePosition().add(direction.scale(6.0));
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

	@Override
	public boolean isFollowMovementSuppressed() {
		return isVisualInteractionMovementOwned();
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
			case SHIELD_RAISE -> raiseShield();
			case SHIELD_LOWER -> lowerShield();
			case RESET -> {
				this.attackAnimationUntil = 0L;
				this.stopTriggeredAnim(ACTION_CONTROLLER, ATTACK_TRIGGER);
				this.stopTriggeredAnim(ACTION_CONTROLLER, HURT_TRIGGER);
				lowerShield();
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
		LOCOMOTION,
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
		if (this.alertMode == EchoRelicState.AlertMode.PEACEFUL) {
			return null;
		}

		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isRecent(owner, owner.getLastHurtByMobTimestamp()) && canProtectAgainst(ownerAttacker)) {
			return ownerAttacker;
		}

		LivingEntity ownerTarget = owner.getLastHurtMob();
		if (isRecent(owner, owner.getLastHurtMobTimestamp()) && canProtectAgainst(ownerTarget)) {
			return ownerTarget;
		}
		if (this.alertMode == EchoRelicState.AlertMode.AGGRESSIVE) {
			double range = this.activityMode == EchoRelicState.ActivityMode.WAIT ? 6.0 : 16.0;
			AABB scanBox = this.activityMode == EchoRelicState.ActivityMode.WAIT
					? new AABB(this.activityAnchor.x - range, this.activityAnchor.y - 4.0, this.activityAnchor.z - range,
							this.activityAnchor.x + range, this.activityAnchor.y + 4.0, this.activityAnchor.z + range)
					: this.getBoundingBox().inflate(range);
			return this.level().getEntitiesOfClass(Monster.class, scanBox, this::canProtectAgainst)
					.stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
		}
		return null;
	}

	private static boolean isRecent(LivingEntity source, int timestamp) {
		return timestamp > 0 && source.tickCount - timestamp <= 100;
	}

	private static boolean isRecentWithin(LivingEntity source, int timestamp, int ticks) {
		return timestamp > 0 && source.tickCount - timestamp <= ticks;
	}

	private boolean canProtectAgainst(@Nullable LivingEntity target) {
		if (target == null || !target.isAlive() || this.distanceToSqr(target) > 32.0 * 32.0 || !this.canAttack(target)) {
			return false;
		}
		if (this.activityMode == EchoRelicState.ActivityMode.WAIT) {
			return target.position().distanceToSqr(this.activityAnchor) <= 8.0 * 8.0;
		}
		if (this.activityMode == EchoRelicState.ActivityMode.WANDER) {
			return target.position().distanceToSqr(this.activityAnchor) <= 16.0 * 16.0;
		}
		return true;
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
		if (target instanceof EchoWarriorEntity echo && owner != null && owner == echo.getOwner()) {
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
		if (other instanceof EchoWarriorEntity echo && owner != null && owner == echo.getOwner()) {
			return true;
		}
		return owner != null && owner.isAlliedTo(other) || super.considersEntityAsAlly(other);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		Entity attacker = source.getEntity();
		if (attacker == this.getOwner() || attacker instanceof EchoWarriorEntity echo && echo.getOwner() == this.getOwner()) {
			return false;
		}
		if (isLegionEnduresActive() && attacker instanceof LivingEntity living && this.canAttack(living)) {
			this.legionAccumulatedDamage += Math.max(0.0F, damage);
		}
		float previousHealth = this.getHealth();
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			Entity attackerEntity = source.getEntity();
			long now = level.getGameTime();
			endCaughtExit(now, true);
			triggerHurtPresentation(now, now >= this.attackAnimationUntil
					&& this.shieldChargeTarget == null && !isLegionEnduresActive());
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
		this.reflectModuleMeleeDamage(level, source, previousHealth);
		return hurt;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
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
		this.activityAnchor = this.position();
		setEyeAttentionPoint(owner.getEyePosition());
		setAttentionPoint(owner.getEyePosition());
	}

	public void applyRelicState(ItemStack relic, boolean resetAnchor) {
		if (relic.isEmpty()) {
			return;
		}
		EchoRelicState.ActivityMode previousActivity = this.activityMode;
		EchoRelicState.AlertMode previousAlert = this.alertMode;
		this.activityMode = EchoRelicState.activityMode(relic);
		this.alertMode = EchoRelicState.alertMode(relic);
		this.enabledSkills = EchoRelicState.enabledSkills(relic);
		if (previousAlert != this.alertMode || previousActivity != this.activityMode || resetAnchor) {
			this.setTarget(null);
			BrainUtil.clearMemory(this, net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
		}
		if (previousActivity != this.activityMode || resetAnchor) {
			EchoActivityMovement.reset(this);
		}
		if ((this.enabledSkills & 1 << 1) == 0 && this.shieldChargeTarget != null) stopShieldCharge();
		if ((this.enabledSkills & 1 << 2) == 0 && isLegionEnduresActive() && this.level() instanceof ServerLevel serverLevel) {
			finishLegionEndures(serverLevel);
		}
		if (resetAnchor || this.activityAnchor == Vec3.ZERO) {
			this.activityAnchor = this.position();
		}
		double oldMaximum = this.getMaxHealth();
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(EchoRelicState.maximumHealth(relic));
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(EchoRelicState.attackDamage(relic));
		this.getAttribute(Attributes.ARMOR).setBaseValue(EchoRelicState.armor(relic));
		this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(EchoRelicState.movementSpeed(relic));
		this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(EchoRelicState.knockbackResistance(relic));
		this.applyModuleState();
		if (this.getHealth() >= oldMaximum - 0.01F) {
			this.setHealth(this.getMaxHealth());
		} else if (this.getHealth() > this.getMaxHealth()) {
			this.setHealth(this.getMaxHealth());
		}
	}

	public boolean shouldFollowOwner() {
		return this.activityMode == EchoRelicState.ActivityMode.FOLLOW && !isLegionEnduresActive();
	}

	public int meleeAttackInterval() {
		ItemStack relic = currentRelic();
		return relic.isEmpty() ? EchoHeroType.ROMAN_LEGIONARY.baseAttackIntervalTicks()
				: EchoRelicState.attackIntervalTicks(relic);
	}

	@Override
	public LivingEntity livingEntity() {
		return this;
	}

	@Override
	public EchoHeroType heroType() {
		return EchoHeroType.ROMAN_LEGIONARY;
	}

	public boolean isFormationActive() {
		return this.formationActive;
	}

	public boolean isShieldBondActive() {
		return this.shieldBondActive;
	}

	public boolean isShieldRaised() {
		return this.entityData.get(SHIELD_RAISED);
	}

	public boolean isLegionEnduresActive() {
		return this.level().getGameTime() < this.legionEnduresUntil;
	}

	private ItemStack currentRelic() {
		LivingEntity owner = this.getOwner();
		if (!(owner instanceof Player player) || this.summonerUuid == null) {
			return ItemStack.EMPTY;
		}
		ItemStack summoner = TestEchoSummonerItem.findSummonerStack(player, this.summonerUuid);
		return TestEchoSummonerItem.relicStack(summoner);
	}

	private void persistCurrentRelic(ItemStack relic) {
		LivingEntity owner = this.getOwner();
		if (!(owner instanceof Player player) || this.summonerUuid == null) return;
		ItemStack summoner = TestEchoSummonerItem.findSummonerStack(player, this.summonerUuid);
		if (!summoner.isEmpty()) TestEchoSummonerItem.setRelicStack(summoner, relic);
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
	public void onRemoval(Entity.RemovalReason reason) {
		cleanupTransientSkillEffects();
		super.onRemoval(reason);
	}

	private void cleanupTransientSkillEffects() {
		this.formationActive = false;
		updateFormationEffect(this, -1);
		this.getAttribute(Attributes.ARMOR).removeModifier(LEGION_ARMOR_ID);
		this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(LEGION_KNOCKBACK_ID);
		LivingEntity owner = this.getOwner();
		if (!(owner instanceof Player) || !(this.level() instanceof ServerLevel level)) return;
		updateFormationEffect(owner, formationAmplifier(level, owner, owner));
		for (RomanLegionaryEchoEntity echo : level.getEntitiesOfClass(RomanLegionaryEchoEntity.class,
				owner.getBoundingBox().inflate(24.0), candidate -> candidate != this && candidate.getOwner() == owner)) {
			updateFormationEffect(echo, formationAmplifier(level, owner, echo));
		}
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
		output.putInt("ActivityMode", this.activityMode.ordinal());
		output.putInt("AlertMode", this.alertMode.ordinal());
		output.putInt("EnabledSkills", this.enabledSkills);
		output.putDouble("ActivityAnchorX", this.activityAnchor.x);
		output.putDouble("ActivityAnchorY", this.activityAnchor.y);
		output.putDouble("ActivityAnchorZ", this.activityAnchor.z);
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
		this.activityMode = EchoRelicState.ActivityMode.byOrdinal(input.getIntOr("ActivityMode", 0));
		this.alertMode = EchoRelicState.AlertMode.byOrdinal(input.getIntOr("AlertMode", 1));
		this.enabledSkills = input.getIntOr("EnabledSkills", EchoHeroType.ROMAN_LEGIONARY.allSkillsEnabledMask());
		this.activityAnchor = new Vec3(input.getDoubleOr("ActivityAnchorX", this.getX()),
				input.getDoubleOr("ActivityAnchorY", this.getY()), input.getDoubleOr("ActivityAnchorZ", this.getZ()));
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
				.triggerableAnim(HURT_TRIGGER, HURT));
		controllers.add(new AnimationController<RomanLegionaryEchoEntity>("shield_pose", 0, this::selectShieldAnimation));
	}

	private PlayState selectShieldAnimation(AnimationTest<RomanLegionaryEchoEntity> test) {
		int currentTick = test.animatable().tickCount;
		if (test.animatable().entityData.get(SHIELD_RAISED)) {
			this.shieldAnimationWasRaised = true;
			this.shieldLowerAnimationUntil = Integer.MIN_VALUE;
			return test.setAndContinue(SHIELD_RAISE);
		}
		if (this.shieldAnimationWasRaised) {
			this.shieldAnimationWasRaised = false;
			this.shieldLowerAnimationUntil = currentTick + 5;
		}
		if (currentTick <= this.shieldLowerAnimationUntil) {
			return test.setAndContinue(SHIELD_LOWER);
		}
		return PlayState.STOP;
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
