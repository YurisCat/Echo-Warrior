package com.yuriscat.echowarrior.compat.binding;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.item.SummonerData1201;
import com.yuriscat.echowarrior.compat.item.SummonerFuel1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1201;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** World-owned authority; item NBT is only an import on first registration and an output thereafter. */
public final class EchoBindingSavedData1201 extends SavedData {
    public static final String FILE_ID = "echo_warrior_bindings_1201";
    private final Map<UUID, Binding> bindings = new HashMap<>();
    private final Map<UUID, Long> performanceWarnings = new HashMap<>();
    public boolean tryWarning(UUID player, long now) {
        Long last = performanceWarnings.get(player);
        if (last != null && now - last < 3_600_000L) return false;
        performanceWarnings.put(player, now);
        setDirty();
        return true;
    }
    // Not static: opening a second integrated world must not inherit the first world's locations.
    private final SummonerIdentityTracker1201 identities = new SummonerIdentityTracker1201();

    public static EchoBindingSavedData1201 get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                EchoBindingSavedData1201::load, EchoBindingSavedData1201::new, FILE_ID);
    }

    public Binding get(UUID id) { return bindings.get(id); }
    public List<Binding> bindings() { return List.copyOf(bindings.values()); }
    SummonerIdentityTracker1201 identities() { return identities; }

    /** Trusted server inventory only, not arbitrary client-provided ItemStacks. */
    public Binding register(ItemStack stack) {
        UUID id = SummonerData1201.getOrCreateSummonerId(stack);
        Binding existing = bindings.get(id);
        if (existing != null) return existing;
        List<ItemStack> contents = SummonerData1201.contents(stack);
        SummonerData1201.serializeContents(contents); // Validate before creating any world record.
        Binding created = new Binding(id, this::setDirty);
        created.contents = copyContents(contents);
        created.fuel = SummonerData1201.fuel(stack);
        // Local revisions and spirit UUIDs never confer world authority or control.
        created.revision = 1;
        bindings.put(id, created);
        setDirty();
        return created;
    }

    /** Called only after confirmed destruction. Ordinary moves/drop/unload must never call this. */
    public boolean remove(UUID id) {
        Binding binding = bindings.remove(id);
        if (binding == null) return false;
        binding.removed = true; // Invalidate already-held handles as well as entity generation checks.
        identities.forget(id);
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        bindings.values().forEach(binding -> entries.add(binding.save()));
        tag.put("Bindings", entries);
        ListTag warnings = new ListTag();
        performanceWarnings.forEach((player, time) -> {
            CompoundTag warning = new CompoundTag();
            warning.putUUID("Player", player); warning.putLong("Time", time); warnings.add(warning);
        });
        tag.put("PerformanceWarnings", warnings);
        return tag;
    }

    public static EchoBindingSavedData1201 load(CompoundTag tag) {
        EchoBindingSavedData1201 data = new EchoBindingSavedData1201();
        ListTag warnings = tag.getList("PerformanceWarnings", Tag.TAG_COMPOUND);
        for (int index = 0; index < warnings.size(); index++) {
            CompoundTag warning = warnings.getCompound(index);
            if (warning.hasUUID("Player")) data.performanceWarnings.put(warning.getUUID("Player"), warning.getLong("Time"));
        }
        ListTag entries = tag.getList("Bindings", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            if (!entry.hasUUID("Summoner")) continue;
            Binding binding = Binding.load(entry, data::setDirty);
            data.bindings.putIfAbsent(binding.summonerId, binding);
        }
        return data;
    }

    private static List<ItemStack> copyContents(List<ItemStack> contents) {
        return contents.stream().map(ItemStack::copy).toList();
    }

    public static final class Binding {
        private final UUID summonerId;
        private final Runnable dirty;
        private List<ItemStack> contents = java.util.Collections.nCopies(8, ItemStack.EMPTY);
        private int fuel;
        private double fuelFraction;
        private long revision;
        private UUID controllerId;
        private UUID spiritId;
        private long generation;
        private boolean active;
        private boolean removed;
        private long lastFuelTick = Long.MIN_VALUE;
        private SpiritSnapshot snapshot = new SpiritSnapshot("minecraft:overworld", 0, 0, 0,
                0, 0, 0, 0, 300, new CompoundTag());

        private Binding(UUID summonerId, Runnable dirty) {
            this.summonerId = summonerId;
            this.dirty = dirty;
        }

        public UUID summonerId() { return summonerId; }
        public UUID controllerId() { return controllerId; }
        public UUID spiritId() { return spiritId; }
        public long generation() { return generation; }
        public boolean active() { return active && !removed; }
        public long stateRevision() { return revision; }
        public int fuel() { return fuel; }
        public double fuelFraction() { return fuelFraction; }
        public List<ItemStack> contents() { return copyContents(contents); }
        public SpiritSnapshot snapshot() { return snapshot; }
        public ItemStack relic() { return contents.get(SummonerData1201.RELIC_SLOT).copy(); }
        public List<ItemStack> accessories() { return copyContents(contents.subList(0, SummonerData1201.ACCESSORY_SLOTS)); }
        public int activityMode() { return EchoRelicState1201.activityMode(relic()).ordinal(); }
        public int alertMode() { return EchoRelicState1201.alertMode(relic()).ordinal(); }
        public int enabledSkills() { return EchoRelicState1201.enabledSkills(relic()); }
        public long legionCooldownEnd() { return EchoRelicState1201.legionCooldownEnd(relic()); }
        public void transferController(UUID controller) {
            Objects.requireNonNull(controller);
            if (!controller.equals(controllerId)) { changed(); controllerId = controller; }
        }

        /** Combat/skill updates may change only the already-installed relic, never replace its identity. */
        public boolean persistRelic(ItemStack updated) {
            ItemStack current = relic();
            if (removed || current.isEmpty() || current.getItem() != updated.getItem()
                    || !EchoRelicState1201.relicId(current).equals(EchoRelicState1201.relicId(updated))) return false;
            var changedContents = new java.util.ArrayList<>(contents());
            changedContents.set(SummonerData1201.RELIC_SLOT, updated.copy());
            return commitEquipment(revision, changedContents);
        }

        /** Low-level snapshot CAS. Caller must validate authorization and item transfer. */
        public boolean commitContents(long expectedRevision, List<ItemStack> updated) {
            if (removed || expectedRevision != revision) return false;
            ListTag encoded = SummonerData1201.serializeContents(updated);
            if (encoded.equals(SummonerData1201.serializeContents(contents))) return true;
            changed();
            contents = copyContents(updated);
            return true;
        }

        /** Menu transaction: reject illegal contents, invalidate the old spirit on relic identity change. */
        public boolean commitEquipment(long expectedRevision, List<ItemStack> updated) {
            if (removed || expectedRevision != revision) return false;
            SummonerData1201.serializeContents(updated); // Validate size/counts before any change.
            var unique = new java.util.HashSet<net.minecraft.world.item.Item>();
            for (int index = 0; index < SummonerData1201.ACCESSORY_SLOTS; index++) {
                ItemStack stack = updated.get(index);
                if (!stack.isEmpty() && (!(stack.getItem() instanceof com.yuriscat.echowarrior.compat.item.EchoSummonerAccessory1201)
                        || !unique.add(stack.getItem()))) return false;
            }
            ItemStack fuelStack = updated.get(SummonerData1201.FUEL_SLOT);
            if (!fuelStack.isEmpty() && SummonerFuel1201.value(fuelStack) <= 0) return false;
            ItemStack relic = updated.get(SummonerData1201.RELIC_SLOT);
            if (!relic.isEmpty() && !(relic.getItem() instanceof com.yuriscat.echowarrior.compat.item.EchoRelicItem1201)) return false;
            ItemStack oldRelic = contents.get(SummonerData1201.RELIC_SLOT);
            boolean sameRelic = oldRelic.getItem() == relic.getItem()
                    && com.yuriscat.echowarrior.compat.item.EchoRelicState1201.relicId(oldRelic)
                    .equals(com.yuriscat.echowarrior.compat.item.EchoRelicState1201.relicId(relic));
            if (!commitContents(expectedRevision, updated)) return false;
            if (!sameRelic && active) {
                // Part of the contents revision, not a second state change. Entity disposal follows the entity port.
                clearActiveState();
            }
            return true;
        }

        public void setFuel(int amount) {
            ensurePresent();
            int normalized = Math.max(0, Math.min(SummonerData1201.FUEL_CAPACITY, amount));
            if (fuel == normalized) return;
            changed();
            fuel = normalized;
        }

        /** One atomic authority update for both consumed buffer item and gained energy. */
        public boolean convertOneFuel(long gameTick) {
            if (removed || gameTick == lastFuelTick) return false;
            lastFuelTick = gameTick;
            int value = SummonerFuel1201.value(contents.get(SummonerData1201.FUEL_SLOT));
            if (value <= 0 || fuel + value > SummonerData1201.FUEL_CAPACITY) return false;
            List<ItemStack> updated = copyContents(contents);
            updated.get(SummonerData1201.FUEL_SLOT).shrink(1);
            changed();
            contents = updated;
            fuel += value;
            return true;
        }

        public boolean consumeFuel(int amount) {
            if (removed || amount < 0 || fuel < amount) return false;
            setFuel(fuel - amount);
            return true;
        }

        public boolean consumeFractionalFuel(double amount) {
            if (removed || !Double.isFinite(amount) || amount < 0) return false;
            if (amount == 0) return true;
            double total = fuelFraction + amount;
            double whole = Math.floor(total + 1.0E-7);
            if (whole > fuel) return false;
            changed();
            fuel -= (int) whole;
            fuelFraction = Math.max(0, total - whole);
            return true;
        }

        /** Only after a future spawn transaction has actually succeeded. Does not spawn an entity. */
        public long activate(UUID controller, UUID spirit, SpiritSnapshot state) {
            Objects.requireNonNull(controller);
            Objects.requireNonNull(spirit);
            Objects.requireNonNull(state);
            long next = Math.addExact(generation, 1);
            changed();
            controllerId = controller;
            spiritId = spirit;
            snapshot = state;
            active = true;
            generation = next;
            return generation;
        }

        public boolean matches(UUID spirit, long expectedGeneration) {
            return active() && generation == expectedGeneration && spiritId.equals(spirit);
        }

        public boolean track(UUID spirit, long expectedGeneration, SpiritSnapshot state) {
            Objects.requireNonNull(state);
            if (!matches(spirit, expectedGeneration)) return false;
            if (!snapshot.equals(state)) {
                snapshot = state;
                // Movement/health persistence must not invalidate every open equipment menu each tick.
                dirty.run();
            }
            return true;
        }

        public boolean deactivate(UUID spirit, long expectedGeneration) {
            if (!matches(spirit, expectedGeneration)) return false;
            changed();
            clearActiveState();
            return true;
        }

        private void clearActiveState() {
            active = false;
            spiritId = null;
            snapshot = new SpiritSnapshot(snapshot.dimension(), snapshot.x(), snapshot.y(), snapshot.z(),
                    0, 0, 0, 0, 300, new CompoundTag());
        }

        public void mirrorTo(ItemStack stack) {
            ensurePresent();
            SummonerData1201.applyAuthority(stack, summonerId, active ? spiritId : null, fuel, revision, contents);
        }

        private void ensurePresent() {
            if (removed) throw new IllegalStateException("Removed binding handle");
        }

        private void changed() {
            ensurePresent();
            revision = Math.addExact(revision, 1);
            dirty.run();
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Summoner", summonerId);
            tag.put("Contents", SummonerData1201.serializeContents(contents));
            tag.putInt("Fuel", fuel);
            tag.putDouble("FuelFraction", fuelFraction);
            tag.putLong("StateRevision", revision);
            if (controllerId != null) tag.putUUID("Controller", controllerId);
            if (spiritId != null) tag.putUUID("Spirit", spiritId);
            tag.putLong("Generation", generation);
            tag.putBoolean("Active", active);
            tag.put("Snapshot", snapshot.save());
            return tag;
        }

        private static Binding load(CompoundTag tag, Runnable dirty) {
            Binding binding = new Binding(tag.getUUID("Summoner"), dirty);
            ItemStack carrier = new ItemStack(ModContent1201.ECHO_SUMMONER);
            carrier.getOrCreateTag().put(SummonerData1201.CONTENTS_KEY,
                    tag.getList("Contents", Tag.TAG_COMPOUND).copy());
            List<ItemStack> slots = SummonerData1201.contents(carrier);
            SummonerData1201.serializeContents(slots);
            binding.contents = copyContents(slots);
            binding.fuel = Math.max(0, Math.min(SummonerData1201.FUEL_CAPACITY, tag.getInt("Fuel")));
            double fraction = tag.getDouble("FuelFraction");
            binding.fuelFraction = Double.isFinite(fraction) ? Math.max(0, Math.min(0.999999999, fraction)) : 0;
            binding.revision = Math.max(0, tag.getLong("StateRevision"));
            binding.controllerId = tag.hasUUID("Controller") ? tag.getUUID("Controller") : null;
            binding.spiritId = tag.hasUUID("Spirit") ? tag.getUUID("Spirit") : null;
            binding.generation = Math.max(0, tag.getLong("Generation"));
            binding.active = tag.getBoolean("Active") && binding.controllerId != null
                    && binding.spiritId != null && binding.generation > 0;
            if (!binding.active) binding.spiritId = null;
            binding.snapshot = SpiritSnapshot.load(tag.getCompound("Snapshot"));
            return binding;
        }
    }

    /** Detached entity migration payload. Temporary attacks/physics are deliberately not inferred here. */
    public record SpiritSnapshot(String dimension, double x, double y, double z,
                                 float health, float absorption, int fireTicks, int frozenTicks,
                                 int airSupply, CompoundTag migrationState) {
        public SpiritSnapshot {
            Objects.requireNonNull(dimension);
            Objects.requireNonNull(migrationState);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(health) || !Float.isFinite(absorption)) {
                throw new IllegalArgumentException("Non-finite entity snapshot");
            }
            health = Math.max(0, health);
            absorption = Math.max(0, absorption);
            fireTicks = Math.max(0, fireTicks);
            frozenTicks = Math.max(0, frozenTicks);
            migrationState = migrationState.copy();
        }

        @Override public CompoundTag migrationState() { return migrationState.copy(); }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dimension", dimension);
            tag.putDouble("X", x);
            tag.putDouble("Y", y);
            tag.putDouble("Z", z);
            tag.putFloat("Health", health);
            tag.putFloat("Absorption", absorption);
            tag.putInt("FireTicks", fireTicks);
            tag.putInt("FrozenTicks", frozenTicks);
            tag.putInt("AirSupply", airSupply);
            tag.put("MigrationState", migrationState.copy());
            return tag;
        }

        private static SpiritSnapshot load(CompoundTag tag) {
            return new SpiritSnapshot(tag.contains("Dimension") ? tag.getString("Dimension") : "minecraft:overworld",
                    tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"),
                    tag.getFloat("Health"), tag.getFloat("Absorption"), tag.getInt("FireTicks"),
                    tag.getInt("FrozenTicks"), tag.contains("AirSupply") ? tag.getInt("AirSupply") : 300,
                    tag.getCompound("MigrationState"));
        }
    }
}
