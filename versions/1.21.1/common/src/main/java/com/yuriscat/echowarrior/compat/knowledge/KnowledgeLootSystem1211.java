package com.yuriscat.echowarrior.compat.knowledge;

import com.yuriscat.echowarrior.compat.ModContent1211;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.Consumer;

public final class KnowledgeLootSystem1211 {
    private static final ResourceKey<LootTable> RANDOM_KNOWLEDGE = ResourceKey.create(
            Registries.LOOT_TABLE, ModContent1211.id("gameplay/knowledge_fragment/random"));

    private KnowledgeLootSystem1211() {
    }

    public static void modify(ResourceKey<LootTable> key, Consumer<LootPool.Builder> poolConsumer) {
        if (key.equals(BuiltInLootTables.VILLAGE_CARTOGRAPHER) || key.equals(BuiltInLootTables.SIMPLE_DUNGEON)) {
            addKnowledgePool(poolConsumer, 0.25F, ConstantValue.exactly(1.0F));
        } else if (key.equals(BuiltInLootTables.ABANDONED_MINESHAFT)) {
            addKnowledgePool(poolConsumer, 0.15F, ConstantValue.exactly(1.0F));
        } else if (key.equals(BuiltInLootTables.STRONGHOLD_LIBRARY)) {
            addKnowledgePool(poolConsumer, 0.75F, UniformGenerator.between(1.0F, 2.0F));
        }
    }

    private static void addKnowledgePool(Consumer<LootPool.Builder> poolConsumer, float chance, NumberProvider rolls) {
        poolConsumer.accept(LootPool.lootPool().setRolls(rolls)
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(NestedLootTable.lootTableReference(RANDOM_KNOWLEDGE)));
    }
}
