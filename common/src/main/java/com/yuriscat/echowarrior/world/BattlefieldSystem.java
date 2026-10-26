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
import net.minecraft.server.level.TicketType;
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
	// FULL chunks only, no block/entity ticking. Timeout is a fallback for abnormal teardown.
	private static final TicketType GENERATION_TICKET = new TicketType(620L, TicketType.FLAG_LOADING);
	private static GenerationAttempt activeAttempt;

	private BattlefieldSystem() {
	}

	public static void initialize() {
		BattlefieldGenerationSelfTest.arm();
	}

	public static void noteChunk(ServerLevel level, LevelChunk chunk, boolean newlyGenerated) {
		if (!level.dimension().equals(Level.OVERWORLD)) return;
		// Suppress only our own temporary loads, never unrelated exploration during an async attempt.
		if (activeAttempt != null && activeAttempt.chunks.owns(chunk.getPos().pack())) return;
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
		ChunkPos pos = chunk.getPos();
		long regionKey = ChunkPos.pack(Math.floorDiv(pos.x(), BattlefieldSavedData.REGION_CHUNKS),
				Math.floorDiv(pos.z(), BattlefieldSavedData.REGION_CHUNKS));
		LongSet loaded = LOADED_REGION_CHUNKS.get(regionKey);
		if (loaded == null) return;
		loaded.remove(pos.pack());
		if (loaded.isEmpty()) {
			LOADED_REGION_CHUNKS.remove(regionKey);
			PENDING_REGIONS.remove(regionKey);
		}
	}

	public static void markPlayerModified(ServerLevel level, BlockPos pos) {
		LevelChunk chunk = level.getChunkSource().getChunkNow(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getZ(), 16));
		if (chunk != null) PlatformServices.markPlayerModified(chunk);
	}

	public static void tick(MinecraftServer server) {
		BattlefieldGenerationSelfTest.tick(server);
		ServerLevel level = server.getLevel(Level.OVERWORLD);
		if (level == null) return;
		long now = level.getGameTime();
		if (activeAttempt != null) {
			tickPreparedGeneration(server, level, now);
			return;
		}
		tickForcedGeneration(server, level);
		if (activeAttempt != null) return;
		if (now % 20L != 0L) return;
		BattlefieldSavedData data = BattlefieldSavedData.get(level);
		tickReplacementGeneration(level, data, now);
		if (activeAttempt != null || PENDING_REGIONS.isEmpty() || now - data.lastPlacementTick() < 600L) return;

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
			EchoWarrior.LOGGER.info("Generated battlefield ruin at {} with guaranteed relic block at {}.",
					placement.center, placement.relic);
			break;
		}
	}

	public static void clear() {
		if (activeAttempt != null) {
			activeAttempt.chunks.close();
			activeAttempt = null;
		}
		PENDING_REGIONS.clear();
		LOADED_REGION_CHUNKS.clear();
		FORCED_JOBS.clear();
		pendingCursor = 0;
	}

	private static void tickReplacementGeneration(ServerLevel level, BattlefieldSavedData data, long now) {
		BattlefieldSavedData.ReplacementJob job = data.nextReadyReplacement(now);
		if (job == null || now - data.lastPlacementTick() < 600L) return;
		BlockPos origin = BlockPos.of(job.originCenter());
		// This bounded region lookup needs no chunks. Crowded old worlds can keep their jobs dormant.
		if (!hasReplacementRegion(data, origin)) {
			data.retryReplacement(job.originCenter(), now + 1200L);
			return;
		}
		ReplacementDistance distance = replacementDistance(job.attempts());
		RandomSource random = RandomSource.create(job.seed() ^ (long)job.attempts() * 0x9E3779B97F4A7C15L);
		double angle = random.nextDouble() * Math.PI * 2.0;
		double targetDistance = distance.minimum + random.nextDouble() * (distance.maximum - distance.minimum);
		int targetX = origin.getX() + (int)Math.round(Math.cos(angle) * targetDistance);
		int targetZ = origin.getZ() + (int)Math.round(Math.sin(angle) * targetDistance);
		ChunkPos candidate = new ChunkPos(Math.floorDiv(targetX, 16), Math.floorDiv(targetZ, 16));
		if (regionKey(candidate) == regionKey(new ChunkPos(Math.floorDiv(origin.getX(), 16), Math.floorDiv(origin.getZ(), 16))) || !data.canUseReplacementRegion(candidate)) {
			retryReplacement(data, job, now);
			return;
		}
		activeAttempt = new GenerationAttempt(level, candidate, origin, distance.minimum, distance.maximum,
				random, job, null, now);
	}

	private static long regionKey(ChunkPos chunk) {
		return ChunkPos.pack(Math.floorDiv(chunk.x(), BattlefieldSavedData.REGION_CHUNKS),
				Math.floorDiv(chunk.z(), BattlefieldSavedData.REGION_CHUNKS));
	}

	private static boolean hasReplacementRegion(BattlefieldSavedData data, BlockPos origin) {
		int size = BattlefieldSavedData.REGION_CHUNKS * 16;
		// Include the candidate chunk's local offset and rounding at the outer search boundary.
		int reach = REPLACEMENT_FALLBACK_MAX_DISTANCE + 32;
		long originRegion = regionKey(new ChunkPos(Math.floorDiv(origin.getX(), 16), Math.floorDiv(origin.getZ(), 16)));
		for (int rx = Math.floorDiv(origin.getX() - reach, size); rx <= Math.floorDiv(origin.getX() + reach, size); rx++) {
			for (int rz = Math.floorDiv(origin.getZ() - reach, size); rz <= Math.floorDiv(origin.getZ() + reach, size); rz++) {
				if (ChunkPos.pack(rx, rz) != originRegion && data.canUseReplacementRegion(
						new ChunkPos(rx * BattlefieldSavedData.REGION_CHUNKS, rz * BattlefieldSavedData.REGION_CHUNKS))) return true;
			}
		}
		return false;
	}

	private static void retryReplacement(BattlefieldSavedData data, BattlefieldSavedData.ReplacementJob job, long now) {
		// Persist the same job and seed. Failed terrain searches become less frequent, capped at ten seconds.
		long delay = Math.min(200L, 40L << Math.min(3, job.attempts() / 8));
		data.retryReplacement(job.originCenter(), now + delay);
	}

	private static void tickPreparedGeneration(MinecraftServer server, ServerLevel level, long now) {
		GenerationAttempt attempt = activeAttempt;
		if (attempt.level != level) {
			clear();
			return;
		}
		boolean finished = false;
		try {
			BattlefieldChunkPreparation.State state = attempt.chunks.tick(now);
			if (state == BattlefieldChunkPreparation.State.WAITING) return;
			finished = true;
			BattlefieldSavedData data = BattlefieldSavedData.get(level);
			Placement placement = state == BattlefieldChunkPreparation.State.READY
					? tryPreparedAttempt(level, data, attempt) : null;
			if (attempt.replacement != null) {
				finishReplacement(level, data, attempt.replacement, placement, now);
			} else {
				finishForcedAttempt(server, level, data, attempt.forced, placement);
			}
		} catch (RuntimeException | Error failure) {
			finished = true;
			throw failure;
		} finally {
			if (finished) {
				attempt.chunks.close();
				activeAttempt = null;
			}
		}
	}

	private static Placement tryPreparedAttempt(ServerLevel level, BattlefieldSavedData data, GenerationAttempt attempt) {
		BattlefieldSavedData.RegionState region = data.region(regionKey(attempt.candidate));
		if (attempt.replacement != null ? !data.canUseReplacementRegion(attempt.candidate)
				: region != null && region.status() == BattlefieldSavedData.Status.ACTIVE) return null;
		RandomSource random = attempt.random;
		for (int localAttempt = 0; localAttempt < 8; localAttempt++) {
			int x = attempt.candidate.getMinBlockX() + 2 + random.nextInt(12);
			int z = attempt.candidate.getMinBlockZ() + 2 + random.nextInt(12);
			double distance = horizontalDistanceSqr(attempt.origin, new BlockPos(x, attempt.origin.getY(), z));
			if (distance < attempt.minimumSqr || distance > attempt.maximumSqr) continue;
			int radius = 7 + random.nextInt(4);
			int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			BlockPos center = new BlockPos(x, surface, z);
			if (!isSafeSite(level, data, center, radius)) continue;
			Placement placement = placeSite(level, center, radius, random);
			if (placement != null) return placement;
		}
		return null;
	}

	private static void finishReplacement(ServerLevel level, BattlefieldSavedData data,
										BattlefieldSavedData.ReplacementJob job, Placement placement, long now) {
		if (placement == null) {
			retryReplacement(data, job, now);
			return;
		}
		BattlefieldSavedData.RegionState prepared = data.prepareReplacementRegion(level, new ChunkPos(Math.floorDiv(placement.center.getX(), 16), Math.floorDiv(placement.center.getZ(), 16)));
		data.activate(prepared.key(), placement.center, placement.relic, placement.culture.id(), placement.brushables);
		data.markPlaced(now);
		data.completeReplacement(job.originCenter());
		PENDING_REGIONS.remove(prepared.key());
		LOADED_REGION_CHUNKS.remove(prepared.key());
		EchoWarrior.LOGGER.info(
				"Generated replacement battlefield ruin at {} for completed site {}; guaranteed relic block at {}.",
				placement.center, BlockPos.of(job.originCenter()), placement.relic);
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
		if (FORCED_JOBS.isEmpty() || level.getGameTime() % 20L != 0L) return;
		ForcedGenerationJob job = FORCED_JOBS.values().iterator().next();
		BattlefieldSavedData data = BattlefieldSavedData.get(level);
		if (job.regionIndex >= job.regions.size()) {
			finishForcedGeneration(server, job);
			return;
		}
		RegionCandidate candidate = job.regions.get(job.regionIndex);
		BattlefieldSavedData.RegionState existing = data.region(ChunkPos.pack(candidate.regionX, candidate.regionZ));
		if (existing != null && existing.status() == BattlefieldSavedData.Status.ACTIVE) {
			job.nextRegion();
			return;
		}
		if (job.attemptInRegion == 0) job.attemptedRegions++;
		long seed = level.getSeed() ^ ChunkPos.pack(candidate.regionX, candidate.regionZ)
				^ job.origin.asLong() ^ (long)job.attemptInRegion * 0x9E3779B97F4A7C15L;
		RandomSource random = RandomSource.create(seed);
		ChunkPos chunk = new ChunkPos(candidate.regionX * BattlefieldSavedData.REGION_CHUNKS
				+ random.nextInt(BattlefieldSavedData.REGION_CHUNKS),
				candidate.regionZ * BattlefieldSavedData.REGION_CHUNKS + random.nextInt(BattlefieldSavedData.REGION_CHUNKS));
		activeAttempt = new GenerationAttempt(level, chunk, job.origin, 0, job.maximumDistance,
				random, null, job, level.getGameTime());
	}

	private static void finishForcedAttempt(MinecraftServer server, ServerLevel level, BattlefieldSavedData data,
											ForcedGenerationJob job, Placement placement) {
		job.attemptInRegion++;
		if (placement != null) {
			activateForcedPlacement(level, data, placement);
			job.generated.add(placement.center);
			job.nextRegion();
			EchoWarrior.LOGGER.info(
					"Force-generated battlefield ruin at {} with guaranteed relic block at {}.",
					placement.center, placement.relic);
			ServerPlayer requester = server.getPlayerList().getPlayer(job.requester);
			if (requester != null) requester.sendSystemMessage(Component.literal("战场遗迹生成进度："
					+ job.generated.size() + "/" + job.requested + "，中心 " + placement.center.toShortString() + "。"));
		} else if (job.attemptInRegion >= FORCED_ATTEMPTS_PER_REGION) {
			job.nextRegion();
		}
		if (job.generated.size() >= job.requested || job.regionIndex >= job.regions.size()) finishForcedGeneration(server, job);
	}

	private static void activateForcedPlacement(ServerLevel level, BattlefieldSavedData data,
												Placement placement) {
		ChunkPos centerChunk = new ChunkPos(Math.floorDiv(placement.center.getX(), 16),
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
		EchoWarrior.LOGGER.info("battlefield generation job completed: {}/{} sites across {} checked regions.",
				job.generated.size(), job.requested, job.attemptedRegions);
	}





	private static ChunkPos chooseLoadedCandidate(ServerLevel level, long regionKey,
												BattlefieldSavedData.RegionState state, long now) {
		LongSet loaded = LOADED_REGION_CHUNKS.get(regionKey);
		if (loaded == null || loaded.isEmpty()) {
			ChunkPos saved = ChunkPos.unpack(state.candidateChunk());
			return level.getChunkSource().getChunkNow(saved.x(), saved.z()) != null ? saved : null;
		}
		long[] candidates = loaded.toLongArray();
		int start = Math.floorMod(Long.hashCode(state.seed() ^ Math.floorDiv(now, 200L)), candidates.length);
		for (int offset = 0; offset < candidates.length; offset++) {
			ChunkPos candidate = ChunkPos.unpack(candidates[(start + offset) % candidates.length]);
			if (level.getChunkSource().getChunkNow(candidate.x(), candidate.z()) != null) return candidate;
		}
		return null;
	}

	private static Placement findAndPlace(ServerLevel level, BattlefieldSavedData data,
										BattlefieldSavedData.RegionState state, ChunkPos chunk) {
		if (level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) == null) return null;
		RandomSource random = RandomSource.create(state.seed() ^ chunk.pack() ^ level.getGameTime());
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

	private static boolean isSafeSite(ServerLevel level, BattlefieldSavedData data, BlockPos center, int radius) {
		int minChunkX = Math.floorDiv(center.getX() - radius - 1, 16);
		int maxChunkX = Math.floorDiv(center.getX() + radius + 1, 16);
		int minChunkZ = Math.floorDiv(center.getZ() - radius - 1, 16);
		int maxChunkZ = Math.floorDiv(center.getZ() + radius + 1, 16);
		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				LevelChunk loaded = level.getChunkSource().getChunkNow(chunkX, chunkZ);
				if (loaded == null || PlatformServices.isPlayerModified(loaded)) return false;
			}
		}
		if (!level.getBiome(center).is(ModTags.HAS_BATTLEFIELD_RUIN)
				|| !data.isFarEnoughFromKnownSites(center, 512.0)) return false;
		if (level.structureManager().hasAnyStructureAt(center)
				|| level.structureManager().hasAnyStructureAt(center.offset(radius, 0, radius))
				|| level.structureManager().hasAnyStructureAt(center.offset(-radius, 0, -radius))) return false;

		int minimumY = Integer.MAX_VALUE;
		int maximumY = Integer.MIN_VALUE;
		for (int dx = -radius; dx <= radius; dx += 2) {
			for (int dz = -radius; dz <= radius; dz += 2) {
				if (dx * dx + dz * dz > radius * radius) continue;
				BlockPos floor = naturalSurface(level, center.getX() + dx, center.getZ() + dz);
				int y = floor.getY();
				minimumY = Math.min(minimumY, y);
				maximumY = Math.max(maximumY, y);
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
				BlockPos floor = naturalSurface(level, center.getX() + dx, center.getZ() + dz);
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

	// Thick snow layers can enter the heightmap; they are cover, never an excavation floor.
	private static BlockPos naturalSurface(ServerLevel level, int x, int z) {
		BlockPos surface = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
		return level.getBlockState(surface).is(Blocks.SNOW) ? surface.below() : surface;
	}

	private static void clearVegetation(ServerLevel level, BlockPos pos) {
		for (int offset = 0; offset < 3; offset++) {
			BlockPos target = pos.above(offset);
			BlockState state = level.getBlockState(target);
			if (state.isAir()) continue;
			// Snow is replaceable in vanilla, but is not vegetation. Preserve its exact layer count.
			if (state.is(Blocks.SNOW)) break;
			if (!isClearable(state) || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
					|| state.hasBlockEntity() || !state.getFluidState().isEmpty()) break;
			level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	private static boolean isClearable(BlockState state) {
		return state.isAir() || state.is(Blocks.SNOW) || state.canBeReplaced() || state.is(BlockTags.REPLACEABLE)
				|| state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS);
	}

	private static boolean isNaturalFloor(BlockState state) {
		return state.is(Blocks.GRASS_BLOCK) || state.is(BlockTags.DIRT)
				|| state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL);
	}

	private static Block suspiciousFor(BlockState floor) {
		if (floor.is(Blocks.GRASS_BLOCK)) return ModBlocks.SUSPICIOUS_GRASS_BLOCK;
		if (floor.is(Blocks.RED_SAND)) return ModBlocks.SUSPICIOUS_RED_SAND;
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

	private static final class GenerationAttempt {
		private final ServerLevel level;
		private final ChunkPos candidate;
		private final BlockPos origin;
		private final double minimumSqr;
		private final double maximumSqr;
		private final RandomSource random;
		private final BattlefieldSavedData.ReplacementJob replacement;
		private final ForcedGenerationJob forced;
		private final BattlefieldChunkPreparation chunks;

		private GenerationAttempt(ServerLevel level, ChunkPos candidate, BlockPos origin, int minimum, int maximum,
								RandomSource random, BattlefieldSavedData.ReplacementJob replacement,
								ForcedGenerationJob forced, long now) {
			this.level = level;
			this.candidate = candidate;
			this.origin = origin;
			this.minimumSqr = (double)minimum * minimum;
			this.maximumSqr = (double)maximum * maximum;
			this.random = random;
			this.replacement = replacement;
			this.forced = forced;
			List<Long> footprint = new ArrayList<>();
			footprint.add(candidate.pack());
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx != 0 || dz != 0) footprint.add(ChunkPos.pack(candidate.x() + dx, candidate.z() + dz));
				}
			}
			this.chunks = new BattlefieldChunkPreparation(new BattlefieldChunkPreparation.Backend() {
				public void retain(long packed) {
					ChunkPos chunk = ChunkPos.unpack(packed);
					level.getChunkSource().addTicketWithRadius(GENERATION_TICKET, chunk, 0);
				}
				public boolean ready(long packed) {
					ChunkPos chunk = ChunkPos.unpack(packed);
					return level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) != null;
				}
				public void release(long packed) {
					ChunkPos chunk = ChunkPos.unpack(packed);
					level.getChunkSource().removeTicketWithRadius(GENERATION_TICKET, chunk, 0);
				}
			}, footprint, now);
		}
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

	private record Placement(BlockPos center, BlockPos relic, BattlefieldCulture culture,
							List<BlockPos> brushables) {
	}
}
