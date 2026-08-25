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
import com.yuriscat.echowarrior.entity.behavior.EchoActivityMovement;
import com.yuriscat.echowarrior.entity.behavior.EchoFollowOwner;
import com.yuriscat.echowarrior.entity.behavior.EchoWaterSafety;
import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.EchoRelicState;
import com.yuriscat.echowarrior.item.SummonerFuel;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import com.yuriscat.echowarrior.progress.EchoExperienceSystem;
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
import net.minecraft.world.entity.MoverType;
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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
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

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class GuandaoWarriorEchoEntity extends PathfinderMob
		implements EchoWarriorEntity, SmartBrainOwner<GuandaoWarriorEchoEntity>, GeoEntity {
	public static final int SKILL_ARMOR_CLAD = 0;
	public static final int SKILL_GROWING_VALOR = 1;
	public static final int SKILL_CRESCENT_BLADE = 2;
	public static final int SKILL_COMBO = 3;

	private static final EntityDataAccessor<Boolean> COMBO_ACTIVE = SynchedEntityData.defineId(
			GuandaoWarriorEchoEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Byte> VALOR_STACKS = SynchedEntityData.defineId(
			GuandaoWarriorEchoEntity.class, EntityDataSerializers.BYTE);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guandao_warrior.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.guandao_warrior.walk");
	private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.guandao_warrior.attack");
	private static final RawAnimation COMBO = RawAnimation.begin().thenPlay("animation.guandao_warrior.combo");
	private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.guandao_warrior.hurt");
	private static final String ACTION_CONTROLLER = "action";
	private static final String ATTACK_TRIGGER = "attack";
	private static final String COMBO_TRIGGER = "combo";
	private static final String HURT_TRIGGER = "hurt";

	private static final int ATTACK_ANIMATION_TICKS = 34;
	private static final int COMBO_ANIMATION_TICKS = 101;
	private static final int VALOR_DURATION_TICKS = 160;
	private static final int MAX_VALOR_STACKS = 5;
	private static final double VALOR_DAMAGE_PER_STACK = 0.06;
	private static final double NORMAL_RADIUS = 3.25;
	private static final double NORMAL_ANGLE = 220.0;
	private static final int[] COMBO_HIT_TICKS = {23, 43, 58, 77};
	private static final double[] COMBO_DAMAGE = {0.70, 1.00, 0.70, 1.80};
	private static final double[] COMBO_RADIUS = {3.0, 3.5, 3.2, 4.0};
	private static final double[] COMBO_ANGLE = {150.0, 190.0, 120.0, 220.0};
	private static final double[] COMBO_FORWARD_OFFSET = {0.40, 0.50, 0.75, 0.50};
	private static final double[] COMBO_YAW_OFFSET = {0.0, 0.0, -10.0, 0.0};
	private static final Identifier COMBO_STEP_ID = EchoWarrior.id("guandao_combo_step_height");
	private static final AttributeModifier COMBO_STEP = new AttributeModifier(
			COMBO_STEP_ID, 0.45, AttributeModifier.Operation.ADD_VALUE);

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private final Set<UUID> comboPhaseHits = new HashSet<>();
	private final Set<UUID> deflectedProjectiles = new HashSet<>();
	private @Nullable EntityReference<LivingEntity> ownerReference;
	private @Nullable UUID summonerUuid;
	private int missingSummonerTicks;
	private EchoRelicState.ActivityMode activityMode = EchoRelicState.ActivityMode.FOLLOW;
	private EchoRelicState.AlertMode alertMode = EchoRelicState.AlertMode.DEFENSIVE;
	private int enabledSkills = EchoHeroType.GUANDAO_WARRIOR.allSkillsEnabledMask();
	private Vec3 activityAnchor = Vec3.ZERO;
	private long lastNaturalHealAt;
	private long attackAnimationUntil;
	private long valorExpiresAt;
	private long comboStartedAt;
	private int comboPhase = -1;
	private boolean comboPhaseAwardedValor;
	private double comboPhaseValorMultiplier = 1.0;
	private float comboYaw;
	private boolean projectileKnockbackContext;
	private int lastProjectileDamageTick = Integer.MIN_VALUE;
	private int movementAnimationLastMovingTick = Integer.MIN_VALUE;
	private boolean movementAnimationActive;

	public GuandaoWarriorEchoEntity(EntityType<? extends GuandaoWarriorEchoEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, EchoHeroType.GUANDAO_WARRIOR.baseMaximumHealth())
				.add(Attributes.ARMOR, EchoHeroType.GUANDAO_WARRIOR.baseArmor())
				.add(Attributes.ATTACK_DAMAGE, EchoHeroType.GUANDAO_WARRIOR.baseAttackDamage())
				.add(Attributes.MOVEMENT_SPEED, EchoHeroType.GUANDAO_WARRIOR.baseMovementSpeed())
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, EchoHeroType.GUANDAO_WARRIOR.baseKnockbackResistance());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(COMBO_ACTIVE, false);
		builder.define(VALOR_STACKS, (byte)0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
	}

	@Override
	public List<? extends ExtendedSensor<?>> getSensors(GuandaoWarriorEchoEntity owner) {
		return List.of(new NearbyLivingEntitySensor<GuandaoWarriorEchoEntity>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getAlwaysRunningBehaviours(GuandaoWarriorEchoEntity owner) {
		return List.of(new MoveToWalkTarget<>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getIdleBehaviours(GuandaoWarriorEchoEntity owner) {
		return List.of(new EchoFollowOwner<GuandaoWarriorEchoEntity>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getFightingBehaviours(GuandaoWarriorEchoEntity owner) {
		return List.of(
				new InvalidateAttackTarget<GuandaoWarriorEchoEntity>(),
				new SetWalkTargetToAttackTarget<GuandaoWarriorEchoEntity>().speedModifier(1.0F).closeEnoughDist(2),
				new AnimatableMeleeAttack<GuandaoWarriorEchoEntity>(9)
						.attackInterval((entity, target) -> entity.meleeAttackInterval())
						.canAttack((entity, target) -> entity.canPerformMeleeHit(target))
						.whenStarting(GuandaoWarriorEchoEntity::startMeleeAttackAnimation)
		);
	}

	private void startMeleeAttackAnimation() {
		this.attackAnimationUntil = this.level().getGameTime() + ATTACK_ANIMATION_TICKS;
		this.triggerAnim(ACTION_CONTROLLER, ATTACK_TRIGGER);
	}

	private boolean canPerformMeleeHit(LivingEntity target) {
		if (isComboActive() || !target.isAlive() || !this.canAttack(target) || !this.hasLineOfSight(target)) return false;
		double reach = NORMAL_RADIUS + target.getBbWidth() * 0.5;
		return horizontalDistanceSqr(this.position(), target.position()) <= reach * reach;
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
		if (getValorStacks() > 0 && now >= this.valorExpiresAt) {
			setValorStacks(0);
		}
		ItemStack relic = currentRelic();
		if (!relic.isEmpty()) {
			if (isComboActive()) tickCombo(level);
			else tryStartCombo(level, relic);
			if (this.tickCount % 20 == 0) {
				applyRelicState(relic, false);
				tickNaturalHealing(level, relic);
			}
		} else if (isComboActive()) {
			finishCombo();
		}

		if (this.tickCount % 5 == 0 && !isComboActive()) {
			LivingEntity target = selectProtectiveTarget(owner);
			if (target != null) BrainUtil.setTargetOfEntity(this, target);
			enforceActivityBoundary(owner);
		}
		if (this.tickCount % 20 == 0 && this.getTarget() != null) {
			EchoExperienceSystem.markParticipation(this, this.getTarget());
		}
		EchoActivityMovement.tick(level, this, this.activityMode, this.activityAnchor,
				this.getTarget() != null || isComboActive() || now < this.attackAnimationUntil);
		EchoWaterSafety.tick(level, this, owner,
				this.activityMode == EchoRelicState.ActivityMode.FOLLOW && !isComboActive());
	}

	private void tryStartCombo(ServerLevel level, ItemStack relic) {
		if (!EchoRelicState.skillEnabled(relic, SKILL_COMBO)) return;
		long now = level.getGameTime();
		if (now < EchoRelicState.guandaoComboCooldownEnd(relic)) return;
		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive() || !this.canAttack(target) || !this.hasLineOfSight(target)) return;
		float desiredYaw = yawToward(this.getX(), this.getZ(), target.getX(), target.getZ());
		if (horizontalDistanceSqr(this.position(), target.position()) > 4.5 * 4.5
				|| !hasSafeForwardSupport(level, 0.7, desiredYaw)) return;

		AABB triggerArea = this.getBoundingBox().inflate(4.5, 2.5, 4.5);
		long enemies = level.getEntitiesOfClass(LivingEntity.class, triggerArea,
				candidate -> isValidCombatEnemy(candidate) && this.hasLineOfSight(candidate)).stream().limit(2).count();
		double normalDamage = this.getAttributeValue(Attributes.ATTACK_DAMAGE) * valorDamageMultiplier();
		if (enemies < 2 && target.getHealth() <= normalDamage * 2.0) return;

		EchoRelicState.setGuandaoComboCooldownEnd(relic, now + EchoRelicState.GUANDAO_COMBO_COOLDOWN_TICKS);
		persistCurrentRelic(relic);
		this.comboStartedAt = now;
		this.comboYaw = desiredYaw;
		this.comboPhase = -1;
		this.comboPhaseHits.clear();
		this.deflectedProjectiles.clear();
		this.entityData.set(COMBO_ACTIVE, true);
		setComboStepHeight(true);
		lockComboFacing();
		this.getNavigation().stop();
		BrainUtil.clearMemory(this, net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
		this.triggerAnim(ACTION_CONTROLLER, COMBO_TRIGGER);
	}

	private void tickCombo(ServerLevel level) {
		int elapsed = (int)(level.getGameTime() - this.comboStartedAt);
		if (elapsed >= COMBO_ANIMATION_TICKS) {
			finishCombo();
			return;
		}
		this.getNavigation().stop();
		BrainUtil.clearMemory(this, net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
		this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
		if (elapsed == 30 || elapsed == 48 || elapsed == 65) retargetBetweenComboStrikes();
		lockComboFacing();
		moveComboForward(level, elapsed);

		int phase = comboPhaseForTick(elapsed);
		if (phase >= 0) {
			if (phase != this.comboPhase) {
				this.comboPhase = phase;
				this.comboPhaseHits.clear();
				this.comboPhaseAwardedValor = false;
				this.comboPhaseValorMultiplier = valorDamageMultiplier();
			}
			int hits = performSectorAttack(
					level,
					COMBO_RADIUS[phase],
					COMBO_ANGLE[phase],
					COMBO_FORWARD_OFFSET[phase],
					COMBO_YAW_OFFSET[phase],
					COMBO_DAMAGE[phase] * this.comboPhaseValorMultiplier,
					this.comboPhaseHits,
					phase == 0
			);
			if (hits > 0 && !this.comboPhaseAwardedValor) {
				this.comboPhaseAwardedValor = true;
				addValorStack(level.getGameTime());
				level.playSound(null, this.blockPosition(),
						phase == 3 ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_SWEEP,
						SoundSource.PLAYERS, phase == 3 ? 0.9F : 0.55F, phase == 3 ? 0.75F : 0.95F);
			}
		}
		if (elapsed >= 38 && elapsed <= 47) deflectFrontProjectiles(level);
	}

	private int comboPhaseForTick(int elapsed) {
		for (int index = 0; index < COMBO_HIT_TICKS.length; index++) {
			if (Math.abs(elapsed - COMBO_HIT_TICKS[index]) <= 1) return index;
		}
		return -1;
	}

	private void moveComboForward(ServerLevel level, int elapsed) {
		double distance = 0.0;
		if (elapsed >= 16 && elapsed <= 23) distance = 0.075;
		else if (elapsed >= 36 && elapsed <= 43) distance = 0.10;
		else if (elapsed >= 51 && elapsed <= 58) distance = 0.075;
		else if (elapsed >= 68 && elapsed <= 77) distance = 0.10;
		if (distance <= 0.0 || !hasSafeForwardSupport(level, distance + 0.35)) return;
		Vec3 movement = facing(this.comboYaw).scale(distance);
		this.move(MoverType.SELF, movement);
	}

	private boolean hasSafeForwardSupport(ServerLevel level, double distance) {
		return hasSafeForwardSupport(level, distance, this.comboYaw);
	}

	private boolean hasSafeForwardSupport(ServerLevel level, double distance, float yaw) {
		Vec3 ahead = this.position().add(facing(yaw).scale(distance));
		HitResult floor = level.clip(new ClipContext(
				new Vec3(ahead.x, this.getY() + 1.10, ahead.z),
				new Vec3(ahead.x, this.getY() - 1.15, ahead.z),
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				this
		));
		return floor.getType() != HitResult.Type.MISS;
	}

	private void retargetBetweenComboStrikes() {
		LivingEntity best = this.level().getEntitiesOfClass(
				LivingEntity.class,
				this.getBoundingBox().inflate(5.5, 2.75, 5.5),
				candidate -> isValidCombatEnemy(candidate) && this.hasLineOfSight(candidate)
		).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
		if (best == null) return;
		BrainUtil.setTargetOfEntity(this, best);
		float desired = yawToward(this.getX(), this.getZ(), best.getX(), best.getZ());
		this.comboYaw += Mth.clamp(Mth.wrapDegrees(desired - this.comboYaw), -40.0F, 40.0F);
	}

	private void lockComboFacing() {
		this.setYRot(this.comboYaw);
		this.setYBodyRot(this.comboYaw);
		this.setYHeadRot(this.comboYaw);
	}

	private void finishCombo() {
		this.entityData.set(COMBO_ACTIVE, false);
		this.comboPhase = -1;
		this.comboPhaseHits.clear();
		this.deflectedProjectiles.clear();
		setComboStepHeight(false);
	}

	private void setComboStepHeight(boolean active) {
		var step = this.getAttribute(Attributes.STEP_HEIGHT);
		if (step == null) return;
		step.removeModifier(COMBO_STEP_ID);
		if (active) step.addTransientModifier(COMBO_STEP);
	}

	private void deflectFrontProjectiles(ServerLevel level) {
		Vec3 facing = facing(this.comboYaw);
		boolean deflected = false;
		for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(3.5, 2.5, 3.5))) {
			if (!projectile.isAlive() || this.deflectedProjectiles.contains(projectile.getUUID())) continue;
			Entity projectileOwner = projectile.getOwner();
			if (!(projectileOwner instanceof LivingEntity livingOwner) || !this.canAttack(livingOwner)) continue;
			Vec3 towardProjectile = projectile.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
			if (towardProjectile.lengthSqr() > 3.5 * 3.5 || towardProjectile.lengthSqr() < 1.0E-5) continue;
			Vec3 direction = towardProjectile.normalize();
			if (facing.dot(direction) < 0.0 || towardProjectile.dot(projectile.getDeltaMovement()) >= -0.01) continue;

			double speed = projectile.getDeltaMovement().length();
			if (speed < 0.01) continue;
			double side = facing.x * direction.z - facing.z * direction.x >= 0.0 ? 1.0 : -1.0;
			Vec3 perpendicular = new Vec3(-facing.z, 0.0, facing.x);
			Vec3 redirected = facing.scale(0.7).add(perpendicular.scale(side * 0.8)).add(0.0, 0.18, 0.0).normalize();
			projectile.setDeltaMovement(redirected.scale(speed));
			this.deflectedProjectiles.add(projectile.getUUID());
			level.sendParticles(ParticleTypes.CRIT, projectile.getX(), projectile.getY(), projectile.getZ(),
					5, 0.12, 0.12, 0.12, 0.12);
			deflected = true;
		}
		if (deflected) {
			level.playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.25F, 1.75F);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity ignoredPrimaryTarget) {
		if (isComboActive()) return false;
		Set<UUID> hits = new HashSet<>();
		int hitCount = performSectorAttack(level, NORMAL_RADIUS, NORMAL_ANGLE, 0.45, 0.0,
				valorDamageMultiplier(), hits, false);
		if (hitCount <= 0) return false;
		addValorStack(level.getGameTime());
		level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
				SoundSource.PLAYERS, 0.55F, 0.9F);
		return true;
	}

	private int performSectorAttack(
			ServerLevel level,
			double radius,
			double angle,
			double forwardOffset,
			double yawOffset,
			double damageMultiplier,
			Set<UUID> alreadyHit,
			boolean launch
	) {
		Vec3 attackFacing = facing(this.getYRot() + (float)yawOffset);
		Vec3 origin = this.position().add(attackFacing.scale(forwardOffset));
		double cosine = Math.cos(Math.toRadians(angle * 0.5));
		float damage = (float)(this.getAttributeValue(Attributes.ATTACK_DAMAGE) * damageMultiplier);
		int hits = 0;
		AABB area = new AABB(origin.x - radius, this.getY() - 0.75, origin.z - radius,
				origin.x + radius, this.getY() + 2.75, origin.z + radius);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, this::isValidCombatEnemy)) {
			if (alreadyHit.contains(target.getUUID()) || !this.hasLineOfSight(target)) continue;
			Vec3 toward = target.getBoundingBox().getCenter().subtract(origin).multiply(1.0, 0.0, 1.0);
			double permittedRadius = radius + target.getBbWidth() * 0.5;
			if (toward.lengthSqr() > permittedRadius * permittedRadius) continue;
			if (toward.lengthSqr() > 1.0E-5 && attackFacing.dot(toward.normalize()) < cosine) continue;
			if (!target.hurtServer(level, level.damageSources().mobAttack(this), damage)) continue;
			alreadyHit.add(target.getUUID());
			hits++;
			EchoExperienceSystem.markParticipation(this, target);
			if (launch) launchTarget(target, origin);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,
					target.getX(), target.getY() + target.getBbHeight() * 0.55, target.getZ(),
					1, 0.0, 0.0, 0.0, 0.0);
		}
		return hits;
	}

	private void launchTarget(LivingEntity target, Vec3 origin) {
		double resistance = Math.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
		double strength = 1.0 - resistance;
		if (strength <= 0.0) return;
		Vec3 outward = target.position().subtract(origin).multiply(1.0, 0.0, 1.0);
		if (outward.lengthSqr() < 1.0E-5) outward = facing(this.comboYaw);
		else outward = outward.normalize();
		double upward = Math.max(0.0, 0.36 * strength - target.getDeltaMovement().y);
		target.push(outward.x * 0.12 * strength, upward, outward.z * 0.12 * strength);
	}

	private void addValorStack(long now) {
		setValorStacks(Math.min(MAX_VALOR_STACKS, getValorStacks() + 1));
		this.valorExpiresAt = now + VALOR_DURATION_TICKS;
	}

	private void setValorStacks(int stacks) {
		this.entityData.set(VALOR_STACKS, (byte)Math.clamp(stacks, 0, MAX_VALOR_STACKS));
	}

	public int getValorStacks() {
		return this.entityData.get(VALOR_STACKS);
	}

	private double valorDamageMultiplier() {
		return 1.0 + getValorStacks() * VALOR_DAMAGE_PER_STACK;
	}

	public boolean isComboActive() {
		return this.entityData.get(COMBO_ACTIVE);
	}

	private @Nullable LivingEntity selectProtectiveTarget(LivingEntity owner) {
		LivingEntity ownAttacker = this.getLastHurtByMob();
		if (isRecent(this, this.getLastHurtByMobTimestamp()) && canProtectAgainst(ownAttacker)) return ownAttacker;
		if (this.alertMode == EchoRelicState.AlertMode.PEACEFUL) return null;
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isRecent(owner, owner.getLastHurtByMobTimestamp()) && canProtectAgainst(ownerAttacker)) return ownerAttacker;
		LivingEntity ownerTarget = owner.getLastHurtMob();
		if (isRecent(owner, owner.getLastHurtMobTimestamp()) && canProtectAgainst(ownerTarget)) return ownerTarget;
		if (this.alertMode != EchoRelicState.AlertMode.AGGRESSIVE) return null;
		double range = this.activityMode == EchoRelicState.ActivityMode.WAIT ? 6.0 : 16.0;
		AABB box = this.activityMode == EchoRelicState.ActivityMode.WAIT
				? new AABB(this.activityAnchor.x - range, this.activityAnchor.y - 4.0, this.activityAnchor.z - range,
					this.activityAnchor.x + range, this.activityAnchor.y + 4.0, this.activityAnchor.z + range)
				: this.getBoundingBox().inflate(range);
		return this.level().getEntitiesOfClass(Monster.class, box, this::canProtectAgainst).stream()
				.min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
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
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		Entity attacker = source.getEntity();
		if (attacker == this.getOwner() || attacker instanceof EchoWarriorEntity echo && echo.getOwner() == this.getOwner()) return false;
		float previousHealth = this.getHealth();
		boolean previousProjectileContext = this.projectileKnockbackContext;
		boolean projectile = source.is(DamageTypeTags.IS_PROJECTILE);
		this.projectileKnockbackContext = projectile;
		if (projectile) this.lastProjectileDamageTick = this.tickCount;
		boolean hurt;
		try {
			hurt = super.hurtServer(level, source, damage);
		} finally {
			this.projectileKnockbackContext = previousProjectileContext;
		}
		if (hurt && this.getHealth() < previousHealth) {
			if (!isComboActive() && level.getGameTime() >= this.attackAnimationUntil) {
				this.triggerAnim(ACTION_CONTROLLER, HURT_TRIGGER);
			}
			boolean directMelee = attacker instanceof LivingEntity && source.getDirectEntity() == attacker
					&& !source.is(DamageTypeTags.IS_PROJECTILE);
			if (projectile || directMelee) reduceComboCooldown(level, 10L);
		}
		return hurt;
	}

	private void reduceComboCooldown(ServerLevel level, long ticks) {
		ItemStack relic = currentRelic();
		if (relic.isEmpty()) return;
		long now = level.getGameTime();
		long end = EchoRelicState.guandaoComboCooldownEnd(relic);
		if (end <= now) return;
		EchoRelicState.setGuandaoComboCooldownEnd(relic, Math.max(now, end - ticks));
		persistCurrentRelic(relic);
	}

	@Override
	protected float getDamageAfterMagicAbsorb(DamageSource source, float damage) {
		float adjusted = super.getDamageAfterMagicAbsorb(source, damage);
		if (source.is(DamageTypeTags.IS_PROJECTILE)) adjusted *= 0.50F;
		boolean excluded = source.is(DamageTypeTags.IS_FIRE)
				|| source.is(DamageTypeTags.BYPASSES_EFFECTS)
				|| source.is(DamageTypeTags.BYPASSES_RESISTANCE)
				|| source.is(DamageTypes.IN_WALL);
		if (!excluded && this.getMaxHealth() > 0.0F) {
			float missingHealth = 1.0F - this.getHealth() / this.getMaxHealth();
			adjusted *= 1.0F - Math.clamp(missingHealth * 0.30F, 0.0F, 0.30F);
		}
		return adjusted;
	}

	@Override
	public void knockback(double power, double xd, double zd) {
		if (this.projectileKnockbackContext || this.tickCount == this.lastProjectileDamageTick) {
			double resistance = Math.clamp(this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
			double vanillaRetained = 1.0 - resistance;
			double desiredRetained = Math.min(vanillaRetained, 0.05);
			if (vanillaRetained <= 0.0) return;
			power *= desiredRetained / vanillaRetained;
		}
		super.knockback(power, xd, zd);
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	private void tickNaturalHealing(ServerLevel level, ItemStack relic) {
		long now = level.getGameTime();
		if (this.getHealth() >= this.getMaxHealth() || this.getTarget() != null || isComboActive()
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

	private int meleeAttackInterval() {
		ItemStack relic = currentRelic();
		return relic.isEmpty() ? EchoHeroType.GUANDAO_WARRIOR.baseAttackIntervalTicks()
				: EchoRelicState.attackIntervalTicks(relic);
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
	@Override public EchoHeroType heroType() { return EchoHeroType.GUANDAO_WARRIOR; }
	@Override public boolean shouldFollowOwner() { return this.activityMode == EchoRelicState.ActivityMode.FOLLOW && !isComboActive(); }
	@Override public boolean isFollowMovementSuppressed() { return isComboActive(); }
	@Override public @Nullable UUID getOwnerUuid() { LivingEntity owner = getOwner(); return owner == null ? null : owner.getUUID(); }
	@Override public @Nullable UUID getSummonerUuid() { return this.summonerUuid; }
	@Override public @Nullable EntityReference<LivingEntity> getOwnerReference() { return this.ownerReference; }

	@Override
	public void recallTo(Player player) {
		finishCombo();
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
		finishCombo();
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 24, 0.35, 0.7, 0.35, 0.02);
			level.playSound(null, this.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.7F, 0.75F);
		}
		this.discard();
	}

	@Override
	public void onRemoval(Entity.RemovalReason reason) {
		setComboStepHeight(false);
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
		output.putInt("ValorStacks", getValorStacks());
		output.putLong("ValorExpiresAt", this.valorExpiresAt);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ownerReference = EntityReference.readWithOldOwnerConversion(input, "EchoOwner", this.level());
		try { this.summonerUuid = UUID.fromString(input.getStringOr("SummonerUuid", "")); }
		catch (IllegalArgumentException ignored) { this.summonerUuid = null; }
		this.activityMode = EchoRelicState.ActivityMode.byOrdinal(input.getIntOr("ActivityMode", 0));
		this.alertMode = EchoRelicState.AlertMode.byOrdinal(input.getIntOr("AlertMode", 1));
		this.enabledSkills = input.getIntOr("EnabledSkills", EchoHeroType.GUANDAO_WARRIOR.allSkillsEnabledMask());
		this.activityAnchor = new Vec3(input.getDoubleOr("ActivityAnchorX", this.getX()),
				input.getDoubleOr("ActivityAnchorY", this.getY()), input.getDoubleOr("ActivityAnchorZ", this.getZ()));
		setValorStacks(input.getIntOr("ValorStacks", 0));
		this.valorExpiresAt = input.getLongOr("ValorExpiresAt", 0L);
	}

	@Override protected boolean shouldDropLoot(ServerLevel level) { return false; }

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<GuandaoWarriorEchoEntity>("movement", 3, this::selectMovementAnimation));
		controllers.add(new AnimationController<GuandaoWarriorEchoEntity>(ACTION_CONTROLLER, 1, test -> PlayState.STOP)
				.triggerableAnim(ATTACK_TRIGGER, ATTACK)
				.triggerableAnim(COMBO_TRIGGER, COMBO)
				.triggerableAnim(HURT_TRIGGER, HURT));
	}

	private PlayState selectMovementAnimation(AnimationTest<GuandaoWarriorEchoEntity> test) {
		int currentTick = test.animatable().tickCount;
		if (test.isMoving() && !test.animatable().isComboActive()) {
			this.movementAnimationActive = true;
			this.movementAnimationLastMovingTick = currentTick;
		} else if (this.movementAnimationActive && currentTick - this.movementAnimationLastMovingTick >= 4) {
			this.movementAnimationActive = false;
		}
		return test.setAndContinue(this.movementAnimationActive ? WALK : IDLE);
	}

	@Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.animationCache; }

	private static Vec3 facing(float yaw) {
		double radians = Math.toRadians(yaw);
		return new Vec3(-Math.sin(radians), 0.0, Math.cos(radians));
	}

	private static float yawToward(double fromX, double fromZ, double targetX, double targetZ) {
		return (float)(Math.atan2(targetZ - fromZ, targetX - fromX) * 180.0 / Math.PI) - 90.0F;
	}

	private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
		double x = first.x - second.x;
		double z = first.z - second.z;
		return x * x + z * z;
	}
}
