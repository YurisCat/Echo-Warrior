package com.yuriscat.echowarrior;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;

public final class ModTags {
	public static final TagKey<Biome> AZTEC_FAVORED_BIOMES = TagKey.create(
			Registries.BIOME,
			EchoWarrior.id("aztec_favored_biomes")
	);
	public static final TagKey<Item> SUMMONER_MODULES = TagKey.create(
			Registries.ITEM,
			EchoWarrior.id("summoner_modules")
	);

	private ModTags() {
	}
}
