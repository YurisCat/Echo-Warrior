package com.yuriscat.echowarrior.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Common item type for relics that can be inserted into an echo summoner.
 */
public class EchoRelicItem extends Item {
	public EchoRelicItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(
			ItemStack stack,
			Item.TooltipContext context,
			TooltipDisplay display,
			Consumer<Component> builder,
			TooltipFlag tooltipFlag
	) {
		int level = EchoRelicProgress.level(stack);
		builder.accept(Component.literal("等级 " + level + "/" + EchoRelicProgress.MAX_LEVEL).withStyle(ChatFormatting.AQUA));
		if (level >= EchoRelicProgress.MAX_LEVEL) {
			builder.accept(Component.literal("经验：已达到最高等级").withStyle(ChatFormatting.GRAY));
		} else {
			builder.accept(Component.literal(
					"经验 " + EchoRelicProgress.experience(stack) + "/" + EchoRelicProgress.experienceNeeded(level)
			).withStyle(ChatFormatting.GRAY));
		}
	}
}
