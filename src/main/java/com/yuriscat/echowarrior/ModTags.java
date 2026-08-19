package com.yuriscat.echowarrior;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class ModTags {
	public static final TagKey<Biome> AZTEC_FAVORED_BIOMES = TagKey.create(
			Registries.BIOME,
			EchoWarrior.id("aztec_favored_biomes")
	);

	private ModTags() {
	}
}
