package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.recipe.CraftLegacyRepairRecipe;
import com.yuriscat.echowarrior.recipe.KnowledgeFragmentCollectionRecipe;
import com.yuriscat.echowarrior.platform.RegistryRegistrar;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipes {
	private ModRecipes() {
	}

	public static void register(RegistryRegistrar<RecipeSerializer<?>> registrar) {
		registrar.register(EchoWarrior.id("craft_legacy_repair"), CraftLegacyRepairRecipe.SERIALIZER);
		registrar.register(EchoWarrior.id("knowledge_fragment_collection"), KnowledgeFragmentCollectionRecipe.SERIALIZER);
	}
}
