package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1201;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1201.Binding;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1201.SpiritSnapshot;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.binding.SummonerIdentityTracker1201;
import com.yuriscat.echowarrior.compat.binding.SummonerIdentityTracker1201.Location;
import com.yuriscat.echowarrior.compat.item.SummonerData1201;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Isolated-server tests of authority, not claims about spawned heroes or real creative clicks. */
public final class BindingAuthoritySelfTest1201 {
    private static final UUID DISK_ID = UUID.fromString("4c93a2d4-c9fe-4abd-9250-96744b106e51");
    private static final UUID REMOVED_ID = UUID.fromString("78b636bb-7d12-4b91-8015-d1980ec48f82");
    private static final UUID CONTROLLER = UUID.fromString("9c5b63da-3dcd-4570-a170-be9049e66f99");
    private static final UUID SPIRIT = UUID.fromString("86152ba5-a60e-4788-a2e6-fda74643b90c");
    private static int checks;

    private BindingAuthoritySelfTest1201() {}

    public static void run(MinecraftServer server) {
        checks = 0;
        testAuthority();
        testIdentity();
        testWorld(server);
        EchoWarrior1201.LOGGER.info("[Compat1201] BINDING SELFTEST PASSED checks={} boot={}",
                checks, Integer.getInteger("echo_warrior.compat_storage_expected_boot", 0));
    }

