package com.yuriscat.echowarrior.compat.recipe;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1201;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;

public final class KnowledgeFragmentCollectionRecipe1201 extends CustomRecipe {
    public KnowledgeFragmentCollectionRecipe1201(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    private static Result collect(CraftingContainer input) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        String bookmark = "";
        int occupiedSlots = 0;
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            occupiedSlots++;
            if (stack.is(ModContent1201.KNOWLEDGE_FRAGMENT)) {
                String id = KnowledgeStackData1201.fragmentId(stack).orElse("");
                if (id.isEmpty()) return Result.invalid();
                KnowledgeStackData1201.merge(counts, id, 1);
            } else if (stack.is(ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION)) {
                LinkedHashMap<String, Integer> collection = KnowledgeStackData1201.collectionCounts(stack);
                if (collection.isEmpty()) return Result.invalid();
                if (bookmark.isEmpty()) {
                    bookmark = KnowledgeStackData1201.normalizedBookmark(
                            collection, KnowledgeStackData1201.bookmark(stack));
                }
                collection.forEach((id, count) -> KnowledgeStackData1201.merge(counts, id, count));
            } else {
                return Result.invalid();
            }
        }
        if (occupiedSlots < 2 || KnowledgeStackData1201.totalCount(counts) < 2) return Result.invalid();
        if (bookmark.isEmpty()) bookmark = KnowledgeStackData1201.normalizedBookmark(counts, "");
        return new Result(counts, bookmark, true);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        return collect(input).valid();
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        Result result = collect(input);
        return result.valid()
                ? KnowledgeStackData1201.collection(result.counts(), result.bookmark())
                : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION_SERIALIZER;
    }

    private record Result(LinkedHashMap<String, Integer> counts, String bookmark, boolean valid) {
        private static Result invalid() {
            return new Result(new LinkedHashMap<>(), "", false);
        }
    }
}
