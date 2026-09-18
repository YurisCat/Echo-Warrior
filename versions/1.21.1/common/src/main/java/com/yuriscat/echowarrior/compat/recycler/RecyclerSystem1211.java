package com.yuriscat.echowarrior.compat.recycler;

import com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1211;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class RecyclerSystem1211 {
    private RecyclerSystem1211() { }

    public static boolean allowBreak(BlockEntity blockEntity) {
        return !(blockEntity instanceof RecyclerChestBlockEntity1211 recycler && recycler.isSealed());
    }

    public static void tick(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) RecyclerClockData1211.get(overworld).observe(overworld.getDayTime());
    }

    public static long currentMidnightSequence(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        return overworld == null ? 0L : RecyclerClockData1211.get(overworld).midnightSequence();
    }
}
