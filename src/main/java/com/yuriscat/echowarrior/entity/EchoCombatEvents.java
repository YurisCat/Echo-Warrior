package com.yuriscat.echowarrior.entity;

import com.yuriscat.echowarrior.ModEffects;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class EchoCombatEvents {
	private static final ThreadLocal<Boolean> APPLYING_SHIELD_REDUCTION = ThreadLocal.withInitial(() -> false);

	private EchoCombatEvents() {
	}

	public static void initialize() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(EchoCombatEvents::allowDamage);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> clearFormationEffects(handler.getPlayer()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> clearFormationEffects(handler.getPlayer()));
	}

	private static void clearFormationEffects(LivingEntity entity) {
		entity.removeEffect(ModEffects.LEGACY_SOLDIER_FORMATION);
		entity.removeEffect(ModEffects.WEAPONS_RAISED);
		entity.removeEffect(ModEffects.SHIELDS_RAISED);
	}

	private static boolean allowDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
		if (APPLYING_SHIELD_REDUCTION.get() || !(entity.level() instanceof ServerLevel level) || amount <= 0.0F) {
			return true;
		}
		if (!entity.hasEffect(ModEffects.SHIELDS_RAISED)) {
			return true;
		}
		APPLYING_SHIELD_REDUCTION.set(true);
		try {
			entity.hurtServer(level, source, amount * 0.85F);
		} finally {
			APPLYING_SHIELD_REDUCTION.set(false);
		}
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
				entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 3, 0.2, 0.25, 0.2, 0.0);
		return false;
	}
}
