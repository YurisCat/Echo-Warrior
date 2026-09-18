package com.yuriscat.echowarrior.compat.neoforge;

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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

final class EchoWarrior1211NeoForgeClient {
    private EchoWarrior1211NeoForgeClient() {
    }

    static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(EchoWarrior1211NeoForgeClient::onClientSetup);
        modBus.addListener(EchoWarrior1211NeoForgeClient::registerRenderers);
        modBus.addListener(EchoWarrior1211NeoForgeClient::registerScreens);
        modBus.addListener(EchoWarrior1211NeoForgeClient::registerBlockColors);
        modBus.addListener(EchoWarrior1211NeoForgeClient::registerItemColors);
        modBus.addListener(EchoWarrior1211NeoForgeClient::registerClientExtensions);
        gameBus.addListener(EchoWarrior1211NeoForgeClient::onClientTick);
        gameBus.addListener(EchoWarrior1211NeoForgeClient::onRenderGui);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CreativeSummonerInsertionSender1211.setSender((slot, carried) ->
                    PacketDistributor.sendToServer(new CreativeSummonerInsertionPayload1211(slot, carried)));
            CreativeSummonerDestructionSender1211.setSender(summonerIds ->
                    PacketDistributor.sendToServer(new CreativeSummonerDestructionPayload1211(summonerIds)));
            EchoCompassClientProperties1211.register();
            SummonerRelicIconProperty1211.register();
            TooltipShiftState1211.setClientShiftDownSupplier(Screen::hasShiftDown);
        });
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModContent1211.ROMAN_LEGIONARY_ECHO, RomanLegionaryRenderer1211::new);
        event.registerEntityRenderer(ModContent1211.AZTEC_WARRIOR_ECHO, AztecWarriorRenderer1211::new);
        event.registerEntityRenderer(ModContent1211.GUANDAO_WARRIOR_ECHO, GuandaoWarriorRenderer1211::new);
        event.registerEntityRenderer(ModContent1211.JAPANESE_SAMURAI_ECHO, JapaneseSamuraiRenderer1211::new);
        event.registerEntityRenderer(ModContent1211.EGYPTIAN_ARCHER_ECHO, EgyptianArcherRenderer1211::new);
        event.registerEntityRenderer(ModContent1211.EGYPTIAN_ARCHER_ARROW, EgyptianArcherArrowRenderer1211::new);
        event.registerBlockEntityRenderer(ModContent1211.RECYCLER_CHEST, RecyclerChestRenderer1211::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModContent1211.SUMMONER_MENU, SummonerScreen1211::new);
        event.register(ModContent1211.RECYCLER_MENU, RecyclerScreen1211::new);
        event.register(ModContent1211.KNOWLEDGE_READER_MENU, KnowledgeReaderScreen1211::new);
        event.register(ModContent1211.TUTORIAL_MANUAL_MENU, TutorialManualScreen1211::new);
    }

    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(CompatColorProviders1211::suspiciousGrass, ModContent1211.SUSPICIOUS_GRASS_BLOCK);
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(CompatColorProviders1211::suspiciousGrassItem, ModContent1211.SUSPICIOUS_GRASS_BLOCK);
        event.register(CompatColorProviders1211::echoCompass, ModContent1211.ECHO_COMPASS);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return RecyclerChestItemRenderer1211.getInstance();
            }
        }, ModContent1211.ECHO_RECYCLER.asItem());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        AutomatedTestPauseController1211.tick(client);
        EchoCompassPulseHud1211.tick(client);
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        EchoCompassPulseHud1211.render(event.getGuiGraphics());
    }
}
