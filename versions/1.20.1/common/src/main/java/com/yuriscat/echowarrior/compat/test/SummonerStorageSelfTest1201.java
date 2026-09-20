package com.yuriscat.echowarrior.compat.test;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.item.SummonerData1201;
import com.yuriscat.echowarrior.compat.item.SummonerStackContents1201;
import io.netty.buffer.Unpooled;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Set;
import java.util.UUID;

/** Runs only when explicitly enabled in an isolated test server. Not gameplay binding data. */
public final class SummonerStorageSelfTest1201 {
    private static int checks;

    private SummonerStorageSelfTest1201() {}

    public static void run(MinecraftServer server) {
        checks = 0;
        check(BuiltInRegistries.ITEM.get(ModContent1201.SUMMONER_ID) == ModContent1201.ECHO_SUMMONER,
                "real summoner registered");
        ItemStack empty = new ItemStack(ModContent1201.ECHO_SUMMONER);
        check(SummonerData1201.summonerId(empty) == null && SummonerData1201.fuel(empty) == 0
                && SummonerData1201.revision(empty) == 0 && SummonerData1201.contents(empty).size() == 8,
                "empty defaults");
        check(!empty.hasTag(), "reads must not create NBT");
        UUID id = SummonerData1201.getOrCreateSummonerId(empty);
        long revision = SummonerData1201.revision(empty);
        check(id.equals(SummonerData1201.getOrCreateSummonerId(empty))
                && SummonerData1201.revision(empty) == revision, "stable identity and revision");
        SummonerData1201.setFuel(empty, 2000);
        check(SummonerData1201.fuel(empty) == 1000, "fuel upper bound");
        CompoundTag before = empty.getTag().copy();
        check(!SummonerData1201.consumeFuel(empty, -1) && !SummonerData1201.consumeFuel(empty, 1001)
                && empty.getTag().equals(before), "failed consumption does not mutate");
        check(SummonerData1201.consumeFuel(empty, 100) && SummonerData1201.fuel(empty) == 900,
                "fuel subtraction");
        SummonerData1201.setFuel(empty, -1);
        check(SummonerData1201.fuel(empty) == 0, "fuel lower bound");

        ItemStack sample = sample();
        ItemStack restored = ItemStack.of(sample.save(new CompoundTag()));
        check(ItemStack.matches(sample, restored), "real item NBT round trip");
        NonNullList<ItemStack> slots = SummonerData1201.contents(sample);
        slots.get(0).shrink(1);
        check(!SummonerData1201.contents(sample).get(0).isEmpty(), "read snapshot separation");
        slots.get(7).getOrCreateTag().putInt("DisabledSkills", 99);
        check(SummonerData1201.contents(sample).get(7).getTag().getInt("DisabledSkills") == 2,
                "nested NBT read snapshot separation, not just stack count");
        long current = SummonerData1201.revision(sample);
        slots = SummonerData1201.contents(sample);
        slots.set(0, ItemStack.EMPTY);
        slots.set(7, ItemStack.EMPTY);
        check(SummonerData1201.commitContents(sample, current, slots), "atomic relic/accessory removal");
        check(SummonerData1201.revision(sample) == current + 1
                && SummonerData1201.contents(sample).get(0).isEmpty()
                && SummonerData1201.contents(sample).get(7).isEmpty(), "one revision for whole snapshot");
        before = sample.getTag().copy();
        check(!SummonerData1201.commitContents(sample, current, SummonerData1201.contents(restored))
                && before.equals(sample.getTag()), "stale snapshot cannot resurrect removed items");
        check(SummonerData1201.commitContents(sample, current + 1, slots)
                && before.equals(sample.getTag()), "unchanged snapshot does not churn revision");
        slots.get(6).shrink(3);
        check(SummonerData1201.contents(sample).get(6).getCount() == 12, "write snapshot separation");
        check(sample.getTag().getString("ForeignModData").equals("preserve"), "unrelated NBT retained");

        slots = SummonerData1201.contents(sample);
        slots.set(0, new ItemStack(Items.DIAMOND, 2));
        before = sample.getTag().copy();
        boolean rejected = false;
        try { SummonerData1201.commitContents(sample, SummonerData1201.revision(sample), slots); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected && before.equals(sample.getTag()), "invalid content rejected before any write");

        UUID oldId = SummonerData1201.summonerId(restored);
        UUID replacement = SummonerData1201.replaceDuplicateId(restored);
        check(!replacement.equals(oldId) && SummonerData1201.spiritId(restored) == null,
                "duplicate identity repair clears stale spirit");
        check(SummonerData1201.contents(restored).get(7).getTag().getInt("DisabledSkills") == 2,
                "nested relic metadata retained");
        FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.writeItem(restored);
            check(ItemStack.matches(restored, packet.readItem()), "vanilla inventory packet round trip");
        } finally { packet.release(); }

        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        ListTag nested = new ListTag();
        CompoundTag entry = restored.save(new CompoundTag());
        entry.putByte("Slot", (byte)0);
        nested.add(entry);
        CompoundTag blockEntity = new CompoundTag();
        blockEntity.put("Items", nested);
        box.getOrCreateTag().put("BlockEntityTag", blockEntity);
        check(SummonerStackContents1201.summonerIds(box).equals(Set.of(replacement)), "shulker visibility");
        CompoundTag damaged = new ItemStack(Items.DIAMOND_SWORD).save(new CompoundTag());
        damaged.getCompound("tag").putInt("Damage", -17);
        nested.add(damaged);
        before = box.getTag().copy();
        SummonerStackContents1201.scan(box);
        check(before.equals(box.getTag()), "visibility scan cannot normalize the original nested stack NBT");
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        ListTag bundleItems = new ListTag();
        bundleItems.add(box.save(new CompoundTag()));
        bundle.getOrCreateTag().put("Items", bundleItems);
        check(SummonerStackContents1201.summonerIds(bundle).equals(Set.of(replacement)), "nested bundle visibility");
        ItemStack spoof = new ItemStack(Items.PAPER);
        spoof.setTag(restored.getTag().copy());
        spoof.removeTagKey(SummonerData1201.CONTENTS_KEY);
        check(SummonerStackContents1201.summonerIds(spoof).isEmpty(), "foreign item cannot spoof identity");
        ItemStack deep = restored;
        for (int depth = 0; depth < 18; depth++) {
            ItemStack wrapper = new ItemStack(Items.BUNDLE);
            ListTag items = new ListTag();
            items.add(deep.save(new CompoundTag()));
            wrapper.getOrCreateTag().put("Items", items);
            deep = wrapper;
        }
        check(!SummonerStackContents1201.scan(deep).complete(), "truncated scan must not authorize destruction");
        var visibility = new SummonerStackContents1201.VisibilityScan();
        visibility.accept(bundle);
        check(visibility.result().complete() && visibility.result().ids().contains(replacement), "shared visibility finds nested identity");
        for (int index = 0; index < 4097; index++) visibility.accept(new ItemStack(Items.PAPER));
        check(!visibility.result().complete() && visibility.result().ids().contains(replacement), "global visibility budget is shared and preserves partial evidence");
        visibility.accept(restored);
        check(!visibility.result().complete(), "additional roots cannot turn a partial scan complete");
        ItemStack malformed = new ItemStack(ModContent1201.ECHO_SUMMONER);
        malformed.getOrCreateTag().putIntArray("EchoWarriorSummonerId", new int[]{1, 2});
        ListTag invalidSlots = new ListTag();
        CompoundTag invalid = new ItemStack(Items.DIAMOND).save(new CompoundTag());
        invalid.putInt("Slot", 1000);
        invalidSlots.add(invalid);
        malformed.getOrCreateTag().put(SummonerData1201.CONTENTS_KEY, invalidSlots);
        before = malformed.getTag().copy();
        check(SummonerData1201.summonerId(malformed) == null
                && SummonerData1201.contents(malformed).stream().allMatch(ItemStack::isEmpty)
                && before.equals(malformed.getTag()), "invalid UUID/slot read safely without rewriting data");

