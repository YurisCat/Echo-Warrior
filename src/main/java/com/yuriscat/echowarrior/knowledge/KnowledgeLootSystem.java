package com.yuriscat.echowarrior.knowledge;

import com.yuriscat.echowarrior.EchoWarrior;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
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

public final class KnowledgeLootSystem {
	private static final ResourceKey<LootTable> RANDOM_KNOWLEDGE = ResourceKey.create(
		Registries.LOOT_TABLE,
		EchoWarrior.id("gameplay/knowledge_fragment/random")
	);

	private KnowledgeLootSystem() {
	}

	public static void initialize() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (key.equals(BuiltInLootTables.VILLAGE_CARTOGRAPHER) || key.equals(BuiltInLootTables.SIMPLE_DUNGEON)) {
				addKnowledgePool(tableBuilder, 0.25F, ConstantValue.exactly(1.0F));
			} else if (key.equals(BuiltInLootTables.ABANDONED_MINESHAFT)) {
				addKnowledgePool(tableBuilder, 0.15F, ConstantValue.exactly(1.0F));
			} else if (key.equals(BuiltInLootTables.STRONGHOLD_LIBRARY)) {
				addKnowledgePool(tableBuilder, 0.75F, UniformGenerator.between(1.0F, 2.0F));
			}
		});
	}

	private static void addKnowledgePool(LootTable.Builder tableBuilder, float chance, NumberProvider rolls) {
		tableBuilder.withPool(LootPool.lootPool()
			.setRolls(rolls)
			.when(LootItemRandomChanceCondition.randomChance(chance))
			.add(NestedLootTable.lootTableReference(RANDOM_KNOWLEDGE)));
	}
}
