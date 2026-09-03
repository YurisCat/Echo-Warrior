package com.yuriscat.echowarrior.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Common item type for relics that can be inserted into an echo summoner.
 */
public class EchoRelicItem extends Item {
	private final EchoHeroType heroType;

	public EchoRelicItem(Properties properties, EchoHeroType heroType) {
		super(properties);
		this.heroType = heroType;
	}

	public EchoHeroType heroType() {
		return this.heroType;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
		EchoRelicState.ensureInitialized(stack, level.getRandom(), level.getGameTime());
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
		builder.accept(Component.translatable(
				"tooltip.echo_warrior.relic.level", level, EchoRelicProgress.MAX_LEVEL
		).withStyle(ChatFormatting.AQUA));
		if (level >= EchoRelicProgress.MAX_LEVEL) {
			builder.accept(Component.translatable("tooltip.echo_warrior.relic.experience.max")
					.withStyle(ChatFormatting.GRAY));
		} else {
			builder.accept(Component.translatable(
					"tooltip.echo_warrior.relic.experience",
					EchoRelicProgress.experience(stack),
					EchoRelicProgress.experienceNeeded(level)
			).withStyle(ChatFormatting.GRAY));
		}

		if (!EchoRelicState.initialized(stack)) {
			builder.accept(Component.translatable("tooltip.echo_warrior.relic.talents.pending")
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		int mask = EchoRelicState.traitMask(stack);
		builder.accept(Component.empty());
		if (mask == 0) {
			builder.accept(Component.translatable("tooltip.echo_warrior.relic.talents.none")
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		builder.accept(Component.translatable("tooltip.echo_warrior.relic.talents.header")
				.withStyle(ChatFormatting.GOLD));
		boolean showTalentDescriptions = TooltipShiftState.isShiftDown();
		for (EchoTrait trait : EchoTrait.values()) {
			if ((mask & trait.mask()) == 0) {
				continue;
			}
			String nameKey = trait == EchoTrait.BIOME_AFFINITY
					? EchoRelicState.biomeAffinity(stack).nameTranslationKey()
					: trait.nameTranslationKey();
			builder.accept(Component.translatable(nameKey).withStyle(ChatFormatting.AQUA));
			if (showTalentDescriptions) {
				builder.accept(Component.translatable(trait.descriptionTranslationKey())
						.withStyle(ChatFormatting.GRAY));
			}
		}
		if (!showTalentDescriptions) {
			builder.accept(Component.translatable("tooltip.echo_warrior.relic.more_hint")
					.withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
