package com.yuriscat.echowarrior.compat.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

public final class BattlefieldSavedData1211 extends SavedData {
    public static final int REGION_CHUNKS = 48;
    private static final Factory<BattlefieldSavedData1211> FACTORY = new Factory<>(
            BattlefieldSavedData1211::new, BattlefieldSavedData1211::load, DataFixTypes.LEVEL);

    private final Map<Long, RegionState> regions = new HashMap<>();
    private final List<CompletedSite> completedSites = new ArrayList<>();
    private final Map<UUID, Long> salvageTrackers = new HashMap<>();
    private final List<ReplacementJob> replacementJobs = new ArrayList<>();
    private final Set<Long> modifiedChunks = new HashSet<>();
    private long lastPlacementTick = -600L;

    public static BattlefieldSavedData1211 get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "echo_warrior_battlefield_sites");
    }

    public RegionState noteNaturalChunkLoad(ServerLevel level, ChunkPos chunkPos, boolean newlyGenerated) {
        int regionX = Math.floorDiv(chunkPos.x, REGION_CHUNKS);
        int regionZ = Math.floorDiv(chunkPos.z, REGION_CHUNKS);
        long key = ChunkPos.asLong(regionX, regionZ);
        long now = level.getGameTime();
        RegionState current = this.regions.get(key);
        if (current == null) {
            long delay = newlyGenerated ? 40L + level.getRandom().nextInt(561)
                    : 2400L + level.getRandom().nextInt(3601);
            current = new RegionState(regionX, regionZ, Status.WAITING, 0L, 0L,
                    now + delay, chunkPos.toLong(), level.getRandom().nextLong(), "", List.of());
            this.regions.put(key, current);
            setDirty();
        } else if (current.status == Status.COOLDOWN && now >= current.readyAt) {
            current = current.waiting(now, chunkPos.toLong(), level.getRandom().nextLong());
            this.regions.put(key, current);
            setDirty();
        } else if (current.status == Status.WAITING && current.candidateChunk != chunkPos.toLong()) {
            current = current.withCandidateChunk(chunkPos.toLong());
            this.regions.put(key, current);
            setDirty();
        }
        return current;
    }

    public RegionState prepareForcedRegion(ServerLevel level, ChunkPos chunkPos) {
        int regionX = Math.floorDiv(chunkPos.x, REGION_CHUNKS);
        int regionZ = Math.floorDiv(chunkPos.z, REGION_CHUNKS);
        long key = ChunkPos.asLong(regionX, regionZ);
        RegionState current = this.regions.get(key);
        if (current != null && current.status == Status.ACTIVE) return current;
        RegionState prepared = new RegionState(regionX, regionZ, Status.WAITING, 0L, 0L,
                level.getGameTime(), chunkPos.toLong(), level.getRandom().nextLong(), "", List.of());
        this.regions.put(key, prepared);
        setDirty();
        return prepared;
    }

    public boolean canUseReplacementRegion(ChunkPos chunkPos) {
        int regionX = Math.floorDiv(chunkPos.x, REGION_CHUNKS);
        int regionZ = Math.floorDiv(chunkPos.z, REGION_CHUNKS);
        RegionState current = this.regions.get(ChunkPos.asLong(regionX, regionZ));
        return current == null || current.status == Status.WAITING;
    }

    public RegionState prepareReplacementRegion(ServerLevel level, ChunkPos chunkPos) {
        int regionX = Math.floorDiv(chunkPos.x, REGION_CHUNKS);
        int regionZ = Math.floorDiv(chunkPos.z, REGION_CHUNKS);
        long key = ChunkPos.asLong(regionX, regionZ);
        RegionState current = this.regions.get(key);
        if (current != null) return current.status == Status.WAITING ? current : null;
        RegionState prepared = new RegionState(regionX, regionZ, Status.WAITING, 0L, 0L,
                level.getGameTime(), chunkPos.toLong(), level.getRandom().nextLong(), "", List.of());
        this.regions.put(key, prepared);
        setDirty();
        return prepared;
    }

    public RegionState region(long key) { return this.regions.get(key); }

    public void defer(long key, long readyAt) {
        RegionState state = this.regions.get(key);
        if (state == null || state.status != Status.WAITING) return;
        this.regions.put(key, state.withReadyAt(readyAt));
        setDirty();
    }

    public void activate(long key, BlockPos center, BlockPos relic, String culture, List<BlockPos> brushables) {
        RegionState state = this.regions.get(key);
        if (state == null) return;
        this.regions.put(key, state.active(center.asLong(), relic.asLong(), culture,
                brushables.stream().map(BlockPos::asLong).toList()));
        setDirty();
    }

    public RemovalResult removeBrushableAt(BlockPos pos, long now) {
        long packed = pos.asLong();
        for (Map.Entry<Long, RegionState> entry : this.regions.entrySet()) {
            RegionState state = entry.getValue();
            if (state.status != Status.ACTIVE || !state.brushables.contains(packed)) continue;
            List<Long> remaining = without(state.brushables, packed);
            boolean relicCompleted = state.relicPos == packed;
            if (relicCompleted) {
                this.completedSites.add(new CompletedSite(state.centerPos, now, remaining));
                while (this.completedSites.size() > 96) this.completedSites.remove(0);
                entry.setValue(state.cooldown(now + 24000L));
                scheduleReplacement(state.centerPos, now);
            } else {
                entry.setValue(state.withBrushables(remaining));
            }
            setDirty();
            return new RemovalResult(BlockPos.of(state.centerPos), pos, relicCompleted,
                    remaining.stream().map(BlockPos::of).toList());
        }
        for (int index = 0; index < this.completedSites.size(); index++) {
            CompletedSite completed = this.completedSites.get(index);
            if (!completed.remainingBrushables.contains(packed)) continue;
            List<Long> remaining = without(completed.remainingBrushables, packed);
            if (remaining.isEmpty()) {
                this.completedSites.remove(index);
                clearSalvageTrackersAt(BlockPos.of(completed.centerPos));
            } else {
                this.completedSites.set(index, new CompletedSite(completed.centerPos, completed.completedAt, remaining));
            }
            setDirty();
            return new RemovalResult(BlockPos.of(completed.centerPos), pos, false,
                    remaining.stream().map(BlockPos::of).toList());
        }
        return null;
    }

    public ActiveSite nearestActive(BlockPos origin, double maximumDistance) {
        double maximumSqr = maximumDistance * maximumDistance;
        ActiveSite best = null;
        double bestDistance = maximumSqr;
        for (RegionState state : this.regions.values()) {
            if (state.status != Status.ACTIVE) continue;
            double distance = horizontalDistanceSqr(origin, BlockPos.of(state.centerPos));
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = state.activeSite();
            }
        }
        return best;
    }

    public ActiveSite nearestKnownActive(BlockPos origin) {
        return nearestActive(origin, 30_000_000.0);
    }

    public ActiveSite findActiveByCenter(long centerPos) {
        for (RegionState state : this.regions.values()) {
            if (state.status == Status.ACTIVE && state.centerPos == centerPos) return state.activeSite();
        }
        return null;
    }

    public SalvageSite findSalvageByCenter(long centerPos) {
        for (CompletedSite site : this.completedSites) {
            if (site.centerPos == centerPos && !site.remainingBrushables.isEmpty()) return site.salvageSite();
        }
        return null;
    }

    public List<ActiveSite> activeSites() {
        List<ActiveSite> sites = new ArrayList<>();
        for (RegionState state : this.regions.values()) {
            if (state.status == Status.ACTIVE) sites.add(state.activeSite());
        }
        return List.copyOf(sites);
    }

    public List<SalvageSite> salvageSites() {
        return this.completedSites.stream().filter(site -> !site.remainingBrushables.isEmpty())
                .map(CompletedSite::salvageSite).toList();
    }

    public void markPlayerModified(ChunkPos pos) {
        if (this.modifiedChunks.add(pos.toLong())) setDirty();
    }

    public boolean isPlayerModified(ChunkPos pos) { return this.modifiedChunks.contains(pos.toLong()); }

    public OptionalLong salvageCenter(UUID playerId) {
        Long center = this.salvageTrackers.get(playerId);
        return center == null ? OptionalLong.empty() : OptionalLong.of(center);
    }

    public void setSalvageTracker(UUID playerId, BlockPos center) {
        Long previous = this.salvageTrackers.put(playerId, center.asLong());
        if (previous == null || previous.longValue() != center.asLong()) setDirty();
    }

    public void clearSalvageTracker(UUID playerId) {
        if (this.salvageTrackers.remove(playerId) != null) setDirty();
    }

    public void clearSalvageTrackersAt(BlockPos center) {
        long packed = center.asLong();
        if (this.salvageTrackers.entrySet().removeIf(entry -> entry.getValue() == packed)) setDirty();
    }

    public long lastPlacementTick() { return this.lastPlacementTick; }
    public void markPlaced(long now) { this.lastPlacementTick = now; setDirty(); }

    public int count(Status status) {
        int result = 0;
        for (RegionState state : this.regions.values()) if (state.status == status) result++;
        return result;
    }

    public int replacementJobCount() { return this.replacementJobs.size(); }

    public ReplacementJob nextReadyReplacement(long now) {
        ReplacementJob best = null;
        for (ReplacementJob job : this.replacementJobs) {
            if (job.readyAt > now || best != null && job.readyAt >= best.readyAt) continue;
            best = job;
        }
        return best;
    }

    public void retryReplacement(long originCenter, long nextAttemptAt) {
        for (int index = 0; index < this.replacementJobs.size(); index++) {
            ReplacementJob job = this.replacementJobs.get(index);
            if (job.originCenter != originCenter) continue;
            this.replacementJobs.set(index, job.withRetry(nextAttemptAt));
            setDirty();
            return;
        }
    }

    public void completeReplacement(long originCenter) {
        if (this.replacementJobs.removeIf(job -> job.originCenter == originCenter)) setDirty();
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

    @Override
    public CompoundTag save(CompoundTag output, HolderLookup.Provider provider) {
        ListTag regionTags = new ListTag();
        for (RegionState state : this.regions.values()) regionTags.add(state.save());
        output.put("Regions", regionTags);
        ListTag completedTags = new ListTag();
        for (CompletedSite site : this.completedSites) completedTags.add(site.save());
        output.put("CompletedSites", completedTags);
        ListTag trackerTags = new ListTag();
        this.salvageTrackers.forEach((player, center) -> {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Player", player);
            tag.putLong("Center", center);
            trackerTags.add(tag);
        });
        output.put("SalvageTrackers", trackerTags);
        ListTag replacementTags = new ListTag();
        for (ReplacementJob job : this.replacementJobs) replacementTags.add(job.save());
        output.put("ReplacementJobs", replacementTags);
        output.putLongArray("ModifiedChunks", this.modifiedChunks.stream().mapToLong(Long::longValue).toArray());
        output.putLong("LastPlacementTick", this.lastPlacementTick);
        return output;
    }

    private static BattlefieldSavedData1211 load(CompoundTag input, HolderLookup.Provider provider) {
        BattlefieldSavedData1211 data = new BattlefieldSavedData1211();
        ListTag regions = input.getList("Regions", Tag.TAG_COMPOUND);
        for (Tag tag : regions) {
            RegionState state = RegionState.load((CompoundTag)tag);
            data.regions.put(state.key(), state);
        }
        ListTag completed = input.getList("CompletedSites", Tag.TAG_COMPOUND);
        for (Tag tag : completed) data.completedSites.add(CompletedSite.load((CompoundTag)tag));
        ListTag trackers = input.getList("SalvageTrackers", Tag.TAG_COMPOUND);
        for (Tag tag : trackers) {
            CompoundTag tracker = (CompoundTag)tag;
            if (tracker.hasUUID("Player")) data.salvageTrackers.put(tracker.getUUID("Player"), tracker.getLong("Center"));
        }
        ListTag replacements = input.getList("ReplacementJobs", Tag.TAG_COMPOUND);
        for (Tag tag : replacements) data.replacementJobs.add(ReplacementJob.load((CompoundTag)tag));
        for (long chunk : input.getLongArray("ModifiedChunks")) data.modifiedChunks.add(chunk);
        data.lastPlacementTick = input.getLong("LastPlacementTick");
        return data;
    }

    private static List<Long> without(List<Long> positions, long removed) {
        List<Long> result = new ArrayList<>(positions.size());
        for (long position : positions) if (position != removed) result.add(position);
        return List.copyOf(result);
    }

    private void scheduleReplacement(long originCenter, long now) {
        for (ReplacementJob job : this.replacementJobs) if (job.originCenter == originCenter) return;
        this.replacementJobs.add(new ReplacementJob(originCenter, now,
                mix(originCenter ^ now * 0x9E3779B97F4A7C15L), 0));
    }

    private static long mix(long value) {
        value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
        value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    public enum Status { WAITING, ACTIVE, COOLDOWN }

    public record ActiveSite(BlockPos center, BlockPos relic, String culture, List<BlockPos> brushables) {
        public ActiveSite { brushables = List.copyOf(brushables); }
    }

    public record SalvageSite(BlockPos center, List<BlockPos> remaining) {
        public SalvageSite { remaining = List.copyOf(remaining); }
    }

    public record RemovalResult(BlockPos center, BlockPos removed, boolean relicCompleted, List<BlockPos> remaining) {
        public RemovalResult { remaining = List.copyOf(remaining); }
    }

    public record ReplacementJob(long originCenter, long readyAt, long seed, int attempts) {
        private ReplacementJob withRetry(long nextAttemptAt) {
            return new ReplacementJob(this.originCenter, nextAttemptAt, this.seed, this.attempts + 1);
        }
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("OriginCenter", this.originCenter);
            tag.putLong("ReadyAt", this.readyAt);
            tag.putLong("Seed", this.seed);
            tag.putInt("Attempts", this.attempts);
            return tag;
        }
        private static ReplacementJob load(CompoundTag tag) {
            return new ReplacementJob(tag.getLong("OriginCenter"), tag.getLong("ReadyAt"),
                    tag.getLong("Seed"), tag.getInt("Attempts"));
        }
    }

    private record CompletedSite(long centerPos, long completedAt, List<Long> remainingBrushables) {
        private CompletedSite { remainingBrushables = List.copyOf(remainingBrushables); }
        private SalvageSite salvageSite() {
            return new SalvageSite(BlockPos.of(this.centerPos), this.remainingBrushables.stream().map(BlockPos::of).toList());
        }
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Center", this.centerPos);
            tag.putLong("CompletedAt", this.completedAt);
            tag.putLongArray("Brushables", this.remainingBrushables.stream().mapToLong(Long::longValue).toArray());
            return tag;
        }
        private static CompletedSite load(CompoundTag tag) {
            return new CompletedSite(tag.getLong("Center"), tag.getLong("CompletedAt"),
                    java.util.Arrays.stream(tag.getLongArray("Brushables")).boxed().toList());
        }
    }

    public record RegionState(int regionX, int regionZ, Status status, long centerPos, long relicPos,
                              long readyAt, long candidateChunk, long seed, String culture, List<Long> brushables) {
        public RegionState { brushables = List.copyOf(brushables); }
        public long key() { return ChunkPos.asLong(this.regionX, this.regionZ); }
        public RegionState withCandidateChunk(long candidate) {
            return new RegionState(regionX, regionZ, status, centerPos, relicPos, readyAt, candidate, seed, culture, brushables);
        }
        public RegionState withReadyAt(long tick) {
            return new RegionState(regionX, regionZ, status, centerPos, relicPos, tick, candidateChunk, seed, culture, brushables);
        }
        public RegionState withBrushables(List<Long> positions) {
            return new RegionState(regionX, regionZ, status, centerPos, relicPos, readyAt, candidateChunk, seed, culture, positions);
        }
        public RegionState waiting(long now, long candidate, long newSeed) {
            return new RegionState(regionX, regionZ, Status.WAITING, 0L, 0L, now, candidate, newSeed, "", List.of());
        }
        public RegionState active(long center, long relic, String cultureId, List<Long> positions) {
            return new RegionState(regionX, regionZ, Status.ACTIVE, center, relic, Long.MAX_VALUE,
                    candidateChunk, seed, cultureId, positions);
        }
        public RegionState cooldown(long until) {
            return new RegionState(regionX, regionZ, Status.COOLDOWN, centerPos, relicPos, until,
                    candidateChunk, seed, culture, List.of());
        }
        public ActiveSite activeSite() {
            return new ActiveSite(BlockPos.of(centerPos), BlockPos.of(relicPos), culture,
                    brushables.stream().map(BlockPos::of).toList());
        }
        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("RegionX", regionX);
            tag.putInt("RegionZ", regionZ);
            tag.putString("Status", status.name());
            tag.putLong("Center", centerPos);
            tag.putLong("Relic", relicPos);
            tag.putLong("ReadyAt", readyAt);
            tag.putLong("CandidateChunk", candidateChunk);
            tag.putLong("Seed", seed);
            tag.putString("Culture", culture);
            tag.putLongArray("Brushables", brushables.stream().mapToLong(Long::longValue).toArray());
            return tag;
        }
        private static RegionState load(CompoundTag tag) {
            Status status;
            try { status = Status.valueOf(tag.getString("Status")); }
            catch (IllegalArgumentException ignored) { status = Status.WAITING; }
            return new RegionState(tag.getInt("RegionX"), tag.getInt("RegionZ"), status,
                    tag.getLong("Center"), tag.getLong("Relic"), tag.getLong("ReadyAt"),
                    tag.getLong("CandidateChunk"), tag.getLong("Seed"), tag.getString("Culture"),
                    java.util.Arrays.stream(tag.getLongArray("Brushables")).boxed().toList());
        }
    }
}
