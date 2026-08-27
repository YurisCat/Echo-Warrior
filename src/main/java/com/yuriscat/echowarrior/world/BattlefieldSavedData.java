package com.yuriscat.echowarrior.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

public final class BattlefieldSavedData extends SavedData {
	public static final int REGION_CHUNKS = 48;
	private static final Codec<BattlefieldSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			RegionState.CODEC.listOf().optionalFieldOf("regions", List.of())
					.forGetter(data -> List.copyOf(data.regions.values())),
			CompletedSite.CODEC.listOf().optionalFieldOf("completed_sites", List.of())
					.forGetter(data -> data.completedSites),
			SalvageTracker.CODEC.listOf().optionalFieldOf("salvage_trackers", List.of())
					.forGetter(data -> data.salvageTrackers.entrySet().stream()
							.map(entry -> new SalvageTracker(entry.getKey(), entry.getValue())).toList()),
			Codec.LONG.optionalFieldOf("last_placement_tick", -600L).forGetter(data -> data.lastPlacementTick)
	).apply(instance, BattlefieldSavedData::new));
	private static final SavedDataType<BattlefieldSavedData> TYPE = new SavedDataType<>(
			EchoWarrior.id("battlefield_sites"), BattlefieldSavedData::new, CODEC, DataFixTypes.LEVEL);

	private final Map<Long, RegionState> regions = new HashMap<>();
	private final List<CompletedSite> completedSites = new ArrayList<>();
	private final Map<UUID, Long> salvageTrackers = new HashMap<>();
	private long lastPlacementTick = -600L;

	public BattlefieldSavedData() {
	}

	private BattlefieldSavedData(
			List<RegionState> regions,
			List<CompletedSite> completedSites,
			List<SalvageTracker> salvageTrackers,
			long lastPlacementTick
	) {
		for (RegionState state : regions) this.regions.put(state.key(), state);
		this.completedSites.addAll(completedSites);
		for (SalvageTracker tracker : salvageTrackers) {
			this.salvageTrackers.put(tracker.playerId(), tracker.centerPos());
		}
		this.lastPlacementTick = lastPlacementTick;
	}

	public static BattlefieldSavedData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public RegionState noteNaturalChunkLoad(ServerLevel level, ChunkPos chunkPos, boolean newlyGenerated) {
		int regionX = Math.floorDiv(chunkPos.x(), REGION_CHUNKS);
		int regionZ = Math.floorDiv(chunkPos.z(), REGION_CHUNKS);
		long key = ChunkPos.pack(regionX, regionZ);
		long now = level.getGameTime();
		RegionState current = this.regions.get(key);
		if (current == null) {
			long delay = newlyGenerated
					? 40L + level.getRandom().nextInt(561)
					: 2400L + level.getRandom().nextInt(3601);
			current = new RegionState(regionX, regionZ, Status.WAITING, BlockPos.ZERO.asLong(),
					BlockPos.ZERO.asLong(), now + delay, chunkPos.pack(), level.getRandom().nextLong(), List.of());
			this.regions.put(key, current);
			setDirty();
		} else if (current.status == Status.COOLDOWN && now >= current.readyAt) {
			current = current.waiting(now, chunkPos.pack(), level.getRandom().nextLong());
			this.regions.put(key, current);
			setDirty();
		} else if (current.status == Status.WAITING && current.candidateChunk != chunkPos.pack()) {
			current = current.withCandidateChunk(chunkPos.pack());
			this.regions.put(key, current);
			setDirty();
		}
		return current;
	}

	public RegionState prepareForcedRegion(ServerLevel level, ChunkPos chunkPos) {
		int regionX = Math.floorDiv(chunkPos.x(), REGION_CHUNKS);
		int regionZ = Math.floorDiv(chunkPos.z(), REGION_CHUNKS);
		long key = ChunkPos.pack(regionX, regionZ);
		RegionState current = this.regions.get(key);
		if (current != null && current.status == Status.ACTIVE) return current;
		RegionState prepared = new RegionState(regionX, regionZ, Status.WAITING, BlockPos.ZERO.asLong(),
				BlockPos.ZERO.asLong(), level.getGameTime(), chunkPos.pack(), level.getRandom().nextLong(), List.of());
		this.regions.put(key, prepared);
		setDirty();
		return prepared;
	}

	public RegionState region(long key) {
		return this.regions.get(key);
	}

	public void defer(long key, long readyAt) {
		RegionState state = this.regions.get(key);
		if (state == null || state.status != Status.WAITING) return;
		this.regions.put(key, state.withReadyAt(readyAt));
		setDirty();
	}

	public void activate(long key, BlockPos center, BlockPos relic, List<BlockPos> brushables) {
		RegionState state = this.regions.get(key);
		if (state == null) return;
		this.regions.put(key, state.active(center.asLong(), relic.asLong(), toLongList(brushables)));
		setDirty();
	}

	public RemovalResult removeBrushableAt(BlockPos pos, long now) {
		long packed = pos.asLong();
		for (Map.Entry<Long, RegionState> entry : this.regions.entrySet()) {
			RegionState state = entry.getValue();
			if (state.status != Status.ACTIVE) continue;
			List<Long> current = state.effectiveBrushables();
			if (!current.contains(packed)) continue;
			List<Long> remaining = without(current, packed);
			boolean relicCompleted = state.relicPos == packed;
			if (relicCompleted) {
				this.completedSites.add(new CompletedSite(state.centerPos, now, remaining));
				while (this.completedSites.size() > 96) this.completedSites.removeFirst();
				entry.setValue(state.cooldown(now + 24000L));
			} else {
				entry.setValue(state.withBrushables(remaining));
			}
			setDirty();
			return new RemovalResult(BlockPos.of(state.centerPos), pos, relicCompleted, toBlockPosList(remaining));
		}

		for (int index = 0; index < this.completedSites.size(); index++) {
			CompletedSite completed = this.completedSites.get(index);
			if (!completed.remainingBrushables.contains(packed)) continue;
			List<Long> remaining = without(completed.remainingBrushables, packed);
			this.completedSites.set(index, completed.withRemaining(remaining));
			setDirty();
			return new RemovalResult(BlockPos.of(completed.centerPos), pos, false, toBlockPosList(remaining));
		}
		return null;
	}

	public ActiveSite findActiveByCenter(long centerPos) {
		int regionX = Math.floorDiv(BlockPos.getX(centerPos), REGION_CHUNKS * 16);
		int regionZ = Math.floorDiv(BlockPos.getZ(centerPos), REGION_CHUNKS * 16);
		RegionState state = this.regions.get(ChunkPos.pack(regionX, regionZ));
		return state != null && state.status == Status.ACTIVE && state.centerPos == centerPos
				? state.activeSite()
				: null;
	}

	public SalvageSite findSalvageByCenter(long centerPos) {
		for (CompletedSite completed : this.completedSites) {
			if (completed.centerPos == centerPos && !completed.remainingBrushables.isEmpty()) {
				return completed.salvageSite();
			}
		}
		return null;
	}

	public ActiveSite nearestActive(BlockPos origin, double maximumDistance) {
		double maximumSqr = maximumDistance * maximumDistance;
		ActiveSite best = null;
		double bestDistance = maximumSqr;
		int regionSize = REGION_CHUNKS * 16;
		int originRegionX = Math.floorDiv(origin.getX(), regionSize);
		int originRegionZ = Math.floorDiv(origin.getZ(), regionSize);
		int regionRadius = Math.max(1, (int)Math.ceil(maximumDistance / regionSize));
		for (int regionX = originRegionX - regionRadius; regionX <= originRegionX + regionRadius; regionX++) {
			for (int regionZ = originRegionZ - regionRadius; regionZ <= originRegionZ + regionRadius; regionZ++) {
				RegionState state = this.regions.get(ChunkPos.pack(regionX, regionZ));
				if (state == null || state.status != Status.ACTIVE) continue;
				BlockPos center = BlockPos.of(state.centerPos);
				double distance = horizontalDistanceSqr(center, origin);
				if (distance <= bestDistance) {
					bestDistance = distance;
					best = state.activeSite();
				}
			}
		}
		return best;
	}

	/** Finds the nearest indexed active site without walking or loading world chunks. */
	public ActiveSite nearestKnownActive(BlockPos origin) {
		ActiveSite best = null;
		double bestDistance = Double.MAX_VALUE;
		for (RegionState state : this.regions.values()) {
			if (state.status != Status.ACTIVE) continue;
			BlockPos center = BlockPos.of(state.centerPos);
			double distance = horizontalDistanceSqr(center, origin);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = state.activeSite();
			}
		}
		return best;
	}

	public int count(Status status) {
		int count = 0;
		for (RegionState state : this.regions.values()) {
			if (state.status == status) count++;
		}
		return count;
	}

	public boolean isFarEnoughFromKnownSites(BlockPos center, double minimumDistance) {
		double minimumSqr = minimumDistance * minimumDistance;
		for (RegionState state : this.regions.values()) {
			if (state.status == Status.ACTIVE
					&& horizontalDistanceSqr(BlockPos.of(state.centerPos), center) < minimumSqr) return false;
		}
		for (CompletedSite completed : this.completedSites) {
			if (horizontalDistanceSqr(BlockPos.of(completed.centerPos), center) < minimumSqr) return false;
		}
		return true;
	}

	public OptionalLong salvageCenter(UUID playerId) {
		Long center = this.salvageTrackers.get(playerId);
		return center == null ? OptionalLong.empty() : OptionalLong.of(center);
	}

	public void setSalvageTracker(UUID playerId, BlockPos center) {
		long packed = center.asLong();
		Long previous = this.salvageTrackers.put(playerId, packed);
		if (previous == null || previous.longValue() != packed) setDirty();
	}

	public void clearSalvageTracker(UUID playerId) {
		if (this.salvageTrackers.remove(playerId) != null) setDirty();
	}

	public void clearSalvageTrackersAt(BlockPos center) {
		long packed = center.asLong();
		if (this.salvageTrackers.entrySet().removeIf(entry -> entry.getValue().longValue() == packed)) setDirty();
	}

	public long lastPlacementTick() {
		return this.lastPlacementTick;
	}

	public void markPlaced(long now) {
		this.lastPlacementTick = now;
		setDirty();
	}

	private static List<Long> without(List<Long> positions, long removed) {
		List<Long> result = new ArrayList<>(Math.max(0, positions.size() - 1));
		for (long position : positions) {
			if (position != removed) result.add(position);
		}
		return List.copyOf(result);
	}

	private static List<Long> toLongList(List<BlockPos> positions) {
		return positions.stream().map(BlockPos::asLong).toList();
	}

	private static List<BlockPos> toBlockPosList(List<Long> positions) {
		return positions.stream().map(BlockPos::of).toList();
	}

	private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
		double dx = first.getX() - second.getX();
		double dz = first.getZ() - second.getZ();
		return dx * dx + dz * dz;
	}

	public enum Status {
		WAITING,
		ACTIVE,
		COOLDOWN
	}

	public record ActiveSite(BlockPos center, BlockPos relic, List<BlockPos> brushables) {
		public ActiveSite {
			brushables = List.copyOf(brushables);
		}
	}

	public record SalvageSite(BlockPos center, List<BlockPos> remaining) {
		public SalvageSite {
			remaining = List.copyOf(remaining);
		}
	}

	public record RemovalResult(
			BlockPos center,
			BlockPos removed,
			boolean relicCompleted,
			List<BlockPos> remaining
	) {
		public RemovalResult {
			remaining = List.copyOf(remaining);
		}
	}

	public record RegionState(
			int regionX,
			int regionZ,
			Status status,
			long centerPos,
			long relicPos,
			long readyAt,
			long candidateChunk,
			long seed,
			List<Long> brushables
	) {
		private static final Codec<Status> STATUS_CODEC = Codec.STRING.xmap(Status::valueOf, Status::name);
		private static final Codec<RegionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("region_x").forGetter(RegionState::regionX),
				Codec.INT.fieldOf("region_z").forGetter(RegionState::regionZ),
				STATUS_CODEC.fieldOf("status").forGetter(RegionState::status),
				Codec.LONG.fieldOf("center").forGetter(RegionState::centerPos),
				Codec.LONG.fieldOf("relic").forGetter(RegionState::relicPos),
				Codec.LONG.fieldOf("ready_at").forGetter(RegionState::readyAt),
				Codec.LONG.fieldOf("candidate_chunk").forGetter(RegionState::candidateChunk),
				Codec.LONG.fieldOf("seed").forGetter(RegionState::seed),
				Codec.LONG.listOf().optionalFieldOf("brushables", List.of()).forGetter(RegionState::brushables)
		).apply(instance, RegionState::new));

		public RegionState {
			brushables = List.copyOf(brushables);
		}

		public long key() {
			return ChunkPos.pack(this.regionX, this.regionZ);
		}

		public RegionState withCandidateChunk(long candidate) {
			return new RegionState(this.regionX, this.regionZ, this.status, this.centerPos, this.relicPos,
					this.readyAt, candidate, this.seed, this.brushables);
		}

		public RegionState withReadyAt(long tick) {
			return new RegionState(this.regionX, this.regionZ, this.status, this.centerPos, this.relicPos,
					tick, this.candidateChunk, this.seed, this.brushables);
		}

		public RegionState withBrushables(List<Long> positions) {
			return new RegionState(this.regionX, this.regionZ, this.status, this.centerPos, this.relicPos,
					this.readyAt, this.candidateChunk, this.seed, positions);
		}

		public RegionState waiting(long now, long candidate, long newSeed) {
			return new RegionState(this.regionX, this.regionZ, Status.WAITING, BlockPos.ZERO.asLong(),
					BlockPos.ZERO.asLong(), now, candidate, newSeed, List.of());
		}

		public RegionState active(long center, long relic, List<Long> positions) {
			return new RegionState(this.regionX, this.regionZ, Status.ACTIVE, center, relic,
					Long.MAX_VALUE, this.candidateChunk, this.seed, positions);
		}

		public RegionState cooldown(long until) {
			return new RegionState(this.regionX, this.regionZ, Status.COOLDOWN, this.centerPos, this.relicPos,
					until, this.candidateChunk, this.seed, List.of());
		}

		public List<Long> effectiveBrushables() {
			if (!this.brushables.isEmpty()) return this.brushables;
			return this.status == Status.ACTIVE
					? List.of(this.relicPos)
					: List.of();
		}

		public ActiveSite activeSite() {
			return new ActiveSite(BlockPos.of(this.centerPos), BlockPos.of(this.relicPos),
					toBlockPosList(effectiveBrushables()));
		}
	}

	private record CompletedSite(long centerPos, long completedAt, List<Long> remainingBrushables) {
		private static final Codec<CompletedSite> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.LONG.fieldOf("center").forGetter(CompletedSite::centerPos),
				Codec.LONG.fieldOf("completed_at").forGetter(CompletedSite::completedAt),
				Codec.LONG.listOf().optionalFieldOf("remaining_brushables", List.of())
						.forGetter(CompletedSite::remainingBrushables)
		).apply(instance, CompletedSite::new));

		private CompletedSite {
			remainingBrushables = List.copyOf(remainingBrushables);
		}

		private CompletedSite withRemaining(List<Long> remaining) {
			return new CompletedSite(this.centerPos, this.completedAt, remaining);
		}

		private SalvageSite salvageSite() {
			return new SalvageSite(BlockPos.of(this.centerPos), toBlockPosList(this.remainingBrushables));
		}
	}

	private record SalvageTracker(UUID playerId, long centerPos) {
		private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
		private static final Codec<SalvageTracker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUID_CODEC.fieldOf("player").forGetter(SalvageTracker::playerId),
				Codec.LONG.fieldOf("center").forGetter(SalvageTracker::centerPos)
		).apply(instance, SalvageTracker::new));
	}
}
