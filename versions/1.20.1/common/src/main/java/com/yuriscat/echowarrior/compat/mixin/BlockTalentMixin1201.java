package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1201;
import com.yuriscat.echowarrior.compat.item.EchoTrait1201;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Block.class)
public abstract class BlockTalentMixin1201 {
    // Forge adds a boolean to the internal dropResources call. Change only the input snapshot,
    // leaving each loader's own destruction / experience pipeline intact.
    @ModifyVariable(method = "playerDestroy", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ItemStack echoWarrior1201$applyVirtualFortune(ItemStack tool, Level level, Player player,
            BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack originalTool) {
        ItemStack lootTool = tool;
        if (!tool.isEmpty()
                && EchoTalentSystem1201.hasNearbyTalent(player, EchoTrait1201.LUCKY)) {
            Enchantment fortune = Enchantments.BLOCK_FORTUNE;
            int current = EnchantmentHelper.getItemEnchantmentLevel(fortune, tool);
            if (current < 4) {
                lootTool = tool.copy();
                int effective = current + 1;
                var enchantments = new java.util.HashMap<>(EnchantmentHelper.getEnchantments(lootTool));
                enchantments.put(fortune, effective);
                EnchantmentHelper.setEnchantments(enchantments, lootTool);
            }
        }
        return lootTool;
    }
}
