package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.ModTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Compatibility hook for items installable in one of the summoner's six accessory slots. */
public interface EchoSummonerAccessory {
	default boolean canInstall(ItemStack accessory, ItemStack summoner, int slot, List<ItemStack> installed) {
		return installed.stream().noneMatch(stack -> ItemStack.isSameItemSameComponents(stack, accessory));
	}

	static boolean isAccessory(ItemStack stack) {
		return !stack.isEmpty() && (stack.getItem() instanceof EchoSummonerAccessory || stack.is(ModTags.SUMMONER_ACCESSORIES));
	}

	static boolean canInstall(ItemStack stack, ItemStack summoner, int slot, SimpleContainer contents) {
		if (!isAccessory(stack)) return false;
		List<ItemStack> installed = contents.getItems().subList(0, Math.min(6, contents.getContainerSize())).stream()
				.map(ItemStack::copy).toList();
		if (installed.stream().anyMatch(candidate -> ItemStack.isSameItemSameComponents(candidate, stack))) return false;
		return !(stack.getItem() instanceof EchoSummonerAccessory accessory)
				|| accessory.canInstall(stack, summoner, slot, installed);
	}
}
