package com.yuriscat.echowarrior.block;

import com.mojang.serialization.MapCodec;
import com.yuriscat.echowarrior.ModBlockEntities;
import com.yuriscat.echowarrior.block.entity.RecyclerChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

public final class RecyclerChestBlock extends ChestBlock {
	public static final MapCodec<RecyclerChestBlock> CODEC = simpleCodec(RecyclerChestBlock::new);

	public RecyclerChestBlock(BlockBehaviour.Properties properties) {
		super(() -> ModBlockEntities.RECYCLER_CHEST, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties);
	}

	@Override
	public MapCodec<RecyclerChestBlock> codec() {
		return CODEC;
	}

	@Override
	public boolean chestCanConnectTo(BlockState blockState) {
		return false;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(TYPE, ChestType.SINGLE);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
		return new RecyclerChestBlockEntity(worldPosition, blockState);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
			Level level,
			BlockState state,
			BlockEntityType<T> type
	) {
		return level.isClientSide()
				? createTickerHelper(type, ModBlockEntities.RECYCLER_CHEST, RecyclerChestBlockEntity::clientTick)
				: createTickerHelper(type, ModBlockEntities.RECYCLER_CHEST, RecyclerChestBlockEntity::serverTick);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof RecyclerChestBlockEntity recycler && recycler.isSealed()) {
			return 0.0F;
		}
		return super.getDestroyProgress(state, player, level, pos);
	}

	@Override
	protected void onExplosionHit(
			BlockState state,
			ServerLevel level,
			BlockPos pos,
			Explosion explosion,
			BiConsumer<ItemStack, BlockPos> onHit
	) {
		if (level.getBlockEntity(pos) instanceof RecyclerChestBlockEntity recycler && recycler.isSealed()) {
			return;
		}
		super.onExplosionHit(state, level, pos, explosion, onHit);
	}
}
