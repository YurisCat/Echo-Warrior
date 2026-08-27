package com.yuriscat.echowarrior.world;

import com.mojang.serialization.Codec;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModBlocks;
import com.yuriscat.echowarrior.ModTags;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BattlefieldSystem {
	public static final AttachmentType<Boolean> PLAYER_MODIFIED = AttachmentRegistry.createPersistent(
			EchoWarrior.id("player_modified_chunk"), Codec.BOOL);
	private static final ResourceKey<LootTable> COMMON_LOOT = ResourceKey.create(
			Registries.LOOT_TABLE, EchoWarrior.id("archaeology/battlefield_common"));
	private static final ResourceKey<LootTable> GUARANTEED_LOOT = ResourceKey.create(
			Registries.LOOT_TABLE, EchoWarrior.id("archaeology/battlefield_guaranteed_relic"));
	private static final LongSet PENDING_REGIONS = new LongOpenHashSet();
	private static int pendingCursor;

	private BattlefieldSystem() {
	}

	public static void initialize() {
		ServerChunkEvents.CHUNK_GENERATE.register((level, chunk) -> noteChunk(level, chunk, true));
		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyGenerated) -> noteChunk(level, chunk, newlyGenerated));
		ServerTickEvents.END_SERVER_TICK.register(BattlefieldSystem::tick);
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel serverLevel) markPlayerModified(serverLevel, pos);
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level instanceof ServerLevel serverLevel) {
				ItemStack used = player.getItemInHand(hand);
				if (used.getItem() instanceof BlockItem || used.getItem() instanceof BucketItem) {
					markPlayerModified(serverLevel, hit.getBlockPos());
				}
			}
			return InteractionResult.PASS;
		});
	}

	private static void noteChunk(ServerLevel level, LevelChunk chunk, boolean newlyGenerated) {
		if (!level.dimension().equals(Level.OVERWORLD)) return;
		BattlefieldSavedData.RegionState state = BattlefieldSavedData.get(level)
				.noteNaturalChunkLoad(level, chunk.getPos(), newlyGenerated);
		if (state.status() == BattlefieldSavedData.Status.WAITING) PENDING_REGIONS.add(state.key());
	}

	private static void markPlayerModified(ServerLevel level, BlockPos pos) {
		level.getChunkAt(pos).setAttached(PLAYER_MODIFIED, true);
	}

	private static void tick(MinecraftServer server) {
		ServerLevel level = server.getLevel(Level.OVERWORLD);
		if (level == null || PENDING_REGIONS.isEmpty() || level.getGameTime() % 20L != 0L) return;
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
			ChunkPos candidateChunk = ChunkPos.unpack(state.candidateChunk());
			if (!level.hasChunk(candidateChunk.x(), candidateChunk.z())) continue;
			Placement placement = findAndPlace(level, data, state, candidateChunk);
			if (placement != null) {
				data.activate(key, placement.center, placement.relic);
				data.markPlaced(now);
				PENDING_REGIONS.remove(key);
				break;
			}
			data.defer(key, now + 200L);
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
				if (level.getChunk(chunkX, chunkZ).getAttachedOrElse(PLAYER_MODIFIED, false)) return false;
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
		BlockPos guaranteed = floorPositions.get(random.nextInt(count));
		for (int index = 0; index < count; index++) {
			BlockPos pos = floorPositions.get(index);
			Block block = suspiciousFor(level.getBlockState(pos));
			level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
			if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable) {
				brushable.setLootTable(pos.equals(guaranteed) ? GUARANTEED_LOOT : COMMON_LOOT, random.nextLong());
			}
		}
		return new Placement(center, guaranteed);
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
		if (BattlefieldSavedData.get(level).completeAt(pos, level.getGameTime())) {
			EchoCompassSystem.invalidateCompletedSite(pos);
		}
	}

	private record Placement(BlockPos center, BlockPos relic) {
	}
}
