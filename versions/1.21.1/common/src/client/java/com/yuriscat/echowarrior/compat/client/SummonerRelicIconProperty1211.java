package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicItem1211;
import com.yuriscat.echowarrior.compat.item.EchoSummonerItem1211;
import com.yuriscat.echowarrior.compat.item.TooltipShiftState1211;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.ItemStack;

/** Legacy 1.21.1 item-model predicate equivalent of the mainline GUI select property. */
public final class SummonerRelicIconProperty1211 {
    private SummonerRelicIconProperty1211() {
    }

    public static void register() {
        ItemProperties.register(ModContent1211.ECHO_SUMMONER, ModContent1211.id("summoner_relic_icon"),
                (stack, level, owner, seed) -> selectedRelicIcon(stack));
    }

    private static float selectedRelicIcon(ItemStack summoner) {
        if (Minecraft.getInstance().screen == null || !TooltipShiftState1211.isShiftDown()) return 0.0F;
        ItemStack relic = EchoSummonerItem1211.relicStack(summoner);
        if (!(relic.getItem() instanceof EchoRelicItem1211 relicItem)) return 0.0F;
        return switch (relicItem.heroType()) {
            case ROMAN_LEGIONARY -> 0.1F;
            case AZTEC_WARRIOR -> 0.2F;
            case EGYPTIAN_ARCHER -> 0.3F;
            case GUANDAO_WARRIOR -> 0.4F;
            case JAPANESE_SAMURAI -> 0.5F;
        };
    }
}
