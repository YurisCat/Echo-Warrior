package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.item.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** The old override API has no display context: scope it to GuiGraphics item rendering. */
public final class SummonerRelicIconProperty1201 {
    private static int guiDepth;
    private static float lastGuiIcon;
    private SummonerRelicIconProperty1201() {}

    public static void register(EchoCompassClientProperties1201.Registrar registrar) {
        registrar.register(ModContent1201.ECHO_SUMMONER, ModContent1201.id("summoner_relic_icon"),
                (stack, level, owner, seed) -> selectedRelicIcon(stack));
    }

    public static void beginGui() { guiDepth++; }
    public static void endGui() { guiDepth--; }
    public static float lastGuiIcon() { return lastGuiIcon; }

    private static float selectedRelicIcon(ItemStack summoner) {
        if (guiDepth == 0 || Minecraft.getInstance().screen == null) return 0;
        lastGuiIcon = 0;
        if (!TooltipShiftState1201.isShiftDown()) return 0;
        ItemStack relic = SummonerData1201.contents(summoner).get(SummonerData1201.RELIC_SLOT);
        if (relic.getItem() instanceof EchoRelicItem1201 item) {
            lastGuiIcon = switch (item.heroType()) {
                case ROMAN_LEGIONARY -> 0.1F;
                case AZTEC_WARRIOR -> 0.2F;
                case EGYPTIAN_ARCHER -> 0.3F;
                case GUANDAO_WARRIOR -> 0.4F;
                case JAPANESE_SAMURAI -> 0.5F;
            };
        }
        return lastGuiIcon;
    }
}
