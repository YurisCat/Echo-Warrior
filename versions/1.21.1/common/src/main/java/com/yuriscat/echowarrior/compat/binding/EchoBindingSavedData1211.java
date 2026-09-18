package com.yuriscat.echowarrior.compat.binding;

import com.yuriscat.echowarrior.compat.EchoHeroType1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicItem1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1211;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EchoBindingSavedData1211 extends SavedData {
    private static final String FILE_ID = "echo_warrior_bindings_1211";
    private static final SavedData.Factory<EchoBindingSavedData1211> FACTORY = new SavedData.Factory<>(
            EchoBindingSavedData1211::new,
            EchoBindingSavedData1211::load,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, Binding> bindings = new HashMap<>();
    private final Map<UUID, Long> warningCooldowns = new HashMap<>();

    public static EchoBindingSavedData1211 get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public Binding get(UUID summonerId) {
        return this.bindings.get(summonerId);
    }

    public Binding getOrCreate(UUID summonerId) {
        Binding existing = this.bindings.get(summonerId);
        if (existing != null) return existing;
        Binding created = new Binding(summonerId);
        this.bindings.put(summonerId, created);
        this.setDirty();
        return created;
    }

    public List<Binding> bindings() {
        return List.copyOf(this.bindings.values());
    }

    public long lastWarning(UUID playerId) {
        return this.warningCooldowns.getOrDefault(playerId, Long.MIN_VALUE / 2L);
    }

    public void noteWarning(UUID playerId, long epochMillis) {
        this.warningCooldowns.put(playerId, epochMillis);
        this.setDirty();
    }

    public void remove(UUID summonerId) {
        if (this.bindings.remove(summonerId) != null) this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Binding binding : this.bindings.values()) list.add(binding.save(registries));
        tag.put("Bindings", list);
        ListTag warningList = new ListTag();
        for (Map.Entry<UUID, Long> entry : this.warningCooldowns.entrySet()) {
            CompoundTag warning = new CompoundTag();
            warning.putUUID("Player", entry.getKey());
            warning.putLong("LastWarning", entry.getValue());
            warningList.add(warning);
        }
        tag.put("WarningCooldowns", warningList);
        return tag;
    }

    private static EchoBindingSavedData1211 load(CompoundTag tag, HolderLookup.Provider registries) {
        EchoBindingSavedData1211 data = new EchoBindingSavedData1211();
        ListTag list = tag.getList("Bindings", CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            Binding binding = Binding.load(list.getCompound(index), registries);
            data.bindings.put(binding.summonerId, binding);
        }
        ListTag warningList = tag.getList("WarningCooldowns", CompoundTag.TAG_COMPOUND);
        for (int index = 0; index < warningList.size(); index++) {
            CompoundTag warning = warningList.getCompound(index);
            if (warning.hasUUID("Player")) {
                data.warningCooldowns.put(warning.getUUID("Player"), warning.getLong("LastWarning"));
            }
        }
        return data;
    }

    public static Binding roundTripForSelfTest(Binding binding, HolderLookup.Provider registries) {
        return Binding.load(binding.save(registries), registries);
    }

    public static final class Binding {
        private final UUID summonerId;
        private UUID controllerId;
        private UUID spiritId;
        private long generation;
        private boolean active;
        private String dimension = "minecraft:overworld";
        private double x;
        private double y;
        private double z;
        private float health;
        private float absorption;
        private int remainingFireTicks;
        private int ticksFrozen;
        private int airSupply = 300;
        private CompoundTag migrationState = new CompoundTag();
        private int fuel;
        private boolean fuelInitialized;
        private double fuelFraction;
        private ItemStack relic = ItemStack.EMPTY;
        private boolean relicInitialized;
        private final NonNullList<ItemStack> accessories = NonNullList.withSize(6, ItemStack.EMPTY);
        private boolean accessoriesInitialized;
        private int activityMode;
        private int alertMode = 1;
        private int enabledSkills = 0b1111;
        private int shieldCharges = 3;
        private long shieldChargeTime;
        private long legionCooldownEnd;
        private long stateRevision;

        private Binding(UUID summonerId) {
            this.summonerId = summonerId;
        }

        public UUID summonerId() { return this.summonerId; }
        public UUID controllerId() { return this.controllerId; }
        public UUID spiritId() { return this.spiritId; }
        public long generation() { return this.generation; }
        public boolean active() { return this.active; }
        public String dimension() { return this.dimension; }
        public double x() { return this.x; }
        public double y() { return this.y; }
        public double z() { return this.z; }
        public float health() { return this.health; }
        public float absorption() { return this.absorption; }
        public int remainingFireTicks() { return this.remainingFireTicks; }
        public int ticksFrozen() { return this.ticksFrozen; }
        public int airSupply() { return this.airSupply; }
        public CompoundTag migrationState() { return this.migrationState.copy(); }
        public int fuel() { return this.fuel; }
        public boolean fuelInitialized() { return this.fuelInitialized; }
        public double fuelFraction() { return this.fuelFraction; }
        public ItemStack relic() { return this.relic.copy(); }
        public boolean relicInitialized() { return this.relicInitialized; }
        public List<ItemStack> accessories() { return this.accessories.stream().map(ItemStack::copy).toList(); }
        public boolean accessoriesInitialized() { return this.accessoriesInitialized; }
        public long stateRevision() { return this.stateRevision; }
        public int activityMode() {
            return hasRelic() ? EchoRelicState1211.activityMode(this.relic).ordinal() : this.activityMode;
        }
        public int alertMode() {
            return hasRelic() ? EchoRelicState1211.alertMode(this.relic).ordinal() : this.alertMode;
        }
        public int enabledSkills() {
            return hasRelic() ? EchoRelicState1211.enabledSkills(this.relic) : this.enabledSkills;
        }
        public int skillCount() {
            return hasRelic() ? EchoHeroType1211.fromRelic(this.relic).skillCount() : 4;
        }
        public long legionCooldownEnd() {
            return hasRelic() ? EchoRelicState1211.legionCooldownEnd(this.relic) : this.legionCooldownEnd;
        }

        public void initializeFuel(int amount) {
            if (this.fuelInitialized) return;
            this.fuel = Math.max(0, Math.min(1000, amount));
            this.fuelInitialized = true;
            markStateChanged();
        }

        public void setFuel(int amount) {
            int normalized = Math.max(0, Math.min(1000, amount));
            if (this.fuelInitialized && this.fuel == normalized) return;
            this.fuel = normalized;
            this.fuelInitialized = true;
            markStateChanged();
        }

        public boolean consumeFuel(int amount) {
            if (amount <= 0) return true;
            if (this.fuel < amount) return false;
            this.fuel -= amount;
            markStateChanged();
            return true;
        }

        public boolean consumeFractionalFuel(double amount) {
            if (amount <= 0.0) return true;
            double total = this.fuelFraction + amount;
            int whole = (int)Math.floor(total + 1.0E-7);
            if (this.fuel < whole) return false;
            this.fuel -= whole;
            this.fuelFraction = total - whole;
            markStateChanged();
            return true;
        }

        public void initializeRelic(ItemStack relic) {
            if (this.relicInitialized) return;
            this.relic = relic.copy();
            this.relicInitialized = true;
            synchronizeCachedRelicState();
            markStateChanged();
        }

        public void setRelic(ItemStack relic) {
            if (this.relicInitialized && ItemStack.matches(this.relic, relic)) return;
            this.relic = relic.copy();
            this.relicInitialized = true;
            synchronizeCachedRelicState();
            markStateChanged();
        }

        public void initializeAccessories(List<ItemStack> accessories) {
            if (this.accessoriesInitialized) return;
            setAccessories(accessories);
        }

        public void setAccessories(List<ItemStack> accessories) {
            if (this.accessoriesInitialized && matchingAccessories(accessories)) return;
            for (int slot = 0; slot < this.accessories.size(); slot++) {
                ItemStack stack = slot < accessories.size() ? accessories.get(slot) : ItemStack.EMPTY;
                this.accessories.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
            }
            this.accessoriesInitialized = true;
            markStateChanged();
        }

        public void setActivityMode(int mode) {
            int normalized = Math.max(0, Math.min(2, mode));
            if (activityMode() == normalized) return;
            this.activityMode = normalized;
            if (hasRelic()) EchoRelicState1211.setActivityMode(
                    this.relic, EchoRelicState1211.ActivityMode.byOrdinal(this.activityMode));
            markStateChanged();
        }

        public void setAlertMode(int mode) {
            int normalized = Math.max(0, Math.min(2, mode));
            if (alertMode() == normalized) return;
            this.alertMode = normalized;
            if (hasRelic()) EchoRelicState1211.setAlertMode(
                    this.relic, EchoRelicState1211.AlertMode.byOrdinal(this.alertMode));
            markStateChanged();
        }

        public void toggleSkill(int skill) {
            int skillCount = skillCount();
            if (skill < 0 || skill >= skillCount) return;
            int before = enabledSkills();
            if (hasRelic()) {
                EchoRelicState1211.toggleSkill(this.relic, skill);
                this.enabledSkills = EchoRelicState1211.enabledSkills(this.relic);
            } else {
                this.enabledSkills ^= 1 << skill;
            }
            if (enabledSkills() != before) markStateChanged();
        }

        public boolean skillEnabled(int skill) {
            return skill >= 0 && skill < skillCount() && (enabledSkills() & 1 << skill) != 0;
        }

        public int shieldCharges(long now) {
            if (hasRelic()) return EchoRelicState1211.shieldCharges(this.relic, now);
            updateShieldCharges(now);
            return this.shieldCharges;
        }

        public int shieldChargeProgress(long now) {
            if (hasRelic()) return EchoRelicState1211.shieldChargeProgress(this.relic, now);
            updateShieldCharges(now);
            if (this.shieldCharges >= 3) return 1000;
            return (int)Math.max(0L, Math.min(1000L, (now - this.shieldChargeTime) * 10L));
        }

        public boolean consumeShieldCharge(long now) {
            if (hasRelic()) {
                boolean consumed = EchoRelicState1211.consumeShieldCharge(this.relic, now);
                if (consumed) markStateChanged();
                return consumed;
            }
            updateShieldCharges(now);
            if (this.shieldCharges <= 0) return false;
            this.shieldCharges--;
            if (this.shieldCharges == 2) this.shieldChargeTime = now;
            markStateChanged();
            return true;
        }

        private void updateShieldCharges(long now) {
            if (this.shieldCharges >= 3) {
                this.shieldCharges = 3;
                this.shieldChargeTime = now;
                return;
            }
            if (this.shieldChargeTime <= 0L) this.shieldChargeTime = now;
            long recovered = Math.max(0L, now - this.shieldChargeTime) / 100L;
            if (recovered <= 0L) return;
            this.shieldCharges = Math.min(3, this.shieldCharges + (int)recovered);
            this.shieldChargeTime += recovered * 100L;
            if (this.shieldCharges >= 3) this.shieldChargeTime = now;
        }

        public void setLegionCooldownEnd(long gameTime) {
            long normalized = Math.max(0L, gameTime);
            if (legionCooldownEnd() == normalized) return;
            this.legionCooldownEnd = normalized;
            if (hasRelic()) EchoRelicState1211.setLegionCooldownEnd(this.relic, this.legionCooldownEnd);
            markStateChanged();
        }

        private boolean hasRelic() {
            return this.relic.getItem() instanceof EchoRelicItem1211;
        }

        private void synchronizeCachedRelicState() {
            if (!hasRelic()) return;
            this.activityMode = EchoRelicState1211.activityMode(this.relic).ordinal();
            this.alertMode = EchoRelicState1211.alertMode(this.relic).ordinal();
            this.enabledSkills = EchoRelicState1211.enabledSkills(this.relic);
            this.legionCooldownEnd = EchoRelicState1211.legionCooldownEnd(this.relic);
        }

        public long activate(UUID controllerId, UUID spiritId, String dimension,
                             double x, double y, double z, float health) {
            return activate(controllerId, spiritId, dimension, x, y, z, health,
                    0.0F, 0, 0, 300, new CompoundTag());
        }

        public long activate(UUID controllerId, UUID spiritId, String dimension,
                             double x, double y, double z, float health, float absorption,
                             int remainingFireTicks, int ticksFrozen, int airSupply,
                             CompoundTag migrationState) {
            this.controllerId = controllerId;
            this.spiritId = spiritId;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.health = health;
            this.absorption = Math.max(0.0F, absorption);
            this.remainingFireTicks = Math.max(0, remainingFireTicks);
            this.ticksFrozen = Math.max(0, ticksFrozen);
            this.airSupply = airSupply;
            this.migrationState = migrationState.copy();
            this.active = true;
            markStateChanged();
            return ++this.generation;
        }

        public void track(String dimension, double x, double y, double z, float health) {
            track(dimension, x, y, z, health, this.absorption, this.remainingFireTicks,
                    this.ticksFrozen, this.airSupply, this.migrationState);
        }

        public void track(String dimension, double x, double y, double z, float health, float absorption,
                          int remainingFireTicks, int ticksFrozen, int airSupply,
                          CompoundTag migrationState) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.health = health;
            this.absorption = Math.max(0.0F, absorption);
            this.remainingFireTicks = Math.max(0, remainingFireTicks);
            this.ticksFrozen = Math.max(0, ticksFrozen);
            this.airSupply = airSupply;
            this.migrationState = migrationState.copy();
        }

        public void deactivate() {
            boolean changed = this.active || this.spiritId != null || this.health != 0.0F;
            this.active = false;
            this.spiritId = null;
            this.health = 0.0F;
            if (changed) markStateChanged();
        }

        private boolean matchingAccessories(List<ItemStack> values) {
            for (int slot = 0; slot < this.accessories.size(); slot++) {
                ItemStack expected = slot < values.size() ? values.get(slot) : ItemStack.EMPTY;
                if (!ItemStack.matches(this.accessories.get(slot), expected)) return false;
            }
            return true;
        }

        private void markStateChanged() {
            this.stateRevision++;
        }

        private CompoundTag save(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Summoner", this.summonerId);
            if (this.controllerId != null) tag.putUUID("Controller", this.controllerId);
            if (this.spiritId != null) tag.putUUID("Spirit", this.spiritId);
            tag.putLong("Generation", this.generation);
            tag.putBoolean("Active", this.active);
            tag.putString("Dimension", this.dimension);
            tag.putDouble("X", this.x);
            tag.putDouble("Y", this.y);
            tag.putDouble("Z", this.z);
            tag.putFloat("Health", this.health);
            tag.putFloat("Absorption", this.absorption);
            tag.putInt("RemainingFireTicks", this.remainingFireTicks);
            tag.putInt("TicksFrozen", this.ticksFrozen);
            tag.putInt("AirSupply", this.airSupply);
            tag.put("MigrationState", this.migrationState.copy());
            tag.putInt("Fuel", this.fuel);
            tag.putBoolean("FuelInitialized", this.fuelInitialized);
            tag.putDouble("FuelFraction", this.fuelFraction);
            tag.putBoolean("RelicInitialized", this.relicInitialized);
            if (!this.relic.isEmpty()) tag.put("Relic", this.relic.save(registries));
            tag.putBoolean("AccessoriesInitialized", this.accessoriesInitialized);
            ListTag accessoryList = new ListTag();
            for (int slot = 0; slot < this.accessories.size(); slot++) {
                ItemStack accessory = this.accessories.get(slot);
                if (accessory.isEmpty()) continue;
                CompoundTag entry = new CompoundTag();
                entry.putInt("Slot", slot);
                entry.put("Item", accessory.save(registries));
                accessoryList.add(entry);
            }
            tag.put("Accessories", accessoryList);
            tag.putInt("ActivityMode", this.activityMode);
            tag.putInt("AlertMode", this.alertMode);
            tag.putInt("EnabledSkills", this.enabledSkills);
            tag.putInt("ShieldCharges", this.shieldCharges);
            tag.putLong("ShieldChargeTime", this.shieldChargeTime);
            tag.putLong("LegionCooldownEnd", this.legionCooldownEnd);
            tag.putLong("StateRevision", this.stateRevision);
            return tag;
        }

        private static Binding load(CompoundTag tag, HolderLookup.Provider registries) {
            Binding binding = new Binding(tag.getUUID("Summoner"));
            binding.controllerId = tag.hasUUID("Controller") ? tag.getUUID("Controller") : null;
            binding.spiritId = tag.hasUUID("Spirit") ? tag.getUUID("Spirit") : null;
            binding.generation = tag.getLong("Generation");
            binding.active = tag.getBoolean("Active") && binding.spiritId != null;
            binding.dimension = tag.contains("Dimension") ? tag.getString("Dimension") : "minecraft:overworld";
            binding.x = tag.getDouble("X");
            binding.y = tag.getDouble("Y");
            binding.z = tag.getDouble("Z");
            binding.health = tag.getFloat("Health");
            binding.absorption = Math.max(0.0F, tag.getFloat("Absorption"));
            binding.remainingFireTicks = Math.max(0, tag.getInt("RemainingFireTicks"));
            binding.ticksFrozen = Math.max(0, tag.getInt("TicksFrozen"));
            binding.airSupply = tag.contains("AirSupply") ? tag.getInt("AirSupply") : 300;
            binding.migrationState = tag.contains("MigrationState")
                    ? tag.getCompound("MigrationState").copy() : new CompoundTag();
            binding.fuel = tag.getInt("Fuel");
            binding.fuelInitialized = tag.getBoolean("FuelInitialized");
            binding.fuelFraction = Math.max(0.0, Math.min(0.999999, tag.getDouble("FuelFraction")));
            binding.relicInitialized = tag.getBoolean("RelicInitialized") || tag.contains("Relic");
            if (tag.contains("Relic")) {
                binding.relic = ItemStack.parse(registries, tag.getCompound("Relic")).orElse(ItemStack.EMPTY);
            }
            binding.accessoriesInitialized = tag.getBoolean("AccessoriesInitialized") || tag.contains("Accessories");
            ListTag accessories = tag.getList("Accessories", CompoundTag.TAG_COMPOUND);
            for (int index = 0; index < accessories.size(); index++) {
                CompoundTag entry = accessories.getCompound(index);
                int slot = entry.getInt("Slot");
                if (slot < 0 || slot >= binding.accessories.size()) continue;
                binding.accessories.set(slot,
                        ItemStack.parse(registries, entry.getCompound("Item")).orElse(ItemStack.EMPTY));
            }
            binding.activityMode = Math.max(0, Math.min(2, tag.getInt("ActivityMode")));
            binding.alertMode = tag.contains("AlertMode") ? Math.max(0, Math.min(2, tag.getInt("AlertMode"))) : 1;
            binding.enabledSkills = tag.contains("EnabledSkills") ? tag.getInt("EnabledSkills") & 0b1111 : 0b1111;
            binding.shieldCharges = tag.contains("ShieldCharges") ? Math.max(0, Math.min(3, tag.getInt("ShieldCharges"))) : 3;
            binding.shieldChargeTime = tag.getLong("ShieldChargeTime");
            binding.legionCooldownEnd = tag.getLong("LegionCooldownEnd");
            binding.stateRevision = Math.max(0L, tag.getLong("StateRevision"));
            if (binding.hasRelic()) {
                EchoRelicState1211.importLegacyBindingState(binding.relic, binding.activityMode,
                        binding.alertMode, binding.enabledSkills, binding.shieldCharges,
                        binding.shieldChargeTime, binding.legionCooldownEnd);
                binding.synchronizeCachedRelicState();
            }
            return binding;
        }
    }
}
