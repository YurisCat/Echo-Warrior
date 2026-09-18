package com.yuriscat.echowarrior.compat.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Finds summoner bindings stored directly in an item or in vanilla nested item components. */
public final class SummonerStackContents1211 {
    private static final int MAX_NESTING_DEPTH = 16;

    private SummonerStackContents1211() {
    }

    public static Set<UUID> summonerIds(ItemStack root) {
        Set<UUID> found = new HashSet<>();
        collect(root, found, 0);
        return found;
    }

    private static void collect(ItemStack stack, Set<UUID> found, int depth) {
        if (stack.isEmpty() || depth > MAX_NESTING_DEPTH) return;
        UUID summonerId = EchoSummonerItem1211.getSummonerId(stack);
        if (summonerId != null) found.add(summonerId);
        stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .nonEmptyStream().forEach(child -> collect(child, found, depth + 1));
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) bundle.itemCopyStream().forEach(child -> collect(child, found, depth + 1));
    }
}
