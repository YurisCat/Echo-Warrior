package com.yuriscat.echowarrior.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.yuriscat.echowarrior.entity.behavior.EchoFollowOwner;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetWalkTargetToAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.util.BrainUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class RomanLegionaryEchoEntity extends PathfinderMob
		implements OwnableEntity, SmartBrainOwner<RomanLegionaryEchoEntity>, GeoEntity {
	public static final byte VISUAL_NORMAL = 0;
	public static final byte VISUAL_ALERT = 1;
	public static final byte VISUAL_STARTLED = 2;
	public static final byte VISUAL_HURT = 3;
	public static final byte VISUAL_CURIOUS = 4;

	private static final EntityDataAccessor<Float> ATTENTION_X = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ATTENTION_Y = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ATTENTION_Z = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Byte> VISUAL_REACTION = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Long> VISUAL_REACTION_UNTIL = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Long> BLINK_START = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.LONG);
	private static final EntityDataAccessor<Byte> BLINK_COUNT = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> CURIOUS_TILT = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Integer> VISUAL_SEQUENCE = SynchedEntityData.defineId(RomanLegionaryEchoEntity.class, EntityDataSerializers.INT);

	public static final int MAX_LIFETIME_TICKS = 20 * 120;
	public static final int SUMMONER_GRACE_TICKS = 20 * 5;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.roman_legionary.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.roman_legionary.walk");

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable EntityReference<LivingEntity> ownerReference;
	private @Nullable UUID summonerUuid;
	private int remainingLifetime = MAX_LIFETIME_TICKS;
	private int missingSummonerTicks;
	private @Nullable LivingEntity attentionTarget;
	private Vec3 attentionPoint = Vec3.ZERO;
	private int attentionPriority;
	private long attentionStartedAt;
	private long attentionExpiresAt;
	private long nextBlinkAt;
	private long forcedVisualUntil;

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
		entityData.define(VISUAL_REACTION, VISUAL_NORMAL);
		entityData.define(VISUAL_REACTION_UNTIL, 0L);
		entityData.define(BLINK_START, -100L);
		entityData.define(BLINK_COUNT, (byte)0);
		entityData.define(CURIOUS_TILT, (byte)0);
		entityData.define(VISUAL_SEQUENCE, 0);
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
		return List.of(new LookAtTarget<>(), new MoveToWalkTarget<>());
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
				new AnimatableMeleeAttack<RomanLegionaryEchoEntity>(6).attackInterval(20).canAttack((entity, target) -> entity.canAttack(target))
		);
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

		tickVisualAwareness(serverLevel, owner);
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

		if (this.tickCount % 5 != 0) {
			return;
		}

		AttentionCandidate candidate = findBestAttentionCandidate(level, owner, now);
		boolean expired = now >= this.attentionExpiresAt || this.attentionTarget != null && !this.attentionTarget.isAlive();
		if (candidate.priority() >= this.attentionPriority + 80 || expired) {
			applyAttention(candidate, now);
		} else if (this.attentionTarget != null && this.attentionTarget.isAlive()) {
			setAttentionPoint(this.attentionTarget.getEyePosition());
		}

		byte reaction = this.entityData.get(VISUAL_REACTION);
		if ((reaction == VISUAL_STARTLED || reaction == VISUAL_HURT) && now - this.attentionStartedAt >= 8) {
			turnBodyToward(this.attentionPoint, 8.0F);
		}
	}

	private void tickBlinkClock(long now) {
		if (this.nextBlinkAt == 0L) {
			this.nextBlinkAt = now + 50 + this.random.nextInt(71);
		}
		if (now < this.nextBlinkAt || this.entityData.get(VISUAL_REACTION) == VISUAL_STARTLED || this.entityData.get(VISUAL_REACTION) == VISUAL_HURT) {
			return;
		}

		this.entityData.set(BLINK_START, now);
		this.entityData.set(BLINK_COUNT, this.random.nextFloat() < 0.1F ? (byte)2 : (byte)1);
		this.nextBlinkAt = now + 50 + this.random.nextInt(71);
	}

	private AttentionCandidate findBestAttentionCandidate(ServerLevel level, LivingEntity owner, long now) {
		LivingEntity attacker = this.getLastHurtByMob();
		if (isRecent(this, this.getLastHurtByMobTimestamp()) && isVisibleAttentionTarget(attacker)) {
			return new AttentionCandidate(attacker, attacker.getEyePosition(), 1100, VISUAL_HURT, 16, false);
		}

		LivingEntity ownerAttacker = owner.getLastHurtByMob();
		if (isRecent(owner, owner.getLastHurtByMobTimestamp()) && isVisibleAttentionTarget(ownerAttacker)) {
			return new AttentionCandidate(ownerAttacker, ownerAttacker.getEyePosition(), 1000, VISUAL_ALERT, 24, false);
		}

		LivingEntity combatTarget = this.getTarget();
		AttentionCandidate best = isVisibleAttentionTarget(combatTarget)
				? new AttentionCandidate(combatTarget, combatTarget.getEyePosition(), 800, VISUAL_ALERT, 30, false)
				: new AttentionCandidate(owner, owner.getEyePosition(), 220, VISUAL_NORMAL, 35 + this.random.nextInt(36), false);

		List<LivingEntity> nearby = level.getEntitiesOfClass(
				LivingEntity.class,
				this.getBoundingBox().inflate(12.0),
				entity -> entity != this && entity.isAlive() && !entity.isSpectator() && this.hasLineOfSight(entity)
		);
		for (LivingEntity entity : nearby) {
			double distanceSqr = this.distanceToSqr(entity);
			int score;
			byte reaction = VISUAL_NORMAL;
			int duration = 30 + this.random.nextInt(51);

			if (entity instanceof Player player && isPlayerLookingAtEcho(player) && this.random.nextFloat() < 0.85F) {
				score = 790;
				duration = 50 + this.random.nextInt(51);
			} else if (entity instanceof Creeper creeper) {
				boolean primed = creeper.getSwellDir() > 0 || creeper.isIgnited();
				score = primed ? 950 : distanceSqr <= 64.0 ? 720 : 540;
				reaction = primed || distanceSqr <= 64.0 ? VISUAL_STARTLED : VISUAL_ALERT;
				duration = primed ? 30 : 22;
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
				best = new AttentionCandidate(entity, entity.getEyePosition(), score, curious ? VISUAL_CURIOUS : reaction, duration, curious);
			}
		}

		if (best.priority() <= 220 && this.random.nextFloat() < 0.3F) {
			float yaw = (this.getYRot() + this.random.nextFloat() * 130.0F - 65.0F) * ((float)Math.PI / 180.0F);
			double distance = 4.0 + this.random.nextDouble() * 5.0;
			Vec3 point = new Vec3(
					this.getX() - Math.sin(yaw) * distance,
					this.getEyeY() + this.random.nextDouble() * 3.0 - 1.0,
					this.getZ() + Math.cos(yaw) * distance
			);
			boolean curious = this.isInSafeIdleState() && this.random.nextFloat() < 0.1F;
			return new AttentionCandidate(null, point, 230, curious ? VISUAL_CURIOUS : VISUAL_NORMAL, 30 + this.random.nextInt(51), curious);
		}

		return best;
	}

	private boolean isPlayerLookingAtEcho(Player player) {
		if (!this.hasLineOfSight(player) || player.isSpectator()) {
			return false;
		}

		Vec3 towardEcho = this.getEyePosition().subtract(player.getEyePosition());
		double distance = towardEcho.length();
		if (distance < 0.1 || distance > 12.0) {
			return false;
		}

		double gazeAlignment = player.getLookAngle().normalize().dot(towardEcho.scale(1.0 / distance));
		double requiredAlignment = distance < 4.0 ? 0.965 : 0.985;
		return gazeAlignment >= requiredAlignment;
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
		this.attentionTarget = candidate.target();
		this.attentionPoint = candidate.point();
		this.attentionPriority = candidate.priority();
		this.attentionStartedAt = now;
		this.attentionExpiresAt = now + candidate.durationTicks();
		setAttentionPoint(candidate.point());
		setReaction(candidate.reaction(), now + candidate.durationTicks());
		this.entityData.set(CURIOUS_TILT, candidate.curious() ? (byte)(this.random.nextBoolean() ? 1 : -1) : (byte)0);
		this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
	}

	private void setAttentionPoint(Vec3 point) {
		this.attentionPoint = point;
		this.entityData.set(ATTENTION_X, (float)point.x);
		this.entityData.set(ATTENTION_Y, (float)point.y);
		this.entityData.set(ATTENTION_Z, (float)point.z);
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
				setAttentionPoint(owner == null ? this.position().add(0, 1.5, 4) : owner.getEyePosition());
				setReaction(VISUAL_CURIOUS, now + 60);
				this.entityData.set(CURIOUS_TILT, (byte)(this.random.nextBoolean() ? 1 : -1));
			}
			case STARTLED -> {
				LivingEntity owner = this.getOwner();
				setAttentionPoint(owner == null ? this.position().add(0, 1.5, 4) : owner.getEyePosition());
				setReaction(VISUAL_STARTLED, now + 60);
				this.entityData.set(CURIOUS_TILT, (byte)0);
			}
			case RESET -> {
				this.forcedVisualUntil = 0L;
				this.attentionExpiresAt = 0L;
				this.attentionPriority = 0;
				setReaction(VISUAL_NORMAL, 0L);
				this.entityData.set(CURIOUS_TILT, (byte)0);
			}
		}
		this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
	}

	public Vec3 getSyncedAttentionPoint() {
		return new Vec3(this.entityData.get(ATTENTION_X), this.entityData.get(ATTENTION_Y), this.entityData.get(ATTENTION_Z));
	}

	public byte getVisualReaction() {
		return this.entityData.get(VISUAL_REACTION);
	}

	public long getVisualReactionUntil() {
		return this.entityData.get(VISUAL_REACTION_UNTIL);
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

	public enum VisualTestMode {
		BLINK,
		DOUBLE_BLINK,
		CURIOUS,
		STARTLED,
		RESET
	}

	private record AttentionCandidate(@Nullable LivingEntity target, Vec3 point, int priority, byte reaction, int durationTicks, boolean curious) {
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
			Vec3 point = attackerEntity instanceof LivingEntity living ? living.getEyePosition() : this.position().add(this.getLookAngle().reverse());
			setAttentionPoint(point);
			setReaction(VISUAL_HURT, level.getGameTime() + 16);
			this.entityData.set(CURIOUS_TILT, (byte)0);
			this.entityData.set(VISUAL_SEQUENCE, this.entityData.get(VISUAL_SEQUENCE) + 1);
		}
		return hurt;
	}

	public void bindTo(Player owner, UUID summonerUuid) {
		this.ownerReference = EntityReference.of(owner);
		this.summonerUuid = summonerUuid;
		this.remainingLifetime = MAX_LIFETIME_TICKS;
		this.missingSummonerTicks = 0;
		setAttentionPoint(owner.getEyePosition());
	}

	public @Nullable UUID getOwnerUuid() {
		LivingEntity owner = getOwner();
		return owner == null ? null : owner.getUUID();
	}

	@Override
	public @Nullable EntityReference<LivingEntity> getOwnerReference() {
		return this.ownerReference;
	}

	public void recallTo(Player player) {
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
		controllers.add(new AnimationController<>("movement", test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.animationCache;
	}
}
