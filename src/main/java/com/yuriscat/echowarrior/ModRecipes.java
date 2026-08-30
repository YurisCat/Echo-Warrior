package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.recipe.CraftLegacyRepairRecipe;
import com.yuriscat.echowarrior.recipe.KnowledgeFragmentCollectionRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModRecipes {
	private ModRecipes() {
	}

	public static void initialize() {
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
				EchoWarrior.id("craft_legacy_repair"), CraftLegacyRepairRecipe.SERIALIZER);
		Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
				EchoWarrior.id("knowledge_fragment_collection"), KnowledgeFragmentCollectionRecipe.SERIALIZER);
	}
}
