package com.yuriscat.echowarrior.compat.world;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BrushableBlock;

import java.util.ArrayList;
import java.util.List;

/** Explicit test-server-only acceptance across real ticks and vanilla chunk generation/unloading. */
public final class BattlefieldGenerationSelfTest1201 {
    private static boolean armed;
    private static ServerLevel level;
    private static BattlefieldSavedData1201 original;
    private static BattlefieldSavedData1201 fixture;
    private static long started;
    private static int stage;
    private static final BlockPos ORIGIN = new BlockPos(7680000, 64, 7680000);
    private static final List<ChunkPos> generatedChunks = new ArrayList<>();

    private BattlefieldGenerationSelfTest1201() {}

    public static void arm() {
        if (Boolean.getBoolean("echo_warrior.battlefield_generation_test")) armed = true;
    }

    public static void tick(MinecraftServer server) {
        if (!armed) return;
        try {
            if (level == null) {
                level = server.overworld();
                original = BattlefieldSavedData1201.get(level);
                BattlefieldSystem1201.clear();
                fixture = new BattlefieldSavedData1201();
                level.getDataStorage().set("echo_warrior_battlefield_sites", fixture);
                queue(ORIGIN, level.getGameTime());
                started = level.getGameTime();
                stage = 0;
            }
            long now = level.getGameTime();
            require(now - started < 5000, "real generation/unload timed out");
            if (stage == 0) {
                var sites = fixture.activeSites();
                if (sites.isEmpty()) return;
                require(sites.size() == 1 && fixture.replacementJobCount() == 0, "one replacement completed exactly once");
                var site = sites.get(0);
                double dx = site.center().getX() - ORIGIN.getX();
                double dz = site.center().getZ() - ORIGIN.getZ();
                require(dx * dx + dz * dz >= 512 * 512 && dx * dx + dz * dz <= 1280 * 1280,
                        "replacement distance preserved");
                require(site.brushables().size() >= 6 && site.brushables().size() <= 9, "brushable count preserved");
                for (BlockPos pos : site.brushables()) {
                    var chunk = level.getChunkSource().getChunkNow(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
                    require(chunk != null && chunk.getBlockState(pos).getBlock() instanceof BrushableBlock,
                            "actual brushable blocks placed without loading reads");
                }
                ChunkPos center = new ChunkPos(site.center());
                for (int x = center.x - 1; x <= center.x + 1; x++) {
                    for (int z = center.z - 1; z <= center.z + 1; z++) generatedChunks.add(new ChunkPos(x, z));
                }
                stage = 1;
            } else if (stage == 1) {
                // No players/tickets are near this distant fixture. Real unload proves lease release.
                for (ChunkPos chunk : generatedChunks) {
                    if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null) return;
                }
                require(fixture.activeSites().size() == 1 && fixture.replacementJobCount() == 0,
                        "unload does not duplicate or lose saved site");
                BattlefieldSystem1201.clear();
                fixture = new BattlefieldSavedData1201();
                level.getDataStorage().set("echo_warrior_battlefield_sites", fixture);
                queue(ORIGIN, now);
                for (int x = 9997; x <= 10002; x++) {
                    for (int z = 9997; z <= 10002; z++) {
                        if (x == 10000 && z == 10000) continue;
                        BlockPos occupied = new BlockPos(x * 768 + 384, 64, z * 768 + 384);
                        var region = fixture.prepareForcedRegion(level, new ChunkPos(occupied));
                        fixture.activate(region.key(), occupied, occupied, "roman", List.of(occupied));
                    }
                }
                stage = 2;
                started = now;
            } else if (stage == 2 && now - started >= 40) {
                var dormant = fixture.nextReadyReplacement(Long.MAX_VALUE);
                require(dormant != null && dormant.readyAt() >= started + 1200 && dormant.attempts() == 1,
                        "fully occupied search area sleeps without dropping old job");
                require(fixture.replacementJobCount() == 1, "dormant job preserved");
                for (ChunkPos chunk : generatedChunks) {
                    require(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null, "occupied search did not load chunks");
                }
                BattlefieldSystem1201.clear();
                level.getDataStorage().set("echo_warrior_battlefield_sites", original);
                EchoWarrior1201.LOGGER.info("BATTLEFIELD GENERATION SELFTEST PASSED replacement=actual distance=preserved unload=actual crowded=backoff");
                reset();
            }
        } catch (RuntimeException | Error error) {
            if (level != null && original != null) {
                BattlefieldSystem1201.clear();
                level.getDataStorage().set("echo_warrior_battlefield_sites", original);
            }
            reset();
            throw error;
        }
    }

    private static void queue(BlockPos origin, long now) {
        var region = fixture.prepareForcedRegion(level, new ChunkPos(origin));
        fixture.activate(region.key(), origin, origin, "roman", List.of(origin));
        fixture.removeBrushableAt(origin, now);
        fixture.markPlaced(now - 600);
        require(fixture.replacementJobCount() == 1, "actual completion queues one replacement");
    }

    private static void reset() {
        armed = false;
        level = null;
        fixture = null;
        original = null;
        generatedChunks.clear();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Battlefield generation acceptance: " + message);
    }
}
