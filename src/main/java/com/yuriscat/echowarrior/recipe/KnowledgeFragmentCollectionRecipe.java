package com.yuriscat.echowarrior.recipe;

import com.mojang.serialization.MapCodec;
import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.knowledge.KnowledgeStackData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;

public final class KnowledgeFragmentCollectionRecipe extends CustomRecipe {
	public static final KnowledgeFragmentCollectionRecipe INSTANCE = new KnowledgeFragmentCollectionRecipe();
	public static final RecipeSerializer<KnowledgeFragmentCollectionRecipe> SERIALIZER = new RecipeSerializer<>(
			MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

	private static Result collect(CraftingInput input) {
		LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
		String bookmark = "";
		int occupiedSlots = 0;
		for (int y = 0; y < input.height(); y++) {
			for (int x = 0; x < input.width(); x++) {
				ItemStack stack = input.getItem(x, y);
				if (stack.isEmpty()) continue;
				occupiedSlots++;
				if (stack.is(ModItems.KNOWLEDGE_FRAGMENT)) {
					String id = KnowledgeStackData.fragmentId(stack).orElse("");
					if (id.isEmpty()) return Result.invalid();
					// Vanilla crafting consumes one item from an occupied stack per craft.
					KnowledgeStackData.merge(counts, id, 1);
				} else if (stack.is(ModItems.KNOWLEDGE_FRAGMENT_COLLECTION)) {
					LinkedHashMap<String, Integer> collection = KnowledgeStackData.collectionCounts(stack);
					if (collection.isEmpty()) return Result.invalid();
					if (bookmark.isEmpty()) bookmark = KnowledgeStackData.normalizedBookmark(collection, KnowledgeStackData.bookmark(stack));
					collection.forEach((id, count) -> KnowledgeStackData.merge(counts, id, count));
				} else {
					return Result.invalid();
				}
			}
		}
		if (occupiedSlots < 2 || KnowledgeStackData.totalCount(counts) < 2) return Result.invalid();
		if (bookmark.isEmpty()) bookmark = KnowledgeStackData.normalizedBookmark(counts, "");
		return new Result(counts, bookmark, true);
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		return collect(input).valid();
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		Result result = collect(input);
		return result.valid() ? KnowledgeStackData.collection(result.counts(), result.bookmark()) : ItemStack.EMPTY;
	}

	@Override
	public RecipeSerializer<KnowledgeFragmentCollectionRecipe> getSerializer() {
		return SERIALIZER;
	}

	private record Result(LinkedHashMap<String, Integer> counts, String bookmark, boolean valid) {
		private static Result invalid() {
			return new Result(new LinkedHashMap<>(), "", false);
		}
	}
}