    private static void testAuthority() {
        EchoBindingSavedData1201 data = new EchoBindingSavedData1201();
        ItemStack physical = sample(UUID.randomUUID());
        Binding binding = data.register(physical);
        check(data.isDirty() && binding.fuel() == 345 && !binding.active()
                && binding.controllerId() == null && binding.spiritId() == null,
                "initial import retains contents but never trusts item control/spirit");
        binding.mirrorTo(physical);
        check(SummonerData1201.spiritId(physical) == null
                && SummonerData1201.revision(physical) == binding.stateRevision(), "initial mirror revision");
        ItemStack stale = physical.copy();
        List<ItemStack> oldContents = binding.contents();
        long beforeRevision = binding.stateRevision();
        List<ItemStack> removed = new ArrayList<>(oldContents);
        removed.set(0, ItemStack.EMPTY);
        removed.set(7, ItemStack.EMPTY);
        data.setDirty(false);
        check(binding.commitContents(beforeRevision, removed) && data.isDirty()
                && binding.stateRevision() == beforeRevision + 1, "atomic removal marks owner dirty once");
        data.setDirty(false);
        check(!binding.commitContents(beforeRevision, oldContents) && !data.isDirty()
                && binding.contents().get(0).isEmpty() && binding.contents().get(7).isEmpty(),
                "old menu cannot restore removed slots");
        check(binding.commitContents(binding.stateRevision(), removed) && !data.isDirty(), "unchanged contents stay clean");
        stale.getOrCreateTag().putLong("EchoWarriorStateRevision", Long.MAX_VALUE);
        check(data.register(stale) == binding && binding.contents().get(7).isEmpty(),
                "stale item with larger revision cannot overwrite world");
        binding.mirrorTo(stale);
        check(SummonerData1201.contents(stale).get(7).isEmpty()
                && SummonerData1201.revision(stale) == binding.stateRevision()
                && stale.getTag().getString("ForeignData").equals("preserved"), "world mirror replaces forged revision, retains foreign NBT");
        CompoundTag mirror = stale.getTag();
        binding.mirrorTo(stale);
        check(stale.getTag() == mirror && !data.isDirty(), "no-op sync avoids rewriting ItemStack tag");

        List<ItemStack> update = new ArrayList<>(oldContents);
        update.get(7).getOrCreateTag().putInt("DisabledSkills", 3);
        binding.commitContents(binding.stateRevision(), update);
        update.get(7).getOrCreateTag().putInt("DisabledSkills", 99);
        List<ItemStack> read = binding.contents();
        read.get(7).getOrCreateTag().putInt("DisabledSkills", 88);
        check(binding.contents().get(7).getTag().getInt("DisabledSkills") == 3, "world contents detach both read and write NBT");
        beforeRevision = binding.stateRevision();
        List<ItemStack> invalid = new ArrayList<>(binding.contents());
        invalid.set(0, new ItemStack(Items.DIAMOND, 2));
        data.setDirty(false);
        boolean rejected = false;
        try { binding.commitContents(beforeRevision, invalid); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected && !data.isDirty() && binding.stateRevision() == beforeRevision
                && binding.contents().get(0).getCount() == 1, "invalid snapshot is fully rejected");

        binding.setFuel(10);
        data.setDirty(false);
        check(!binding.consumeFuel(-1) && !binding.consumeFuel(11)
                && !binding.consumeFractionalFuel(Double.NaN)
                && !binding.consumeFractionalFuel(Double.POSITIVE_INFINITY)
                && !binding.consumeFractionalFuel(-0.5) && !data.isDirty(), "bad fuel costs do not mutate");
        for (int index = 0; index < 10; index++) check(binding.consumeFractionalFuel(0.1), "fractional fuel accepted " + index);
        check(binding.fuel() == 9 && binding.fuelFraction() < 1.0E-7 && data.isDirty(), "decimal fuel reaches exact whole unit");
        binding.consumeFractionalFuel(0.25);
        data.setDirty(false);
        check(!binding.consumeFractionalFuel(20) && binding.fuel() == 9
                && binding.fuelFraction() == 0.25 && !data.isDirty(), "failed fractional payment preserves remainder");

        CompoundTag migration = new CompoundTag();
        migration.putInt("RetainedBuffTicks", 120);
        SpiritSnapshot first = state("minecraft:overworld", migration);
        migration.putInt("RetainedBuffTicks", 999);
        first.migrationState().putInt("RetainedBuffTicks", 999);
        check(first.migrationState().getInt("RetainedBuffTicks") == 120, "entity snapshot NBT is detached in both directions");
        long firstGeneration = binding.activate(CONTROLLER, SPIRIT, first);
        check(binding.matches(SPIRIT, firstGeneration) && data.isDirty(), "activation owns a generation");
        data.setDirty(false);
        beforeRevision = binding.stateRevision();
        SpiritSnapshot moved = state("minecraft:the_nether", first.migrationState());
        check(binding.track(SPIRIT, firstGeneration, moved) && data.isDirty()
                && binding.stateRevision() == beforeRevision, "tracking persists without invalidating equipment revision");
        data.setDirty(false);
        check(binding.track(SPIRIT, firstGeneration, moved) && !data.isDirty(), "unchanged tracking stays clean");
        UUID newSpirit = UUID.randomUUID();
        long secondGeneration = binding.activate(CONTROLLER, newSpirit, moved);
        data.setDirty(false);
        check(secondGeneration == firstGeneration + 1 && !binding.matches(SPIRIT, firstGeneration)
                && !binding.track(SPIRIT, firstGeneration, first)
                && !binding.deactivate(SPIRIT, firstGeneration) && !data.isDirty(), "old entity cannot track or dismiss a replacement");
        check(!binding.track(newSpirit, firstGeneration, first)
                && !binding.track(SPIRIT, secondGeneration, first), "both entity ID and generation must match");
        check(binding.controllerId().equals(CONTROLLER) && binding.snapshot().equals(moved), "rejected old state preserves control and snapshot");

        CompoundTag encoded = data.save(new CompoundTag());
        EchoBindingSavedData1201 restored = EchoBindingSavedData1201.load(encoded);
        Binding roundTrip = restored.get(binding.summonerId());
        check(roundTrip.matches(newSpirit, secondGeneration) && roundTrip.snapshot().equals(moved)
                && roundTrip.fuel() == 9 && roundTrip.fuelFraction() == 0.25
                && roundTrip.stateRevision() == binding.stateRevision()
                && roundTrip.contents().get(7).getTag().getInt("DisabledSkills") == 3,
                "all binding fields round trip");
        roundTrip.consumeFuel(1);
        check(restored.isDirty() && binding.fuel() == 9 && data.save(new CompoundTag()).equals(encoded),
                "loaded bindings retain owner dirty callback without aliasing original");
        roundTrip.deactivate(newSpirit, secondGeneration);
        check(!roundTrip.active() && roundTrip.spiritId() == null && roundTrip.snapshot().health() == 0,
                "dismissal invalidates entity");
        data.setDirty(false);
        check(data.remove(binding.summonerId()) && data.isDirty() && data.get(binding.summonerId()) == null
                && !binding.matches(newSpirit, secondGeneration), "confirmed removal invalidates retained handles");
        check(!binding.commitContents(binding.stateRevision(), oldContents) && !binding.consumeFuel(1),
                "removed handle cannot continue mutating authority");
        check(EchoBindingSavedData1201.load(data.save(new CompoundTag())).get(binding.summonerId()) == null,
                "removed binding stays absent through serialization");
    }

