package com.yuriscat.echowarrior;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
	public static final CreativeModeTab ECHO_WARRIOR = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			EchoWarrior.id("echo_warrior"),
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.echo_warrior.echo_warrior"))
					.icon(() -> new ItemStack(ModItems.TEST_ECHO_SUMMONER))
					.displayItems((parameters, output) -> output.accept(ModItems.TEST_ECHO_SUMMONER))
					.build()
	);

	private ModCreativeTabs() {
	}

	public static void initialize() {
	}
}
