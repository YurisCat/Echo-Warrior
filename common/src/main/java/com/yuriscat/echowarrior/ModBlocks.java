package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.block.StableBrushableBlock;
import com.yuriscat.echowarrior.block.RecyclerChestBlock;
import com.yuriscat.echowarrior.item.RecyclerChestItem;
import com.yuriscat.echowarrior.item.SuspiciousBlockItem;
import com.yuriscat.echowarrior.platform.RegistryRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModBlocks {
	private static final Map<Identifier, Block> BLOCKS = new LinkedHashMap<>();
	private static final Map<Identifier, Item> BLOCK_ITEMS = new LinkedHashMap<>();
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
	}

	public static void registerBlocks(RegistryRegistrar<Block> registrar) {
		BLOCKS.forEach(registrar::register);
	}

	public static void registerItems(RegistryRegistrar<Item> registrar) {
		BLOCK_ITEMS.forEach(registrar::register);
	}

	private static Block register(
			String path,
			java.util.function.Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties
	) {
		Identifier id = EchoWarrior.id(path);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		Block block = factory.apply(properties.setId(blockKey));
		BLOCKS.put(id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BLOCK_ITEMS.put(id, new SuspiciousBlockItem(block, new Item.Properties().setId(itemKey).stacksTo(1)));
		return block;
	}

	private static RecyclerChestBlock registerRecycler(String path, BlockBehaviour.Properties properties) {
		Identifier id = EchoWarrior.id(path);
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
		RecyclerChestBlock block = new RecyclerChestBlock(properties.setId(blockKey));
		BLOCKS.put(id, block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BLOCK_ITEMS.put(id, new RecyclerChestItem(block, new Item.Properties().setId(itemKey).stacksTo(64)));
		return block;
	}
}
