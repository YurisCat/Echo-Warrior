package com.yuriscat.echowarrior.recycler;

import com.yuriscat.echowarrior.block.entity.RecyclerChestBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class RecyclerSystem {
	private RecyclerSystem() {
	}

	public static void initialize() {
	}

	public static boolean allowBreak(BlockEntity blockEntity) {
		return !(blockEntity instanceof RecyclerChestBlockEntity recycler && recycler.isSealed());
	}

	public static void tick(MinecraftServer server) {
		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld != null) RecyclerClockData.get(overworld).observe(overworld.getOverworldClockTime());
	}

	public static long currentMidnightSequence(ServerLevel level) {
		ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
		return overworld == null ? 0L : RecyclerClockData.get(overworld).midnightSequence();
	}
}
