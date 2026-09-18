package com.yuriscat.echowarrior.compat.world;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.ModTags1211;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BattlefieldSystem1211 {
    private static final int FORCED_ATTEMPTS_PER_REGION = 4;
    private static final int REPLACEMENT_IDEAL_ATTEMPTS = 8;
    private static final int REPLACEMENT_NORMAL_ATTEMPTS = 24;
    private static final int REPLACEMENT_WIDE_ATTEMPTS = 48;
    private static final int REPLACEMENT_IDEAL_MIN_DISTANCE = 640;
    private static final int REPLACEMENT_IDEAL_MAX_DISTANCE = 720;
    private static final int REPLACEMENT_NORMAL_MIN_DISTANCE = 512;
    private static final int REPLACEMENT_NORMAL_MAX_DISTANCE = 896;
    private static final int REPLACEMENT_WIDE_MAX_DISTANCE = 1024;
    private static final int REPLACEMENT_FALLBACK_MAX_DISTANCE = 1280;
    private static final Set<Long> PENDING_REGIONS = new HashSet<>();
    private static final Map<Long, Set<Long>> LOADED_REGION_CHUNKS = new HashMap<>();
    private static final Map<UUID, ForcedGenerationJob> FORCED_JOBS = new HashMap<>();
    private static int pendingCursor;
    private static boolean replacementLoading;

    private BattlefieldSystem1211() {
    }

    public static void noteChunk(ServerLevel level, LevelChunk chunk) {
        noteChunk(level, chunk, false);
    }

    public static void noteChunk(ServerLevel level, LevelChunk chunk, boolean newlyGenerated) {
        if (!level.dimension().equals(Level.OVERWORLD) || replacementLoading) return;
        BattlefieldSavedData1211.RegionState state = BattlefieldSavedData1211.get(level)
                .noteNaturalChunkLoad(level, chunk.getPos(), newlyGenerated);
        if (state.status() == BattlefieldSavedData1211.Status.WAITING) {
            PENDING_REGIONS.add(state.key());
            LOADED_REGION_CHUNKS.computeIfAbsent(state.key(), ignored -> new HashSet<>())
                    .add(chunk.getPos().toLong());
        }
    }

    public static void forgetChunk(ServerLevel level, LevelChunk chunk) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        ChunkPos pos = chunk.getPos();
        long regionKey = ChunkPos.asLong(Math.floorDiv(pos.x, BattlefieldSavedData1211.REGION_CHUNKS),
                Math.floorDiv(pos.z, BattlefieldSavedData1211.REGION_CHUNKS));
        Set<Long> loaded = LOADED_REGION_CHUNKS.get(regionKey);
        if (loaded == null) return;
        loaded.remove(pos.toLong());
        if (loaded.isEmpty()) LOADED_REGION_CHUNKS.remove(regionKey);
    }

    public static void markPlayerModified(ServerLevel level, BlockPos pos) {
        if (level.dimension().equals(Level.OVERWORLD)) {
            BattlefieldSavedData1211.get(level).markPlayerModified(new ChunkPos(pos));
        }
    }

    public static void tick(MinecraftServer server) {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) return;
        long now = level.getGameTime();
        tickForcedGeneration(server, level);
        if (now % 20L != 0L) return;
        BattlefieldSavedData1211 data = BattlefieldSavedData1211.get(level);
        detectRemovedBrushables(level, data, now);
        tickReplacementGeneration(level, data, now);
        if (PENDING_REGIONS.isEmpty() || now - data.lastPlacementTick() < 600L) return;

        Long[] queued = PENDING_REGIONS.toArray(Long[]::new);
        int checks = Math.min(8, queued.length);
        int start = Math.floorMod(pendingCursor, queued.length);
        pendingCursor = (start + checks) % queued.length;
        for (int offset = 0; offset < checks; offset++) {
            long key = queued[(start + offset) % queued.length];
            BattlefieldSavedData1211.RegionState state = data.region(key);
            if (state == null || state.status() != BattlefieldSavedData1211.Status.WAITING) {
                PENDING_REGIONS.remove(key);
                continue;
            }
            if (now < state.readyAt()) continue;
            ChunkPos candidate = chooseLoadedCandidate(level, key, state, now);
            if (candidate == null) continue;
            Placement placement = findAndPlace(level, data, state, candidate);
            if (placement == null) {
                data.defer(key, now + 200L);
                continue;
            }
            data.activate(key, placement.center, placement.relic, placement.culture.id(), placement.brushables);
            data.markPlaced(now);
            PENDING_REGIONS.remove(key);
            LOADED_REGION_CHUNKS.remove(key);
            EchoWarrior1211.LOGGER.info("Generated 1.21.1 battlefield ruin at {} with guaranteed relic block at {}.",
                    placement.center, placement.relic);
            break;
        }
    }

    public static void clear() {
        PENDING_REGIONS.clear();
        LOADED_REGION_CHUNKS.clear();
        FORCED_JOBS.clear();
        pendingCursor = 0;
        replacementLoading = false;
    }

    private static void tickReplacementGeneration(ServerLevel level, BattlefieldSavedData1211 data, long now) {
        BattlefieldSavedData1211.ReplacementJob job = data.nextReadyReplacement(now);
        if (job == null || now - data.lastPlacementTick() < 600L) return;

        Placement placement;
        replacementLoading = true;
        try {
            placement = tryReplacementAttempt(level, data, job);
        } finally {
            replacementLoading = false;
        }
        if (placement == null) {
            int nextAttempt = job.attempts() + 1;
            data.retryReplacement(job.originCenter(), now + (nextAttempt % 8 == 0 ? 200L : 20L));
            return;
        }

        ChunkPos centerChunk = new ChunkPos(Math.floorDiv(placement.center.getX(), 16),
                Math.floorDiv(placement.center.getZ(), 16));
        BattlefieldSavedData1211.RegionState prepared = data.prepareReplacementRegion(level, centerChunk);
        if (prepared == null) {
            data.retryReplacement(job.originCenter(), now + 20L);
            return;
        }
        data.activate(prepared.key(), placement.center, placement.relic, placement.culture.id(), placement.brushables);
        data.markPlaced(now);
        data.completeReplacement(job.originCenter());
        PENDING_REGIONS.remove(prepared.key());
        LOADED_REGION_CHUNKS.remove(prepared.key());
        EchoWarrior1211.LOGGER.info(
                "Generated 1.21.1 replacement battlefield ruin at {} for completed site {}; guaranteed relic block at {}.",
                placement.center, BlockPos.of(job.originCenter()), placement.relic);
    }

    private static Placement tryReplacementAttempt(ServerLevel level, BattlefieldSavedData1211 data,
                                                    BattlefieldSavedData1211.ReplacementJob job) {
        BlockPos origin = BlockPos.of(job.originCenter());
        ReplacementDistance distance = replacementDistance(job.attempts());
        RandomSource random = RandomSource.create(job.seed() ^ (long)job.attempts() * 0x9E3779B97F4A7C15L);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double targetDistance = distance.minimum + random.nextDouble() * (distance.maximum - distance.minimum);
        int targetX = origin.getX() + (int)Math.round(Math.cos(angle) * targetDistance);
        int targetZ = origin.getZ() + (int)Math.round(Math.sin(angle) * targetDistance);
        ChunkPos candidate = new ChunkPos(Math.floorDiv(targetX, 16), Math.floorDiv(targetZ, 16));

        int originRegionX = Math.floorDiv(Math.floorDiv(origin.getX(), 16), BattlefieldSavedData1211.REGION_CHUNKS);
        int originRegionZ = Math.floorDiv(Math.floorDiv(origin.getZ(), 16), BattlefieldSavedData1211.REGION_CHUNKS);
        int candidateRegionX = Math.floorDiv(candidate.x, BattlefieldSavedData1211.REGION_CHUNKS);
        int candidateRegionZ = Math.floorDiv(candidate.z, BattlefieldSavedData1211.REGION_CHUNKS);
        if (originRegionX == candidateRegionX && originRegionZ == candidateRegionZ
                || !data.canUseReplacementRegion(candidate)) return null;

        level.getChunk(candidate.x, candidate.z);
        double minimumSqr = (double)distance.minimum * distance.minimum;
        double maximumSqr = (double)distance.maximum * distance.maximum;
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = candidate.getMinBlockX() + 2 + random.nextInt(12);
            int z = candidate.getMinBlockZ() + 2 + random.nextInt(12);
            BlockPos horizontal = new BlockPos(x, origin.getY(), z);
            double actualDistance = horizontalDistanceSqr(origin, horizontal);
            if (actualDistance < minimumSqr || actualDistance > maximumSqr) continue;
            int radius = 7 + random.nextInt(4);
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos center = new BlockPos(x, surface, z);
            if (!level.getBiome(center).is(ModTags1211.HAS_BATTLEFIELD_RUIN)
                    || !data.isFarEnoughFromKnownSites(center, 512.0)) continue;
            loadSiteChunks(level, x, z, radius);
            if (!isSafeSite(level, data, center, radius)) continue;
            Placement placement = placeSite(level, center, radius, random);
            if (placement != null) return placement;
        }
        return null;
    }

    private static ReplacementDistance replacementDistance(int attempts) {
        if (attempts < REPLACEMENT_IDEAL_ATTEMPTS) {
            return new ReplacementDistance(REPLACEMENT_IDEAL_MIN_DISTANCE, REPLACEMENT_IDEAL_MAX_DISTANCE);
        }
        if (attempts < REPLACEMENT_NORMAL_ATTEMPTS) {
            return new ReplacementDistance(REPLACEMENT_NORMAL_MIN_DISTANCE, REPLACEMENT_NORMAL_MAX_DISTANCE);
        }
        if (attempts < REPLACEMENT_WIDE_ATTEMPTS) {
            return new ReplacementDistance(REPLACEMENT_NORMAL_MIN_DISTANCE, REPLACEMENT_WIDE_MAX_DISTANCE);
        }
        return new ReplacementDistance(REPLACEMENT_NORMAL_MIN_DISTANCE, REPLACEMENT_FALLBACK_MAX_DISTANCE);
    }

    /** Starts a bounded, tick-spread admin generation job without leaving chunks force-loaded. */
    public static boolean requestForceGeneration(ServerLevel level, ServerPlayer requester,
                                                 int requested, int maximumDistance) {
        if (!level.dimension().equals(Level.OVERWORLD) || FORCED_JOBS.containsKey(requester.getUUID())) return false;
        BlockPos origin = requester.blockPosition();
        FORCED_JOBS.put(requester.getUUID(), new ForcedGenerationJob(requester.getUUID(), origin, requested,
                maximumDistance, forcedRegionCandidates(origin, maximumDistance)));
        return true;
    }

    private static List<RegionCandidate> forcedRegionCandidates(BlockPos origin, int maximumDistance) {
        int regionSize = BattlefieldSavedData1211.REGION_CHUNKS * 16;
        int originRegionX = Math.floorDiv(origin.getX(), regionSize);
        int originRegionZ = Math.floorDiv(origin.getZ(), regionSize);
        int radius = Math.max(1, (int)Math.ceil((double)maximumDistance / regionSize) + 1);
        List<RegionCandidate> regions = new ArrayList<>();
        for (int regionX = originRegionX - radius; regionX <= originRegionX + radius; regionX++) {
            for (int regionZ = originRegionZ - radius; regionZ <= originRegionZ + radius; regionZ++) {
                int centerX = regionX * regionSize + regionSize / 2;
                int centerZ = regionZ * regionSize + regionSize / 2;
                double distanceSqr = horizontalDistanceSqr(origin, new BlockPos(centerX, origin.getY(), centerZ));
                if (distanceSqr <= (double)maximumDistance * maximumDistance) {
                    regions.add(new RegionCandidate(regionX, regionZ, distanceSqr));
                }
            }
        }
        regions.sort(Comparator.comparingDouble(RegionCandidate::distanceSqr));
        return List.copyOf(regions);
    }

    private static void tickForcedGeneration(MinecraftServer server, ServerLevel level) {
        if (FORCED_JOBS.isEmpty() || level.getGameTime() % 5L != 0L) return;
        ForcedGenerationJob job = FORCED_JOBS.values().iterator().next();
        BattlefieldSavedData1211 data = BattlefieldSavedData1211.get(level);
        while (job.regionIndex < job.regions.size()) {
            RegionCandidate candidate = job.regions.get(job.regionIndex);
            long key = ChunkPos.asLong(candidate.regionX, candidate.regionZ);
            BattlefieldSavedData1211.RegionState existing = data.region(key);
            if (existing != null && existing.status() == BattlefieldSavedData1211.Status.ACTIVE) {
                job.nextRegion();
                continue;
            }
            if (job.attemptInRegion == 0) job.attemptedRegions++;
            Placement placement;
            replacementLoading = true;
            try {
                placement = tryForcedAttempt(level, data, candidate, job);
            } finally {
                replacementLoading = false;
            }
            job.attemptInRegion++;
            if (placement != null) {
                activateForcedPlacement(level, data, placement);
                job.generated.add(placement.center);
                job.nextRegion();
                EchoWarrior1211.LOGGER.info(
                        "Force-generated 1.21.1 battlefield ruin at {} with guaranteed relic block at {}.",
                        placement.center, placement.relic);
                ServerPlayer requester = server.getPlayerList().getPlayer(job.requester);
                if (requester != null) requester.sendSystemMessage(Component.literal("战场遗迹生成进度："
                        + job.generated.size() + "/" + job.requested + "，中心 " + placement.center.toShortString() + "。"));
            }
            if (job.generated.size() >= job.requested) {
                finishForcedGeneration(server, job);
                return;
            }
            if (placement == null && job.attemptInRegion >= FORCED_ATTEMPTS_PER_REGION) job.nextRegion();
            return;
        }
        finishForcedGeneration(server, job);
    }

    private static Placement tryForcedAttempt(ServerLevel level, BattlefieldSavedData1211 data,
                                              RegionCandidate region, ForcedGenerationJob job) {
        int firstChunkX = region.regionX * BattlefieldSavedData1211.REGION_CHUNKS;
        int firstChunkZ = region.regionZ * BattlefieldSavedData1211.REGION_CHUNKS;
        long seed = level.getSeed() ^ ChunkPos.asLong(region.regionX, region.regionZ)
                ^ job.origin.asLong() ^ (long)job.attemptInRegion * 0x9E3779B97F4A7C15L;
        RandomSource random = RandomSource.create(seed);
        int chunkX = firstChunkX + random.nextInt(BattlefieldSavedData1211.REGION_CHUNKS);
        int chunkZ = firstChunkZ + random.nextInt(BattlefieldSavedData1211.REGION_CHUNKS);
        level.getChunk(chunkX, chunkZ);
        for (int localAttempt = 0; localAttempt < 8; localAttempt++) {
            int x = chunkX * 16 + 2 + random.nextInt(12);
            int z = chunkZ * 16 + 2 + random.nextInt(12);
            if (horizontalDistanceSqr(job.origin, new BlockPos(x, job.origin.getY(), z))
                    > (double)job.maximumDistance * job.maximumDistance) continue;
            int radius = 7 + random.nextInt(4);
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos center = new BlockPos(x, surface, z);
            if (!level.getBiome(center).is(ModTags1211.HAS_BATTLEFIELD_RUIN)
                    || !data.isFarEnoughFromKnownSites(center, 512.0)) continue;
            loadSiteChunks(level, x, z, radius);
            if (!isSafeSite(level, data, center, radius)) continue;
            Placement placement = placeSite(level, center, radius, random);
            if (placement != null) return placement;
        }
        return null;
    }

    private static void activateForcedPlacement(ServerLevel level, BattlefieldSavedData1211 data,
                                                Placement placement) {
        ChunkPos centerChunk = new ChunkPos(Math.floorDiv(placement.center.getX(), 16),
                Math.floorDiv(placement.center.getZ(), 16));
        BattlefieldSavedData1211.RegionState prepared = data.prepareForcedRegion(level, centerChunk);
        data.activate(prepared.key(), placement.center, placement.relic, placement.culture.id(), placement.brushables);
        data.markPlaced(level.getGameTime());
        PENDING_REGIONS.remove(prepared.key());
        LOADED_REGION_CHUNKS.remove(prepared.key());
    }

    private static void finishForcedGeneration(MinecraftServer server, ForcedGenerationJob job) {
        FORCED_JOBS.remove(job.requester);
        String positions = job.generated.stream().map(BlockPos::toShortString)
                .reduce((first, second) -> first + "；" + second).orElse("无");
        Component message = Component.literal("战场遗迹生成完成：" + job.generated.size() + "/" + job.requested
                + "（检查 " + job.attemptedRegions + " 个区域），中心：" + positions + "。"
                + (job.generated.size() < job.requested ? " 未满足的数量没有绕过安全检查。" : ""));
        ServerPlayer requester = server.getPlayerList().getPlayer(job.requester);
        if (requester != null) requester.sendSystemMessage(message);
        EchoWarrior1211.LOGGER.info("1.21.1 battlefield generation job completed: {}/{} sites across {} checked regions.",
                job.generated.size(), job.requested, job.attemptedRegions);
    }

    private static void loadSiteChunks(ServerLevel level, int x, int z, int radius) {
        int minChunkX = Math.floorDiv(x - radius - 1, 16);
        int maxChunkX = Math.floorDiv(x + radius + 1, 16);
        int minChunkZ = Math.floorDiv(z - radius - 1, 16);
        int maxChunkZ = Math.floorDiv(z + radius + 1, 16);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) level.getChunk(chunkX, chunkZ);
        }
    }

    private static void detectRemovedBrushables(ServerLevel level, BattlefieldSavedData1211 data, long now) {
        List<BlockPos> tracked = new ArrayList<>();
        for (BattlefieldSavedData1211.ActiveSite site : data.activeSites()) tracked.addAll(site.brushables());
        for (BattlefieldSavedData1211.SalvageSite site : data.salvageSites()) tracked.addAll(site.remaining());
        for (BlockPos pos : tracked) {
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BrushableBlock || state.is(ModTags1211.BATTLEFIELD_BRUSHABLES)) continue;
            BattlefieldSavedData1211.RemovalResult result = data.removeBrushableAt(pos, now);
            if (result != null) EchoCompassSystem1211.onBattlefieldBlockRemoved(level, result);
        }
    }

    private static ChunkPos chooseLoadedCandidate(ServerLevel level, long regionKey,
                                                   BattlefieldSavedData1211.RegionState state, long now) {
        Set<Long> loaded = LOADED_REGION_CHUNKS.get(regionKey);
        if (loaded == null || loaded.isEmpty()) {
            ChunkPos saved = new ChunkPos(state.candidateChunk());
            return level.hasChunk(saved.x, saved.z) ? saved : null;
        }
        Long[] candidates = loaded.toArray(Long[]::new);
        int start = Math.floorMod(Long.hashCode(state.seed() ^ Math.floorDiv(now, 200L)), candidates.length);
        for (int offset = 0; offset < candidates.length; offset++) {
            ChunkPos candidate = new ChunkPos(candidates[(start + offset) % candidates.length]);
            if (level.hasChunk(candidate.x, candidate.z)) return candidate;
        }
        return null;
    }

    private static Placement findAndPlace(ServerLevel level, BattlefieldSavedData1211 data,
                                          BattlefieldSavedData1211.RegionState state, ChunkPos chunk) {
        RandomSource random = RandomSource.create(state.seed() ^ chunk.toLong() ^ level.getGameTime());
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = chunk.getMinBlockX() + 2 + random.nextInt(12);
            int z = chunk.getMinBlockZ() + 2 + random.nextInt(12);
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos center = new BlockPos(x, surface, z);
            int radius = 7 + random.nextInt(4);
            if (!isSafeSite(level, data, center, radius)) continue;
            Placement placement = placeSite(level, center, radius, random);
            if (placement != null) return placement;
        }
        return null;
    }

    private static boolean isSafeSite(ServerLevel level, BattlefieldSavedData1211 data, BlockPos center, int radius) {
        if (!level.getBiome(center).is(ModTags1211.HAS_BATTLEFIELD_RUIN)) return false;
        int minChunkX = Math.floorDiv(center.getX() - radius - 1, 16);
        int maxChunkX = Math.floorDiv(center.getX() + radius + 1, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - radius - 1, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + radius + 1, 16);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)
                        || data.isPlayerModified(new ChunkPos(chunkX, chunkZ))) return false;
            }
        }
        if (level.structureManager().hasAnyStructureAt(center)) return false;

        int minimumY = Integer.MAX_VALUE;
        int maximumY = Integer.MIN_VALUE;
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                if (dx * dx + dz * dz > radius * radius) continue;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        center.getX() + dx, center.getZ() + dz) - 1;
                minimumY = Math.min(minimumY, y);
                maximumY = Math.max(maximumY, y);
                BlockPos floor = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                BlockState floorState = level.getBlockState(floor);
                BlockState above = level.getBlockState(floor.above());
                if (!isNaturalFloor(floorState) || floorState.hasBlockEntity()
                        || !floorState.isFaceSturdy(level, floor, Direction.UP)
                        || above.is(BlockTags.LOGS) || above.is(BlockTags.LEAVES)
                        || above.hasBlockEntity() || !above.getFluidState().isEmpty() || !isClearable(above)) return false;
            }
        }
        return maximumY - minimumY <= 1;
    }

    private static Placement placeSite(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        BattlefieldCulture1211 culture = BattlefieldCulture1211.random(random);
        List<BlockPos> floorPositions = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double edgeNoise = random.nextDouble() * 2.4 - 1.2;
                if (Math.sqrt(dx * dx + dz * dz) > radius + edgeNoise) continue;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        center.getX() + dx, center.getZ() + dz) - 1;
                BlockPos floor = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
                if (!isNaturalFloor(level.getBlockState(floor))) continue;
                clearVegetation(level, floor.above());
                floorPositions.add(floor);
            }
        }
        Collections.shuffle(floorPositions, new java.util.Random(random.nextLong()));
        int count = Math.min(floorPositions.size(), 6 + random.nextInt(4));
        if (count < 6) return null;
        List<BlockPos> brushables = List.copyOf(floorPositions.subList(0, count));
        BlockPos guaranteed = brushables.get(random.nextInt(count));
        for (BlockPos pos : brushables) {
            Block block = suspiciousFor(level.getBlockState(pos));
            level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable) {
                brushable.setLootTable(pos.equals(guaranteed) ? culture.guaranteedLoot() : culture.commonLoot(),
                        random.nextLong());
            }
        }
        return new Placement(center, guaranteed, culture, brushables);
    }

    private static void clearVegetation(ServerLevel level, BlockPos pos) {
        for (int offset = 0; offset < 3; offset++) {
            BlockPos target = pos.above(offset);
            BlockState state = level.getBlockState(target);
            if (state.isAir()) continue;
            if (!isClearable(state) || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                    || state.hasBlockEntity() || !state.getFluidState().isEmpty()) break;
            level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean isClearable(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.REPLACEABLE)
                || state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS);
    }

    private static boolean isNaturalFloor(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL);
    }

    private static Block suspiciousFor(BlockState floor) {
        if (floor.is(Blocks.GRASS_BLOCK)) return ModContent1211.SUSPICIOUS_GRASS_BLOCK;
        if (floor.is(BlockTags.SAND)) return Blocks.SUSPICIOUS_SAND;
        if (floor.is(Blocks.GRAVEL)) return Blocks.SUSPICIOUS_GRAVEL;
        return ModContent1211.SUSPICIOUS_DIRT;
    }

    private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private record ReplacementDistance(int minimum, int maximum) {
    }

    private record RegionCandidate(int regionX, int regionZ, double distanceSqr) {
    }

    private static final class ForcedGenerationJob {
        private final UUID requester;
        private final BlockPos origin;
        private final int requested;
        private final int maximumDistance;
        private final List<RegionCandidate> regions;
        private final List<BlockPos> generated = new ArrayList<>();
        private int regionIndex;
        private int attemptInRegion;
        private int attemptedRegions;

        private ForcedGenerationJob(UUID requester, BlockPos origin, int requested, int maximumDistance,
                                    List<RegionCandidate> regions) {
            this.requester = requester;
            this.origin = origin;
            this.requested = requested;
            this.maximumDistance = maximumDistance;
            this.regions = regions;
        }

        private void nextRegion() {
            this.regionIndex++;
            this.attemptInRegion = 0;
        }
    }

    private record Placement(BlockPos center, BlockPos relic, BattlefieldCulture1211 culture,
                             List<BlockPos> brushables) {
    }
}
