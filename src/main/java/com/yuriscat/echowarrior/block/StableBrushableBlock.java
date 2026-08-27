package com.yuriscat.echowarrior.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** A brushable surface block that keeps the vanilla brushing behavior without gravity. */
public final class StableBrushableBlock extends BrushableBlock {
	public StableBrushableBlock(
			Block turnsInto,
			SoundEvent brushSound,
			SoundEvent brushCompletedSound,
			BlockBehaviour.Properties properties
	) {
		super(turnsInto, brushSound, brushCompletedSound, properties);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable) {
			brushable.checkReset(level);
		}
	}
}
