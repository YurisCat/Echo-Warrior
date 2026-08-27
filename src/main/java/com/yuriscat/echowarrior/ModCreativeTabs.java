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
					.displayItems((parameters, output) -> {
						output.accept(ModItems.ECHO_COMPASS);
						output.accept(ModItems.TEST_ECHO_SUMMONER);
						output.accept(ModItems.SMALL_KNOWLEDGE);
						output.accept(ModItems.COURAGE_LEGACY);
						output.accept(ModItems.FORTITUDE_LEGACY);
						output.accept(ModItems.PURITY_LEGACY);
						output.accept(ModItems.WISDOM_LEGACY);
						output.accept(ModItems.CRAFT_LEGACY);
						output.accept(ModItems.PLATE_ARMOR_MODULE);
						output.accept(ModItems.CHAINMAIL_ARMOR_MODULE);
						output.accept(ModItems.SPIKED_ARMOR_MODULE);
						output.accept(ModBlocks.SUSPICIOUS_GRASS_BLOCK);
						output.accept(ModBlocks.SUSPICIOUS_DIRT);
						output.accept(new ItemStack(ModItems.ROMAN_LEGIONARY_RELIC));
						output.accept(new ItemStack(ModItems.AZTEC_WARRIOR_RELIC));
						output.accept(new ItemStack(ModItems.EGYPTIAN_ARCHER_RELIC));
						output.accept(new ItemStack(ModItems.GUANDAO_WARRIOR_RELIC));
						output.accept(new ItemStack(ModItems.JAPANESE_SAMURAI_RELIC));
					})
					.build()
	);

	private ModCreativeTabs() {
	}

	public static void initialize() {
	}
}
