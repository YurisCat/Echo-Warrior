package com.yuriscat.echowarrior.world;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModBlocks;
import com.yuriscat.echowarrior.ModTags;
import com.yuriscat.echowarrior.platform.PlatformServices;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BattlefieldSystem {
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
	private static final LongSet PENDING_REGIONS = new LongOpenHashSet();
	private static final Map<Long, LongSet> LOADED_REGION_CHUNKS = new HashMap<>();
	private static final Map<UUID, ForcedGenerationJob> FORCED_JOBS = new HashMap<>();
	private static int pendingCursor;
	private static boolean forceGenerationLoading;

	private BattlefieldSystem() {
	}

	public static void initialize() {
	}

	public static void noteChunk(ServerLevel level, LevelChunk chunk, boolean newlyGenerated) {
		if (!level.dimension().equals(Level.OVERWORLD) || forceGenerationLoading) return;
		BattlefieldSavedData.RegionState state = BattlefieldSavedData.get(level)
				.noteNaturalChunkLoad(level, chunk.getPos(), newlyGenerated);
		if (state.status() == BattlefieldSavedData.Status.WAITING) {
			PENDING_REGIONS.add(state.key());
			LOADED_REGION_CHUNKS.computeIfAbsent(state.key(), ignored -> new LongOpenHashSet())
					.add(chunk.getPos().pack());
		}
	}

	public static void forgetChunk(ServerLevel level, LevelChunk chunk) {
		if (!level.dimension().equals(Level.OVERWORLD)) return;
		ChunkPos chunkPos = chunk.getPos();
		long regionKey = ChunkPos.pack(
				Math.floorDiv(chunkPos.x(), BattlefieldSavedData.REGION_CHUNKS),
				Math.floorDiv(chunkPos.z(), BattlefieldSavedData.REGION_CHUNKS));
		LongSet loaded = LOADED_REGION_CHUNKS.get(regionKey);
		if (loaded == null) return;
		loaded.remove(chunkPos.pack());
		if (loaded.isEmpty()) LOADED_REGION_CHUNKS.remove(regionKey);
	}

	public static void markPlayerModified(ServerLevel level, BlockPos pos) {
		PlatformServices.markPlayerModified(level.getChunkAt(pos));
	}

	public static void tick(MinecraftServer server) {
		ServerLevel level = server.getLevel(Level.OVERWORLD);
		if (level == null) return;
		tickForcedGeneration(server, level);
		tickReplacementGeneration(level);
		if (PENDING_REGIONS.isEmpty() || level.getGameTime() % 20L != 0L) return;
		BattlefieldSavedData data = BattlefieldSavedData.get(level);
		long now = level.getGameTime();
		if (now - data.lastPlacementTick() < 600L) return;

		long[] queued = PENDING_REGIONS.toLongArray();
		int checks = Math.min(8, queued.length);
		int start = Math.floorMod(pendingCursor, queued.length);
		pendingCursor = (start + checks) % queued.length;
		for (int offset = 0; offset < checks; offset++) {
			long key = queued[(start + offset) % queued.length];
			BattlefieldSavedData.RegionState state = data.region(key);
			if (state == null || state.status() != BattlefieldSavedData.Status.WAITING) {
				PENDING_REGIONS.remove(key);
				continue;
			}
			if (now < state.readyAt()) continue;
			ChunkPos candidateChunk = chooseLoadedCandidate(level, key, state, now);
			if (candidateChunk == null) continue;
			Placement placement = findAndPlace(level, data, state, candidateChunk);
			if (placement != null) {
				data.activate(key, placement.center, placement.relic, placement.culture.id(), placement.brushables);
				data.markPlaced(now);
				PENDING_REGIONS.remove(key);
				LOADED_REGION_CHUNKS.remove(key);
				EchoWarrior.LOGGER.info("Generated battlefield ruin at {} with guaranteed relic block at {}.",
						placement.center, placement.relic);
				break;
			}
			data.defer(key, now + 200L);
		}
	}

	public static void clear() {
		PENDING_REGIONS.clear();
		LOADED_REGION_CHUNKS.clear();
		FORCED_JOBS.clear();
		pendingCursor = 0;
		forceGenerationLoading = false;
	}

	private static void tickReplacementGeneration(ServerLevel level) {
		long now = level.getGameTime();
		if (now % 20L != 0L) return;
		BattlefieldSavedData data = BattlefieldSavedData.get(level);
		BattlefieldSavedData.ReplacementJob job = data.nextReadyReplacement(now);
		if (job == null || now - data.lastPlacementTick() < 600L) return;

		Placement placement;
		forceGenerationLoading = true;
		try {
			placement = tryReplacementAttempt(level, data, job);
		} finally {
			forceGenerationLoading = false;
		}
		if (placement == null) {
			int nextAttempt = job.attempts() + 1;
			long retryDelay = nextAttempt % 8 == 0 ? 200L : 20L;
			data.retryReplacement(job.originCenter(), now + retryDelay);
			return;
		}

		ChunkPos centerChunk = new ChunkPos(
				Math.floorDiv(placement.center.getX(), 16),
				Math.floorDiv(placement.center.getZ(), 16));
		BattlefieldSavedData.RegionState prepared = data.prepareReplacementRegion(level, centerChunk);
		if (prepared == null) {
			data.retryReplacement(job.originCenter(), now + 20L);
			return;
		}
		data.activate(prepared.key(), placement.center, placement.relic, placement.culture.id(), placement.brushables);
		data.markPlaced(now);
		data.completeReplacement(job.originCenter());
		PENDING_REGIONS.remove(prepared.key());
		LOADED_REGION_CHUNKS.remove(prepared.key());
		EchoWarrior.LOGGER.info("Generated replacement battlefield ruin at {} for completed site {}; guaranteed relic block at {}.",
				placement.center, BlockPos.of(job.originCenter()), placement.relic);
	}

	private static Placement tryReplacementAttempt(
			ServerLevel level,
			BattlefieldSavedData data,
			BattlefieldSavedData.ReplacementJob job
	) {
		BlockPos origin = BlockPos.of(job.originCenter());
		ReplacementDistance distance = replacementDistance(job.attempts());
		long attemptSeed = job.seed() ^ (long)job.attempts() * 0x9E3779B97F4A7C15L;
		RandomSource random = RandomSource.create(attemptSeed);
		double angle = random.nextDouble() * Math.PI * 2.0;
		double targetDistance = distance.minimum
				+ random.nextDouble() * (distance.maximum - distance.minimum);
		int targetX = origin.getX() + (int)Math.round(Math.cos(angle) * targetDistance);
		int targetZ = origin.getZ() + (int)Math.round(Math.sin(angle) * targetDistance);
		ChunkPos candidateChunk = new ChunkPos(Math.floorDiv(targetX, 16), Math.floorDiv(targetZ, 16));

		int originRegionX = Math.floorDiv(Math.floorDiv(origin.getX(), 16), BattlefieldSavedData.REGION_CHUNKS);
		int originRegionZ = Math.floorDiv(Math.floorDiv(origin.getZ(), 16), BattlefieldSavedData.REGION_CHUNKS);
		int candidateRegionX = Math.floorDiv(candidateChunk.x(), BattlefieldSavedData.REGION_CHUNKS);
		int candidateRegionZ = Math.floorDiv(candidateChunk.z(), BattlefieldSavedData.REGION_CHUNKS);
		if (originRegionX == candidateRegionX && originRegionZ == candidateRegionZ
				|| !data.canUseReplacementRegion(candidateChunk)) return null;

		level.getChunk(candidateChunk.x(), candidateChunk.z());
		double minimumSqr = (double)distance.minimum * distance.minimum;
		double maximumSqr = (double)distance.maximum * distance.maximum;
		for (int localAttempt = 0; localAttempt < 8; localAttempt++) {
			int x = candidateChunk.getMinBlockX() + 2 + random.nextInt(12);
			int z = candidateChunk.getMinBlockZ() + 2 + random.nextInt(12);
			BlockPos horizontal = new BlockPos(x, origin.getY(), z);
			double actualDistance = horizontalDistanceSqr(origin, horizontal);
			if (actualDistance < minimumSqr || actualDistance > maximumSqr) continue;
			int radius = 7 + random.nextInt(4);
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			BlockPos center = new BlockPos(x, surface, z);
			if (!level.getBiome(center).is(ModTags.HAS_BATTLEFIELD_RUIN)
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

	private static ChunkPos chooseLoadedCandidate(
			ServerLevel level,
			long regionKey,
			BattlefieldSavedData.RegionState state,
			long now
	) {
		LongSet loaded = LOADED_REGION_CHUNKS.get(regionKey);
		if (loaded == null || loaded.isEmpty()) {
			ChunkPos saved = ChunkPos.unpack(state.candidateChunk());
			return level.hasChunk(saved.x(), saved.z()) ? saved : null;
		}

		long[] candidates = loaded.toLongArray();
		int start = Math.floorMod(Long.hashCode(state.seed() ^ Math.floorDiv(now, 200L)), candidates.length);
		for (int offset = 0; offset < candidates.length; offset++) {
			ChunkPos candidate = ChunkPos.unpack(candidates[(start + offset) % candidates.length]);
			if (level.hasChunk(candidate.x(), candidate.z())) return candidate;
		}
		return null;
	}

	/** Starts a bounded, tick-spread admin generation job without leaving chunks force-loaded. */
	public static boolean requestForceGeneration(
			ServerLevel level,
			ServerPlayer requester,
			int requested,
			int maximumDistance
	) {
		if (!level.dimension().equals(Level.OVERWORLD) || FORCED_JOBS.containsKey(requester.getUUID())) return false;
		BlockPos origin = requester.blockPosition();
		List<RegionCandidate> regions = forcedRegionCandidates(origin, maximumDistance);
		FORCED_JOBS.put(requester.getUUID(), new ForcedGenerationJob(
				requester.getUUID(), origin, requested, maximumDistance, regions));
		return true;
	}

	private static List<RegionCandidate> forcedRegionCandidates(BlockPos origin, int maximumDistance) {
		int regionSize = BattlefieldSavedData.REGION_CHUNKS * 16;
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
		BattlefieldSavedData data = BattlefieldSavedData.get(level);
		while (job.regionIndex < job.regions.size()) {
			RegionCandidate candidate = job.regions.get(job.regionIndex);
			long key = ChunkPos.pack(candidate.regionX, candidate.regionZ);
			BattlefieldSavedData.RegionState existing = data.region(key);
			if (existing != null && existing.status() == BattlefieldSavedData.Status.ACTIVE) {
				job.nextRegion();
				continue;
			}
			if (job.attemptInRegion == 0) job.attemptedRegions++;
			Placement placement;
		forceGenerationLoading = true;
		try {
			placement = tryForcedAttempt(level, data, candidate, job);
		} finally {
			forceGenerationLoading = false;
		}
			job.attemptInRegion++;
			if (placement != null) {
				activateForcedPlacement(level, data, placement);
				job.generated.add(placement.center);
				job.nextRegion();
				EchoWarrior.LOGGER.info("Force-generated battlefield ruin at {} with guaranteed relic block at {}.",
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

	private static Placement tryForcedAttempt(
			ServerLevel level,
			BattlefieldSavedData data,
			RegionCandidate region,
			ForcedGenerationJob job
	) {
		int firstChunkX = region.regionX * BattlefieldSavedData.REGION_CHUNKS;
		int firstChunkZ = region.regionZ * BattlefieldSavedData.REGION_CHUNKS;
		long seed = level.getSeed() ^ ChunkPos.pack(region.regionX, region.regionZ)
				^ job.origin.asLong() ^ (long)job.attemptInRegion * 0x9E3779B97F4A7C15L;
		RandomSource random = RandomSource.create(seed);
		int chunkX = firstChunkX + random.nextInt(BattlefieldSavedData.REGION_CHUNKS);
		int chunkZ = firstChunkZ + random.nextInt(BattlefieldSavedData.REGION_CHUNKS);
		level.getChunk(chunkX, chunkZ);
		for (int localAttempt = 0; localAttempt < 8; localAttempt++) {
			int x = chunkX * 16 + 2 + random.nextInt(12);
			int z = chunkZ * 16 + 2 + random.nextInt(12);
			if (horizontalDistanceSqr(job.origin, new BlockPos(x, job.origin.getY(), z))
					> (double)job.maximumDistance * job.maximumDistance) continue;
			int radius = 7 + random.nextInt(4);
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			BlockPos center = new BlockPos(x, surface, z);
			if (!level.getBiome(center).is(ModTags.HAS_BATTLEFIELD_RUIN)
					|| !data.isFarEnoughFromKnownSites(center, 512.0)) continue;
			loadSiteChunks(level, x, z, radius);
			if (!isSafeSite(level, data, center, radius)) continue;
			Placement placement = placeSite(level, center, radius, random);
			if (placement != null) return placement;
		}
		return null;
	}

	private static void activateForcedPlacement(
			ServerLevel level,
			BattlefieldSavedData data,
			Placement placement
	) {
		ChunkPos centerChunk = new ChunkPos(
				Math.floorDiv(placement.center.getX(), 16),
				Math.floorDiv(placement.center.getZ(), 16));
		BattlefieldSavedData.RegionState prepared = data.prepareForcedRegion(level, centerChunk);
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
		EchoWarrior.LOGGER.info("Battlefield generation job completed: {}/{} sites across {} checked regions.",
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

	private static Placement findAndPlace(
			ServerLevel level,
			BattlefieldSavedData data,
			BattlefieldSavedData.RegionState state,
			ChunkPos chunk
	) {
		RandomSource random = RandomSource.create(state.seed() ^ chunk.pack() ^ level.getGameTime());
		for (int attempt = 0; attempt < 8; attempt++) {
			int x = chunk.getMinBlockX() + 2 + random.nextInt(12);
			int z = chunk.getMinBlockZ() + 2 + random.nextInt(12);
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			BlockPos center = new BlockPos(x, surface, z);
			int radius = 7 + random.nextInt(4);
			if (!isSafeSite(level, data, center, radius)) continue;
			Placement placed = placeSite(level, center, radius, random);
			if (placed != null) return placed;
		}
		return null;
	}

	private static boolean isSafeSite(ServerLevel level, BattlefieldSavedData data, BlockPos center, int radius) {
		if (!level.getBiome(center).is(ModTags.HAS_BATTLEFIELD_RUIN)
				|| !data.isFarEnoughFromKnownSites(center, 512.0)) return false;
		int minChunkX = Math.floorDiv(center.getX() - radius - 1, 16);
		int maxChunkX = Math.floorDiv(center.getX() + radius + 1, 16);
		int minChunkZ = Math.floorDiv(center.getZ() - radius - 1, 16);
		int maxChunkZ = Math.floorDiv(center.getZ() + radius + 1, 16);
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				if (!level.hasChunk(chunkX, chunkZ)) return false;
				if (PlatformServices.isPlayerModified(level.getChunk(chunkX, chunkZ))) return false;
			}
		}
		if (level.structureManager().hasAnyStructureAt(center)
				|| level.structureManager().hasAnyStructureAt(center.offset(radius, 0, radius))
				|| level.structureManager().hasAnyStructureAt(center.offset(-radius, 0, -radius))) return false;

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
				if (!isNaturalFloor(floorState) || floorState.hasBlockEntity()
						|| !floorState.isFaceSturdy(level, floor, Direction.UP)) return false;
				BlockState above = level.getBlockState(floor.above());
				if (above.is(BlockTags.LOGS) || above.is(BlockTags.LEAVES) || above.hasBlockEntity()
						|| !above.getFluidState().isEmpty() || !isClearable(above)) return false;
			}
		}
		return maximumY - minimumY <= 1;
	}

	private static Placement placeSite(ServerLevel level, BlockPos center, int radius, RandomSource random) {
		BattlefieldCulture culture = BattlefieldCulture.random(random);
		List<BlockPos> floorPositions = new ArrayList<>();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double edgeNoise = random.nextDouble() * 2.4 - 1.2;
				if (Math.sqrt(dx * dx + dz * dz) > radius + edgeNoise) continue;
				int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
						center.getX() + dx, center.getZ() + dz) - 1;
				BlockPos floor = new BlockPos(center.getX() + dx, y, center.getZ() + dz);
				BlockState state = level.getBlockState(floor);
				if (!isNaturalFloor(state)) continue;
				clearVegetation(level, floor.above());
				floorPositions.add(floor);
			}
		}
		Collections.shuffle(floorPositions, new java.util.Random(random.nextLong()));
		int count = Math.min(floorPositions.size(), 6 + random.nextInt(4));
		if (count < 6) return null;
		List<BlockPos> brushables = List.copyOf(floorPositions.subList(0, count));
		BlockPos guaranteed = floorPositions.get(random.nextInt(count));
		for (int index = 0; index < count; index++) {
			BlockPos pos = floorPositions.get(index);
			Block block = suspiciousFor(level.getBlockState(pos));
			level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
			if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable) {
				brushable.setLootTable(pos.equals(guaranteed) ? culture.guaranteedLoot() : culture.commonLoot(), random.nextLong());
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
		if (floor.is(Blocks.GRASS_BLOCK)) return ModBlocks.SUSPICIOUS_GRASS_BLOCK;
		if (floor.is(BlockTags.SAND)) return Blocks.SUSPICIOUS_SAND;
		if (floor.is(Blocks.GRAVEL)) return Blocks.SUSPICIOUS_GRAVEL;
		return ModBlocks.SUSPICIOUS_DIRT;
	}

	public static void onBrushableRemoved(ServerLevel level, BlockPos pos, BlockState removed) {
		if (!level.dimension().equals(Level.OVERWORLD) || !(removed.getBlock() instanceof BrushableBlock)) return;
		BattlefieldSavedData.RemovalResult result = BattlefieldSavedData.get(level)
				.removeBrushableAt(pos, level.getGameTime());
		if (result == null) return;
		EchoCompassSystem.onBattlefieldBlockRemoved(level, result);
		if (result.relicCompleted()) {
			EchoWarrior.LOGGER.info("Completed battlefield ruin by removing its guaranteed relic block at {}; {} archaeological blocks remain.",
					pos, result.remaining().size());
		}
	}

	private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
		double dx = first.getX() - second.getX();
		double dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}

	private record RegionCandidate(int regionX, int regionZ, double distanceSqr) {
	}

	private record ReplacementDistance(int minimum, int maximum) {
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

		private ForcedGenerationJob(
				UUID requester,
				BlockPos origin,
				int requested,
				int maximumDistance,
				List<RegionCandidate> regions
		) {
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

	private record Placement(BlockPos center, BlockPos relic, BattlefieldCulture culture, List<BlockPos> brushables) {
	}
}
