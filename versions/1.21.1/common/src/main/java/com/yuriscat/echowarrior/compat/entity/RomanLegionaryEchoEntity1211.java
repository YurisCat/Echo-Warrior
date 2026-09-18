package com.yuriscat.echowarrior.compat.entity;

import com.mojang.serialization.Dynamic;
import com.yuriscat.echowarrior.compat.EchoHeroType1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.ModDamageTypes1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.combat.ShieldChargeCreeperControl1211;
import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1211;
import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1211;
import com.yuriscat.echowarrior.compat.progress.EchoExperienceSystem1211;
import com.yuriscat.echowarrior.compat.mixin.ShulkerBulletAccessor1211;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.SmartBrainProvider;
import net.tslat.smartbrainlib.api.core.behaviour.OneRandomBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.FollowOwner;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetRandomWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetWalkTargetToAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.util.BrainUtils;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class RomanLegionaryEchoEntity1211 extends TamableAnimal
        implements GeoEntity, SmartBrainOwner<RomanLegionaryEchoEntity1211>, EchoWarriorEntity1211 {
    public static final byte VISUAL_NORMAL = 0;
    public static final byte VISUAL_ALERT = 1;
    public static final byte VISUAL_STARTLED = 2;
    public static final byte VISUAL_HURT = 3;
    public static final byte VISUAL_CURIOUS = 4;
    public static final byte VISUAL_MUTUAL_GAZE = 5;
    public static final byte VISUAL_CAUGHT = 6;
    public static final byte VISUAL_LOCOMOTION = 7;
    private static final String SUMMONER_ID_KEY = "EchoWarriorSummonerId";
    private static final int SKILL_FORMATION = 0;
    private static final int SKILL_BULWARK = 1;
    private static final int SKILL_SHIELD_CHARGE = 2;
    private static final int SKILL_LEGION_ENDURES = 3;
    private static final int ACTIVITY_FOLLOW = 0;
    private static final int ACTIVITY_WAIT = 1;
    private static final int ACTIVITY_WANDER = 2;
    private static final int ALERT_AGGRESSIVE = 0;
    private static final int ALERT_DEFENSIVE = 1;
    private static final int ALERT_PEACEFUL = 2;
    private static final byte MELEE_NONE = 0;
    private static final byte MELEE_FIRST = 1;
    private static final byte MELEE_RECOVER = 2;
    private static final byte MELEE_FOLLOW = 3;
    private static final int VISUAL_KIND_NORMAL = 0;
    private static final int VISUAL_KIND_LOCOMOTION = 1;
    private static final int VISUAL_KIND_APPROACHING = 2;
    private static final int VISUAL_KIND_COMBAT = 3;
    private static final int VISUAL_KIND_CLOSE_CREEPER = 4;
    private static final int VISUAL_KIND_DAMAGE = 5;
    private static final int VISUAL_KIND_PRIMED_CREEPER = 6;
    private static final int VISUAL_KIND_MUTUAL_GAZE = 7;
    private static final int VISUAL_KIND_CAUGHT = 8;
    private static final double HEAD_GAZE_RADIUS = 0.35;
    private static final double VISUAL_HEAD_CENTER_HEIGHT = 27.5 / 16.0;
    private static final double INVISIBLE_GAZE_RANGE = 4.0;
    private static final int GAZE_MISS_TOLERANCE_TICKS = 2;
    private static final int COMBAT_GAZE_SUPPRESSION_TICKS = 60;
    private static final int MUTUAL_GAZE_PRIORITY = 790;
    private static final int CAUGHT_PREWATCH_TICKS = 8;
    private static final int CAUGHT_GLANCE_START_TICKS = 18;
    private static final double CAUGHT_MAX_OWNER_DISTANCE_SQR = 16.0 * 16.0;
    private static final double CAUGHT_EXIT_WALK_ELIGIBLE_OWNER_DISTANCE_SQR = 9.0 * 9.0;
    private static final double CAUGHT_EXIT_MAX_OWNER_DISTANCE_SQR = 12.0 * 12.0;
    private static final double CAUGHT_EXIT_FOLLOW_CANCEL_DISTANCE_SQR = 15.0 * 15.0;
    private static final float CAUGHT_EXIT_MIN_OWNER_ANGLE = 70.0F;
    private static final float CAUGHT_EXIT_MAX_WALK_ANGLE = 130.0F;
    private static final float FOLLOW_DAMAGE_MULTIPLIER = 0.75F;
    private static final int BASE_ATTACK_INTERVAL_TICKS = 20;
    private static final int BASE_FIRST_TICKS = 11;
    private static final int BASE_FIRST_HIT_TICKS = 6;
    private static final int BASE_FOLLOW_TICKS = 10;
    private static final int BASE_FOLLOW_HIT_TICKS = 5;
    private static final int BASE_RECOVER_TICKS = 5;
    private static final float BULWARK_MULTIPLIER = 0.50F;
    private static final float FORMATION_SHIELD_MULTIPLIER = 0.85F;
    private static final double FIRST_STRIKE_RANGE = 2.25;
    private static final double FOLLOW_COMMIT_RANGE = 2.75;
    private static final double FOLLOW_STRIKE_RANGE = 3.10;
    private static final double ATTACK_TRACKING_STOP_RANGE = 1.70;
    private static final double FIRST_TRACKING_SPEED = 0.30;
    private static final double FIRST_TRACKING_LIMIT = 1.50;
    private static final double FOLLOW_TRACKING_SPEED = 0.20;
    private static final double FOLLOW_TRACKING_LIMIT = 0.75;
    private static final double ATTACK_TRACKING_MAX_SUBSTEP = 0.25;
    private static final float ATTACK_TRACKING_TURN_PER_TICK = 15.0F;
    private static final float ATTACK_TRACKING_TOTAL_TURN = 45.0F;
    private static final String ACTION_CONTROLLER = "action";
    private static final String ATTACK_FIRST_TRIGGER = "attack_first";
    private static final String ATTACK_RECOVER_TRIGGER = "attack_recover";
    private static final String HURT_TRIGGER = "hurt";
    private static final double SHIELD_PROJECTILE_INTERCEPT_RADIUS = 1.5;
    private static final ResourceLocation FORMATION_ATTACK_ID = ModContent1211.id("soldier_formation_attack");
    private static final ResourceLocation LEGION_ARMOR_ID = ModContent1211.id("legion_endures_armor");
    private static final ResourceLocation LEGION_KNOCKBACK_ID = ModContent1211.id("legion_endures_knockback");
    private static final AttributeModifier FORMATION_ATTACK = new AttributeModifier(
            FORMATION_ATTACK_ID, 2.0, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier LEGION_ARMOR = new AttributeModifier(
            LEGION_ARMOR_ID, 12.0, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier LEGION_KNOCKBACK = new AttributeModifier(
            LEGION_KNOCKBACK_ID, 1.0, AttributeModifier.Operation.ADD_VALUE);
    private static final EntityDataAccessor<Boolean> SHIELD_RAISED = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> MELEE_ACTION = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> MELEE_ADVANCING = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> MELEE_ACTION_SPEED = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ATTENTION_X = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ATTENTION_Y = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ATTENTION_Z = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EYE_ATTENTION_X = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EYE_ATTENTION_Y = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> EYE_ATTENTION_Z = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> VISUAL_REACTION = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> BLINK_START = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Byte> BLINK_COUNT = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> CURIOUS_TILT = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> VISUAL_SEQUENCE = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ATTENTION_STARTED_AT = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> CAUGHT_REACTION_START = SynchedEntityData.defineId(
            RomanLegionaryEchoEntity1211.class, EntityDataSerializers.LONG);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.roman_legionary.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.roman_legionary.walk");
    private static final RawAnimation ATTACK_FIRST = RawAnimation.begin()
            .thenPlay("animation.roman_legionary.attack_first")
            .thenPlayAndHold("animation.roman_legionary.attack_follow");
    private static final RawAnimation ATTACK_RECOVER = RawAnimation.begin().thenPlay("animation.roman_legionary.attack_recover");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.roman_legionary.hurt");
    private static final RawAnimation SHIELD_RAISE = RawAnimation.begin().thenPlayAndHold("animation.roman_legionary.shield_raise");
    private static final RawAnimation SHIELD_LOWER = RawAnimation.begin().thenPlay("animation.roman_legionary.shield_lower");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Set<UUID> reflectedProjectiles = new HashSet<>();
    private UUID summonerId;
    private long bindingGeneration;
    private int activityMode = ACTIVITY_FOLLOW;
    private int alertMode = ALERT_DEFENSIVE;
    private int enabledSkills = 0b1111;
    private Vec3 activityAnchor = Vec3.ZERO;
    private boolean formationActive;
    private long targetLastSeenAt;
    private UUID meleeTargetId;
    private long nextMeleeAttackAt;
    private long meleeEndsAt;
    private long meleeHitAt;
    private boolean meleeHitResolved;
    private int committedMeleeInterval = BASE_ATTACK_INTERVAL_TICKS;
    private float attackTrackingStartYaw;
    private double attackTrackingTravelled;
    private boolean attackTrackingStopped = true;
    private boolean attackTrackingMovedThisTick;
    private long lastNaturalHealAt;
    private Entity shieldChargeTarget;
    private long shieldChargeStartedAt;
    private long shieldChargeUntil;
    private long shieldInternalCooldownUntil;
    private UUID recentlyChargedTargetId;
    private long recentlyChargedTargetUntil;
    private long legionEnduresUntil;
    private float legionAccumulatedDamage;
    private boolean shieldAnimationWasRaised;
    private int shieldLowerAnimationUntil = Integer.MIN_VALUE;
    private long nextBlinkAt;
    private long visualDamageUntil;
    private UUID visualDamageSourceId;
    private Vec3 visualDamagePoint = Vec3.ZERO;
    private long visualLocomotionUntil;
    private Vec3 visualLocomotionDirection = Vec3.ZERO;
    private long nextVisualThreatScanAt;
    private VisualCandidate scannedVisualThreat;
    private long scannedVisualThreatUntil;
    private UUID quietVisualTargetId;
    private Vec3 quietVisualPoint = Vec3.ZERO;
    private long quietVisualUntil;
    private boolean quietVisualCurious;
    private UUID activeVisualTargetId;
    private int activeVisualKind = -1;
    private final Map<UUID, PlayerGazeProgress> playerGazeProgress = new HashMap<>();
    private UUID mutualGazePlayerId;
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
    private long caughtExitEndsAt = -1L;
    private LivingEntity caughtExitFocusTarget;
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

    public RomanLegionaryEchoEntity1211(EntityType<? extends RomanLegionaryEchoEntity1211> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        EchoHeroType1211 hero = EchoHeroType1211.ROMAN_LEGIONARY;
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, hero.maximumHealth())
                .add(Attributes.ATTACK_DAMAGE, hero.attackDamage())
                .add(Attributes.ARMOR, hero.armor())
                .add(Attributes.MOVEMENT_SPEED, hero.movementSpeed())
                .add(Attributes.KNOCKBACK_RESISTANCE, hero.knockbackResistance())
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHIELD_RAISED, false);
        builder.define(MELEE_ACTION, MELEE_NONE);
        builder.define(MELEE_ADVANCING, false);
        builder.define(MELEE_ACTION_SPEED, 1.0F);
        builder.define(ATTENTION_X, 0.0F);
        builder.define(ATTENTION_Y, 0.0F);
        builder.define(ATTENTION_Z, 0.0F);
        builder.define(EYE_ATTENTION_X, 0.0F);
        builder.define(EYE_ATTENTION_Y, 0.0F);
        builder.define(EYE_ATTENTION_Z, 0.0F);
        builder.define(VISUAL_REACTION, VISUAL_NORMAL);
        builder.define(BLINK_START, -100L);
        builder.define(BLINK_COUNT, (byte)0);
        builder.define(CURIOUS_TILT, (byte)0);
        builder.define(VISUAL_SEQUENCE, 0);
        builder.define(ATTENTION_STARTED_AT, 0L);
        builder.define(CAUGHT_REACTION_START, -100L);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected Brain.Provider<?> brainProvider() {
        return new SmartBrainProvider<>(this);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return this.brainProvider().makeBrain(dynamic);
    }

    @Override
    protected void customServerAiStep() {
        tickBrain(this);
    }

    @Override
    public List<? extends ExtendedSensor<? extends RomanLegionaryEchoEntity1211>> getSensors() {
        return List.of(new NearbyLivingEntitySensor<>());
    }

    @Override
    public BrainActivityGroup<? extends RomanLegionaryEchoEntity1211> getCoreTasks() {
        return BrainActivityGroup.coreTasks(new MoveToWalkTarget<>());
    }

    @Override
    public BrainActivityGroup<? extends RomanLegionaryEchoEntity1211> getIdleTasks() {
        return BrainActivityGroup.idleTasks(
                new FollowOwner<RomanLegionaryEchoEntity1211>()
                        .startCondition(entity -> entity.activityMode == ACTIVITY_FOLLOW && entity.canAct()
                                && !entity.isSocialMovementOwned()),
                new OneRandomBehaviour<RomanLegionaryEchoEntity1211>(
                        new SetRandomWalkTarget<RomanLegionaryEchoEntity1211>()
                                .speedModifier(0.8F)
                                .startCondition(entity -> entity.activityMode == ACTIVITY_WANDER && entity.canAct()
                                        && !entity.isSocialMovementOwned()),
                        new Idle<RomanLegionaryEchoEntity1211>().runFor(entity -> entity.getRandom().nextInt(30, 61)))
        );
    }

    @Override
    public BrainActivityGroup<? extends RomanLegionaryEchoEntity1211> getFightTasks() {
        return BrainActivityGroup.fightTasks(
                new InvalidateAttackTarget<RomanLegionaryEchoEntity1211>()
                        .invalidateIf((entity, target) -> !entity.canRetainCombatTarget(target)),
                new SetWalkTargetToAttackTarget<RomanLegionaryEchoEntity1211>()
                        .speedMod(1.15F)
                        .closeEnoughDist((entity, target) -> 1)
                        .startCondition(RomanLegionaryEchoEntity1211::canAct)
        );
    }

    private boolean canAct() {
        return this.entityData.get(MELEE_ACTION) == MELEE_NONE
                && this.shieldChargeTarget == null && !isLegionEnduresActive();
    }

    private boolean isSocialMovementOwned() {
        return this.mutualGazePlayerId != null || this.caughtExitMode != CaughtExitMode.NONE;
    }

    public void bindTo(Player owner, UUID summonerId) {
        bindTo(owner, summonerId, this.bindingGeneration);
    }

    public void bindTo(Player owner, UUID summonerId, long generation) {
        this.setOwnerUUID(owner.getUUID());
        this.setTame(true, false);
        this.summonerId = summonerId;
        this.bindingGeneration = generation;
        if (this.activityAnchor == Vec3.ZERO) this.activityAnchor = this.position();
        if (this.level() instanceof ServerLevel level) {
            applyVisualCandidate(new VisualCandidate(owner, owner.getEyePosition(), 220,
                    VISUAL_NORMAL, VISUAL_KIND_NORMAL, false), level.getGameTime());
            EchoBindingSavedData1211.Binding binding = EchoBindingSavedData1211.get(level.getServer()).get(summonerId);
            if (binding != null) applyBindingState(binding, true);
        }
    }

    public void applyBindingState(EchoBindingSavedData1211.Binding binding, boolean resetAnchor) {
        int previousActivity = this.activityMode;
        this.activityMode = binding.activityMode();
        this.alertMode = binding.alertMode();
        this.enabledSkills = binding.enabledSkills();
        this.formationActive = skillEnabled(SKILL_FORMATION);
        applyRelicState(binding.relic(), false);
        EchoAccessorySystem1211.apply(this, binding.accessories());
        if (resetAnchor || previousActivity != this.activityMode || this.activityAnchor == Vec3.ZERO) {
            this.activityAnchor = this.position();
            clearCombatAndMovement();
        }
        if (!skillEnabled(SKILL_SHIELD_CHARGE) && this.shieldChargeTarget != null) stopShieldCharge();
        if (!skillEnabled(SKILL_LEGION_ENDURES) && isLegionEnduresActive()) cancelLegionEndures();
    }

    public UUID getSummonerId() { return this.summonerId; }
    public long getBindingGeneration() { return this.bindingGeneration; }
    @Override public LivingEntity livingEntity() { return this; }
    @Override public EchoHeroType1211 heroType() { return EchoHeroType1211.ROMAN_LEGIONARY; }
    public void setSummonerIdForTest(UUID summonerId) { this.summonerId = summonerId; }
    public void setBindingGenerationForTest(long generation) { this.bindingGeneration = generation; }
    public boolean isFormationActive() { return this.formationActive; }
    public boolean isLegionEnduresActive() { return this.level().getGameTime() < this.legionEnduresUntil; }
    public boolean isShieldRaised() { return this.entityData.get(SHIELD_RAISED); }
    public Vec3 getSyncedAttentionPoint() { return syncedVisualPoint(ATTENTION_X, ATTENTION_Y, ATTENTION_Z); }
    public Vec3 getSyncedEyeAttentionPoint() { return syncedVisualPoint(EYE_ATTENTION_X, EYE_ATTENTION_Y, EYE_ATTENTION_Z); }
    public byte getVisualReaction() { return this.entityData.get(VISUAL_REACTION); }
    public long getBlinkStart() { return this.entityData.get(BLINK_START); }
    public byte getBlinkCount() { return this.entityData.get(BLINK_COUNT); }
    public byte getCuriousTilt() { return this.entityData.get(CURIOUS_TILT); }
    public int getVisualSequence() { return this.entityData.get(VISUAL_SEQUENCE); }
    public long getAttentionStartedAt() { return this.entityData.get(ATTENTION_STARTED_AT); }
    public long getCaughtReactionStart() { return this.entityData.get(CAUGHT_REACTION_START); }

    public ItemStack activeRelic() {
        return this.level() instanceof ServerLevel level && this.summonerId != null
                ? EchoBindingSystem1211.relic(level, this.summonerId) : ItemStack.EMPTY;
    }

    public void applyRelicState(ItemStack relic, boolean preserveHealthGain) {
        if (relic.isEmpty()) return;
        double oldMaximum = this.getMaxHealth();
        AttributeInstance maximumHealth = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance attackDamage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
        AttributeInstance movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (maximumHealth != null) maximumHealth.setBaseValue(EchoRelicState1211.maximumHealth(relic));
        if (attackDamage != null) attackDamage.setBaseValue(EchoRelicState1211.attackDamage(relic));
        if (armor != null) armor.setBaseValue(EchoRelicState1211.armor(relic));
        if (movement != null) movement.setBaseValue(EchoRelicState1211.movementSpeed(relic));
        if (preserveHealthGain) {
            this.setHealth(Math.min(this.getMaxHealth(), this.getHealth()
                    + (float)Math.max(0.0, this.getMaxHealth() - oldMaximum)));
        } else if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    public void aiStep() {
        if (this.level() instanceof ServerLevel level && !EchoBindingSystem1211.validateAndTrack(this)) {
            this.discard();
            return;
        }
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) return;
        this.setAirSupply(this.getMaxAirSupply());
        long now = level.getGameTime();
        LivingEntity owner = getOwner();
        boolean ownerAvailable = owner instanceof Player player && player.isAlive() && !player.isSpectator()
                && player.level() == this.level();
        if (!ownerAvailable) owner = this;

        if (this.tickCount % 10 == 0 && this.summonerId != null) {
            EchoBindingSavedData1211.Binding binding = currentBinding(level);
            if (binding != null) applyBindingState(binding, false);
        }
        tickCombatSkills(level, owner, ownerAvailable, now);
        if (this.entityData.get(MELEE_ACTION) != MELEE_NONE) tickMelee(level, now);
        else if (canAct()) tryStartMelee(now);
        if (this.tickCount % 5 == 0) {
            updateCombatTarget(level, owner, now);
            enforceActivityBoundary(owner);
            tickFormation(level, owner, ownerAvailable);
        }
        tickVisualLife(level, owner, ownerAvailable, now);
        if (this.tickCount % 20 == 0) tickNaturalHealing(level, now);
        if (this.activityMode == ACTIVITY_WAIT && this.getTarget() == null && canAct()
                && !isSocialMovementOwned()) {
            BrainUtils.clearMemory(this, MemoryModuleType.WALK_TARGET);
            this.getNavigation().stop();
        }
    }

    private void tickVisualLife(ServerLevel level, LivingEntity owner, boolean ownerAvailable, long now) {
        tickBlinkClock(now);
        updateLocomotionVisualClock(now);
        if (now >= this.nextVisualThreatScanAt) {
            this.scannedVisualThreat = scanVisualThreats(level);
            this.scannedVisualThreatUntil = now + 3L;
            this.nextVisualThreatScanAt = now + 2L;
        }

        VisualCandidate best = now < this.scannedVisualThreatUntil && isValidVisualCandidate(this.scannedVisualThreat)
                ? this.scannedVisualThreat : null;
        LivingEntity damageSource = resolveLiving(level, this.visualDamageSourceId);
        if (now < this.visualDamageUntil) {
            if (isVisibleAttentionTarget(damageSource)) this.visualDamagePoint = damageSource.getEyePosition();
            if (this.visualDamagePoint.equals(Vec3.ZERO)) {
                this.visualDamagePoint = this.getEyePosition().add(facing(this.yBodyRot).scale(4.0));
            }
            best = betterVisualCandidate(best, new VisualCandidate(
                    isVisibleAttentionTarget(damageSource) ? damageSource : null, this.visualDamagePoint,
                    1100, VISUAL_HURT, VISUAL_KIND_DAMAGE, false));
        }
        LivingEntity combatTarget = this.getTarget();
        if (isVisibleAttentionTarget(combatTarget)) {
            best = betterVisualCandidate(best, new VisualCandidate(combatTarget, combatTarget.getEyePosition(),
                    800, VISUAL_ALERT, VISUAL_KIND_COMBAT, false));
        }

        boolean combatSuppressed = isMutualGazeCombatSuppressed(owner);
        Player acquiredPlayer = this.mutualGazePlayerId == null && this.caughtExitMode == CaughtExitMode.NONE
                && now >= this.caughtExitOwnerAvoidUntil
                ? tickPlayerGazeAcquisition(level, owner, combatSuppressed, now) : null;
        if (now < this.caughtExitOwnerAvoidUntil && (combatSuppressed
                || best != null && (isHardSocialInterrupt(best) || isCaughtReactionInterrupt(best)))) {
            this.caughtExitOwnerAvoidUntil = -1L;
        }

        if (this.mutualGazePlayerId != null) {
            if (combatSuppressed || best != null && (isHardSocialInterrupt(best)
                    || isCaughtReactionPendingOrActive() && isCaughtReactionInterrupt(best))) {
                endMutualGaze(now);
            } else if (tickMutualGaze(level, best, now)) {
                return;
            }
        }

        if (this.caughtExitMode != CaughtExitMode.NONE) {
            if (combatSuppressed || best != null && (isHardSocialInterrupt(best)
                    || isCaughtReactionInterrupt(best))) {
                endCaughtExit(now, true);
            } else if (tickCaughtExit(level, owner, now)) {
                return;
            }
        }

        if (acquiredPlayer != null && !combatSuppressed
                && (best == null || !isHardSocialInterrupt(best))) {
            beginMutualGaze(acquiredPlayer, owner, now);
            return;
        }

        if (now < this.visualLocomotionUntil) {
            best = betterVisualCandidate(best, new VisualCandidate(null,
                    this.getEyePosition().add(this.visualLocomotionDirection.scale(6.0)),
                    300, VISUAL_LOCOMOTION, VISUAL_KIND_LOCOMOTION, false));
        }
        best = betterVisualCandidate(best, quietVisualCandidate(level, owner, ownerAvailable, now));
        if (best == null) {
            Vec3 point = this.getEyePosition().add(facing(this.yBodyRot).scale(6.0));
            best = new VisualCandidate(null, point, 100, VISUAL_NORMAL, VISUAL_KIND_NORMAL, false);
        }
        applyVisualCandidate(best, now);
    }

    private Player tickPlayerGazeAcquisition(ServerLevel level, LivingEntity owner,
                                              boolean combatSuppressed, long now) {
        if (combatSuppressed || now < this.mutualGazeCooldownUntil) {
            this.playerGazeProgress.clear();
            return null;
        }
        Set<UUID> presentPlayers = new HashSet<>();
        Player qualifiedOwner = null;
        Player longestGazePlayer = null;
        int longestGazeTicks = -1;
        for (Player player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            UUID playerId = player.getUUID();
            presentPlayers.add(playerId);
            PlayerGazeProgress progress = this.playerGazeProgress.computeIfAbsent(
                    playerId, ignored -> new PlayerGazeProgress());
            GazeSample sample = samplePlayerHeadGaze(player);
            if (sample.state() == GazeState.VALID) {
                progress.validTicks++;
                progress.missedTicks = 0;
            } else if (sample.state() == GazeState.MISSED
                    && progress.missedTicks < GAZE_MISS_TOLERANCE_TICKS) {
                progress.missedTicks++;
            } else {
                progress.reset();
            }
            if (progress.validTicks < RomanVisualMath1211.requiredGazeTicks(sample.distance())) continue;
            if (player == owner) {
                qualifiedOwner = player;
            } else if (progress.validTicks > longestGazeTicks) {
                longestGazeTicks = progress.validTicks;
                longestGazePlayer = player;
            }
        }
        this.playerGazeProgress.keySet().removeIf(id -> !presentPlayers.contains(id));
        return qualifiedOwner != null ? qualifiedOwner : longestGazePlayer;
    }

    private GazeSample samplePlayerHeadGaze(Player player) {
        Vec3 playerEye = player.getEyePosition();
        Vec3 headCenter = getVisualHeadCenter();
        double distance = playerEye.distanceTo(headCenter);
        if (distance < 0.1 || player.isInvisible() && distance > INVISIBLE_GAZE_RANGE
                || !hasClearViewFromPlayer(player, headCenter)) {
            return new GazeSample(GazeState.BLOCKED, distance);
        }
        return new GazeSample(RomanVisualMath1211.gazeHitsHead(
                playerEye, player.getLookAngle(), headCenter, HEAD_GAZE_RADIUS)
                ? GazeState.VALID : GazeState.MISSED, distance);
    }

    private Vec3 getVisualHeadCenter() {
        return this.position().add(0.0, VISUAL_HEAD_CENTER_HEIGHT, 0.0);
    }

    private boolean hasClearViewFromPlayer(Player player, Vec3 headCenter) {
        return this.level().clip(new ClipContext(player.getEyePosition(), headCenter,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    private boolean isMutualGazeCombatSuppressed(LivingEntity owner) {
        LivingEntity target = this.getTarget();
        return target != null && target.isAlive() || !canAct()
                || isRecentWithin(this, this.getLastHurtByMobTimestamp(), COMBAT_GAZE_SUPPRESSION_TICKS)
                || isRecentWithin(this, this.getLastHurtMobTimestamp(), COMBAT_GAZE_SUPPRESSION_TICKS)
                || isRecentWithin(owner, owner.getLastHurtByMobTimestamp(), COMBAT_GAZE_SUPPRESSION_TICKS)
                || isRecentWithin(owner, owner.getLastHurtMobTimestamp(), COMBAT_GAZE_SUPPRESSION_TICKS);
    }

    private static boolean isRecentWithin(LivingEntity entity, int timestamp, int ticks) {
        return timestamp > 0 && entity.tickCount - timestamp <= ticks;
    }

    private void beginMutualGaze(Player player, LivingEntity owner, long now) {
        boolean caughtEligible = player == owner && now >= this.caughtReactionCooldownUntil
                && Objects.equals(this.activeVisualTargetId, player.getUUID())
                && this.activeVisualKind == VISUAL_KIND_NORMAL
                && now - this.entityData.get(ATTENTION_STARTED_AT) >= CAUGHT_PREWATCH_TICKS;
        this.mutualGazePlayerId = player.getUUID();
        this.mutualGazeLastSeenPoint = player.getEyePosition();
        this.mutualGazeHoldTicksRemaining = 40 + this.random.nextInt(41);
        this.mutualGazeLostSightAt = -1L;
        this.mutualGazeBodyTurning = false;
        this.mutualGazeAligned = false;
        this.mutualGazeDistractionStartedAt = -1L;
        this.caughtReactionScheduledAt = caughtEligible ? now + chooseCaughtReactionDelay() : -1L;
        this.caughtReactionStartedAt = -1L;
        this.caughtReactionGlanceEndAt = -1L;
        this.caughtReactionFinishAt = -1L;
        this.caughtReactionGazeMissTicks = 0;
        this.playerGazeProgress.clear();
        clearSocialWalkTarget();
        applyVisualCandidate(new VisualCandidate(player, this.mutualGazeLastSeenPoint,
                MUTUAL_GAZE_PRIORITY, VISUAL_MUTUAL_GAZE, VISUAL_KIND_MUTUAL_GAZE, false), now);
    }

    private boolean tickMutualGaze(ServerLevel level, VisualCandidate candidate, long now) {
        Player player = level.getPlayerByUUID(this.mutualGazePlayerId);
        if (player == null || !player.isAlive() || player.isSpectator()) {
            endMutualGaze(now);
            return false;
        }
        Vec3 headCenter = getVisualHeadCenter();
        double distanceSqr = player.getEyePosition().distanceToSqr(headCenter);
        boolean visible = (!player.isInvisible() || distanceSqr <= INVISIBLE_GAZE_RANGE * INVISIBLE_GAZE_RANGE)
                && hasClearViewFromPlayer(player, headCenter);
        if (isCaughtReactionActive() && (!visible || distanceSqr > CAUGHT_MAX_OWNER_DISTANCE_SQR)) {
            endMutualGaze(now);
            return false;
        }
        if (visible) {
            this.mutualGazeLostSightAt = -1L;
            this.mutualGazeLastSeenPoint = player.getEyePosition();
        } else {
            if (this.mutualGazeLostSightAt < 0L) this.mutualGazeLostSightAt = now;
            if (now - this.mutualGazeLostSightAt > 10L) {
                endMutualGaze(now);
                return false;
            }
        }
        clearSocialWalkTarget();
        if (candidate != null && candidate.kind() == VISUAL_KIND_CLOSE_CREEPER) {
            if (this.mutualGazeDistractionStartedAt < 0L) this.mutualGazeDistractionStartedAt = now;
            applyVisualCandidate(candidate, now);
            if (now - this.mutualGazeDistractionStartedAt > 20L) {
                endMutualGaze(now);
                return false;
            }
            return true;
        }
        this.mutualGazeDistractionStartedAt = -1L;
        applyVisualCandidate(new VisualCandidate(player, this.mutualGazeLastSeenPoint,
                MUTUAL_GAZE_PRIORITY, VISUAL_MUTUAL_GAZE, VISUAL_KIND_MUTUAL_GAZE, false), now);
        if (tickCaughtReaction(player, now)) return true;
        tickMutualGazeBodyFacing(now);
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

    private void tickMutualGazeBodyFacing(long now) {
        if (now - this.entityData.get(ATTENTION_STARTED_AT) < 4L || isCaughtReactionActive()) return;
        float desiredYaw = yawToward(this.getX(), this.getZ(),
                this.mutualGazeLastSeenPoint.x, this.mutualGazeLastSeenPoint.z);
        float difference = Math.abs(Mth.wrapDegrees(desiredYaw - this.yBodyRot));
        if (!this.mutualGazeBodyTurning && difference > 5.0F) this.mutualGazeBodyTurning = true;
        if (!this.mutualGazeBodyTurning || difference <= 5.0F) {
            this.mutualGazeBodyTurning = false;
            this.mutualGazeAligned = true;
            return;
        }
        this.mutualGazeAligned = false;
        turnBodyToward(this.mutualGazeLastSeenPoint, 8.0F);
    }

    private boolean tickCaughtReaction(Player owner, long now) {
        if (this.caughtReactionScheduledAt >= 0L && !isCaughtReactionActive()) {
            GazeState state = samplePlayerHeadGaze(owner).state();
            if (state == GazeState.VALID) {
                this.caughtReactionGazeMissTicks = 0;
            } else if (state == GazeState.MISSED
                    && this.caughtReactionGazeMissTicks < GAZE_MISS_TOLERANCE_TICKS) {
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
        clearSocialWalkTarget();
        if (elapsed < 10L) {
            applySocialVisual(owner.getEyePosition(), owner.getEyePosition(), owner,
                    VISUAL_CAUGHT, VISUAL_KIND_CAUGHT, now);
            return true;
        }
        if (elapsed < 12L) {
            applySocialVisual(owner.getEyePosition(), this.caughtReactionAwayPoint, owner,
                    VISUAL_CAUGHT, VISUAL_KIND_CAUGHT, now);
            return true;
        }
        if (elapsed < CAUGHT_GLANCE_START_TICKS) {
            applySocialVisual(this.caughtReactionAwayPoint, this.caughtReactionAwayPoint, null,
                    VISUAL_CAUGHT, VISUAL_KIND_CAUGHT, now);
            return true;
        }
        if (this.caughtReactionGlanceEndAt < 0L) {
            boolean ownerStillLooking = samplePlayerHeadGaze(owner).state() == GazeState.VALID;
            this.caughtReactionGlanceEndAt = now + (ownerStillLooking
                    ? 4L + this.random.nextInt(4) : 8L + this.random.nextInt(4));
            this.caughtReactionFinishAt = this.caughtReactionGlanceEndAt + 5L + this.random.nextInt(3);
        }
        if (now < this.caughtReactionGlanceEndAt) {
            applySocialVisual(this.caughtReactionAwayPoint, owner.getEyePosition(), owner,
                    VISUAL_CAUGHT, VISUAL_KIND_CAUGHT, now);
            return true;
        }
        applySocialVisual(this.caughtReactionAwayPoint, this.caughtReactionAwayPoint, null,
                VISUAL_CAUGHT, VISUAL_KIND_CAUGHT, now);
        if (now < this.caughtReactionFinishAt) return true;
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
        this.entityData.set(BLINK_START, now + 3L);
        this.entityData.set(BLINK_COUNT, (byte)2);
        this.nextBlinkAt = now + 120L;
        this.entityData.set(CURIOUS_TILT, (byte)0);
        this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
    }

    private Vec3 createCaughtReactionAwayPoint(Player owner) {
        float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
        float offset = 35.0F + this.random.nextFloat() * 20.0F;
        float targetYaw = ownerYaw + (this.random.nextBoolean() ? offset : -offset);
        double radians = targetYaw * Mth.DEG_TO_RAD;
        double distance = 6.0 + this.random.nextDouble() * 3.0;
        return new Vec3(this.getX() - Math.sin(radians) * distance,
                this.getEyeY() + 0.5 + this.random.nextDouble(),
                this.getZ() + Math.cos(radians) * distance);
    }

    private int chooseCaughtReactionDelay() {
        int roll = this.random.nextInt(100);
        if (roll < 30) return 0;
        if (roll < 75) return 20 + this.random.nextInt(21);
        if (roll < 95) return 40 + this.random.nextInt(41);
        return 80 + this.random.nextInt(41);
    }

    private boolean isCaughtReactionPendingOrActive() {
        return this.caughtReactionScheduledAt >= 0L || isCaughtReactionActive();
    }

    private boolean isCaughtReactionActive() {
        return this.caughtReactionStartedAt >= 0L;
    }

    private void finishCaughtReaction(long now) {
        Vec3 awayPoint = this.caughtReactionAwayPoint;
        LivingEntity owner = this.getOwner();
        endMutualGaze(now);
        if (this.level() instanceof ServerLevel level && owner != null) {
            startCaughtExit(level, owner, awayPoint, now);
        }
    }

    private void startCaughtExit(ServerLevel level, LivingEntity owner, Vec3 initialAwayPoint, long now) {
        int roll = this.random.nextInt(100);
        this.caughtExitMode = roll < 15 ? CaughtExitMode.LOOK_AWAY
                : roll < 40 ? CaughtExitMode.TURN_AWAY : CaughtExitMode.WALK_AWAY;
        this.caughtExitEndsAt = now + 40L + this.random.nextInt(41);
        this.caughtExitWalkStartsAt = -1L;
        this.caughtExitWalkStarted = false;
        this.caughtExitWalkArrived = false;
        this.caughtExitSecondaryPlanned = this.random.nextFloat() < 0.75F;
        this.caughtExitSecondaryDone = false;
        this.caughtExitSecondaryScheduledAt = -1L;
        this.caughtExitSecondaryStartedAt = -1L;
        this.caughtExitSecondaryGlanceEndAt = -1L;
        this.caughtExitSecondaryReturnEndAt = -1L;
        this.caughtExitFocusTarget = null;
        this.caughtExitFocusPoint = initialAwayPoint;
        this.caughtExitOwnerAvoidUntil = -1L;
        clearSocialWalkTarget();

        if (this.caughtExitMode == CaughtExitMode.WALK_AWAY
                && !chooseCaughtExitWalkTarget(level, owner)) {
            this.caughtExitMode = CaughtExitMode.TURN_AWAY;
        }
        if (this.caughtExitMode == CaughtExitMode.WALK_AWAY) {
            this.caughtExitFocusPoint = this.caughtExitWalkTarget.add(0.0, this.getEyeHeight(), 0.0);
            this.caughtExitWalkStartsAt = now + 5L + this.random.nextInt(6);
        } else {
            selectCaughtExitFocus(level, owner, initialAwayPoint);
            this.caughtExitSecondaryScheduledAt = this.caughtExitSecondaryPlanned
                    ? now + 16L + this.random.nextInt(15) : -1L;
        }
        this.caughtExitBodyTargetYaw = chooseCaughtExitBodyYaw(owner);
        applyCaughtExitFocus(now);
    }

    private boolean chooseCaughtExitWalkTarget(ServerLevel level, LivingEntity owner) {
        if (this.distanceToSqr(owner) > CAUGHT_EXIT_WALK_ELIGIBLE_OWNER_DISTANCE_SQR) return false;
        float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
        for (int attempt = 0; attempt < 8; attempt++) {
            float offset = CAUGHT_EXIT_MIN_OWNER_ANGLE + this.random.nextFloat()
                    * (CAUGHT_EXIT_MAX_WALK_ANGLE - CAUGHT_EXIT_MIN_OWNER_ANGLE);
            float targetYaw = ownerYaw + (this.random.nextBoolean() ? offset : -offset);
            double radians = targetYaw * Mth.DEG_TO_RAD;
            double distance = 1.5 + this.random.nextDouble() * 1.5;
            Vec3 desired = new Vec3(this.getX() - Math.sin(radians) * distance,
                    this.getY(), this.getZ() + Math.cos(radians) * distance);
            Vec3 safe = safeAttackTrackingDestination(level, desired);
            if (safe == null || owner.distanceToSqr(safe) > CAUGHT_EXIT_MAX_OWNER_DISTANCE_SQR) continue;
            this.caughtExitWalkTarget = safe;
            return true;
        }
        return false;
    }

    private void selectCaughtExitFocus(ServerLevel level, LivingEntity owner, Vec3 fallbackPoint) {
        LivingEntity nearestCreature = null;
        Player nearestOtherPlayer = null;
        double creatureDistanceSqr = Double.MAX_VALUE;
        double playerDistanceSqr = Double.MAX_VALUE;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(12.0), entity -> entity != this && entity != owner
                        && entity.isAlive() && !entity.isSpectator() && !(entity instanceof Monster)
                        && this.hasLineOfSight(entity))) {
            if (!isCaughtExitDirectionAwayFromOwner(owner, entity.getEyePosition())) continue;
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
        this.caughtExitFocusPoint = this.caughtExitFocusTarget != null
                ? this.caughtExitFocusTarget.getEyePosition()
                : isCaughtExitDirectionAwayFromOwner(owner, fallbackPoint)
                ? fallbackPoint : createCaughtExitFallbackPoint(owner);
    }

    private boolean isCaughtExitDirectionAwayFromOwner(LivingEntity owner, Vec3 point) {
        float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
        float pointYaw = yawToward(this.getX(), this.getZ(), point.x, point.z);
        return Math.abs(Mth.wrapDegrees(pointYaw - ownerYaw)) >= CAUGHT_EXIT_MIN_OWNER_ANGLE;
    }

    private float chooseCaughtExitBodyYaw(LivingEntity owner) {
        float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
        float focusYaw = yawToward(this.getX(), this.getZ(),
                this.caughtExitFocusPoint.x, this.caughtExitFocusPoint.z);
        float difference = Mth.wrapDegrees(focusYaw - ownerYaw);
        float direction = Math.abs(difference) < 1.0F
                ? (this.random.nextBoolean() ? 1.0F : -1.0F) : Math.signum(difference);
        float offset = Mth.clamp(Math.abs(difference), 45.0F,
                this.caughtExitMode == CaughtExitMode.WALK_AWAY ? CAUGHT_EXIT_MAX_WALK_ANGLE : 90.0F);
        return ownerYaw + direction * offset;
    }

    private boolean tickCaughtExit(ServerLevel level, LivingEntity owner, long now) {
        if (this.caughtExitMode == CaughtExitMode.NONE) return false;
        if (this.distanceToSqr(owner) >= CAUGHT_EXIT_FOLLOW_CANCEL_DISTANCE_SQR) {
            endCaughtExit(now, true);
            return false;
        }
        tickCaughtExitBodyFacing();
        if (this.caughtExitMode == CaughtExitMode.WALK_AWAY && !this.caughtExitWalkArrived) {
            if (now >= this.caughtExitEndsAt) {
                endCaughtExit(now, false);
                return true;
            }
            Vec3 focus = this.caughtExitWalkTarget.add(0.0, this.getEyeHeight(), 0.0);
            applySocialVisual(focus, focus, null, VISUAL_NORMAL, VISUAL_KIND_NORMAL, now);
            if (!this.caughtExitWalkStarted && now >= this.caughtExitWalkStartsAt) {
                double speed = 0.65 + this.random.nextDouble() * 0.10;
                if (!this.getNavigation().moveTo(this.caughtExitWalkTarget.x,
                        this.caughtExitWalkTarget.y, this.caughtExitWalkTarget.z, speed)) {
                    fallbackCaughtExitWalkToTurn(level, owner, now);
                    return true;
                }
                this.caughtExitWalkStarted = true;
                this.caughtExitSecondaryScheduledAt = this.caughtExitSecondaryPlanned
                        ? now + 16L + this.random.nextInt(15) : -1L;
            }
            tickCaughtExitSecondaryGlance(owner, now);
            boolean reached = this.position().distanceToSqr(this.caughtExitWalkTarget) <= 0.75 * 0.75;
            if (this.caughtExitWalkStarted && reached) {
                this.caughtExitWalkArrived = true;
                this.getNavigation().stop();
                selectCaughtExitFocus(level, owner, createCaughtExitFallbackPoint(owner));
                this.caughtExitBodyTargetYaw = chooseCaughtExitBodyYaw(owner);
                this.caughtExitEndsAt = Math.max(this.caughtExitEndsAt,
                        now + 16L + this.random.nextInt(15));
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
            this.caughtExitSecondaryScheduledAt = now + 16L + this.random.nextInt(15);
        }
        applyCaughtExitFocus(now);
    }

    private void tickCaughtExitSecondaryGlance(LivingEntity owner, long now) {
        if (!this.caughtExitSecondaryPlanned || this.caughtExitSecondaryDone
                || this.caughtExitSecondaryScheduledAt < 0L || now < this.caughtExitSecondaryScheduledAt) return;
        if (this.caughtExitSecondaryStartedAt < 0L) {
            this.caughtExitSecondaryStartedAt = now;
            this.caughtExitSecondaryOwnerStillLooking = owner instanceof Player player
                    && samplePlayerHeadGaze(player).state() == GazeState.VALID;
            this.caughtExitSecondaryGlanceEndAt = now + (this.caughtExitSecondaryOwnerStillLooking
                    ? 3L + this.random.nextInt(3) : 7L + this.random.nextInt(5));
            this.caughtExitSecondaryReturnEndAt = this.caughtExitSecondaryGlanceEndAt + 4L;
            this.caughtExitSecondaryHeadPoint = createSecondaryGlanceHeadPoint(owner);
        }
        if (now < this.caughtExitSecondaryGlanceEndAt) {
            Vec3 headPoint = now - this.caughtExitSecondaryStartedAt >= 2L
                    ? this.caughtExitSecondaryHeadPoint : this.caughtExitFocusPoint;
            applySocialVisual(headPoint, owner.getEyePosition(), owner,
                    VISUAL_NORMAL, VISUAL_KIND_NORMAL, now);
            return;
        }
        applyCaughtExitFocus(now);
        if (now >= this.caughtExitSecondaryReturnEndAt) {
            this.caughtExitSecondaryDone = true;
            if (this.caughtExitSecondaryOwnerStillLooking) {
                this.caughtExitEndsAt = Math.max(this.caughtExitEndsAt,
                        now + 20L + this.random.nextInt(21));
            }
        }
    }

    private Vec3 createSecondaryGlanceHeadPoint(LivingEntity owner) {
        float focusYaw = yawToward(this.getX(), this.getZ(),
                this.caughtExitFocusPoint.x, this.caughtExitFocusPoint.z);
        float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
        float difference = Mth.wrapDegrees(ownerYaw - focusYaw);
        float turn = Math.signum(difference) * Math.min(Math.abs(difference),
                8.0F + this.random.nextFloat() * 7.0F);
        double radians = (focusYaw + turn) * Mth.DEG_TO_RAD;
        return new Vec3(this.getX() - Math.sin(radians) * 8.0,
                this.caughtExitFocusPoint.y, this.getZ() + Math.cos(radians) * 8.0);
    }

    private Vec3 createCaughtExitFallbackPoint(LivingEntity owner) {
        float ownerYaw = yawToward(this.getX(), this.getZ(), owner.getX(), owner.getZ());
        float offset = CAUGHT_EXIT_MIN_OWNER_ANGLE + this.random.nextFloat()
                * (CAUGHT_EXIT_MAX_WALK_ANGLE - CAUGHT_EXIT_MIN_OWNER_ANGLE);
        double radians = (ownerYaw + (this.random.nextBoolean() ? offset : -offset)) * Mth.DEG_TO_RAD;
        double distance = 6.0 + this.random.nextDouble() * 3.0;
        return new Vec3(this.getX() - Math.sin(radians) * distance,
                this.getEyeY() + this.random.nextDouble(),
                this.getZ() + Math.cos(radians) * distance);
    }

    private void applyCaughtExitFocus(long now) {
        applySocialVisual(this.caughtExitFocusPoint, this.caughtExitFocusPoint,
                this.caughtExitFocusTarget, VISUAL_NORMAL, VISUAL_KIND_NORMAL, now);
    }

    private void tickCaughtExitBodyFacing() {
        if (this.caughtExitMode != CaughtExitMode.TURN_AWAY
                && !(this.caughtExitMode == CaughtExitMode.WALK_AWAY
                && (!this.caughtExitWalkStarted || this.caughtExitWalkArrived))) return;
        double radians = this.caughtExitBodyTargetYaw * Mth.DEG_TO_RAD;
        turnBodyToward(new Vec3(this.getX() - Math.sin(radians) * 6.0,
                this.getEyeY(), this.getZ() + Math.cos(radians) * 6.0), 5.0F);
    }

    private void endCaughtExit(long now, boolean interrupted) {
        if (this.caughtExitMode == CaughtExitMode.NONE) return;
        if (this.caughtExitMode == CaughtExitMode.WALK_AWAY) this.getNavigation().stop();
        Vec3 finalFocus = this.caughtExitFocusPoint;
        this.caughtExitMode = CaughtExitMode.NONE;
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
            this.caughtReactionCooldownUntil = now + 160L + this.random.nextInt(141);
            this.mutualGazeCooldownUntil = Math.max(this.mutualGazeCooldownUntil,
                    this.caughtReactionCooldownUntil);
            this.caughtExitOwnerAvoidUntil = now + 80L + this.random.nextInt(61);
            this.caughtExitOwnerAvoidPoint = finalFocus;
            this.quietVisualTargetId = null;
            this.quietVisualPoint = finalFocus;
            this.quietVisualUntil = this.caughtExitOwnerAvoidUntil;
            this.quietVisualCurious = false;
            applyVisualCandidate(new VisualCandidate(null, finalFocus, 235,
                    VISUAL_NORMAL, VISUAL_KIND_NORMAL, false), now);
        }
    }

    private void startMutualGazeGlanceAway(long now) {
        this.mutualGazeCooldownUntil = now + 10L + this.random.nextInt(21);
        float offset = 35.0F + this.random.nextFloat() * 65.0F;
        double radians = (this.yBodyRot + (this.random.nextBoolean() ? offset : -offset)) * Mth.DEG_TO_RAD;
        double distance = 4.0 + this.random.nextDouble() * 3.0;
        Vec3 point = new Vec3(this.getX() - Math.sin(radians) * distance,
                this.getEyeY() + this.random.nextDouble() * 1.5 - 0.5,
                this.getZ() + Math.cos(radians) * distance);
        this.quietVisualTargetId = null;
        this.quietVisualPoint = point;
        this.quietVisualUntil = this.mutualGazeCooldownUntil;
        this.quietVisualCurious = false;
        applyVisualCandidate(new VisualCandidate(null, point, 235,
                VISUAL_NORMAL, VISUAL_KIND_NORMAL, false), now);
    }

    private void endMutualGaze(long now) {
        boolean caughtWasActive = isCaughtReactionActive();
        this.caughtReactionScheduledAt = -1L;
        this.caughtReactionStartedAt = -1L;
        this.caughtReactionGlanceEndAt = -1L;
        this.caughtReactionFinishAt = -1L;
        this.caughtReactionGazeMissTicks = 0;
        this.entityData.set(CAUGHT_REACTION_START, -100L);
        if (caughtWasActive) {
            this.caughtReactionCooldownUntil = now + 160L + this.random.nextInt(141);
            this.mutualGazeCooldownUntil = Math.max(this.mutualGazeCooldownUntil,
                    this.caughtReactionCooldownUntil);
        }
        this.mutualGazePlayerId = null;
        this.mutualGazeHoldTicksRemaining = 0;
        this.mutualGazeLostSightAt = -1L;
        this.mutualGazeBodyTurning = false;
        this.mutualGazeAligned = false;
        this.mutualGazeDistractionStartedAt = -1L;
        this.playerGazeProgress.clear();
        this.activeVisualTargetId = null;
        this.activeVisualKind = -1;
        if (this.entityData.get(VISUAL_REACTION) == VISUAL_MUTUAL_GAZE
                || this.entityData.get(VISUAL_REACTION) == VISUAL_CAUGHT) {
            this.entityData.set(VISUAL_REACTION, VISUAL_NORMAL);
            this.entityData.set(CURIOUS_TILT, (byte)0);
        }
    }

    private static boolean isHardSocialInterrupt(VisualCandidate candidate) {
        return candidate.kind() == VISUAL_KIND_PRIMED_CREEPER
                || candidate.kind() == VISUAL_KIND_DAMAGE
                || candidate.kind() == VISUAL_KIND_COMBAT;
    }

    private static boolean isCaughtReactionInterrupt(VisualCandidate candidate) {
        return candidate.target() instanceof Creeper || candidate.kind() == VISUAL_KIND_APPROACHING;
    }

    private void clearSocialWalkTarget() {
        BrainUtils.clearMemory(this, MemoryModuleType.WALK_TARGET);
        this.getNavigation().stop();
    }

    private void turnBodyToward(Vec3 point, float maxDegrees) {
        float desiredYaw = yawToward(this.getX(), this.getZ(), point.x, point.z);
        float difference = Mth.wrapDegrees(desiredYaw - this.yBodyRot);
        this.yBodyRot += Mth.clamp(difference, -maxDegrees, maxDegrees);
        this.setYRot(this.yBodyRot);
    }

    private void tickBlinkClock(long now) {
        if (this.nextBlinkAt == 0L) this.nextBlinkAt = now + 50L + this.random.nextInt(71);
        byte reaction = this.entityData.get(VISUAL_REACTION);
        if (now < this.nextBlinkAt || reaction == VISUAL_STARTLED || reaction == VISUAL_HURT) return;
        this.entityData.set(BLINK_START, now);
        this.entityData.set(BLINK_COUNT, this.random.nextFloat() < 0.1F ? (byte)2 : (byte)1);
        this.nextBlinkAt = now + 50L + this.random.nextInt(71);
    }

    private void updateLocomotionVisualClock(long now) {
        Vec3 movement = this.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        boolean moving = !this.getNavigation().isDone() || movement.lengthSqr() > 2.5E-4;
        if (!moving) return;
        this.visualLocomotionDirection = movement.lengthSqr() > 1.0E-4
                ? movement.normalize() : facing(this.yBodyRot);
        this.visualLocomotionUntil = now + 6L;
    }

    private VisualCandidate scanVisualThreats(ServerLevel level) {
        VisualCandidate best = null;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(12.0), entity -> entity != this && entity.isAlive()
                        && !entity.isSpectator() && this.hasLineOfSight(entity))) {
            double distanceSqr = this.distanceToSqr(candidate);
            if (candidate instanceof Creeper creeper) {
                boolean primed = creeper.isIgnited() || creeper.getSwellDir() > 0;
                if (primed) {
                    best = betterVisualCandidate(best, new VisualCandidate(creeper, creeper.getEyePosition(),
                            1200, VISUAL_STARTLED, VISUAL_KIND_PRIMED_CREEPER, false));
                } else if (distanceSqr <= 64.0) {
                    best = betterVisualCandidate(best, new VisualCandidate(creeper, creeper.getEyePosition(),
                            900, VISUAL_STARTLED, VISUAL_KIND_CLOSE_CREEPER, false));
                }
                continue;
            }
            double closingSpeed = approachingSpeed(candidate);
            if (distanceSqr <= 100.0 && closingSpeed > 0.22) {
                best = betterVisualCandidate(best, new VisualCandidate(candidate, candidate.getEyePosition(),
                        Math.min(790, 620 + (int)(closingSpeed * 250.0)), VISUAL_STARTLED,
                        VISUAL_KIND_APPROACHING, false));
            }
        }
        return best;
    }

    private VisualCandidate quietVisualCandidate(ServerLevel level, LivingEntity owner,
                                                  boolean ownerAvailable, long now) {
        if (ownerAvailable && now < this.caughtExitOwnerAvoidUntil) {
            if (this.caughtExitOwnerAvoidPoint.equals(Vec3.ZERO)
                    || !isCaughtExitDirectionAwayFromOwner(owner, this.caughtExitOwnerAvoidPoint)) {
                this.caughtExitOwnerAvoidPoint = createCaughtExitFallbackPoint(owner);
            }
            return new VisualCandidate(null, this.caughtExitOwnerAvoidPoint, 235,
                    VISUAL_NORMAL, VISUAL_KIND_NORMAL, false);
        }
        LivingEntity target = resolveLiving(level, this.quietVisualTargetId);
        if (now >= this.quietVisualUntil || this.quietVisualTargetId != null && !isVisibleAttentionTarget(target)) {
            this.quietVisualTargetId = null;
            this.quietVisualPoint = Vec3.ZERO;
            this.quietVisualCurious = false;
            target = null;

            if (ownerAvailable && isVisibleAttentionTarget(owner)) target = owner;
            for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(12.0), entity -> entity != this && entity.isAlive()
                            && !entity.isSpectator() && !(entity instanceof Creeper)
                            && this.hasLineOfSight(entity))) {
                if (target == null || this.random.nextInt(3) == 0 && this.distanceToSqr(candidate) < this.distanceToSqr(target)) {
                    target = candidate;
                }
            }
            if (this.random.nextFloat() < 0.3F) {
                float yaw = (this.getYRot() + this.random.nextFloat() * 130.0F - 65.0F) * Mth.DEG_TO_RAD;
                double distance = 4.0 + this.random.nextDouble() * 5.0;
                this.quietVisualPoint = new Vec3(this.getX() - Math.sin(yaw) * distance,
                        this.getEyeY() + this.random.nextDouble() * 3.0 - 1.0,
                        this.getZ() + Math.cos(yaw) * distance);
                target = null;
            }
            this.quietVisualTargetId = target == null ? null : target.getUUID();
            if (target != null) this.quietVisualPoint = target.getEyePosition();
            this.quietVisualUntil = now + 30L + this.random.nextInt(51);
            this.quietVisualCurious = isInSafeVisualIdleState() && this.random.nextFloat() < 0.1F;
        } else if (target != null) {
            this.quietVisualPoint = target.getEyePosition();
        }
        if (this.quietVisualPoint.equals(Vec3.ZERO)) return null;
        return new VisualCandidate(target, this.quietVisualPoint, target == owner ? 220 : 170,
                this.quietVisualCurious ? VISUAL_CURIOUS : VISUAL_NORMAL,
                VISUAL_KIND_NORMAL, this.quietVisualCurious);
    }

    private boolean isInSafeVisualIdleState() {
        return this.getTarget() == null && this.visualDamageUntil <= this.level().getGameTime()
                && this.getNavigation().isDone();
    }

    private boolean isVisibleAttentionTarget(LivingEntity target) {
        return target != null && target.isAlive() && target.level() == this.level()
                && this.distanceToSqr(target) <= 32.0 * 32.0 && this.hasLineOfSight(target);
    }

    private boolean isValidVisualCandidate(VisualCandidate candidate) {
        return candidate != null && (candidate.target() == null
                || candidate.target().isAlive() && candidate.target().level() == this.level()
                && this.hasLineOfSight(candidate.target()));
    }

    private double approachingSpeed(LivingEntity target) {
        Vec3 towardEcho = this.position().subtract(target.position());
        return towardEcho.lengthSqr() < 1.0E-4 ? 0.0
                : target.getDeltaMovement().dot(towardEcho.normalize());
    }

    private static VisualCandidate betterVisualCandidate(VisualCandidate current, VisualCandidate candidate) {
        return candidate != null && (current == null || candidate.priority() > current.priority()) ? candidate : current;
    }

    private void applyVisualCandidate(VisualCandidate candidate, long now) {
        applySocialVisual(candidate.point(), candidate.point(), candidate.target(),
                candidate.reaction(), candidate.kind(), candidate.curious(), now);
    }

    private void applySocialVisual(Vec3 headPoint, Vec3 eyePoint, LivingEntity target,
                                   byte reaction, int kind, long now) {
        applySocialVisual(headPoint, eyePoint, target, reaction, kind, false, now);
    }

    private void applySocialVisual(Vec3 headPoint, Vec3 eyePoint, LivingEntity target,
                                   byte reaction, int kind, boolean curious, long now) {
        UUID targetId = target == null ? null : target.getUUID();
        boolean changed = kind != this.activeVisualKind
                || !Objects.equals(targetId, this.activeVisualTargetId)
                || reaction != this.entityData.get(VISUAL_REACTION);
        setVisualPoint(ATTENTION_X, ATTENTION_Y, ATTENTION_Z, headPoint);
        setVisualPoint(EYE_ATTENTION_X, EYE_ATTENTION_Y, EYE_ATTENTION_Z, eyePoint);
        this.entityData.set(VISUAL_REACTION, reaction);
        if (!changed) return;
        this.activeVisualKind = kind;
        this.activeVisualTargetId = targetId;
        this.entityData.set(ATTENTION_STARTED_AT, now);
        this.entityData.set(CURIOUS_TILT,
                curious ? (byte)(this.random.nextBoolean() ? 1 : -1) : (byte)0);
        this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
    }

    private Vec3 syncedVisualPoint(EntityDataAccessor<Float> x, EntityDataAccessor<Float> y,
                                   EntityDataAccessor<Float> z) {
        Vec3 point = new Vec3(this.entityData.get(x), this.entityData.get(y), this.entityData.get(z));
        return point.equals(Vec3.ZERO) ? this.getEyePosition().add(facing(this.yBodyRot).scale(6.0)) : point;
    }

    private void setVisualPoint(EntityDataAccessor<Float> x, EntityDataAccessor<Float> y,
                                EntityDataAccessor<Float> z, Vec3 point) {
        this.entityData.set(x, (float)point.x);
        this.entityData.set(y, (float)point.y);
        this.entityData.set(z, (float)point.z);
    }

    private record VisualCandidate(LivingEntity target, Vec3 point, int priority, byte reaction,
                                   int kind, boolean curious) {
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

    private enum CaughtExitMode {
        NONE,
        LOOK_AWAY,
        TURN_AWAY,
        WALK_AWAY
    }

    private void updateCombatTarget(ServerLevel level, LivingEntity owner, long now) {
        if (!canAct()) return;
        LivingEntity selected = selectProtectiveTarget(level, owner);
        LivingEntity current = this.getTarget();
        if (selected != null && this.hasLineOfSight(selected)) {
            this.targetLastSeenAt = now;
        } else if (selected == null && current != null && current.isAlive() && canAttack(current)
                && now - this.targetLastSeenAt <= 40L) {
            selected = current;
        }
        if (selected == null) {
            this.setTarget(null);
            BrainUtils.clearMemory(this, MemoryModuleType.ATTACK_TARGET);
        } else {
            this.setTarget(selected);
            BrainUtils.setTargetOfEntity(this, selected);
        }
    }

    private LivingEntity selectProtectiveTarget(ServerLevel level, LivingEntity owner) {
        LivingEntity selfAttacker = this.getLastHurtByMob();
        if (isRecent(this, this.getLastHurtByMobTimestamp()) && canProtectAgainst(selfAttacker)) return selfAttacker;
        if (this.alertMode == ALERT_PEACEFUL) return null;
        LivingEntity ownerAttacker = owner.getLastHurtByMob();
        if (isRecent(owner, owner.getLastHurtByMobTimestamp()) && canProtectAgainst(ownerAttacker)) return ownerAttacker;
        LivingEntity ownerTarget = owner.getLastHurtMob();
        if (isRecent(owner, owner.getLastHurtMobTimestamp()) && canProtectAgainst(ownerTarget)) return ownerTarget;
        LivingEntity current = this.getTarget();
        if (canProtectAgainst(current)) return current;
        if (this.alertMode != ALERT_AGGRESSIVE) return null;
        double range = EchoAccessorySystem1211.proactiveRange(this, 16.0, this.activityMode == ACTIVITY_WAIT);
        AABB scanBox = this.activityMode == ACTIVITY_WAIT
                ? new AABB(this.activityAnchor.x - range, this.activityAnchor.y - 4.0,
                        this.activityAnchor.z - range, this.activityAnchor.x + range,
                        this.activityAnchor.y + 4.0, this.activityAnchor.z + range)
                : this.getBoundingBox().inflate(range);
        return level.getEntitiesOfClass(Monster.class, scanBox,
                        monster -> !(monster instanceof Creeper) && canProtectAgainst(monster) && this.hasLineOfSight(monster))
                .stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    private boolean canProtectAgainst(LivingEntity target) {
        if (target == null || !target.isAlive() || !canAttack(target)) return false;
        LivingEntity owner = this.getOwner();
        Vec3 center = this.activityMode == ACTIVITY_FOLLOW && owner != null ? owner.position() : this.activityAnchor;
        double radius = this.activityMode == ACTIVITY_WAIT ? 8.0 : this.activityMode == ACTIVITY_WANDER ? 16.0 : 32.0;
        return target.position().distanceToSqr(center) <= radius * radius;
    }

    private boolean canRetainCombatTarget(LivingEntity target) {
        return canAct() && canProtectAgainst(target)
                && (this.hasLineOfSight(target) || this.level().getGameTime() - this.targetLastSeenAt <= 40L);
    }

    private static boolean isRecent(LivingEntity entity, int timestamp) {
        return timestamp > 0 && entity.tickCount - timestamp <= 100;
    }

    private void enforceActivityBoundary(LivingEntity owner) {
        LivingEntity target = this.getTarget();
        if (target != null && !canProtectAgainst(target)) clearCombatAndMovement();
        if (this.activityMode != ACTIVITY_FOLLOW && this.position().distanceToSqr(this.activityAnchor) > 24.0 * 24.0) {
            clearCombatAndMovement();
        }
        if (this.activityMode == ACTIVITY_FOLLOW && owner != this && this.distanceToSqr(owner) > 24.0 * 24.0) {
            this.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        }
    }

    private void clearCombatAndMovement() {
        long now = this.level().getGameTime();
        if (this.mutualGazePlayerId != null) endMutualGaze(now);
        if (this.caughtExitMode != CaughtExitMode.NONE) endCaughtExit(now, true);
        this.setTarget(null);
        BrainUtils.clearMemory(this, MemoryModuleType.ATTACK_TARGET);
        BrainUtils.clearMemory(this, MemoryModuleType.WALK_TARGET);
        this.getNavigation().stop();
    }

    private void tryStartMelee(long now) {
        LivingEntity target = this.getTarget();
        if (target == null || now < this.nextMeleeAttackAt || !this.hasLineOfSight(target)
                || !withinStrikeRange(target, FIRST_STRIKE_RANGE)) return;
        this.meleeTargetId = target.getUUID();
        this.committedMeleeInterval = EchoTalentSystem1211.attackIntervalTicks(this, activeRelic());
        this.entityData.set(MELEE_ACTION_SPEED, BASE_ATTACK_INTERVAL_TICKS / (float)this.committedMeleeInterval);
        this.entityData.set(MELEE_ACTION, MELEE_FIRST);
        this.meleeEndsAt = now + scaledAttackTicks(BASE_FIRST_TICKS);
        this.meleeHitAt = now + scaledAttackTicks(BASE_FIRST_HIT_TICKS);
        this.meleeHitResolved = false;
        stopMovementForAction();
        face(target);
        beginAttackTracking();
        stopActionTriggers();
        this.triggerAnim(ACTION_CONTROLLER, ATTACK_FIRST_TRIGGER);
    }

    private void tickMelee(ServerLevel level, long now) {
        this.attackTrackingMovedThisTick = false;
        stopMovementForAction();
        byte action = this.entityData.get(MELEE_ACTION);
        if (action == MELEE_FIRST) tickMeleeFirst(level, now);
        else if (action == MELEE_FOLLOW) tickMeleeFollow(level, now);
        else if (action == MELEE_RECOVER) {
            if (now >= this.meleeEndsAt) finishMelee(now);
        } else finishMelee(now);
        this.entityData.set(MELEE_ADVANCING,
                this.entityData.get(MELEE_ACTION) != MELEE_NONE && this.attackTrackingMovedThisTick);
    }

    private void tickMeleeFirst(ServerLevel level, long now) {
        tickAttackTracking(level, this.meleeTargetId, FIRST_TRACKING_SPEED, FIRST_TRACKING_LIMIT);
        if (!this.meleeHitResolved && now >= this.meleeHitAt) {
            this.meleeHitResolved = true;
            LivingEntity target = resolveLiving(level, this.meleeTargetId);
            if (canPerformMeleeHit(target, FIRST_STRIKE_RANGE)) {
                dealMeleeDamage(level, target,
                        ModDamageTypes1211.source(level, ModDamageTypes1211.ROMAN_FIRST_STRIKE, this), 1.0F);
            }
        }
        if (now < this.meleeEndsAt) return;
        LivingEntity target = resolveLiving(level, this.meleeTargetId);
        if (!canPerformMeleeHit(target, FOLLOW_COMMIT_RANGE)) {
            startMeleeRecovery(now);
            return;
        }
        face(target);
        beginAttackTracking();
        this.entityData.set(MELEE_ACTION, MELEE_FOLLOW);
        this.meleeEndsAt = now + scaledAttackTicks(BASE_FOLLOW_TICKS);
        this.meleeHitAt = now + scaledAttackTicks(BASE_FOLLOW_HIT_TICKS);
        this.meleeHitResolved = false;
    }

    private void tickMeleeFollow(ServerLevel level, long now) {
        if (!this.meleeHitResolved && now <= this.meleeHitAt) {
            tickAttackTracking(level, this.meleeTargetId, FOLLOW_TRACKING_SPEED, FOLLOW_TRACKING_LIMIT);
        }
        if (!this.meleeHitResolved && now >= this.meleeHitAt) {
            this.meleeHitResolved = true;
            LivingEntity target = resolveLiving(level, this.meleeTargetId);
            if (canPerformMeleeHit(target, FOLLOW_STRIKE_RANGE)) {
                dealMeleeDamage(level, target,
                        ModDamageTypes1211.source(level, ModDamageTypes1211.ROMAN_FOLLOWUP, this),
                        FOLLOW_DAMAGE_MULTIPLIER);
            }
        }
        if (now >= this.meleeEndsAt) finishMelee(now);
    }

    private void startMeleeRecovery(long now) {
        this.entityData.set(MELEE_ACTION, MELEE_RECOVER);
        this.meleeEndsAt = now + scaledAttackTicks(BASE_RECOVER_TICKS);
        this.meleeHitAt = Long.MAX_VALUE;
        this.meleeHitResolved = true;
        this.attackTrackingStopped = true;
        this.entityData.set(MELEE_ADVANCING, false);
        stopActionTriggers();
        this.triggerAnim(ACTION_CONTROLLER, ATTACK_RECOVER_TRIGGER);
    }

    private void finishMelee(long now) {
        stopActionTriggers();
        this.entityData.set(MELEE_ACTION, MELEE_NONE);
        this.entityData.set(MELEE_ADVANCING, false);
        this.entityData.set(MELEE_ACTION_SPEED, 1.0F);
        this.meleeTargetId = null;
        this.meleeEndsAt = 0L;
        this.meleeHitAt = Long.MAX_VALUE;
        this.meleeHitResolved = false;
        this.attackTrackingTravelled = 0.0;
        this.attackTrackingStopped = true;
        this.attackTrackingMovedThisTick = false;
        this.nextMeleeAttackAt = Math.max(this.nextMeleeAttackAt, now + this.committedMeleeInterval);
    }

    private void stopActionTriggers() {
        this.stopTriggeredAnim(ACTION_CONTROLLER, ATTACK_FIRST_TRIGGER);
        this.stopTriggeredAnim(ACTION_CONTROLLER, ATTACK_RECOVER_TRIGGER);
        this.stopTriggeredAnim(ACTION_CONTROLLER, HURT_TRIGGER);
    }

    private int scaledAttackTicks(int baseTicks) {
        return Math.max(1, Math.round(baseTicks * this.committedMeleeInterval
                / (float)BASE_ATTACK_INTERVAL_TICKS));
    }

    private boolean canPerformMeleeHit(LivingEntity target, double range) {
        return target != null && target.isAlive() && canProtectAgainst(target)
                && this.hasLineOfSight(target) && withinStrikeRange(target, range);
    }

    private void dealMeleeDamage(ServerLevel level, LivingEntity target, DamageSource source, float multiplier) {
        float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier;
        if (!target.hurt(source, damage)) return;
        this.setLastHurtMob(target);
        EchoExperienceSystem1211.markParticipation(this, target);
    }

    private boolean dealTalentDamage(ServerLevel level, LivingEntity target, float baseAmount) {
        if (!target.hurt(level.damageSources().mobAttack(this), baseAmount)) return false;
        this.setLastHurtMob(target);
        EchoExperienceSystem1211.markParticipation(this, target);
        return true;
    }

    private boolean withinStrikeRange(LivingEntity target, double range) {
        AABB ownBox = this.getBoundingBox();
        AABB targetBox = target.getBoundingBox();
        if (targetBox.maxY < ownBox.minY - 1.0 || targetBox.minY > ownBox.maxY + 1.0) return false;
        double reach = range + target.getBbWidth() * 0.5;
        return horizontalDistance(this.position(), target.position()) <= reach;
    }

    private void stopMovementForAction() {
        BrainUtils.clearMemory(this, MemoryModuleType.WALK_TARGET);
        this.getNavigation().stop();
        this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
    }

    private void face(Entity target) {
        float yaw = (float)(Mth.atan2(target.getZ() - this.getZ(), target.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    private void beginAttackTracking() {
        this.attackTrackingTravelled = 0.0;
        this.attackTrackingStartYaw = this.yBodyRot;
        this.attackTrackingStopped = false;
    }

    private void tickAttackTracking(ServerLevel level, UUID targetId, double speed, double travelLimit) {
        if (this.attackTrackingStopped || targetId == null) return;
        LivingEntity target = resolveLiving(level, targetId);
        if (target == null || !target.isAlive() || !this.canAttack(target)) {
            this.attackTrackingStopped = true;
            return;
        }
        float desired = yawToward(this.getX(), this.getZ(), target.getX(), target.getZ());
        float fromStart = Mth.wrapDegrees(desired - this.attackTrackingStartYaw);
        if (Math.abs(fromStart) > ATTACK_TRACKING_TOTAL_TURN) {
            this.attackTrackingStopped = true;
            return;
        }
        float delta = Mth.wrapDegrees(desired - this.yBodyRot);
        float yaw = this.yBodyRot + Mth.clamp(delta,
                -ATTACK_TRACKING_TURN_PER_TICK, ATTACK_TRACKING_TURN_PER_TICK);
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);

        if (this.attackTrackingTravelled >= travelLimit) return;
        double requested = speed * BASE_ATTACK_INTERVAL_TICKS / Math.max(1.0, this.committedMeleeInterval);
        while (requested > 1.0E-5 && this.attackTrackingTravelled < travelLimit) {
            Vec3 towardTarget = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            double distance = towardTarget.length();
            if (distance <= ATTACK_TRACKING_STOP_RANGE || distance < 1.0E-5) return;
            double step = Math.min(ATTACK_TRACKING_MAX_SUBSTEP, Math.min(requested, Math.min(
                    travelLimit - this.attackTrackingTravelled,
                    distance - ATTACK_TRACKING_STOP_RANGE)));
            if (step <= 1.0E-5) return;
            Vec3 before = this.position();
            if (!moveAttackTrackingStep(level, towardTarget.scale(step / distance))) {
                this.attackTrackingStopped = true;
                return;
            }
            double travelled = horizontalDistance(before, this.position());
            if (travelled <= 1.0E-5) {
                this.attackTrackingStopped = true;
                return;
            }
            this.attackTrackingTravelled += travelled;
            this.attackTrackingMovedThisTick = true;
            requested -= travelled;
        }
    }

    private boolean moveAttackTrackingStep(ServerLevel level, Vec3 horizontalDelta) {
        Vec3 safe = safeAttackTrackingDestination(level, this.position().add(horizontalDelta));
        if (safe == null) return false;
        double verticalVelocity = this.getDeltaMovement().y;
        this.setPos(safe);
        this.setDeltaMovement(0.0, verticalVelocity, 0.0);
        this.hasImpulse = true;
        return true;
    }

    private Vec3 safeAttackTrackingDestination(ServerLevel level, Vec3 desired) {
        for (double dy : new double[]{0.0, 1.0, -1.0}) {
            Vec3 candidate = new Vec3(desired.x, desired.y + dy, desired.z);
            if (!isWithinAttackActivityBoundary(candidate)) continue;
            AABB moved = this.getBoundingBox().move(candidate.subtract(this.position()));
            if (!level.noCollision(this, moved) || !hasSafeAttackTrackingSupport(level, candidate)) continue;
            return candidate;
        }
        return null;
    }

    private static boolean hasSafeAttackTrackingSupport(ServerLevel level, Vec3 candidate) {
        BlockPos feet = BlockPos.containing(candidate);
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        BlockState floorState = level.getBlockState(floor);
        if (!floorState.isFaceSturdy(level, floor, Direction.UP)
                || !level.getFluidState(feet).isEmpty()
                || !level.getFluidState(head).isEmpty()) return false;
        return !isAttackTrackingHazard(level.getBlockState(feet))
                && !isAttackTrackingHazard(level.getBlockState(head))
                && !isAttackTrackingHazard(floorState);
    }

    private static boolean isAttackTrackingHazard(BlockState state) {
        return state.is(BlockTags.FIRE)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE);
    }

    private boolean isWithinAttackActivityBoundary(Vec3 point) {
        LivingEntity owner = this.getOwner();
        Vec3 center = this.activityMode == ACTIVITY_FOLLOW && owner != null ? owner.position() : this.activityAnchor;
        double radius = this.activityMode == ACTIVITY_WAIT ? 8.0
                : this.activityMode == ACTIVITY_WANDER ? 24.0 : 32.0;
        return point.distanceToSqr(center) <= radius * radius;
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double x = first.x - second.x;
        double z = first.z - second.z;
        return Math.sqrt(x * x + z * z);
    }

    private static float yawToward(double fromX, double fromZ, double targetX, double targetZ) {
        return (float)(Mth.atan2(targetZ - fromZ, targetX - fromX) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private void tickCombatSkills(ServerLevel level, LivingEntity owner, boolean ownerAvailable, long now) {
        if (isLegionEnduresActive()) {
            tickLegionEndures(level, now);
            return;
        }
        if (this.legionEnduresUntil != 0L && now >= this.legionEnduresUntil) finishLegionEndures(level);
        if (!ownerAvailable && this.shieldChargeTarget != null) stopShieldCharge();
        else if (this.shieldChargeTarget != null) tickShieldCharge(level, owner, now);
        else if (ownerAvailable && skillEnabled(SKILL_SHIELD_CHARGE) && now >= this.shieldInternalCooldownUntil) {
            Entity target = findShieldChargeTarget(level, owner, now);
            EchoBindingSavedData1211.Binding binding = currentBinding(level);
            if (target != null && binding != null && EchoBindingSystem1211.consumeShieldCharge(level, binding, now)) {
                this.shieldChargeTarget = target;
                this.shieldChargeStartedAt = now;
                this.shieldChargeUntil = now + 20L;
                raiseShield(now);
            }
        }
        EchoBindingSavedData1211.Binding binding = currentBinding(level);
        if (binding != null && skillEnabled(SKILL_LEGION_ENDURES)
                && canStartLegionEndures(level, owner, binding, now)) startLegionEndures(level, binding, now);
    }

    private Entity findShieldChargeTarget(ServerLevel level, LivingEntity owner, long now) {
        Projectile best = null;
        double bestTime = Double.MAX_VALUE;
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
                owner.getBoundingBox().inflate(12.0), Projectile::isAlive)) {
            if (isRecentlyCharged(projectile, now) || this.reflectedProjectiles.contains(projectile.getUUID())
                    || projectile.getOwner() == owner || projectile.getOwner() == this
                    || projectile.getOwner() instanceof LivingEntity living && !this.canAttack(living)) continue;
            Vec3 velocity = projectile.getDeltaMovement();
            double speedSqr = velocity.lengthSqr();
            if (speedSqr < 0.0025) continue;
            double time = Mth.clamp(owner.getEyePosition().subtract(projectile.position()).dot(velocity) / speedSqr, 0.0, 20.0);
            if (time <= 0.0 || projectile.position().add(velocity.scale(time)).distanceToSqr(owner.getEyePosition()) > 2.25) continue;
            if (time < bestTime) {
                bestTime = time;
                best = projectile;
            }
        }
        if (best != null) return best;
        return level.getEntitiesOfClass(Creeper.class, owner.getBoundingBox().inflate(8.0),
                        creeper -> creeper.isAlive() && !isRecentlyCharged(creeper, now)
                                && this.canAttack(creeper) && (creeper.isIgnited() || creeper.getSwellDir() > 0)
                                && !(owner.getLastHurtMob() == creeper
                                && owner.tickCount - owner.getLastHurtMobTimestamp() <= 100))
                .stream().min(Comparator.comparingDouble(owner::distanceToSqr)).orElse(null);
    }

    private void tickShieldCharge(ServerLevel level, LivingEntity owner, long now) {
        Entity target = this.shieldChargeTarget;
        if (target == null || !target.isAlive() || now > this.shieldChargeUntil) {
            stopShieldCharge();
            return;
        }
        long elapsed = Math.max(0L, now - this.shieldChargeStartedAt);
        double progress = Mth.clamp(elapsed / 20.0, 0.0, 1.0);
        double speed = 0.32 + Math.sin(progress * Math.PI) * 0.34;
        Vec3 direction = shieldChargeDestination(target, owner).subtract(this.position());
        if (direction.lengthSqr() > 0.01) {
            Vec3 velocity = direction.normalize().scale(speed);
            this.setDeltaMovement(velocity.x, Math.max(this.getDeltaMovement().y, velocity.y), velocity.z);
            face(target);
        }
        if (elapsed % 2L == 0L) level.sendParticles(ParticleTypes.GUST,
                this.getX(), this.getY() + 0.75, this.getZ(), 1, 0.08, 0.2, 0.08, 0.01);
        boolean impacted = false;
        if (target instanceof Projectile projectile && shieldInterceptsProjectile(projectile)) {
            impacted = redirectProjectile(level, owner, projectile);
        } else if (target instanceof Creeper creeper && this.distanceToSqr(creeper) <= 3.0 && elapsed >= 6L) {
            dealTalentDamage(level, creeper,
                    (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0F);
            ShieldChargeCreeperControl1211.disorient(level, creeper, 40L);
            Vec3 away = creeper.position().subtract(owner.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() < 1.0E-5) away = creeper.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() < 1.0E-5) away = new Vec3(0.0, 0.0, 1.0);
            Vec3 charge = creeper.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            if (charge.lengthSqr() < 1.0E-5) charge = away;
            Vec3 combined = away.normalize().scale(0.7).add(charge.normalize().scale(0.3)).normalize();
            creeper.knockback(2.2, -combined.x, -combined.z);
            creeper.setDeltaMovement(creeper.getDeltaMovement().add(combined.scale(0.35)).add(0.0, 0.18, 0.0));
            level.sendParticles(ParticleTypes.GUST, creeper.getX(), creeper.getY() + 0.8, creeper.getZ(),
                    4, 0.3, 0.3, 0.3, 0.02);
            impacted = true;
        }
        if (!impacted) return;
        this.recentlyChargedTargetId = target.getUUID();
        this.recentlyChargedTargetUntil = now + 40L;
        level.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 0.9F);
        stopShieldCharge();
    }

    private Vec3 shieldChargeDestination(Entity target, LivingEntity owner) {
        if (!(target instanceof Projectile projectile)) return target.position();
        Vec3 velocity = projectile.getDeltaMovement();
        double speedSqr = velocity.lengthSqr();
        if (speedSqr < 1.0E-5) return projectile.position();
        double time = Mth.clamp(owner.getEyePosition().subtract(projectile.position()).dot(velocity) / speedSqr, 0.0, 20.0);
        return projectile.position().add(velocity.scale(time));
    }

    private boolean shieldInterceptsProjectile(Projectile projectile) {
        double shieldHeight = this.getBbHeight() * 0.55;
        Vec3 shieldOld = new Vec3(this.xOld, this.yOld + shieldHeight, this.zOld);
        Vec3 shieldNow = this.position().add(0.0, shieldHeight, 0.0);
        Vec3 projectileOld = new Vec3(projectile.xOld, projectile.yOld, projectile.zOld);
        Vec3 projectileNow = projectile.position();
        double previousSweep = movingSegmentsDistanceToSqr(projectileOld, projectileNow, shieldOld, shieldNow);
        Vec3 projectileNext = projectileNow.add(projectile.getDeltaMovement());
        Vec3 shieldNext = shieldNow.add(this.getDeltaMovement());
        double projectedSweep = movingSegmentsDistanceToSqr(projectileNow, projectileNext, shieldNow, shieldNext);
        double radiusSqr = SHIELD_PROJECTILE_INTERCEPT_RADIUS * SHIELD_PROJECTILE_INTERCEPT_RADIUS;
        return Math.min(previousSweep, projectedSweep) <= radiusSqr;
    }

    private boolean redirectProjectile(ServerLevel level, LivingEntity owner, Projectile projectile) {
        Vec3 incoming = projectile.getDeltaMovement();
        double speed = incoming.length();
        if (speed < 0.01) return false;
        LivingEntity returnTarget = shieldChargeProjectileReturnTarget(level, projectile);
        Vec3 redirected = returnTarget == null ? projectile.position().subtract(owner.getEyePosition())
                : returnTarget.getEyePosition().subtract(projectile.position());
        if (redirected.lengthSqr() < 1.0E-5) redirected = incoming.reverse();
        projectile.setDeltaMovement(redirected.normalize().scale(speed));
        projectile.setOwner(this);
        projectile.hasImpulse = true;
        if (projectile instanceof ShulkerBullet bullet) {
            ShulkerBulletAccessor1211 accessor = (ShulkerBulletAccessor1211)(Object)bullet;
            accessor.echoWarrior1211$setFinalTarget(returnTarget);
            if (returnTarget != null) accessor.echoWarrior1211$selectNextMoveDirection(null);
        }
        this.reflectedProjectiles.add(projectile.getUUID());
        level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(),
                7, 0.15, 0.15, 0.15, 0.05);
        return true;
    }

    private LivingEntity shieldChargeProjectileReturnTarget(ServerLevel level, Projectile projectile) {
        Entity shooter = projectile.getOwner();
        if (shooter instanceof LivingEntity living && living.isAlive() && living.level() == level && this.canAttack(living)) {
            return living;
        }
        return level.getEntitiesOfClass(Monster.class, projectile.getBoundingBox().inflate(16.0),
                        monster -> monster.isAlive() && this.canAttack(monster) && this.hasLineOfSight(monster))
                .stream().min(Comparator.comparingDouble(projectile::distanceToSqr)).orElse(null);
    }

    private static double movingSegmentsDistanceToSqr(Vec3 firstStart, Vec3 firstEnd,
                                                       Vec3 secondStart, Vec3 secondEnd) {
        Vec3 relativeStart = firstStart.subtract(secondStart);
        Vec3 relativeEnd = firstEnd.subtract(secondEnd);
        Vec3 segment = relativeEnd.subtract(relativeStart);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-8) return relativeStart.lengthSqr();
        double time = Mth.clamp(-relativeStart.dot(segment) / lengthSqr, 0.0, 1.0);
        return relativeStart.add(segment.scale(time)).lengthSqr();
    }

    private boolean isRecentlyCharged(Entity target, long now) {
        return this.recentlyChargedTargetId != null && now < this.recentlyChargedTargetUntil
                && this.recentlyChargedTargetId.equals(target.getUUID());
    }

    public boolean redirectProjectileForSelfTest(ServerLevel level, LivingEntity owner, Projectile projectile) {
        return redirectProjectile(level, owner, projectile);
    }

    public static boolean isAttackTrackingHazardForSelfTest(BlockState state) {
        return isAttackTrackingHazard(state);
    }

    public static double movingSegmentsDistanceToSqrForSelfTest(Vec3 firstStart, Vec3 firstEnd,
                                                                 Vec3 secondStart, Vec3 secondEnd) {
        return movingSegmentsDistanceToSqr(firstStart, firstEnd, secondStart, secondEnd);
    }

    private void raiseShield(long now) {
        if (this.entityData.get(MELEE_ACTION) != MELEE_NONE) finishMelee(now);
        stopMovementForAction();
        this.entityData.set(SHIELD_RAISED, true);
    }

    private void lowerShield() { this.entityData.set(SHIELD_RAISED, false); }

    private void stopShieldCharge() {
        this.shieldChargeTarget = null;
        this.shieldInternalCooldownUntil = Math.max(this.shieldInternalCooldownUntil, this.level().getGameTime() + 10L);
        Vec3 velocity = this.getDeltaMovement();
        this.setDeltaMovement(velocity.x * 0.15, velocity.y, velocity.z * 0.15);
        lowerShield();
    }

    private boolean canStartLegionEndures(ServerLevel level, LivingEntity owner,
                                           EchoBindingSavedData1211.Binding binding, long now) {
        if (now < binding.legionCooldownEnd()) return false;
        if (this.getHealth() >= this.getMaxHealth() * 0.6F
                && !isRecent(this, this.getLastHurtByMobTimestamp())
                && !isRecent(owner, owner.getLastHurtByMobTimestamp())) return false;
        return level.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(6.0), this::canAttack).size() >= 2;
    }

    private void startLegionEndures(ServerLevel level, EchoBindingSavedData1211.Binding binding, long now) {
        this.legionEnduresUntil = now + 100L;
        this.legionAccumulatedDamage = 0.0F;
        EchoBindingSystem1211.setLegionCooldownEnd(level, binding, now + 400L);
        addTransientModifier(Attributes.ARMOR, LEGION_ARMOR);
        addTransientModifier(Attributes.KNOCKBACK_RESISTANCE, LEGION_KNOCKBACK);
        clearCombatAndMovement();
        raiseShield(now);
        for (Monster monster : level.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(12.0),
                monster -> canAttack(monster) && hasLineOfSight(monster))) {
            monster.setTarget(this);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, monster.getX(),
                    monster.getY() + monster.getBbHeight() + 0.2, monster.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 0.65F);
    }

    private void tickLegionEndures(ServerLevel level, long now) {
        stopMovementForAction();
        this.setTarget(null);
        if ((now - (this.legionEnduresUntil - 100L)) % 3L == 0L) {
            double phase = (now % 24L) / 23.0;
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(),
                    this.getY() + this.getBbHeight() + 0.2 + Math.sin(phase * Math.PI), this.getZ(),
                    1, 0.25, 0.05, 0.25, 0.0);
        }
        if (this.tickCount % 10 == 0) {
            for (Monster monster : level.getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(12.0),
                    monster -> canAttack(monster) && hasLineOfSight(monster))) monster.setTarget(this);
        }
    }

    private void finishLegionEndures(ServerLevel level) {
        this.legionEnduresUntil = 0L;
        removeModifier(Attributes.ARMOR, LEGION_ARMOR_ID);
        removeModifier(Attributes.KNOCKBACK_RESISTANCE, LEGION_KNOCKBACK_ID);
        lowerShield();
        float damage = this.legionAccumulatedDamage;
        double scale = Math.max(1.0, Math.sqrt(Math.max(0.0, damage) / 20.0));
        for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(6.0), this::canAttack)) {
            if (damage > 0.0F) dealTalentDamage(level, enemy, damage);
            double distance = Math.max(0.5, this.distanceTo(enemy));
            enemy.knockback(1.5 * scale * Math.max(0.2, 1.0 - distance / 8.0),
                    this.getX() - enemy.getX(), this.getZ() - enemy.getZ());
        }
        level.sendParticles(ParticleTypes.GUST, this.getX(), this.getY() + 0.8, this.getZ(),
                20, 1.8, 0.5, 1.8, 0.12);
        level.playSound(null, this.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 0.9F, 0.9F);
        this.legionAccumulatedDamage = 0.0F;
    }

    private void cancelLegionEndures() {
        this.legionEnduresUntil = 0L;
        this.legionAccumulatedDamage = 0.0F;
        removeModifier(Attributes.ARMOR, LEGION_ARMOR_ID);
        removeModifier(Attributes.KNOCKBACK_RESISTANCE, LEGION_KNOCKBACK_ID);
        lowerShield();
    }

    private void tickFormation(ServerLevel level, LivingEntity owner, boolean ownerAvailable) {
        this.formationActive = skillEnabled(SKILL_FORMATION);
        updateFormationBeneficiary(level, owner);
        for (RomanLegionaryEchoEntity1211 echo : level.getEntitiesOfClass(RomanLegionaryEchoEntity1211.class,
                owner.getBoundingBox().inflate(9.0), candidate -> candidate.isAlive() && candidate.sameOwner(this))) {
            updateFormationBeneficiary(level, echo);
        }
    }

    private void updateFormationBeneficiary(ServerLevel level, LivingEntity beneficiary) {
        boolean active = level.getEntitiesOfClass(RomanLegionaryEchoEntity1211.class,
                        beneficiary.getBoundingBox().inflate(8.0), candidate -> candidate.isAlive()
                                && candidate.sameOwner(this) && candidate.formationActive
                                && candidate.distanceToSqr(beneficiary) <= 64.0)
                .stream().findAny().isPresent();
        AttributeInstance attack = beneficiary.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) return;
        if (active && !attack.hasModifier(FORMATION_ATTACK_ID)) attack.addTransientModifier(FORMATION_ATTACK);
        if (!active && attack.hasModifier(FORMATION_ATTACK_ID)) attack.removeModifier(FORMATION_ATTACK_ID);
    }

    private boolean sameOwner(RomanLegionaryEchoEntity1211 other) {
        return this.getOwnerUUID() != null && this.getOwnerUUID().equals(other.getOwnerUUID());
    }

    private boolean formationShieldProtectsThis() {
        LivingEntity owner = this.getOwner();
        if (!(owner instanceof Player player) || !(this.level() instanceof ServerLevel level)) return false;
        boolean shield = player.getMainHandItem().getItem() instanceof ShieldItem
                || player.getOffhandItem().getItem() instanceof ShieldItem;
        if (!shield) return false;
        return level.getEntitiesOfClass(RomanLegionaryEchoEntity1211.class,
                        this.getBoundingBox().inflate(8.0), candidate -> candidate.isAlive() && candidate.sameOwner(this)
                                && candidate.formationActive && candidate.distanceToSqr(this) <= 64.0)
                .stream().findAny().isPresent();
    }

    private void tickNaturalHealing(ServerLevel level, long now) {
        if (this.getHealth() >= this.getMaxHealth() || this.getTarget() != null
                || this.tickCount - this.getLastHurtByMobTimestamp() < 100 || now - this.lastNaturalHealAt < 40L
                || this.summonerId == null) return;
        if (!EchoBindingSystem1211.consumeFractionalFuel(level, this.summonerId,
                EchoRelicState1211.naturalHealingCost(activeRelic(), 2))) return;
        this.heal(1.0F);
        this.lastNaturalHealAt = now;
        level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(),
                2, 0.15, 0.3, 0.15, 0.0);
    }

    private EchoBindingSavedData1211.Binding currentBinding(ServerLevel level) {
        return this.summonerId == null ? null : EchoBindingSavedData1211.get(level.getServer()).get(this.summonerId);
    }

    private boolean skillEnabled(int skill) {
        return skill >= 0 && skill < 4 && (this.enabledSkills & 1 << skill) != 0;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        LivingEntity owner = this.getOwner();
        if (target == this || target == owner || target.isAlliedTo(this)
                || owner != null && owner.isAlliedTo(target)) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        if (target instanceof TamableAnimal tame && owner != null && tame.getOwnerUUID() != null
                && tame.getOwnerUUID().equals(owner.getUUID())) return false;
        return super.canAttack(target);
    }

    @Override
    protected float getDamageAfterMagicAbsorb(DamageSource source, float damage) {
        float adjusted = super.getDamageAfterMagicAbsorb(source, damage);
        if (isBulwarkProtection(source)) adjusted *= BULWARK_MULTIPLIER;
        if (formationShieldProtectsThis()) adjusted *= FORMATION_SHIELD_MULTIPLIER;
        return adjusted;
    }

    private boolean isBulwarkProtection(DamageSource source) {
        if (!skillEnabled(SKILL_BULWARK) || source.is(DamageTypeTags.IS_EXPLOSION)) return false;
        boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile;
        boolean direct = source.getDirectEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity();
        if (!projectile && !direct) return false;
        Vec3 sourcePosition = source.getSourcePosition();
        Vec3 toward = sourcePosition == null ? Vec3.ZERO
                : sourcePosition.subtract(this.position()).multiply(1.0, 0.0, 1.0);
        if (toward.horizontalDistanceSqr() < 1.0E-5 && source.getDirectEntity() instanceof Projectile shot) {
            toward = shot.getDeltaMovement().reverse().multiply(1.0, 0.0, 1.0);
        }
        return toward.horizontalDistanceSqr() >= 1.0E-5 && facing(this.yBodyRot).dot(toward.normalize()) >= 0.0;
    }

    private static Vec3 facing(float yaw) {
        double radians = Math.toRadians(yaw);
        return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (attacker == this.getOwner() || attacker instanceof RomanLegionaryEchoEntity1211 roman && sameOwner(roman)) {
            return false;
        }
        if (isLegionEnduresActive() && attacker instanceof LivingEntity living && canAttack(living)) {
            this.legionAccumulatedDamage += Math.max(0.0F, amount);
        }
        boolean blocked = isBulwarkProtection(source);
        float before = this.getHealth();
        boolean hurt = super.hurt(source, amount);
        if (hurt && this.level() instanceof ServerLevel level) {
            if (blocked && this.getHealth() < before) {
                Vec3 front = this.position().add(facing(this.yBodyRot).scale(this.getBbWidth() * 0.6)).add(0.0, 1.0, 0.0);
                level.sendParticles(ParticleTypes.CRIT, front.x, front.y, front.z, 5, 0.18, 0.35, 0.18, 0.01);
                level.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK,
                        SoundSource.PLAYERS, 0.4F, 1.15F);
            }
            long now = level.getGameTime();
            this.entityData.set(BLINK_START, now);
            this.entityData.set(BLINK_COUNT, (byte)1);
            this.nextBlinkAt = now + 50L + this.random.nextInt(71);
            if (attacker instanceof LivingEntity living && isVisibleAttentionTarget(living)) {
                this.visualDamageSourceId = living.getUUID();
                this.visualDamagePoint = living.getEyePosition();
                this.visualDamageUntil = now + 16L;
                applyVisualCandidate(new VisualCandidate(living, living.getEyePosition(), 1100,
                        VISUAL_HURT, VISUAL_KIND_DAMAGE, false), now);
            } else {
                this.visualDamageSourceId = null;
                this.visualDamagePoint = this.getSyncedEyeAttentionPoint();
                this.visualDamageUntil = now + 16L;
                this.entityData.set(VISUAL_REACTION, VISUAL_HURT);
                this.entityData.set(CURIOUS_TILT, (byte)0);
                this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
            }
            if (this.entityData.get(MELEE_ACTION) == MELEE_NONE
                    && this.shieldChargeTarget == null && !isLegionEnduresActive()) {
                stopActionTriggers();
                this.triggerAnim(ACTION_CONTROLLER, HURT_TRIGGER);
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        EchoBindingSystem1211.deactivate(this);
        cleanupTransientState();
        super.die(source);
    }

    @Override
    public void writeMigrationState(CompoundTag tag) {
        tag.putLong("RomanLastNaturalHealAt", this.lastNaturalHealAt);
    }

    @Override
    public void readMigrationState(CompoundTag tag) {
        this.lastNaturalHealAt = tag.getLong("RomanLastNaturalHealAt");
    }

    public void recallTo(Player player) {
        if (this.level() instanceof ServerLevel level && this.entityData.get(MELEE_ACTION) != MELEE_NONE) {
            finishMelee(level.getGameTime());
        }
        if (this.shieldChargeTarget != null) stopShieldCharge();
        if (isLegionEnduresActive()) cancelLegionEndures();
        clearCombatAndMovement();
        if (!com.yuriscat.echowarrior.compat.entity.behavior.EchoSafeTeleport1211.teleportBesideOwner(this, player)) {
            return;
        }
        this.activityAnchor = this.position();
        if (this.level() instanceof ServerLevel level) {
            applyVisualCandidate(new VisualCandidate(player, player.getEyePosition(), 220,
                    VISUAL_NORMAL, VISUAL_KIND_NORMAL, false), level.getGameTime());
        }
    }

    @Override
    public void dismiss() {
        if (this.isRemoved()) return;
        cleanupTransientState();
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(),
                    24, 0.35, 0.7, 0.35, 0.02);
            level.playSound(null, this.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                    SoundSource.PLAYERS, 0.7F, 0.75F);
        }
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        cleanupTransientState();
        super.remove(reason);
    }

    private void cleanupTransientState() {
        removeModifier(Attributes.ATTACK_DAMAGE, FORMATION_ATTACK_ID);
        removeModifier(Attributes.ARMOR, LEGION_ARMOR_ID);
        removeModifier(Attributes.KNOCKBACK_RESISTANCE, LEGION_KNOCKBACK_ID);
        LivingEntity owner = this.getOwner();
        if (owner != null) {
            AttributeInstance attack = owner.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) attack.removeModifier(FORMATION_ATTACK_ID);
        }
        this.reflectedProjectiles.clear();
    }

    private void addTransientModifier(Holder<Attribute> attribute, AttributeModifier modifier) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null && !instance.hasModifier(modifier.id())) instance.addTransientModifier(modifier);
    }

    private void removeModifier(Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    private static LivingEntity resolveLiving(ServerLevel level, UUID id) {
        if (id == null) return null;
        Entity entity = level.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.summonerId != null) tag.putUUID(SUMMONER_ID_KEY, this.summonerId);
        tag.putLong("EchoWarriorBindingGeneration", this.bindingGeneration);
        tag.putInt("EchoWarriorActivityMode", this.activityMode);
        tag.putInt("EchoWarriorAlertMode", this.alertMode);
        tag.putInt("EchoWarriorEnabledSkills", this.enabledSkills);
        tag.putDouble("EchoWarriorActivityAnchorX", this.activityAnchor.x);
        tag.putDouble("EchoWarriorActivityAnchorY", this.activityAnchor.y);
        tag.putDouble("EchoWarriorActivityAnchorZ", this.activityAnchor.z);
        tag.putLong("EchoWarriorLastNaturalHealAt", this.lastNaturalHealAt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.summonerId = tag.hasUUID(SUMMONER_ID_KEY) ? tag.getUUID(SUMMONER_ID_KEY) : null;
        this.bindingGeneration = tag.getLong("EchoWarriorBindingGeneration");
        this.activityMode = Mth.clamp(tag.getInt("EchoWarriorActivityMode"), 0, 2);
        this.alertMode = tag.contains("EchoWarriorAlertMode")
                ? Mth.clamp(tag.getInt("EchoWarriorAlertMode"), 0, 2) : ALERT_DEFENSIVE;
        this.enabledSkills = tag.contains("EchoWarriorEnabledSkills")
                ? tag.getInt("EchoWarriorEnabledSkills") & 0b1111 : 0b1111;
        this.activityAnchor = new Vec3(tag.getDouble("EchoWarriorActivityAnchorX"),
                tag.getDouble("EchoWarriorActivityAnchorY"), tag.getDouble("EchoWarriorActivityAnchorZ"));
        this.lastNaturalHealAt = tag.getLong("EchoWarriorLastNaturalHealAt");
    }

    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }
    @Override protected boolean shouldDropLoot() { return false; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "locomotion", 4,
                state -> state.setAndContinue((this.entityData.get(MELEE_ADVANCING)
                        || (state.isMoving() && this.entityData.get(MELEE_ACTION) == MELEE_NONE)) ? WALK : IDLE))
                .setAnimationSpeedHandler(entity -> entity.entityData.get(MELEE_ADVANCING)
                        ? (double)entity.entityData.get(MELEE_ACTION_SPEED) : 1.0D));
        controllers.add(new AnimationController<>(this, ACTION_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim(ATTACK_FIRST_TRIGGER, ATTACK_FIRST)
                .triggerableAnim(ATTACK_RECOVER_TRIGGER, ATTACK_RECOVER)
                .triggerableAnim(HURT_TRIGGER, HURT)
                .setAnimationSpeedHandler(entity -> entity.entityData.get(MELEE_ACTION) == MELEE_NONE
                        ? 1.0D : (double)entity.entityData.get(MELEE_ACTION_SPEED)));
        controllers.add(new AnimationController<>(this, "shield_pose", 0, state -> {
            int currentTick = state.getAnimatable().tickCount;
            if (state.getAnimatable().entityData.get(SHIELD_RAISED)) {
                this.shieldAnimationWasRaised = true;
                this.shieldLowerAnimationUntil = Integer.MIN_VALUE;
                return state.setAndContinue(SHIELD_RAISE);
            }
            if (this.shieldAnimationWasRaised) {
                this.shieldAnimationWasRaised = false;
                this.shieldLowerAnimationUntil = currentTick + 5;
            }
            return currentTick <= this.shieldLowerAnimationUntil
                    ? state.setAndContinue(SHIELD_LOWER) : PlayState.STOP;
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.animationCache; }
}
