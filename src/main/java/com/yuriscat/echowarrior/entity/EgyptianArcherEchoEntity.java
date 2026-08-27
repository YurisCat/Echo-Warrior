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
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.entity.behavior.EchoActivityMovement;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EgyptianArcherEchoEntity extends PathfinderMob implements EchoWarriorEntity, GeoEntity {
	public static final byte VISUAL_NORMAL = 0;
	public static final byte VISUAL_ALERT = 1;
	public static final byte VISUAL_STARTLED = 2;
	public static final byte VISUAL_HURT = 3;
	public static final byte VISUAL_CURIOUS = 4;
	public static final byte VISUAL_MUTUAL_GAZE = 5;
	public static final byte VISUAL_CAUGHT = 6;
	public static final byte VISUAL_LOCOMOTION = 7;

	private static final EntityDataAccessor<Float> ATTENTION_X = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ATTENTION_Y = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ATTENTION_Z = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> EYE_ATTENTION_X = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> EYE_ATTENTION_Y = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> EYE_ATTENTION_Z = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Byte> VISUAL_REACTION = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Long> VISUAL_REACTION_UNTIL = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> BLINK_START = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Byte> BLINK_COUNT = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> CURIOUS_TILT = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> VISUAL_SEQUENCE = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Long> ATTENTION_STARTED_AT = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> CAUGHT_REACTION_START = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Boolean> COMBAT_GAZE_LOCKED = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> COMBAT_GAZE_TARGET_ID = SynchedEntityData.defineId(EgyptianArcherEchoEntity.class, EntityDataSerializers.INT);

	private static final double HEAD_GAZE_RADIUS = 0.35;
	private static final double VISUAL_HEAD_CENTER_HEIGHT = 27.5 / 16.0;
	private static final double INVISIBLE_GAZE_RANGE = 4.0;
	private static final int GAZE_MISS_TOLERANCE_TICKS = 2;
	private static final int COMBAT_GAZE_TARGET_GRACE_TICKS = 3;
	private static final int COMBAT_GAZE_SUPPRESSION_TICKS = 20 * 3;
	private static final int POST_COMBAT_VISUAL_SETTLE_TICKS = 30;
	private static final int POST_COMBAT_VISUAL_DIAGNOSTIC_TICKS = 80;
	private static final int POST_BOW_VISUAL_RELEASE_GRACE_TICKS = 8;
	private static final int MUTUAL_GAZE_PRIORITY = 790;
	private static final int EYE_STICKY_TICKS = 5;
	private static final int HEAD_STICKY_TICKS = 10;
	private static final int LOCOMOTION_ATTENTION_PRIORITY = 320;
	private static final int LOCOMOTION_ATTENTION_TICKS = 6;
	private static final int LOCOMOTION_ATTENTION_RELEASE_TICKS = 20;
	private static final int CAUGHT_PREWATCH_TICKS = 8;
	private static final int CAUGHT_GLANCE_START_TICKS = 18;
	private static final double CAUGHT_MAX_OWNER_DISTANCE_SQR = 16.0 * 16.0;
	private static final double CAUGHT_EXIT_WALK_ELIGIBLE_OWNER_DISTANCE_SQR = 9.0 * 9.0;
	private static final double CAUGHT_EXIT_MAX_OWNER_DISTANCE_SQR = 12.0 * 12.0;
	private static final double CAUGHT_EXIT_FOLLOW_CANCEL_DISTANCE_SQR = 15.0 * 15.0;
	private static final float CAUGHT_EXIT_MIN_OWNER_ANGLE = 70.0F;
	private static final float CAUGHT_EXIT_MAX_WALK_ANGLE = 130.0F;

	public static final int SKILL_CAT_GOD = 0;
	public static final int SKILL_ARROW_MODE = 1;
	public static final int SKILL_CHARIOT_VOLLEY = 2;
	public static final int SKILL_BACKSTEP = 3;

	private static final byte ACTION_IDLE = 0;
	private static final byte ACTION_NOCK = 1;
	private static final byte ACTION_DRAW = 2;
	private static final byte ACTION_AIM = 3;
	private static final byte ACTION_SHOOT = 4;
	private static final byte ACTION_BACKSTEP = 5;
	private static final byte ACTION_MELEE = 6;
	private static final byte ACTION_BOW_LOWER = 7;
	private static final byte ACTION_RECOVER = 8;
	private static final byte ACTION_UNNOCK = 9;
	private static final EntityDataAccessor<Byte> ACTION = SynchedEntityData.defineId(
			EgyptianArcherEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> ATTACK_INTERVAL = SynchedEntityData.defineId(
			EgyptianArcherEchoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ACTION_DURATION_TICKS = SynchedEntityData.defineId(
			EgyptianArcherEchoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> RANGED_RELOAD_STYLE = SynchedEntityData.defineId(
			EgyptianArcherEchoEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> ARROW_MODE = SynchedEntityData.defineId(
			EgyptianArcherEchoEntity.class, EntityDataSerializers.INT);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.egyptian_archer.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.egyptian_archer.walk");
	private static final RawAnimation FIRST_NOCK_UPPER = RawAnimation.begin().thenPlayAndHold("animation.egyptian_archer.draw_bow_nock_upper");
	private static final RawAnimation RELOAD_NOCK_UPPER = RawAnimation.begin().thenPlayAndHold("animation.egyptian_archer.reload_bow_nock_upper");
	private static final RawAnimation FIRST_DRAW_UPPER = RawAnimation.begin().thenPlayAndHold("animation.egyptian_archer.draw_bow_pull_upper");
	private static final RawAnimation RELOAD_DRAW_UPPER = RawAnimation.begin().thenPlayAndHold("animation.egyptian_archer.reload_bow_pull_upper");
	private static final RawAnimation BOW_AIM_UPPER = RawAnimation.begin().thenLoop("animation.egyptian_archer.bow_aim_upper");
	private static final RawAnimation SHOOT_UPPER = RawAnimation.begin().thenPlayAndHold("animation.egyptian_archer.shoot_upper");
	private static final RawAnimation UNNOCK_UPPER = RawAnimation.begin().thenPlay("animation.egyptian_archer.un_nock_upper");
	private static final RawAnimation BOW_LOWER_UPPER = RawAnimation.begin().thenPlay("animation.egyptian_archer.bow_lower_upper");
	private static final RawAnimation BOW_RECOVER_UPPER = RawAnimation.begin().thenPlay("animation.egyptian_archer.bow_recover_upper");
	private static final RawAnimation BACKSTEP = RawAnimation.begin().thenPlay("animation.egyptian_archer.backstep_shoot");
	// The committed melee state intentionally lasts a few ticks beyond the source
	// clip so its recovery cannot be interrupted. Hold the neutral final keyframe
	// during that gap; a plain thenPlay lets the triggered controller briefly fall
	// back to the previously cached bow clip when the melee timeline expires.
	private static final RawAnimation MELEE = RawAnimation.begin().thenPlayAndHold("animation.egyptian_archer.melee_attack_upper");
	private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.egyptian_archer.hurt");
	private static final String ACTION_CONTROLLER = "action";
	private static final String FIRST_NOCK_TRIGGER = "first_nock";
	private static final String RELOAD_NOCK_TRIGGER = "reload_nock";
	private static final String FIRST_DRAW_TRIGGER = "first_draw";
	private static final String RELOAD_DRAW_TRIGGER = "reload_draw";
	private static final String BOW_AIM_TRIGGER = "bow_aim";
	private static final String SHOOT_TRIGGER = "shoot";
	private static final String UNNOCK_TRIGGER = "un_nock";
	private static final String BOW_LOWER_TRIGGER = "bow_lower";
	private static final String BOW_RECOVER_TRIGGER = "bow_recover";
	private static final String BACKSTEP_TRIGGER = "backstep";
	private static final String MELEE_TRIGGER = "melee";
	private static final String HURT_TRIGGER = "hurt";

	private static final double MAX_RANGE = 24.0;
	private static final double VOLLEY_RANGE = 16.0;
	private static final double CLOSE_THREAT_TRIGGER_RANGE = 8.0;
	private static final double CLOSE_THREAT_RELEASE_RANGE = 10.0;
	private static final double BACKSTEP_TRIGGER_RANGE = 4.0;
	private static final double BACKSTEP_VOLLEY_RANGE = 12.0;
	private static final double BACKSTEP_DISTANCE = 5.0;
	private static final int BACKSTEP_TICKS = 20;
	private static final int BACKSTEP_VOLLEY_RELEASE_TICK = 11;
	private static final int MAX_BACKSTEP_TARGETS = 6;
	private static final float ARROW_SPEED = 2.2F;
	private static final float ARROW_INACCURACY = 0.5F;
	private static final double ARROW_GRAVITY = 0.05;
	private static final float COMBAT_TURN_SPEED = 45.0F;
	private static final float FIRE_FACING_TOLERANCE = 20.0F;
	private static final int EMERGENCY_TARGET_TICKS = 60;
	private static final int BOW_RECOVERY_TICKS = 8;
	private static final int BOW_LOWER_TICKS = 10;
	private static final int TARGET_LOSS_GRACE_TICKS = 6;
	private static final int COMBAT_SIGHT_MEMORY_TICKS = 20;
	private static final int BASE_RANGED_ATTACK_INTERVAL = 42;
	private static final int MIN_RANGED_ATTACK_INTERVAL = 24;
	private static final int UNNOCK_TICKS = 12;
	private static final double COMMITTED_RELEASE_RANGE = 32.0;
	private static final double AIM_ALIGNMENT_DOT = 0.8660254037844386;
	private static final float FIRST_NOCK_ANIMATION_SECONDS = 1.70833F;
	private static final float RELOAD_NOCK_ANIMATION_SECONDS = 1.69166F;
	private static final float DRAW_ANIMATION_SECONDS = 0.54167F;
	private static final float RELEASE_ANIMATION_SECONDS = 0.25F;
	private static final double RETREAT_SCAN_RANGE = 12.0;
	private static final double RETREAT_MIN_PROGRESS = 0.18;
	private static final double DIRECT_RETREAT_LOOKAHEAD = 1.5;
	private static final double[] DIRECT_RETREAT_PROBES = {0.45, 0.9, DIRECT_RETREAT_LOOKAHEAD};
	private static final double DIRECT_RETREAT_SPEED_SCALE = 0.70;
	private static final double MIN_DIRECT_RETREAT_SPEED = 0.15;
	private static final double MAX_DIRECT_RETREAT_SPEED = 0.24;
	private static final double MELEE_RETREAT_SPEED_SCALE = 0.85;
	private static final double MAX_MELEE_RETREAT_SPEED = 0.32;
	private static final double MELEE_ESCAPE_RELEASE_RANGE = 4.0;
	private static final double SELF_DEFENSE_RELEASE_RANGE = CLOSE_THREAT_RELEASE_RANGE;
	private static final double MELEE_HIGH_THREAT_RANGE = 6.0;
	private static final int MELEE_BLOCKED_HIGH_THREAT_TICKS = 10;
	private static final double RECENT_ATTACKER_RETREAT_WEIGHT = 2.5;
	private static final double DIRECT_RETREAT_JUMP_VELOCITY = 0.42;
	private static final int DIRECT_RETREAT_JUMP_TICKS = 12;
	private static final double MELEE_KNOCKBACK = 0.6;
	private static final double IMMEDIATE_MELEE_BOUNDING_BOX_GAP = 2.1;
	private static final double DIRECT_HIT_MELEE_BOUNDING_BOX_GAP = 2.5;
	private static final double COMMITTED_MELEE_HIT_RANGE = 4.0;
	private static final int VOLLEY_PARTICLE_COLOR = 0xE6C84E;

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable EntityReference<LivingEntity> ownerReference;
	private @Nullable UUID summonerUuid;
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
	private EchoRelicState.ActivityMode activityMode = EchoRelicState.ActivityMode.FOLLOW;
	private EchoRelicState.AlertMode alertMode = EchoRelicState.AlertMode.DEFENSIVE;
	private int enabledSkills = EchoHeroType.EGYPTIAN_ARCHER.defaultEnabledSkillsMask();
	private EchoRelicState.EgyptianArrowMode arrowMode = EchoRelicState.EgyptianArrowMode.OFF;
	private Vec3 activityAnchor = Vec3.ZERO;
	private long lastNaturalHealAt;
	private long nextAttackAt;
	private long actionStartedAt;
	private long actionEndsAt;
	private long shotReleaseAt;
	private boolean shotReleased;
	private RangedPhaseBudget rangedPhaseBudget = rangedPhaseBudget(BASE_RANGED_ATTACK_INTERVAL);
	private int aimTicksRemaining;
	private @Nullable UUID aimTargetUuid;
	private @Nullable Entity committedAimTarget;
	private boolean pendingBackstep;
	private long targetLostAt = -1L;
	private @Nullable LivingEntity actionTarget;
	private @Nullable LivingEntity combatSightTarget;
	private long combatSightLostAt = -1L;
	private Vec3 combatSightLostPosition = Vec3.ZERO;
	private boolean combatSightExpired;
	private @Nullable LivingEntity selfDefenseTarget;
	private boolean meleeEscapeActive;
	private boolean meleeRetargetUsed;
	private boolean meleeHitAgainDuringAction;
	private long meleeRetreatBlockedSince = -1L;
	private @Nullable Monster highThreatTarget;
	private @Nullable LivingEntity resumeTargetAfterThreat;
	private @Nullable LivingEntity emergencyTarget;
	private @Nullable LivingEntity resumeTargetAfterEmergency;
	private long emergencyTargetUntil;
	private Vec3 backstepStart = Vec3.ZERO;
	private Vec3 backstepLanding = Vec3.ZERO;
	private Vec3 backstepLastSafe = Vec3.ZERO;
	private float backstepYaw;
	private boolean backstepVolleyReleased;
	private int movementAnimationLastMovingTick = Integer.MIN_VALUE;
	private boolean movementAnimationActive;
	private boolean bowReturnMovementFrameLocked;
	private int bowReturnMovementReleaseDeferredAtTick = Integer.MIN_VALUE;
	private boolean combatApproaching;
	private boolean combatKiting;
	private Vec3 combatRetreatDestination = Vec3.ZERO;
	private Vec3 combatRetreatDirection = Vec3.ZERO;
	private Vec3 combatRetreatVelocity = Vec3.ZERO;
	private Vec3 combatRetreatProgressPosition = Vec3.ZERO;
	private long combatRetreatRepathAt;
	private long combatRetreatProgressAt;
	private long combatDirectRetreatBlockedUntil;
	private long combatStepJumpUntil;
	private float combatFacingYaw;
	private boolean combatFacingInitialized;
	private @Nullable LivingEntity combatGazeTarget;
	private long combatGazeTargetUntil;
	private long postCombatVisualSettleUntil;
	private long postCombatVisualDiagnosticUntil;
	private Vec3 postCombatVisualSettleDirection = Vec3.ZERO;
	private @Nullable AttentionKind lastLoggedPostCombatAttentionKind;
	private int lastLoggedPostCombatAttentionTargetId = Integer.MIN_VALUE;
	private int lastLoggedCombatGazeTargetId = Integer.MIN_VALUE;
	private boolean lastLoggedCombatGazeLocked;
	private byte lastLoggedCombatGazeAction = Byte.MIN_VALUE;
	private long locomotionAttentionUntil;
	private Vec3 locomotionAttentionDirection = Vec3.ZERO;

	public EgyptianArcherEchoEntity(EntityType<? extends EgyptianArcherEchoEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, EchoHeroType.EGYPTIAN_ARCHER.baseMaximumHealth())
				.add(Attributes.ARMOR, EchoHeroType.EGYPTIAN_ARCHER.baseArmor())
				.add(Attributes.ATTACK_DAMAGE, EchoHeroType.EGYPTIAN_ARCHER.baseAttackDamage())
				.add(Attributes.MOVEMENT_SPEED, EchoHeroType.EGYPTIAN_ARCHER.baseMovementSpeed())
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, EchoHeroType.EGYPTIAN_ARCHER.baseKnockbackResistance());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, ACTION_IDLE);
		builder.define(ATTACK_INTERVAL, EchoHeroType.EGYPTIAN_ARCHER.baseAttackIntervalTicks());
		builder.define(ACTION_DURATION_TICKS, 1);
		builder.define(RANGED_RELOAD_STYLE, false);
		builder.define(ARROW_MODE, EchoRelicState.EgyptianArrowMode.OFF.ordinal());
		builder.define(ATTENTION_X, 0.0F);
		builder.define(ATTENTION_Y, 0.0F);
		builder.define(ATTENTION_Z, 0.0F);
		builder.define(EYE_ATTENTION_X, 0.0F);
		builder.define(EYE_ATTENTION_Y, 0.0F);
		builder.define(EYE_ATTENTION_Z, 0.0F);
		builder.define(VISUAL_REACTION, VISUAL_NORMAL);
		builder.define(VISUAL_REACTION_UNTIL, 0L);
		builder.define(BLINK_START, -100L);
		builder.define(BLINK_COUNT, (byte)0);
		builder.define(CURIOUS_TILT, (byte)0);
		builder.define(VISUAL_SEQUENCE, 0);
		builder.define(ATTENTION_STARTED_AT, 0L);
		builder.define(CAUGHT_REACTION_START, -100L);
		builder.define(COMBAT_GAZE_LOCKED, false);
		builder.define(COMBAT_GAZE_TARGET_ID, -1);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
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

		ItemStack relic = currentRelic();
		if (!relic.isEmpty()) {
			EchoRelicState.backstepCharges(relic, level.getGameTime());
			persistCurrentRelic(relic);
			if (this.tickCount % 20 == 0) {
				applyRelicState(relic, false);
				tickNaturalHealing(level, relic);
			}
		}
		validateSelfDefenseTarget();
		tickCombatSightMemory(level.getGameTime());
		boolean preparingShotLostTarget = (action() == ACTION_NOCK || action() == ACTION_DRAW || action() == ACTION_AIM)
				&& !canContinueCombatAgainst(this.getTarget());
		if (action() == ACTION_SHOOT && isCommittedTargetAlive(this.actionTarget)) {
			// SHOOT is a committed release. Ordinary scans must not replace its target midway
			// through the authored release and make the face snap toward an unrelated enemy.
			if (this.getTarget() != this.actionTarget) this.setTarget(this.actionTarget);
		} else if (action() == ACTION_BACKSTEP && canDefendAgainst(this.actionTarget)) {
			// Backstep is another committed combat action. Keep the triggering threat stable
			// even when the jump crosses a WAIT/WANDER activity boundary.
			if (this.getTarget() != this.actionTarget) this.setTarget(this.actionTarget);
		} else if (action() != ACTION_MELEE && action() != ACTION_SHOOT
				&& ((this.tickCount & 1) == 0 || preparingShotLostTarget)) {
			this.setTarget(selectProtectiveTarget(owner));
			if ((this.tickCount & 3) == 0) enforceActivityBoundary(owner);
		}
		CatGodCreeperSystem.tickAura(level, this);
		tickCombat(level, relic);
		if (this.getTarget() != null && this.tickCount % 20 == 0) {
			EchoExperienceSystem.markParticipation(this, this.getTarget());
		}
		tickMovement(level, owner);
		EchoActivityMovement.tick(level, this, this.activityMode, this.activityAnchor,
				this.getTarget() != null || action() != ACTION_IDLE || isVisualInteractionMovementOwned());
		tickVisualAwareness(level, owner);
		EchoWaterSafety.tick(level, this, owner, this.activityMode == EchoRelicState.ActivityMode.FOLLOW
				&& action() != ACTION_BACKSTEP && !isVisualInteractionMovementOwned());
		LivingEntity facingTarget = (action() == ACTION_MELEE || action() == ACTION_BACKSTEP)
				&& canDefendAgainst(this.actionTarget) || action() == ACTION_SHOOT
				&& isCommittedTargetAlive(this.actionTarget)
				? this.actionTarget
				: this.getTarget();
		if (action() == ACTION_SHOOT ? isCommittedTargetAlive(facingTarget)
				: action() == ACTION_MELEE || action() == ACTION_BACKSTEP
						? canDefendAgainst(facingTarget) : canContinueCombatAgainst(facingTarget)) {
			faceTarget(facingTarget);
		}
	}

	private void tickCombat(ServerLevel level, ItemStack relic) {
		long now = level.getGameTime();
		LivingEntity target = this.getTarget();
		if (action() != ACTION_MELEE && action() != ACTION_BACKSTEP) {
			LivingEntity meleeThreat = selectImmediateMeleeThreat(level, target);
			if (meleeThreat != null) {
				// Immediate melee danger owns every bow phase. During RELEASE this is the
				// only event allowed to interrupt before the projectile has actually spawned.
				startMeleeAttack(now, meleeThreat);
				return;
			}
		}
		if (action() == ACTION_MELEE) {
			tickCommittedMelee(level, now, relic);
			return;
		}
		if (action() == ACTION_BACKSTEP) {
			tickBackstep(level, relic);
			return;
		}
		if (action() == ACTION_RECOVER || action() == ACTION_BOW_LOWER) {
			stopMovementIntent();
			if (now >= this.actionEndsAt) finishAction();
			return;
		}
		if (action() == ACTION_UNNOCK) {
			stopMovementIntent();
			if (now >= this.actionEndsAt) startBowLower(now);
			return;
		}
		if (action() == ACTION_NOCK) {
			tickNock(level, now, relic, target);
			return;
		}
		if (action() == ACTION_DRAW) {
			tickDraw(level, now, relic, target);
			return;
		}
		if (action() == ACTION_AIM) {
			tickAim(level, now, relic, target);
			return;
		}
		if (action() == ACTION_SHOOT) {
			tickRelease(level, now, relic);
			return;
		}

		if (target == null || !canContinueCombatAgainst(target)) {
			this.setTarget(null);
			return;
		}
		this.targetLostAt = -1L;
		this.getLookControl().setLookAt(target, 35.0F, 35.0F);
		if (this.meleeEscapeActive) {
			LivingEntity closeThreat = nearestMeleeEscapeThreat(level, target);
			if (closeThreat == null || this.distanceTo(closeThreat) >= MELEE_ESCAPE_RELEASE_RANGE) {
				clearMeleeEscape();
			} else {
				retainSelfDefenseTarget(closeThreat, "melee_escape");
				this.setTarget(closeThreat);
				if (now >= this.nextAttackAt && isInMeleeRange(closeThreat)) startMeleeAttack(now, closeThreat);
				return;
			}
		}
		if (shouldBackstep(target, relic) && startBackstep(level, relic, target)) return;
		// Emergency melee owns the close-range decision before ranged attacks.
		// Otherwise a disabled/blocked Backstep lets the always-valid bow attack
		// starve melee forever while an enemy is already touching the archer.
		if (isInMeleeRange(target)) {
			startMeleeAttack(now, target);
			return;
		}
		if (canRangedAttack(target)) startNock(now, target, false);
	}

	private void tickNock(ServerLevel level, long now, ItemStack relic, @Nullable LivingEntity target) {
		if (canContinueCombatAgainst(target)) {
			this.targetLostAt = -1L;
			this.actionTarget = target;
			this.getLookControl().setLookAt(target, 35.0F, 35.0F);
			if (shouldBackstep(target, relic) && startBackstep(level, relic, target)) return;
		} else {
			this.setTarget(null);
			this.actionTarget = null;
			stopMovementIntent();
		}
		if (now < this.actionEndsAt) return;
		LivingEntity owner = this.getOwner();
		LivingEntity drawTarget = owner == null ? this.getTarget() : selectProtectiveTarget(owner);
		this.setTarget(drawTarget);
		if (canRangedAttack(drawTarget)) startDraw(now, drawTarget);
		else startUnnock(now);
	}

	private void tickDraw(ServerLevel level, long now, ItemStack relic, @Nullable LivingEntity target) {
		if (canContinueCombatAgainst(target)) {
			this.targetLostAt = -1L;
			this.actionTarget = target;
			this.getLookControl().setLookAt(target, 35.0F, 35.0F);
			if (shouldBackstep(target, relic) && startBackstep(level, relic, target)) return;
		} else {
			this.setTarget(null);
			if (this.targetLostAt < 0L) this.targetLostAt = now;
			stopMovementIntent();
		}
		if (now < this.actionEndsAt) return;
		LivingEntity owner = this.getOwner();
		LivingEntity aimTarget = owner == null ? this.getTarget() : selectProtectiveTarget(owner);
		this.setTarget(aimTarget);
		if (canContinueCombatAgainst(aimTarget)) startAim(now, aimTarget);
		else startBowRecovery(now);
	}

	private void tickAim(ServerLevel level, long now, ItemStack relic, @Nullable LivingEntity target) {
		if (!canContinueCombatAgainst(target)) {
			this.setTarget(null);
			stopMovementIntent();
			if (this.targetLostAt < 0L) this.targetLostAt = now;
			if (isCombatSightExpired(this.actionTarget)
					|| now - this.targetLostAt >= TARGET_LOSS_GRACE_TICKS) startBowRecovery(now);
			return;
		}
		this.targetLostAt = -1L;
		this.actionTarget = target;
		this.getLookControl().setLookAt(target, 90.0F, 90.0F);
		if (shouldBackstep(target, relic) && startBackstep(level, relic, target)) return;
		if (!target.getUUID().equals(this.aimTargetUuid)) {
			this.aimTargetUuid = target.getUUID();
			this.aimTicksRemaining = this.rangedPhaseBudget.aimTicks();
			EchoWarrior.LOGGER.info(
					"[EgyptianArcherRangedState] archer={} tick={} event=aim_reset target={} aimTicks={}",
					this.getId(), now, target.getId(), this.aimTicksRemaining);
		}
		if (!isAimStable(target)) {
			// The renderer keeps the head and pupils locked to the combat target, while
			// Minecraft's server-side LookControl can occasionally leave getLookAngle()
			// just outside the alignment threshold (most often for steep or fast-moving
			// targets). Do not let that cosmetic pitch/yaw discrepancy hold AIM forever.
			// After the authored aim window plus a short grace period, release only when
			// line of sight, range, and the archer's body facing are still valid.
			long aimAge = now - this.actionStartedAt;
			if (aimAge >= this.rangedPhaseBudget.aimTicks() + 4L && canReleaseAfterAimAlignmentGrace(target)) {
				Entity aimTarget = rangedAimTarget(target);
				EchoWarrior.LOGGER.info(
						"[EgyptianArcherRangedState] archer={} tick={} event=aim_alignment_fallback "
								+ "target={} aimAge={} lookDot={} distance={}",
						this.getId(), now, target.getId(), aimAge,
						String.format(Locale.ROOT, "%.3f", aimAlignmentDot(aimTarget)),
						formatDistance(Math.sqrt(this.distanceToSqr(aimTarget))));
				startRelease(now, target);
			}
			return;
		}
		if (--this.aimTicksRemaining <= 0) startRelease(now, target);
	}

	private void tickRelease(ServerLevel level, long now, ItemStack relic) {
		LivingEntity committedTarget = this.actionTarget;
		if (!this.shotReleased && now >= this.shotReleaseAt) {
			this.shotReleased = true;
			if (canFireCommittedShot(committedTarget)) fireMainShot(level, committedTarget, this.committedAimTarget);
		}
		if (canDefendAgainst(committedTarget) && shouldBackstep(committedTarget, relic)) {
			this.pendingBackstep = true;
		}
		if (now < this.actionEndsAt) return;
		if (this.pendingBackstep && canDefendAgainst(committedTarget)) {
			this.pendingBackstep = false;
			finishAction();
			if (startBackstep(level, relic, committedTarget)) return;
		}
		LivingEntity owner = this.getOwner();
		LivingEntity nextTarget = owner == null ? null : selectProtectiveTarget(owner);
		this.setTarget(nextTarget);
		if (canContinueCombatAgainst(nextTarget)) startNock(now, nextTarget, true);
		else startBowLower(now);
	}

	private void tickCommittedMelee(ServerLevel level, long now, ItemStack relic) {
		LivingEntity meleeTarget = this.actionTarget;
		if (!this.shotReleased && !canDefendAgainst(meleeTarget) && !this.meleeRetargetUsed) {
			LivingEntity replacement = selectCommittedMeleeReplacement(level, meleeTarget);
			if (replacement != null) {
				this.meleeRetargetUsed = true;
				this.actionTarget = replacement;
				this.setTarget(replacement);
				retainSelfDefenseTarget(replacement, "melee_retarget");
				meleeTarget = replacement;
			}
		}
		if (canDefendAgainst(meleeTarget)) {
			this.getLookControl().setLookAt(meleeTarget, 90.0F, 90.0F);
		}
		if (!this.shotReleased && now >= this.shotReleaseAt) {
			this.shotReleased = true;
			boolean eligible = canLandCommittedMeleeHit(meleeTarget);
			boolean damaged = false;
			if (eligible) {
				float damage = meleeTarget.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD) ? 3.6F : 3.0F;
				damaged = meleeTarget.hurtServer(level, level.damageSources().mobAttack(this), damage);
				if (damaged) {
					meleeTarget.knockback(MELEE_KNOCKBACK, this.getX() - meleeTarget.getX(), this.getZ() - meleeTarget.getZ());
				}
			}
			EchoWarrior.LOGGER.info(
					"[EgyptianArcherMelee] archer={} tick={} event=release target={} eligible={} damaged={} centerDistance={} boxGap={}",
					this.getId(), now, entityId(meleeTarget), eligible, damaged,
					formatDistance(meleeTarget == null ? Double.NaN : Math.sqrt(this.distanceToSqr(meleeTarget))),
					formatDistance(meleeTarget == null ? Double.NaN : Math.sqrt(boundingBoxDistanceSqr(meleeTarget))));
		}
		if (now < this.actionEndsAt) return;

		LivingEntity nearestThreat = nearestMeleeEscapeThreat(level, meleeTarget);
		boolean stillPressured = nearestThreat != null && this.distanceTo(nearestThreat) < MELEE_ESCAPE_RELEASE_RANGE;
		boolean retreatBlocked = this.meleeRetreatBlockedSince >= 0L
				&& now - this.meleeRetreatBlockedSince >= MELEE_BLOCKED_HIGH_THREAT_TICKS;
		boolean highThreat = isHighMeleeThreat(level) || retreatBlocked;
		boolean shouldSpendBackstep = nearestThreat != null && (highThreat
				|| stillPressured || retreatBlocked || this.meleeHitAgainDuringAction);
		if (nearestThreat != null) retainSelfDefenseTarget(nearestThreat, "melee_finish");
		finishAction();
		if (shouldSpendBackstep && startBackstep(level, relic, nearestThreat)) {
			return;
		}
		if (!stillPressured) clearMeleeEscape();
	}

	private void startNock(long now, @Nullable LivingEntity target, boolean reloadStyle) {
		this.rangedPhaseBudget = rangedPhaseBudget(attackInterval());
		this.entityData.set(ACTION, ACTION_NOCK);
		this.entityData.set(ACTION_DURATION_TICKS, this.rangedPhaseBudget.nockTicks());
		this.entityData.set(RANGED_RELOAD_STYLE, reloadStyle);
		this.actionStartedAt = now;
		this.actionEndsAt = now + this.rangedPhaseBudget.nockTicks();
		this.actionTarget = target;
		this.targetLostAt = -1L;
		beginCombatFacing();
		this.shotReleased = false;
		this.aimTargetUuid = null;
		this.committedAimTarget = null;
		this.pendingBackstep = false;
		this.triggerAnim(ACTION_CONTROLLER, reloadStyle ? RELOAD_NOCK_TRIGGER : FIRST_NOCK_TRIGGER);
		EchoWarrior.LOGGER.info(
				"[EgyptianArcherRangedState] archer={} tick={} event=nock_start target={} reload={} budget={}/{}/{}/{}",
				this.getId(), now, entityId(target), reloadStyle, this.rangedPhaseBudget.nockTicks(),
				this.rangedPhaseBudget.drawTicks(), this.rangedPhaseBudget.aimTicks(),
				this.rangedPhaseBudget.releaseTicks());
	}

	private void startDraw(long now, LivingEntity target) {
		this.entityData.set(ACTION, ACTION_DRAW);
		this.entityData.set(ACTION_DURATION_TICKS, this.rangedPhaseBudget.drawTicks());
		this.actionStartedAt = now;
		this.actionEndsAt = now + this.rangedPhaseBudget.drawTicks();
		this.actionTarget = target;
		this.targetLostAt = -1L;
		this.triggerAnim(ACTION_CONTROLLER,
				this.entityData.get(RANGED_RELOAD_STYLE) ? RELOAD_DRAW_TRIGGER : FIRST_DRAW_TRIGGER);
	}

	private void startAim(long now, LivingEntity target) {
		this.entityData.set(ACTION, ACTION_AIM);
		this.entityData.set(ACTION_DURATION_TICKS, this.rangedPhaseBudget.aimTicks());
		this.actionStartedAt = now;
		this.actionEndsAt = Long.MAX_VALUE;
		this.actionTarget = target;
		this.targetLostAt = -1L;
		this.aimTicksRemaining = this.rangedPhaseBudget.aimTicks();
		this.aimTargetUuid = target.getUUID();
		this.triggerAnim(ACTION_CONTROLLER, BOW_AIM_TRIGGER);
	}

	private void startRelease(long now, LivingEntity target) {
		int releaseTicks = this.rangedPhaseBudget.releaseTicks();
		this.entityData.set(ACTION, ACTION_SHOOT);
		this.entityData.set(ACTION_DURATION_TICKS, releaseTicks);
		this.actionStartedAt = now;
		this.actionEndsAt = now + releaseTicks;
		this.shotReleaseAt = now + (releaseTicks + 1L) / 2L;
		this.actionTarget = target;
		this.committedAimTarget = rangedAimTarget(target);
		this.targetLostAt = -1L;
		this.shotReleased = false;
		this.pendingBackstep = false;
		this.triggerAnim(ACTION_CONTROLLER, SHOOT_TRIGGER);
	}

	private void fireMainShot(ServerLevel level, LivingEntity target, @Nullable Entity aimTarget) {
		if (!target.isAlive() || aimTarget == null) return;
		// RELEASE is committed after a valid AIM. Do not re-check ordinary line of
		// sight here: any newly interposed wall is handled by projectile collision.
		spawnArrow(level, target, aimTarget, COMMITTED_RELEASE_RANGE);
		if (!skillEnabled(SKILL_CHARIOT_VOLLEY)) return;
		List<LivingEntity> alternatives = combatTargets(level, VOLLEY_RANGE, target);
		int enemyCount = alternatives.size() + 1;
		if (enemyCount < 2) return;
		float chance = Math.min(0.55F, 0.15F + 0.08F * (enemyCount - 2));
		if (this.random.nextFloat() < chance
				&& spawnArrow(level, alternatives.get(this.random.nextInt(alternatives.size()))) != null) {
			level.sendParticles(new DustParticleOptions(VOLLEY_PARTICLE_COLOR, 0.9F),
					this.getX(), this.getY() + this.getBbHeight() * 0.58, this.getZ(),
					14, 0.42, 0.55, 0.42, 0.025);
		}
	}

	private @Nullable EgyptianArcherArrowEntity spawnArrow(ServerLevel level, LivingEntity target) {
		Entity aimTarget = rangedAimTarget(target);
		return aimTarget == null ? null : spawnArrow(level, target, aimTarget, MAX_RANGE);
	}

	private @Nullable EgyptianArcherArrowEntity spawnArrow(ServerLevel level, LivingEntity target,
			Entity aimTarget, double permittedRange) {
		if (!target.isAlive() || aimTarget.isRemoved()
				|| this.distanceToSqr(aimTarget) > permittedRange * permittedRange) return null;
		EgyptianArcherArrowEntity arrow = ModEntities.EGYPTIAN_ARCHER_ARROW.create(level,
				net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		if (arrow == null) return null;
		arrow.setPos(this.getX(), this.getEyeY() - 0.18, this.getZ());
		boolean pierce = this.arrowMode == EchoRelicState.EgyptianArrowMode.CONE && this.random.nextFloat() < 0.25F;
		arrow.configure(this, this.arrowMode, (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE), pierce);
		double dx = aimTarget.getX() - arrow.getX();
		double dz = aimTarget.getZ() - arrow.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		double targetY = aimTarget.getY() + aimTarget.getBbHeight() * 0.62;
		double dy = ballisticAimHeight(horizontal, targetY - arrow.getY());
		arrow.shoot(dx, dy, dz, ARROW_SPEED, ARROW_INACCURACY);
		if (!level.addFreshEntity(arrow)) return null;
		level.playSound(null, this.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.75F, 0.95F + this.random.nextFloat() * 0.1F);
		return arrow;
	}

	private boolean canFireCommittedShot(@Nullable LivingEntity target) {
		if (!isCommittedTargetAlive(target)) return false;
		Entity aimTarget = this.committedAimTarget;
		if (aimTarget == null || aimTarget.isRemoved()
				|| this.distanceToSqr(aimTarget) > COMMITTED_RELEASE_RANGE * COMMITTED_RELEASE_RANGE) {
			aimTarget = committedAimTarget(target);
			this.committedAimTarget = aimTarget;
		}
		return aimTarget != null && !aimTarget.isRemoved()
				&& this.distanceToSqr(aimTarget) <= COMMITTED_RELEASE_RANGE * COMMITTED_RELEASE_RANGE;
	}

	private boolean isCommittedTargetAlive(@Nullable LivingEntity target) {
		return target != null && target.isAlive() && !target.isRemoved() && this.canAttack(target);
	}

	private @Nullable Entity committedAimTarget(LivingEntity target) {
		if (!(target instanceof EnderDragon dragon)) return target;
		return Arrays.stream(dragon.getSubEntities())
				.filter(part -> !part.isRemoved())
				.filter(part -> this.distanceToSqr(part) <= COMMITTED_RELEASE_RANGE * COMMITTED_RELEASE_RANGE)
				.min(Comparator.comparingDouble(this::distanceToSqr))
				.orElse(null);
	}

	private boolean canRangedAttack(@Nullable LivingEntity target) {
		if (target == null) return false;
		Entity aimTarget = rangedAimTarget(target);
		return aimTarget != null && this.distanceToSqr(aimTarget) <= MAX_RANGE * MAX_RANGE;
	}

	private boolean isAimStable(LivingEntity target) {
		Entity aimTarget = rangedAimTarget(target);
		if (aimTarget == null || this.distanceToSqr(aimTarget) > MAX_RANGE * MAX_RANGE
				|| !isFacingTarget(target)) return false;
		return aimAlignmentDot(aimTarget) >= AIM_ALIGNMENT_DOT;
	}

	private boolean canReleaseAfterAimAlignmentGrace(LivingEntity target) {
		Entity aimTarget = rangedAimTarget(target);
		return aimTarget != null && this.distanceToSqr(aimTarget) <= MAX_RANGE * MAX_RANGE
				&& isFacingTarget(target);
	}

	private double aimAlignmentDot(@Nullable Entity aimTarget) {
		if (aimTarget == null) return -1.0;
		Vec3 aimPoint = aimTarget instanceof LivingEntity living
				? living.getEyePosition()
				: aimTarget.getBoundingBox().getCenter();
		Vec3 targetDirection = aimPoint.subtract(this.getEyePosition());
		if (targetDirection.lengthSqr() <= 1.0E-6) return 1.0;
		return this.getLookAngle().normalize().dot(targetDirection.normalize());
	}

	private static RangedPhaseBudget rangedPhaseBudget(int requestedInterval) {
		int interval = Mth.clamp(requestedInterval, MIN_RANGED_ATTACK_INTERVAL, BASE_RANGED_ATTACK_INTERVAL);
		float progress = (interval - MIN_RANGED_ATTACK_INTERVAL)
				/ (float)(BASE_RANGED_ATTACK_INTERVAL - MIN_RANGED_ATTACK_INTERVAL);
		int nockTicks = Math.round(13.0F + 10.0F * progress);
		int drawTicks = Mth.floor(5.0F + 3.0F * progress + 1.0E-5F);
		int releaseTicks = Math.round(3.0F + 2.0F * progress);
		int aimTicks = interval - nockTicks - drawTicks - releaseTicks;
		if (aimTicks < 3 || nockTicks + drawTicks + aimTicks + releaseTicks != interval) {
			throw new IllegalStateException("Invalid Egyptian Archer ranged phase budget for " + interval);
		}
		return new RangedPhaseBudget(interval, nockTicks, drawTicks, aimTicks, releaseTicks);
	}

	private void tickCombatSightMemory(long now) {
		LivingEntity target = this.getTarget();
		if (target == null) {
			if (this.combatSightTarget != null && !this.combatSightTarget.isAlive()) resetCombatSightMemory();
			return;
		}
		if (!canDefendAgainst(target)) {
			if (target == this.combatSightTarget) this.combatSightExpired = true;
			return;
		}
		boolean hasSight = rangedAimTarget(target) != null;
		if (target != this.combatSightTarget) {
			this.combatSightTarget = target;
			this.combatSightLostAt = hasSight ? -1L : now;
			this.combatSightLostPosition = hasSight ? Vec3.ZERO : this.position();
			this.combatSightExpired = false;
			return;
		}
		if (hasSight) {
			this.combatSightLostAt = -1L;
			this.combatSightLostPosition = Vec3.ZERO;
			this.combatSightExpired = false;
			return;
		}
		if (this.combatSightLostAt < 0L) {
			this.combatSightLostAt = now;
			this.combatSightLostPosition = this.position();
			return;
		}
		if (!this.combatSightExpired && now - this.combatSightLostAt >= COMBAT_SIGHT_MEMORY_TICKS) {
			this.combatSightExpired = true;
			this.getNavigation().stop();
			double progress = Math.sqrt(horizontalDistanceSqr(this.position(), this.combatSightLostPosition));
			EchoWarrior.LOGGER.info(
					"[EgyptianArcherSight] archer={} tick={} event=timeout target={} hiddenTicks={} progress={} navigationDone={}",
					this.getId(), now, target.getId(), now - this.combatSightLostAt,
					formatDistance(progress), this.getNavigation().isDone());
		}
	}

	private void resetCombatSightMemory() {
		this.combatSightTarget = null;
		this.combatSightLostAt = -1L;
		this.combatSightLostPosition = Vec3.ZERO;
		this.combatSightExpired = false;
	}

	private boolean isCombatSightExpired(@Nullable LivingEntity target) {
		return target != null && target == this.combatSightTarget && this.combatSightExpired;
	}

	private boolean canAcquireCombatTarget(@Nullable LivingEntity target) {
		if (target == null || !canProtectAgainst(target) || rangedAimTarget(target) == null) return false;
		return !(target instanceof Creeper creeper) || !CatGodCreeperSystem.isPanicking(creeper)
				|| target == this.getLastHurtByMob() && isRecentWithin(this, this.getLastHurtByMobTimestamp(), 20);
	}

	private @Nullable Entity rangedAimTarget(LivingEntity target) {
		if (!(target instanceof EnderDragon dragon)) {
			return this.hasLineOfSight(target) ? target : null;
		}
		return Arrays.stream(dragon.getSubEntities())
				.filter(part -> this.distanceToSqr(part) <= MAX_RANGE * MAX_RANGE)
				.filter(this::hasLineOfSight)
				.min(Comparator.comparingDouble(this::distanceToSqr))
				.orElse(null);
	}

	private static double ballisticAimHeight(double horizontal, double vertical) {
		if (horizontal < 1.0E-4) return vertical;
		double speedSquared = ARROW_SPEED * ARROW_SPEED;
		double discriminant = speedSquared * speedSquared
				- ARROW_GRAVITY * (ARROW_GRAVITY * horizontal * horizontal + 2.0 * vertical * speedSquared);
		if (discriminant <= 0.0) return vertical;
		double lowArcTangent = (speedSquared - Math.sqrt(discriminant)) / (ARROW_GRAVITY * horizontal);
		return horizontal * lowArcTangent;
	}

	private void faceTarget(LivingEntity target) {
		Entity aimTarget = action() == ACTION_SHOOT && target == this.actionTarget
				&& this.committedAimTarget != null && !this.committedAimTarget.isRemoved()
				? this.committedAimTarget
				: target instanceof EnderDragon ? rangedAimTarget(target) : target;
		if (aimTarget == null) aimTarget = target;
		float desiredYaw = yawToward(this.getX(), this.getZ(), aimTarget.getX(), aimTarget.getZ());
		if (!this.combatFacingInitialized) beginCombatFacing();
		this.combatFacingYaw = Mth.approachDegrees(this.combatFacingYaw, desiredYaw, COMBAT_TURN_SPEED);
		this.setYRot(this.combatFacingYaw);
		this.setYBodyRot(this.combatFacingYaw);
		this.setYHeadRot(this.combatFacingYaw);
		this.getLookControl().setLookAt(aimTarget, 90.0F, 90.0F);
	}

	private void beginCombatFacing() {
		this.combatFacingYaw = this.getYRot();
		this.combatFacingInitialized = true;
	}

	private boolean isFacingTarget(LivingEntity target) {
		Entity aimTarget = target instanceof EnderDragon ? rangedAimTarget(target) : target;
		if (aimTarget == null) aimTarget = target;
		float desiredYaw = yawToward(this.getX(), this.getZ(), aimTarget.getX(), aimTarget.getZ());
		float currentYaw = this.combatFacingInitialized ? this.combatFacingYaw : this.getYRot();
		return Math.abs(Mth.degreesDifference(currentYaw, desiredYaw)) <= FIRE_FACING_TOLERANCE;
	}

	private void startMeleeAttack(long now, LivingEntity target) {
		byte previousAction = action();
		stopBowAnimations();
		this.stopTriggeredAnim(ACTION_CONTROLLER, HURT_TRIGGER);
		this.setTarget(target);
		this.entityData.set(ACTION, ACTION_MELEE);
		this.entityData.set(ACTION_DURATION_TICKS, 20);
		this.actionStartedAt = now;
		this.actionEndsAt = now + 20L;
		this.shotReleaseAt = now + 8L;
		this.shotReleased = false;
		this.aimTargetUuid = null;
		this.committedAimTarget = null;
		this.pendingBackstep = false;
		this.targetLostAt = -1L;
		this.actionTarget = target;
		retainSelfDefenseTarget(target, "melee_start");
		this.meleeEscapeActive = true;
		this.meleeRetargetUsed = false;
		this.meleeHitAgainDuringAction = false;
		beginCombatFacing();
		this.nextAttackAt = now + 20L;
		this.triggerAnim(ACTION_CONTROLLER, MELEE_TRIGGER);
		EchoWarrior.LOGGER.info(
				"[EgyptianArcherMelee] archer={} tick={} event=start target={} type={} previousAction={} centerDistance={} boxGap={} activity={}",
				this.getId(), now, target.getId(), target.getType(), actionName(previousAction),
				formatDistance(Math.sqrt(this.distanceToSqr(target))),
				formatDistance(Math.sqrt(boundingBoxDistanceSqr(target))), this.activityMode);
	}

	private boolean isInMeleeRange(LivingEntity target) {
		return canDefendAgainst(target)
				&& boundingBoxDistanceSqr(target) <= IMMEDIATE_MELEE_BOUNDING_BOX_GAP * IMMEDIATE_MELEE_BOUNDING_BOX_GAP
				&& this.hasLineOfSight(target);
	}

	private boolean canLandCommittedMeleeHit(@Nullable LivingEntity target) {
		return canDefendAgainst(target)
				&& this.distanceToSqr(target) <= COMMITTED_MELEE_HIT_RANGE * COMMITTED_MELEE_HIT_RANGE
				&& this.hasLineOfSight(target);
	}

	private double boundingBoxDistanceSqr(Entity target) {
		AABB ownBox = this.getBoundingBox();
		AABB targetBox = target.getBoundingBox();
		double dx = Math.max(0.0, Math.max(ownBox.minX - targetBox.maxX, targetBox.minX - ownBox.maxX));
		double dy = Math.max(0.0, Math.max(ownBox.minY - targetBox.maxY, targetBox.minY - ownBox.maxY));
		double dz = Math.max(0.0, Math.max(ownBox.minZ - targetBox.maxZ, targetBox.minZ - ownBox.maxZ));
		return dx * dx + dy * dy + dz * dz;
	}

	private static String formatDistance(double distance) {
		return Double.isFinite(distance) ? String.format(Locale.ROOT, "%.2f", distance) : "n/a";
	}

	private @Nullable LivingEntity selectImmediateMeleeThreat(ServerLevel level, @Nullable LivingEntity currentTarget) {
		LivingEntity owner = this.getOwner();
		LivingEntity recentAttacker = this.getLastHurtByMob();
		boolean recentDamage = isRecentWithin(this, this.getLastHurtByMobTimestamp(), 20);
		return level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0), candidate ->
				(candidate instanceof Enemy || candidate == currentTarget)
						&& isInMeleeRange(candidate)
						&& (!(candidate instanceof Creeper creeper) || !CatGodCreeperSystem.isPanicking(creeper)
								|| recentDamage && candidate == recentAttacker))
				.stream()
				.min(Comparator.comparingInt((LivingEntity candidate) -> {
					if (recentDamage && candidate == recentAttacker) return 0;
					if (candidate instanceof Monster monster
							&& (monster.getTarget() == this || monster.getTarget() == owner)) return 1;
					return candidate == currentTarget ? 2 : 3;
				}).thenComparingDouble(this::distanceToSqr))
				.orElse(null);
	}

	private @Nullable LivingEntity selectCommittedMeleeReplacement(ServerLevel level,
			@Nullable LivingEntity previousTarget) {
		return level.getEntitiesOfClass(LivingEntity.class,
				this.getBoundingBox().inflate(MELEE_ESCAPE_RELEASE_RANGE), candidate ->
						candidate != previousTarget && canDefendAgainst(candidate)
								&& this.distanceToSqr(candidate) <= MELEE_ESCAPE_RELEASE_RANGE * MELEE_ESCAPE_RELEASE_RANGE
								&& this.hasLineOfSight(candidate)
								&& (candidate instanceof Enemy || candidate == this.getTarget()
										|| candidate == this.getLastHurtByMob()))
				.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
	}

	private @Nullable LivingEntity nearestMeleeEscapeThreat(ServerLevel level,
			@Nullable LivingEntity preferredTarget) {
		return meleeRetreatThreats(level, preferredTarget).stream()
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
	}

	private boolean isHighMeleeThreat(ServerLevel level) {
		long nearbyThreats = meleeRetreatThreats(level, this.actionTarget).stream()
				.filter(threat -> this.distanceToSqr(threat) <= MELEE_HIGH_THREAT_RANGE * MELEE_HIGH_THREAT_RANGE)
				.limit(2L).count();
		return nearbyThreats >= 2L || this.getHealth() <= this.getMaxHealth() * 0.4F;
	}

	private void clearMeleeEscape() {
		this.meleeEscapeActive = false;
		this.meleeRetargetUsed = false;
		this.meleeHitAgainDuringAction = false;
		this.meleeRetreatBlockedSince = -1L;
		resetCombatRetreat();
	}

	private void startBowLower(long now) {
		this.entityData.set(ACTION, ACTION_BOW_LOWER);
		this.entityData.set(ACTION_DURATION_TICKS, BOW_LOWER_TICKS);
		this.actionStartedAt = now;
		this.actionEndsAt = now + BOW_LOWER_TICKS;
		this.actionTarget = null;
		this.targetLostAt = -1L;
		this.shotReleased = false;
		this.aimTargetUuid = null;
		this.committedAimTarget = null;
		this.pendingBackstep = false;
		stopMovementIntent();
		extendPostCombatVisualSettleThrough(this.actionEndsAt + POST_BOW_VISUAL_RELEASE_GRACE_TICKS);
		this.triggerAnim(ACTION_CONTROLLER, BOW_LOWER_TRIGGER);
		EchoWarrior.LOGGER.info(
				"[EgyptianArcherRangedState] archer={} tick={} event=bow_lower_start duration={} settleUntil={}",
				this.getId(), now, BOW_LOWER_TICKS, this.postCombatVisualSettleUntil);
	}

	private void startBowRecovery(long now) {
		this.entityData.set(ACTION, ACTION_RECOVER);
		this.entityData.set(ACTION_DURATION_TICKS, BOW_RECOVERY_TICKS);
		this.actionStartedAt = now;
		this.actionEndsAt = now + BOW_RECOVERY_TICKS;
		this.actionTarget = null;
		this.targetLostAt = -1L;
		this.shotReleased = false;
		this.aimTargetUuid = null;
		this.committedAimTarget = null;
		this.pendingBackstep = false;
		stopMovementIntent();
		extendPostCombatVisualSettleThrough(this.actionEndsAt + POST_BOW_VISUAL_RELEASE_GRACE_TICKS);
		this.triggerAnim(ACTION_CONTROLLER, BOW_RECOVER_TRIGGER);
		EchoWarrior.LOGGER.info(
				"[EgyptianArcherRangedState] archer={} tick={} event=bow_recovery_start duration={} settleUntil={}",
				this.getId(), now, BOW_RECOVERY_TICKS, this.postCombatVisualSettleUntil);
	}

	private void startUnnock(long now) {
		this.entityData.set(ACTION, ACTION_UNNOCK);
		this.entityData.set(ACTION_DURATION_TICKS, UNNOCK_TICKS);
		this.entityData.set(RANGED_RELOAD_STYLE, true);
		this.actionStartedAt = now;
		this.actionEndsAt = now + UNNOCK_TICKS;
		this.actionTarget = null;
		this.targetLostAt = -1L;
		this.shotReleased = false;
		this.aimTargetUuid = null;
		this.committedAimTarget = null;
		this.pendingBackstep = false;
		stopMovementIntent();
		extendPostCombatVisualSettleThrough(this.actionEndsAt + BOW_LOWER_TICKS
				+ POST_BOW_VISUAL_RELEASE_GRACE_TICKS);
		this.triggerAnim(ACTION_CONTROLLER, UNNOCK_TRIGGER);
		EchoWarrior.LOGGER.info(
				"[EgyptianArcherRangedState] archer={} tick={} event=un_nock_start duration={} settleUntil={}",
				this.getId(), now, UNNOCK_TICKS, this.postCombatVisualSettleUntil);
	}

	private void stopBowAnimations() {
		this.stopTriggeredAnim(ACTION_CONTROLLER, FIRST_NOCK_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, RELOAD_NOCK_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, FIRST_DRAW_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, RELOAD_DRAW_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, BOW_AIM_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, SHOOT_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, UNNOCK_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, BOW_LOWER_TRIGGER);
		this.stopTriggeredAnim(ACTION_CONTROLLER, BOW_RECOVER_TRIGGER);
	}

	private boolean shouldBackstep(LivingEntity target, ItemStack relic) {
		return this.distanceToSqr(target) <= BACKSTEP_TRIGGER_RANGE * BACKSTEP_TRIGGER_RANGE && canBackstep(relic);
	}

	private boolean canBackstep(ItemStack relic) {
		return !relic.isEmpty() && EchoRelicState.skillEnabled(relic, SKILL_BACKSTEP)
				&& EchoRelicState.backstepCharges(relic, this.level().getGameTime()) > 0;
	}

	private boolean startBackstep(ServerLevel level, ItemStack relic, LivingEntity nearestEnemy) {
		Vec3 landing = findBackstepLanding(level, nearestEnemy);
		if (landing == null || !EchoRelicState.consumeBackstepCharge(relic, level.getGameTime())) return false;
		stopBowAnimations();
		clearMeleeEscape();
		retainSelfDefenseTarget(nearestEnemy, "backstep_start");
		this.actionTarget = nearestEnemy;
		this.setTarget(nearestEnemy);
		persistCurrentRelic(relic);
		this.backstepStart = this.position();
		this.backstepLanding = landing;
		this.backstepLastSafe = this.backstepStart;
		// Preserve the exact combat-facing direction at take-off. Picking a new yaw
		// here caused the visible twist immediately before the jump.
		this.backstepYaw = this.getYRot();
		this.setYRot(this.backstepYaw);
		this.setYBodyRot(this.backstepYaw);
		this.setYHeadRot(this.backstepYaw);
		this.actionStartedAt = level.getGameTime();
		this.actionEndsAt = this.actionStartedAt + BACKSTEP_TICKS;
		this.entityData.set(ACTION_DURATION_TICKS, BACKSTEP_TICKS);
		this.targetLostAt = -1L;
		this.aimTargetUuid = null;
		this.committedAimTarget = null;
		this.backstepVolleyReleased = false;
		this.entityData.set(ACTION, ACTION_BACKSTEP);
		this.setNoGravity(true);
		this.setDeltaMovement(Vec3.ZERO);
		this.getNavigation().stop();
		this.triggerAnim(ACTION_CONTROLLER, BACKSTEP_TRIGGER);
		return true;
	}

	private void tickBackstep(ServerLevel level, ItemStack relic) {
		long elapsed = level.getGameTime() - this.actionStartedAt;
		double t = Math.clamp(elapsed / (double)BACKSTEP_TICKS, 0.0, 1.0);
		double arc = 1.15 * 4.0 * t * (1.0 - t);
		Vec3 point = this.backstepStart.lerp(this.backstepLanding, t).add(0.0, arc, 0.0);
		if (!isBackstepPointSafe(level, point)) {
			this.snapTo(this.backstepLastSafe.x, this.backstepLastSafe.y, this.backstepLastSafe.z,
					this.backstepYaw, this.getXRot());
			this.setNoGravity(false);
			finishAction();
			return;
		}
		this.snapTo(point.x, point.y, point.z, this.backstepYaw, this.getXRot());
		this.backstepLastSafe = point;
		this.setYRot(this.backstepYaw);
		this.setYBodyRot(this.backstepYaw);
		this.setYHeadRot(this.backstepYaw);
		this.setDeltaMovement(Vec3.ZERO);
		if (!this.backstepVolleyReleased && elapsed >= BACKSTEP_VOLLEY_RELEASE_TICK) {
			this.backstepVolleyReleased = true;
			// The authored held arrow disappears and the bow string releases at
			// 13/24 seconds. Tick 11 is the first 20 TPS simulation step at or
			// after that keyframe. Re-scan here so a dead or newly hidden target
			// never receives a phantom projectile from the committed jump.
			for (LivingEntity target : combatTargets(level, BACKSTEP_VOLLEY_RANGE, null).stream()
					.limit(MAX_BACKSTEP_TARGETS).toList()) spawnArrow(level, target);
		}
		if (elapsed >= BACKSTEP_TICKS) {
			this.snapTo(this.backstepLanding.x, this.backstepLanding.y, this.backstepLanding.z,
					this.backstepYaw, this.getXRot());
			this.setNoGravity(false);
			finishAction();
		}
	}

	private @Nullable Vec3 findBackstepLanding(ServerLevel level, LivingEntity nearestEnemy) {
		Vec3 best = null;
		double bestScore = this.distanceToSqr(nearestEnemy);
		for (int index = 0; index < 32; index++) {
			double angle = Math.PI * 2.0 * index / 32.0;
			double x = this.getX() + Math.cos(angle) * BACKSTEP_DISTANCE;
			double z = this.getZ() + Math.sin(angle) * BACKSTEP_DISTANCE;
			for (int dy : new int[] {0, 1, -1, 2, -2}) {
				Vec3 candidate = new Vec3(x, this.getY() + dy, z);
				if (!isSafeLanding(level, candidate) || !isBackstepArcSafe(level, candidate)) continue;
				double score = candidate.distanceToSqr(nearestEnemy.position());
				if (score > bestScore) {
					bestScore = score;
					best = candidate;
				}
			}
		}
		return best;
	}

	private boolean isSafeLanding(ServerLevel level, Vec3 candidate) {
		BlockPos feet = BlockPos.containing(candidate);
		BlockPos floor = feet.below();
		if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
				|| !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()
				|| level.getBlockState(feet).is(BlockTags.FIRE) || level.getBlockState(feet).is(Blocks.POWDER_SNOW)) return false;
		AABB moved = this.getBoundingBox().move(candidate.subtract(this.position()));
		return level.noCollision(this, moved) && !containsBackstepHazard(level, moved);
	}

	private boolean isBackstepArcSafe(ServerLevel level, Vec3 landing) {
		Vec3 start = this.position();
		for (int step = 1; step <= BACKSTEP_TICKS; step++) {
			double t = step / (double)BACKSTEP_TICKS;
			double arc = 1.15 * 4.0 * t * (1.0 - t);
			Vec3 point = start.lerp(landing, t).add(0.0, arc, 0.0);
			AABB moved = this.getBoundingBox().move(point.subtract(start));
			if (!level.noCollision(this, moved) || containsBackstepHazard(level, moved)) return false;
		}
		return true;
	}

	private boolean isBackstepPointSafe(ServerLevel level, Vec3 point) {
		AABB moved = this.getBoundingBox().move(point.subtract(this.position()));
		return level.noCollision(this, moved) && !containsBackstepHazard(level, moved);
	}

	private static boolean containsBackstepHazard(ServerLevel level, AABB box) {
		int minX = Mth.floor(box.minX + 1.0E-5);
		int minY = Mth.floor(box.minY + 1.0E-5);
		int minZ = Mth.floor(box.minZ + 1.0E-5);
		int maxX = Mth.floor(box.maxX - 1.0E-5);
		int maxY = Mth.floor(box.maxY - 1.0E-5);
		int maxZ = Mth.floor(box.maxZ - 1.0E-5);
		for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
			if (level.getFluidState(pos).is(FluidTags.LAVA)
					|| level.getBlockState(pos).is(BlockTags.FIRE)
					|| level.getBlockState(pos).is(Blocks.POWDER_SNOW)) return true;
		}
		return false;
	}

	private List<LivingEntity> combatTargets(ServerLevel level, double range, @Nullable LivingEntity excluded) {
		return level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range), candidate ->
				candidate != excluded && (candidate instanceof Enemy || candidate == this.getTarget())
						&& candidate.distanceToSqr(this) <= range * range && this.canAttack(candidate)
						&& this.hasLineOfSight(candidate)).stream()
				.sorted(Comparator.comparingDouble(this::distanceToSqr)).toList();
	}

	private void tickMovement(ServerLevel level, LivingEntity owner) {
		if (action() == ACTION_BACKSTEP || action() == ACTION_RECOVER || action() == ACTION_BOW_LOWER
				|| action() == ACTION_UNNOCK
				|| isVisualInteractionMovementOwned()) return;
		if (action() == ACTION_NOCK && this.getTarget() == null) {
			stopMovementIntent();
			return;
		}
		LivingEntity target = (action() == ACTION_MELEE || action() == ACTION_BACKSTEP)
				&& canDefendAgainst(this.actionTarget)
				? this.actionTarget : this.getTarget();
		if (action() == ACTION_MELEE || this.meleeEscapeActive) {
			List<LivingEntity> meleeThreats = meleeRetreatThreats(level, target);
			double nearestThreatDistance = meleeThreats.stream().mapToDouble(this::distanceTo)
					.min().orElse(Double.MAX_VALUE);
			if (nearestThreatDistance < MELEE_ESCAPE_RELEASE_RANGE) {
				tickCombatRetreat(level, meleeThreats, this.combatRetreatProgressPosition == Vec3.ZERO,
						MELEE_RETREAT_SPEED_SCALE, MAX_MELEE_RETREAT_SPEED, MELEE_RETREAT_SPEED_SCALE, true);
			} else {
				stopMovementIntent();
				this.meleeRetreatBlockedSince = -1L;
			}
			return;
		}
		if (target != null) {
			double distance = this.distanceTo(target);
			boolean hasSight = rangedAimTarget(target) != null;
			boolean boundaryIndependentSelfDefense = isRetainedSelfDefenseTarget(target)
					&& !canProtectAgainst(target);
			List<LivingEntity> threats = retreatThreats(level, target);
			double nearestThreatDistance = threats.stream().mapToDouble(this::distanceTo).min().orElse(Double.MAX_VALUE);
			if (skillEnabled(SKILL_CHARIOT_VOLLEY)) {
				boolean wasKiting = this.combatKiting;
				if (this.combatKiting) {
					this.combatKiting = nearestThreatDistance <= CLOSE_THREAT_RELEASE_RANGE;
				} else {
					this.combatKiting = nearestThreatDistance < CLOSE_THREAT_TRIGGER_RANGE;
				}
				if (this.combatKiting) {
					this.combatApproaching = false;
					tickCombatRetreat(level, threats, !wasKiting, DIRECT_RETREAT_SPEED_SCALE,
							MAX_DIRECT_RETREAT_SPEED, 1.05, false);
				} else {
					if (wasKiting) resetCombatRetreat();
					if (boundaryIndependentSelfDefense) stopMovementIntent();
					else tickCombatApproach(target, distance, hasSight);
				}
			} else if (boundaryIndependentSelfDefense) stopMovementIntent();
			else tickCombatApproach(target, distance, hasSight);
			return;
		}
		this.combatApproaching = false;
		this.combatKiting = false;
		resetCombatRetreat();
		if (this.activityMode == EchoRelicState.ActivityMode.FOLLOW) {
			double distance = this.distanceTo(owner);
			if (distance > 32.0) recallTo((Player)owner);
			else if (distance > 5.0) this.getNavigation().moveTo(owner, 1.0);
			else if (distance < 3.0) this.getNavigation().stop();
		}
	}

	private void tickCombatApproach(LivingEntity target, double distance, boolean hasSight) {
		if (!hasSight || distance > 20.0) this.combatApproaching = true;
		else if (distance <= 18.0) this.combatApproaching = false;
		if (this.combatApproaching) this.getNavigation().moveTo(target, 1.0);
		else stopMovementIntent();
	}

	private List<LivingEntity> retreatThreats(ServerLevel level, LivingEntity primaryTarget) {
		List<LivingEntity> threats = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
				this.getBoundingBox().inflate(RETREAT_SCAN_RANGE), candidate ->
						candidate != this && candidate.distanceToSqr(this) <= RETREAT_SCAN_RANGE * RETREAT_SCAN_RANGE
								&& (candidate == primaryTarget
										? canContinueCombatAgainst(candidate) : canAcquireCombatTarget(candidate))
								&& (candidate instanceof Enemy || candidate == primaryTarget)));
		if (canContinueCombatAgainst(primaryTarget) && !threats.contains(primaryTarget)) threats.add(primaryTarget);
		return threats;
	}

	private List<LivingEntity> meleeRetreatThreats(ServerLevel level, @Nullable LivingEntity primaryTarget) {
		LivingEntity recentAttacker = this.getLastHurtByMob();
		boolean recentDamage = isRecentWithin(this, this.getLastHurtByMobTimestamp(), 40);
		List<LivingEntity> threats = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
				this.getBoundingBox().inflate(CLOSE_THREAT_TRIGGER_RANGE), candidate ->
						candidate != this && candidate.distanceToSqr(this) <= CLOSE_THREAT_TRIGGER_RANGE * CLOSE_THREAT_TRIGGER_RANGE
								&& canDefendAgainst(candidate)
								&& (candidate instanceof Enemy || candidate == primaryTarget
										|| recentDamage && candidate == recentAttacker)
								&& (!(candidate instanceof Creeper creeper) || !CatGodCreeperSystem.isPanicking(creeper)
										|| recentDamage && candidate == recentAttacker)));
		if (canDefendAgainst(primaryTarget) && this.distanceToSqr(primaryTarget)
				<= CLOSE_THREAT_TRIGGER_RANGE * CLOSE_THREAT_TRIGGER_RANGE && !threats.contains(primaryTarget)) {
			threats.add(primaryTarget);
		}
		return threats;
	}

	private void tickCombatRetreat(ServerLevel level, List<LivingEntity> threats, boolean enteringRetreat,
			double speedScale, double maximumSpeed, double pathSpeed, boolean meleeRetreat) {
		long now = level.getGameTime();
		if (threats.isEmpty()) {
			this.combatKiting = false;
			resetCombatRetreat();
			stopMovementIntent();
			return;
		}
		if (this.combatRetreatProgressPosition == Vec3.ZERO) {
			this.combatRetreatProgressPosition = this.position();
			this.combatRetreatProgressAt = now;
		}
		boolean stuck = false;
		if (now - this.combatRetreatProgressAt >= 10L) {
			stuck = horizontalDistanceSqr(this.position(), this.combatRetreatProgressPosition)
					< RETREAT_MIN_PROGRESS * RETREAT_MIN_PROGRESS;
			this.combatRetreatProgressPosition = this.position();
			this.combatRetreatProgressAt = now;
			if (stuck) this.combatDirectRetreatBlockedUntil = now + 20L;
		}

		Vec3 directAway = stableRetreatDirection(threats);
		DirectRetreatClearance directClearance = directAway == Vec3.ZERO
				? DirectRetreatClearance.BLOCKED
				: directRetreatClearance(level, directAway, now);
		if (meleeRetreat) {
			if (stuck || directClearance == DirectRetreatClearance.BLOCKED) {
				if (this.meleeRetreatBlockedSince < 0L) this.meleeRetreatBlockedSince = now;
			} else {
				this.meleeRetreatBlockedSince = -1L;
			}
		}
		if (!stuck && now >= this.combatDirectRetreatBlockedUntil
				&& directClearance != DirectRetreatClearance.BLOCKED) {
			this.getNavigation().stop();
			this.getMoveControl().setWait();
			this.setSpeed(0.0F);
			this.setXxa(0.0F);
			this.setZza(0.0F);
			Vec3 wanted = this.position().add(directAway.scale(2.5));
			this.combatRetreatDestination = wanted;
			double minimumSpeed = meleeRetreat ? 0.0 : MIN_DIRECT_RETREAT_SPEED;
			double retreatSpeed = Mth.clamp(this.getAttributeValue(Attributes.MOVEMENT_SPEED)
					* speedScale, minimumSpeed, maximumSpeed);
			this.combatRetreatVelocity = directAway.scale(retreatSpeed);
			Vec3 currentVelocity = this.getDeltaMovement();
			double verticalVelocity = currentVelocity.y;
			if (directClearance == DirectRetreatClearance.STEP_UP && this.onGround()) {
				// aiStep runs after vanilla has already consumed JumpControl for this tick.
				// Give the retreat its ordinary jump impulse now as well as requesting the
				// controller jump, so a one-block wall cannot hold it in place for a tick.
				this.getJumpControl().jump();
				verticalVelocity = Math.max(verticalVelocity, DIRECT_RETREAT_JUMP_VELOCITY);
				this.combatStepJumpUntil = now + DIRECT_RETREAT_JUMP_TICKS;
				this.combatRetreatProgressPosition = this.position();
				this.combatRetreatProgressAt = now;
			}
			this.setDeltaMovement(this.combatRetreatVelocity.x, verticalVelocity, this.combatRetreatVelocity.z);
			return;
		}
		clearDirectRetreatMotion();

		boolean replan = enteringRetreat || stuck || this.getNavigation().isDone()
				|| !retreatDestinationStillUseful(threats);
		if (!replan) return;
		if (!enteringRetreat && !stuck && now < this.combatRetreatRepathAt) return;

		RetreatPath retreat = findRetreatPath(level, threats);
		if (retreat == null) {
			stopMovementIntent();
			this.combatRetreatDestination = Vec3.ZERO;
			this.combatRetreatRepathAt = now + 6L;
			return;
		}
		this.combatRetreatDestination = retreat.destination();
		this.combatRetreatDirection = retreat.direction();
		this.combatRetreatRepathAt = now + 8L;
		this.getNavigation().moveTo(retreat.path(), pathSpeed);
	}

	private Vec3 stableRetreatDirection(List<LivingEntity> threats) {
		Vec3 desired = rawRetreatDirection(threats);
		if (desired == Vec3.ZERO) return Vec3.ZERO;
		if (this.combatRetreatDirection == Vec3.ZERO
				|| this.combatRetreatDirection.dot(desired) < 0.2) {
			this.combatRetreatDirection = desired;
		} else {
			this.combatRetreatDirection = this.combatRetreatDirection.scale(0.8)
					.add(desired.scale(0.2)).normalize();
		}
		return this.combatRetreatDirection;
	}

	private Vec3 rawRetreatDirection(List<LivingEntity> threats) {
		Vec3 away = Vec3.ZERO;
		LivingEntity recentAttacker = this.getLastHurtByMob();
		boolean recentDamage = isRecentWithin(this, this.getLastHurtByMobTimestamp(), 40);
		for (LivingEntity threat : threats) {
			Vec3 delta = this.position().subtract(threat.position()).multiply(1.0, 0.0, 1.0);
			double horizontalSqr = delta.horizontalDistanceSqr();
			if (horizontalSqr > 1.0E-5) {
				double weight = 1.0 / horizontalSqr;
				if (recentDamage && threat == recentAttacker) weight *= RECENT_ATTACKER_RETREAT_WEIGHT;
				away = away.add(delta.scale(weight));
			}
		}
		if (away.horizontalDistanceSqr() < 1.0E-5) {
			LivingEntity nearest = threats.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
			if (nearest == null) return Vec3.ZERO;
			away = this.position().subtract(nearest.position()).multiply(1.0, 0.0, 1.0);
		}
		return away.horizontalDistanceSqr() < 1.0E-5 ? Vec3.ZERO : away.normalize();
	}

	private DirectRetreatClearance directRetreatClearance(ServerLevel level, Vec3 direction, long now) {
		boolean stepUp = false;
		for (double distance : DIRECT_RETREAT_PROBES) {
			Vec3 candidate = this.position().add(direction.scale(distance));
			AABB moved = this.getBoundingBox().move(candidate.subtract(this.position()));
			if (level.noCollision(this, moved) && hasRetreatSupport(level, candidate)
					&& !containsBackstepHazard(level, moved)) continue;

			// A one-block step is traversable only when the whole collision box is clear
			// one block higher and that raised position has safe support. While the jump
			// is already airborne, keep the horizontal retreat intent until the box has
			// actually risen above the step instead of cancelling it mid-jump.
			AABB stepped = moved.move(0.0, 1.0, 0.0);
			Vec3 steppedCandidate = candidate.add(0.0, 1.0, 0.0);
			boolean mayStep = this.onGround() || now < this.combatStepJumpUntil;
			if (!mayStep || level.noCollision(this, moved)
					|| !level.noCollision(this, stepped) || !hasRetreatSupport(level, steppedCandidate)
					|| containsBackstepHazard(level, stepped)) return DirectRetreatClearance.BLOCKED;
			stepUp = true;
		}
		return stepUp ? DirectRetreatClearance.STEP_UP : DirectRetreatClearance.CLEAR;
	}

	private boolean hasRetreatSupport(ServerLevel level, Vec3 candidate) {
		BlockPos feet = BlockPos.containing(candidate);
		BlockPos floor = feet.below();
		if (level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return true;
		BlockPos lowerFloor = floor.below();
		if (!level.getBlockState(lowerFloor).isFaceSturdy(level, lowerFloor, Direction.UP)) return false;
		AABB lowered = this.getBoundingBox().move(candidate.subtract(this.position())).move(0.0, -1.0, 0.0);
		return level.noCollision(this, lowered) && !containsBackstepHazard(level, lowered);
	}

	private void stopMovementIntent() {
		this.getNavigation().stop();
		this.getMoveControl().setWait();
		this.setSpeed(0.0F);
		this.setXxa(0.0F);
		this.setZza(0.0F);
		clearDirectRetreatMotion();
	}

	private void clearDirectRetreatMotion() {
		this.combatStepJumpUntil = 0L;
		if (this.combatRetreatVelocity == Vec3.ZERO) return;
		Vec3 currentVelocity = this.getDeltaMovement();
		this.setDeltaMovement(0.0, currentVelocity.y, 0.0);
		this.combatRetreatVelocity = Vec3.ZERO;
	}

	private boolean retreatDestinationStillUseful(List<LivingEntity> threats) {
		if (this.combatRetreatDestination == Vec3.ZERO) return false;
		double currentMinimum = threats.stream().mapToDouble(threat -> threat.distanceToSqr(this)).min().orElse(0.0);
		double destinationMinimum = threats.stream()
				.mapToDouble(threat -> threat.position().distanceToSqr(this.combatRetreatDestination))
				.min().orElse(0.0);
		return destinationMinimum > currentMinimum + 1.0;
	}

	private @Nullable RetreatPath findRetreatPath(ServerLevel level, List<LivingEntity> threats) {
		Vec3 away = stableRetreatDirection(threats);
		if (away == Vec3.ZERO) return null;
		double baseAngle = Math.atan2(away.z, away.x);
		double currentMinimum = threats.stream().mapToDouble(threat -> threat.distanceToSqr(this)).min().orElse(0.0);
		RetreatPath best = findRetreatPath(level, threats, away, baseAngle, currentMinimum,
				new int[] {0, 20, -20, 40, -40, 60, -60});
		if (best != null) return best;
		return findRetreatPath(level, threats, away, baseAngle, currentMinimum,
				new int[] {90, -90, 120, -120, 180});
	}

	private @Nullable RetreatPath findRetreatPath(ServerLevel level, List<LivingEntity> threats, Vec3 away,
			double baseAngle, double currentMinimum, int[] angleOffsets) {
		RetreatPath best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (double distance : new double[] {8.0, 6.0, 4.0}) {
			for (int angleOffset : angleOffsets) {
				double angle = baseAngle + Math.toRadians(angleOffset);
				Vec3 direction = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
				double x = this.getX() + Math.cos(angle) * distance;
				double z = this.getZ() + Math.sin(angle) * distance;
				for (int dy : new int[] {0, 1, -1, 2, -2}) {
					Vec3 candidate = new Vec3(x, this.getY() + dy, z);
					if (!isSafeRetreatDestination(level, candidate)) continue;
					double minimumDistance = threats.stream()
							.mapToDouble(threat -> threat.position().distanceToSqr(candidate)).min().orElse(0.0);
					if (minimumDistance <= currentMinimum + 1.0) continue;
					Path path = this.getNavigation().createPath(BlockPos.containing(candidate), 0);
					if (path == null || !path.canReach()) continue;
					double distanceGain = Math.sqrt(minimumDistance) - Math.sqrt(currentMinimum);
					double alignment = direction.dot(away);
					double score = distanceGain * 4.0 + alignment * 3.0 + distance * 0.05 - Math.abs(dy) * 0.2;
					if (score <= bestScore) continue;
					bestScore = score;
					best = new RetreatPath(candidate, path, direction);
				}
			}
		}
		return best;
	}

	private boolean isSafeRetreatDestination(ServerLevel level, Vec3 candidate) {
		BlockPos feet = BlockPos.containing(candidate);
		BlockPos floor = feet.below();
		if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
				|| !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()
				|| level.getBlockState(feet).is(BlockTags.FIRE) || level.getBlockState(feet).is(Blocks.POWDER_SNOW)) return false;
		AABB moved = this.getBoundingBox().move(candidate.subtract(this.position()));
		return level.noCollision(this, moved);
	}

	private void resetCombatRetreat() {
		this.combatRetreatDestination = Vec3.ZERO;
		this.combatRetreatDirection = Vec3.ZERO;
		clearDirectRetreatMotion();
		this.combatRetreatProgressPosition = Vec3.ZERO;
		this.combatRetreatRepathAt = 0L;
		this.combatRetreatProgressAt = 0L;
		this.combatDirectRetreatBlockedUntil = 0L;
	}

	private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
		double dx = first.x - second.x;
		double dz = first.z - second.z;
		return dx * dx + dz * dz;
	}

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
		if (this.entityData.get(VISUAL_REACTION_UNTIL) <= now && this.entityData.get(VISUAL_REACTION) != VISUAL_NORMAL) {
			this.entityData.set(VISUAL_REACTION, VISUAL_NORMAL);
			this.entityData.set(CURIOUS_TILT, (byte)0);
		}
		boolean wasCombatGazeLocked = this.entityData.get(COMBAT_GAZE_LOCKED);
		LivingEntity combatTarget = resolveCombatGazeTarget(now);
		boolean combatGazeLocked = canLockCombatGazeTo(combatTarget);
		this.entityData.set(COMBAT_GAZE_LOCKED, combatGazeLocked);
		this.entityData.set(COMBAT_GAZE_TARGET_ID, combatGazeLocked ? entityId(combatTarget) : -1);
		logCombatGazeTransition(now, combatTarget, combatGazeLocked);
		if (combatGazeLocked) {
			this.postCombatVisualSettleUntil = 0L;
			lockVisualAttentionToCombatTarget(combatTarget, now);
			return;
		}
		if (wasCombatGazeLocked) beginPostCombatVisualSettle(now);
		if (isTransientBowTargetLoss(now)) {
			// A selector gap during a committed draw must not hand the head to idle/wander
			// awareness for a few frames and then snap it back when the same target returns.
			// Keep the last synchronized combat point until the bow state resolves.
			return;
		}

		if (this.forcedVisualUntil > now) {
			return;
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

	private void beginPostCombatVisualSettle(long now) {
		long settleUntil = Math.max(now + POST_COMBAT_VISUAL_SETTLE_TICKS,
				projectedPostBowVisualSettleUntil(now));
		this.postCombatVisualSettleUntil = Math.max(this.postCombatVisualSettleUntil, settleUntil);
		this.postCombatVisualDiagnosticUntil = Math.max(this.postCombatVisualDiagnosticUntil,
				settleUntil + POST_COMBAT_VISUAL_DIAGNOSTIC_TICKS);
		Vec3 lastCombatDirection = this.attentionPoint.subtract(this.getEyePosition());
		if (lastCombatDirection.lengthSqr() <= 1.0E-4) {
			float yaw = this.yBodyRot * ((float)Math.PI / 180.0F);
			lastCombatDirection = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		}
		this.postCombatVisualSettleDirection = lastCombatDirection.normalize();
		this.lastLoggedPostCombatAttentionKind = null;
		this.lastLoggedPostCombatAttentionTargetId = Integer.MIN_VALUE;
		this.playerGazeProgress.clear();
	}

	private long projectedPostBowVisualSettleUntil(long now) {
		return switch (action()) {
			case ACTION_NOCK -> this.actionEndsAt + UNNOCK_TICKS + BOW_LOWER_TICKS
					+ POST_BOW_VISUAL_RELEASE_GRACE_TICKS;
			case ACTION_DRAW -> this.actionEndsAt + BOW_RECOVERY_TICKS
					+ POST_BOW_VISUAL_RELEASE_GRACE_TICKS;
			case ACTION_AIM -> now + TARGET_LOSS_GRACE_TICKS + BOW_RECOVERY_TICKS
					+ POST_BOW_VISUAL_RELEASE_GRACE_TICKS;
			case ACTION_SHOOT -> this.actionEndsAt + BOW_LOWER_TICKS
					+ POST_BOW_VISUAL_RELEASE_GRACE_TICKS;
			case ACTION_UNNOCK -> this.actionEndsAt + BOW_LOWER_TICKS
					+ POST_BOW_VISUAL_RELEASE_GRACE_TICKS;
			case ACTION_BOW_LOWER, ACTION_RECOVER -> this.actionEndsAt
					+ POST_BOW_VISUAL_RELEASE_GRACE_TICKS;
			default -> now;
		};
	}

	private void extendPostCombatVisualSettleThrough(long settleUntil) {
		this.postCombatVisualSettleUntil = Math.max(this.postCombatVisualSettleUntil, settleUntil);
		this.postCombatVisualDiagnosticUntil = Math.max(this.postCombatVisualDiagnosticUntil,
				settleUntil + 20L);
		if (this.headAttentionKind == AttentionKind.POST_COMBAT) {
			this.attentionExpiresAt = Math.max(this.attentionExpiresAt, settleUntil);
		}
		if (this.eyeAttentionKind == AttentionKind.POST_COMBAT) {
			this.eyeAttentionExpiresAt = Math.max(this.eyeAttentionExpiresAt, settleUntil);
		}
	}

	private boolean isTransientBowTargetLoss(long now) {
		return (action() == ACTION_NOCK || action() == ACTION_DRAW || action() == ACTION_AIM || action() == ACTION_SHOOT)
				&& this.targetLostAt >= 0L && now - this.targetLostAt < TARGET_LOSS_GRACE_TICKS;
	}

	private @Nullable LivingEntity resolveCombatGazeTarget(long now) {
		if (action() == ACTION_SHOOT && isCommittedTargetAlive(this.actionTarget)) {
			this.combatGazeTarget = this.actionTarget;
			this.combatGazeTargetUntil = now + COMBAT_GAZE_TARGET_GRACE_TICKS;
			return this.actionTarget;
		}
		if ((action() == ACTION_MELEE || action() == ACTION_BACKSTEP)
				&& canDefendAgainst(this.actionTarget)) {
			this.combatGazeTarget = this.actionTarget;
			this.combatGazeTargetUntil = now + COMBAT_GAZE_TARGET_GRACE_TICKS;
			return this.actionTarget;
		}
		LivingEntity currentTarget = this.getTarget();
		if (canContinueCombatAgainst(currentTarget)) {
			this.combatGazeTarget = currentTarget;
			this.combatGazeTargetUntil = now + COMBAT_GAZE_TARGET_GRACE_TICKS;
			return currentTarget;
		}
		if (isCombatGazeAction() && canContinueCombatAgainst(this.actionTarget)) {
			this.combatGazeTarget = this.actionTarget;
			this.combatGazeTargetUntil = now + COMBAT_GAZE_TARGET_GRACE_TICKS;
			return this.actionTarget;
		}
		if (isCombatGazeAction() && now <= this.combatGazeTargetUntil
				&& canContinueCombatAgainst(this.combatGazeTarget)) {
			return this.combatGazeTarget;
		}
		this.combatGazeTarget = null;
		this.combatGazeTargetUntil = 0L;
		return null;
	}

	private boolean canLockCombatGazeTo(@Nullable LivingEntity target) {
		return action() == ACTION_SHOOT && target == this.actionTarget
				? isCommittedTargetAlive(target)
				: (action() == ACTION_MELEE || action() == ACTION_BACKSTEP) && target == this.actionTarget
				? canDefendAgainst(target)
				: canContinueCombatAgainst(target);
	}

	private boolean isCombatGazeAction() {
		return action() == ACTION_NOCK || action() == ACTION_DRAW || action() == ACTION_AIM || action() == ACTION_SHOOT
				|| action() == ACTION_BACKSTEP || action() == ACTION_MELEE;
	}

	private void logCombatGazeTransition(long now, @Nullable LivingEntity resolvedTarget, boolean locked) {
		int resolvedId = resolvedTarget == null ? -1 : resolvedTarget.getId();
		byte currentAction = action();
		if (resolvedId == this.lastLoggedCombatGazeTargetId && locked == this.lastLoggedCombatGazeLocked
				&& currentAction == this.lastLoggedCombatGazeAction) return;
		LivingEntity selectedTarget = this.getTarget();
		EchoWarrior.LOGGER.info(
				"[EgyptianArcherGaze] archer={} tick={} action={} locked={} selected={} committed={} resolved={}",
				this.getId(), now, actionName(action()), locked, entityId(selectedTarget),
				entityId(this.actionTarget), resolvedId);
		this.lastLoggedCombatGazeTargetId = resolvedId;
		this.lastLoggedCombatGazeLocked = locked;
		this.lastLoggedCombatGazeAction = currentAction;
	}

	private static int entityId(@Nullable Entity entity) {
		return entity == null ? -1 : entity.getId();
	}

	private static String actionName(byte action) {
		return switch (action) {
			case ACTION_NOCK -> "nock";
			case ACTION_DRAW -> "draw";
			case ACTION_AIM -> "aim";
			case ACTION_SHOOT -> "shoot";
			case ACTION_BACKSTEP -> "backstep";
			case ACTION_MELEE -> "melee";
			case ACTION_BOW_LOWER -> "bow_lower";
			case ACTION_RECOVER -> "recover";
			case ACTION_UNNOCK -> "un_nock";
			default -> "idle";
		};
	}

	private static boolean isBowReturnActionState(byte action) {
		return action == ACTION_UNNOCK || action == ACTION_BOW_LOWER || action == ACTION_RECOVER;
	}

	private void lockVisualAttentionToCombatTarget(LivingEntity combatTarget, long now) {
		if (this.mutualGazePlayerUuid != null) endMutualGaze(now);
		if (isCaughtExitActive()) endCaughtExit(now, true);
		this.playerGazeProgress.clear();

		Entity aimTarget = action() == ACTION_SHOOT && combatTarget == this.actionTarget
				&& this.committedAimTarget != null && !this.committedAimTarget.isRemoved()
				? this.committedAimTarget
				: combatTarget instanceof EnderDragon ? rangedAimTarget(combatTarget) : combatTarget;
		if (aimTarget == null) aimTarget = combatTarget;
		Vec3 point = aimTarget instanceof LivingEntity living
				? living.getEyePosition()
				: aimTarget.getBoundingBox().getCenter();
		this.eyeAttentionTarget = combatTarget;
		this.eyeAttentionPoint = point;
		this.eyeAttentionPriority = 2000;
		this.eyeAttentionExpiresAt = now + 2L;
		this.eyeAttentionKind = AttentionKind.COMBAT_TARGET;
		this.eyeStickyUntil = now + 2L;
		this.attentionTarget = combatTarget;
		this.attentionPoint = point;
		this.attentionPriority = 2000;
		this.attentionExpiresAt = now + 2L;
		this.headAttentionKind = AttentionKind.COMBAT_TARGET;
		this.headStickyUntil = now + 2L;
		this.pendingHeadTarget = combatTarget;
		this.pendingHeadKind = AttentionKind.COMBAT_TARGET;
		this.pendingHeadSince = now;
		this.bodyAttentionTarget = null;
		this.bodyAttentionKind = AttentionKind.NORMAL;
		this.bodyAttentionExpiresAt = now;
		setEyeAttentionPoint(point);
		setAttentionPoint(point);
		this.entityData.set(CURIOUS_TILT, (byte)0);
		byte reaction = this.entityData.get(VISUAL_REACTION);
		if (reaction == VISUAL_NORMAL || reaction == VISUAL_CURIOUS || reaction == VISUAL_MUTUAL_GAZE
				|| reaction == VISUAL_CAUGHT || reaction == VISUAL_LOCOMOTION) {
			setReaction(VISUAL_ALERT, now + 2L);
		}
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
		return action() != ACTION_IDLE || target != null && target.isAlive()
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
		return action() != ACTION_IDLE || target != null && target.isAlive();
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
		boolean activelyMoving = isActuallyMovingForAttention();
		boolean locomotionAttention = shouldUseLocomotionAttention();
		boolean postCombatSettling = now < this.postCombatVisualSettleUntil;
		AttentionCandidate best = isVisibleAttentionTarget(combatTarget)
				? new AttentionCandidate(combatTarget, combatTarget.getEyePosition(), 800, VISUAL_ALERT, 30, false, AttentionKind.COMBAT_TARGET)
				: now < this.caughtExitOwnerAvoidUntil
						? new AttentionCandidate(null, this.caughtExitOwnerAvoidPoint, 220, VISUAL_NORMAL,
								35 + this.random.nextInt(36), false, AttentionKind.NORMAL)
						: postCombatSettling
								? activelyMoving
										? new AttentionCandidate(null, createLocomotionAttentionPoint(), LOCOMOTION_ATTENTION_PRIORITY,
												VISUAL_LOCOMOTION, LOCOMOTION_ATTENTION_TICKS, false, AttentionKind.LOCOMOTION)
										: new AttentionCandidate(null, createPostCombatVisualSettlePoint(), LOCOMOTION_ATTENTION_PRIORITY,
												VISUAL_LOCOMOTION, POST_COMBAT_VISUAL_SETTLE_TICKS, false, AttentionKind.POST_COMBAT)
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
			logPostCombatAttentionTransition(candidate, now);
		} else if (sameHeadTarget && this.attentionTarget.isAlive()) {
			setAttentionPoint(this.attentionTarget.getEyePosition());
		}

		configureBodyAttention(candidate, now);
	}

	private void logPostCombatAttentionTransition(AttentionCandidate candidate, long now) {
		if (now > this.postCombatVisualDiagnosticUntil) return;
		int targetId = entityId(candidate.target());
		if (candidate.kind() == this.lastLoggedPostCombatAttentionKind
				&& targetId == this.lastLoggedPostCombatAttentionTargetId) return;
		EchoWarrior.LOGGER.info(
				"[EgyptianArcherPostCombatGaze] archer={} tick={} action={} kind={} target={} priority={} settleRemaining={} moving={}",
				this.getId(), now, actionName(action()), candidate.kind().name().toLowerCase(Locale.ROOT),
				targetId, candidate.priority(), Math.max(0L, this.postCombatVisualSettleUntil - now),
				isActuallyMovingForAttention());
		this.lastLoggedPostCombatAttentionKind = candidate.kind();
		this.lastLoggedPostCombatAttentionTargetId = targetId;
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
		return action() == ACTION_IDLE && this.getTarget() == null
				&& !isRecent(this, this.getLastHurtByMobTimestamp())
				&& this.getNavigation().isDone();
	}

	private boolean shouldUseLocomotionAttention() {
		if (isVisualInteractionMovementOwned()) return false;
		boolean moving = isActuallyMovingForAttention();
		if (moving) {
			Vec3 movement = this.getDeltaMovement();
			Vec3 direction = new Vec3(movement.x, 0.0, movement.z);
			if (direction.lengthSqr() <= 1.0E-4) {
				float yaw = this.yBodyRot * ((float)Math.PI / 180.0F);
				direction = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
			}
			if (direction.lengthSqr() > 1.0E-4) this.locomotionAttentionDirection = direction.normalize();
			this.locomotionAttentionUntil = this.level().getGameTime() + LOCOMOTION_ATTENTION_RELEASE_TICKS;
			return true;
		}
		return this.level().getGameTime() < this.locomotionAttentionUntil;
	}

	private boolean isActuallyMovingForAttention() {
		if (isVisualInteractionMovementOwned()) return false;
		Vec3 movement = this.getDeltaMovement();
		double horizontalSpeedSqr = movement.x * movement.x + movement.z * movement.z;
		return !this.getNavigation().isDone() || horizontalSpeedSqr > 2.5E-4;
	}

	private Vec3 createLocomotionAttentionPoint() {
		Vec3 movement = this.getDeltaMovement();
		Vec3 direction = new Vec3(movement.x, 0.0, movement.z);
		if (direction.lengthSqr() > 1.0E-4) {
			direction = direction.normalize();
			this.locomotionAttentionDirection = direction;
		} else if (this.locomotionAttentionDirection.lengthSqr() > 1.0E-4) {
			direction = this.locomotionAttentionDirection;
		} else {
			float yaw = this.yBodyRot * ((float)Math.PI / 180.0F);
			direction = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
		}
		return this.getEyePosition().add(direction.scale(6.0));
	}

	private Vec3 createPostCombatVisualSettlePoint() {
		Vec3 direction = this.postCombatVisualSettleDirection;
		if (direction.lengthSqr() <= 1.0E-4) {
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

	public boolean isCombatGazeLocked() {
		return this.entityData.get(COMBAT_GAZE_LOCKED);
	}

	public int getCombatGazeTargetIdForDiagnostics() {
		return this.entityData.get(COMBAT_GAZE_TARGET_ID);
	}

	public boolean isRangedHeadFrameStabilized() {
		return action() == ACTION_NOCK || action() == ACTION_DRAW || action() == ACTION_AIM || action() == ACTION_SHOOT
				|| action() == ACTION_BOW_LOWER || action() == ACTION_RECOVER
				|| action() == ACTION_UNNOCK;
	}

	public byte getActionStateForDiagnostics() {
		return action();
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

	private enum AttentionKind {
		PRIMED_CREEPER,
		DAMAGE_SOURCE,
		CLOSE_CREEPER,
		COMBAT_TARGET,
		MUTUAL_GAZE,
		APPROACHING,
		LOCOMOTION,
		POST_COMBAT,
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
		LivingEntity current = this.getTarget();
		current = clearExpiredEmergencyTarget(current);
		LivingEntity retainedSelfDefense = validateSelfDefenseTarget();
		Monster boundaryThreat = selectBoundaryIndependentSelfDefenseTarget(owner, current, retainedSelfDefense);
		if (boundaryThreat != null) {
			retainSelfDefenseTarget(boundaryThreat, "boundary_close_threat");
			return boundaryThreat;
		}
		if (retainedSelfDefense != null) return retainedSelfDefense;
		LivingEntity emergency = this.emergencyTarget;
		Monster closeThreat = selectCloseThreat(owner, current, emergency == null);
		if (closeThreat != null) {
			if (this.highThreatTarget == null && closeThreat != current && canProtectAgainst(current)) {
				this.resumeTargetAfterThreat = current;
			}
			this.highThreatTarget = closeThreat;
			return closeThreat;
		}
		if (emergency != null) return emergency;
		if (this.highThreatTarget != null) {
			LivingEntity oldHighThreat = this.highThreatTarget;
			if (canContinueCombatAgainst(oldHighThreat)
					&& this.distanceToSqr(oldHighThreat) <= CLOSE_THREAT_RELEASE_RANGE * CLOSE_THREAT_RELEASE_RANGE) {
				if (rangedAimTarget(oldHighThreat) == null) {
					LivingEntity visibleReplacement = selectVisibleCombatReplacement(owner, oldHighThreat);
					if (visibleReplacement != null) {
						this.highThreatTarget = visibleReplacement instanceof Monster monster
								&& this.distanceToSqr(monster) <= CLOSE_THREAT_RELEASE_RANGE * CLOSE_THREAT_RELEASE_RANGE
								? monster : null;
						this.resumeTargetAfterThreat = null;
						return visibleReplacement;
					}
				}
				return oldHighThreat;
			}
			this.highThreatTarget = null;
			LivingEntity resumeTarget = this.resumeTargetAfterThreat;
			this.resumeTargetAfterThreat = null;
			if (canAcquireCombatTarget(resumeTarget)) return resumeTarget;
			if (current == oldHighThreat) current = null;
		}
		if (canContinueCombatAgainst(current)) {
			if (rangedAimTarget(current) == null) {
				LivingEntity visibleReplacement = selectVisibleCombatReplacement(owner, current);
				if (visibleReplacement != null) return visibleReplacement;
			}
			return current;
		}
		LivingEntity ownAttacker = this.getLastHurtByMob();
		if (isRecent(this, this.getLastHurtByMobTimestamp()) && canAcquireCombatTarget(ownAttacker)) return ownAttacker;
		if (this.alertMode == EchoRelicState.AlertMode.PEACEFUL) return null;
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isRecent(owner, owner.getLastHurtByMobTimestamp()) && canAcquireCombatTarget(ownerAttacker)) return ownerAttacker;
		LivingEntity ownerTarget = owner.getLastHurtMob();
		if (isRecent(owner, owner.getLastHurtMobTimestamp()) && canAcquireCombatTarget(ownerTarget)) return ownerTarget;
		if (this.alertMode != EchoRelicState.AlertMode.AGGRESSIVE) return null;
		double range = this.activityMode == EchoRelicState.ActivityMode.WAIT ? 6.0 : MAX_RANGE;
		return this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range),
				candidate -> candidate instanceof Enemy
						&& this.distanceToSqr(candidate) <= range * range
						&& canAcquireCombatTarget(candidate))
				.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
	}

	private @Nullable LivingEntity selectVisibleCombatReplacement(LivingEntity owner, LivingEntity hiddenCurrent) {
		LivingEntity ownAttacker = this.getLastHurtByMob();
		if (ownAttacker != hiddenCurrent && isRecent(this, this.getLastHurtByMobTimestamp())
				&& canAcquireCombatTarget(ownAttacker)) return ownAttacker;
		if (this.alertMode == EchoRelicState.AlertMode.PEACEFUL) return null;
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (ownerAttacker != hiddenCurrent && isRecent(owner, owner.getLastHurtByMobTimestamp())
				&& canAcquireCombatTarget(ownerAttacker)) return ownerAttacker;
		LivingEntity ownerTarget = owner.getLastHurtMob();
		if (ownerTarget != hiddenCurrent && isRecent(owner, owner.getLastHurtMobTimestamp())
				&& canAcquireCombatTarget(ownerTarget)) return ownerTarget;
		if (this.alertMode != EchoRelicState.AlertMode.AGGRESSIVE) return null;
		double range = this.activityMode == EchoRelicState.ActivityMode.WAIT ? 6.0 : MAX_RANGE;
		return this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range),
				candidate -> candidate != hiddenCurrent && candidate instanceof Enemy
						&& this.distanceToSqr(candidate) <= range * range
						&& canAcquireCombatTarget(candidate))
				.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
	}

	private @Nullable LivingEntity clearExpiredEmergencyTarget(@Nullable LivingEntity current) {
		if (this.emergencyTarget == null) return current;
		LivingEntity oldEmergency = this.emergencyTarget;
		boolean active = this.level().getGameTime() < this.emergencyTargetUntil
				&& (oldEmergency == current
						? canContinueCombatAgainst(oldEmergency) : canAcquireCombatTarget(oldEmergency))
				&& this.distanceToSqr(oldEmergency) <= MAX_RANGE * MAX_RANGE;
		if (active) return current;
		this.emergencyTarget = null;
		this.emergencyTargetUntil = 0L;
		LivingEntity resumeTarget = this.resumeTargetAfterEmergency;
		this.resumeTargetAfterEmergency = null;
		if (this.resumeTargetAfterThreat == oldEmergency) {
			this.resumeTargetAfterThreat = canContinueCombatAgainst(resumeTarget) ? resumeTarget : null;
		}
		return current == oldEmergency ? canContinueCombatAgainst(resumeTarget) ? resumeTarget : null : current;
	}

	private @Nullable Monster selectBoundaryIndependentSelfDefenseTarget(LivingEntity owner,
			@Nullable LivingEntity current, @Nullable LivingEntity retainedSelfDefense) {
		LivingEntity ownAttacker = this.getLastHurtByMob();
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		boolean ownDamageRecent = isRecent(this, this.getLastHurtByMobTimestamp());
		boolean ownerDamageRecent = isRecent(owner, owner.getLastHurtByMobTimestamp());
		List<Monster> candidates = this.level().getEntitiesOfClass(Monster.class,
				this.getBoundingBox().inflate(BACKSTEP_TRIGGER_RANGE), candidate -> {
					if (candidate.distanceToSqr(this) > BACKSTEP_TRIGGER_RANGE * BACKSTEP_TRIGGER_RANGE
							|| canProtectAgainst(candidate) || !canDefendAgainst(candidate)) return false;
					boolean continuing = candidate == current && canContinueCombatAgainst(candidate)
							|| candidate == retainedSelfDefense && canContinueCombatAgainst(candidate);
					if (!continuing && rangedAimTarget(candidate) == null) return false;
					boolean ownDamager = ownDamageRecent && candidate == ownAttacker;
					if (candidate instanceof Creeper creeper && CatGodCreeperSystem.isPanicking(creeper)
							&& !ownDamager) return false;
					if (this.alertMode == EchoRelicState.AlertMode.PEACEFUL) return ownDamager;
					return this.alertMode == EchoRelicState.AlertMode.AGGRESSIVE
							|| ownDamager || ownerDamageRecent && candidate == ownerAttacker
							|| candidate == current || candidate == retainedSelfDefense
							|| candidate.getTarget() == this || candidate.getTarget() == owner;
				});
		if (candidates.isEmpty()) return null;

		Comparator<Monster> threatOrder = Comparator.comparingInt((Monster candidate) -> closeThreatPriority(
				candidate, owner, ownAttacker, ownDamageRecent, ownerAttacker, ownerDamageRecent))
				.thenComparingDouble(this::distanceToSqr);
		Monster best = candidates.stream().min(threatOrder).orElse(null);
		if (!(retainedSelfDefense instanceof Monster sticky) || !candidates.contains(sticky) || best == null) return best;
		int bestPriority = closeThreatPriority(best, owner, ownAttacker, ownDamageRecent,
				ownerAttacker, ownerDamageRecent);
		int stickyPriority = closeThreatPriority(sticky, owner, ownAttacker, ownDamageRecent,
				ownerAttacker, ownerDamageRecent);
		return bestPriority < stickyPriority ? best : sticky;
	}

	private @Nullable Monster selectCloseThreat(LivingEntity owner, @Nullable LivingEntity current,
			boolean allowLingeringThreat) {
		boolean alreadyInCombat = canContinueCombatAgainst(current)
				|| this.alertMode == EchoRelicState.AlertMode.AGGRESSIVE;
		LivingEntity ownAttacker = this.getLastHurtByMob();
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		boolean ownDamageRecent = isRecent(this, this.getLastHurtByMobTimestamp());
		boolean ownerDamageRecent = isRecent(owner, owner.getLastHurtByMobTimestamp());
		List<Monster> candidates = new ArrayList<>(this.level().getEntitiesOfClass(Monster.class,
				this.getBoundingBox().inflate(CLOSE_THREAT_TRIGGER_RANGE), candidate ->
						candidate.distanceToSqr(this) <= CLOSE_THREAT_TRIGGER_RANGE * CLOSE_THREAT_TRIGGER_RANGE
								&& canAcquireCombatTarget(candidate)));
		if (allowLingeringThreat && this.highThreatTarget != null && canAcquireCombatTarget(this.highThreatTarget)
				&& this.distanceToSqr(this.highThreatTarget) <= CLOSE_THREAT_RELEASE_RANGE * CLOSE_THREAT_RELEASE_RANGE
				&& !candidates.contains(this.highThreatTarget)) {
			candidates.add(this.highThreatTarget);
		}
		candidates.removeIf(candidate -> {
					boolean recentDamager = ownDamageRecent && candidate == ownAttacker
							|| ownerDamageRecent && candidate == ownerAttacker;
					if (this.alertMode == EchoRelicState.AlertMode.PEACEFUL
							&& !(ownDamageRecent && candidate == ownAttacker)) return true;
					if (!alreadyInCombat && !recentDamager) return true;
					return candidate instanceof Creeper creeper && CatGodCreeperSystem.isPanicking(creeper)
							&& !recentDamager;
				});
		if (candidates.isEmpty()) return null;

		Comparator<Monster> threatOrder = Comparator.comparingInt((Monster candidate) -> closeThreatPriority(
				candidate, owner, ownAttacker, ownDamageRecent, ownerAttacker, ownerDamageRecent))
				.thenComparingDouble(this::distanceToSqr);
		Monster best = candidates.stream().min(threatOrder).orElse(null);
		Monster sticky = null;
		if (allowLingeringThreat && this.highThreatTarget != null && candidates.contains(this.highThreatTarget)) {
			sticky = this.highThreatTarget;
		} else if (current instanceof Monster currentMonster && candidates.contains(currentMonster)) {
			sticky = currentMonster;
		}
		if (best == null || sticky == null) return best;

		int bestPriority = closeThreatPriority(best, owner, ownAttacker, ownDamageRecent,
				ownerAttacker, ownerDamageRecent);
		int stickyPriority = closeThreatPriority(sticky, owner, ownAttacker, ownDamageRecent,
				ownerAttacker, ownerDamageRecent);
		// Distance only breaks ties when acquiring a threat. Once acquired, an equal-tier
		// enemy cannot steal focus every two ticks; only a strictly more dangerous enemy can.
		return bestPriority < stickyPriority ? best : sticky;
	}

	private int closeThreatPriority(Monster candidate, LivingEntity owner,
			@Nullable LivingEntity ownAttacker, boolean ownDamageRecent,
			@Nullable LivingEntity ownerAttacker, boolean ownerDamageRecent) {
		if (ownDamageRecent && candidate == ownAttacker || isInMeleeRange(candidate)) return 0;
		if (candidate.getTarget() == this || candidate.getTarget() == owner) return 1;
		if (ownerDamageRecent && candidate == ownerAttacker) return 2;
		return 3;
	}

	private static boolean isRecent(LivingEntity source, int timestamp) {
		return timestamp > 0 && source.tickCount - timestamp <= 100;
	}

	private static boolean isRecentWithin(LivingEntity source, int timestamp, int ticks) {
		return timestamp > 0 && source.tickCount - timestamp <= ticks;
	}

	private boolean canProtectAgainst(@Nullable LivingEntity target) {
		if (!canDefendAgainst(target)) return false;
		if (target instanceof Phantom && !canRangedAttack(target)) return false;
		if (this.activityMode == EchoRelicState.ActivityMode.WAIT) return target.position().distanceToSqr(this.activityAnchor) <= 64.0;
		if (this.activityMode == EchoRelicState.ActivityMode.WANDER) return target.position().distanceToSqr(this.activityAnchor) <= 256.0;
		return true;
	}

	private boolean canContinueCombatAgainst(@Nullable LivingEntity target) {
		return (canProtectAgainst(target) || isRetainedSelfDefenseTarget(target))
				&& !isCombatSightExpired(target);
	}

	private boolean isRetainedSelfDefenseTarget(@Nullable LivingEntity target) {
		return target != null && target == this.selfDefenseTarget && canDefendAgainst(target)
				&& this.distanceToSqr(target) <= SELF_DEFENSE_RELEASE_RANGE * SELF_DEFENSE_RELEASE_RANGE
				&& !isCombatSightExpired(target)
				&& (target == this.getTarget() || rangedAimTarget(target) != null);
	}

	private @Nullable LivingEntity validateSelfDefenseTarget() {
		LivingEntity target = this.selfDefenseTarget;
		if (target == null) return null;
		if (isRetainedSelfDefenseTarget(target)) return target;
		EchoWarrior.LOGGER.info(
				"[EgyptianArcherSelfDefense] archer={} tick={} event=release target={} distance={} anchorDistance={} activity={}",
				this.getId(), this.level().getGameTime(), entityId(target),
				formatDistance(Math.sqrt(this.distanceToSqr(target))),
				formatDistance(Math.sqrt(target.position().distanceToSqr(this.activityAnchor))), this.activityMode);
		this.selfDefenseTarget = null;
		return null;
	}

	private void retainSelfDefenseTarget(@Nullable LivingEntity target, String reason) {
		if (!canDefendAgainst(target)
				|| this.distanceToSqr(target) > SELF_DEFENSE_RELEASE_RANGE * SELF_DEFENSE_RELEASE_RANGE) return;
		if (this.distanceToSqr(target) < MELEE_ESCAPE_RELEASE_RANGE * MELEE_ESCAPE_RELEASE_RANGE) {
			// Basic close-quarters survival retreat remains active even when Chariot Soul is
			// disabled, and resumes automatically if a Backstep ends or is blocked inside 4 blocks.
			this.meleeEscapeActive = true;
		}
		if (this.selfDefenseTarget == target) return;
		LivingEntity previous = this.selfDefenseTarget;
		this.selfDefenseTarget = target;
		EchoWarrior.LOGGER.info(
				"[EgyptianArcherSelfDefense] archer={} tick={} event=acquire target={} previous={} reason={} distance={} anchorDistance={} activity={}",
				this.getId(), this.level().getGameTime(), entityId(target), entityId(previous), reason,
				formatDistance(Math.sqrt(this.distanceToSqr(target))),
				formatDistance(Math.sqrt(target.position().distanceToSqr(this.activityAnchor))), this.activityMode);
	}

	private boolean canDefendAgainst(@Nullable LivingEntity target) {
		return target != null && target.isAlive() && this.distanceToSqr(target) <= 1024.0 && this.canAttack(target);
	}

	private void enforceActivityBoundary(LivingEntity owner) {
		LivingEntity target = this.getTarget();
		if (target == null) return;
		if (isRetainedSelfDefenseTarget(target)) return;
		Vec3 center = this.activityMode == EchoRelicState.ActivityMode.FOLLOW ? owner.position() : this.activityAnchor;
		double giveUp = this.activityMode == EchoRelicState.ActivityMode.WAIT ? 8.0
				: this.activityMode == EchoRelicState.ActivityMode.WANDER ? 24.0 : 32.0;
		if (target.position().distanceToSqr(center) > giveUp * giveUp) this.setTarget(null);
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
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		Entity attacker = source.getEntity();
		if (attacker == this.getOwner() || attacker instanceof EchoWarriorEntity echo && echo.getOwner() == this.getOwner()) return false;
		long now = level.getGameTime();
		float previousHealth = this.getHealth();
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && damage > 0.0F) {
			endCaughtExit(now, true);
			if (attacker instanceof LivingEntity living && !isActivelyFighting()) {
				applyAttention(new AttentionCandidate(living, living.getEyePosition(), 1100,
						VISUAL_HURT, 16, false, AttentionKind.DAMAGE_SOURCE), now);
			} else {
				setReaction(VISUAL_HURT, now + 16);
				this.entityData.set(CURIOUS_TILT, (byte)0);
				this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
			}
			if (attacker instanceof LivingEntity livingAttacker && canDefendAgainst(livingAttacker)
					&& this.distanceToSqr(livingAttacker) <= MAX_RANGE * MAX_RANGE) {
				if (action() == ACTION_MELEE) this.meleeHitAgainDuringAction = true;
				Entity directAttacker = source.getDirectEntity();
				boolean closePhysicalHit = (directAttacker == null || directAttacker == livingAttacker)
						&& boundingBoxDistanceSqr(livingAttacker) <= DIRECT_HIT_MELEE_BOUNDING_BOX_GAP
								* DIRECT_HIT_MELEE_BOUNDING_BOX_GAP;
				if (this.distanceToSqr(livingAttacker) <= SELF_DEFENSE_RELEASE_RANGE * SELF_DEFENSE_RELEASE_RANGE) {
					retainSelfDefenseTarget(livingAttacker, closePhysicalHit ? "direct_melee_hit" : "close_damage_source");
				}
				if (canContinueCombatAgainst(livingAttacker)) {
					if (this.emergencyTarget == null && livingAttacker != this.getTarget()
							&& canContinueCombatAgainst(this.getTarget())) {
						this.resumeTargetAfterEmergency = this.getTarget();
					}
					LivingEntity previousEmergency = this.emergencyTarget;
					if (previousEmergency != null && previousEmergency != livingAttacker
							&& this.resumeTargetAfterThreat == previousEmergency) {
						this.resumeTargetAfterThreat = livingAttacker;
					}
					this.emergencyTarget = livingAttacker;
					this.emergencyTargetUntil = now + EMERGENCY_TARGET_TICKS;
					LivingEntity current = this.getTarget();
					if (action() != ACTION_MELEE && action() != ACTION_SHOOT
							&& (current == null || this.distanceToSqr(current) > CLOSE_THREAT_TRIGGER_RANGE * CLOSE_THREAT_TRIGGER_RANGE)) {
						this.setTarget(livingAttacker);
					}
				}
				if (closePhysicalHit && action() != ACTION_MELEE && action() != ACTION_BACKSTEP) {
					// A successful direct close-range hit is definitive evidence of an immediate
					// melee threat. This self-defense path intentionally ignores WAIT/WANDER
					// pursuit boundaries, and accepts a null direct entity because some melee
					// damage sources only expose their owning attacker.
					startMeleeAttack(now, livingAttacker);
				}
			}
			triggerHurtPresentation(now, action() == ACTION_IDLE);
		}
		this.reflectModuleMeleeDamage(level, source, previousHealth);
		return hurt;
	}

	private void triggerHurtPresentation(long now, boolean playBodyAnimation) {
		this.entityData.set(BLINK_START, now);
		this.entityData.set(BLINK_COUNT, (byte)1);
		if (playBodyAnimation) this.triggerAnim(ACTION_CONTROLLER, HURT_TRIGGER);
	}

	private void tickNaturalHealing(ServerLevel level, ItemStack relic) {
		long now = level.getGameTime();
		if (this.getHealth() >= this.getMaxHealth() || this.getTarget() != null
				|| this.tickCount - this.getLastHurtByMobTimestamp() < 100 || now - this.lastNaturalHealAt < 40L) return;
		LivingEntity owner = this.getOwner();
		if (!(owner instanceof Player player) || this.summonerUuid == null) return;
		ItemStack summoner = TestEchoSummonerItem.findSummonerStack(player, this.summonerUuid);
		if (summoner.isEmpty() || !SummonerFuel.consumeFractional(summoner, SummonerFuel.healCost(relic))) return;
		this.heal(1.0F);
		this.lastNaturalHealAt = now;
		level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 2, 0.15, 0.3, 0.15, 0.0);
	}

	private byte action() { return this.entityData.get(ACTION); }
	private int attackInterval() { return this.entityData.get(ATTACK_INTERVAL); }
	private boolean skillEnabled(int skill) { return (this.enabledSkills & 1 << skill) != 0; }
	private void finishAction() {
		byte completedAction = action();
		long now = this.level().getGameTime();
		// A completed PLAY_ONCE bow-return clip has already left the rendered
		// timeline. Calling stopTriggeredAnim here makes GeckoLib rebuild an
		// AnimationPoint from that completed clip and can expose one stale pose for a
		// frame. Melee is different: its RawAnimation holds the neutral final frame,
		// so clear that still-active hold when the committed action ends.
		boolean naturallyCompletedMelee = completedAction == ACTION_MELEE;
		if (naturallyCompletedMelee) {
			this.stopTriggeredAnim(ACTION_CONTROLLER, MELEE_TRIGGER);
		} else if (!isBowReturnActionState(completedAction)) {
			stopBowAnimations();
			this.stopTriggeredAnim(ACTION_CONTROLLER, BACKSTEP_TRIGGER);
			this.stopTriggeredAnim(ACTION_CONTROLLER, MELEE_TRIGGER);
		}
		this.entityData.set(ACTION, ACTION_IDLE);
		this.entityData.set(ACTION_DURATION_TICKS, 1);
		this.entityData.set(RANGED_RELOAD_STYLE, false);
		this.actionTarget = null;
		this.shotReleased = false;
		this.aimTicksRemaining = 0;
		this.aimTargetUuid = null;
		this.committedAimTarget = null;
		this.pendingBackstep = false;
		this.targetLostAt = -1L;
		this.meleeRetargetUsed = false;
		this.meleeHitAgainDuringAction = false;
		this.combatFacingInitialized = false;
		if (naturallyCompletedMelee) {
			EchoWarrior.LOGGER.info(
					"[EgyptianArcherMelee] archer={} tick={} event=finish heldFinal=true controllerStopped=true",
					this.getId(), now);
		}
		if (isBowReturnActionState(completedAction)) {
			EchoWarrior.LOGGER.info(
					"[EgyptianArcherRangedState] archer={} tick={} event=bow_return_finish previousAction={} settleRemaining={}",
					this.getId(), now, actionName(completedAction),
					Math.max(0L, this.postCombatVisualSettleUntil - now));
		}
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
		this.arrowMode = EchoRelicState.egyptianArrowMode(relic);
		this.entityData.set(ARROW_MODE, this.arrowMode.ordinal());
		this.entityData.set(ATTACK_INTERVAL, EchoRelicState.attackIntervalTicks(relic));
		if (previousActivity != this.activityMode || previousAlert != this.alertMode || resetAnchor) {
			this.setTarget(null);
			this.highThreatTarget = null;
			this.resumeTargetAfterThreat = null;
			this.emergencyTarget = null;
			this.resumeTargetAfterEmergency = null;
			this.emergencyTargetUntil = 0L;
			EchoActivityMovement.reset(this);
		}
		if (resetAnchor || this.activityAnchor == Vec3.ZERO) this.activityAnchor = this.position();
		double oldMaximum = this.getMaxHealth();
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(EchoRelicState.maximumHealth(relic));
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(EchoRelicState.attackDamage(relic));
		this.getAttribute(Attributes.ARMOR).setBaseValue(EchoRelicState.armor(relic));
		this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(EchoRelicState.movementSpeed(relic));
		this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(EchoRelicState.knockbackResistance(relic));
		this.applyModuleState();
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

	@Override public LivingEntity livingEntity() { return this; }
	@Override public EchoHeroType heroType() { return EchoHeroType.EGYPTIAN_ARCHER; }
	@Override public boolean shouldFollowOwner() {
		return this.activityMode == EchoRelicState.ActivityMode.FOLLOW
				&& action() != ACTION_BACKSTEP && !isVisualInteractionMovementOwned();
	}
	@Override public boolean isFollowMovementSuppressed() {
		return action() == ACTION_BACKSTEP || isVisualInteractionMovementOwned();
	}
	@Override public @Nullable UUID getOwnerUuid() { LivingEntity owner = getOwner(); return owner == null ? null : owner.getUUID(); }
	@Override public @Nullable UUID getSummonerUuid() { return this.summonerUuid; }
	@Override public @Nullable EntityReference<LivingEntity> getOwnerReference() { return this.ownerReference; }

	public boolean isCatGodActive() { return skillEnabled(SKILL_CAT_GOD); }

	@Override
	public void recallTo(Player player) {
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
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 24, 0.35, 0.7, 0.35, 0.02);
			level.playSound(null, this.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7F, 0.75F);
		}
		this.discard();
	}

	@Override public boolean canBreatheUnderwater() { return true; }
	@Override protected boolean shouldDropLoot(ServerLevel level) { return false; }

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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ownerReference = EntityReference.readWithOldOwnerConversion(input, "EchoOwner", this.level());
		try { this.summonerUuid = UUID.fromString(input.getStringOr("SummonerUuid", "")); }
		catch (IllegalArgumentException ignored) { this.summonerUuid = null; }
		this.activityMode = EchoRelicState.ActivityMode.byOrdinal(input.getIntOr("ActivityMode", 0));
		this.alertMode = EchoRelicState.AlertMode.byOrdinal(input.getIntOr("AlertMode", 1));
		this.enabledSkills = input.getIntOr("EnabledSkills", EchoHeroType.EGYPTIAN_ARCHER.defaultEnabledSkillsMask());
		this.activityAnchor = new Vec3(input.getDoubleOr("ActivityAnchorX", this.getX()),
				input.getDoubleOr("ActivityAnchorY", this.getY()), input.getDoubleOr("ActivityAnchorZ", this.getZ()));
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<EgyptianArcherEchoEntity>("movement", 3, this::selectMovementAnimation));
		AnimationController<EgyptianArcherEchoEntity> actionController =
				new AnimationController<EgyptianArcherEchoEntity>(ACTION_CONTROLLER, 1, this::selectActionAnimation)
				.triggerableAnim(FIRST_NOCK_TRIGGER, FIRST_NOCK_UPPER)
				.triggerableAnim(RELOAD_NOCK_TRIGGER, RELOAD_NOCK_UPPER)
				.triggerableAnim(FIRST_DRAW_TRIGGER, FIRST_DRAW_UPPER)
				.triggerableAnim(RELOAD_DRAW_TRIGGER, RELOAD_DRAW_UPPER)
				.triggerableAnim(BOW_AIM_TRIGGER, BOW_AIM_UPPER)
				.triggerableAnim(SHOOT_TRIGGER, SHOOT_UPPER)
				.triggerableAnim(UNNOCK_TRIGGER, UNNOCK_UPPER)
				.triggerableAnim(BOW_LOWER_TRIGGER, BOW_LOWER_UPPER)
				.triggerableAnim(BOW_RECOVER_TRIGGER, BOW_RECOVER_UPPER)
				.triggerableAnim(BACKSTEP_TRIGGER, BACKSTEP)
				.triggerableAnim(MELEE_TRIGGER, MELEE)
				.triggerableAnim(HURT_TRIGGER, HURT);
		controllers.add(actionController);
	}

	private PlayState selectMovementAnimation(AnimationTest<EgyptianArcherEchoEntity> test) {
		EgyptianArcherEchoEntity archer = test.animatable();
		int currentTick = archer.tickCount;
		double horizontalSpeed = archer.getDeltaMovement().horizontalDistance();
		if (isBowReturnActionState(archer.action())) {
			if (!this.bowReturnMovementFrameLocked) {
				this.bowReturnMovementFrameLocked = true;
				this.movementAnimationActive = false;
				EchoWarrior.LOGGER.info(
						"[EgyptianArcherBowBoundaryClient] archer={} tick={} event=idle_base_lock action={}",
						archer.getId(), archer.level().getGameTime(), actionName(archer.action()));
			}
			this.bowReturnMovementReleaseDeferredAtTick = Integer.MIN_VALUE;
			PlayState playState = test.setAndContinue(IDLE);
			test.controller().setTimelineTime(0.0D);
			test.setControllerSpeed(0.0F);
			return playState;
		}
		if (this.bowReturnMovementFrameLocked) {
			if (this.bowReturnMovementReleaseDeferredAtTick == Integer.MIN_VALUE) {
				this.bowReturnMovementReleaseDeferredAtTick = currentTick;
				EchoWarrior.LOGGER.info(
						"[EgyptianArcherBowBoundaryClient] archer={} tick={} event=idle_base_release_deferred action={}",
						archer.getId(), archer.level().getGameTime(), actionName(archer.action()));
			}
			// Entity-data synchronization and the action controller can cross the
			// PLAY_ONCE end boundary on different render frames inside the same game
			// tick. Keep the matching idle frame underneath for that whole tick so no
			// interpolation frame can expose the model's reset pose.
			if (currentTick <= this.bowReturnMovementReleaseDeferredAtTick) {
				PlayState playState = test.setAndContinue(IDLE);
				test.controller().setTimelineTime(0.0D);
				test.setControllerSpeed(0.0F);
				return playState;
			}
			this.bowReturnMovementFrameLocked = false;
			this.bowReturnMovementReleaseDeferredAtTick = Integer.MIN_VALUE;
			EchoWarrior.LOGGER.info(
					"[EgyptianArcherBowBoundaryClient] archer={} tick={} event=idle_base_release action={}",
					archer.getId(), archer.level().getGameTime(), actionName(archer.action()));
		}
		boolean visiblyMoving = test.isMoving() && horizontalSpeed > 0.025;
		if (visiblyMoving && archer.action() != ACTION_BACKSTEP) {
			this.movementAnimationActive = true;
			this.movementAnimationLastMovingTick = currentTick;
		} else if (this.movementAnimationActive && currentTick - this.movementAnimationLastMovingTick >= 4) {
			this.movementAnimationActive = false;
		}
		if (this.movementAnimationActive) {
			test.setControllerSpeed(Mth.clamp((float)(horizontalSpeed / 0.12), 0.65F, 1.6F));
			return test.setAndContinue(WALK);
		}
		test.setControllerSpeed(1.0F);
		return test.setAndContinue(IDLE);
	}

	private PlayState selectActionAnimation(AnimationTest<EgyptianArcherEchoEntity> test) {
		EgyptianArcherEchoEntity archer = test.animatable();
		float sourceSeconds = switch (archer.action()) {
			case ACTION_NOCK -> archer.entityData.get(RANGED_RELOAD_STYLE)
					? RELOAD_NOCK_ANIMATION_SECONDS : FIRST_NOCK_ANIMATION_SECONDS;
			case ACTION_DRAW -> DRAW_ANIMATION_SECONDS;
			case ACTION_SHOOT -> RELEASE_ANIMATION_SECONDS;
			case ACTION_UNNOCK -> RELOAD_NOCK_ANIMATION_SECONDS;
			default -> 0.0F;
		};
		if (sourceSeconds > 0.0F) {
			test.setControllerSpeed(sourceSeconds * 20.0F
					/ Math.max(1, archer.entityData.get(ACTION_DURATION_TICKS)));
		} else test.setControllerSpeed(1.0F);
		return PlayState.STOP;
	}

	@Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.animationCache; }

	private static float yawToward(double fromX, double fromZ, double targetX, double targetZ) {
		return (float)(Math.atan2(targetZ - fromZ, targetX - fromX) * 180.0 / Math.PI) - 90.0F;
	}

	private record RetreatPath(Vec3 destination, Path path, Vec3 direction) {
	}

	private record RangedPhaseBudget(int interval, int nockTicks, int drawTicks, int aimTicks, int releaseTicks) {
	}

	private enum DirectRetreatClearance {
		BLOCKED,
		CLEAR,
		STEP_UP
	}
}
