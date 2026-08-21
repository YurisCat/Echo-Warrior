package com.yuriscat.echowarrior.entity;

import com.yuriscat.echowarrior.ModDamageTypes;
import com.yuriscat.echowarrior.ModEffects;
import com.yuriscat.echowarrior.item.EchoRelicState;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class EgyptianArcherArrowEntity extends Arrow {
	private static final EntityDataAccessor<Integer> ARROW_MODE = SynchedEntityData.defineId(
			EgyptianArcherArrowEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> RAW_DAMAGE = SynchedEntityData.defineId(
			EgyptianArcherArrowEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> PIERCE_ON_HIT = SynchedEntityData.defineId(
			EgyptianArcherArrowEntity.class, EntityDataSerializers.BOOLEAN);

	private final Set<UUID> hitEntities = new HashSet<>();

	public EgyptianArcherArrowEntity(EntityType<? extends EgyptianArcherArrowEntity> type, Level level) {
		super(type, level);
		this.pickup = Pickup.DISALLOWED;
	}

	public void configure(LivingEntity owner, EchoRelicState.EgyptianArrowMode mode, float damage, boolean pierceOnHit) {
		this.setOwner(owner);
		this.entityData.set(ARROW_MODE, mode.ordinal());
		this.entityData.set(RAW_DAMAGE, damage);
		this.entityData.set(PIERCE_ON_HIT, pierceOnHit);
		this.pickup = Pickup.DISALLOWED;
	}

	public EchoRelicState.EgyptianArrowMode arrowMode() {
		return EchoRelicState.EgyptianArrowMode.byOrdinal(this.entityData.get(ARROW_MODE));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ARROW_MODE, EchoRelicState.EgyptianArrowMode.OFF.ordinal());
		builder.define(RAW_DAMAGE, 5.0F);
		builder.define(PIERCE_ON_HIT, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			int color = switch (arrowMode()) {
				case LEAF -> 0xE6C84E;
				case CONE -> 0x17120F;
				case OFF -> -1;
			};
			if (color >= 0 && !this.isInGround()) {
				this.level().addParticle(new DustParticleOptions(color, 0.72F),
						this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
			}
		} else if (this.isInGround() && this.inGroundTime > 40) {
			this.discard();
		}
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		if (this.hitEntities.contains(entity.getUUID())) return false;
		if (entity instanceof LivingEntity living && this.getOwner() instanceof EgyptianArcherEchoEntity owner
				&& !owner.canAttack(living)) return false;
		return super.canHitEntity(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		if (!(this.level() instanceof ServerLevel level) || !(hitResult.getEntity() instanceof LivingEntity target)) {
			this.discard();
			return;
		}
		Entity ownerEntity = this.getOwner();
		if (ownerEntity instanceof EgyptianArcherEchoEntity owner && !owner.canAttack(target)) return;
		this.hitEntities.add(target.getUUID());
		float damage = this.entityData.get(RAW_DAMAGE);
		if (target.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) damage *= 1.20F;
		DamageSource normalArrowSource = level.damageSources().arrow(this, ownerEntity == null ? this : ownerEntity);
		DamageSource actualSource = normalArrowSource;
		if (arrowMode() == EchoRelicState.EgyptianArrowMode.CONE) {
			float reducedArmor = target.getArmorValue() * 0.65F;
			damage = CombatRules.getDamageAfterAbsorb(target, damage, normalArrowSource, reducedArmor,
					(float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
			actualSource = level.damageSources().source(ModDamageTypes.ARMOR_PIERCING_ARROW, this,
					ownerEntity == null ? this : ownerEntity);
		}
		boolean damaged = target.hurtServer(level, actualSource, damage);
		if (damaged) {
			if (ownerEntity instanceof LivingEntity owner) owner.setLastHurtMob(target);
			if (arrowMode() == EchoRelicState.EgyptianArrowMode.LEAF) {
				target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 80, 0, false, true, true));
				target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, true, true));
			}
		}
		this.playSound(SoundEvents.ARROW_HIT, 1.0F, 1.0F);
		boolean continuePiercing = arrowMode() == EchoRelicState.EgyptianArrowMode.CONE
				&& this.entityData.get(PIERCE_ON_HIT) && this.hitEntities.size() < 2;
		if (!continuePiercing) this.discard();
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return ItemStack.EMPTY;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("ArrowMode", this.entityData.get(ARROW_MODE));
		output.putFloat("RawDamage", this.entityData.get(RAW_DAMAGE));
		output.putBoolean("PierceOnHit", this.entityData.get(PIERCE_ON_HIT));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(ARROW_MODE, input.getIntOr("ArrowMode", 0));
		this.entityData.set(RAW_DAMAGE, input.getFloatOr("RawDamage", 5.0F));
		this.entityData.set(PIERCE_ON_HIT, input.getBooleanOr("PierceOnHit", false));
		this.pickup = Pickup.DISALLOWED;
	}
}
