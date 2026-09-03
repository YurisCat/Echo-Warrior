package com.yuriscat.echowarrior.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public final class RecyclerChestItem extends BlockItem {
	public static final int NAME_COLOR = 0x419386;

	public RecyclerChestItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable("block.echo_warrior.echo_recycler")
				.withStyle(style -> style.withColor(NAME_COLOR));
	}

	@Override
	public void appendHoverText(
			ItemStack stack,
			TooltipContext context,
			TooltipDisplay display,
			Consumer<Component> builder,
			TooltipFlag flag
	) {
		builder.accept(Component.translatable("item.echo_warrior.echo_recycler.summary")
				.withStyle(ChatFormatting.GRAY));
		if (!TooltipShiftState.isShiftDown()) {
			builder.accept(Component.translatable("item.echo_warrior.echo_recycler.more_hint")
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		builder.accept(Component.empty());
		builder.accept(Component.translatable("gui.echo_warrior.recycler.accept_header")
				.withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("gui.echo_warrior.recycler.accept_knowledge")
				.withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("gui.echo_warrior.recycler.accept_legacy")
				.withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("gui.echo_warrior.recycler.accept_accessory")
				.withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("gui.echo_warrior.recycler.accept_relic")
				.withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable("gui.echo_warrior.recycler.midnight_result")
				.withStyle(ChatFormatting.GRAY));
	}
}
