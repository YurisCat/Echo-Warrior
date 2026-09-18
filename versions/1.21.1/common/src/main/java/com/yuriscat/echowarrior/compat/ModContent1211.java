package com.yuriscat.echowarrior.compat;

import com.yuriscat.echowarrior.compat.entity.AztecWarriorEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.GuandaoWarriorEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.EgyptianArcherArrowEntity1211;
import com.yuriscat.echowarrior.compat.entity.EgyptianArcherEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1211;
import com.yuriscat.echowarrior.compat.block.RecyclerChestBlock1211;
import com.yuriscat.echowarrior.compat.block.StableBrushableBlock1211;
import com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1211;
import com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1211;
import com.yuriscat.echowarrior.compat.item.EchoCompassItem1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicItem1211;
import com.yuriscat.echowarrior.compat.item.EchoSummonerItem1211;
import com.yuriscat.echowarrior.compat.item.KnowledgeFragmentCollectionItem1211;
import com.yuriscat.echowarrior.compat.item.KnowledgeFragmentItem1211;
import com.yuriscat.echowarrior.compat.item.LegacyItem1211;
import com.yuriscat.echowarrior.compat.item.RecyclerChestItem1211;
import com.yuriscat.echowarrior.compat.item.SuspiciousBlockItem1211;
import com.yuriscat.echowarrior.compat.item.TutorialManualItem1211;
import com.yuriscat.echowarrior.compat.menu.SummonerMenu1211;
import com.yuriscat.echowarrior.compat.menu.RecyclerMenu1211;
import com.yuriscat.echowarrior.compat.menu.KnowledgeReaderMenu1211;
import com.yuriscat.echowarrior.compat.menu.TutorialManualMenu1211;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import com.yuriscat.echowarrior.compat.recipe.CraftLegacyRepairRecipe1211;
import com.yuriscat.echowarrior.compat.recipe.KnowledgeFragmentCollectionRecipe1211;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared content catalogue so Fabric and NeoForge register exactly the same 1.21.1 objects. */
public final class ModContent1211 {
    private static final Map<ResourceLocation, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, EntityType<?>> ENTITY_TYPES = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, BlockEntityType<?>> BLOCK_ENTITY_TYPES = new LinkedHashMap<>();

    public static final String ROMAN_RELIC_ID = "roman_legionary_relic";
    public static final String AZTEC_RELIC_ID = "aztec_warrior_relic";
    public static final String EGYPTIAN_RELIC_ID = "egyptian_archer_relic";
    public static final String GUANDAO_RELIC_ID = "guandao_warrior_relic";
    public static final String JAPANESE_RELIC_ID = "japanese_samurai_relic";
    public static final String SUMMONER_ID = "test_echo_summoner";
    public static final String ECHO_COMPASS_ID = "echo_compass";
    public static final String KNOWLEDGE_FRAGMENT_ID = "knowledge_fragment";
    public static final String KNOWLEDGE_FRAGMENT_COLLECTION_ID = "knowledge_fragment_collection";
    public static final String TUTORIAL_MANUAL_ID = "tutorial_manual";
    public static final String ROMAN_ENTITY_ID = "roman_legionary_echo";
    public static final String AZTEC_ENTITY_ID = "aztec_warrior_echo";
    public static final String GUANDAO_ENTITY_ID = "guandao_warrior_echo";
    public static final String JAPANESE_ENTITY_ID = "japanese_samurai_echo";
    public static final String EGYPTIAN_ENTITY_ID = "egyptian_archer_echo";
    public static final String EGYPTIAN_ARROW_ENTITY_ID = "egyptian_archer_arrow";
    public static final String SUMMONER_MENU_ID = "summoner";
    public static final String RECYCLER_MENU_ID = "recycler";
    public static final String KNOWLEDGE_READER_MENU_ID = "knowledge_reader";
    public static final String TUTORIAL_MANUAL_MENU_ID = "tutorial_manual";
    public static final String ECHO_RECYCLER_ID = "echo_recycler";
    public static final String SUSPICIOUS_GRASS_BLOCK_ID = "suspicious_grass_block";
    public static final String SUSPICIOUS_DIRT_ID = "suspicious_dirt";
    public static final String RECYCLER_CHEST_BLOCK_ENTITY_ID = "recycler_chest";
    public static final String CRAFT_LEGACY_REPAIR_RECIPE_ID = "craft_legacy_repair";
    public static final String KNOWLEDGE_FRAGMENT_COLLECTION_RECIPE_ID = "knowledge_fragment_collection";
    public static final String COURAGE_LEGACY_ID = "courage_legacy";
    public static final String FORTITUDE_LEGACY_ID = "fortitude_legacy";
    public static final String PURITY_LEGACY_ID = "purity_legacy";
    public static final String WISDOM_LEGACY_ID = "wisdom_legacy";
    public static final String CRAFT_LEGACY_ID = "craft_legacy";

