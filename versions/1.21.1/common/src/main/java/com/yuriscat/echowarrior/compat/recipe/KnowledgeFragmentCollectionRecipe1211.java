package com.yuriscat.echowarrior.compat.recipe;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1211;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;

public final class KnowledgeFragmentCollectionRecipe1211 extends CustomRecipe {
    public KnowledgeFragmentCollectionRecipe1211(CraftingBookCategory category) {
        super(category);
    }

    private static Result collect(CraftingInput input) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        String bookmark = "";
        int occupiedSlots = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            occupiedSlots++;
            if (stack.is(ModContent1211.KNOWLEDGE_FRAGMENT)) {
                String id = KnowledgeStackData1211.fragmentId(stack).orElse("");
                if (id.isEmpty()) return Result.invalid();
                KnowledgeStackData1211.merge(counts, id, 1);
            } else if (stack.is(ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION)) {
                LinkedHashMap<String, Integer> collection = KnowledgeStackData1211.collectionCounts(stack);
                if (collection.isEmpty()) return Result.invalid();
                if (bookmark.isEmpty()) {
                    bookmark = KnowledgeStackData1211.normalizedBookmark(
                            collection, KnowledgeStackData1211.bookmark(stack));
                }
                collection.forEach((id, count) -> KnowledgeStackData1211.merge(counts, id, count));
            } else {
                return Result.invalid();
            }
        }
        if (occupiedSlots < 2 || KnowledgeStackData1211.totalCount(counts) < 2) return Result.invalid();
        if (bookmark.isEmpty()) bookmark = KnowledgeStackData1211.normalizedBookmark(counts, "");
        return new Result(counts, bookmark, true);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return collect(input).valid();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Result result = collect(input);
        return result.valid()
                ? KnowledgeStackData1211.collection(result.counts(), result.bookmark())
                : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION_SERIALIZER;
    }

    private record Result(LinkedHashMap<String, Integer> counts, String bookmark, boolean valid) {
        private static Result invalid() {
            return new Result(new LinkedHashMap<>(), "", false);
        }
    }
}
