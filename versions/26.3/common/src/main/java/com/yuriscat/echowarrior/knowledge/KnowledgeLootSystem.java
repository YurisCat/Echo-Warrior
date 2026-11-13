package com.yuriscat.echowarrior.knowledge;

import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

import java.util.function.Consumer;

public final class KnowledgeLootSystem {
	private static final ResourceKey<LootTable> RANDOM_KNOWLEDGE = ResourceKey.create(
		Registries.LOOT_TABLE,
		EchoWarrior.id("gameplay/knowledge_fragment/random")
	);

	private KnowledgeLootSystem() {
	}

	public static void initialize() {
	}

	public static void modify(ResourceKey<LootTable> key, Consumer<LootPool.Builder> poolConsumer,
			HolderGetter.Provider registries) {
		if (key.equals(BuiltInLootTables.VILLAGE_CARTOGRAPHER) || key.equals(BuiltInLootTables.SIMPLE_DUNGEON)) {
			addKnowledgePool(poolConsumer, 0.25F, ContextIntProviders.exactly(1), registries);
		} else if (key.equals(BuiltInLootTables.ABANDONED_MINESHAFT)) {
			addKnowledgePool(poolConsumer, 0.15F, ContextIntProviders.exactly(1), registries);
		} else if (key.equals(BuiltInLootTables.STRONGHOLD_LIBRARY)) {
			addKnowledgePool(poolConsumer, 0.75F, ContextIntProviders.between(1, 2), registries);
		}
	}

	private static void addKnowledgePool(Consumer<LootPool.Builder> poolConsumer, float chance,
			Holder<ContextIntProvider> rolls, HolderGetter.Provider registries) {
		poolConsumer.accept(LootPool.lootPool()
			.setRolls(rolls)
			.when(Holder.direct(LootItemRandomChanceCondition.randomChance(chance).build()))
			.add(NestedLootTable.lootTableReference(registries.lookupOrThrow(Registries.LOOT_TABLE)
					.getOrThrow(RANDOM_KNOWLEDGE))));
	}
}
