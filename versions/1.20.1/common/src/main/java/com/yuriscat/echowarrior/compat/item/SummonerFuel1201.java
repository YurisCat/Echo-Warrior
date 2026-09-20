package com.yuriscat.echowarrior.compat.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SummonerFuel1201 {
    public static final int CAPACITY = SummonerData1201.FUEL_CAPACITY;
    public static final int SUMMON_COST = 100;
    private SummonerFuel1201() {}
    public static int summonCost(ItemStack relic) { return EchoRelicState1201.summonCost(relic); }
    public static double healCost(ItemStack relic) { return EchoRelicState1201.naturalHealingCost(relic, 2); }
    public static int value(ItemStack stack) {
        if (stack.is(Items.ROTTEN_FLESH)) return 20;
        if (stack.is(Items.SOUL_SAND) || stack.is(Items.SOUL_SOIL)) return 50;
        return 0;
    }
}
