package com.yuriscat.echowarrior;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biome;

public final class ModTags {
	public static final TagKey<Biome> AZTEC_FAVORED_BIOMES = TagKey.create(
			Registries.BIOME,
			EchoWarrior.id("aztec_favored_biomes")
	);
	public static final TagKey<Item> SUMMONER_ACCESSORIES = TagKey.create(
			Registries.ITEM,
			EchoWarrior.id("summoner_accessories")
	);
	/** Reserved for future special-enemy inheritance drops. */
	public static final TagKey<Item> LEGACIES = TagKey.create(Registries.ITEM, EchoWarrior.id("legacies"));
	public static final TagKey<Item> BATTLEFIELD_RELICS = TagKey.create(
			Registries.ITEM,
			EchoWarrior.id("battlefield_relics")
	);
	public static final TagKey<Item> RECYCLER_KNOWLEDGE = itemTag("recycler/knowledge");
	public static final TagKey<Item> RECYCLER_LEGACY = itemTag("recycler/legacy");
	public static final TagKey<Item> RECYCLER_ACCESSORY_COMMON = itemTag("recycler/accessory_common");
	public static final TagKey<Item> RECYCLER_ACCESSORY_UNCOMMON = itemTag("recycler/accessory_uncommon");
	public static final TagKey<Item> RECYCLER_ACCESSORY_RARE = itemTag("recycler/accessory_rare");
	public static final TagKey<Item> RECYCLER_RELIC = itemTag("recycler/relic");
	public static final TagKey<Biome> HAS_BATTLEFIELD_RUIN = TagKey.create(
			Registries.BIOME,
			EchoWarrior.id("has_battlefield_ruin")
	);
	public static final TagKey<Block> BATTLEFIELD_BRUSHABLES = TagKey.create(
			Registries.BLOCK,
			EchoWarrior.id("battlefield_brushables")
	);

	private ModTags() {
	}

	private static TagKey<Item> itemTag(String path) {
		return TagKey.create(Registries.ITEM, EchoWarrior.id(path));
	}
}
