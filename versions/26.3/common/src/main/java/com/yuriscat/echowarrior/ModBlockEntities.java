package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.block.entity.RecyclerChestBlockEntity;
import com.yuriscat.echowarrior.platform.RegistryRegistrar;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static BlockEntityType<RecyclerChestBlockEntity> RECYCLER_CHEST;

	private ModBlockEntities() {
	}

	public static void register(
			RegistryRegistrar<BlockEntityType<?>> registrar,
			BlockEntityType<RecyclerChestBlockEntity> recyclerChest
	) {
		RECYCLER_CHEST = recyclerChest;
		registrar.register(EchoWarrior.id("recycler_chest"), RECYCLER_CHEST);
	}
}
