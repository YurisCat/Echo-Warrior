package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.ModContent1201;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * 1.20.1 replacement for the summoner's custom-data/container components.
 * Reads are side-effect free; returned contents are detached snapshots. Writes are
 * server-side persistence primitives, NOT client authorization or a replacement for
 * world binding authority. Slot/item compatibility belongs to the menu.
 */
public final class SummonerData1201 {
    public static final int ACCESSORY_SLOTS = 6;
    public static final int FUEL_SLOT = 6;
    public static final int RELIC_SLOT = 7;
    public static final int SLOT_COUNT = 8;
    public static final int FUEL_CAPACITY = 1000;
    public static final String CONTENTS_KEY = "EchoWarriorContents";
    private static final String ID_KEY = "EchoWarriorSummonerId";
    private static final String SPIRIT_KEY = "EchoWarriorSpiritId";
    private static final String FUEL_KEY = "EchoWarriorFuel";
    private static final String REVISION_KEY = "EchoWarriorStateRevision";

    private SummonerData1201() {}

    public static UUID summonerId(ItemStack stack) {
        return stack.is(ModContent1201.ECHO_SUMMONER) ? readUuid(stack.getTag(), ID_KEY) : null;
    }

    public static UUID getOrCreateSummonerId(ItemStack stack) {
        requireSummoner(stack);
        UUID id = summonerId(stack);
        if (id == null) {
            id = UUID.randomUUID();
            CompoundTag tag = copyTag(stack);
            tag.putUUID(ID_KEY, id);
            commit(stack, tag);
        }
        return id;
    }

    public static UUID spiritId(ItemStack stack) {
        return readUuid(stack.getTag(), SPIRIT_KEY);
    }

    public static void setSpiritId(ItemStack stack, UUID id) {
        requireSummoner(stack);
        if (java.util.Objects.equals(spiritId(stack), id)) return;
        CompoundTag tag = copyTag(stack);
        if (id == null) tag.remove(SPIRIT_KEY);
        else tag.putUUID(SPIRIT_KEY, id);
        commit(stack, tag);
    }

    public static UUID replaceDuplicateId(ItemStack stack) {
        requireSummoner(stack);
        CompoundTag tag = copyTag(stack);
        UUID id = UUID.randomUUID();
        tag.putUUID(ID_KEY, id);
        tag.remove(SPIRIT_KEY);
        commit(stack, tag);
        return id;
    }

    public static long revision(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getLong(REVISION_KEY));
    }

    public static int fuel(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, Math.min(FUEL_CAPACITY, tag.getInt(FUEL_KEY)));
    }

    public static void setFuel(ItemStack stack, int amount) {
        requireSummoner(stack);
        int normalized = Math.max(0, Math.min(FUEL_CAPACITY, amount));
        if (fuel(stack) == normalized) return;
        CompoundTag tag = copyTag(stack);
        tag.putInt(FUEL_KEY, normalized);
        commit(stack, tag);
    }

    public static boolean consumeFuel(ItemStack stack, int amount) {
        requireSummoner(stack);
        if (amount < 0 || fuel(stack) < amount) return false;
        setFuel(stack, fuel(stack) - amount);
        return true;
    }

    public static NonNullList<ItemStack> contents(ItemStack stack) {
        NonNullList<ItemStack> result = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        CompoundTag tag = stack.getTag();
        if (tag == null) return result;
        ListTag entries = tag.getList(CONTENTS_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            if (!entry.contains("Slot", Tag.TAG_ANY_NUMERIC)) continue;
            int slot = entry.getInt("Slot");
            // ItemStack.of retains nested tags in 1.20.1 and may normalize them while loading.
            if (slot >= 0 && slot < SLOT_COUNT) result.set(slot, ItemStack.of(entry.copy()));
        }
        return result;
    }

    /** Compare-and-set the entire eight-slot snapshot. Rejected requests do not mutate anything. */
    public static boolean commitContents(ItemStack stack, long expectedRevision, List<ItemStack> slots) {
        requireSummoner(stack);
        if (expectedRevision != revision(stack)) return false;
        ListTag entries = serializeContents(slots);
        CompoundTag tag = copyTag(stack);
        if (entries.isEmpty()) tag.remove(CONTENTS_KEY);
        else tag.put(CONTENTS_KEY, entries);
        // Avoid revision churn and redundant equip/sync events for unchanged contents.
        if (!copyTag(stack).equals(tag)) commit(stack, tag);
        return true;
    }

    /** Shared structural validation. Gameplay item eligibility is checked by the server menu. */
    public static ListTag serializeContents(List<ItemStack> slots) {
        if (slots.size() != SLOT_COUNT) throw new IllegalArgumentException("Expected all eight summoner slots");
        ListTag entries = new ListTag();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack item = slots.get(slot);
            int limit = slot == FUEL_SLOT ? item.getMaxStackSize() : 1;
            if (!item.isEmpty() && item.getCount() > limit) {
                throw new IllegalArgumentException("Overfull summoner slot " + slot);
            }
            if (item.isEmpty()) continue;
            CompoundTag entry = item.save(new CompoundTag());
            entry.putInt("Slot", slot);
            entries.add(entry);
        }
        return entries;
    }

    /** Server-to-item mirror only. Never use a client packet as this method's authority. */
    public static void applyAuthority(ItemStack stack, UUID id, UUID spirit, int fuel,
                                      long revision, List<ItemStack> slots) {
        requireSummoner(stack);
        if (!id.equals(summonerId(stack))) throw new IllegalArgumentException("Summoner identity mismatch");
        if (revision < 0 || fuel < 0 || fuel > FUEL_CAPACITY) {
            throw new IllegalArgumentException("Invalid authoritative state");
        }
        ListTag entries = serializeContents(slots);
        CompoundTag tag = copyTag(stack);
        if (entries.isEmpty()) tag.remove(CONTENTS_KEY);
        else tag.put(CONTENTS_KEY, entries);
        if (spirit == null) tag.remove(SPIRIT_KEY);
        else tag.putUUID(SPIRIT_KEY, spirit);
        tag.putInt(FUEL_KEY, fuel);
        tag.putLong(REVISION_KEY, revision);
        // Mirror revisions come from SavedData, even if forged/stale NBT claims a larger value.
        if (!copyTag(stack).equals(tag)) stack.setTag(tag);
    }

    private static void commit(ItemStack stack, CompoundTag tag) {
        tag.putLong(REVISION_KEY, Math.addExact(revision(stack), 1));
        stack.setTag(tag);
    }

    private static CompoundTag copyTag(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().copy() : new CompoundTag();
    }

    private static UUID readUuid(CompoundTag tag, String key) {
        return tag != null && tag.hasUUID(key) ? tag.getUUID(key) : null;
    }

    private static void requireSummoner(ItemStack stack) {
        if (!stack.is(ModContent1201.ECHO_SUMMONER)) throw new IllegalArgumentException("Not an Echo Summoner");
    }
}
