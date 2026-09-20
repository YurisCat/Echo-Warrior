package com.yuriscat.echowarrior.compat.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Old-NBT equivalent of scanning item containers and bundles for visible summoner UUIDs. */
public final class SummonerStackContents1201 {
    private static final int MAX_DEPTH = 16;
    private static final int MAX_ITEMS = 4096;

    private SummonerStackContents1201() {}

    public static Set<UUID> summonerIds(ItemStack root) {
        ScanResult result = scan(root);
        if (!result.complete()) throw new IllegalStateException("Summoner visibility scan exceeded its safety budget");
        return result.ids();
    }

    /** Destruction checks must retain bindings when complete is false, never interpret partial absence as deletion. */
    public static ScanResult scan(ItemStack root) {
        Set<UUID> found = new HashSet<>();
        int[] budget = {MAX_ITEMS, 1};
        collect(root, found, 0, budget);
        return new ScanResult(Set.copyOf(found), budget[1] == 1);
    }

    public record ScanResult(Set<UUID> ids, boolean complete) {}

    /** Shared budget across every visible root, not a fresh expensive allowance per inventory slot. */
    public static final class VisibilityScan {
        private final Set<UUID> found = new HashSet<>();
        private final int[] budget = {MAX_ITEMS, 1};
        public void accept(ItemStack stack) {
            if (budget[1] == 1) collect(stack, found, 0, budget);
        }
        public ScanResult result() { return new ScanResult(Set.copyOf(found), budget[1] == 1); }
    }

    private static void collect(ItemStack stack, Set<UUID> found, int depth, int[] budget) {
        if (stack.isEmpty()) return;
        if (depth > MAX_DEPTH || budget[0]-- <= 0) {
            budget[1] = 0;
            return;
        }
        UUID id = SummonerData1201.summonerId(stack);
        if (id != null) found.add(id);
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        collectList(tag.getList(SummonerData1201.CONTENTS_KEY, Tag.TAG_COMPOUND), found, depth, budget);
        collectList(tag.getCompound("BlockEntityTag").getList("Items", Tag.TAG_COMPOUND), found, depth, budget);
        if (stack.is(Items.BUNDLE)) {
            collectList(tag.getList("Items", Tag.TAG_COMPOUND), found, depth, budget);
        }
    }

    private static void collectList(ListTag items, Set<UUID> found, int depth, int[] budget) {
        for (int index = 0; index < items.size(); index++) {
            if (budget[0] <= 0) {
                budget[1] = 0;
                break;
            }
            // Loading a damaged/capability-backed stack may mutate the supplied compound.
            collect(ItemStack.of(items.getCompound(index).copy()), found, depth + 1, budget);
        }
    }
}
