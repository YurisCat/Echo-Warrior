package com.yuriscat.echowarrior.compat.item;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Compatibility hook for items installable in the summoner's six accessory slots. */
public interface EchoSummonerAccessory1211 {
    default boolean canInstall(ItemStack accessory, ItemStack summoner, int slot, List<ItemStack> installed) {
        return installed.stream().noneMatch(stack -> ItemStack.isSameItemSameComponents(stack, accessory));
    }

    static boolean isAccessory(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof EchoSummonerAccessory1211;
    }

    static boolean canInstall(ItemStack stack, ItemStack summoner, int slot, SimpleContainer contents) {
        if (!isAccessory(stack) || slot < 0 || slot >= Math.min(6, contents.getContainerSize())) return false;
        List<ItemStack> installed = contents.getItems().subList(0, Math.min(6, contents.getContainerSize())).stream()
                .map(ItemStack::copy).toList();
        if (installed.stream().anyMatch(candidate -> ItemStack.isSameItemSameComponents(candidate, stack))) return false;
        EchoSummonerAccessory1211 accessory = (EchoSummonerAccessory1211)stack.getItem();
        return accessory.canInstall(stack, summoner, slot, installed);
    }
}
