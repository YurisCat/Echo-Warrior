package com.yuriscat.echowarrior.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class SummonerFuel {
	private static final String FUEL_KEY = "EchoWarriorFuel";
	private static final String FRACTION_KEY = "EchoWarriorFuelFraction";
	public static final int CAPACITY = 1000;
	public static final int BASE_SUMMON_COST = 100;
	public static final double BASE_HEAL_COST = 2.0;

	private SummonerFuel() {
	}

	public static int amount(ItemStack summoner) {
		return Math.clamp(summoner.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getIntOr(FUEL_KEY, 0), 0, CAPACITY);
	}

	public static void setAmount(ItemStack summoner, int amount) {
		int clamped = Math.clamp(amount, 0, CAPACITY);
		CustomData.update(DataComponents.CUSTOM_DATA, summoner, tag -> tag.putInt(FUEL_KEY, clamped));
	}

	public static int summonCost(ItemStack relic) {
		return (int)Math.ceil(BASE_SUMMON_COST * EchoRelicState.summonCostPercent(relic) / 100.0);
	}

	public static double healCost(ItemStack relic) {
		return BASE_HEAL_COST * EchoRelicState.summonCostPercent(relic) / 100.0;
	}

	public static boolean consume(ItemStack summoner, int amount) {
		int current = amount(summoner);
		if (amount < 0 || current < amount) {
			return false;
		}
		setAmount(summoner, current - amount);
		return true;
	}

	public static boolean consumeFractional(ItemStack summoner, double cost) {
		if (cost <= 0.0) {
			return true;
		}
		double fraction = summoner.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getDoubleOr(FRACTION_KEY, 0.0);
		double total = fraction + cost;
		int whole = (int)Math.floor(total + 1.0E-7);
		if (amount(summoner) < whole) {
			return false;
		}
		setAmount(summoner, amount(summoner) - whole);
		double remaining = total - whole;
		CustomData.update(DataComponents.CUSTOM_DATA, summoner, tag -> tag.putDouble(FRACTION_KEY, remaining));
		return true;
	}

	public static boolean isFuel(ItemStack stack) {
		return stack.is(Items.ROTTEN_FLESH) || stack.is(Items.SOUL_SAND) || stack.is(Items.SOUL_SOIL);
	}

	public static int value(ItemStack stack) {
		if (stack.is(Items.ROTTEN_FLESH)) return 20;
		if (stack.is(Items.SOUL_SAND) || stack.is(Items.SOUL_SOIL)) return 50;
		return 0;
	}
}
