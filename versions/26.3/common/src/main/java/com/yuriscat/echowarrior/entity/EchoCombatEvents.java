package com.yuriscat.echowarrior.entity;

import com.yuriscat.echowarrior.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;

public final class EchoCombatEvents {
	private static final ThreadLocal<Boolean> APPLYING_SHIELD_REDUCTION = ThreadLocal.withInitial(() -> false);

	private EchoCombatEvents() {
	}

	public static void initialize() {
	}

	public static void clearPersistentCombatEffects(LivingEntity entity) {
		entity.removeEffect(ModEffects.LEGACY_SOLDIER_FORMATION);
		entity.removeEffect(ModEffects.WEAPONS_RAISED);
		entity.removeEffect(ModEffects.SHIELDS_RAISED);
		entity.removeEffect(ModEffects.HUITZILOPOCHTLI_BLESSING);
	}

	public static boolean allowDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
		if (APPLYING_SHIELD_REDUCTION.get() || !(entity.level() instanceof ServerLevel level) || amount <= 0.0F) {
			return true;
		}
		float adjusted = modifyIncomingDamage(entity, source, amount);
		if (adjusted == amount) return true;
		APPLYING_SHIELD_REDUCTION.set(true);
		try {
			entity.hurtServer(level, source, adjusted);
		} finally {
			APPLYING_SHIELD_REDUCTION.set(false);
		}
		return false;
	}

	public static float modifyIncomingDamage(
			LivingEntity entity,
			net.minecraft.world.damagesource.DamageSource source,
			float amount
	) {
		if (!(entity.level() instanceof ServerLevel level) || amount <= 0.0F
				|| !entity.hasEffect(ModEffects.SHIELDS_RAISED)) return amount;
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
				entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 3, 0.2, 0.25, 0.2, 0.0);
		return amount * 0.85F;
	}

	public static void afterDamage(
			LivingEntity victim,
			net.minecraft.world.damagesource.DamageSource source,
			float baseDamageTaken,
			float damageTaken,
			boolean blocked
	) {
		if (!(victim.level() instanceof ServerLevel level) || blocked || damageTaken <= 0.0F) return;
		if (victim instanceof Creeper creeper && source.getDirectEntity() != null) {
			CatGodCreeperSystem.onDirectlyDamaged(level, creeper);
		}
		if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() == null) return;
		if (victim instanceof AztecWarriorEchoEntity aztec) {
			aztec.tryApplyCurse(level, attacker);
			return;
		}
		for (AztecWarriorEchoEntity aztec : level.getEntitiesOfClass(
				AztecWarriorEchoEntity.class,
				victim.getBoundingBox().inflate(128.0),
				candidate -> candidate.isAlive() && victim.getUUID().equals(candidate.getOwnerUuid())
		)) {
			aztec.tryApplyCurse(level, attacker);
			break;
		}
	}
}