    public static final String PLATE_ARMOR_ACCESSORY_ID = "plate_armor_accessory";
    public static final String CHAINMAIL_ARMOR_ACCESSORY_ID = "chainmail_armor_accessory";
    public static final String SPIKED_ARMOR_ACCESSORY_ID = "spiked_armor_accessory";
    public static final String BATTLE_WORN_WHETSTONE_ACCESSORY_ID = "battle_worn_whetstone_accessory";
    public static final String MOUNTAIN_BURDEN_BLADE_ACCESSORY_ID = "mountain_burden_blade_accessory";
    public static final String FRACTURED_CRYSTAL_BLADE_ACCESSORY_ID = "fractured_crystal_blade_accessory";
    public static final String TWIN_OATH_BADGE_ACCESSORY_ID = "twin_oath_badge_accessory";
    public static final String BATTLE_BLINDFOLD_ACCESSORY_ID = "battle_blindfold_accessory";
    public static final String CRACK_RING_HAMMER_CHARM_ACCESSORY_ID = "crack_ring_hammer_charm_accessory";
    public static final String VICTORS_LAUREL_ACCESSORY_ID = "victors_laurel_accessory";
    public static final String BLOOD_PACT_FANG_ACCESSORY_ID = "blood_pact_fang_accessory";
    public static final String MEMORY_RITUAL_KNIFE_ACCESSORY_ID = "memory_ritual_knife_accessory";
    public static final String SUBSTITUTE_DOLL_ACCESSORY_ID = "substitute_doll_accessory";
    public static final String HEART_SPROUT_AMBER_ACCESSORY_ID = "heart_sprout_amber_accessory";
    public static final String FEAST_HAM_ACCESSORY_ID = "feast_ham_accessory";
    public static final String PEACEMAKER_ACCESSORY_ID = "peacemaker_accessory";
    public static final String SUNWHEEL_GARLAND_ACCESSORY_ID = "sunwheel_garland_accessory";
    public static final String MOONDEW_BOTTLE_ACCESSORY_ID = "moondew_bottle_accessory";
    public static final String TOMATO_FISH_ACCESSORY_ID = "tomato_fish_accessory";
    public static final String CAT_BELL_FISH_CHARM_ACCESSORY_ID = "cat_bell_fish_charm_accessory";
    public static final String LIGHT_GATHERING_MAGNET_ACCESSORY_ID = "light_gathering_magnet_accessory";
    public static final String TRAINING_NOTES_ACCESSORY_ID = "training_notes_accessory";
    public static final String HAWKEYE_LENS_ACCESSORY_ID = "hawkeye_lens_accessory";
    public static final String WINDCHASER_FEATHER_ACCESSORY_ID = "windchaser_feather_accessory";
    public static final String HOLLOW_BIRD_BONE_ACCESSORY_ID = "hollow_bird_bone_accessory";