        // Deliberately separate test fixture: full authority/entity binding is not ported yet.
        Fixture fixture = server.overworld().getDataStorage().computeIfAbsent(
                Fixture::load, Fixture::new, "echo_warrior_storage_selftest_1201");
        int expectedBoot = Integer.getInteger("echo_warrior.compat_storage_expected_boot", 0);
        check(expectedBoot == 0 || fixture.boots == expectedBoot - 1, "expected disk boot counter");
        if (fixture.boots > 0) {
            check(fixture.stack.is(ModContent1201.ECHO_SUMMONER)
                    && fixture.savedId.equals(SummonerData1201.summonerId(fixture.stack))
                    && SummonerData1201.fuel(fixture.stack) == 345
                    && SummonerData1201.revision(fixture.stack) == fixture.savedRevision
                    && SummonerData1201.contents(fixture.stack).get(6).getCount() == 12
                    && SummonerData1201.contents(fixture.stack).get(7).getTag().getInt("DisabledSkills") == 2,
                    "real summoner disk save/restart");
        }
        fixture.stack = sample();
        fixture.savedId = SummonerData1201.summonerId(fixture.stack);
        fixture.savedRevision = SummonerData1201.revision(fixture.stack);
        fixture.boots++;
        fixture.setDirty();
        EchoWarrior1201.LOGGER.info("[Compat1201] STORAGE SELFTEST PASSED checks={} boot={}", checks, fixture.boots);
    }

    private static ItemStack sample() {
        ItemStack stack = new ItemStack(ModContent1201.ECHO_SUMMONER);
        SummonerData1201.getOrCreateSummonerId(stack);
        SummonerData1201.setSpiritId(stack, UUID.randomUUID());
        SummonerData1201.setFuel(stack, 345);
        stack.getOrCreateTag().putString("ForeignModData", "preserve");
        NonNullList<ItemStack> slots = NonNullList.withSize(8, ItemStack.EMPTY);
        // Vanilla markers exercise opaque stack storage; real relic/accessory gameplay is pending.
        slots.set(0, new ItemStack(Items.DIAMOND));
        slots.set(5, new ItemStack(Items.EMERALD));
        slots.set(6, new ItemStack(Items.ROTTEN_FLESH, 12));
        slots.set(7, new ItemStack(Items.PAPER));
        slots.get(7).getOrCreateTag().putInt("DisabledSkills", 2);
        if (!SummonerData1201.commitContents(stack, SummonerData1201.revision(stack), slots)) {
            throw new IllegalStateException("Fresh fixture commit was rejected");
        }
        return stack;
    }

    private static void check(boolean condition, String name) {
        if (!condition) throw new IllegalStateException("[Compat1201] Storage check failed: " + name);
        checks++;
    }

    public static final class Fixture extends SavedData {
        private int boots;
        private ItemStack stack = ItemStack.EMPTY;
        private UUID savedId;
        private long savedRevision;

        private static Fixture load(CompoundTag tag) {
            Fixture fixture = new Fixture();
            fixture.boots = tag.getInt("Boots");
            fixture.stack = ItemStack.of(tag.getCompound("Item"));
            fixture.savedId = tag.hasUUID("ExpectedId") ? tag.getUUID("ExpectedId") : null;
            fixture.savedRevision = tag.getLong("ExpectedRevision");
            return fixture;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putInt("Boots", boots);
            tag.put("Item", stack.save(new CompoundTag()));
            tag.putUUID("ExpectedId", savedId);
            tag.putLong("ExpectedRevision", savedRevision);
            return tag;
        }
    }
}
