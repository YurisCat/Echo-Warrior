package com.yuriscat.echowarrior.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class KnowledgeTooltip {
	public static final int KNOWLEDGE_COLOR = 0xFFEDC9;
	private static final int RECYCLE_BIN_COLOR = 0xA68358;
	private static final int RECYCLE_ACTION_COLOR = 0x93CE85;

	private KnowledgeTooltip() {
	}

	public static Component knowledgeName(String translationKey) {
		return Component.translatable(translationKey).withStyle(style -> style.withColor(KNOWLEDGE_COLOR));
	}

	public static void appendFragmentDetails(Consumer<Component> builder) {
		if (!TooltipShiftState.isShiftDown()) {
			builder.accept(Component.translatable("item.echo_warrior.knowledge_fragment.more_hint")
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		Component fragment = coloredTerm(
				"item.echo_warrior.knowledge_fragment.term.fragment", KNOWLEDGE_COLOR);
		Component collection = coloredTerm(
				"item.echo_warrior.knowledge_fragment.term.collection", KNOWLEDGE_COLOR);
		Component recycleBin = coloredTerm(
				"item.echo_warrior.knowledge_fragment.term.recycle_bin", RECYCLE_BIN_COLOR);
		Component recycleAction = coloredTerm(
				"item.echo_warrior.knowledge_fragment.term.recycle_action", RECYCLE_ACTION_COLOR);

		builder.accept(Component.translatable(
				"item.echo_warrior.knowledge_fragment.detail.craft", fragment, collection
		).withStyle(ChatFormatting.GRAY));
		builder.accept(Component.translatable(
				"item.echo_warrior.knowledge_fragment.detail.recycle", fragment, recycleBin, recycleAction
		).withStyle(ChatFormatting.GRAY));
	}

	private static Component coloredTerm(String translationKey, int color) {
		return Component.translatable(translationKey).withStyle(style -> style.withColor(color));
	}
}