    public static final EchoRelicItem1211 ROMAN_LEGIONARY_RELIC = relic(ROMAN_RELIC_ID, EchoHeroType1211.ROMAN_LEGIONARY);
    public static final EchoRelicItem1211 AZTEC_WARRIOR_RELIC = relic(AZTEC_RELIC_ID, EchoHeroType1211.AZTEC_WARRIOR);
    public static final EchoRelicItem1211 EGYPTIAN_ARCHER_RELIC = relic(EGYPTIAN_RELIC_ID, EchoHeroType1211.EGYPTIAN_ARCHER);
    public static final EchoRelicItem1211 GUANDAO_WARRIOR_RELIC = relic(GUANDAO_RELIC_ID, EchoHeroType1211.GUANDAO_WARRIOR);
    public static final EchoRelicItem1211 JAPANESE_SAMURAI_RELIC = relic(JAPANESE_RELIC_ID, EchoHeroType1211.JAPANESE_SAMURAI);
    public static final EchoCompassItem1211 ECHO_COMPASS = registerItem(ECHO_COMPASS_ID,
            new EchoCompassItem1211(new Item.Properties().stacksTo(1)));
    public static final KnowledgeFragmentItem1211 KNOWLEDGE_FRAGMENT = registerItem(KNOWLEDGE_FRAGMENT_ID,
            new KnowledgeFragmentItem1211(new Item.Properties().stacksTo(64)));
    public static final KnowledgeFragmentCollectionItem1211 KNOWLEDGE_FRAGMENT_COLLECTION = registerItem(
            KNOWLEDGE_FRAGMENT_COLLECTION_ID,
            new KnowledgeFragmentCollectionItem1211(new Item.Properties().stacksTo(1)));
    public static final TutorialManualItem1211 TUTORIAL_MANUAL = registerItem(TUTORIAL_MANUAL_ID,
            new TutorialManualItem1211(new Item.Properties().stacksTo(1)));
    public static final EchoSummonerItem1211 ECHO_SUMMONER = registerItem(SUMMONER_ID,
            new EchoSummonerItem1211(new Item.Properties().stacksTo(1)));
    public static final LegacyItem1211 COURAGE_LEGACY = legacy(COURAGE_LEGACY_ID, LegacyItem1211.LegacyType.COURAGE);
    public static final LegacyItem1211 FORTITUDE_LEGACY = legacy(FORTITUDE_LEGACY_ID, LegacyItem1211.LegacyType.FORTITUDE);
    public static final LegacyItem1211 PURITY_LEGACY = legacy(PURITY_LEGACY_ID, LegacyItem1211.LegacyType.PURITY);
    public static final LegacyItem1211 WISDOM_LEGACY = legacy(WISDOM_LEGACY_ID, LegacyItem1211.LegacyType.WISDOM);
    public static final LegacyItem1211 CRAFT_LEGACY = legacy(CRAFT_LEGACY_ID, LegacyItem1211.LegacyType.CRAFT);