    private static void testIdentity() {
        SummonerIdentityTracker1201 tracker = new SummonerIdentityTracker1201();
        Location first = new Location(CONTROLLER, 0);
        Location second = new Location(CONTROLLER, 1);
        Location carried = new Location(CONTROLLER, -1);
        Map<Location, ItemStack> inventory = new HashMap<>();
        ItemStack original = sample(UUID.randomUUID());
        UUID originalId = SummonerData1201.summonerId(original);
        inventory.put(first, original);
        tracker.resolve(original, first, key -> inventory.getOrDefault(key, ItemStack.EMPTY));
        ItemStack replacementObject = original.copy();
        inventory.put(first, replacementObject);
        check(tracker.resolve(replacementObject, first, key -> inventory.getOrDefault(key, ItemStack.EMPTY)).equals(originalId),
                "network object replacement in same slot retains identity");
        ItemStack duplicate = original.copy();
        inventory.put(second, duplicate);
        UUID duplicateId = tracker.resolve(duplicate, second, key -> inventory.getOrDefault(key, ItemStack.EMPTY));
        check(!duplicateId.equals(originalId) && SummonerData1201.spiritId(duplicate) == null
                && SummonerData1201.contents(duplicate).get(7).getTag().getInt("DisabledSkills") == 2,
                "later visible copy receives new identity without losing contents");
        inventory.remove(first);
        inventory.put(carried, replacementObject);
        check(tracker.resolve(replacementObject, carried, key -> inventory.getOrDefault(key, ItemStack.EMPTY)).equals(originalId),
                "ordinary move to cursor retains identity");
        check(tracker.resolve(replacementObject.copy(), null, key -> inventory.getOrDefault(key, ItemStack.EMPTY)).equals(originalId),
                "detached menu snapshot is not mistaken for a physical duplicate");
        EchoBindingSavedData1201 data = new EchoBindingSavedData1201();
        Binding canonical = data.register(replacementObject);
        canonical.activate(CONTROLLER, SPIRIT, state("minecraft:overworld", new CompoundTag()));
        Binding copy = data.register(duplicate);
        check(!copy.active() && copy.controllerId() == null && canonical.active(), "duplicate never inherits active control");
    }

