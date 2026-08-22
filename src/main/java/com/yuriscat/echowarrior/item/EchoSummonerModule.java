package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.ModTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Compatibility hook for future summoner module items.
 *
 * <p>An item may opt in by implementing this interface or by belonging to the
 * {@code echo_warrior:summoner_modules} item tag. Tagged-only modules use the
 * default compatibility rule; code-backed modules can reject a particular
 * summoner, slot, or installed-module combination.</p>
 */
public interface EchoSummonerModule {
	default boolean canInstall(
			ItemStack module,
			ItemStack summoner,
			int moduleSlot,
			List<ItemStack> installedModules
	) {
		return true;
	}

	static boolean isModule(ItemStack stack) {
		return !stack.isEmpty() && (stack.getItem() instanceof EchoSummonerModule || stack.is(ModTags.SUMMONER_MODULES));
	}

	static boolean canInstall(ItemStack stack, ItemStack summoner, int moduleSlot, SimpleContainer contents) {
		if (!isModule(stack)) return false;
		if (!(stack.getItem() instanceof EchoSummonerModule module)) return true;
		List<ItemStack> installed = contents.getItems().subList(0, Math.min(6, contents.getContainerSize())).stream()
				.map(ItemStack::copy).toList();
		return module.canInstall(stack, summoner, moduleSlot, installed);
	}
}