    public static final EchoAccessoryItem1211 PLATE_ARMOR_ACCESSORY = accessory(PLATE_ARMOR_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.PLATE_ARMOR);
    public static final EchoAccessoryItem1211 CHAINMAIL_ARMOR_ACCESSORY = accessory(CHAINMAIL_ARMOR_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.CHAINMAIL_ARMOR);
    public static final EchoAccessoryItem1211 SPIKED_ARMOR_ACCESSORY = accessory(SPIKED_ARMOR_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.SPIKED_ARMOR);
    public static final EchoAccessoryItem1211 BATTLE_WORN_WHETSTONE_ACCESSORY = accessory(BATTLE_WORN_WHETSTONE_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.BATTLE_WORN_WHETSTONE);
    public static final EchoAccessoryItem1211 MOUNTAIN_BURDEN_BLADE_ACCESSORY = accessory(MOUNTAIN_BURDEN_BLADE_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.MOUNTAIN_BURDEN_BLADE);
    public static final EchoAccessoryItem1211 FRACTURED_CRYSTAL_BLADE_ACCESSORY = accessory(FRACTURED_CRYSTAL_BLADE_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.FRACTURED_CRYSTAL_BLADE);
    public static final EchoAccessoryItem1211 TWIN_OATH_BADGE_ACCESSORY = accessory(TWIN_OATH_BADGE_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.TWIN_OATH_BADGE);
    public static final EchoAccessoryItem1211 BATTLE_BLINDFOLD_ACCESSORY = accessory(BATTLE_BLINDFOLD_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.BATTLE_BLINDFOLD);
    public static final EchoAccessoryItem1211 CRACK_RING_HAMMER_CHARM_ACCESSORY = accessory(CRACK_RING_HAMMER_CHARM_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.CRACK_RING_HAMMER_CHARM);
    public static final EchoAccessoryItem1211 VICTORS_LAUREL_ACCESSORY = accessory(VICTORS_LAUREL_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.VICTORS_LAUREL);
    public static final EchoAccessoryItem1211 BLOOD_PACT_FANG_ACCESSORY = accessory(BLOOD_PACT_FANG_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.BLOOD_PACT_FANG);
    public static final EchoAccessoryItem1211 MEMORY_RITUAL_KNIFE_ACCESSORY = accessory(MEMORY_RITUAL_KNIFE_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.MEMORY_RITUAL_KNIFE);
    public static final EchoAccessoryItem1211 SUBSTITUTE_DOLL_ACCESSORY = accessory(SUBSTITUTE_DOLL_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.SUBSTITUTE_DOLL);
    public static final EchoAccessoryItem1211 HEART_SPROUT_AMBER_ACCESSORY = accessory(HEART_SPROUT_AMBER_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.HEART_SPROUT_AMBER);
    public static final EchoAccessoryItem1211 FEAST_HAM_ACCESSORY = accessory(FEAST_HAM_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.FEAST_HAM);
    public static final EchoAccessoryItem1211 PEACEMAKER_ACCESSORY = accessory(PEACEMAKER_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.PEACEMAKER);
    public static final EchoAccessoryItem1211 SUNWHEEL_GARLAND_ACCESSORY = accessory(SUNWHEEL_GARLAND_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.SUNWHEEL_GARLAND);
    public static final EchoAccessoryItem1211 MOONDEW_BOTTLE_ACCESSORY = accessory(MOONDEW_BOTTLE_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.MOONDEW_BOTTLE);
    public static final EchoAccessoryItem1211 TOMATO_FISH_ACCESSORY = accessory(TOMATO_FISH_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.TOMATO_FISH);
    public static final EchoAccessoryItem1211 CAT_BELL_FISH_CHARM_ACCESSORY = accessory(CAT_BELL_FISH_CHARM_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.CAT_BELL_FISH_CHARM);
    public static final EchoAccessoryItem1211 LIGHT_GATHERING_MAGNET_ACCESSORY = accessory(LIGHT_GATHERING_MAGNET_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.LIGHT_GATHERING_MAGNET);
    public static final EchoAccessoryItem1211 TRAINING_NOTES_ACCESSORY = accessory(TRAINING_NOTES_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.TRAINING_NOTES);
    public static final EchoAccessoryItem1211 HAWKEYE_LENS_ACCESSORY = accessory(HAWKEYE_LENS_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.HAWKEYE_LENS);
    public static final EchoAccessoryItem1211 WINDCHASER_FEATHER_ACCESSORY = accessory(WINDCHASER_FEATHER_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.WINDCHASER_FEATHER);
    public static final EchoAccessoryItem1211 HOLLOW_BIRD_BONE_ACCESSORY = accessory(HOLLOW_BIRD_BONE_ACCESSORY_ID, EchoAccessoryItem1211.AccessoryType.HOLLOW_BIRD_BONE);

