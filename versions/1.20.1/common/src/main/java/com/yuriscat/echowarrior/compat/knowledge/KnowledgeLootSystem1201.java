package com.yuriscat.echowarrior.compat.knowledge;

import com.yuriscat.echowarrior.compat.ModContent1201;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.Consumer;

public final class KnowledgeLootSystem1201 {
    private static final ResourceLocation RANDOM_KNOWLEDGE = ModContent1201.id("gameplay/knowledge_fragment/random");

    private KnowledgeLootSystem1201() {
    }

    public static void modify(ResourceLocation key, Consumer<LootPool.Builder> poolConsumer) {
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
                .add(LootTableReference.lootTableReference(RANDOM_KNOWLEDGE)));
    }
}
