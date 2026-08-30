package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.EchoCompassItem;
import com.yuriscat.echowarrior.item.EchoAccessoryItem;
import com.yuriscat.echowarrior.item.LegacyItem;
import com.yuriscat.echowarrior.item.KnowledgeFragmentCollectionItem;
import com.yuriscat.echowarrior.item.KnowledgeFragmentItem;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static final EchoCompassItem ECHO_COMPASS = register(
			"echo_compass",
			properties -> new EchoCompassItem(properties.stacksTo(1))
	);
	public static final KnowledgeFragmentItem KNOWLEDGE_FRAGMENT = register(
			"knowledge_fragment", properties -> new KnowledgeFragmentItem(properties.stacksTo(64)));
	public static final KnowledgeFragmentCollectionItem KNOWLEDGE_FRAGMENT_COLLECTION = register(
			"knowledge_fragment_collection", properties -> new KnowledgeFragmentCollectionItem(properties.stacksTo(1)));
	public static final LegacyItem COURAGE_LEGACY = legacy("courage_legacy", LegacyItem.LegacyType.COURAGE);
	public static final LegacyItem FORTITUDE_LEGACY = legacy("fortitude_legacy", LegacyItem.LegacyType.FORTITUDE);
	public static final LegacyItem PURITY_LEGACY = legacy("purity_legacy", LegacyItem.LegacyType.PURITY);
	public static final LegacyItem WISDOM_LEGACY = legacy("wisdom_legacy", LegacyItem.LegacyType.WISDOM);
	public static final LegacyItem CRAFT_LEGACY = legacy("craft_legacy", LegacyItem.LegacyType.CRAFT);

	public static final EchoAccessoryItem PLATE_ARMOR_ACCESSORY = accessory("plate_armor_accessory", EchoAccessoryItem.AccessoryType.PLATE_ARMOR);
	public static final EchoAccessoryItem CHAINMAIL_ARMOR_ACCESSORY = accessory("chainmail_armor_accessory", EchoAccessoryItem.AccessoryType.CHAINMAIL_ARMOR);
	public static final EchoAccessoryItem SPIKED_ARMOR_ACCESSORY = accessory("spiked_armor_accessory", EchoAccessoryItem.AccessoryType.SPIKED_ARMOR);
	public static final EchoAccessoryItem BATTLE_WORN_WHETSTONE_ACCESSORY = accessory("battle_worn_whetstone_accessory", EchoAccessoryItem.AccessoryType.BATTLE_WORN_WHETSTONE);
	public static final EchoAccessoryItem MOUNTAIN_BURDEN_BLADE_ACCESSORY = accessory("mountain_burden_blade_accessory", EchoAccessoryItem.AccessoryType.MOUNTAIN_BURDEN_BLADE);
	public static final EchoAccessoryItem FRACTURED_CRYSTAL_BLADE_ACCESSORY = accessory("fractured_crystal_blade_accessory", EchoAccessoryItem.AccessoryType.FRACTURED_CRYSTAL_BLADE);
	public static final EchoAccessoryItem TWIN_OATH_BADGE_ACCESSORY = accessory("twin_oath_badge_accessory", EchoAccessoryItem.AccessoryType.TWIN_OATH_BADGE);
	public static final EchoAccessoryItem BATTLE_BLINDFOLD_ACCESSORY = accessory("battle_blindfold_accessory", EchoAccessoryItem.AccessoryType.BATTLE_BLINDFOLD);
	public static final EchoAccessoryItem CRACK_RING_HAMMER_CHARM_ACCESSORY = accessory("crack_ring_hammer_charm_accessory", EchoAccessoryItem.AccessoryType.CRACK_RING_HAMMER_CHARM);
	public static final EchoAccessoryItem VICTORS_LAUREL_ACCESSORY = accessory("victors_laurel_accessory", EchoAccessoryItem.AccessoryType.VICTORS_LAUREL);
	public static final EchoAccessoryItem BLOOD_PACT_FANG_ACCESSORY = accessory("blood_pact_fang_accessory", EchoAccessoryItem.AccessoryType.BLOOD_PACT_FANG);
	public static final EchoAccessoryItem MEMORY_RITUAL_KNIFE_ACCESSORY = accessory("memory_ritual_knife_accessory", EchoAccessoryItem.AccessoryType.MEMORY_RITUAL_KNIFE);
	public static final EchoAccessoryItem SUBSTITUTE_DOLL_ACCESSORY = accessory("substitute_doll_accessory", EchoAccessoryItem.AccessoryType.SUBSTITUTE_DOLL);
	public static final EchoAccessoryItem HEART_SPROUT_AMBER_ACCESSORY = accessory("heart_sprout_amber_accessory", EchoAccessoryItem.AccessoryType.HEART_SPROUT_AMBER);
	public static final EchoAccessoryItem FEAST_HAM_ACCESSORY = accessory("feast_ham_accessory", EchoAccessoryItem.AccessoryType.FEAST_HAM);
	public static final EchoAccessoryItem PEACEMAKER_ACCESSORY = accessory("peacemaker_accessory", EchoAccessoryItem.AccessoryType.PEACEMAKER);
	public static final EchoAccessoryItem SUNWHEEL_GARLAND_ACCESSORY = accessory("sunwheel_garland_accessory", EchoAccessoryItem.AccessoryType.SUNWHEEL_GARLAND);
	public static final EchoAccessoryItem MOONDEW_BOTTLE_ACCESSORY = accessory("moondew_bottle_accessory", EchoAccessoryItem.AccessoryType.MOONDEW_BOTTLE);
	public static final EchoAccessoryItem TOMATO_FISH_ACCESSORY = accessory("tomato_fish_accessory", EchoAccessoryItem.AccessoryType.TOMATO_FISH);
	public static final EchoAccessoryItem CAT_BELL_FISH_CHARM_ACCESSORY = accessory("cat_bell_fish_charm_accessory", EchoAccessoryItem.AccessoryType.CAT_BELL_FISH_CHARM);
	public static final EchoAccessoryItem LIGHT_GATHERING_MAGNET_ACCESSORY = accessory("light_gathering_magnet_accessory", EchoAccessoryItem.AccessoryType.LIGHT_GATHERING_MAGNET);
	public static final EchoAccessoryItem TRAINING_NOTES_ACCESSORY = accessory("training_notes_accessory", EchoAccessoryItem.AccessoryType.TRAINING_NOTES);
	public static final EchoAccessoryItem HAWKEYE_LENS_ACCESSORY = accessory("hawkeye_lens_accessory", EchoAccessoryItem.AccessoryType.HAWKEYE_LENS);
	public static final EchoAccessoryItem WINDCHASER_FEATHER_ACCESSORY = accessory("windchaser_feather_accessory", EchoAccessoryItem.AccessoryType.WINDCHASER_FEATHER);
	public static final EchoAccessoryItem HOLLOW_BIRD_BONE_ACCESSORY = accessory("hollow_bird_bone_accessory", EchoAccessoryItem.AccessoryType.HOLLOW_BIRD_BONE);
	public static final TestEchoSummonerItem TEST_ECHO_SUMMONER = register(
			"test_echo_summoner",
			properties -> new TestEchoSummonerItem(properties.stacksTo(1))
	);
	public static final EchoRelicItem ROMAN_LEGIONARY_RELIC = register(
			"roman_legionary_relic",
			properties -> new EchoRelicItem(properties.stacksTo(1), EchoHeroType.ROMAN_LEGIONARY)
	);
	public static final EchoRelicItem AZTEC_WARRIOR_RELIC = register(
			"aztec_warrior_relic",
			properties -> new EchoRelicItem(properties.stacksTo(1), EchoHeroType.AZTEC_WARRIOR)
	);
	public static final EchoRelicItem EGYPTIAN_ARCHER_RELIC = register(
			"egyptian_archer_relic",
			properties -> new EchoRelicItem(properties.stacksTo(1), EchoHeroType.EGYPTIAN_ARCHER)
	);
	public static final EchoRelicItem GUANDAO_WARRIOR_RELIC = register(
			"guandao_warrior_relic",
			properties -> new EchoRelicItem(properties.stacksTo(1), EchoHeroType.GUANDAO_WARRIOR)
	);
	public static final EchoRelicItem JAPANESE_SAMURAI_RELIC = register(
			"japanese_samurai_relic",
			properties -> new EchoRelicItem(properties.stacksTo(1), EchoHeroType.JAPANESE_SAMURAI)
	);

	private ModItems() {
	}

	public static void initialize() {
	}

	private static LegacyItem legacy(String path, LegacyItem.LegacyType type) {
		return register(path, properties -> new LegacyItem(properties.stacksTo(64), type));
	}

	private static EchoAccessoryItem accessory(String path, EchoAccessoryItem.AccessoryType type) {
		return register(path, properties -> new EchoAccessoryItem(properties.stacksTo(1).rarity(type.rarity()), type));
	}

	private static <T extends Item> T register(String path, java.util.function.Function<Item.Properties, T> factory) {
		Identifier id = EchoWarrior.id(path);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		T item = factory.apply(new Item.Properties().setId(key));
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}
}
