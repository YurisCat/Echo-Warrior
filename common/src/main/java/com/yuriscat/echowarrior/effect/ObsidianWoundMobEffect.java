package com.yuriscat.echowarrior.effect;

import com.yuriscat.echowarrior.ModDamageTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

public final class ObsidianWoundMobEffect extends MobEffect {
	public ObsidianWoundMobEffect() {
		super(MobEffectCategory.HARMFUL, 0x5B1116);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return duration % 20 == 0;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		boolean damaged = entity.hurtServer(level, level.damageSources().source(ModDamageTypes.OBSIDIAN_WOUND), 1.0F);
		level.sendParticles(
				new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
				entity.getX(), entity.getY() + entity.getBbHeight() * 0.65, entity.getZ(),
				2, entity.getBbWidth() * 0.2, entity.getBbHeight() * 0.12, entity.getBbWidth() * 0.2, 0.01
		);
		return damaged;
	}
}
