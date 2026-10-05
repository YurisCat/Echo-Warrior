package com.yuriscat.echowarrior.compat.world;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Counts index operations, then exercises the actual removal Mixin and chunk-load repair. */
public final class BattlefieldPerformanceSelfTest1201 {
    private BattlefieldPerformanceSelfTest1201() {}

    public static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        checkIndexBounds(level);
        checkRemovalEvents(level);
        EchoWarrior1201.LOGGER.info("[Compat1201] BATTLEFIELD PERFORMANCE SELFTEST PASSED regions=10000 lookup=1 search=49 removal=event reload=local");
    }

    private static void checkIndexBounds(ServerLevel level) {
        BattlefieldSavedData1201 data = new BattlefieldSavedData1201();
        CountingRegions regions = new CountingRegions();
        try {
            var field = BattlefieldSavedData1201.class.getDeclaredField("regions");
            field.setAccessible(true);
            field.set(data, regions);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot instrument isolated battlefield index", error);
        }
        for (int index = 0; index < 10000; index++) {
            data.prepareForcedRegion(level, new ChunkPos((1000 + index) * 48, 1000 * 48));
        }
        // The guaranteed block crosses both negative-coordinate region boundaries.
        BlockPos center = new BlockPos(-769, 100, -769);
        BlockPos relic = center.offset(2, 0, 2);
        BlockPos ordinary = center.offset(-1, 0, 0);
        var region = data.prepareForcedRegion(level, new ChunkPos(center));
        data.activate(region.key(), center, relic, "roman", List.of(relic, ordinary));
        regions.forbidFullScan = true;
        regions.lookups = 0;
        require(data.findActiveByCenter(center.asLong()) != null && regions.lookups == 1,
                "locked target uses exactly one lookup, regardless of world history");
        require(data.findActiveByCenter(center.above().asLong()) == null, "center identity includes Y");
        regions.lookups = 0;
        require(data.nearestActive(center.offset(2048, 0, 0), 2048).center().equals(center)
                        && regions.lookups == 49, "radius boundary uses 49 region lookups");
        require(data.nearestActive(center.offset(2049, 0, 0), 2048) == null, "outside radius excluded");
        require(data.trackedBrushablesInChunk(new ChunkPos(relic)).equals(List.of(relic)),
                "chunk reconciliation includes neighboring regions but excludes other chunks");
        require(data.removeBrushableAt(ordinary, 10).remaining().equals(List.of(relic)), "ordinary removal retained site");
        require(data.removeBrushableAt(relic, 20).relicCompleted(), "boundary-crossing relic completes site");
        require(data.findActiveByCenter(center.asLong()) == null && data.replacementJobCount() == 1,
                "completion invalidates target and schedules one replacement");
        require(data.removeBrushableAt(relic, 21) == null && data.replacementJobCount() == 1, "duplicate removal harmless");
        regions.forbidFullScan = false;
        // Admin locate remains a global search, including distant sites outside compass range.
        var distant = data.prepareForcedRegion(level, new ChunkPos(100000, 100000));
        BlockPos farCenter = new BlockPos(1600008, 100, 1600008);
        data.activate(distant.key(), farCenter, farCenter, "roman", List.of(farCenter));
        require(data.nearestKnownActive(center).center().equals(farCenter), "admin locate remains unbounded");
    }

    private static void checkRemovalEvents(ServerLevel level) {
        BattlefieldSavedData1201 original = BattlefieldSavedData1201.get(level);
        BattlefieldSavedData1201 data = new BattlefieldSavedData1201();
        level.getChunk(0, 0);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 8, 8) + 4;
        BlockPos relic = new BlockPos(8, y, 8);
        BlockPos ordinary = relic.east();
        BlockPos last = relic.south();
        Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        for (BlockPos pos : List.of(relic, ordinary, last)) {
            require(level.getBlockEntity(pos) == null && level.getBlockEntity(pos.below()) == null, "empty fixture positions");
            saved.put(pos, level.getBlockState(pos));
            saved.put(pos.below(), level.getBlockState(pos.below()));
        }
        try {
            level.getDataStorage().set("echo_warrior_battlefield_sites", data);
            for (BlockPos pos : List.of(relic, ordinary, last)) {
                level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(pos, ModContent1201.SUSPICIOUS_GRASS_BLOCK.defaultBlockState(), 3);
            }
            var region = data.prepareForcedRegion(level, new ChunkPos(relic));
            data.activate(region.key(), relic, relic, "roman", List.of(relic, ordinary, last));
            level.setBlock(relic, level.getBlockState(relic).setValue(BlockStateProperties.DUSTED, 2), 3);
            require(data.findActiveByCenter(relic.asLong()).brushables().size() == 3, "brushing-stage update does not complete");
            level.setBlock(ordinary, Blocks.AIR.defaultBlockState(), 3);
            require(data.findActiveByCenter(relic.asLong()).brushables().size() == 2, "actual removal updates immediately");
            level.setBlock(relic, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            require(data.findActiveByCenter(relic.asLong()) == null && data.replacementJobCount() == 1,
                    "actual relic replacement completes before the next server tick");
            UUID player = UUID.randomUUID();
            data.setSalvageTracker(player, relic);
            require(data.findSalvageByCenter(relic.asLong()).remaining().equals(List.of(last)), "salvage survives completion");
            // Use the vanilla falling brushable too; it inherits the same removal callback.
            level.setBlock(last, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 3);
            require(data.findSalvageByCenter(relic.asLong()) != null, "another brushable still counts");
            level.setBlock(last, Blocks.AIR.defaultBlockState(), 3);
            require(data.findSalvageByCenter(relic.asLong()) == null && data.salvageCenter(player).isEmpty(),
                    "last salvage removal clears shared count and persistent tracker");

            // Simulate a stale legacy save without invoking the removal callback.
            data.activate(region.key(), relic, relic, "roman", List.of(relic, ordinary));
            data.markPlaced(level.getGameTime());
            BattlefieldSystem1201.noteChunk(level, level.getChunk(0, 0));
            require(data.findActiveByCenter(relic.asLong()) == null && data.findSalvageByCenter(relic.asLong()) == null,
                    "chunk-load reconciliation removes stale active and completed records");
            // Tick maintenance no longer inspects blocks in every known active site.
            BlockPos unloaded = new BlockPos(7000008, 100, 7000008);
            var farRegion = data.prepareForcedRegion(level, new ChunkPos(unloaded));
            data.activate(farRegion.key(), unloaded, unloaded, "roman", List.of(unloaded));
            BattlefieldSystem1201.tick(level.getServer());
            require(data.findActiveByCenter(unloaded.asLong()) != null && !level.hasChunkAt(unloaded),
                    "tick leaves unloaded sites untouched without loading chunks");
        } finally {
            // Keep rollback callbacks on the test data; restore the real index last.
            saved.forEach((pos, state) -> level.setBlock(pos, state, 3));
            level.getDataStorage().set("echo_warrior_battlefield_sites", original);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Battlefield performance: " + message);
    }

    private static final class CountingRegions extends HashMap<Long, BattlefieldSavedData1201.RegionState> {
        private int lookups;
        private boolean forbidFullScan;
        @Override public BattlefieldSavedData1201.RegionState get(Object key) { lookups++; return super.get(key); }
        @Override public Collection<BattlefieldSavedData1201.RegionState> values() {
            require(!forbidFullScan, "full region scan forbidden on gameplay lookup");
            return super.values();
        }
        @Override public Set<Map.Entry<Long, BattlefieldSavedData1201.RegionState>> entrySet() {
            require(!forbidFullScan, "full region scan forbidden on block removal");
            return super.entrySet();
        }
    }
}
