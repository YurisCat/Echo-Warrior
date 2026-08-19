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
		builder.accept(Component.literal("等级 " + level + "/" + EchoRelicProgress.MAX_LEVEL).withStyle(ChatFormatting.AQUA));
		if (level >= EchoRelicProgress.MAX_LEVEL) {
			builder.accept(Component.literal("经验：已达到最高等级").withStyle(ChatFormatting.GRAY));
		} else {
			builder.accept(Component.literal(
					"经验 " + EchoRelicProgress.experience(stack) + "/" + EchoRelicProgress.experienceNeeded(level)
			).withStyle(ChatFormatting.GRAY));
		}

		if (!EchoRelicState.initialized(stack)) {
			builder.accept(Component.literal("天赋：等待生成").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		int mask = EchoRelicState.traitMask(stack);
		builder.accept(Component.empty());
		if (mask == 0) {
			builder.accept(Component.literal("没有天赋（本遗物的随机结果）").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		builder.accept(Component.literal("天赋：").withStyle(ChatFormatting.GOLD));
		for (EchoTrait trait : EchoTrait.values()) {
			if ((mask & trait.mask()) == 0) {
				continue;
			}
			builder.accept(Component.literal(trait.displayName()).withStyle(ChatFormatting.AQUA));
			builder.accept(Component.literal(traitDescription(trait)).withStyle(ChatFormatting.GRAY));
		}
	}

	private static String traitDescription(EchoTrait trait) {
		return switch (trait) {
			case BAD_TEMPER -> "召唤与自然恢复燃料消耗+20%，攻击力+4";
			case LAZY -> "召唤与自然恢复燃料消耗-20%，移动速度-25%";
			case COURAGE -> "攻击力+2";
			case SKINNY -> "生命值-25%，移动速度和攻击速度+25%";
			case STURDY -> "护甲+4，移动速度-25%";
		};
	}
}
