package com.yuriscat.echowarrior.compat.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class SummonerFuel1211 {
    private static final String FUEL_KEY = "EchoWarriorFuel";
    public static final int CAPACITY = 1000;
    public static final int SUMMON_COST = 100;

    private SummonerFuel1211() {
    }

    public static int summonCost(ItemStack relic) {
        return EchoRelicState1211.summonCost(relic);
    }

    public static double healCost(ItemStack relic) {
        return EchoRelicState1211.naturalHealingCost(relic, 2);
    }

    public static int amount(ItemStack summoner) {
        int amount = summoner.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(FUEL_KEY);
        return Math.max(0, Math.min(CAPACITY, amount));
    }

    public static void setAmount(ItemStack summoner, int amount) {
        int clamped = Math.max(0, Math.min(CAPACITY, amount));
        CustomData.update(DataComponents.CUSTOM_DATA, summoner, tag -> tag.putInt(FUEL_KEY, clamped));
    }

    public static boolean consume(ItemStack summoner, int amount) {
        int current = amount(summoner);
        if (amount < 0 || current < amount) return false;
        setAmount(summoner, current - amount);
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