    private static void testWorld(MinecraftServer server) {
        EchoBindingSavedData1201 data = EchoBindingSavedData1201.get(server);
        int boot = Integer.getInteger("echo_warrior.compat_storage_expected_boot", 0);
        check(server.getLevel(Level.NETHER) != null
                && EchoBindingSavedData1201.get(server.getLevel(Level.NETHER).getServer()) == data,
                "one overworld authority shared across dimensions");
        Binding binding = data.get(DISK_ID);
        if (boot == 1) check(binding == null, "first boot starts with no persisted authority");
        if (boot == 2) {
            check(binding != null && binding.matches(SPIRIT, 2) && binding.controllerId().equals(CONTROLLER)
                    && binding.fuel() == 320 && binding.fuelFraction() == 0.4
                    && binding.contents().get(6).getCount() == 12
                    && binding.contents().get(7).getTag().getInt("DisabledSkills") == 2
                    && binding.snapshot().equals(state("minecraft:the_nether", migrationState())),
                    "real world SavedData survives shutdown/restart without a visible item");
            check(data.get(REMOVED_ID) == null, "removed world record stays removed on disk");
            long revision = binding.stateRevision();
            ItemStack stale = sample(DISK_ID);
            check(EchoBindingSystem1201.synchronize(server.getLevel(Level.NETHER), stale) == binding
                    && SummonerData1201.fuel(stale) == 320
                    && SummonerData1201.revision(stale) == revision
                    && SPIRIT.equals(SummonerData1201.spiritId(stale)), "late stale item loads authority, not vice versa");
        }
        if (binding == null) {
            binding = EchoBindingSystem1201.synchronize(server.overworld(), sample(DISK_ID));
            binding.setFuel(321);
            binding.consumeFuel(1);
            binding.consumeFractionalFuel(0.4);
            binding.activate(CONTROLLER, UUID.randomUUID(), state("minecraft:overworld", migrationState()));
            binding.activate(CONTROLLER, SPIRIT, state("minecraft:the_nether", migrationState()));
        }
        // Exercise Item.inventoryTick on a server-side player object. No client or player connection is simulated.
        ItemStack inventoryStack = sample(UUID.randomUUID());
        ServerPlayer player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "BindingSelfTest"));
        ModContent1201.ECHO_SUMMONER.inventoryTick(inventoryStack, server.overworld(), player, 0, false);
        UUID inventoryId = SummonerData1201.summonerId(inventoryStack);
        check(data.get(inventoryId) != null && SummonerData1201.spiritId(inventoryStack) == null,
                "registered item inventory callback reaches world authority");
        data.remove(inventoryId);
        Binding removed = data.register(sample(REMOVED_ID));
        removed.activate(CONTROLLER, UUID.randomUUID(), state("minecraft:overworld", new CompoundTag()));
        check(data.remove(REMOVED_ID) && !removed.active(), "real world removal is dirty and immediate");
    }

    private static ItemStack sample(UUID id) {
        ItemStack stack = new ItemStack(ModContent1201.ECHO_SUMMONER);
        stack.getOrCreateTag().putUUID("EchoWarriorSummonerId", id);
        stack.getOrCreateTag().putString("ForeignData", "preserved");
        SummonerData1201.setFuel(stack, 345);
        SummonerData1201.setSpiritId(stack, UUID.randomUUID());
        NonNullList<ItemStack> contents = NonNullList.withSize(8, ItemStack.EMPTY);
        contents.set(0, new ItemStack(Items.DIAMOND));
        contents.set(6, new ItemStack(Items.ROTTEN_FLESH, 12));
        contents.set(7, new ItemStack(Items.PAPER));
        contents.get(7).getOrCreateTag().putInt("DisabledSkills", 2);
        if (!SummonerData1201.commitContents(stack, SummonerData1201.revision(stack), contents)) {
            throw new IllegalStateException("Fresh binding sample rejected");
        }
        return stack;
    }

    private static CompoundTag migrationState() {
        CompoundTag state = new CompoundTag();
        state.putInt("RetainedBuffTicks", 120);
        return state;
    }

    private static SpiritSnapshot state(String dimension, CompoundTag migration) {
        return new SpiritSnapshot(dimension, 12.25, 70.5, -42.75, 17.5F, 3.0F, 17, 23, 123, migration);
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new IllegalStateException("[Compat1201] Binding check failed: " + name);
        checks++;
    }
}
