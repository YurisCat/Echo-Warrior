package com.yuriscat.echowarrior.compat.fabric;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.client.AutomatedTestPauseController1211;
import com.yuriscat.echowarrior.compat.client.CompatColorProviders1211;
import com.yuriscat.echowarrior.compat.client.CreativeSummonerDestructionSender1211;
import com.yuriscat.echowarrior.compat.client.CreativeSummonerInsertionSender1211;
import com.yuriscat.echowarrior.compat.network.CreativeSummonerDestructionPayload1211;
import com.yuriscat.echowarrior.compat.network.CreativeSummonerInsertionPayload1211;
import com.yuriscat.echowarrior.compat.client.KnowledgeReaderScreen1211;
import com.yuriscat.echowarrior.compat.client.RomanLegionaryRenderer1211;
import com.yuriscat.echowarrior.compat.client.RecyclerChestRenderer1211;
import com.yuriscat.echowarrior.compat.client.RecyclerChestItemRenderer1211;
import com.yuriscat.echowarrior.compat.client.RecyclerScreen1211;
import com.yuriscat.echowarrior.compat.client.AztecWarriorRenderer1211;
import com.yuriscat.echowarrior.compat.client.GuandaoWarriorRenderer1211;
import com.yuriscat.echowarrior.compat.client.JapaneseSamuraiRenderer1211;
import com.yuriscat.echowarrior.compat.client.EgyptianArcherArrowRenderer1211;
import com.yuriscat.echowarrior.compat.client.EgyptianArcherRenderer1211;
import com.yuriscat.echowarrior.compat.client.EchoCompassClientProperties1211;
import com.yuriscat.echowarrior.compat.client.EchoCompassPulseHud1211;
import com.yuriscat.echowarrior.compat.client.SummonerScreen1211;
import com.yuriscat.echowarrior.compat.client.SummonerRelicIconProperty1211;
import com.yuriscat.echowarrior.compat.client.TutorialManualScreen1211;
import com.yuriscat.echowarrior.compat.item.TooltipShiftState1211;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.gui.screens.Screen;

public final class EchoWarrior1211FabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CreativeSummonerInsertionSender1211.setSender((slot, carried) ->
                ClientPlayNetworking.send(new CreativeSummonerInsertionPayload1211(slot, carried)));
        CreativeSummonerDestructionSender1211.setSender(summonerIds ->
                ClientPlayNetworking.send(new CreativeSummonerDestructionPayload1211(summonerIds)));
        EchoCompassClientProperties1211.register();
        SummonerRelicIconProperty1211.register();
        ColorProviderRegistry.BLOCK.register(
                CompatColorProviders1211::suspiciousGrass, ModContent1211.SUSPICIOUS_GRASS_BLOCK);
        BlockRenderLayerMap.INSTANCE.putBlock(
                ModContent1211.SUSPICIOUS_GRASS_BLOCK, RenderType.cutoutMipped());
        ColorProviderRegistry.ITEM.register(
                CompatColorProviders1211::suspiciousGrassItem, ModContent1211.SUSPICIOUS_GRASS_BLOCK);
        ColorProviderRegistry.ITEM.register(
                CompatColorProviders1211::echoCompass, ModContent1211.ECHO_COMPASS);
        BuiltinItemRendererRegistry.INSTANCE.register(
                ModContent1211.ECHO_RECYCLER,
                (stack, context, poses, buffers, light, overlay) ->
                        RecyclerChestItemRenderer1211.getInstance()
                                .renderByItem(stack, context, poses, buffers, light, overlay));
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> EchoCompassPulseHud1211.render(graphics));
        EntityRendererRegistry.register(ModContent1211.ROMAN_LEGIONARY_ECHO, RomanLegionaryRenderer1211::new);
        ClientTickEvents.END_CLIENT_TICK.register(AutomatedTestPauseController1211::tick);
        ClientTickEvents.END_CLIENT_TICK.register(EchoCompassPulseHud1211::tick);
        EntityRendererRegistry.register(ModContent1211.AZTEC_WARRIOR_ECHO, AztecWarriorRenderer1211::new);
        EntityRendererRegistry.register(ModContent1211.GUANDAO_WARRIOR_ECHO, GuandaoWarriorRenderer1211::new);
        EntityRendererRegistry.register(ModContent1211.JAPANESE_SAMURAI_ECHO, JapaneseSamuraiRenderer1211::new);
        EntityRendererRegistry.register(ModContent1211.EGYPTIAN_ARCHER_ECHO, EgyptianArcherRenderer1211::new);
        EntityRendererRegistry.register(ModContent1211.EGYPTIAN_ARCHER_ARROW, EgyptianArcherArrowRenderer1211::new);
        MenuScreens.register(ModContent1211.SUMMONER_MENU, SummonerScreen1211::new);
        MenuScreens.register(ModContent1211.RECYCLER_MENU, RecyclerScreen1211::new);
        MenuScreens.register(ModContent1211.KNOWLEDGE_READER_MENU, KnowledgeReaderScreen1211::new);
        MenuScreens.register(ModContent1211.TUTORIAL_MANUAL_MENU, TutorialManualScreen1211::new);
        BlockEntityRenderers.register(ModContent1211.RECYCLER_CHEST, RecyclerChestRenderer1211::new);
        TooltipShiftState1211.setClientShiftDownSupplier(Screen::hasShiftDown);
    }
}
