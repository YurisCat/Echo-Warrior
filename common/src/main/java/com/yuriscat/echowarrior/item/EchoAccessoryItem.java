package com.yuriscat.echowarrior.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class EchoAccessoryItem extends Item implements EchoSummonerAccessory {
	private static final int POSITIVE_COLOR = 0x93CE85;
	private static final int NEGATIVE_COLOR = 0xE46D6D;
	private static final int TERM_COLOR = KnowledgeTooltip.KNOWLEDGE_COLOR;
	private final AccessoryType type;

	public EchoAccessoryItem(Properties properties, AccessoryType type) {
		super(properties);
		this.type = type;
	}

	public AccessoryType type() {
		return this.type;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
			Consumer<Component> builder, TooltipFlag flag) {
		for (int index = 0; index < this.type.effects.size(); index++) {
			EffectPolarity polarity = this.type.effects.get(index);
			int color = polarity == EffectPolarity.POSITIVE ? POSITIVE_COLOR : NEGATIVE_COLOR;
			builder.accept(Component.literal(polarity.sign)
					.withStyle(style -> style.withColor(color))
					.append(Component.translatable(
							"item.echo_warrior.accessory." + this.type.id + ".effect." + (index + 1)
					).withStyle(style -> style.withColor(color))));
		}

		if (!TooltipShiftState.isShiftDown()) {
			builder.accept(Component.translatable("item.echo_warrior.accessory.more_hint")
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		Component summoner = coloredTerm("item.echo_warrior.accessory.term.summoner");
		Component echo = coloredTerm("item.echo_warrior.accessory.term.echo");
		Component accessory = coloredTerm("item.echo_warrior.accessory.term.accessory");
		builder.accept(detailLine("item.echo_warrior.accessory.detail.equip", summoner, echo));
		builder.accept(detailLine("item.echo_warrior.accessory.detail.unique", accessory));
	}

	private static Component detailLine(String translationKey, Component... arguments) {
		return Component.literal("+").withStyle(ChatFormatting.GRAY)
				.append(Component.translatable(translationKey, (Object[]) arguments)
						.withStyle(ChatFormatting.GRAY));
	}

	private static Component coloredTerm(String translationKey) {
		return Component.translatable(translationKey).withStyle(style -> style.withColor(TERM_COLOR));
	}

	public enum AccessoryType {
		PLATE_ARMOR("plate_armor", Rarity.COMMON, EffectPolarity.POSITIVE),
		CHAINMAIL_ARMOR("chainmail_armor", Rarity.UNCOMMON, EffectPolarity.POSITIVE, EffectPolarity.NEGATIVE),
		SPIKED_ARMOR("spiked_armor", Rarity.UNCOMMON, EffectPolarity.POSITIVE, EffectPolarity.POSITIVE),
		BATTLE_WORN_WHETSTONE("battle_worn_whetstone", Rarity.COMMON, EffectPolarity.POSITIVE),
		MOUNTAIN_BURDEN_BLADE("mountain_burden_blade", Rarity.UNCOMMON, EffectPolarity.POSITIVE, EffectPolarity.NEGATIVE),
		FRACTURED_CRYSTAL_BLADE("fractured_crystal_blade", Rarity.UNCOMMON, EffectPolarity.POSITIVE, EffectPolarity.NEGATIVE),
		TWIN_OATH_BADGE("twin_oath_badge", Rarity.COMMON, EffectPolarity.POSITIVE, EffectPolarity.POSITIVE),
		BATTLE_BLINDFOLD("battle_blindfold", Rarity.UNCOMMON, EffectPolarity.POSITIVE, EffectPolarity.NEGATIVE),
		CRACK_RING_HAMMER_CHARM("crack_ring_hammer_charm", Rarity.RARE, EffectPolarity.POSITIVE),
		VICTORS_LAUREL("victors_laurel", Rarity.RARE, EffectPolarity.POSITIVE),
		BLOOD_PACT_FANG("blood_pact_fang", Rarity.RARE, EffectPolarity.POSITIVE),
		MEMORY_RITUAL_KNIFE("memory_ritual_knife", Rarity.RARE, EffectPolarity.POSITIVE, EffectPolarity.POSITIVE),
		SUBSTITUTE_DOLL("substitute_doll", Rarity.COMMON, EffectPolarity.POSITIVE),
		HEART_SPROUT_AMBER("heart_sprout_amber", Rarity.COMMON, EffectPolarity.POSITIVE),
		FEAST_HAM("feast_ham", Rarity.UNCOMMON, EffectPolarity.POSITIVE, EffectPolarity.NEGATIVE),
		PEACEMAKER("peacemaker", Rarity.EPIC, EffectPolarity.POSITIVE, EffectPolarity.NEGATIVE),
		SUNWHEEL_GARLAND("sunwheel_garland", Rarity.UNCOMMON, EffectPolarity.POSITIVE),
		MOONDEW_BOTTLE("moondew_bottle", Rarity.UNCOMMON, EffectPolarity.POSITIVE),
		TOMATO_FISH("tomato_fish", Rarity.COMMON, EffectPolarity.POSITIVE),
		CAT_BELL_FISH_CHARM("cat_bell_fish_charm", Rarity.RARE, EffectPolarity.POSITIVE, EffectPolarity.POSITIVE),
		LIGHT_GATHERING_MAGNET("light_gathering_magnet", Rarity.UNCOMMON, EffectPolarity.POSITIVE),
		TRAINING_NOTES("training_notes", Rarity.UNCOMMON, EffectPolarity.POSITIVE, EffectPolarity.NEGATIVE),
		HAWKEYE_LENS("hawkeye_lens", Rarity.COMMON, EffectPolarity.POSITIVE),
		WINDCHASER_FEATHER("windchaser_feather", Rarity.COMMON, EffectPolarity.POSITIVE),
		HOLLOW_BIRD_BONE("hollow_bird_bone", Rarity.UNCOMMON, EffectPolarity.POSITIVE, EffectPolarity.NEGATIVE);

		private final String id;
		private final Rarity rarity;
		private final java.util.List<EffectPolarity> effects;

		AccessoryType(String id, Rarity rarity, EffectPolarity... effects) {
			this.id = id;
			this.rarity = rarity;
			this.effects = java.util.List.of(effects);
		}

		public String id() { return this.id; }

		public Rarity rarity() { return this.rarity; }

		public int effectCount() { return this.effects.size(); }

		public boolean effectIsPositive(int index) {
			return index >= 0 && index < this.effects.size() && this.effects.get(index) == EffectPolarity.POSITIVE;
		}
	}

	private enum EffectPolarity {
		POSITIVE("+"),
		NEGATIVE("-");

		private final String sign;

		EffectPolarity(String sign) {
			this.sign = sign;
		}
	}
}
