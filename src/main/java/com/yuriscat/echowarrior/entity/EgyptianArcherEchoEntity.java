package com.yuriscat.echowarrior.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
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
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class EgyptianArcherEchoEntity extends PathfinderMob implements EchoWarriorEntity, GeoEntity {
	public static final int SKILL_CAT_GOD = 0;
	public static final int SKILL_ARROW_MODE = 1;
	public static final int SKILL_CHARIOT_VOLLEY = 2;
	public static final int SKILL_BACKSTEP = 3;

	private static final byte ACTION_IDLE = 0;
	private static final byte ACTION_DRAW = 1;
	private static final byte ACTION_SHOOT = 2;
	private static final byte ACTION_BACKSTEP = 3;
	private static final byte ACTION_MELEE = 4;
	private static final EntityDataAccessor<Byte> ACTION = SynchedEntityData.defineId(
			EgyptianArcherEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> ATTACK_INTERVAL = SynchedEntityData.defineId(
			EgyptianArcherEchoEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ARROW_MODE = SynchedEntityData.defineId(
			EgyptianArcherEchoEntity.class, EntityDataSerializers.INT);

	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.egyptian_archer.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.egyptian_archer.walk");
	private static final RawAnimation DRAW_BOW_UPPER = RawAnimation.begin().thenPlayAndHold("animation.egyptian_archer.draw_bow_upper");
	private static final RawAnimation DRAW_BOW_LOWER = RawAnimation.begin().thenPlayAndHold("animation.egyptian_archer.draw_bow_lower");
	private static final RawAnimation SHOOT_UPPER = RawAnimation.begin().thenPlay("animation.egyptian_archer.shoot_upper");
	private static final RawAnimation SHOOT_LOWER = RawAnimation.begin().thenPlay("animation.egyptian_archer.shoot_lower");
	private static final RawAnimation BACKSTEP = RawAnimation.begin().thenPlay("animation.egyptian_archer.backstep_jump");
	private static final RawAnimation MELEE = RawAnimation.begin().thenPlay("animation.egyptian_archer.melee_attack");
	private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.egyptian_archer.hurt");
	private static final String ACTION_CONTROLLER = "action";
	private static final String DRAW_TRIGGER = "draw_bow";
	private static final String SHOOT_TRIGGER = "shoot";
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
	private static final int MAX_BACKSTEP_TARGETS = 6;
	private static final float ARROW_SPEED = 2.2F;
	private static final float ARROW_INACCURACY = 0.5F;
	private static final double ARROW_GRAVITY = 0.05;
	private static final float COMBAT_TURN_SPEED = 45.0F;
	private static final float FIRE_FACING_TOLERANCE = 20.0F;
	private static final int MAX_FULL_DRAW_HOLD_TICKS = 6;
	private static final int EMERGENCY_TARGET_TICKS = 60;
	private static final int VOLLEY_PARTICLE_COLOR = 0xE6C84E;

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable EntityReference<LivingEntity> ownerReference;
	private @Nullable UUID summonerUuid;
	private int missingSummonerTicks;
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
	private boolean pendingBackstep;
	private @Nullable LivingEntity actionTarget;
	private @Nullable Monster highThreatTarget;
	private @Nullable LivingEntity resumeTargetAfterThreat;
	private @Nullable LivingEntity emergencyTarget;
	private @Nullable LivingEntity resumeTargetAfterEmergency;
	private long emergencyTargetUntil;
	private Vec3 backstepStart = Vec3.ZERO;
	private Vec3 backstepLanding = Vec3.ZERO;
	private float backstepYaw;
	private boolean backstepVolleyReleased;
	private int movementAnimationLastMovingTick = Integer.MIN_VALUE;
	private boolean movementAnimationActive;
	private boolean combatApproaching;
	private boolean combatKiting;
	private float combatFacingYaw;
	private boolean combatFacingInitialized;

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
		builder.define(ARROW_MODE, EchoRelicState.EgyptianArrowMode.OFF.ordinal());
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
		if ((this.tickCount & 1) == 0) {
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
				this.getTarget() != null || action() != ACTION_IDLE);
		EchoWaterSafety.tick(level, this, owner, this.activityMode == EchoRelicState.ActivityMode.FOLLOW
				&& action() != ACTION_BACKSTEP);
		LivingEntity facingTarget = this.getTarget();
		if (facingTarget != null && (action() == ACTION_DRAW || action() == ACTION_SHOOT || action() == ACTION_MELEE)) {
			faceTarget(facingTarget);
		}
	}

	private void tickCombat(ServerLevel level, ItemStack relic) {
		long now = level.getGameTime();
		LivingEntity target = this.getTarget();
		if (target == null || !canProtectAgainst(target)) {
			this.setTarget(null);
			if (action() == ACTION_DRAW) finishAction();
			if (action() == ACTION_BACKSTEP) tickBackstep(level, relic);
			if ((action() == ACTION_SHOOT || action() == ACTION_MELEE) && now >= this.actionEndsAt) finishAction();
			return;
		}
		this.getLookControl().setLookAt(target, 35.0F, 35.0F);

		if (action() == ACTION_BACKSTEP) {
			tickBackstep(level, relic);
			return;
		}
		if (action() == ACTION_DRAW) {
			if (shouldBackstep(target, relic) && findBackstepLanding(level, target) != null) {
				startBackstep(level, relic, target);
				return;
			}
			if (now >= this.actionEndsAt && (isFacingTarget(target)
					|| now >= this.actionEndsAt + MAX_FULL_DRAW_HOLD_TICKS)) enterShoot(now);
			return;
		}
		if (action() == ACTION_SHOOT) {
			boolean waitingForAim = false;
			if (!this.shotReleased && now >= this.shotReleaseAt) {
				if (isFacingTarget(target) || now >= this.shotReleaseAt + MAX_FULL_DRAW_HOLD_TICKS) {
					this.shotReleased = true;
					fireMainShot(level, target);
				} else waitingForAim = true;
			}
			if (shouldBackstep(target, relic)) this.pendingBackstep = true;
			if (now >= this.actionEndsAt && !waitingForAim) {
				finishAction();
				if (this.pendingBackstep) {
					this.pendingBackstep = false;
					startBackstep(level, relic, target);
				}
			}
			return;
		}
		if (action() == ACTION_MELEE) {
			if (!this.shotReleased && now >= this.shotReleaseAt) {
				this.shotReleased = true;
				if (this.distanceToSqr(target) <= 7.0 && this.hasLineOfSight(target)) {
					float damage = target.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD) ? 3.6F : 3.0F;
					target.hurtServer(level, level.damageSources().mobAttack(this), damage);
				}
			}
			if (now >= this.actionEndsAt) finishAction();
			return;
		}

		if (shouldBackstep(target, relic)) {
			if (startBackstep(level, relic, target)) return;
			if (this.distanceToSqr(target) <= 7.0) {
				startMeleeAttack(now, target);
				return;
			}
		}
		if (now >= this.nextAttackAt && this.distanceToSqr(target) <= MAX_RANGE * MAX_RANGE
				&& this.hasLineOfSight(target)) {
			startRangedAttack(now, target);
			return;
		}
		if (this.distanceToSqr(target) <= 7.0 && !canBackstep(relic)) startMeleeAttack(now, target);
	}

	private void startRangedAttack(long now, LivingEntity target) {
		int interval = attackInterval();
		int drawTicks = Math.max(8, Math.round(31.0F * interval / 42.0F));
		this.entityData.set(ACTION, ACTION_DRAW);
		this.actionStartedAt = now;
		this.actionEndsAt = now + drawTicks;
		this.actionTarget = target;
		beginCombatFacing();
		this.shotReleased = false;
		this.nextAttackAt = now + interval;
		this.triggerAnim(ACTION_CONTROLLER, DRAW_TRIGGER);
	}

	private void enterShoot(long now) {
		int interval = attackInterval();
		int shootTicks = Math.max(4, interval - Math.max(8, Math.round(31.0F * interval / 42.0F)));
		this.entityData.set(ACTION, ACTION_SHOOT);
		this.actionStartedAt = now;
		this.actionEndsAt = now + shootTicks;
		this.shotReleaseAt = now + Math.max(1, Math.round(4.0F * interval / 42.0F));
		this.triggerAnim(ACTION_CONTROLLER, SHOOT_TRIGGER);
	}

	private void fireMainShot(ServerLevel level, LivingEntity target) {
		if (!target.isAlive()) return;
		// Line of sight is checked before drawing the bow. Once the shot is committed,
		// always create the projectile and let its normal block collision handle cover.
		// Rechecking here could silently consume shots when a ledge briefly obscured a
		// target below the archer between the draw and release frames.
		spawnArrow(level, target);
		if (!skillEnabled(SKILL_CHARIOT_VOLLEY)) return;
		List<LivingEntity> alternatives = combatTargets(level, VOLLEY_RANGE, target);
		int enemyCount = alternatives.size() + 1;
		if (enemyCount < 2) return;
		float chance = Math.min(0.55F, 0.15F + 0.08F * (enemyCount - 2));
		if (this.random.nextFloat() < chance
				&& spawnArrow(level, alternatives.get(this.random.nextInt(alternatives.size())))) {
			level.sendParticles(new DustParticleOptions(VOLLEY_PARTICLE_COLOR, 0.9F),
					this.getX(), this.getY() + this.getBbHeight() * 0.58, this.getZ(),
					14, 0.42, 0.55, 0.42, 0.025);
		}
	}

	private boolean spawnArrow(ServerLevel level, LivingEntity target) {
		EgyptianArcherArrowEntity arrow = ModEntities.EGYPTIAN_ARCHER_ARROW.create(level,
				net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
		if (arrow == null) return false;
		arrow.setPos(this.getX(), this.getEyeY() - 0.18, this.getZ());
		boolean pierce = this.arrowMode == EchoRelicState.EgyptianArrowMode.CONE && this.random.nextFloat() < 0.25F;
		arrow.configure(this, this.arrowMode, (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE), pierce);
		double dx = target.getX() - arrow.getX();
		double dz = target.getZ() - arrow.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		double targetY = target.getY() + target.getBbHeight() * 0.62;
		double dy = ballisticAimHeight(horizontal, targetY - arrow.getY());
		arrow.shoot(dx, dy, dz, ARROW_SPEED, ARROW_INACCURACY);
		if (!level.addFreshEntity(arrow)) return false;
		level.playSound(null, this.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.75F, 0.95F + this.random.nextFloat() * 0.1F);
		return true;
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
		float desiredYaw = yawToward(this.getX(), this.getZ(), target.getX(), target.getZ());
		if (!this.combatFacingInitialized) beginCombatFacing();
		this.combatFacingYaw = Mth.approachDegrees(this.combatFacingYaw, desiredYaw, COMBAT_TURN_SPEED);
		this.setYRot(this.combatFacingYaw);
		this.setYBodyRot(this.combatFacingYaw);
		this.setYHeadRot(this.combatFacingYaw);
		this.getLookControl().setLookAt(target, 90.0F, 90.0F);
	}

	private void beginCombatFacing() {
		this.combatFacingYaw = this.getYRot();
		this.combatFacingInitialized = true;
	}

	private boolean isFacingTarget(LivingEntity target) {
		float desiredYaw = yawToward(this.getX(), this.getZ(), target.getX(), target.getZ());
		float currentYaw = this.combatFacingInitialized ? this.combatFacingYaw : this.getYRot();
		return Math.abs(Mth.degreesDifference(currentYaw, desiredYaw)) <= FIRE_FACING_TOLERANCE;
	}

	private void startMeleeAttack(long now, LivingEntity target) {
		this.entityData.set(ACTION, ACTION_MELEE);
		this.actionStartedAt = now;
		this.actionEndsAt = now + 20L;
		this.shotReleaseAt = now + 8L;
		this.shotReleased = false;
		this.actionTarget = target;
		beginCombatFacing();
		this.nextAttackAt = now + 20L;
		this.triggerAnim(ACTION_CONTROLLER, MELEE_TRIGGER);
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
		persistCurrentRelic(relic);
		this.backstepStart = this.position();
		this.backstepLanding = landing;
		this.backstepYaw = this.combatFacingInitialized ? this.combatFacingYaw
				: yawToward(this.getX(), this.getZ(), nearestEnemy.getX(), nearestEnemy.getZ());
		this.setYRot(this.backstepYaw);
		this.setYBodyRot(this.backstepYaw);
		this.setYHeadRot(this.backstepYaw);
		this.actionStartedAt = level.getGameTime();
		this.actionEndsAt = this.actionStartedAt + BACKSTEP_TICKS;
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
		this.snapTo(point.x, point.y, point.z, this.backstepYaw, this.getXRot());
		this.setYRot(this.backstepYaw);
		this.setYBodyRot(this.backstepYaw);
		this.setYHeadRot(this.backstepYaw);
		this.setDeltaMovement(Vec3.ZERO);
		if (!this.backstepVolleyReleased && elapsed >= 6L) {
			this.backstepVolleyReleased = true;
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
				if (!isSafeLanding(level, candidate)) continue;
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
		return level.noCollision(this, moved);
	}

	private List<LivingEntity> combatTargets(ServerLevel level, double range, @Nullable LivingEntity excluded) {
		return level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range), candidate ->
				candidate != excluded && (candidate instanceof Monster || candidate == this.getTarget())
						&& candidate.distanceToSqr(this) <= range * range && this.canAttack(candidate)
						&& this.hasLineOfSight(candidate)).stream()
				.sorted(Comparator.comparingDouble(this::distanceToSqr)).toList();
	}

	private void tickMovement(ServerLevel level, LivingEntity owner) {
		if (action() == ACTION_BACKSTEP) return;
		LivingEntity target = this.getTarget();
		if (target != null) {
			double distance = this.distanceTo(target);
			boolean hasSight = this.hasLineOfSight(target);
			boolean kiting = skillEnabled(SKILL_CHARIOT_VOLLEY) && distance < CLOSE_THREAT_TRIGGER_RANGE;
			if (skillEnabled(SKILL_CHARIOT_VOLLEY)) {
				if (kiting && (!this.combatKiting || (this.tickCount & 3) == 0 || this.getNavigation().isDone())) {
					this.combatApproaching = false;
					Vec3 away = DefaultRandomPos.getPosAway(this, 10, 5, target.position());
					if (away != null) this.getNavigation().moveTo(away.x, away.y, away.z, 1.05);
					else this.getNavigation().stop();
				} else if (!kiting) tickCombatApproach(target, distance, hasSight);
			} else tickCombatApproach(target, distance, hasSight);
			this.combatKiting = kiting;
			if (kiting && this.horizontalCollision && this.onGround()) {
				this.getJumpControl().jump();
			}
			return;
		}
		this.combatApproaching = false;
		this.combatKiting = false;
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
		else this.getNavigation().stop();
	}

	private @Nullable LivingEntity selectProtectiveTarget(LivingEntity owner) {
		LivingEntity current = this.getTarget();
		current = clearExpiredEmergencyTarget(current);
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
			this.highThreatTarget = null;
			LivingEntity resumeTarget = this.resumeTargetAfterThreat;
			this.resumeTargetAfterThreat = null;
			if (canProtectAgainst(resumeTarget)) return resumeTarget;
			if (current == oldHighThreat) current = null;
		}
		if (canProtectAgainst(current)) return current;
		LivingEntity ownAttacker = this.getLastHurtByMob();
		if (isRecent(this, this.getLastHurtByMobTimestamp()) && canProtectAgainst(ownAttacker)) return ownAttacker;
		if (this.alertMode == EchoRelicState.AlertMode.PEACEFUL) return null;
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isRecent(owner, owner.getLastHurtByMobTimestamp()) && canProtectAgainst(ownerAttacker)) return ownerAttacker;
		LivingEntity ownerTarget = owner.getLastHurtMob();
		if (isRecent(owner, owner.getLastHurtMobTimestamp()) && canProtectAgainst(ownerTarget)) return ownerTarget;
		if (this.alertMode != EchoRelicState.AlertMode.AGGRESSIVE) return null;
		double range = this.activityMode == EchoRelicState.ActivityMode.WAIT ? 6.0 : MAX_RANGE;
		return this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(range), this::canProtectAgainst)
				.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
	}

	private @Nullable LivingEntity clearExpiredEmergencyTarget(@Nullable LivingEntity current) {
		if (this.emergencyTarget == null) return current;
		LivingEntity oldEmergency = this.emergencyTarget;
		boolean active = this.level().getGameTime() < this.emergencyTargetUntil
				&& canProtectAgainst(oldEmergency)
				&& this.distanceToSqr(oldEmergency) <= MAX_RANGE * MAX_RANGE;
		if (active) return current;
		this.emergencyTarget = null;
		this.emergencyTargetUntil = 0L;
		LivingEntity resumeTarget = this.resumeTargetAfterEmergency;
		this.resumeTargetAfterEmergency = null;
		if (this.resumeTargetAfterThreat == oldEmergency) {
			this.resumeTargetAfterThreat = canProtectAgainst(resumeTarget) ? resumeTarget : null;
		}
		return current == oldEmergency ? canProtectAgainst(resumeTarget) ? resumeTarget : null : current;
	}

	private @Nullable Monster selectCloseThreat(LivingEntity owner, @Nullable LivingEntity current,
			boolean allowLingeringThreat) {
		boolean alreadyInCombat = canProtectAgainst(current) || this.alertMode == EchoRelicState.AlertMode.AGGRESSIVE;
		LivingEntity ownAttacker = this.getLastHurtByMob();
		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		boolean ownDamageRecent = isRecent(this, this.getLastHurtByMobTimestamp());
		boolean ownerDamageRecent = isRecent(owner, owner.getLastHurtByMobTimestamp());
		List<Monster> candidates = new ArrayList<>(this.level().getEntitiesOfClass(Monster.class,
				this.getBoundingBox().inflate(CLOSE_THREAT_TRIGGER_RANGE), candidate ->
						candidate.distanceToSqr(this) <= CLOSE_THREAT_TRIGGER_RANGE * CLOSE_THREAT_TRIGGER_RANGE
								&& canProtectAgainst(candidate)));
		if (allowLingeringThreat && this.highThreatTarget != null && canProtectAgainst(this.highThreatTarget)
				&& this.distanceToSqr(this.highThreatTarget) <= CLOSE_THREAT_RELEASE_RANGE * CLOSE_THREAT_RELEASE_RANGE
				&& !candidates.contains(this.highThreatTarget)) {
			candidates.add(this.highThreatTarget);
		}
		return candidates.stream()
				.filter(candidate -> {
					boolean recentDamager = ownDamageRecent && candidate == ownAttacker
							|| ownerDamageRecent && candidate == ownerAttacker;
					if (this.alertMode == EchoRelicState.AlertMode.PEACEFUL
							&& !(ownDamageRecent && candidate == ownAttacker)) return false;
					if (!alreadyInCombat && !recentDamager) return false;
					return !(candidate instanceof Creeper creeper && CatGodCreeperSystem.isPanicking(creeper))
							|| recentDamager;
				})
				.min(Comparator.comparingInt((Monster candidate) -> closeThreatPriority(candidate, owner,
						ownAttacker, ownDamageRecent, ownerAttacker, ownerDamageRecent))
						.thenComparingDouble(this::distanceToSqr))
				.orElse(null);
	}

	private int closeThreatPriority(Monster candidate, LivingEntity owner,
			@Nullable LivingEntity ownAttacker, boolean ownDamageRecent,
			@Nullable LivingEntity ownerAttacker, boolean ownerDamageRecent) {
		if (candidate.getTarget() == this || candidate.getTarget() == owner) return 0;
		if (ownDamageRecent && candidate == ownAttacker || ownerDamageRecent && candidate == ownerAttacker) return 1;
		return 2;
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

	private void enforceActivityBoundary(LivingEntity owner) {
		LivingEntity target = this.getTarget();
		if (target == null) return;
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
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt) {
			if (attacker instanceof LivingEntity livingAttacker && canProtectAgainst(livingAttacker)
					&& this.distanceToSqr(livingAttacker) <= MAX_RANGE * MAX_RANGE) {
				if (this.emergencyTarget == null && livingAttacker != this.getTarget()
						&& canProtectAgainst(this.getTarget())) {
					this.resumeTargetAfterEmergency = this.getTarget();
				}
				LivingEntity previousEmergency = this.emergencyTarget;
				if (previousEmergency != null && previousEmergency != livingAttacker
						&& this.resumeTargetAfterThreat == previousEmergency) {
					this.resumeTargetAfterThreat = livingAttacker;
				}
				this.emergencyTarget = livingAttacker;
				this.emergencyTargetUntil = level.getGameTime() + EMERGENCY_TARGET_TICKS;
				LivingEntity current = this.getTarget();
				if (current == null || this.distanceToSqr(current) > CLOSE_THREAT_TRIGGER_RANGE * CLOSE_THREAT_TRIGGER_RANGE) {
					this.setTarget(livingAttacker);
				}
			}
			if (action() == ACTION_IDLE) this.triggerAnim(ACTION_CONTROLLER, HURT_TRIGGER);
		}
		return hurt;
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
		this.entityData.set(ACTION, ACTION_IDLE);
		this.actionTarget = null;
		this.shotReleased = false;
		this.combatFacingInitialized = false;
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
	@Override public boolean shouldFollowOwner() { return this.activityMode == EchoRelicState.ActivityMode.FOLLOW && action() != ACTION_BACKSTEP; }
	@Override public boolean isFollowMovementSuppressed() { return action() == ACTION_BACKSTEP; }
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
		controllers.add(new AnimationController<EgyptianArcherEchoEntity>(ACTION_CONTROLLER, 1, this::selectActionAnimation)
				.triggerableAnim(DRAW_TRIGGER, DRAW_BOW_UPPER)
				.triggerableAnim(SHOOT_TRIGGER, SHOOT_UPPER)
				.triggerableAnim(BACKSTEP_TRIGGER, BACKSTEP)
				.triggerableAnim(MELEE_TRIGGER, MELEE)
				.triggerableAnim(HURT_TRIGGER, HURT));
	}

	private PlayState selectMovementAnimation(AnimationTest<EgyptianArcherEchoEntity> test) {
		int currentTick = test.animatable().tickCount;
		if (test.isMoving() && test.animatable().action() != ACTION_BACKSTEP) {
			this.movementAnimationActive = true;
			this.movementAnimationLastMovingTick = currentTick;
		} else if (this.movementAnimationActive && currentTick - this.movementAnimationLastMovingTick >= 4) {
			this.movementAnimationActive = false;
		}
		if (this.movementAnimationActive) {
			double horizontalSpeed = test.animatable().getDeltaMovement().horizontalDistance();
			test.setControllerSpeed(Mth.clamp((float)(horizontalSpeed / 0.12), 0.65F, 1.6F));
			return test.setAndContinue(WALK);
		}
		if (test.animatable().action() == ACTION_DRAW) {
			test.setControllerSpeed(61.6F / Math.max(1, test.animatable().attackInterval()));
			return test.setAndContinue(DRAW_BOW_LOWER);
		}
		if (test.animatable().action() == ACTION_SHOOT) {
			test.setControllerSpeed(61.6F / Math.max(1, test.animatable().attackInterval()));
			return test.setAndContinue(SHOOT_LOWER);
		}
		test.setControllerSpeed(1.0F);
		return test.setAndContinue(IDLE);
	}

	private PlayState selectActionAnimation(AnimationTest<EgyptianArcherEchoEntity> test) {
		if (test.animatable().action() == ACTION_DRAW || test.animatable().action() == ACTION_SHOOT) {
			test.setControllerSpeed(61.6F / Math.max(1, test.animatable().attackInterval()));
		} else test.setControllerSpeed(1.0F);
		return PlayState.STOP;
	}

	@Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.animationCache; }

	private static float yawToward(double fromX, double fromZ, double targetX, double targetZ) {
		return (float)(Math.atan2(targetZ - fromZ, targetX - fromX) * 180.0 / Math.PI) - 90.0F;
	}
}
