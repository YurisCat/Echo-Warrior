package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
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
