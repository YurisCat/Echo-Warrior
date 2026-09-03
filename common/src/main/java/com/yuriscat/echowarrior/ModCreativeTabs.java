package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.knowledge.KnowledgeCatalog;
import com.yuriscat.echowarrior.knowledge.KnowledgeStackData;
import com.yuriscat.echowarrior.platform.RegistryRegistrar;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;

public final class ModCreativeTabs {
	public static CreativeModeTab ECHO_WARRIOR;
	public static CreativeModeTab ACCESSORIES;
	public static CreativeModeTab KNOWLEDGE_FRAGMENTS;

	private ModCreativeTabs() {
	}

	public static void register(RegistryRegistrar<CreativeModeTab> registrar, TabBuilderFactory factory) {
		ECHO_WARRIOR = registrar.register(
				EchoWarrior.id("echo_warrior"),
				factory.create(0)
						.title(Component.translatable("itemGroup.echo_warrior.echo_warrior"))
						.icon(() -> new ItemStack(ModItems.TEST_ECHO_SUMMONER))
						.displayItems((parameters, output) -> {
							output.accept(ModItems.TUTORIAL_MANUAL);
							output.accept(ModItems.ECHO_COMPASS);
							output.accept(ModItems.TEST_ECHO_SUMMONER);
							output.accept(ModBlocks.ECHO_RECYCLER);
							output.accept(ModItems.COURAGE_LEGACY);
							output.accept(ModItems.FORTITUDE_LEGACY);
							output.accept(ModItems.PURITY_LEGACY);
							output.accept(ModItems.WISDOM_LEGACY);
							output.accept(ModItems.CRAFT_LEGACY);
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

		ACCESSORIES = registrar.register(
				EchoWarrior.id("echo_warrior_accessories"),
				factory.create(1)
						.title(Component.translatable("itemGroup.echo_warrior.accessories"))
						.icon(() -> new ItemStack(ModItems.MEMORY_RITUAL_KNIFE_ACCESSORY))
						.displayItems((parameters, output) -> {
							output.accept(ModItems.PLATE_ARMOR_ACCESSORY);
							output.accept(ModItems.CHAINMAIL_ARMOR_ACCESSORY);
							output.accept(ModItems.SPIKED_ARMOR_ACCESSORY);
							output.accept(ModItems.BATTLE_WORN_WHETSTONE_ACCESSORY);
							output.accept(ModItems.MOUNTAIN_BURDEN_BLADE_ACCESSORY);
							output.accept(ModItems.FRACTURED_CRYSTAL_BLADE_ACCESSORY);
							output.accept(ModItems.TWIN_OATH_BADGE_ACCESSORY);
							output.accept(ModItems.BATTLE_BLINDFOLD_ACCESSORY);
							output.accept(ModItems.CRACK_RING_HAMMER_CHARM_ACCESSORY);
							output.accept(ModItems.VICTORS_LAUREL_ACCESSORY);
							output.accept(ModItems.BLOOD_PACT_FANG_ACCESSORY);
							output.accept(ModItems.MEMORY_RITUAL_KNIFE_ACCESSORY);
							output.accept(ModItems.SUBSTITUTE_DOLL_ACCESSORY);
							output.accept(ModItems.HEART_SPROUT_AMBER_ACCESSORY);
							output.accept(ModItems.FEAST_HAM_ACCESSORY);
							output.accept(ModItems.PEACEMAKER_ACCESSORY);
							output.accept(ModItems.SUNWHEEL_GARLAND_ACCESSORY);
							output.accept(ModItems.MOONDEW_BOTTLE_ACCESSORY);
							output.accept(ModItems.TOMATO_FISH_ACCESSORY);
							output.accept(ModItems.CAT_BELL_FISH_CHARM_ACCESSORY);
							output.accept(ModItems.LIGHT_GATHERING_MAGNET_ACCESSORY);
							output.accept(ModItems.TRAINING_NOTES_ACCESSORY);
							output.accept(ModItems.HAWKEYE_LENS_ACCESSORY);
							output.accept(ModItems.WINDCHASER_FEATHER_ACCESSORY);
							output.accept(ModItems.HOLLOW_BIRD_BONE_ACCESSORY);
						})
						.build()
		);

		KNOWLEDGE_FRAGMENTS = registrar.register(
				EchoWarrior.id("echo_warrior_knowledge_fragments"),
				factory.create(2)
						.title(Component.translatable("itemGroup.echo_warrior.knowledge_fragments"))
						.icon(() -> new ItemStack(ModItems.KNOWLEDGE_FRAGMENT_COLLECTION))
						.displayItems((parameters, output) -> {
							output.accept(fullKnowledgeCollection());
							for (KnowledgeCatalog.Entry entry : KnowledgeCatalog.entries()) {
								output.accept(KnowledgeStackData.fragment(entry.id()));
							}
						})
						.build()
		);
	}

	private static ItemStack fullKnowledgeCollection() {
		LinkedHashMap<String, Integer> pages = new LinkedHashMap<>();
		for (KnowledgeCatalog.Entry entry : KnowledgeCatalog.entries()) {
			pages.put(entry.id(), 1);
		}
		String firstPage = KnowledgeCatalog.entries().isEmpty() ? "" : KnowledgeCatalog.entries().getFirst().id();
		return KnowledgeStackData.collection(pages, firstPage);
	}

	@FunctionalInterface
	public interface TabBuilderFactory {
		CreativeModeTab.Builder create(int index);
	}
}
