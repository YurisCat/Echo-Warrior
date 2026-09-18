package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1211;
import com.yuriscat.echowarrior.compat.item.EchoTrait1211;
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
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Block.class)
public abstract class BlockTalentMixin1211 {
    @Redirect(method = "playerDestroy", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void echoWarrior1211$applyVirtualFortune(BlockState state, Level level, BlockPos pos,
                                                     BlockEntity blockEntity, Entity breaker, ItemStack tool) {
        ItemStack lootTool = tool;
        if (breaker instanceof Player player && !tool.isEmpty()
                && EchoTalentSystem1211.hasNearbyTalent(player, EchoTrait1211.LUCKY)) {
            Holder<Enchantment> fortune = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.FORTUNE);
            int current = EnchantmentHelper.getItemEnchantmentLevel(fortune, tool);
            if (current < 4) {
                lootTool = tool.copy();
                int effective = current + 1;
                EnchantmentHelper.updateEnchantments(lootTool, enchantments -> enchantments.set(fortune, effective));
            }
        }
        Block.dropResources(state, level, pos, blockEntity, breaker, lootTool);
    }
}
