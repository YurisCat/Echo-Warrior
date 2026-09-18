package com.yuriscat.echowarrior.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.biome.Biome;

public final class ModTags1211 {
    public static final TagKey<Biome> AZTEC_FAVORED_BIOMES = TagKey.create(
            Registries.BIOME, EchoWarrior1211.id("aztec_favored_biomes"));
    public static final TagKey<Item> BATTLEFIELD_RELICS = itemTag("battlefield_relics");
    public static final TagKey<Item> RECYCLER_KNOWLEDGE = itemTag("recycler/knowledge");
    public static final TagKey<Item> RECYCLER_LEGACY = itemTag("recycler/legacy");
    public static final TagKey<Item> RECYCLER_ACCESSORY_COMMON = itemTag("recycler/accessory_common");
    public static final TagKey<Item> RECYCLER_ACCESSORY_UNCOMMON = itemTag("recycler/accessory_uncommon");
    public static final TagKey<Item> RECYCLER_ACCESSORY_RARE = itemTag("recycler/accessory_rare");
    public static final TagKey<Item> RECYCLER_RELIC = itemTag("recycler/relic");
    public static final TagKey<Biome> HAS_BATTLEFIELD_RUIN = TagKey.create(
            Registries.BIOME, EchoWarrior1211.id("has_battlefield_ruin"));
    public static final TagKey<Block> BATTLEFIELD_BRUSHABLES = TagKey.create(
            Registries.BLOCK, EchoWarrior1211.id("battlefield_brushables"));

    private ModTags1211() { }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, EchoWarrior1211.id(path));
    }
}