    public static final Block SUSPICIOUS_GRASS_BLOCK = registerBlock(SUSPICIOUS_GRASS_BLOCK_ID,
            new StableBrushableBlock1211(Blocks.GRASS_BLOCK, SoundEvents.BRUSH_GRAVEL, SoundEvents.BRUSH_GRAVEL,
                    BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.5F)
                            .sound(SoundType.GRASS).noLootTable().pushReaction(PushReaction.DESTROY)));
    public static final Block SUSPICIOUS_DIRT = registerBlock(SUSPICIOUS_DIRT_ID,
            new StableBrushableBlock1211(Blocks.DIRT, SoundEvents.BRUSH_GRAVEL, SoundEvents.BRUSH_GRAVEL,
                    BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.5F)
                            .sound(SoundType.ROOTED_DIRT).noLootTable().pushReaction(PushReaction.DESTROY)));
    public static final RecyclerChestBlock1211 ECHO_RECYCLER = registerBlock(ECHO_RECYCLER_ID,
            new RecyclerChestBlock1211(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST)
                    .pushReaction(PushReaction.BLOCK)));
    public static final Item SUSPICIOUS_GRASS_BLOCK_ITEM = registerItem(SUSPICIOUS_GRASS_BLOCK_ID,
            new SuspiciousBlockItem1211(SUSPICIOUS_GRASS_BLOCK, new Item.Properties().stacksTo(1)));
    public static final Item SUSPICIOUS_DIRT_ITEM = registerItem(SUSPICIOUS_DIRT_ID,
            new SuspiciousBlockItem1211(SUSPICIOUS_DIRT, new Item.Properties().stacksTo(1)));
    public static final RecyclerChestItem1211 ECHO_RECYCLER_ITEM = registerItem(ECHO_RECYCLER_ID,
            new RecyclerChestItem1211(ECHO_RECYCLER, new Item.Properties().stacksTo(64)));
    public static final BlockEntityType<RecyclerChestBlockEntity1211> RECYCLER_CHEST = registerBlockEntity(
            RECYCLER_CHEST_BLOCK_ENTITY_ID,
            BlockEntityType.Builder.of(RecyclerChestBlockEntity1211::new, ECHO_RECYCLER).build(null));

    public static final EntityType<RomanLegionaryEchoEntity1211> ROMAN_LEGIONARY_ECHO = registerEntity(
            ROMAN_ENTITY_ID,
            EntityType.Builder.of(RomanLegionaryEchoEntity1211::new, MobCategory.CREATURE)
                    .sized(0.75F, 1.95F)
                    .eyeHeight(1.75F)
                    .clientTrackingRange(10)
                    .build(id(ROMAN_ENTITY_ID).toString()));
    public static final EntityType<AztecWarriorEchoEntity1211> AZTEC_WARRIOR_ECHO = registerEntity(
            AZTEC_ENTITY_ID,
            EntityType.Builder.of(AztecWarriorEchoEntity1211::new, MobCategory.CREATURE)
                    .sized(0.8F, 2.0F)
                    .eyeHeight(1.78F)
                    .clientTrackingRange(10)
                    .build(id(AZTEC_ENTITY_ID).toString()));
    public static final EntityType<GuandaoWarriorEchoEntity1211> GUANDAO_WARRIOR_ECHO = registerEntity(
            GUANDAO_ENTITY_ID,
            EntityType.Builder.of(GuandaoWarriorEchoEntity1211::new, MobCategory.CREATURE)
                    .sized(0.85F, 2.1F)
                    .eyeHeight(1.85F)
                    .clientTrackingRange(10)
                    .build(id(GUANDAO_ENTITY_ID).toString()));
    public static final EntityType<JapaneseSamuraiEchoEntity1211> JAPANESE_SAMURAI_ECHO = registerEntity(
            JAPANESE_ENTITY_ID,
            EntityType.Builder.of(JapaneseSamuraiEchoEntity1211::new, MobCategory.CREATURE)
                    .sized(0.75F, 1.95F)
                    .eyeHeight(1.75F)
                    .clientTrackingRange(10)
                    .build(id(JAPANESE_ENTITY_ID).toString()));
    public static final EntityType<EgyptianArcherEchoEntity1211> EGYPTIAN_ARCHER_ECHO = registerEntity(
            EGYPTIAN_ENTITY_ID,
            EntityType.Builder.of(EgyptianArcherEchoEntity1211::new, MobCategory.CREATURE)
                    .sized(0.75F, 1.95F)
                    .eyeHeight(1.74F)
                    .clientTrackingRange(10)
                    .build(id(EGYPTIAN_ENTITY_ID).toString()));
    public static final EntityType<EgyptianArcherArrowEntity1211> EGYPTIAN_ARCHER_ARROW = registerEntity(
            EGYPTIAN_ARROW_ENTITY_ID,
            EntityType.Builder.<EgyptianArcherArrowEntity1211>of(EgyptianArcherArrowEntity1211::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(1)
                    .build(id(EGYPTIAN_ARROW_ENTITY_ID).toString()));
    public static final MenuType<SummonerMenu1211> SUMMONER_MENU = new MenuType<>(
            SummonerMenu1211::new, FeatureFlags.VANILLA_SET);
    public static final MenuType<RecyclerMenu1211> RECYCLER_MENU = new MenuType<>(
            RecyclerMenu1211::new, FeatureFlags.VANILLA_SET);
    public static final MenuType<KnowledgeReaderMenu1211> KNOWLEDGE_READER_MENU = new MenuType<>(
            KnowledgeReaderMenu1211::new, FeatureFlags.VANILLA_SET);
    public static final MenuType<TutorialManualMenu1211> TUTORIAL_MANUAL_MENU = new MenuType<>(
            TutorialManualMenu1211::new, FeatureFlags.VANILLA_SET);
    public static final SimpleCraftingRecipeSerializer<CraftLegacyRepairRecipe1211> CRAFT_LEGACY_REPAIR_SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(CraftLegacyRepairRecipe1211::new);
    public static final SimpleCraftingRecipeSerializer<KnowledgeFragmentCollectionRecipe1211>
            KNOWLEDGE_FRAGMENT_COLLECTION_SERIALIZER =
            new SimpleCraftingRecipeSerializer<>(KnowledgeFragmentCollectionRecipe1211::new);

    private ModContent1211() {
    }

    public static Map<ResourceLocation, Item> items() { return Map.copyOf(ITEMS); }
    public static Map<ResourceLocation, EntityType<?>> entityTypes() { return Map.copyOf(ENTITY_TYPES); }
    public static Map<ResourceLocation, Block> blocks() { return Map.copyOf(BLOCKS); }
    public static Map<ResourceLocation, BlockEntityType<?>> blockEntityTypes() { return Map.copyOf(BLOCK_ENTITY_TYPES); }
    public static List<Item> accessories() {
        return ITEMS.values().stream().filter(EchoAccessoryItem1211.class::isInstance).toList();
    }

    private static EchoRelicItem1211 relic(String path, EchoHeroType1211 hero) {
        return registerItem(path, new EchoRelicItem1211(new Item.Properties().stacksTo(1), hero));
    }
    private static EchoAccessoryItem1211 accessory(String path, EchoAccessoryItem1211.AccessoryType type) {
        return registerItem(path, new EchoAccessoryItem1211(new Item.Properties().stacksTo(1), type));
    }
    private static LegacyItem1211 legacy(String path, LegacyItem1211.LegacyType type) {
        return registerItem(path, new LegacyItem1211(new Item.Properties().stacksTo(64), type));
    }
    private static <T extends Item> T registerItem(String path, T item) {
        ITEMS.put(id(path), item);
        return item;
    }
    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> registerEntity(String path, EntityType<T> type) {
        ENTITY_TYPES.put(id(path), type);
        return type;
    }
    private static <T extends Block> T registerBlock(String path, T block) {
        BLOCKS.put(id(path), block);
        return block;
    }
    private static <T extends BlockEntityType<?>> T registerBlockEntity(String path, T type) {
        BLOCK_ENTITY_TYPES.put(id(path), type);
        return type;
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EchoWarrior1211.MOD_ID, path);
    }
}
