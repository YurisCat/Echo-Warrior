package com.yuriscat.echowarrior.compat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class SuspiciousBlockItem1211 extends BlockItem {
    public SuspiciousBlockItem1211(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echo_warrior.suspicious_block.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
