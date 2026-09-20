package com.yuriscat.echowarrior.compat.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla brushing without the falling-block behavior. */
public final class StableBrushableBlock1201 extends BrushableBlock {
    public StableBrushableBlock1201(Block turnsInto, SoundEvent brushSound,
                                    SoundEvent completedSound, BlockBehaviour.Properties properties) {
        super(turnsInto, properties, brushSound, completedSound);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable) brushable.checkReset();
    }
}
