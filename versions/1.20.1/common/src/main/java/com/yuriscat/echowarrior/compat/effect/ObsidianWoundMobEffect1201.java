package com.yuriscat.echowarrior.compat.effect;

import com.yuriscat.echowarrior.compat.ModDamageTypes1201;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

public final class ObsidianWoundMobEffect1201 extends MobEffect {
    public ObsidianWoundMobEffect1201() { super(MobEffectCategory.HARMFUL, 0x5B1116); }

    @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 20 == 0; }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        boolean damaged = entity.hurt(ModDamageTypes1201.source(level, ModDamageTypes1201.OBSIDIAN_WOUND), 1.0F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ(),
                2, entity.getBbWidth() * 0.2, entity.getBbHeight() * 0.12, entity.getBbWidth() * 0.2, 0.01);

    }
}
