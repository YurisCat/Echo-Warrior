package com.yuriscat.echowarrior.effect;

import com.yuriscat.echowarrior.ModDamageTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class BleedingMobEffect extends MobEffect {
	public BleedingMobEffect() {
		super(MobEffectCategory.HARMFUL, 0xA52222);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		if (!com.yuriscat.echowarrior.entity.EchoBleeding.tick(level, entity, false)) return true;
		level.sendParticles(new DustParticleOptions(0xA52222, 0.8F),
				entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ(),
				4, entity.getBbWidth() * 0.25, entity.getBbHeight() * 0.18, entity.getBbWidth() * 0.25, 0.0);
		return true;
	}
}
