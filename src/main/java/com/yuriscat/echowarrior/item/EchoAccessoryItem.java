package com.yuriscat.echowarrior.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class EchoAccessoryItem extends Item implements EchoSummonerAccessory {
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
		builder.accept(Component.translatable("item.echo_warrior.accessory.echo_only"));
		builder.accept(Component.translatable("item.echo_warrior.accessory." + this.type.id + ".effect"));
	}

	public enum AccessoryType {
		PLATE_ARMOR("plate_armor"), CHAINMAIL_ARMOR("chainmail_armor"), SPIKED_ARMOR("spiked_armor"),
		BATTLE_WORN_WHETSTONE("battle_worn_whetstone"), MOUNTAIN_BURDEN_BLADE("mountain_burden_blade"),
		FRACTURED_CRYSTAL_BLADE("fractured_crystal_blade"), TWIN_OATH_BADGE("twin_oath_badge"),
		BATTLE_BLINDFOLD("battle_blindfold"), CRACK_RING_HAMMER_CHARM("crack_ring_hammer_charm"),
		VICTORS_LAUREL("victors_laurel"), BLOOD_PACT_FANG("blood_pact_fang"), MEMORY_RITUAL_KNIFE("memory_ritual_knife"),
		SUBSTITUTE_DOLL("substitute_doll"), HEART_SPROUT_AMBER("heart_sprout_amber"), FEAST_HAM("feast_ham"),
		PEACEMAKER("peacemaker"), SUNWHEEL_GARLAND("sunwheel_garland"), MOONDEW_BOTTLE("moondew_bottle"),
		TOMATO_FISH("tomato_fish"), CAT_BELL_FISH_CHARM("cat_bell_fish_charm"),
		LIGHT_GATHERING_MAGNET("light_gathering_magnet"), TRAINING_NOTES("training_notes"),
		HAWKEYE_LENS("hawkeye_lens"), WINDCHASER_FEATHER("windchaser_feather"), HOLLOW_BIRD_BONE("hollow_bird_bone");

		private final String id;

		AccessoryType(String id) { this.id = id; }

		public String id() { return this.id; }
	}
}
