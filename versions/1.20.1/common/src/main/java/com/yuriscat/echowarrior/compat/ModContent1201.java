package com.yuriscat.echowarrior.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import com.yuriscat.echowarrior.compat.item.EchoSummonerItem1201;
import com.yuriscat.echowarrior.compat.menu.SummonerMenu1201;
import net.minecraft.world.inventory.MenuType;

/** Isolated 1.20.1 gameplay registry. Loader factories bind menus before play. */
public final class ModContent1201 {
    private static final java.util.Map<ResourceLocation, Item> ITEMS = new java.util.LinkedHashMap<>();
    private static final java.util.Map<ResourceLocation, net.minecraft.world.level.block.Block> BLOCKS = new java.util.LinkedHashMap<>();
    public static final net.minecraft.world.level.block.Block SUSPICIOUS_GRASS_BLOCK = brushable("suspicious_grass_block", net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
    public static final net.minecraft.world.level.block.Block SUSPICIOUS_DIRT = brushable("suspicious_dirt", net.minecraft.world.level.block.Blocks.DIRT);
    public static final Item SUSPICIOUS_GRASS_BLOCK_ITEM = register("suspicious_grass_block", new com.yuriscat.echowarrior.compat.item.SuspiciousBlockItem1201(SUSPICIOUS_GRASS_BLOCK, new Item.Properties().stacksTo(1)));
    public static final Item SUSPICIOUS_DIRT_ITEM = register("suspicious_dirt", new com.yuriscat.echowarrior.compat.item.SuspiciousBlockItem1201(SUSPICIOUS_DIRT, new Item.Properties().stacksTo(1)));
    public static final Item ECHO_COMPASS = register("echo_compass", new com.yuriscat.echowarrior.compat.item.EchoCompassItem1201(new Item.Properties().stacksTo(1)));
    public static final com.yuriscat.echowarrior.compat.block.RecyclerChestBlock1201 ECHO_RECYCLER = block("echo_recycler",
            new com.yuriscat.echowarrior.compat.block.RecyclerChestBlock1201(net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.CHEST).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    public static net.minecraft.world.level.block.entity.BlockEntityType<com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1201> RECYCLER_CHEST;
    public static MenuType<com.yuriscat.echowarrior.compat.menu.RecyclerMenu1201> RECYCLER_MENU;
    public static final Item ECHO_RECYCLER_ITEM = register("echo_recycler", new com.yuriscat.echowarrior.compat.item.RecyclerChestItem1201(ECHO_RECYCLER, new Item.Properties()));
    public static final ResourceLocation SUMMONER_ID = new ResourceLocation(EchoWarrior1201.MOD_ID, "test_echo_summoner");
    public static final Item ECHO_SUMMONER = register("test_echo_summoner", new EchoSummonerItem1201());
    private static final java.util.Map<EchoHeroType1201, Item> RELICS = new java.util.EnumMap<>(EchoHeroType1201.class);
    private static final java.util.Map<com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType, Item> ACCESSORIES =
            new java.util.EnumMap<>(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.class);
    static {
        for (var hero : EchoHeroType1201.values()) RELICS.put(hero, register(hero.id() + "_relic",
                new com.yuriscat.echowarrior.compat.item.EchoRelicItem1201(new Item.Properties().stacksTo(1), hero)));
        for (var type : com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.values()) {
            ACCESSORIES.put(type, register(type.id() + "_accessory",
                    new com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201(new Item.Properties().stacksTo(1), type)));
        }
    }
    public static final ResourceLocation MENU_ID = new ResourceLocation(EchoWarrior1201.MOD_ID, "echo_summoner");
    // 1.20.1 vanilla constructors are private; each loader supplies its supported factory.
    public static MenuType<SummonerMenu1201> SUMMONER_MENU;
    public static MenuType<com.yuriscat.echowarrior.compat.menu.KnowledgeReaderMenu1201> KNOWLEDGE_READER_MENU;
    public static MenuType<com.yuriscat.echowarrior.compat.menu.TutorialManualMenu1201> TUTORIAL_MANUAL_MENU;
    public static final Item KNOWLEDGE_FRAGMENT = register("knowledge_fragment", new com.yuriscat.echowarrior.compat.item.KnowledgeFragmentItem1201(new Item.Properties()));
    public static final Item KNOWLEDGE_FRAGMENT_COLLECTION = register("knowledge_fragment_collection", new com.yuriscat.echowarrior.compat.item.KnowledgeFragmentCollectionItem1201(new Item.Properties().stacksTo(1)));
    public static final Item TUTORIAL_MANUAL = register("tutorial_manual", new com.yuriscat.echowarrior.compat.item.TutorialManualItem1201(new Item.Properties().stacksTo(1)));
    public static final net.minecraft.world.item.crafting.RecipeSerializer<com.yuriscat.echowarrior.compat.recipe.KnowledgeFragmentCollectionRecipe1201> KNOWLEDGE_FRAGMENT_COLLECTION_SERIALIZER =
            new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(com.yuriscat.echowarrior.compat.recipe.KnowledgeFragmentCollectionRecipe1201::new);
    public static final net.minecraft.world.item.crafting.RecipeSerializer<com.yuriscat.echowarrior.compat.recipe.CraftLegacyRepairRecipe1201> CRAFT_LEGACY_REPAIR_SERIALIZER =
            new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(com.yuriscat.echowarrior.compat.recipe.CraftLegacyRepairRecipe1201::new);

    private static net.minecraft.world.level.block.Block brushable(String name, net.minecraft.world.level.block.Block original) {
        return block(name, new com.yuriscat.echowarrior.compat.block.StableBrushableBlock1201(original,
                net.minecraft.sounds.SoundEvents.BRUSH_GRAVEL, net.minecraft.sounds.SoundEvents.BRUSH_GRAVEL,
                net.minecraft.world.level.block.state.BlockBehaviour.Properties.copy(original).strength(0.5F)
                        .noLootTable().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    }

    private static Item register(String name, Item item) {
        ITEMS.put(new ResourceLocation(EchoWarrior1201.MOD_ID, name), item);
        return item;
    }
    public static java.util.Map<ResourceLocation, Item> items() { return java.util.Collections.unmodifiableMap(ITEMS); }
    private static <T extends net.minecraft.world.level.block.Block> T block(String name, T block) {
        BLOCKS.put(id(name), block); return block;
    }
    public static java.util.Map<ResourceLocation, net.minecraft.world.level.block.Block> blocks() { return java.util.Collections.unmodifiableMap(BLOCKS); }
    public static Item relic(EchoHeroType1201 hero) { return RELICS.get(hero); }
    public static Item accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType type) {
        return ACCESSORIES.get(type);
    }

    public static final Item PLATE_ARMOR_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.PLATE_ARMOR);
    public static final Item CHAINMAIL_ARMOR_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.CHAINMAIL_ARMOR);
    public static final Item SPIKED_ARMOR_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.SPIKED_ARMOR);
    public static final Item BATTLE_WORN_WHETSTONE_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.BATTLE_WORN_WHETSTONE);
    public static final Item MOUNTAIN_BURDEN_BLADE_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.MOUNTAIN_BURDEN_BLADE);
    public static final Item FRACTURED_CRYSTAL_BLADE_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.FRACTURED_CRYSTAL_BLADE);
    public static final Item TWIN_OATH_BADGE_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.TWIN_OATH_BADGE);
    public static final Item BATTLE_BLINDFOLD_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.BATTLE_BLINDFOLD);
    public static final Item CRACK_RING_HAMMER_CHARM_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.CRACK_RING_HAMMER_CHARM);
    public static final Item VICTORS_LAUREL_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.VICTORS_LAUREL);
    public static final Item BLOOD_PACT_FANG_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.BLOOD_PACT_FANG);
    public static final Item MEMORY_RITUAL_KNIFE_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.MEMORY_RITUAL_KNIFE);
    public static final Item SUBSTITUTE_DOLL_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.SUBSTITUTE_DOLL);
    public static final Item HEART_SPROUT_AMBER_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.HEART_SPROUT_AMBER);
    public static final Item FEAST_HAM_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.FEAST_HAM);
    public static final Item PEACEMAKER_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.PEACEMAKER);
    public static final Item SUNWHEEL_GARLAND_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.SUNWHEEL_GARLAND);
    public static final Item MOONDEW_BOTTLE_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.MOONDEW_BOTTLE);
    public static final Item TOMATO_FISH_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.TOMATO_FISH);
    public static final Item CAT_BELL_FISH_CHARM_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.CAT_BELL_FISH_CHARM);
    public static final Item LIGHT_GATHERING_MAGNET_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.LIGHT_GATHERING_MAGNET);
    public static final Item TRAINING_NOTES_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.TRAINING_NOTES);
    public static final Item HAWKEYE_LENS_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.HAWKEYE_LENS);
    public static final Item WINDCHASER_FEATHER_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.WINDCHASER_FEATHER);
    public static final Item HOLLOW_BIRD_BONE_ACCESSORY = accessory(com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType.HOLLOW_BIRD_BONE);
    public static final Item COURAGE_LEGACY = register("courage_legacy", new com.yuriscat.echowarrior.compat.item.LegacyItem1201(new Item.Properties(), com.yuriscat.echowarrior.compat.item.LegacyItem1201.LegacyType.COURAGE));
    public static final Item FORTITUDE_LEGACY = register("fortitude_legacy", new com.yuriscat.echowarrior.compat.item.LegacyItem1201(new Item.Properties(), com.yuriscat.echowarrior.compat.item.LegacyItem1201.LegacyType.FORTITUDE));
    public static final Item PURITY_LEGACY = register("purity_legacy", new com.yuriscat.echowarrior.compat.item.LegacyItem1201(new Item.Properties(), com.yuriscat.echowarrior.compat.item.LegacyItem1201.LegacyType.PURITY));
    public static final Item WISDOM_LEGACY = register("wisdom_legacy", new com.yuriscat.echowarrior.compat.item.LegacyItem1201(new Item.Properties(), com.yuriscat.echowarrior.compat.item.LegacyItem1201.LegacyType.WISDOM));
    public static final Item CRAFT_LEGACY = register("craft_legacy", new com.yuriscat.echowarrior.compat.item.LegacyItem1201(new Item.Properties(), com.yuriscat.echowarrior.compat.item.LegacyItem1201.LegacyType.CRAFT));

    private static final java.util.Map<ResourceLocation, net.minecraft.world.entity.EntityType<?>> ENTITIES = new java.util.LinkedHashMap<>();
    public static final net.minecraft.world.entity.EntityType<com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1201> ROMAN_LEGIONARY_ECHO = entity("roman_legionary_echo",
            com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1201::new, net.minecraft.world.entity.MobCategory.CREATURE, 0.75F, 1.95F, 10, 3);
    public static final net.minecraft.world.entity.EntityType<com.yuriscat.echowarrior.compat.entity.AztecWarriorEchoEntity1201> AZTEC_WARRIOR_ECHO = entity("aztec_warrior_echo",
            com.yuriscat.echowarrior.compat.entity.AztecWarriorEchoEntity1201::new, net.minecraft.world.entity.MobCategory.CREATURE, 0.8F, 2F, 10, 3);
    public static final net.minecraft.world.entity.EntityType<com.yuriscat.echowarrior.compat.entity.GuandaoWarriorEchoEntity1201> GUANDAO_WARRIOR_ECHO = entity("guandao_warrior_echo",
            com.yuriscat.echowarrior.compat.entity.GuandaoWarriorEchoEntity1201::new, net.minecraft.world.entity.MobCategory.CREATURE, 0.85F, 2.1F, 10, 3);
    public static final net.minecraft.world.entity.EntityType<com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1201> JAPANESE_SAMURAI_ECHO = entity("japanese_samurai_echo",
            com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1201::new, net.minecraft.world.entity.MobCategory.CREATURE, 0.75F, 1.95F, 10, 3);
    public static final net.minecraft.world.entity.EntityType<com.yuriscat.echowarrior.compat.entity.EgyptianArcherEchoEntity1201> EGYPTIAN_ARCHER_ECHO = entity("egyptian_archer_echo",
            com.yuriscat.echowarrior.compat.entity.EgyptianArcherEchoEntity1201::new, net.minecraft.world.entity.MobCategory.CREATURE, 0.75F, 1.95F, 10, 3);
    public static final net.minecraft.world.entity.EntityType<com.yuriscat.echowarrior.compat.entity.EgyptianArcherArrowEntity1201> EGYPTIAN_ARCHER_ARROW = entity("egyptian_archer_arrow",
            com.yuriscat.echowarrior.compat.entity.EgyptianArcherArrowEntity1201::new, net.minecraft.world.entity.MobCategory.MISC, 0.5F, 0.5F, 4, 1);
    private static <T extends net.minecraft.world.entity.Entity> net.minecraft.world.entity.EntityType<T> entity(
            String path, net.minecraft.world.entity.EntityType.EntityFactory<T> factory,
            net.minecraft.world.entity.MobCategory category, float width, float height, int tracking, int interval) {
        // 1.20.1 Builder.build(String) probes Mojang's fixed vanilla DFU schema even for new mod IDs.
        // Use the same public constructor as FabricEntityTypeBuilder; retain serialize=true.
        // Our explicit NBT readers own mod-field migration, not a nonexistent vanilla DFU choice.
        var result = new net.minecraft.world.entity.EntityType<T>(factory, category, true, true, false,
                category == net.minecraft.world.entity.MobCategory.CREATURE || category == net.minecraft.world.entity.MobCategory.MISC,
                com.google.common.collect.ImmutableSet.of(), net.minecraft.world.entity.EntityDimensions.scalable(width, height),
                tracking, interval, net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
        ENTITIES.put(id(path), result);
        return result;
    }
    public static java.util.Map<ResourceLocation, net.minecraft.world.entity.EntityType<?>> entities() { return java.util.Collections.unmodifiableMap(ENTITIES); }
    public static ResourceLocation id(String path) { return new ResourceLocation(EchoWarrior1201.MOD_ID, path); }
    private ModContent1201() {}
}
