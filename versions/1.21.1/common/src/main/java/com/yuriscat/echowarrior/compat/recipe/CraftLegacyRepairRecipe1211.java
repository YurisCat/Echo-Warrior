package com.yuriscat.echowarrior.compat.recipe;

import com.yuriscat.echowarrior.compat.ModContent1211;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Repairs one damaged item by 20% when crafted together with a Craft Legacy. */
public final class CraftLegacyRepairRecipe1211 extends CustomRecipe {
    public CraftLegacyRepairRecipe1211(CraftingBookCategory category) {
        super(category);
    }

    private static ItemStack findTarget(CraftingInput input) {
        if (input.ingredientCount() != 2) return ItemStack.EMPTY;
        boolean foundLegacy = false;
        ItemStack target = ItemStack.EMPTY;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(ModContent1211.CRAFT_LEGACY)) {
                if (foundLegacy) return ItemStack.EMPTY;
                foundLegacy = true;
            } else {
                if (!target.isEmpty() || stack.getCount() != 1 || !stack.isDamageableItem() || !stack.isDamaged()) {
                    return ItemStack.EMPTY;
                }
                target = stack;
            }
        }
        return foundLegacy ? target : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !findTarget(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack target = findTarget(input);
        if (target.isEmpty()) return ItemStack.EMPTY;
        ItemStack repaired = target.copyWithCount(1);
        int repair = (int)Math.ceil(repaired.getMaxDamage() * 0.20);
        repaired.setDamageValue(Math.max(0, repaired.getDamageValue() - repair));
        return repaired;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModContent1211.CRAFT_LEGACY_REPAIR_SERIALIZER;
    }
}
