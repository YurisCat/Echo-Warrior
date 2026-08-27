package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.EchoCompassItem;
import com.yuriscat.echowarrior.item.EchoModuleItem;
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
	public static final Item SMALL_KNOWLEDGE = register("small_knowledge", properties -> new Item(properties.stacksTo(64)));
	public static final Item COURAGE_LEGACY = register("courage_legacy", properties -> new Item(properties.stacksTo(64)));
	public static final Item FORTITUDE_LEGACY = register("fortitude_legacy", properties -> new Item(properties.stacksTo(64)));
	public static final Item PURITY_LEGACY = register("purity_legacy", properties -> new Item(properties.stacksTo(64)));
	public static final Item WISDOM_LEGACY = register("wisdom_legacy", properties -> new Item(properties.stacksTo(64)));
	public static final Item CRAFT_LEGACY = register("craft_legacy", properties -> new Item(properties.stacksTo(64)));
	public static final EchoModuleItem PLATE_ARMOR_MODULE = register(
			"plate_armor_module",
			properties -> new EchoModuleItem(properties.stacksTo(1), EchoModuleItem.ModuleType.PLATE_ARMOR)
	);
	public static final EchoModuleItem CHAINMAIL_ARMOR_MODULE = register(
			"chainmail_armor_module",
			properties -> new EchoModuleItem(properties.stacksTo(1), EchoModuleItem.ModuleType.CHAINMAIL_ARMOR)
	);
	public static final EchoModuleItem SPIKED_ARMOR_MODULE = register(
			"spiked_armor_module",
			properties -> new EchoModuleItem(properties.stacksTo(1), EchoModuleItem.ModuleType.SPIKED_ARMOR)
	);
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

	private static <T extends Item> T register(String path, java.util.function.Function<Item.Properties, T> factory) {
		Identifier id = EchoWarrior.id(path);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
		T item = factory.apply(new Item.Properties().setId(key));
		return Registry.register(BuiltInRegistries.ITEM, id, item);
	}
}
