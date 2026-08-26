package com.yuriscat.echowarrior.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModDamageTypes;
import com.yuriscat.echowarrior.entity.behavior.EchoActivityMovement;
import com.yuriscat.echowarrior.entity.behavior.EchoFollowOwner;
import com.yuriscat.echowarrior.entity.behavior.EchoWaterSafety;
import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.EchoRelicState;
import com.yuriscat.echowarrior.item.SummonerFuel;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import com.yuriscat.echowarrior.progress.EchoExperienceSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetWalkTargetToAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.util.BrainUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Medium-armour, high-mobility single-target duelist built around Zanshin
 * dodges, Fumikomi charges, a branchable two-slash normal attack and Stab.
 */
public final class JapaneseSamuraiEchoEntity extends PathfinderMob
		implements EchoWarriorEntity, SmartBrainOwner<JapaneseSamuraiEchoEntity>, GeoEntity {
	public static final int SKILL_ZANSHIN = 0;
	public static final int SKILL_FUMIKOMI = 1;
	public static final int SKILL_ZAN = 2;
	public static final int SKILL_STAB = 3;

	public static final byte ACTION_NONE = 0;
	public static final byte ACTION_ATTACK_FIRST = 1;
	public static final byte ACTION_ATTACK_RECOVER = 2;
	public static final byte ACTION_ATTACK_FOLLOW = 3;
	public static final byte ACTION_STAB = 4;
	public static final byte ACTION_DASH_FORWARD = 5;
	public static final byte ACTION_DASH_BACKWARD = 6;
	public static final byte ACTION_HURT = 7;

	private static final EntityDataAccessor<Byte> ACTION = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Long> ACTION_STARTED_AT = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> ACTION_ENDS_AT = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Float> ACTION_SPEED = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> AFTERIMAGE_NEUTRAL = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> AFTERIMAGE_ADVANCED = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> AFTERIMAGE_OUTLINE = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> AFTERIMAGE_SEQUENCE = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> AFTERIMAGE_KIND = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Float> AFTERIMAGE_X = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AFTERIMAGE_Y = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AFTERIMAGE_Z = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AFTERIMAGE_DIRECTION_X = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AFTERIMAGE_DIRECTION_Z = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AFTERIMAGE_YAW = SynchedEntityData.defineId(
			JapaneseSamuraiEchoEntity.class, EntityDataSerializers.FLOAT);

	public static final byte AFTERIMAGE_ZANSHIN_REAL = 1;
	public static final byte AFTERIMAGE_ZANSHIN_PHANTOM = 2;
	public static final byte AFTERIMAGE_FUMIKOMI = 3;

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.japanese_samurai.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.japanese_samurai.walk");
	private static final RawAnimation ATTACK_FIRST = RawAnimation.begin().thenPlayAndHold("animation.japanese_samurai.attack_first");
	private static final RawAnimation ATTACK_RECOVER = RawAnimation.begin().thenPlayAndHold("animation.japanese_samurai.attack_recover");
	private static final RawAnimation ATTACK_FOLLOW = RawAnimation.begin().thenPlayAndHold("animation.japanese_samurai.attack_follow");
	private static final RawAnimation STAB = RawAnimation.begin().thenPlayAndHold("animation.japanese_samurai.stab");
	private static final RawAnimation DASH_FORWARD = RawAnimation.begin().thenPlayAndHold("animation.japanese_samurai.dash_forward");
	private static final RawAnimation DASH_BACKWARD = RawAnimation.begin().thenPlayAndHold("animation.japanese_samurai.dash_backward");
	private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.japanese_samurai.hurt");
	private static final String ACTION_CONTROLLER = "action";
	private static final String ATTACK_FIRST_TRIGGER = "attack_first";
	private static final String ATTACK_RECOVER_TRIGGER = "attack_recover";
	private static final String ATTACK_FOLLOW_TRIGGER = "attack_follow";
	private static final String STAB_TRIGGER = "stab";
	private static final String DASH_FORWARD_TRIGGER = "dash_forward";
	private static final String DASH_BACKWARD_TRIGGER = "dash_backward";
	private static final String HURT_TRIGGER = "hurt";

	private static final int BASE_ATTACK_CYCLE_TICKS = 40;
	private static final int BASE_FIRST_TICKS = 13;
	private static final int BASE_RECOVER_TICKS = 10;
	private static final int BASE_FOLLOW_TICKS = 25;
	private static final int BASE_FOLLOW_HIT_TICKS = 13;
	private static final int STAB_ANIMATION_TICKS = 30;
	private static final int STAB_FIRST_HIT_TICK = 16;
	private static final int STAB_SECOND_HIT_TICK = 20;
	private static final int HURT_TICKS = 10;
	private static final int DASH_BACKWARD_TICKS = 14;
	private static final double NORMAL_MELEE_RANGE = 2.75;
	private static final double FIRST_SLASH_RADIUS = 2.25;
	private static final double FIRST_SLASH_ANGLE = 90.0;
	private static final double SECOND_SLASH_RADIUS = 2.75;
	private static final double SECOND_SLASH_ANGLE = 160.0;
	private static final double ATTACK_TRACKING_STOP_RANGE = 1.70;
	private static final double FIRST_SLASH_TRACKING_SPEED = 0.30;
	private static final double FIRST_SLASH_TRACKING_LIMIT = 1.50;
	private static final double SECOND_SLASH_TRACKING_SPEED = 0.20;
	private static final double SECOND_SLASH_TRACKING_LIMIT = 0.75;
	private static final float ATTACK_TRACKING_TURN_PER_TICK = 15.0F;
	private static final float ATTACK_TRACKING_TOTAL_TURN = 45.0F;
	private static final double FUMIKOMI_MAX_RANGE = 8.0;
	private static final double FUMIKOMI_STOP_RANGE = 2.0;
	private static final double FUMIKOMI_SPEED = 0.45;
	private static final int FUMIKOMI_MIN_TICKS = 6;
	private static final int FUMIKOMI_MAX_TICKS = 14;
	private static final int FUMIKOMI_INTERNAL_COOLDOWN_TICKS = 10;
	private static final double DODGE_BACKSTEP_DISTANCE = 2.0;
	private static final double STAB_TRIGGER_RANGE = 2.25;
	private static final double STAB_CAPSULE_LENGTH = 2.75;
	private static final double STAB_CAPSULE_RADIUS = 0.40;
	private static final int ZANSHIN_ATTACK_BONUS_TICKS = 10;
	private static final float ZANSHIN_ATTACK_BONUS = 0.20F;
	private static final float ZANSHIN_BASE_CAP = 0.40F;
	private static final float ZANSHIN_FINAL_CAP = 0.60F;
	private static final DustParticleOptions CYAN_AFTERIMAGE = new DustParticleOptions(0xAEEFFF, 1.15F);
	private static final DustParticleOptions GOLD_AFTERIMAGE = new DustParticleOptions(0xFFE4A3, 1.15F);
	private static final DustParticleOptions BLOOD = new DustParticleOptions(0xB51020, 1.05F);
	private static final Identifier SPECIAL_STEP_ID = EchoWarrior.id("samurai_special_step_height");
	private static final AttributeModifier SPECIAL_STEP = new AttributeModifier(
			SPECIAL_STEP_ID, 1.0, AttributeModifier.Operation.ADD_VALUE);

	private static final Map<UUID, Long> PINNED_UNTIL = new ConcurrentHashMap<>();
	private static final Map<UUID, Vec3> PINNED_CENTERS = new ConcurrentHashMap<>();

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private final List<UUID> secondSlashTargets = new ArrayList<>();
	private final Map<UUID, Vec3> stabTargets = new LinkedHashMap<>();
	private @Nullable EntityReference<LivingEntity> ownerReference;
	private @Nullable UUID summonerUuid;
	private int missingSummonerTicks;
	private EchoRelicState.ActivityMode activityMode = EchoRelicState.ActivityMode.FOLLOW;
	private EchoRelicState.AlertMode alertMode = EchoRelicState.AlertMode.DEFENSIVE;
	private int enabledSkills = EchoHeroType.JAPANESE_SAMURAI.allSkillsEnabledMask();
	private Vec3 activityAnchor = Vec3.ZERO;
	private long lastNaturalHealAt;
	private long nextNormalAttackAt;
	private long actionHitAt;
	private boolean actionHitResolved;
	private @Nullable UUID lockedTargetUuid;
	private @Nullable UUID secondSlashPrimaryUuid;
	private @Nullable UUID dashTargetUuid;
	private long fumikomiInternalCooldownUntil;
	private Vec3 dashBackwardDirection = Vec3.ZERO;
	private double dashBackwardTravelled;
	private float stabYaw;
	private boolean stabDirectionLocked;
	private long zanshinBonusUntil;
	private int fumikomiAfterimageStep;
	private Vec3 fumikomiAfterimageOrigin = Vec3.ZERO;
	private double attackTrackingTravelled;
	private float attackTrackingStartYaw;
	private boolean attackTrackingStopped;

	public JapaneseSamuraiEchoEntity(EntityType<? extends JapaneseSamuraiEchoEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, EchoHeroType.JAPANESE_SAMURAI.baseMaximumHealth())
				.add(Attributes.ARMOR, EchoHeroType.JAPANESE_SAMURAI.baseArmor())
				.add(Attributes.ATTACK_DAMAGE, EchoHeroType.JAPANESE_SAMURAI.baseAttackDamage())
				.add(Attributes.MOVEMENT_SPEED, EchoHeroType.JAPANESE_SAMURAI.baseMovementSpeed())
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, EchoHeroType.JAPANESE_SAMURAI.baseKnockbackResistance());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, ACTION_NONE);
		builder.define(ACTION_STARTED_AT, 0L);
		builder.define(ACTION_ENDS_AT, 0L);
		builder.define(ACTION_SPEED, 1.0F);
		builder.define(AFTERIMAGE_NEUTRAL, true);
		builder.define(AFTERIMAGE_ADVANCED, true);
		builder.define(AFTERIMAGE_OUTLINE, false);
		builder.define(AFTERIMAGE_SEQUENCE, 0);
		builder.define(AFTERIMAGE_KIND, (byte)0);
		builder.define(AFTERIMAGE_X, 0.0F);
		builder.define(AFTERIMAGE_Y, 0.0F);
		builder.define(AFTERIMAGE_Z, 0.0F);
		builder.define(AFTERIMAGE_DIRECTION_X, 0.0F);
		builder.define(AFTERIMAGE_DIRECTION_Z, 0.0F);
		builder.define(AFTERIMAGE_YAW, 0.0F);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
	}

	@Override
	public List<? extends ExtendedSensor<?>> getSensors(JapaneseSamuraiEchoEntity owner) {
		return List.of(new NearbyLivingEntitySensor<JapaneseSamuraiEchoEntity>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getAlwaysRunningBehaviours(JapaneseSamuraiEchoEntity owner) {
		return List.of(new MoveToWalkTarget<>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getIdleBehaviours(JapaneseSamuraiEchoEntity owner) {
		return List.of(new EchoFollowOwner<JapaneseSamuraiEchoEntity>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getFightingBehaviours(JapaneseSamuraiEchoEntity owner) {
		return List.of(
				new InvalidateAttackTarget<JapaneseSamuraiEchoEntity>(),
				new SetWalkTargetToAttackTarget<JapaneseSamuraiEchoEntity>()
						.speedModifier(1.0F)
						.closeEnoughDist(2)
		);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!(this.level() instanceof ServerLevel level)) return;
		LivingEntity owner = this.getOwner();
		if (!(owner instanceof Player player) || !owner.isAlive() || owner.level() != this.level()) {
			dismiss();
			return;
		}

		boolean hasSummoner = this.summonerUuid != null
				&& player.getInventory().contains(stack -> TestEchoSummonerItem.hasSummoner(stack, this.summonerUuid));
		this.missingSummonerTicks = hasSummoner ? 0 : this.missingSummonerTicks + 1;
		if (this.missingSummonerTicks > RomanLegionaryEchoEntity.SUMMONER_GRACE_TICKS) {
			dismiss();
			return;
		}

		long now = level.getGameTime();
		cleanupExpiredPins(now);
		ItemStack relic = currentRelic();
		if (!relic.isEmpty()) {
			EchoRelicState.fumikomiCharges(relic, now);
			if (action() != ACTION_NONE) tickAction(level, relic, now);
			else tryStartCombatAction(level, relic, now);
			if (this.tickCount % 20 == 0) {
				applyRelicState(relic, false);
				tickNaturalHealing(level, relic);
			}
		} else if (action() != ACTION_NONE) {
			finishAction(now);
		}

		if (this.tickCount % 5 == 0 && action() == ACTION_NONE) {
			LivingEntity target = selectProtectiveTarget(owner);
			if (target != null) BrainUtil.setTargetOfEntity(this, target);
			enforceActivityBoundary(owner);
		}
		if (this.tickCount % 20 == 0 && this.getTarget() != null) {
			EchoExperienceSystem.markParticipation(this, this.getTarget());
		}

		boolean actionOwned = action() != ACTION_NONE;
		if (actionOwned) stopMovementIntent();
		EchoActivityMovement.tick(level, this, this.activityMode, this.activityAnchor,
				this.getTarget() != null || actionOwned);
		EchoWaterSafety.tick(level, this, owner,
				this.activityMode == EchoRelicState.ActivityMode.FOLLOW && !actionOwned);
	}

	private void tryStartCombatAction(ServerLevel level, ItemStack relic, long now) {
		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive() || !this.canAttack(target)) return;
		double distance = horizontalDistance(this.position(), target.position());

		if (distance > NORMAL_MELEE_RANGE && distance <= FUMIKOMI_MAX_RANGE
				&& now >= this.fumikomiInternalCooldownUntil && this.hasLineOfSight(target)
				&& EchoRelicState.fumikomiCharges(relic, now) > 0
				&& canPrecheckDash(level, target)) {
			startFumikomi(level, relic, target, now, distance);
			return;
		}
		if (distance <= STAB_TRIGGER_RANGE && this.hasLineOfSight(target)
				&& now >= EchoRelicState.samuraiStabCooldownEnd(relic)) {
			startStab(relic, target, now);
			return;
		}
		if (distance <= NORMAL_MELEE_RANGE && this.hasLineOfSight(target) && now >= this.nextNormalAttackAt) {
			startNormalAttack(relic, target, now);
		}
	}

	private void startNormalAttack(ItemStack relic, LivingEntity target, long now) {
		int interval = relic.isEmpty() ? BASE_ATTACK_CYCLE_TICKS : EchoRelicState.attackIntervalTicks(relic);
		float speed = BASE_ATTACK_CYCLE_TICKS / (float)interval;
		int firstTicks = scaledAttackTicks(BASE_FIRST_TICKS, interval);
		this.nextNormalAttackAt = now + interval;
		this.lockedTargetUuid = target.getUUID();
		this.secondSlashTargets.clear();
		this.secondSlashPrimaryUuid = null;
		faceTargetImmediately(target);
		beginAttackTracking();
		setAction(ACTION_ATTACK_FIRST, now, firstTicks, now + firstTicks, speed, ATTACK_FIRST_TRIGGER);
	}

	private void startStab(ItemStack relic, LivingEntity target, long now) {
		EchoRelicState.setSamuraiStabCooldownEnd(relic, now + EchoRelicState.SAMURAI_STAB_COOLDOWN_TICKS);
		persistCurrentRelic(relic);
		this.lockedTargetUuid = target.getUUID();
		this.stabTargets.clear();
		this.stabDirectionLocked = false;
		this.stabYaw = this.yBodyRot;
		setAction(ACTION_STAB, now, STAB_ANIMATION_TICKS, now + STAB_FIRST_HIT_TICK, 1.0F, STAB_TRIGGER);
	}

	private void startFumikomi(ServerLevel level, ItemStack relic, LivingEntity target, long now, double distance) {
		if (!EchoRelicState.consumeFumikomiCharge(relic, now)) return;
		persistCurrentRelic(relic);
		double travel = Math.max(0.0, distance - FUMIKOMI_STOP_RANGE);
		int duration = Math.clamp((int)Math.ceil(travel / FUMIKOMI_SPEED), FUMIKOMI_MIN_TICKS, FUMIKOMI_MAX_TICKS);
		this.dashTargetUuid = target.getUUID();
		this.fumikomiAfterimageOrigin = this.position();
		this.fumikomiAfterimageStep = 0;
		setSpecialStepHeight(true);
		faceTargetImmediately(target);
		setAction(ACTION_DASH_FORWARD, now, duration, Long.MAX_VALUE,
				(40.0F / 3.0F) / duration, DASH_FORWARD_TRIGGER);
		emitFumikomiAfterimage(level, this.position());
	}

	private void startDashBackward(ServerLevel level, DamageSource source, long now) {
		Vec3 direction = dodgeDirectionAwayFrom(source);
		if (direction.horizontalDistanceSqr() < 1.0E-5) direction = this.getLookAngle().reverse().multiply(1.0, 0.0, 1.0).normalize();
		this.dashBackwardDirection = direction;
		this.dashBackwardTravelled = 0.0;
		setSpecialStepHeight(true);
		setAction(ACTION_DASH_BACKWARD, now, DASH_BACKWARD_TICKS, Long.MAX_VALUE,
				(40.0F / 3.0F) / DASH_BACKWARD_TICKS, DASH_BACKWARD_TRIGGER);
		emitZanshinResidual(level, this.position(), direction, false);
	}

	private void tickAction(ServerLevel level, ItemStack relic, long now) {
		stopMovementIntent();
		switch (action()) {
			case ACTION_ATTACK_FIRST -> tickAttackFirst(level, relic, now);
			case ACTION_ATTACK_RECOVER -> {
				if (now >= actionEndsAt()) finishAction(now);
			}
			case ACTION_ATTACK_FOLLOW -> tickAttackFollow(level, now);
			case ACTION_STAB -> tickStab(level, now);
			case ACTION_DASH_FORWARD -> tickFumikomi(level, now);
			case ACTION_DASH_BACKWARD -> tickDashBackward(level, now);
			case ACTION_HURT -> {
				if (now >= actionEndsAt()) finishAction(now);
			}
			default -> finishAction(now);
		}
	}

	private void tickAttackFirst(ServerLevel level, ItemStack relic, long now) {
		if (!this.actionHitResolved && now <= this.actionHitAt) {
			tickAttackTracking(level, this.lockedTargetUuid, FIRST_SLASH_TRACKING_SPEED,
					FIRST_SLASH_TRACKING_LIMIT, now);
		}
		if (!this.actionHitResolved && now >= this.actionHitAt) {
			this.actionHitResolved = true;
			performSlash(level, FIRST_SLASH_RADIUS, FIRST_SLASH_ANGLE,
					this.lockedTargetUuid, null, 1.0F, 0.5F, 0.08, true);
			List<LivingEntity> followTargets = targetsInSector(level, SECOND_SLASH_RADIUS, SECOND_SLASH_ANGLE);
			if (followTargets.isEmpty()) {
				int interval = relic.isEmpty() ? BASE_ATTACK_CYCLE_TICKS : EchoRelicState.attackIntervalTicks(relic);
				int duration = scaledAttackTicks(BASE_RECOVER_TICKS, interval);
				setAction(ACTION_ATTACK_RECOVER, now, duration, Long.MAX_VALUE,
						BASE_ATTACK_CYCLE_TICKS / (float)interval, ATTACK_RECOVER_TRIGGER);
				return;
			}
			this.secondSlashTargets.clear();
			for (LivingEntity target : followTargets) this.secondSlashTargets.add(target.getUUID());
			LivingEntity locked = resolveLiving(level, this.lockedTargetUuid);
			this.secondSlashPrimaryUuid = locked != null && followTargets.contains(locked)
					? locked.getUUID() : followTargets.getFirst().getUUID();
			int interval = relic.isEmpty() ? BASE_ATTACK_CYCLE_TICKS : EchoRelicState.attackIntervalTicks(relic);
			int duration = scaledAttackTicks(BASE_FOLLOW_TICKS, interval);
			int hit = scaledAttackTicks(BASE_FOLLOW_HIT_TICKS, interval);
			beginAttackTracking();
			setAction(ACTION_ATTACK_FOLLOW, now, duration, now + hit,
					BASE_ATTACK_CYCLE_TICKS / (float)interval, ATTACK_FOLLOW_TRIGGER);
		}
	}

	private void tickAttackFollow(ServerLevel level, long now) {
		if (!this.actionHitResolved && now <= this.actionHitAt) {
			tickAttackTracking(level, this.secondSlashPrimaryUuid, SECOND_SLASH_TRACKING_SPEED,
					SECOND_SLASH_TRACKING_LIMIT, now);
		}
		if (!this.actionHitResolved && now >= this.actionHitAt) {
			this.actionHitResolved = true;
			performSlash(level, SECOND_SLASH_RADIUS, SECOND_SLASH_ANGLE,
					this.secondSlashPrimaryUuid, List.copyOf(this.secondSlashTargets), 1.0F, 0.5F, 0.28, false);
		}
		if (now >= actionEndsAt()) finishAction(now);
	}

	private void tickStab(ServerLevel level, long now) {
		long elapsed = now - actionStartedAt();
		if (!this.stabTargets.isEmpty() && elapsed < STAB_SECOND_HIT_TICK) {
			for (Map.Entry<UUID, Vec3> entry : this.stabTargets.entrySet()) {
				LivingEntity pinned = resolveLiving(level, entry.getKey());
				if (pinned != null && pinned.isAlive()) freezePinnedTarget(pinned, entry.getValue());
			}
		}
		if (!this.stabDirectionLocked && elapsed < STAB_FIRST_HIT_TICK) {
			LivingEntity target = resolveLiving(level, this.lockedTargetUuid);
			if (target != null && target.isAlive()) turnToward(target, 20.0F);
		}
		if (!this.stabDirectionLocked && elapsed >= STAB_FIRST_HIT_TICK) {
			this.stabDirectionLocked = true;
			this.stabYaw = this.yBodyRot;
			performStabFirstHit(level, now);
		}
		if (!this.actionHitResolved && elapsed >= STAB_SECOND_HIT_TICK) {
			this.actionHitResolved = true;
			performStabSecondHit(level);
		}
		if (now >= actionEndsAt()) finishAction(now);
	}

	private void tickFumikomi(ServerLevel level, long now) {
		LivingEntity target = resolveLiving(level, this.dashTargetUuid);
		if (target == null || !target.isAlive() || !this.canAttack(target) || !this.hasLineOfSight(target)) {
			finishAction(now);
			return;
		}
		turnToward(target, 15.0F);
		Vec3 delta = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
		double distance = delta.length();
		if (distance <= FUMIKOMI_STOP_RANGE || now >= actionEndsAt()) {
			finishAction(now);
			return;
		}
		double step = Math.min(FUMIKOMI_SPEED, distance - FUMIKOMI_STOP_RANGE);
		Vec3 before = this.position();
		if (step <= 0.0 || !moveSpecial(level, delta.normalize().scale(step))) {
			finishAction(now);
			return;
		}
		// The client interpolates the body between its previous and latest server
		// positions. Only sample the previous position, which the body has visibly
		// traversed already, so a trail snapshot can never appear ahead of it.
		double visiblyTravelled = horizontalDistance(this.fumikomiAfterimageOrigin, before);
		while (this.fumikomiAfterimageStep < 4
				&& visiblyTravelled >= (this.fumikomiAfterimageStep + 1) * 1.25) {
			this.fumikomiAfterimageStep++;
			emitFumikomiAfterimage(level, before);
		}
	}

	private void tickDashBackward(ServerLevel level, long now) {
		if (now >= actionEndsAt() || this.dashBackwardTravelled >= DODGE_BACKSTEP_DISTANCE - 0.01) {
			finishAction(now);
			return;
		}
		double remaining = DODGE_BACKSTEP_DISTANCE - this.dashBackwardTravelled;
		double step = Math.min(DODGE_BACKSTEP_DISTANCE / DASH_BACKWARD_TICKS, remaining);
		Vec3 before = this.position();
		if (!moveSpecial(level, this.dashBackwardDirection.scale(step))) {
			finishAction(now);
			return;
		}
		this.dashBackwardTravelled += horizontalDistance(before, this.position());
		int elapsed = (int)(now - actionStartedAt());
		if (elapsed == 5 || elapsed == 10) emitZanshinResidual(level, before, this.dashBackwardDirection, false);
	}

	private void performSlash(
			ServerLevel level,
			double radius,
			double angle,
			@Nullable UUID primaryUuid,
			@Nullable List<UUID> allowedTargets,
			float primaryMultiplier,
			float secondaryMultiplier,
			double knockbackStrength,
			boolean suppressVanillaKnockback
	) {
		List<LivingEntity> targets = targetsInSector(level, radius, angle);
		if (allowedTargets != null) targets = targets.stream().filter(target -> allowedTargets.contains(target.getUUID())).toList();
		boolean damaged = false;
		for (LivingEntity target : targets) {
			boolean primary = primaryUuid != null && primaryUuid.equals(target.getUUID());
			float multiplier = primary ? primaryMultiplier : secondaryMultiplier;
			if (dealDamage(level, target, multiplier, suppressVanillaKnockback)) {
				damaged = true;
				applyLightKnockback(target, knockbackStrength);
			}
		}
		if (damaged) grantAttackZanshin(level.getGameTime());
		level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
				SoundSource.PLAYERS, 0.55F, angle > 100.0 ? 0.88F : 1.08F);
		Vec3 particle = this.position().add(facing(this.yBodyRot).scale(radius * 0.65)).add(0.0, 1.0, 0.0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, particle.x, particle.y, particle.z, 1, 0.15, 0.15, 0.15, 0.0);
	}

	private void performStabFirstHit(ServerLevel level, long now) {
		this.stabTargets.clear();
		boolean damaged = false;
		for (LivingEntity target : targetsInStabCapsule(level, this.stabYaw)) {
			Vec3 stored = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			if (dealDamage(level, target, 1.5F)) {
				damaged = true;
				this.stabTargets.put(target.getUUID(), stored);
				PINNED_UNTIL.put(target.getUUID(), now + 8L);
				PINNED_CENTERS.put(target.getUUID(), stored);
				freezePinnedTarget(target, stored);
			}
		}
		if (damaged) grantAttackZanshin(now);
		level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
				SoundSource.PLAYERS, 0.65F, 0.92F);
	}

	private void performStabSecondHit(ServerLevel level) {
		boolean damaged = false;
		for (Map.Entry<UUID, Vec3> entry : this.stabTargets.entrySet()) {
			LivingEntity target = resolveLiving(level, entry.getKey());
			Vec3 bloodPosition = target != null ? target.position().add(0.0, target.getBbHeight() * 0.55, 0.0) : entry.getValue();
			if (target != null && target.isAlive() && this.canAttack(target) && dealDamage(level, target, 3.0F)) {
				damaged = true;
				applyLightKnockback(target, 0.32);
			}
			PINNED_UNTIL.remove(entry.getKey());
			PINNED_CENTERS.remove(entry.getKey());
			level.sendParticles(BLOOD, bloodPosition.x, bloodPosition.y, bloodPosition.z,
					12, 0.28, 0.35, 0.28, 0.03);
		}
		if (damaged) grantAttackZanshin(level.getGameTime());
		level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
				SoundSource.PLAYERS, 0.75F, 0.78F);
	}

	private boolean dealDamage(ServerLevel level, LivingEntity target, float multiplier) {
		return dealDamage(level, target, multiplier, false);
	}

	private boolean dealDamage(
			ServerLevel level,
			LivingEntity target,
			float multiplier,
			boolean suppressVanillaKnockback
	) {
		if (!target.isAlive() || !this.canAttack(target)) return false;
		float previousHealth = target.getHealth();
		float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier;
		DamageSource source = suppressVanillaKnockback
				? level.damageSources().source(ModDamageTypes.SAMURAI_FIRST_SLASH, this)
				: level.damageSources().mobAttack(this);
		boolean hurt = target.hurtServer(level, source, damage);
		return hurt && target.getHealth() < previousHealth;
	}

	private void grantAttackZanshin(long now) {
		this.zanshinBonusUntil = now + ZANSHIN_ATTACK_BONUS_TICKS;
	}

	private List<LivingEntity> targetsInSector(ServerLevel level, double radius, double angleDegrees) {
		Vec3 forward = facing(this.yBodyRot);
		double cosine = Math.cos(Math.toRadians(angleDegrees * 0.5));
		return level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius, 2.0, radius), target -> {
			if (!isValidCombatEnemy(target) || !this.hasLineOfSight(target)) return false;
			Vec3 delta = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
			double horizontal = delta.length();
			if (horizontal > radius + target.getBbWidth() * 0.5 || horizontal < 1.0E-5) return false;
			return forward.dot(delta.scale(1.0 / horizontal)) >= cosine;
		}).stream().sorted(Comparator.comparingDouble(this::distanceToSqr)).toList();
	}

	private List<LivingEntity> targetsInStabCapsule(ServerLevel level, float yaw) {
		Vec3 start = this.position().add(0.0, this.getBbHeight() * 0.55, 0.0);
		Vec3 end = start.add(facing(yaw).scale(STAB_CAPSULE_LENGTH));
		AABB search = this.getBoundingBox().expandTowards(facing(yaw).scale(STAB_CAPSULE_LENGTH)).inflate(0.75, 1.0, 0.75);
		return level.getEntitiesOfClass(LivingEntity.class, search, target -> {
			if (!isValidCombatEnemy(target) || !this.hasLineOfSight(target)) return false;
			Vec3 point = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
			double allowance = STAB_CAPSULE_RADIUS + target.getBbWidth() * 0.5;
			return distanceToSegmentSqr(point, start, end) <= allowance * allowance;
		}).stream().sorted(Comparator.comparingDouble(target -> projectionAlong(
				target.position().add(0.0, target.getBbHeight() * 0.5, 0.0), start, end))).toList();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		Entity attacker = source.getEntity();
		if (attacker == this.getOwner()
				|| attacker instanceof EchoWarriorEntity echo && echo.getOwner() == this.getOwner()) return false;
		long now = level.getGameTime();
		if (action() == ACTION_DASH_FORWARD && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;

		if (isEligibleDodgeEvent(level, source, damage)) {
			float chance = zanshinChance(now);
			if (chance > 0.0F && this.random.nextFloat() < chance) {
				onSuccessfulDodge(level, source, now);
				return false;
			}
		}

		float previousHealth = this.getHealth();
		boolean wasIdle = action() == ACTION_NONE;
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && this.getHealth() < previousHealth) {
			if (wasIdle) {
				setAction(ACTION_HURT, now, HURT_TICKS, Long.MAX_VALUE, 1.0F, HURT_TRIGGER);
			}
			if (attacker instanceof LivingEntity living && canProtectAgainst(living)) {
				BrainUtil.setTargetOfEntity(this, living);
			}
		}
		return hurt;
	}

	private boolean isEligibleDodgeEvent(ServerLevel level, DamageSource source, float damage) {
		if (damage <= 0.0F || this.isDeadOrDying() || this.isInvulnerableTo(level, source)) return false;
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
		if (this.invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN) && damage <= this.lastHurt) return false;
		return !source.is(DamageTypes.IN_FIRE)
				&& !source.is(DamageTypes.CAMPFIRE)
				&& !source.is(DamageTypes.ON_FIRE)
				&& !source.is(DamageTypes.LAVA)
				&& !source.is(DamageTypes.HOT_FLOOR)
				&& !source.is(DamageTypes.IN_WALL)
				&& !source.is(DamageTypes.CRAMMING)
				&& !source.is(DamageTypes.DROWN)
				&& !source.is(DamageTypes.STARVE)
				&& !source.is(DamageTypes.FALL)
				&& !source.is(DamageTypes.FLY_INTO_WALL)
				&& !source.is(DamageTypes.FELL_OUT_OF_WORLD)
				&& !source.is(DamageTypes.MAGIC)
				&& !source.is(DamageTypes.WITHER)
				&& !source.is(DamageTypes.DRY_OUT)
				&& !source.is(DamageTypes.FREEZE)
				&& !source.is(DamageTypes.OUTSIDE_BORDER)
				&& !source.is(DamageTypes.GENERIC_KILL)
				&& !source.is(ModDamageTypes.BLEEDING)
				&& !source.is(ModDamageTypes.OBSIDIAN_WOUND);
	}

	private float zanshinChance(long now) {
		float denominator = Math.max(1.0F, this.getMaxHealth() - 1.0F);
		float base = Math.clamp((this.getMaxHealth() - this.getHealth()) / denominator, 0.0F, 1.0F) * ZANSHIN_BASE_CAP;
		float bonus = now < this.zanshinBonusUntil ? ZANSHIN_ATTACK_BONUS : 0.0F;
		return Math.min(ZANSHIN_FINAL_CAP, base + bonus);
	}

	private void onSuccessfulDodge(ServerLevel level, DamageSource source, long now) {
		ItemStack relic = currentRelic();
		if (!relic.isEmpty() && EchoRelicState.addFumikomiCharge(relic, now)) persistCurrentRelic(relic);
		advanceDodgedProjectile(source);
		if (action() == ACTION_NONE) {
			startDashBackward(level, source, now);
		} else if (action() == ACTION_DASH_BACKWARD) {
			emitZanshinResidual(level, this.position(), this.dashBackwardDirection, true);
		} else {
			Vec3 direction = selectPhantomDodgeDirection(level, source);
			emitZanshinResidual(level, this.position(), direction, true);
		}
		level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
				SoundSource.PLAYERS, 0.55F, 1.55F);
	}

	private void advanceDodgedProjectile(DamageSource source) {
		if (!(source.getDirectEntity() instanceof Projectile projectile)) return;
		Vec3 velocity = projectile.getDeltaMovement();
		if (velocity.lengthSqr() < 1.0E-5) return;
		Vec3 advanced = this.position().add(velocity.normalize().scale(this.getBbWidth() + 1.0));
		projectile.setPos(advanced);
	}

	private Vec3 selectPhantomDodgeDirection(ServerLevel level, DamageSource source) {
		Vec3 backward = this.getLookAngle().reverse().multiply(1.0, 0.0, 1.0).normalize();
		Vec3 left = new Vec3(-backward.z, 0.0, backward.x);
		Vec3 right = left.reverse();
		Vec3 sourcePosition = source.getSourcePosition();
		Vec3 best = backward;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (Vec3 candidate : List.of(backward, left, right)) {
			if (safeSpecialDestination(level, this.position().add(candidate.scale(1.05))) == null) continue;
			double score = sourcePosition == null ? candidate.dot(backward)
					: this.position().add(candidate.scale(1.05)).distanceToSqr(sourcePosition);
			if (score > bestScore) {
				bestScore = score;
				best = candidate;
			}
		}
		return best;
	}

	private Vec3 dodgeDirectionAwayFrom(DamageSource source) {
		Vec3 sourcePosition = source.getSourcePosition();
		if (sourcePosition == null) return this.getLookAngle().reverse().multiply(1.0, 0.0, 1.0).normalize();
		Vec3 away = this.position().subtract(sourcePosition).multiply(1.0, 0.0, 1.0);
		return away.horizontalDistanceSqr() < 1.0E-5 ? Vec3.ZERO : away.normalize();
	}

	private void emitZanshinResidual(ServerLevel level, Vec3 origin, Vec3 direction, boolean phantom) {
		syncAfterimageEvent(phantom ? AFTERIMAGE_ZANSHIN_PHANTOM : AFTERIMAGE_ZANSHIN_REAL, origin, direction);
		for (int stage = 0; stage < 3; stage++) {
			double distance = phantom ? 0.35 * (stage + 1) : 0.18 * stage;
			Vec3 point = origin.add(direction.scale(distance)).add(0.0, 0.95, 0.0);
			if (safeSpecialDestination(level, point.add(0.0, -0.95, 0.0)) == null && phantom) continue;
			if (isAfterimageNeutral()) {
				level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 5 - stage,
						0.18, 0.45, 0.18, 0.01);
			} else {
				level.sendParticles(CYAN_AFTERIMAGE, point.x, point.y, point.z, 6 - stage,
						0.20, 0.50, 0.20, 0.01);
			}
		}
	}

	private void emitFumikomiAfterimage(ServerLevel level, Vec3 origin) {
		syncAfterimageEvent(AFTERIMAGE_FUMIKOMI, origin, facing(this.yBodyRot));
		Vec3 point = origin.add(0.0, 0.95, 0.0);
		if (isAfterimageNeutral()) {
			level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 8, 0.22, 0.52, 0.22, 0.01);
		} else {
			level.sendParticles(GOLD_AFTERIMAGE, point.x, point.y, point.z, 9, 0.22, 0.52, 0.22, 0.01);
		}
		level.sendParticles(ParticleTypes.CLOUD, origin.x, origin.y + 0.08, origin.z, 3, 0.18, 0.04, 0.18, 0.01);
		level.playSound(null, this.blockPosition(), SoundEvents.WIND_CHARGE_THROW,
				SoundSource.PLAYERS, 0.28F, 1.35F);
	}

	private void syncAfterimageEvent(byte kind, Vec3 origin, Vec3 direction) {
		Vec3 horizontal = direction.multiply(1.0, 0.0, 1.0);
		if (horizontal.horizontalDistanceSqr() > 1.0E-5) horizontal = horizontal.normalize();
		this.entityData.set(AFTERIMAGE_KIND, kind);
		this.entityData.set(AFTERIMAGE_X, (float)origin.x);
		this.entityData.set(AFTERIMAGE_Y, (float)origin.y);
		this.entityData.set(AFTERIMAGE_Z, (float)origin.z);
		this.entityData.set(AFTERIMAGE_DIRECTION_X, (float)horizontal.x);
		this.entityData.set(AFTERIMAGE_DIRECTION_Z, (float)horizontal.z);
		this.entityData.set(AFTERIMAGE_YAW, this.yBodyRot);
		this.entityData.set(AFTERIMAGE_SEQUENCE, this.entityData.get(AFTERIMAGE_SEQUENCE) + 1);
	}

	private boolean canPrecheckDash(ServerLevel level, LivingEntity target) {
		Vec3 delta = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
		double total = Math.max(0.0, delta.length() - FUMIKOMI_STOP_RANGE);
		if (total <= 0.0 || delta.horizontalDistanceSqr() < 1.0E-5) return false;
		Vec3 direction = delta.normalize();
		Vec3 cursor = this.position();
		for (double travelled = 0.5; travelled <= total + 1.0E-5; travelled += 0.5) {
			Vec3 desired = cursor.add(direction.scale(Math.min(0.5, total - travelled + 0.5)));
			Vec3 safe = safeSpecialDestination(level, desired);
			if (safe == null) return false;
			cursor = safe;
		}
		return true;
	}

	private boolean moveSpecial(ServerLevel level, Vec3 horizontalDelta) {
		Vec3 safe = safeSpecialDestination(level, this.position().add(horizontalDelta));
		if (safe == null) return false;
		this.snapTo(safe.x, safe.y, safe.z, this.getYRot(), this.getXRot());
		this.setDeltaMovement(Vec3.ZERO);
		return true;
	}

	private void beginAttackTracking() {
		this.attackTrackingTravelled = 0.0;
		this.attackTrackingStartYaw = this.yBodyRot;
		this.attackTrackingStopped = false;
	}

	private void tickAttackTracking(
			ServerLevel level,
			@Nullable UUID targetUuid,
			double speed,
			double travelLimit,
			long now
	) {
		if (this.attackTrackingStopped || targetUuid == null) return;
		LivingEntity target = resolveLiving(level, targetUuid);
		if (target == null || !target.isAlive() || !this.canAttack(target) || !this.hasLineOfSight(target)) {
			this.attackTrackingStopped = true;
			return;
		}

		float desiredYaw = yawToward(this.getX(), this.getZ(), target.getX(), target.getZ());
		float totalTurn = Mth.wrapDegrees(desiredYaw - this.attackTrackingStartYaw);
		if (Math.abs(totalTurn) > ATTACK_TRACKING_TOTAL_TURN) {
			this.attackTrackingStopped = true;
			return;
		}
		float turn = Mth.clamp(Mth.wrapDegrees(desiredYaw - this.yBodyRot),
				-ATTACK_TRACKING_TURN_PER_TICK, ATTACK_TRACKING_TURN_PER_TICK);
		float yaw = this.yBodyRot + turn;
		this.setYRot(yaw);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);

		if (this.attackTrackingTravelled >= travelLimit) return;
		Vec3 delta = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
		double distance = delta.length();
		if (distance <= ATTACK_TRACKING_STOP_RANGE || distance < 1.0E-5) return;
		double step = Math.min(speed, Math.min(
				travelLimit - this.attackTrackingTravelled,
				distance - ATTACK_TRACKING_STOP_RANGE));
		if (step <= 1.0E-5) return;

		Vec3 before = this.position();
		if (!moveSpecial(level, delta.scale(step / distance))) {
			this.attackTrackingStopped = true;
			return;
		}
		this.attackTrackingTravelled += horizontalDistance(before, this.position());
		if (((now - actionStartedAt()) & 1L) == 0L) {
			level.sendParticles(ParticleTypes.CLOUD, before.x, before.y + 0.08, before.z,
					1, 0.10, 0.025, 0.10, 0.004);
		}
	}

	private @Nullable Vec3 safeSpecialDestination(ServerLevel level, Vec3 desired) {
		for (double dy : new double[] {0.0, 1.0, -1.0}) {
			Vec3 candidate = new Vec3(desired.x, this.getY() + dy, desired.z);
			AABB moved = this.getBoundingBox().move(candidate.subtract(this.position()));
			if (!level.noCollision(this, moved) || !hasSafeSupport(level, candidate)) continue;
			return candidate;
		}
		return null;
	}

	private static boolean hasSafeSupport(ServerLevel level, Vec3 candidate) {
		BlockPos feet = BlockPos.containing(candidate);
		BlockPos floor = feet.below();
		return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
				&& level.getFluidState(feet).isEmpty()
				&& level.getFluidState(feet.above()).isEmpty();
	}

	private void setAction(byte action, long now, int duration, long hitAt, float speed, String trigger) {
		stopAllActionTriggers();
		this.entityData.set(ACTION, action);
		this.entityData.set(ACTION_STARTED_AT, now);
		this.entityData.set(ACTION_ENDS_AT, now + duration);
		this.entityData.set(ACTION_SPEED, speed);
		this.actionHitAt = hitAt;
		this.actionHitResolved = false;
		stopMovementIntent();
		this.triggerAnim(ACTION_CONTROLLER, trigger);
	}

	private void finishAction(long now) {
		byte previous = action();
		if (previous == ACTION_STAB) releaseStabPins();
		if (previous == ACTION_DASH_FORWARD || previous == ACTION_DASH_BACKWARD) setSpecialStepHeight(false);
		if (previous == ACTION_DASH_FORWARD) this.fumikomiInternalCooldownUntil = now + FUMIKOMI_INTERNAL_COOLDOWN_TICKS;
		stopAllActionTriggers();
		this.entityData.set(ACTION, ACTION_NONE);
		this.entityData.set(ACTION_STARTED_AT, 0L);
		this.entityData.set(ACTION_ENDS_AT, 0L);
		this.entityData.set(ACTION_SPEED, 1.0F);
		this.actionHitAt = Long.MAX_VALUE;
		this.actionHitResolved = false;
		this.lockedTargetUuid = null;
		this.secondSlashTargets.clear();
		this.secondSlashPrimaryUuid = null;
		this.dashTargetUuid = null;
		this.dashBackwardDirection = Vec3.ZERO;
		this.stabDirectionLocked = false;
		this.stabTargets.clear();
		this.attackTrackingTravelled = 0.0;
		this.attackTrackingStopped = true;
	}

	private void stopAllActionTriggers() {
		this.stopTriggeredAnim(ACTION_CONTROLLER, ATTACK_FIRST_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, ATTACK_RECOVER_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, ATTACK_FOLLOW_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, STAB_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, DASH_FORWARD_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, DASH_BACKWARD_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, HURT_TRIGGER);
	}

	private void releaseStabPins() {
		for (UUID uuid : this.stabTargets.keySet()) {
			PINNED_UNTIL.remove(uuid);
			PINNED_CENTERS.remove(uuid);
		}
	}

	private static void cleanupExpiredPins(long now) {
		PINNED_UNTIL.entrySet().removeIf(entry -> {
			if (entry.getValue() > now) return false;
			PINNED_CENTERS.remove(entry.getKey());
			return true;
		});
	}

	public static boolean isTemporarilyPinned(LivingEntity entity) {
		Long until = PINNED_UNTIL.get(entity.getUUID());
		return until != null && entity.level().getGameTime() < until;
	}

	private static void freezePinnedTarget(LivingEntity target, Vec3 storedCenter) {
		Vec3 center = PINNED_CENTERS.getOrDefault(target.getUUID(), storedCenter);
		target.snapTo(center.x, center.y - target.getBbHeight() * 0.5, center.z, target.getYRot(), target.getXRot());
		target.setDeltaMovement(Vec3.ZERO);
		if (target instanceof net.minecraft.world.entity.Mob mob) {
			mob.getNavigation().stop();
			mob.setTarget(null);
		}
	}

	private void setSpecialStepHeight(boolean enabled) {
		var attribute = this.getAttribute(Attributes.STEP_HEIGHT);
		if (attribute == null) return;
		attribute.removeModifier(SPECIAL_STEP_ID);
		if (enabled) attribute.addTransientModifier(SPECIAL_STEP);
	}

	private void stopMovementIntent() {
		this.getNavigation().stop();
		this.getMoveControl().setWait();
		this.setSpeed(0.0F);
		this.setXxa(0.0F);
		this.setZza(0.0F);
		this.setDeltaMovement(Vec3.ZERO);
		BrainUtil.clearMemory(this, net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
	}

	private void faceTargetImmediately(LivingEntity target) {
		float yaw = yawToward(this.getX(), this.getZ(), target.getX(), target.getZ());
		this.setYRot(yaw);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);
	}

	private void turnToward(LivingEntity target, float maxDegrees) {
		float desired = yawToward(this.getX(), this.getZ(), target.getX(), target.getZ());
		float delta = Mth.wrapDegrees(desired - this.yBodyRot);
		float yaw = this.yBodyRot + Mth.clamp(delta, -maxDegrees, maxDegrees);
		this.setYRot(yaw);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);
	}

	private void applyLightKnockback(LivingEntity target, double strength) {
		Vec3 direction = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
		if (direction.horizontalDistanceSqr() > 1.0E-5) target.knockback(strength, -direction.x, -direction.z);
	}

	private @Nullable LivingEntity selectProtectiveTarget(LivingEntity owner) {
		LivingEntity ownAttacker = this.getLastHurtByMob();
		if (isRecent(this, this.getLastHurtByMobTimestamp()) && canProtectAgainst(ownAttacker)) return ownAttacker;
		if (this.alertMode == EchoRelicState.AlertMode.PEACEFUL) return null;
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isRecent(owner, owner.getLastHurtByMobTimestamp()) && canProtectAgainst(ownerAttacker)) return ownerAttacker;
		LivingEntity ownerTarget = owner.getLastHurtMob();
		if (isRecent(owner, owner.getLastHurtMobTimestamp()) && canProtectAgainst(ownerTarget)) return ownerTarget;
		if (this.alertMode == EchoRelicState.AlertMode.AGGRESSIVE) {
			double range = this.activityMode == EchoRelicState.ActivityMode.WAIT ? 6.0 : 16.0;
			AABB box = this.activityMode == EchoRelicState.ActivityMode.WAIT
					? new AABB(this.activityAnchor.x - range, this.activityAnchor.y - 4.0, this.activityAnchor.z - range,
						this.activityAnchor.x + range, this.activityAnchor.y + 4.0, this.activityAnchor.z + range)
					: this.getBoundingBox().inflate(range);
			return this.level().getEntitiesOfClass(Monster.class, box, this::canProtectAgainst).stream()
					.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
		}
		return null;
	}

	private static boolean isRecent(LivingEntity source, int timestamp) {
		return timestamp > 0 && source.tickCount - timestamp <= 100;
	}

	private boolean canProtectAgainst(@Nullable LivingEntity target) {
		if (target == null || !target.isAlive() || this.distanceToSqr(target) > 1024.0 || !this.canAttack(target)) return false;
		if (this.activityMode == EchoRelicState.ActivityMode.WAIT) return target.position().distanceToSqr(this.activityAnchor) <= 64.0;
		if (this.activityMode == EchoRelicState.ActivityMode.WANDER) return target.position().distanceToSqr(this.activityAnchor) <= 256.0;
		return true;
	}

	private boolean isValidCombatEnemy(LivingEntity target) {
		if (!this.canAttack(target)) return false;
		if (target == this.getTarget() || target instanceof Monster) return true;
		if (target == this.getLastHurtByMob() && isRecent(this, this.getLastHurtByMobTimestamp())) return true;
		LivingEntity owner = this.getOwner();
		return owner != null && (
				target == owner.getLastHurtByMob() && isRecent(owner, owner.getLastHurtByMobTimestamp())
						|| target == owner.getLastHurtMob() && isRecent(owner, owner.getLastHurtMobTimestamp())
		);
	}

	private void enforceActivityBoundary(LivingEntity owner) {
		LivingEntity target = this.getTarget();
		Vec3 center = this.activityMode == EchoRelicState.ActivityMode.FOLLOW ? owner.position() : this.activityAnchor;
		double giveUp = this.activityMode == EchoRelicState.ActivityMode.WAIT ? 8.0
				: this.activityMode == EchoRelicState.ActivityMode.WANDER ? 24.0 : 32.0;
		if (target != null && target.position().distanceToSqr(center) > giveUp * giveUp) {
			BrainUtil.clearMemory(this, net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
			this.setTarget(null);
		}
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		LivingEntity owner = this.getOwner();
		if (target == this || target == owner || target.isAlliedTo(this) || owner != null && owner.isAlliedTo(target)) return false;
		if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
		if (target instanceof EchoWarriorEntity echo && owner != null && owner == echo.getOwner()) return false;
		if (target instanceof OwnableEntity ownable && owner != null && ownable.getRootOwner() == owner) return false;
		return super.canAttack(target);
	}

	@Override
	protected boolean considersEntityAsAlly(Entity other) {
		LivingEntity owner = this.getOwner();
		if (other == owner) return true;
		if (other instanceof EchoWarriorEntity echo && owner != null && owner == echo.getOwner()) return true;
		return owner != null && owner.isAlliedTo(other) || super.considersEntityAsAlly(other);
	}

	@Override
	public boolean isPushable() {
		return action() != ACTION_DASH_FORWARD && action() != ACTION_DASH_BACKWARD && super.isPushable();
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	private void tickNaturalHealing(ServerLevel level, ItemStack relic) {
		long now = level.getGameTime();
		if (this.getHealth() >= this.getMaxHealth() || this.getTarget() != null || action() != ACTION_NONE
				|| this.tickCount - this.getLastHurtByMobTimestamp() < 100 || now - this.lastNaturalHealAt < 40L) return;
		LivingEntity owner = this.getOwner();
		if (!(owner instanceof Player player) || this.summonerUuid == null) return;
		ItemStack summoner = TestEchoSummonerItem.findSummonerStack(player, this.summonerUuid);
		if (summoner.isEmpty() || !SummonerFuel.consumeFractional(summoner, SummonerFuel.healCost(relic))) return;
		this.heal(1.0F);
		this.lastNaturalHealAt = now;
		level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 2, 0.15, 0.3, 0.15, 0.0);
	}

	@Override
	public void bindTo(Player owner, UUID summonerUuid) {
		this.ownerReference = EntityReference.of(owner);
		this.summonerUuid = summonerUuid;
		this.missingSummonerTicks = 0;
		this.activityAnchor = this.position();
	}

	@Override
	public void applyRelicState(ItemStack relic, boolean resetAnchor) {
		if (relic.isEmpty()) return;
		EchoRelicState.ActivityMode previousActivity = this.activityMode;
		EchoRelicState.AlertMode previousAlert = this.alertMode;
		this.activityMode = EchoRelicState.activityMode(relic);
		this.alertMode = EchoRelicState.alertMode(relic);
		this.enabledSkills = EchoRelicState.enabledSkills(relic);
		if (previousActivity != this.activityMode || previousAlert != this.alertMode || resetAnchor) {
			this.setTarget(null);
			BrainUtil.clearMemory(this, net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
		}
		if (previousActivity != this.activityMode || resetAnchor) EchoActivityMovement.reset(this);
		if (resetAnchor || this.activityAnchor == Vec3.ZERO) this.activityAnchor = this.position();
		double oldMaximum = this.getMaxHealth();
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(EchoRelicState.maximumHealth(relic));
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(EchoRelicState.attackDamage(relic));
		this.getAttribute(Attributes.ARMOR).setBaseValue(EchoRelicState.armor(relic));
		this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(EchoRelicState.movementSpeed(relic));
		this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(EchoRelicState.knockbackResistance(relic));
		if (this.getHealth() >= oldMaximum - 0.01F) this.setHealth(this.getMaxHealth());
		else if (this.getHealth() > this.getMaxHealth()) this.setHealth(this.getMaxHealth());
	}

	private ItemStack currentRelic() {
		LivingEntity owner = this.getOwner();
		if (!(owner instanceof Player player) || this.summonerUuid == null) return ItemStack.EMPTY;
		return TestEchoSummonerItem.relicStack(TestEchoSummonerItem.findSummonerStack(player, this.summonerUuid));
	}

	private void persistCurrentRelic(ItemStack relic) {
		LivingEntity owner = this.getOwner();
		if (!(owner instanceof Player player) || this.summonerUuid == null) return;
		ItemStack summoner = TestEchoSummonerItem.findSummonerStack(player, this.summonerUuid);
		if (!summoner.isEmpty()) TestEchoSummonerItem.setRelicStack(summoner, relic);
	}

	public byte action() { return this.entityData.get(ACTION); }
	public long actionStartedAt() { return this.entityData.get(ACTION_STARTED_AT); }
	public long actionEndsAt() { return this.entityData.get(ACTION_ENDS_AT); }
	public float actionAnimationSpeed() { return this.entityData.get(ACTION_SPEED); }
	public boolean isAfterimageNeutral() { return this.entityData.get(AFTERIMAGE_NEUTRAL); }
	public boolean isAfterimageAdvanced() { return this.entityData.get(AFTERIMAGE_ADVANCED); }
	public boolean isAfterimageOutline() { return this.entityData.get(AFTERIMAGE_OUTLINE); }
	public int afterimageSequence() { return this.entityData.get(AFTERIMAGE_SEQUENCE); }
	public byte afterimageKind() { return this.entityData.get(AFTERIMAGE_KIND); }
	public Vec3 afterimageOrigin() { return new Vec3(
			this.entityData.get(AFTERIMAGE_X), this.entityData.get(AFTERIMAGE_Y), this.entityData.get(AFTERIMAGE_Z)); }
	public Vec3 afterimageDirection() { return new Vec3(
			this.entityData.get(AFTERIMAGE_DIRECTION_X), 0.0, this.entityData.get(AFTERIMAGE_DIRECTION_Z)); }
	public float afterimageYaw() { return this.entityData.get(AFTERIMAGE_YAW); }
	public void setAfterimageNeutral(boolean neutral) { this.entityData.set(AFTERIMAGE_NEUTRAL, neutral); }
	public void setAfterimageAdvanced(boolean advanced) {
		this.entityData.set(AFTERIMAGE_ADVANCED, advanced);
		if (!advanced) this.entityData.set(AFTERIMAGE_OUTLINE, false);
	}
	public void setAfterimageOutline(boolean outline) {
		this.entityData.set(AFTERIMAGE_OUTLINE, outline && isAfterimageAdvanced());
	}

	@Override public LivingEntity livingEntity() { return this; }
	@Override public EchoHeroType heroType() { return EchoHeroType.JAPANESE_SAMURAI; }
	@Override public boolean shouldFollowOwner() { return this.activityMode == EchoRelicState.ActivityMode.FOLLOW && action() == ACTION_NONE; }
	@Override public boolean isFollowMovementSuppressed() { return action() != ACTION_NONE; }
	@Override public @Nullable UUID getOwnerUuid() { LivingEntity owner = getOwner(); return owner == null ? null : owner.getUUID(); }
	@Override public @Nullable UUID getSummonerUuid() { return this.summonerUuid; }
	@Override public @Nullable EntityReference<LivingEntity> getOwnerReference() { return this.ownerReference; }

	@Override
	public void recallTo(Player player) {
		finishAction(this.level().getGameTime());
		Vec3 side = player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(1.5);
		double x = player.getX() + side.x;
		double z = player.getZ() + side.z;
		float yaw = yawToward(x, z, player.getX(), player.getZ());
		this.snapTo(x, player.getY(), z, yaw, 0.0F);
		this.setYBodyRot(yaw);
		this.setYHeadRot(yaw);
		this.getNavigation().stop();
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.25, 0.5, 0.25, 0.01);
			level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.45F, 1.45F);
		}
	}

	@Override
	public void dismiss() {
		if (this.isRemoved()) return;
		finishAction(this.level().getGameTime());
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 24, 0.35, 0.7, 0.35, 0.02);
			level.playSound(null, this.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7F, 0.75F);
		}
		this.discard();
	}

	@Override
	public void onRemoval(Entity.RemovalReason reason) {
		releaseStabPins();
		setSpecialStepHeight(false);
		super.onRemoval(reason);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		EntityReference.store(this.ownerReference, output, "EchoOwner");
		if (this.summonerUuid != null) output.putString("SummonerUuid", this.summonerUuid.toString());
		output.putInt("ActivityMode", this.activityMode.ordinal());
		output.putInt("AlertMode", this.alertMode.ordinal());
		output.putInt("EnabledSkills", this.enabledSkills);
		output.putDouble("ActivityAnchorX", this.activityAnchor.x);
		output.putDouble("ActivityAnchorY", this.activityAnchor.y);
		output.putDouble("ActivityAnchorZ", this.activityAnchor.z);
		output.putLong("ZanshinBonusUntil", this.zanshinBonusUntil);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ownerReference = EntityReference.readWithOldOwnerConversion(input, "EchoOwner", this.level());
		try { this.summonerUuid = UUID.fromString(input.getStringOr("SummonerUuid", "")); }
		catch (IllegalArgumentException ignored) { this.summonerUuid = null; }
		this.activityMode = EchoRelicState.ActivityMode.byOrdinal(input.getIntOr("ActivityMode", 0));
		this.alertMode = EchoRelicState.AlertMode.byOrdinal(input.getIntOr("AlertMode", 1));
		this.enabledSkills = input.getIntOr("EnabledSkills", EchoHeroType.JAPANESE_SAMURAI.allSkillsEnabledMask());
		this.activityAnchor = new Vec3(input.getDoubleOr("ActivityAnchorX", this.getX()),
				input.getDoubleOr("ActivityAnchorY", this.getY()), input.getDoubleOr("ActivityAnchorZ", this.getZ()));
		this.zanshinBonusUntil = input.getLongOr("ZanshinBonusUntil", 0L);
	}

	@Override protected boolean shouldDropLoot(ServerLevel level) { return false; }

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<JapaneseSamuraiEchoEntity>("movement", 3, this::selectMovementAnimation));
		controllers.add(new AnimationController<JapaneseSamuraiEchoEntity>(ACTION_CONTROLLER, 0, test -> {
			test.setControllerSpeed(test.animatable().actionAnimationSpeed());
			return PlayState.STOP;
		})
				.triggerableAnim(ATTACK_FIRST_TRIGGER, ATTACK_FIRST)
				.triggerableAnim(ATTACK_RECOVER_TRIGGER, ATTACK_RECOVER)
				.triggerableAnim(ATTACK_FOLLOW_TRIGGER, ATTACK_FOLLOW)
				.triggerableAnim(STAB_TRIGGER, STAB)
				.triggerableAnim(DASH_FORWARD_TRIGGER, DASH_FORWARD)
				.triggerableAnim(DASH_BACKWARD_TRIGGER, DASH_BACKWARD)
				.triggerableAnim(HURT_TRIGGER, HURT));
	}

	private PlayState selectMovementAnimation(AnimationTest<JapaneseSamuraiEchoEntity> test) {
		return test.setAndContinue(test.animatable().action() == ACTION_NONE && test.isMoving() ? WALK : IDLE);
	}

	@Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.animationCache; }

	private static int scaledAttackTicks(int baseTicks, int interval) {
		return Math.max(1, (int)Math.ceil(baseTicks * interval / (double)BASE_ATTACK_CYCLE_TICKS));
	}

	private static @Nullable LivingEntity resolveLiving(ServerLevel level, @Nullable UUID uuid) {
		if (uuid == null) return null;
		Entity entity = level.getEntity(uuid);
		return entity instanceof LivingEntity living ? living : null;
	}

	private static Vec3 facing(float yaw) {
		double radians = Math.toRadians(yaw);
		return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
	}

	private static float yawToward(double fromX, double fromZ, double targetX, double targetZ) {
		return (float)(Math.atan2(targetZ - fromZ, targetX - fromX) * 180.0 / Math.PI) - 90.0F;
	}

	private static double horizontalDistance(Vec3 first, Vec3 second) {
		double x = first.x - second.x;
		double z = first.z - second.z;
		return Math.sqrt(x * x + z * z);
	}

	private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 segment = end.subtract(start);
		double lengthSqr = segment.lengthSqr();
		if (lengthSqr < 1.0E-8) return point.distanceToSqr(start);
		double t = Math.clamp(point.subtract(start).dot(segment) / lengthSqr, 0.0, 1.0);
		return point.distanceToSqr(start.add(segment.scale(t)));
	}

	private static double projectionAlong(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 segment = end.subtract(start);
		double lengthSqr = segment.lengthSqr();
		return lengthSqr < 1.0E-8 ? 0.0 : point.subtract(start).dot(segment) / lengthSqr;
	}
}
