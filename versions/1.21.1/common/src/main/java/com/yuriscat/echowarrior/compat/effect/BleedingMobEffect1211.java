package com.yuriscat.echowarrior.compat.effect;

import com.yuriscat.echowarrior.compat.ModDamageTypes1211;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

public final class BleedingMobEffect1211 extends MobEffect {
    private static final DustParticleOptions PARTICLE = new DustParticleOptions(new Vector3f(0.65F, 0.13F, 0.13F), 0.8F);

    public BleedingMobEffect1211() { super(MobEffectCategory.HARMFUL, 0xA52222); }

    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 40 == 0; }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        boolean damaged = entity.hurt(ModDamageTypes1211.source(level, ModDamageTypes1211.BLEEDING), 1.0F);
        level.sendParticles(PARTICLE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ(),
                4, entity.getBbWidth() * 0.25, entity.getBbHeight() * 0.18, entity.getBbWidth() * 0.25, 0.0);
        return damaged;
    }
}
