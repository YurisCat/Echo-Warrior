package com.yuriscat.echowarrior.world;

import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exercises the shared terrain gate on a safe flat fixture across the negative region boundary. */
public final class BattlefieldSpacingSelfTest {
    private BattlefieldSpacingSelfTest() {}

    public static void run(ServerLevel level) {
        RedSandSelfTest.run(level);
        BlockPos center = new BlockPos(-1, 96, -1);
        Map<BlockPos, BlockState> previous = new LinkedHashMap<>();
        try {
            for (int x = -10; x <= 8; x++) {
                for (int z = -10; z <= 8; z++) {
                    for (int y = 95; y <= 100; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        require(level.getBlockEntity(pos) == null, "disposable fixture has no block entities");
                        previous.put(pos, level.getBlockState(pos));
                    }
                }
            }
            for (BlockPos pos : previous.keySet()) {
                level.setBlock(pos, (pos.getY() == 95 ? Blocks.STONE : pos.getY() == 96
                        ? Blocks.GRASS_BLOCK : Blocks.AIR).defaultBlockState(), 2);
            }
            require(safe(level, new BattlefieldSavedData(), center), "terrain is safe before adding any known site");
            BattlefieldSavedData near = withSite(level, center.offset(511, 0, 0));
            require(!safe(level, near, center), "511 blocks rejected across two different regions");
            BattlefieldSavedData boundary = withSite(level, center.offset(512, 0, 0));
            require(safe(level, boundary, center), "exactly 512 blocks remains allowed");
            EchoWarrior.LOGGER.info("BATTLEFIELD SPACING SELFTEST PASSED near=511-rejected boundary=512-allowed regions=negative-positive");
        } finally {
            previous.forEach((pos, state) -> level.setBlock(pos, state, 2));
        }
    }

    private static BattlefieldSavedData withSite(ServerLevel level, BlockPos center) {
        BattlefieldSavedData data = new BattlefieldSavedData();
        var region = data.prepareForcedRegion(level, new ChunkPos(Math.floorDiv(center.getX(), 16), Math.floorDiv(center.getZ(), 16)));
        data.activate(region.key(), center, center, "roman", List.of(center));
        return data;
    }

    private static boolean safe(ServerLevel level, BattlefieldSavedData data, BlockPos center) {
        try {
            Method method = BattlefieldSystem.class.getDeclaredMethod("isSafeSite", ServerLevel.class,
                    BattlefieldSavedData.class, BlockPos.class, int.class);
            method.setAccessible(true);
            return (boolean)method.invoke(null, level, data, center, 7);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot exercise shared battlefield safety gate", error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Battlefield spacing: " + message);
    }
}
