package com.yuriscat.echowarrior.recycler;

import com.yuriscat.echowarrior.block.entity.RecyclerChestBlockEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class RecyclerSystem {
	private RecyclerSystem() {
	}

	public static void initialize() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
				!(blockEntity instanceof RecyclerChestBlockEntity recycler && recycler.isSealed()));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ServerLevel overworld = server.getLevel(Level.OVERWORLD);
			if (overworld != null) RecyclerClockData.get(overworld).observe(overworld.getOverworldClockTime());
		});
	}

	public static long currentMidnightSequence(ServerLevel level) {
		ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
		return overworld == null ? 0L : RecyclerClockData.get(overworld).midnightSequence();
	}
}
