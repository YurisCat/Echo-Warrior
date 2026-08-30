package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.block.entity.RecyclerChestBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<RecyclerChestBlockEntity> RECYCLER_CHEST = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			EchoWarrior.id("recycler_chest"),
			FabricBlockEntityTypeBuilder.create(RecyclerChestBlockEntity::new, ModBlocks.ECHO_RECYCLER).build()
	);

	private ModBlockEntities() {
	}

	public static void initialize() {
	}
}
