package com.yuriscat.echowarrior.compat.item;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface EchoSummonerAccessory1201 {
    /** Uniqueness follows registered item type, not display name or mutable NBT. */
    static boolean canInstall(Container contents, int targetSlot, ItemStack candidate) {
        if (!(candidate.getItem() instanceof EchoSummonerAccessory1201)) return false;
        for (int index = 0; index < SummonerData1201.ACCESSORY_SLOTS; index++) {
            if (index != targetSlot && contents.getItem(index).is(candidate.getItem())) return false;
        }
        return true;
    }
}
