package com.yuriscat.echowarrior.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.FollowOwner;
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
	public static final int MAX_LIFETIME_TICKS = 20 * 120;
	public static final int SUMMONER_GRACE_TICKS = 20 * 5;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.roman_legionary.idle");
	private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.roman_legionary.walk");

	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private @Nullable EntityReference<LivingEntity> ownerReference;
	private @Nullable UUID summonerUuid;
	private int remainingLifetime = MAX_LIFETIME_TICKS;
	private int missingSummonerTicks;

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
		return List.of(new FollowOwner<RomanLegionaryEchoEntity>()
				.speedModifier(1.1F)
				.closeEnoughDist(3.0F)
				.startFollowingAfter(5.0F)
				.teleportAfterDist(16.0F)
				.noTimeout());
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
		return super.hurtServer(level, source, damage);
	}

	public void bindTo(Player owner, UUID summonerUuid) {
		this.ownerReference = EntityReference.of(owner);
		this.summonerUuid = summonerUuid;
		this.remainingLifetime = MAX_LIFETIME_TICKS;
		this.missingSummonerTicks = 0;
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
		this.snapTo(player.getX() + side.x, player.getY(), player.getZ() + side.z, player.getYRot(), 0.0F);
		this.getNavigation().stop();
		if (this.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.25, 0.5, 0.25, 0.01);
			serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.45F, 1.45F);
		}
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
