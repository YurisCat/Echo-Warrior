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
import com.yuriscat.echowarrior.ModEffects;
import com.yuriscat.echowarrior.ModTags;
import com.yuriscat.echowarrior.entity.behavior.EchoFollowOwner;
import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.EchoRelicState;
import com.yuriscat.echowarrior.item.SummonerFuel;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import com.yuriscat.echowarrior.progress.EchoExperienceSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.monster.Monster;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class AztecWarriorEchoEntity extends PathfinderMob
		implements EchoWarriorEntity, SmartBrainOwner<AztecWarriorEchoEntity>, GeoEntity {
	public static final int SKILL_CURSE = 0;
	public static final int SKILL_BLESSING = 1;
	public static final int SKILL_OBSIDIAN_WOUND = 2;
	public static final int SKILL_PURSUIT = 3;
	public static final int SKILL_MACUAHUITL = 4;

	private static final EntityDataAccessor<Boolean> PURSUIT_ACTIVE = SynchedEntityData.defineId(
			AztecWarriorEchoEntity.class, EntityDataSerializers.BOOLEAN);
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.aztec_warrior.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.aztec_warrior.walk");
	private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.aztec_warrior.attack");
	private static final RawAnimation PURSUIT = RawAnimation.begin().thenPlay("animation.aztec_warrior.pursuit");
	private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.aztec_warrior.hurt");
	private static final String ACTION_CONTROLLER = "action";
	private static final Identifier FAVORED_ATTACK_ID = EchoWarrior.id("aztec_favored_attack");
	private static final Identifier FAVORED_SPEED_ID = EchoWarrior.id("aztec_favored_speed");
	private static final Identifier PURSUIT_SPEED_ID = EchoWarrior.id("pursuit_speed");
	private static final Identifier PURSUIT_KNOCKBACK_ID = EchoWarrior.id("pursuit_knockback");
	private static final AttributeModifier FAVORED_ATTACK = new AttributeModifier(
			FAVORED_ATTACK_ID, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier FAVORED_SPEED = new AttributeModifier(
			FAVORED_SPEED_ID, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier PURSUIT_SPEED = new AttributeModifier(
			PURSUIT_SPEED_ID, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
	private static final AttributeModifier PURSUIT_KNOCKBACK = new AttributeModifier(
			PURSUIT_KNOCKBACK_ID, 0.50, AttributeModifier.Operation.ADD_VALUE);
	private static final Map<LivingEntity, Long> OWNER_SUN_HEAL_TIMES = new WeakHashMap<>();

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable EntityReference<LivingEntity> ownerReference;
	private @Nullable UUID summonerUuid;
	private int missingSummonerTicks;
	private EchoRelicState.ActivityMode activityMode = EchoRelicState.ActivityMode.FOLLOW;
	private EchoRelicState.AlertMode alertMode = EchoRelicState.AlertMode.DEFENSIVE;
	private int enabledSkills = EchoHeroType.AZTEC_WARRIOR.allSkillsEnabledMask();
	private Vec3 activityAnchor = Vec3.ZERO;
	private long lastNaturalHealAt;
	private long pursuitStartedAt;
	private long pursuitBuffUntil;
	private Vec3 pursuitStart = Vec3.ZERO;
	private Vec3 pursuitLanding = Vec3.ZERO;
	private boolean pursuitImpacted;
	private int movementAnimationLastMovingTick = Integer.MIN_VALUE;
	private boolean movementAnimationActive;

	public AztecWarriorEchoEntity(EntityType<? extends AztecWarriorEchoEntity> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, EchoHeroType.AZTEC_WARRIOR.baseMaximumHealth())
				.add(Attributes.ARMOR, EchoHeroType.AZTEC_WARRIOR.baseArmor())
				.add(Attributes.ATTACK_DAMAGE, EchoHeroType.AZTEC_WARRIOR.baseAttackDamage())
				.add(Attributes.MOVEMENT_SPEED, EchoHeroType.AZTEC_WARRIOR.baseMovementSpeed())
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, EchoHeroType.AZTEC_WARRIOR.baseKnockbackResistance());
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PURSUIT_ACTIVE, false);
	}

	@Override
	protected void registerGoals() {
	}

	@Override
	public List<? extends ExtendedSensor<?>> getSensors(AztecWarriorEchoEntity owner) {
		return List.of(new NearbyLivingEntitySensor<AztecWarriorEchoEntity>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getAlwaysRunningBehaviours(AztecWarriorEchoEntity owner) {
		return List.of(new MoveToWalkTarget<>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getIdleBehaviours(AztecWarriorEchoEntity owner) {
		return List.of(new EchoFollowOwner<AztecWarriorEchoEntity>());
	}

	@Override
	public List<? extends BehaviorControl<?>> getFightingBehaviours(AztecWarriorEchoEntity owner) {
		return List.of(
				new InvalidateAttackTarget<AztecWarriorEchoEntity>(),
				new SetWalkTargetToAttackTarget<AztecWarriorEchoEntity>().speedModifier(1.05F).closeEnoughDist(1),
				new AnimatableMeleeAttack<AztecWarriorEchoEntity>(14)
						.attackInterval((entity, target) -> entity.meleeAttackInterval())
						.canAttack((entity, target) -> entity.canPerformMeleeHit(target))
						.whenStarting(entity -> entity.triggerAnim(ACTION_CONTROLLER, "attack"))
		);
	}

	private boolean canPerformMeleeHit(LivingEntity target) {
		return !isPursuing() && target.isAlive() && this.canAttack(target)
				&& this.hasLineOfSight(target) && this.isWithinMeleeAttackRange(target);
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
			EchoRelicState.pursuitCharges(relic, level.getGameTime());
			persistCurrentRelic(relic);
			if (isPursuing()) tickPursuit(level, relic);
			else tryStartPursuit(level, relic);
			if (this.tickCount % 10 == 0) tickBlessing(level, owner, relic);
			if (this.tickCount % 20 == 0) {
				applyRelicState(relic, false);
				tickNaturalHealing(level, relic);
			}
		}
		if (this.tickCount % 5 == 0 && !isPursuing()) {
			LivingEntity target = selectProtectiveTarget(owner);
			if (target != null) BrainUtil.setTargetOfEntity(this, target);
			enforceActivityBoundary(owner);
		}
		if (this.tickCount % 20 == 0 && this.getTarget() != null) {
			EchoExperienceSystem.markParticipation(this, this.getTarget());
		}
		tickPursuitBuffVisuals(level);
	}

	private void tryStartPursuit(ServerLevel level, ItemStack relic) {
		if (!EchoRelicState.skillEnabled(relic, SKILL_PURSUIT)) return;
		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive() || !this.hasLineOfSight(target)) return;
		double horizontal = Math.sqrt(this.distanceToSqr(target) - Math.pow(target.getY() - this.getY(), 2));
		double height = target.getY() - this.getY();
		long now = level.getGameTime();
		if (horizontal < 4.0 || horizontal > 10.0 || height > 4.0 || height < -6.0
				|| now < EchoRelicState.pursuitCooldownEnd(relic)
				|| EchoRelicState.pursuitCharges(relic, now) <= 0) return;
		Vec3 landing = findSafePursuitLanding(level, target);
		if (landing == null || !arcIsClear(level, landing)) return;
		if (!EchoRelicState.consumePursuitCharge(relic, now)) return;
		EchoRelicState.setPursuitCooldownEnd(relic, now + 40L);
		persistCurrentRelic(relic);
		this.pursuitStart = this.position();
		this.pursuitLanding = landing;
		this.pursuitStartedAt = now;
		this.pursuitImpacted = false;
		this.entityData.set(PURSUIT_ACTIVE, true);
		this.setNoGravity(true);
		this.setDeltaMovement(Vec3.ZERO);
		this.getNavigation().stop();
		this.triggerAnim(ACTION_CONTROLLER, "pursuit");
	}

	private void tickPursuit(ServerLevel level, ItemStack relic) {
		long elapsed = level.getGameTime() - this.pursuitStartedAt;
		this.getNavigation().stop();
		this.setDeltaMovement(Vec3.ZERO);
		if (elapsed <= 18L) {
			double t = Math.clamp(elapsed / 18.0, 0.0, 1.0);
			double arc = 2.1 * 4.0 * t * (1.0 - t);
			Vec3 point = this.pursuitStart.lerp(this.pursuitLanding, t).add(0.0, arc, 0.0);
			this.snapTo(point.x, point.y, point.z,
					yawToward(this.pursuitStart.x, this.pursuitStart.z, this.pursuitLanding.x, this.pursuitLanding.z), 0.0F);
		}
		if (elapsed >= 18L && !this.pursuitImpacted) {
			this.pursuitImpacted = true;
			this.snapTo(this.pursuitLanding.x, this.pursuitLanding.y, this.pursuitLanding.z,
					yawToward(this.pursuitStart.x, this.pursuitStart.z, this.pursuitLanding.x, this.pursuitLanding.z), 0.0F);
			pursuitImpact(level, relic);
			this.pursuitBuffUntil = level.getGameTime() + 60L;
			updatePursuitModifiers();
		}
		if (elapsed >= 24L) {
			this.entityData.set(PURSUIT_ACTIVE, false);
			this.setNoGravity(false);
		}
	}

	private void pursuitImpact(ServerLevel level, ItemStack relic) {
		float damage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.0), this::canAttack)) {
			if (target.distanceToSqr(this) > 4.0 || !this.hasLineOfSight(target)) continue;
			if (target.hurtServer(level, level.damageSources().mobAttack(this), damage)) {
				target.knockback(0.6, this.getX() - target.getX(), this.getZ() - target.getZ());
				if (EchoRelicState.skillEnabled(relic, SKILL_OBSIDIAN_WOUND) && this.random.nextFloat() < 0.15F) {
					applyObsidianWound(level, target);
				}
			}
		}
		level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.15, this.getZ(), 12, 0.65, 0.10, 0.65, 0.02);
		level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.15, this.getZ(), 8, 0.55, 0.08, 0.55, 0.02);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState()),
				this.getX(), this.getY() + 0.1, this.getZ(), 10, 0.6, 0.08, 0.6, 0.08);
		level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.35F, 1.25F);
	}

	private @Nullable Vec3 findSafePursuitLanding(ServerLevel level, LivingEntity target) {
		int[][] offsets = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}};
		BlockPos origin = target.blockPosition();
		for (int[] offset : offsets) {
			for (int dy = 2; dy >= -6; dy--) {
				BlockPos feet = origin.offset(offset[0], dy, offset[1]);
				BlockPos floor = feet.below();
				if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
						|| level.getFluidState(feet).is(FluidTags.LAVA)
						|| level.getBlockState(feet).is(BlockTags.FIRE)
						|| level.getBlockState(feet).is(Blocks.POWDER_SNOW)) continue;
				Vec3 candidate = Vec3.atBottomCenterOf(feet);
				AABB moved = this.getBoundingBox().move(candidate.subtract(this.position()));
				if (level.noCollision(this, moved)) return candidate;
			}
		}
		return null;
	}

	private boolean arcIsClear(ServerLevel level, Vec3 landing) {
		HitResult hit = level.clip(new ClipContext(this.getEyePosition(), landing.add(0.0, 1.0, 0.0),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		if (hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceTo(landing.add(0.0, 1.0, 0.0)) > 1.5) return false;
		for (int step = 1; step < 8; step++) {
			double t = step / 8.0;
			Vec3 point = this.position().lerp(landing, t).add(0.0, 2.1 * 4.0 * t * (1.0 - t), 0.0);
			AABB moved = this.getBoundingBox().move(point.subtract(this.position()));
			if (!level.noCollision(this, moved)) return false;
		}
		return true;
	}

	private void tickBlessing(ServerLevel level, LivingEntity owner, ItemStack relic) {
		boolean enabled = EchoRelicState.skillEnabled(relic, SKILL_BLESSING);
		updateFavoredBiomeModifiers(enabled && level.getBiome(this.blockPosition()).is(ModTags.AZTEC_FAVORED_BIOMES));
		if (!enabled) return;
		applySunBlessingIfEligible(level, this, false);
		applySunBlessingIfEligible(level, owner, true);
	}

	private void applySunBlessingIfEligible(ServerLevel level, LivingEntity beneficiary, boolean owner) {
		long timeOfDay = Math.floorMod(level.getOverworldClockTime(), 24_000L);
		boolean eligible = level.dimensionType().hasSkyLight() && timeOfDay < 12_000L
				&& level.canSeeSky(beneficiary.blockPosition());
		if (!eligible) return;
		beneficiary.addEffect(new MobEffectInstance(ModEffects.HUITZILOPOCHTLI_BLESSING, 25, 0, false, true, true));
		long now = level.getGameTime();
		if (owner) {
			long last = OWNER_SUN_HEAL_TIMES.getOrDefault(beneficiary, Long.MIN_VALUE / 2);
			if (now - last >= 80L && beneficiary.getHealth() < beneficiary.getMaxHealth()) {
				beneficiary.heal(1.0F);
				OWNER_SUN_HEAL_TIMES.put(beneficiary, now);
			}
		} else if (this.tickCount % 80 == 0 && this.getHealth() < this.getMaxHealth()) {
			this.heal(1.0F);
		}
	}

	private void updateFavoredBiomeModifiers(boolean active) {
		var attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
		var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (attack != null) {
			attack.removeModifier(FAVORED_ATTACK_ID);
			if (active) attack.addTransientModifier(FAVORED_ATTACK);
		}
		if (speed != null) {
			speed.removeModifier(FAVORED_SPEED_ID);
			if (active) speed.addTransientModifier(FAVORED_SPEED);
		}
	}

	private void updatePursuitModifiers() {
		boolean active = this.level().getGameTime() < this.pursuitBuffUntil;
		var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
		var knockback = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (speed != null) {
			speed.removeModifier(PURSUIT_SPEED_ID);
			if (active) speed.addTransientModifier(PURSUIT_SPEED);
		}
		if (knockback != null) {
			knockback.removeModifier(PURSUIT_KNOCKBACK_ID);
			if (active) knockback.addTransientModifier(PURSUIT_KNOCKBACK);
		}
	}

	private void tickPursuitBuffVisuals(ServerLevel level) {
		updatePursuitModifiers();
		if (level.getGameTime() < this.pursuitBuffUntil && this.tickCount % 4 == 0) {
			level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.4 + this.random.nextDouble() * 1.1,
					this.getZ(), 1, 0.22, 0.10, 0.22, 0.0);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity entity) {
		boolean hit = super.doHurtTarget(level, entity);
		if (!hit || !(entity instanceof LivingEntity mainTarget)) return hit;
		ItemStack relic = currentRelic();
		if (!relic.isEmpty() && EchoRelicState.skillEnabled(relic, SKILL_OBSIDIAN_WOUND) && this.random.nextFloat() < 0.30F) {
			applyObsidianWound(level, mainTarget);
		}
		if (!relic.isEmpty() && EchoRelicState.skillEnabled(relic, SKILL_MACUAHUITL)) {
			performMacuahuitlSweep(level, mainTarget, relic);
		}
		return true;
	}

	private void performMacuahuitlSweep(ServerLevel level, LivingEntity mainTarget, ItemStack relic) {
		boolean empowered = level.getGameTime() < this.pursuitBuffUntil;
		double radius = empowered ? 3.0 : 2.5;
		double halfAngle = empowered ? 90.0 : 80.0;
		float splashDamage = (float)(this.getAttributeValue(Attributes.ATTACK_DAMAGE) * (empowered ? 0.75 : 0.50));
		Vec3 facing = this.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius), this::canAttack)) {
			if (target == mainTarget || target.distanceToSqr(this) > radius * radius || !this.hasLineOfSight(target)) continue;
			Vec3 toward = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
			double angle = Math.toDegrees(Math.acos(Math.clamp(facing.dot(toward), -1.0, 1.0)));
			if (angle > halfAngle) continue;
			if (target.hurtServer(level, level.damageSources().mobAttack(this), splashDamage)
					&& EchoRelicState.skillEnabled(relic, SKILL_OBSIDIAN_WOUND) && this.random.nextFloat() < 0.15F) {
				applyObsidianWound(level, target);
			}
		}
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, mainTarget.getX(), mainTarget.getY() + mainTarget.getBbHeight() * 0.5,
				mainTarget.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
	}

	public void tryApplyCurse(ServerLevel level, LivingEntity attacker) {
		ItemStack relic = currentRelic();
		if (relic.isEmpty() || !EchoRelicState.skillEnabled(relic, SKILL_CURSE) || this.random.nextFloat() >= 0.60F) return;
		boolean stronger = this.random.nextFloat() < 0.25F;
		MobEffectInstance current = attacker.getEffect(MobEffects.WEAKNESS);
		if (!stronger && current != null && current.getAmplifier() >= 1) return;
		attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stronger ? 60 : 100, stronger ? 1 : 0));
		level.sendParticles(ParticleTypes.WITCH, attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.7,
				attacker.getZ(), 5, 0.25, 0.35, 0.25, 0.01);
	}

	public static void applyObsidianWound(ServerLevel level, LivingEntity target) {
		target.addEffect(new MobEffectInstance(ModEffects.OBSIDIAN_WOUND, 120, 0, false, true, true));
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
				target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 5, 0.25, 0.3, 0.25, 0.04);
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState()),
				target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 3, 0.25, 0.3, 0.25, 0.04);
		level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.35F, 0.75F);
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
		float adjusted = level.getGameTime() < this.pursuitBuffUntil ? damage * 0.80F : damage;
		boolean hurt = super.hurtServer(level, source, adjusted);
		if (hurt && adjusted > 0.0F) this.triggerAnim(ACTION_CONTROLLER, "hurt");
		return hurt;
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
		if (this.activityMode == EchoRelicState.ActivityMode.FOLLOW || target != null) return;
		double idleRadius = this.activityMode == EchoRelicState.ActivityMode.WAIT ? 2.0 : 16.0;
		if (this.position().distanceToSqr(this.activityAnchor) > idleRadius * idleRadius) {
			this.getNavigation().moveTo(this.activityAnchor.x, this.activityAnchor.y, this.activityAnchor.z, 1.0);
		} else if (this.activityMode == EchoRelicState.ActivityMode.WANDER && this.tickCount % 60 == 0 && this.random.nextInt(3) == 0) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double distance = 3.0 + this.random.nextDouble() * 10.0;
			this.getNavigation().moveTo(this.activityAnchor.x + Math.cos(angle) * distance,
					this.activityAnchor.y, this.activityAnchor.z + Math.sin(angle) * distance, 0.8);
		}
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
		if (resetAnchor || this.activityAnchor == Vec3.ZERO) this.activityAnchor = this.position();
		double oldMaximum = this.getMaxHealth();
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(EchoRelicState.maximumHealth(relic));
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(EchoRelicState.attackDamage(relic));
		this.getAttribute(Attributes.ARMOR).setBaseValue(EchoRelicState.armor(relic));
		this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(EchoRelicState.movementSpeed(relic));
		this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(EchoRelicState.knockbackResistance(relic));
		if (this.getHealth() >= oldMaximum - 0.01F) this.setHealth(this.getMaxHealth());
		else if (this.getHealth() > this.getMaxHealth()) this.setHealth(this.getMaxHealth());
		if (!EchoRelicState.skillEnabled(relic, SKILL_BLESSING)) updateFavoredBiomeModifiers(false);
	}

	private int meleeAttackInterval() {
		ItemStack relic = currentRelic();
		return relic.isEmpty() ? EchoHeroType.AZTEC_WARRIOR.baseAttackIntervalTicks() : EchoRelicState.attackIntervalTicks(relic);
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
	@Override public EchoHeroType heroType() { return EchoHeroType.AZTEC_WARRIOR; }
	@Override public boolean shouldFollowOwner() { return this.activityMode == EchoRelicState.ActivityMode.FOLLOW && !isPursuing(); }
	@Override public @Nullable UUID getOwnerUuid() { LivingEntity owner = getOwner(); return owner == null ? null : owner.getUUID(); }
	@Override public @Nullable UUID getSummonerUuid() { return this.summonerUuid; }
	@Override public @Nullable EntityReference<LivingEntity> getOwnerReference() { return this.ownerReference; }

	public boolean isPursuing() {
		return this.entityData.get(PURSUIT_ACTIVE);
	}

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

	@Override
	public void onRemoval(Entity.RemovalReason reason) {
		updateFavoredBiomeModifiers(false);
		this.pursuitBuffUntil = 0L;
		updatePursuitModifiers();
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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.ownerReference = EntityReference.readWithOldOwnerConversion(input, "EchoOwner", this.level());
		try { this.summonerUuid = UUID.fromString(input.getStringOr("SummonerUuid", "")); }
		catch (IllegalArgumentException ignored) { this.summonerUuid = null; }
		this.activityMode = EchoRelicState.ActivityMode.byOrdinal(input.getIntOr("ActivityMode", 0));
		this.alertMode = EchoRelicState.AlertMode.byOrdinal(input.getIntOr("AlertMode", 1));
		this.enabledSkills = input.getIntOr("EnabledSkills", EchoHeroType.AZTEC_WARRIOR.allSkillsEnabledMask());
		this.activityAnchor = new Vec3(input.getDoubleOr("ActivityAnchorX", this.getX()),
				input.getDoubleOr("ActivityAnchorY", this.getY()), input.getDoubleOr("ActivityAnchorZ", this.getZ()));
	}

	@Override protected boolean shouldDropLoot(ServerLevel level) { return false; }

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<AztecWarriorEchoEntity>("movement", 3, this::selectMovementAnimation));
		controllers.add(new AnimationController<AztecWarriorEchoEntity>(ACTION_CONTROLLER, 1, test -> PlayState.STOP)
				.triggerableAnim("attack", ATTACK)
				.triggerableAnim("pursuit", PURSUIT)
				.triggerableAnim("hurt", HURT));
	}

	private PlayState selectMovementAnimation(AnimationTest<AztecWarriorEchoEntity> test) {
		int currentTick = test.animatable().tickCount;
		if (test.isMoving() && !test.animatable().isPursuing()) {
			this.movementAnimationActive = true;
			this.movementAnimationLastMovingTick = currentTick;
		} else if (this.movementAnimationActive && currentTick - this.movementAnimationLastMovingTick >= 4) {
			this.movementAnimationActive = false;
		}
		return test.setAndContinue(this.movementAnimationActive ? WALK : IDLE);
	}

	@Override public AnimatableInstanceCache getAnimatableInstanceCache() { return this.animationCache; }

	private static float yawToward(double fromX, double fromZ, double targetX, double targetZ) {
		return (float)(Math.atan2(targetZ - fromZ, targetX - fromX) * 180.0 / Math.PI) - 90.0F;
	}
}
