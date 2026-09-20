package com.yuriscat.echowarrior.compat.block;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1201;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public final class RecyclerChestBlock1201 extends ChestBlock {

    public RecyclerChestBlock1201(BlockBehaviour.Properties properties) {
        super(properties, () -> ModContent1201.RECYCLER_CHEST);
    }


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(TYPE, ChestType.SINGLE);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos).setValue(TYPE, ChestType.SINGLE);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RecyclerChestBlockEntity1201(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? createTickerHelper(type, ModContent1201.RECYCLER_CHEST, RecyclerChestBlockEntity1201::clientTick)
                : createTickerHelper(type, ModContent1201.RECYCLER_CHEST, RecyclerChestBlockEntity1201::serverTick);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RecyclerChestBlockEntity1201 recycler && recycler.isSealed()) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

}
