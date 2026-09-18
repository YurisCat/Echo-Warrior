package com.yuriscat.echowarrior.compat.block;

import com.mojang.serialization.MapCodec;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1211;
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

public final class RecyclerChestBlock1211 extends ChestBlock {
    public static final MapCodec<RecyclerChestBlock1211> CODEC = simpleCodec(RecyclerChestBlock1211::new);

    public RecyclerChestBlock1211(BlockBehaviour.Properties properties) {
        super(properties, () -> ModContent1211.RECYCLER_CHEST);
    }

    @Override public MapCodec<? extends ChestBlock> codec() { return CODEC; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(TYPE, ChestType.SINGLE);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos).setValue(TYPE, ChestType.SINGLE);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RecyclerChestBlockEntity1211(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? createTickerHelper(type, ModContent1211.RECYCLER_CHEST, RecyclerChestBlockEntity1211::clientTick)
                : createTickerHelper(type, ModContent1211.RECYCLER_CHEST, RecyclerChestBlockEntity1211::serverTick);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RecyclerChestBlockEntity1211 recycler && recycler.isSealed()) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected void onExplosionHit(BlockState state, Level level, BlockPos pos, Explosion explosion,
                                  BiConsumer<ItemStack, BlockPos> onHit) {
        if (level.getBlockEntity(pos) instanceof RecyclerChestBlockEntity1211 recycler && recycler.isSealed()) return;
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }
}
