package com.yuriscat.echowarrior.compat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.yuriscat.echowarrior.compat.EchoHeroType1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.ModCreativeTabs1211;
import com.yuriscat.echowarrior.compat.ModDamageTypes1211;
import com.yuriscat.echowarrior.compat.ModEffects1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.combat.ShieldChargeCreeperControl1211;
import com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.RomanVisualMath1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicItem1211;
import com.yuriscat.echowarrior.compat.item.EchoSummonerItem1211;
import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1211;
import com.yuriscat.echowarrior.compat.item.EchoSummonerAccessory1211;
import com.yuriscat.echowarrior.compat.item.EchoBiomeAffinity1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicProgress1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1211;
import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1211;
import com.yuriscat.echowarrior.compat.item.EchoTrait1211;
import com.yuriscat.echowarrior.compat.item.SummonerFuel1211;
import com.yuriscat.echowarrior.compat.item.SummonerStackContents1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeCatalog1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1211;
import com.yuriscat.echowarrior.compat.tutorial.TutorialManualCatalog1211;
import com.yuriscat.echowarrior.compat.tutorial.TutorialManualStackData1211;
import com.yuriscat.echowarrior.compat.mixin.ShulkerBulletAccessor1211;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.EnumSet;

public final class CompatSelfTestCommand1211 {
    private CompatSelfTestCommand1211() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_warrior_compat")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("selftest").executes(context -> run(context.getSource())))
                .then(Commands.literal("reroll_traits").executes(context -> rerollTraits(context.getSource()))));
    }

    private static int run(CommandSourceStack source) {
        UUID testSummonerId = null;
        UUID testCreeperId = null;
        try {
            require(ModContent1211.items().size() == 43, "complete item catalogue");
            ModContent1211.items().forEach((id, item) ->
                    require(id.equals(BuiltInRegistries.ITEM.getKey(item)), "item registry id " + id));
            require(ModContent1211.items().containsValue(ModContent1211.ECHO_COMPASS)
                            && ModContent1211.items().containsValue(ModContent1211.KNOWLEDGE_FRAGMENT)
                            && ModContent1211.items().containsValue(ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION)
                            && ModContent1211.items().containsValue(ModContent1211.TUTORIAL_MANUAL),
                    "compass, knowledge, and tutorial items");
            require(ModContent1211.items().containsValue(ModContent1211.ECHO_RECYCLER_ITEM)
                            && ModContent1211.items().containsValue(ModContent1211.SUSPICIOUS_GRASS_BLOCK_ITEM)
                            && ModContent1211.items().containsValue(ModContent1211.SUSPICIOUS_DIRT_ITEM),
                    "recycler and suspicious block items");
            require(ModContent1211.blocks().size() == 3, "complete block catalogue");
            ModContent1211.blocks().forEach((id, block) ->
                    require(id.equals(BuiltInRegistries.BLOCK.getKey(block)), "block registry id " + id));
            require(ModContent1211.blockEntityTypes().size() == 1
                            && ModContent1211.blockEntityTypes().containsValue(ModContent1211.RECYCLER_CHEST),
                    "recycler block entity catalogue");
            ModContent1211.blockEntityTypes().forEach((id, type) ->
                    require(id.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type)),
                            "block entity registry id " + id));
            require(BuiltInRegistries.MENU.getKey(ModContent1211.SUMMONER_MENU)
                            .equals(ModContent1211.id(ModContent1211.SUMMONER_MENU_ID))
                            && BuiltInRegistries.MENU.getKey(ModContent1211.RECYCLER_MENU)
                            .equals(ModContent1211.id(ModContent1211.RECYCLER_MENU_ID))
                            && BuiltInRegistries.MENU.getKey(ModContent1211.KNOWLEDGE_READER_MENU)
                            .equals(ModContent1211.id(ModContent1211.KNOWLEDGE_READER_MENU_ID))
                            && BuiltInRegistries.MENU.getKey(ModContent1211.TUTORIAL_MANUAL_MENU)
                            .equals(ModContent1211.id(ModContent1211.TUTORIAL_MANUAL_MENU_ID)),
                    "complete menu registry catalogue");
            require(ModCreativeTabs1211.tabs().size() == 3, "complete creative tab catalogue");
            ModCreativeTabs1211.tabs().forEach((id, tab) ->
                    require(id.equals(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab)),
                            "creative tab registry id " + id));
            require(KnowledgeCatalog1211.entries().size() == 40, "complete knowledge catalogue");
            require(KnowledgeCatalog1211.entries().stream().filter(entry -> !entry.illustrations().isEmpty()).count() == 27
                            && KnowledgeCatalog1211.entries().stream()
                            .mapToInt(entry -> entry.illustrations().size()).sum() == 27,
                    "complete high-detail knowledge illustration catalogue");
            require(TutorialManualCatalog1211.pageCount() == 44
                            && TutorialManualCatalog1211.accessoryIds().size() == 25
                            && TutorialManualCatalog1211.firstPage(TutorialManualCatalog1211.Chapter.SEARCH) == 2
                            && TutorialManualCatalog1211.firstPage(TutorialManualCatalog1211.Chapter.HEROES) == 8,
                    "complete tutorial catalogue");
            require(ModContent1211.entityTypes().size() == 6, "complete entity catalogue");
            ModContent1211.entityTypes().forEach((id, type) ->
                    require(id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)), "entity registry id " + id));
            require(ModEffects1211.effects().size() == 6, "complete effect catalogue");
            ModEffects1211.effects().forEach((id, effect) ->
                    require(id.equals(BuiltInRegistries.MOB_EFFECT.getKey(effect)), "effect registry id " + id));
            require(ModEffects1211.LEGACY_SOLDIER_FORMATION != null
                            && ModEffects1211.WEAPONS_RAISED != null
                            && ModEffects1211.SHIELDS_RAISED != null
                            && ModEffects1211.HUITZILOPOCHTLI_BLESSING != null
                            && ModEffects1211.OBSIDIAN_WOUND != null
                            && ModEffects1211.BLEEDING != null,
                    "effect holders bound");
            for (var damageType : java.util.List.of(
                    ModDamageTypes1211.ARMOR_PIERCING_ARROW,
                    ModDamageTypes1211.BLEEDING,
                    ModDamageTypes1211.OBSIDIAN_WOUND,
                    ModDamageTypes1211.ROMAN_FIRST_STRIKE,
                    ModDamageTypes1211.ROMAN_FOLLOWUP,
                    ModDamageTypes1211.SAMURAI_FIRST_SLASH,
                    ModDamageTypes1211.SAMURAI_STAB,
                    ModDamageTypes1211.SPIKED_ARMOR_REFLECTION)) {
                require(source.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                                .getHolder(damageType).isPresent(),
                        "damage type " + damageType.location());
            }
            require(ModDamageTypes1211.source(source.getLevel(), ModDamageTypes1211.ROMAN_FIRST_STRIKE)
                            .is(DamageTypeTags.NO_KNOCKBACK),
                    "Roman first strike no-knockback tag");
            require(ModDamageTypes1211.source(source.getLevel(), ModDamageTypes1211.ROMAN_FOLLOWUP)
                            .is(DamageTypeTags.BYPASSES_COOLDOWN),
                    "Roman follow-up cooldown bypass tag");
            require(BuiltInRegistries.RECIPE_SERIALIZER.getKey(ModContent1211.CRAFT_LEGACY_REPAIR_SERIALIZER)
                            .equals(ModContent1211.id(ModContent1211.CRAFT_LEGACY_REPAIR_RECIPE_ID)),
                    "craft legacy repair serializer registry id");
            require(BuiltInRegistries.RECIPE_SERIALIZER.getKey(
                            ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION_SERIALIZER)
                            .equals(ModContent1211.id(ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION_RECIPE_ID)),
                    "knowledge fragment collection serializer registry id");
            EnumSet<EchoAccessoryItem1211.AccessoryType> accessoryTypes = EnumSet.noneOf(
                    EchoAccessoryItem1211.AccessoryType.class);
            ModContent1211.items().values().forEach(item -> {
                if (item instanceof EchoAccessoryItem1211 accessory) accessoryTypes.add(accessory.type());
            });
            require(accessoryTypes.size() == EchoAccessoryItem1211.AccessoryType.values().length
                            && accessoryTypes.size() == 25,
                    "complete accessory catalogue");

            for (EchoHeroType1211 heroType : EchoHeroType1211.values()) {
                ItemStack heroRelic = new ItemStack(relicFor(heroType));
                require(EchoRelicState1211.ensureInitialized(heroRelic,
                                RandomSource.create(0xEC00L + heroType.ordinal()), source.getLevel().getGameTime()),
                        heroType.id() + " relic initialization");
                require(EchoHeroType1211.fromRelic(heroRelic) == heroType,
                        heroType.id() + " relic identity");
                require(EchoRelicState1211.enabledSkills(heroRelic) == heroType.defaultEnabledSkillsMask(),
                        heroType.id() + " default skill mask");
                validateTraitRoll(heroRelic);

                ItemStack leveledRelic = new ItemStack(relicFor(heroType));
                EchoRelicState1211.setTraitsForSelfTest(leveledRelic, 0, EchoBiomeAffinity1211.OPENLAND);
                EchoRelicProgress1211.ProgressResult leveled = EchoRelicProgress1211.addExperience(leveledRelic, 1305);
                require(leveled.newLevel() == EchoRelicProgress1211.MAX_LEVEL
                                && close(EchoRelicState1211.maximumHealth(leveledRelic), heroType.maximumHealth() * 2.0)
                                && close(EchoRelicState1211.attackDamage(leveledRelic), heroType.attackDamage() * 2.0),
                        heroType.id() + " level 30 growth");

                EchoWarriorEntity1211 hero = createHero(source, heroType);
                require(hero != null, heroType.id() + " entity factory");
                require(close(hero.livingEntity().getAttributeValue(Attributes.MAX_HEALTH), heroType.maximumHealth())
                                && close(hero.livingEntity().getAttributeValue(Attributes.ATTACK_DAMAGE), heroType.attackDamage())
                                && close(hero.livingEntity().getAttributeValue(Attributes.ARMOR), heroType.armor())
                                && close(hero.livingEntity().getAttributeValue(Attributes.MOVEMENT_SPEED), heroType.movementSpeed())
                                && close(hero.livingEntity().getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),
                                heroType.knockbackResistance()),
                        heroType.id() + " base attributes");
                hero.applyRelicState(leveledRelic, false);
                require(close(hero.livingEntity().getAttributeValue(Attributes.MAX_HEALTH), heroType.maximumHealth() * 2.0)
                                && close(hero.livingEntity().getAttributeValue(Attributes.ATTACK_DAMAGE),
                                heroType.attackDamage() * 2.0),
                        heroType.id() + " relic attribute application");
                hero.livingEntity().discard();
            }

            require(BuiltInRegistries.ITEM.getKey(ModContent1211.ECHO_SUMMONER)
                    .equals(ModContent1211.id(ModContent1211.SUMMONER_ID)), "summoner registry id");
            require(BuiltInRegistries.ITEM.getKey(ModContent1211.ROMAN_LEGIONARY_RELIC)
                    .equals(ModContent1211.id(ModContent1211.ROMAN_RELIC_ID)), "relic registry id");
            require(BuiltInRegistries.ITEM.getKey(ModContent1211.PLATE_ARMOR_ACCESSORY)
                    .equals(ModContent1211.id(ModContent1211.PLATE_ARMOR_ACCESSORY_ID)), "plate accessory registry id");
            require(BuiltInRegistries.ITEM.getKey(ModContent1211.HAWKEYE_LENS_ACCESSORY)
                    .equals(ModContent1211.id(ModContent1211.HAWKEYE_LENS_ACCESSORY_ID)), "hawkeye accessory registry id");
            require(BuiltInRegistries.ITEM.getKey(ModContent1211.FEAST_HAM_ACCESSORY)
                    .equals(ModContent1211.id(ModContent1211.FEAST_HAM_ACCESSORY_ID)), "feast accessory registry id");
            require(BuiltInRegistries.ITEM.getKey(ModContent1211.LIGHT_GATHERING_MAGNET_ACCESSORY)
                    .equals(ModContent1211.id(ModContent1211.LIGHT_GATHERING_MAGNET_ACCESSORY_ID)),
                    "magnet accessory registry id");
            require(BuiltInRegistries.ITEM.getKey(ModContent1211.VICTORS_LAUREL_ACCESSORY)
                    .equals(ModContent1211.id(ModContent1211.VICTORS_LAUREL_ACCESSORY_ID)),
                    "laurel accessory registry id");
            require(BuiltInRegistries.ENTITY_TYPE.getKey(ModContent1211.ROMAN_LEGIONARY_ECHO)
                    .equals(ModContent1211.id(ModContent1211.ROMAN_ENTITY_ID)), "entity registry id");

            ItemStack summoner = new ItemStack(ModContent1211.ECHO_SUMMONER);
            EchoSummonerItem1211.setRelicStack(summoner, new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC));
            EchoSummonerItem1211.setAccessoryStacks(summoner, java.util.List.of(
                    new ItemStack(ModContent1211.PLATE_ARMOR_ACCESSORY),
                    new ItemStack(ModContent1211.HAWKEYE_LENS_ACCESSORY),
                    new ItemStack(ModContent1211.FEAST_HAM_ACCESSORY),
                    new ItemStack(ModContent1211.LIGHT_GATHERING_MAGNET_ACCESSORY),
                    new ItemStack(ModContent1211.VICTORS_LAUREL_ACCESSORY),
                    ItemStack.EMPTY));
            SummonerFuel1211.setAmount(summoner, 150);
            require(EchoSummonerItem1211.relicStack(summoner).is(ModContent1211.ROMAN_LEGIONARY_RELIC),
                    "relic component round-trip");
            ItemStack directSummoner = new ItemStack(ModContent1211.ECHO_SUMMONER);
            ItemStack directRelic = new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC);
            ItemStack directAccessory = new ItemStack(ModContent1211.PLATE_ARMOR_ACCESSORY);
            require(EchoSummonerItem1211.insertIntoInternalSlotForSelfTest(directSummoner, directRelic)
                            && directRelic.isEmpty()
                            && EchoSummonerItem1211.relicStack(directSummoner)
                            .is(ModContent1211.ROMAN_LEGIONARY_RELIC),
                    "direct relic insertion without item loss");
            require(EchoSummonerItem1211.insertIntoInternalSlotForSelfTest(directSummoner, directAccessory)
                            && directAccessory.isEmpty()
                            && EchoSummonerItem1211.accessoryStacks(directSummoner).get(0)
                            .is(ModContent1211.PLATE_ARMOR_ACCESSORY),
                    "direct accessory insertion without item loss");
            EchoSummonerItem1211.getOrCreateSummonerId(directSummoner);
            ItemStack synchronizedDirectSummoner = directSummoner.copy();
            SummonerFuel1211.setAmount(synchronizedDirectSummoner, 37);
            ItemStack unrelatedSummoner = directSummoner.copy();
            EchoSummonerItem1211.replaceSummonerIdForDuplicate(unrelatedSummoner);
            require(EchoSummonerItem1211.isSamePhysicalSummoner(directSummoner, synchronizedDirectSummoner)
                            && !EchoSummonerItem1211.isSamePhysicalSummoner(directSummoner, unrelatedSummoner),
                    "same summoner identity survives component synchronization");
            ItemStack nestedSummonerContainer = new ItemStack(Items.SHULKER_BOX);
            nestedSummonerContainer.set(DataComponents.CONTAINER,
                    ItemContainerContents.fromItems(java.util.List.of(directSummoner.copy())));
            require(SummonerStackContents1211.summonerIds(nestedSummonerContainer)
                            .contains(EchoSummonerItem1211.getSummonerId(directSummoner)),
                    "nested summoner discovery for confirmed creative destruction");
            ItemStack manualBeforePageTurn = new ItemStack(ModContent1211.TUTORIAL_MANUAL);
            ItemStack manualAfterPageTurn = manualBeforePageTurn.copy();
            TutorialManualStackData1211.setBookmark(manualAfterPageTurn, 1);
            require(TutorialManualStackData1211.isSamePhysicalManual(
                            manualBeforePageTurn, manualAfterPageTurn)
                            && !TutorialManualStackData1211.isSamePhysicalManual(
                            manualBeforePageTurn, new ItemStack(Items.BOOK)),
                    "tutorial page bookmarks do not create a false held-item replacement");
            String firstKnowledgePage = KnowledgeCatalog1211.entries().get(0).id();
            String secondKnowledgePage = KnowledgeCatalog1211.entries().get(1).id();
            ItemStack collectionBeforePageTurn = KnowledgeStackData1211.collection(
                    java.util.Map.of(firstKnowledgePage, 1, secondKnowledgePage, 1), firstKnowledgePage);
            ItemStack collectionAfterPageTurn = collectionBeforePageTurn.copy();
            KnowledgeStackData1211.setBookmark(collectionAfterPageTurn, secondKnowledgePage);
            require(KnowledgeStackData1211.isSamePhysicalCollection(
                            collectionBeforePageTurn, collectionAfterPageTurn)
                            && !KnowledgeStackData1211.isSamePhysicalCollection(
                            collectionBeforePageTurn, new ItemStack(Items.BOOK)),
                    "knowledge collection page bookmarks do not create a false held-item replacement");
            ItemStack destroyedSummoner = new ItemStack(ModContent1211.ECHO_SUMMONER);
            UUID destroyedSummonerId = EchoSummonerItem1211.getOrCreateSummonerId(destroyedSummoner);
            EchoBindingSystem1211.synchronize(source.getLevel(), destroyedSummoner);
            require(EchoBindingSystem1211.destroySummoner(source.getLevel(), destroyedSummonerId)
                            && EchoBindingSavedData1211.get(source.getServer()).get(destroyedSummonerId) == null,
                    "destroyed summoner binding removal");
            ItemStack menuCommitSummoner = new ItemStack(ModContent1211.ECHO_SUMMONER);
            EchoSummonerItem1211.setRelicStack(menuCommitSummoner,
                    new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC));
            EchoSummonerItem1211.setAccessoryStacks(menuCommitSummoner,
                    java.util.List.of(new ItemStack(ModContent1211.PLATE_ARMOR_ACCESSORY)));
            EchoBindingSavedData1211.Binding menuCommitBinding =
                    EchoBindingSystem1211.synchronize(source.getLevel(), menuCommitSummoner);
            EchoSummonerItem1211.setAccessoryStacks(menuCommitSummoner, java.util.List.of());
            EchoBindingSystem1211.commitMenuContents(source.getLevel(), menuCommitSummoner);
            require(menuCommitBinding.relic().is(ModContent1211.ROMAN_LEGIONARY_RELIC)
                            && menuCommitBinding.accessories().stream().allMatch(ItemStack::isEmpty)
                            && EchoSummonerItem1211.accessoryStacks(menuCommitSummoner).stream()
                            .allMatch(ItemStack::isEmpty),
                    "menu accessory removal commits without authoritative rollback duplication");
            testSummonerId = EchoSummonerItem1211.getOrCreateSummonerId(summoner);
            ItemStack duplicatedSummoner = summoner.copy();
            UUID replacementSummonerId = EchoSummonerItem1211.replaceSummonerIdForDuplicate(duplicatedSummoner);
            require(!replacementSummonerId.equals(testSummonerId)
                            && replacementSummonerId.equals(EchoSummonerItem1211.getOrCreateSummonerId(duplicatedSummoner))
                            && testSummonerId.equals(EchoSummonerItem1211.getOrCreateSummonerId(summoner)),
                    "duplicated summoner identity replacement");
            EchoBindingSavedData1211.Binding binding = EchoBindingSystem1211.synchronize(source.getLevel(), summoner);
            require(testSummonerId.equals(binding.summonerId()) && binding.fuel() == 150,
                    "binding fuel initialization");
            require(binding.relicInitialized() && EchoRelicState1211.initialized(binding.relic()),
                    "authoritative relic initialization");
            require(binding.accessoriesInitialized()
                            && binding.accessories().get(0).is(ModContent1211.PLATE_ARMOR_ACCESSORY)
                            && binding.accessories().get(4).is(ModContent1211.VICTORS_LAUREL_ACCESSORY),
                    "authoritative accessory initialization");
            SimpleContainer accessoryContents = new SimpleContainer(6);
            accessoryContents.setItem(0, new ItemStack(ModContent1211.PLATE_ARMOR_ACCESSORY));
            require(!EchoSummonerAccessory1211.canInstall(new ItemStack(ModContent1211.PLATE_ARMOR_ACCESSORY),
                            summoner, 1, accessoryContents)
                            && EchoSummonerAccessory1211.canInstall(new ItemStack(ModContent1211.FEAST_HAM_ACCESSORY),
                            summoner, 1, accessoryContents),
                    "accessory duplicate rejection");
            require(close(EchoAccessorySystem1211.armorBonus(binding.accessories()), 2.0)
                            && close(EchoAccessorySystem1211.maximumHealthBonus(binding.accessories()), 12.0)
                            && close(EchoAccessorySystem1211.movementMultiplier(binding.accessories()), 0.85)
                            && close(EchoAccessorySystem1211.proactiveRange(binding.accessories(), 16.0, false), 24.0)
                            && close(EchoAccessorySystem1211.proactiveRange(binding.accessories(), 16.0, true), 8.0)
                            && EchoAccessorySystem1211.worldExperienceReward(5, true) == 8
                            && EchoAccessorySystem1211.growthExperienceReward(5, true) == 8
                            && close(EchoAccessorySystem1211.victorHealAmount(72.0F), 7.2),
                    "Roman accessory fixed bonuses");
            validateTraitRoll(binding.relic());
            require(EchoBindingSystem1211.consumeFuel(source.getLevel(), summoner, 100)
                            && binding.fuel() == 50 && SummonerFuel1211.amount(summoner) == 50,
                    "authoritative fuel consumption");
            EchoBindingSystem1211.addFuel(source.getLevel(), summoner, 75);
            require(binding.fuel() == 125 && SummonerFuel1211.amount(summoner) == 125,
                    "authoritative fuel refill");

            ItemStack lazyRelic = new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC);
            EchoRelicState1211.setTraitsForSelfTest(lazyRelic, EchoTrait1211.LAZY.mask(),
                    EchoBiomeAffinity1211.OPENLAND);
            require(EchoRelicState1211.summonCost(lazyRelic) == 85
                            && close(EchoRelicState1211.naturalHealingCost(lazyRelic, 2), 1.7),
                    "lazy fuel costs");
            binding.setFuel(3);
            require(EchoBindingSystem1211.consumeFractionalFuel(source.getLevel(), testSummonerId, 1.7)
                            && binding.fuel() == 2
                            && EchoBindingSystem1211.consumeFractionalFuel(source.getLevel(), testSummonerId, 1.7)
                            && binding.fuel() == 0,
                    "fractional fuel accumulation");
            binding.setFuel(125);

            for (int seed = 0; seed < 512; seed++) {
                ItemStack rolled = new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC);
                require(EchoRelicState1211.ensureInitialized(rolled, RandomSource.create(seed)),
                        "trait initialization");
                validateTraitRoll(rolled);
            }

            ItemStack growthRelic = new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC);
            int growthMask = EchoTrait1211.COURAGE.mask() | EchoTrait1211.SKINNY.mask();
            EchoRelicState1211.setTraitsForSelfTest(growthRelic, growthMask, EchoBiomeAffinity1211.OPENLAND);
            EchoRelicProgress1211.ProgressResult growth = EchoRelicProgress1211.addExperience(growthRelic, 1305);
            require(growth.newLevel() == 30 && growth.levelsGained() == 29
                            && EchoRelicProgress1211.experience(growthRelic) == 0,
                    "level 30 experience curve");
            require(close(EchoRelicState1211.maximumHealth(growthRelic), 60.0)
                            && close(EchoRelicState1211.attackDamage(growthRelic), 13.0)
                            && close(EchoRelicState1211.movementSpeed(growthRelic), 0.308)
                            && EchoRelicState1211.attackSpeedPercent(growthRelic) == 110,
                    "level and permanent talent attributes");
            EchoBindingSystem1211.persistRelic(source.getLevel(), testSummonerId, growthRelic);
            EchoBindingSystem1211.synchronize(source.getLevel(), summoner);
            require(EchoRelicProgress1211.level(EchoSummonerItem1211.relicStack(summoner)) == 30,
                    "authoritative relic mirror");

            long stateRevisionBeforeControls = binding.stateRevision();
            EchoBindingSystem1211.setActivityMode(source.getLevel(), binding, 2);
            EchoBindingSystem1211.setAlertMode(source.getLevel(), binding, 0);
            ItemStack staleMenuRelic = binding.relic();
            EchoBindingSystem1211.toggleSkill(source.getLevel(), binding, 1);
            require(binding.activityMode() == 2 && binding.alertMode() == 0 && !binding.skillEnabled(1)
                            && binding.skillEnabled(0) && binding.skillEnabled(2) && binding.skillEnabled(3),
                    "mode and skill state");
            require(binding.stateRevision() > stateRevisionBeforeControls,
                    "authoritative summoner state revision advances on control changes");
            ItemStack reconciledMenuRelic = EchoBindingSystem1211.reconcileMenuRelicForSave(
                    staleMenuRelic, binding.relic());
            require(!EchoRelicState1211.skillEnabled(reconciledMenuRelic, 1),
                    "stale summoner menu cannot restore a disabled skill");
            long now = source.getLevel().getGameTime();
            require(binding.shieldCharges(now) == 3 && binding.consumeShieldCharge(now)
                            && binding.shieldCharges(now) == 2 && binding.shieldChargeProgress(now) == 0,
                    "shield charge state");
            binding.setLegionCooldownEnd(now + 400L);
            require(binding.legionCooldownEnd() == now + 400L, "legion cooldown state");

            CompoundTag migrationState = new CompoundTag();
            migrationState.putLong("SelfTestTimestamp", 0xEC401211L);
            migrationState.putInt("SelfTestAction", 3);
            binding.track(source.getLevel().dimension().location().toString(),
                    12.25, 72.0, -8.5, 42.5F, 3.75F, 87, 19, 241, migrationState);

            EchoBindingSavedData1211.Binding roundTripped = EchoBindingSavedData1211.roundTripForSelfTest(
                    binding, source.getLevel().registryAccess());
            require(roundTripped.summonerId().equals(binding.summonerId())
                            && roundTripped.fuel() == 125 && close(roundTripped.fuelFraction(), 0.4)
                            && roundTripped.activityMode() == 2 && roundTripped.alertMode() == 0
                            && !roundTripped.skillEnabled(1)
                            && roundTripped.legionCooldownEnd() == now + 400L
                            && roundTripped.stateRevision() == binding.stateRevision(),
                    "binding state save round-trip");
            require(close(roundTripped.x(), 12.25) && close(roundTripped.y(), 72.0)
                            && close(roundTripped.z(), -8.5) && close(roundTripped.health(), 42.5)
                            && close(roundTripped.absorption(), 3.75)
                            && roundTripped.remainingFireTicks() == 87
                            && roundTripped.ticksFrozen() == 19
                            && roundTripped.airSupply() == 241
                            && roundTripped.migrationState().getLong("SelfTestTimestamp") == 0xEC401211L
                            && roundTripped.migrationState().getInt("SelfTestAction") == 3,
                    "cross-dimension entity snapshot save round-trip");
            require(roundTripped.relicInitialized()
                            && EchoRelicProgress1211.level(roundTripped.relic()) == 30
                            && EchoRelicState1211.hasTrait(roundTripped.relic(), EchoTrait1211.COURAGE)
                            && EchoRelicState1211.hasTrait(roundTripped.relic(), EchoTrait1211.SKINNY),
                    "authoritative relic save round-trip");
            require(roundTripped.accessoriesInitialized()
                            && roundTripped.accessories().get(0).is(ModContent1211.PLATE_ARMOR_ACCESSORY)
                            && roundTripped.accessories().get(1).is(ModContent1211.HAWKEYE_LENS_ACCESSORY)
                            && roundTripped.accessories().get(2).is(ModContent1211.FEAST_HAM_ACCESSORY)
                            && roundTripped.accessories().get(3).is(ModContent1211.LIGHT_GATHERING_MAGNET_ACCESSORY)
                            && roundTripped.accessories().get(4).is(ModContent1211.VICTORS_LAUREL_ACCESSORY),
                    "authoritative accessories save round-trip");

            RomanLegionaryEchoEntity1211 original = ModContent1211.ROMAN_LEGIONARY_ECHO.create(source.getLevel());
            RomanLegionaryEchoEntity1211 restored = ModContent1211.ROMAN_LEGIONARY_ECHO.create(source.getLevel());
            require(original != null && restored != null, "entity factory");
            UUID ownerId = UUID.randomUUID();
            UUID summonerId = testSummonerId;
            original.setOwnerUUID(ownerId);
            original.setTame(true, false);
            original.setSummonerIdForTest(summonerId);
            long firstGeneration = binding.activate(ownerId, original.getUUID(),
                    source.getLevel().dimension().location().toString(), 0.0, 64.0, 0.0, original.getHealth());
            original.setBindingGenerationForTest(firstGeneration);
            require(EchoBindingSystem1211.validateAndTrack(original), "current generation validation");
            long secondGeneration = binding.activate(ownerId, restored.getUUID(),
                    source.getLevel().dimension().location().toString(), 0.0, 64.0, 0.0, restored.getHealth());
            require(secondGeneration > firstGeneration && !EchoBindingSystem1211.validateAndTrack(original),
                    "stale generation rejection");
            restored.setSummonerIdForTest(summonerId);
            restored.setBindingGenerationForTest(secondGeneration);
            CompoundTag saved = original.saveWithoutId(new CompoundTag());
            RomanLegionaryEchoEntity1211 saveRestored = ModContent1211.ROMAN_LEGIONARY_ECHO.create(source.getLevel());
            require(saveRestored != null, "save restored entity factory");
            saveRestored.load(saved);
            require(ownerId.equals(saveRestored.getOwnerUUID()), "owner save round-trip");
            require(summonerId.equals(saveRestored.getSummonerId()), "summoner save round-trip");
            require(saveRestored.getBindingGeneration() == firstGeneration, "generation save round-trip");

            EchoHeroType1211 hero = EchoHeroType1211.ROMAN_LEGIONARY;
            require(close(original.getAttributeValue(Attributes.MAX_HEALTH), hero.maximumHealth()), "maximum health");
            require(close(original.getAttributeValue(Attributes.ATTACK_DAMAGE), hero.attackDamage()), "attack damage");
            require(close(original.getAttributeValue(Attributes.ARMOR), hero.armor()), "armor");
            require(close(original.getAttributeValue(Attributes.MOVEMENT_SPEED), hero.movementSpeed()), "movement speed");
            require(close(original.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), hero.knockbackResistance()),
                    "knockback resistance");

            original.applyRelicState(growthRelic, false);
            EchoAccessorySystem1211.apply(original, binding.accessories());
            require(close(original.getAttributeValue(Attributes.MAX_HEALTH), 72.0)
                            && close(original.getAttributeValue(Attributes.ATTACK_DAMAGE), 13.0)
                            && close(original.getAttributeValue(Attributes.ARMOR), 10.0)
                            && close(original.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.2618),
                    "entity relic and accessory attribute application");

            ItemStack combatRelic = new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC);
            EchoRelicState1211.setTraitsForSelfTest(combatRelic,
                    EchoTrait1211.UNDEAD_SLAYER.mask() | EchoTrait1211.UNYIELDING.mask(),
                    EchoBiomeAffinity1211.OPENLAND);
            EchoBindingSystem1211.persistRelic(source.getLevel(), testSummonerId, combatRelic);
            original.applyRelicState(combatRelic, false);
            original.setHealth(original.getMaxHealth() * 0.5F);
            Zombie talentTarget = EntityType.ZOMBIE.create(source.getLevel());
            require(talentTarget != null, "talent target factory");
            require(close(EchoTalentSystem1211.modifyOutgoingDamage(
                            original, talentTarget, source.getLevel(), 10.0F), 12.0),
                    "undead slayer outgoing damage");
            require(close(EchoTalentSystem1211.modifyFinalIncomingDamage(original,
                            source.getLevel().damageSources().mobAttack(talentTarget), 10.0F), 8.5),
                    "unyielding final damage reduction");

            require(RomanLegionaryEchoEntity1211.isAttackTrackingHazardForSelfTest(
                            Blocks.MAGMA_BLOCK.defaultBlockState())
                            && RomanLegionaryEchoEntity1211.isAttackTrackingHazardForSelfTest(
                            Blocks.POWDER_SNOW.defaultBlockState())
                            && !RomanLegionaryEchoEntity1211.isAttackTrackingHazardForSelfTest(
                            Blocks.STONE.defaultBlockState()),
                    "attack tracking hazard filter");
            require(close(RomanLegionaryEchoEntity1211.movingSegmentsDistanceToSqrForSelfTest(
                    new Vec3(-1.0, 0.0, 0.0), new Vec3(1.0, 0.0, 0.0),
                    new Vec3(0.0, 0.0, -1.0), new Vec3(0.0, 0.0, 1.0)), 0.0),
                    "moving projectile interception geometry");
            require(close(RomanVisualMath1211.calculateBlink(1.5F, 0L, (byte)1), 1.0)
                            && close(RomanVisualMath1211.calculateBlink(5.5F, 0L, (byte)2), 1.0)
                            && close(RomanVisualMath1211.calculateHurtBlink(2.0F, 0L), 1.0)
                            && close(RomanVisualMath1211.calculateHurtBlink(6.0F, 0L), 0.0),
                    "Roman blink timing");
            require(close(RomanVisualMath1211.worldYawToward(new Vec3(0.0, 0.0, 1.0)), 0.0)
                            && close(RomanVisualMath1211.worldYawToward(new Vec3(1.0, 0.0, 0.0)), -90.0)
                            && close(RomanVisualMath1211.parentCompensation(0.0F, false), 1.0)
                            && close(RomanVisualMath1211.parentCompensation(10.0F * Mth.DEG_TO_RAD, false), 0.0),
                    "Roman procedural gaze math");
            require(RomanVisualMath1211.requiredGazeTicks(12.0) == 10
                            && RomanVisualMath1211.requiredGazeTicks(20.0) == 14
                            && RomanVisualMath1211.gazeHitsHead(Vec3.ZERO, new Vec3(0.0, 0.0, 1.0),
                            new Vec3(0.0, 0.0, 8.0), 0.35)
                            && !RomanVisualMath1211.gazeHitsHead(Vec3.ZERO, new Vec3(0.0, 0.0, 1.0),
                            new Vec3(1.0, 0.0, 8.0), 0.35),
                    "Roman mutual gaze acquisition math");

            Zombie projectileShooter = EntityType.ZOMBIE.create(source.getLevel());
            require(projectileShooter != null, "projectile shooter factory");
            original.setPos(0.0, 64.0, 0.0);
            projectileShooter.setPos(8.0, 64.0, 0.0);
            ShulkerBullet shulkerBullet = new ShulkerBullet(
                    source.getLevel(), projectileShooter, original, Direction.Axis.X);
            shulkerBullet.setPos(2.0, 65.0, 0.0);
            shulkerBullet.setDeltaMovement(0.4, 0.0, 0.0);
            require(original.redirectProjectileForSelfTest(source.getLevel(), original, shulkerBullet),
                    "shulker bullet redirect");
            require(shulkerBullet.getOwner() == original
                            && ((ShulkerBulletAccessor1211)(Object)shulkerBullet)
                            .echoWarrior1211$getFinalTarget() == projectileShooter
                            && shulkerBullet.getDeltaMovement().x > 0.0,
                    "shulker bullet rehoming");

            Creeper creeper = EntityType.CREEPER.create(source.getLevel());
            require(creeper != null, "creeper factory");
            testCreeperId = creeper.getUUID();
            creeper.setTarget(original);
            creeper.setSwellDir(1);
            long disorientNow = source.getLevel().getGameTime();
            ShieldChargeCreeperControl1211.disorient(source.getLevel(), creeper, 40L);
            require(ShieldChargeCreeperControl1211.isDisoriented(creeper, disorientNow)
                            && creeper.getTarget() == null && creeper.getSwellDir() == -1,
                    "creeper shield-charge disorientation");
            creeper.setTarget(original);
            creeper.setSwellDir(1);
            ShieldChargeCreeperControl1211.enforceForSelfTest(creeper);
            require(creeper.getTarget() == null && creeper.getSwellDir() == -1,
                    "creeper persistent target suppression");

            source.sendSuccess(() -> Component.literal("ECHO_WARRIOR_1_21_1_SELFTEST PASS"), false);
            return 1;
        } catch (RuntimeException error) {
            source.sendFailure(Component.literal("ECHO_WARRIOR_1_21_1_SELFTEST FAIL: " + error.getMessage()));
            return 0;
        } finally {
            if (testSummonerId != null) EchoBindingSavedData1211.get(source.getServer()).remove(testSummonerId);
            if (testCreeperId != null) ShieldChargeCreeperControl1211.clearForSelfTest(testCreeperId);
        }
    }

    private static int rerollTraits(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ItemStack held = player.getMainHandItem();
        ItemStack relic = held.is(ModContent1211.ECHO_SUMMONER)
                ? EchoSummonerItem1211.relicStack(held).copy() : held;
        if (!(relic.getItem() instanceof EchoRelicItem1211)) {
            source.sendFailure(Component.literal("主手需要拿着任意英雄遗物，或装有英雄遗物的召唤器。"));
            return 0;
        }
        int mask = EchoRelicState1211.rerollTraits(relic, player.getRandom());
        if (held.is(ModContent1211.ECHO_SUMMONER)) {
            EchoSummonerItem1211.setRelicStack(held, relic);
            EchoBindingSystem1211.updateRelic(source.getLevel(), held);
        }
        source.sendSuccess(() -> Component.literal("已重新随机 1.21.1 遗物天赋，掩码=" + mask), false);
        return 1;
    }

    private static EchoRelicItem1211 relicFor(EchoHeroType1211 heroType) {
        return switch (heroType) {
            case ROMAN_LEGIONARY -> ModContent1211.ROMAN_LEGIONARY_RELIC;
            case AZTEC_WARRIOR -> ModContent1211.AZTEC_WARRIOR_RELIC;
            case EGYPTIAN_ARCHER -> ModContent1211.EGYPTIAN_ARCHER_RELIC;
            case GUANDAO_WARRIOR -> ModContent1211.GUANDAO_WARRIOR_RELIC;
            case JAPANESE_SAMURAI -> ModContent1211.JAPANESE_SAMURAI_RELIC;
        };
    }

    private static EchoWarriorEntity1211 createHero(CommandSourceStack source, EchoHeroType1211 heroType) {
        return switch (heroType) {
            case ROMAN_LEGIONARY -> ModContent1211.ROMAN_LEGIONARY_ECHO.create(source.getLevel());
            case AZTEC_WARRIOR -> ModContent1211.AZTEC_WARRIOR_ECHO.create(source.getLevel());
            case EGYPTIAN_ARCHER -> ModContent1211.EGYPTIAN_ARCHER_ECHO.create(source.getLevel());
            case GUANDAO_WARRIOR -> ModContent1211.GUANDAO_WARRIOR_ECHO.create(source.getLevel());
            case JAPANESE_SAMURAI -> ModContent1211.JAPANESE_SAMURAI_ECHO.create(source.getLevel());
        };
    }

    private static void validateTraitRoll(ItemStack relic) {
        int mask = EchoRelicState1211.traitMask(relic);
        int count = Integer.bitCount(mask);
        require(count >= 2 && count <= 4, "trait count");
        for (EchoTrait1211 first : EchoTrait1211.values()) {
            if ((mask & first.mask()) == 0) continue;
            for (EchoTrait1211 second : EchoTrait1211.values()) {
                if (first != second && (mask & second.mask()) != 0) {
                    require(!first.conflictsWith(second), "trait conflict");
                }
            }
        }
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 0.0001;
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new IllegalStateException(label);
    }
}
