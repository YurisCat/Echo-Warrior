package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.block.StableBrushableBlock;
import com.yuriscat.echowarrior.block.RecyclerChestBlock;
import com.yuriscat.echowarrior.item.RecyclerChestItem;
import com.yuriscat.echowarrior.item.SuspiciousBlockItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
	public static final Block SUSPICIOUS_GRASS_BLOCK = register(
			"suspicious_grass_block",
			properties -> new StableBrushableBlock(
					Blocks.GRASS_BLOCK,
					SoundEvents.BRUSH_GRAVEL,
					SoundEvents.BRUSH_GRAVEL,
					properties
			),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.GRASS)
					.strength(0.5F)
					.sound(SoundType.GRASS)
					.noLootTable()
					.pushReaction(PushReaction.DESTROY)
	);
	public static final Block SUSPICIOUS_DIRT = register(
			"suspicious_dirt",
			properties -> new StableBrushableBlock(
					Blocks.DIRT,
					SoundEvents.BRUSH_GRAVEL,
					SoundEvents.BRUSH_GRAVEL,
					properties
			),
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.DIRT)
					.strength(0.5F)
					.sound(SoundType.ROOTED_DIRT)
					.noLootTable()
					.pushReaction(PushReaction.DESTROY)
	);
	public static final RecyclerChestBlock ECHO_RECYCLER = registerRecycler(
			"echo_recycler",
			BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST)
					.pushReaction(PushReaction.BLOCK)
	);

	private ModBlocks() {
	}

	public static void initialize() {
		BlockEntityType.BRUSHABLE_BLOCK.addValidBlock(SUSPICIOUS_GRASS_BLOCK);
		BlockEntityType.BRUSHABLE_BLOCK.addValidBlock(SUSPICIOUS_DIRT);
	}

	private static Block register(
			String path,
			java.util.function.Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties
	) {
		Identifier id = EchoWarrior.id(path);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = Registry.register(BuiltInRegistries.BLOCK, id, factory.apply(properties.setId(blockKey)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		Registry.register(BuiltInRegistries.ITEM, id,
				new SuspiciousBlockItem(block, new Item.Properties().setId(itemKey).stacksTo(1)));
		return block;
	}

	private static RecyclerChestBlock registerRecycler(String path, BlockBehaviour.Properties properties) {
		Identifier id = EchoWarrior.id(path);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		RecyclerChestBlock block = Registry.register(
				BuiltInRegistries.BLOCK,
				id,
				new RecyclerChestBlock(properties.setId(blockKey))
		);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		Registry.register(
				BuiltInRegistries.ITEM,
				id,
				new RecyclerChestItem(block, new Item.Properties().setId(itemKey).stacksTo(64))
		);
		return block;
	}
}
