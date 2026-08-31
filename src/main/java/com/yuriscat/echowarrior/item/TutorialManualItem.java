package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.menu.TutorialManualMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public final class TutorialManualItem extends Item {
	public TutorialManualItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		return TutorialManualMenu.open(player, hand);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> builder, TooltipFlag flag) {
		builder.accept(Component.translatable("item.echo_warrior.tutorial_manual.subtitle")
				.withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("item.echo_warrior.tutorial_manual.read_hint")
				.withStyle(ChatFormatting.DARK_GRAY));
	}
}
