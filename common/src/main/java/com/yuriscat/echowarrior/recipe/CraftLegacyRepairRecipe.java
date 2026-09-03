package com.yuriscat.echowarrior.recipe;

import com.mojang.serialization.MapCodec;
import com.yuriscat.echowarrior.ModItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class CraftLegacyRepairRecipe extends CustomRecipe {
	public static final CraftLegacyRepairRecipe INSTANCE = new CraftLegacyRepairRecipe();
	public static final RecipeSerializer<CraftLegacyRepairRecipe> SERIALIZER = new RecipeSerializer<>(
			MapCodec.unit(INSTANCE), StreamCodec.unit(INSTANCE));

	private static ItemStack findTarget(CraftingInput input) {
		if (input.ingredientCount() != 2) return ItemStack.EMPTY;
		boolean foundLegacy = false;
		ItemStack target = ItemStack.EMPTY;
		for (ItemStack stack : input.items()) {
			if (stack.isEmpty()) continue;
			if (stack.is(ModItems.CRAFT_LEGACY)) {
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
	public ItemStack assemble(CraftingInput input) {
		ItemStack target = findTarget(input);
		if (target.isEmpty()) return ItemStack.EMPTY;
		ItemStack repaired = target.copyWithCount(1);
		int repair = (int)Math.ceil(repaired.getMaxDamage() * 0.20);
		repaired.setDamageValue(Math.max(0, repaired.getDamageValue() - repair));
		return repaired;
	}

	@Override
	public RecipeSerializer<CraftLegacyRepairRecipe> getSerializer() {
		return SERIALIZER;
	}
}
