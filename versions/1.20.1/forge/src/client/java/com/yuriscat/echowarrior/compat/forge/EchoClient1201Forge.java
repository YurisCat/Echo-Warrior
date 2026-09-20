package com.yuriscat.echowarrior.compat.forge;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.network.EchoNetwork1201;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = EchoWarrior1201.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class EchoClient1201Forge {
    @SubscribeEvent public static void renderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.yuriscat.echowarrior.compat.ModContent1201.RECYCLER_CHEST, com.yuriscat.echowarrior.compat.client.RecyclerChestRenderer1201::new);
        event.registerEntityRenderer(com.yuriscat.echowarrior.compat.ModContent1201.ROMAN_LEGIONARY_ECHO, com.yuriscat.echowarrior.compat.client.RomanLegionaryRenderer1201::new);
        event.registerEntityRenderer(com.yuriscat.echowarrior.compat.ModContent1201.AZTEC_WARRIOR_ECHO, com.yuriscat.echowarrior.compat.client.AztecWarriorRenderer1201::new);
        event.registerEntityRenderer(com.yuriscat.echowarrior.compat.ModContent1201.GUANDAO_WARRIOR_ECHO, com.yuriscat.echowarrior.compat.client.GuandaoWarriorRenderer1201::new);
        event.registerEntityRenderer(com.yuriscat.echowarrior.compat.ModContent1201.JAPANESE_SAMURAI_ECHO, com.yuriscat.echowarrior.compat.client.JapaneseSamuraiRenderer1201::new);
        event.registerEntityRenderer(com.yuriscat.echowarrior.compat.ModContent1201.EGYPTIAN_ARCHER_ECHO, com.yuriscat.echowarrior.compat.client.EgyptianArcherRenderer1201::new);
        event.registerEntityRenderer(com.yuriscat.echowarrior.compat.ModContent1201.EGYPTIAN_ARCHER_ARROW, com.yuriscat.echowarrior.compat.client.EgyptianArcherArrowRenderer1201::new);
    }
    @SubscribeEvent public static void initialize(FMLClientSetupEvent event) {
        // ForgeGui overrides Gui.render without calling super: a vanilla render Mixin never runs here.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(EchoClient1201Forge::renderHud);
        event.enqueueWork(() -> {
            com.yuriscat.echowarrior.compat.client.EchoCompassClientProperties1201.register(net.minecraft.client.renderer.item.ItemProperties::register);
            com.yuriscat.echowarrior.compat.client.SummonerRelicIconProperty1201.register(net.minecraft.client.renderer.item.ItemProperties::register);
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    com.yuriscat.echowarrior.compat.ModContent1201.SUSPICIOUS_GRASS_BLOCK, net.minecraft.client.renderer.RenderType.cutoutMipped());
            net.minecraft.client.gui.screens.MenuScreens.register(com.yuriscat.echowarrior.compat.ModContent1201.RECYCLER_MENU, com.yuriscat.echowarrior.compat.client.RecyclerScreen1201::new);
            net.minecraft.client.gui.screens.MenuScreens.register(com.yuriscat.echowarrior.compat.ModContent1201.KNOWLEDGE_READER_MENU, com.yuriscat.echowarrior.compat.client.KnowledgeReaderScreen1201::new);
            net.minecraft.client.gui.screens.MenuScreens.register(com.yuriscat.echowarrior.compat.ModContent1201.TUTORIAL_MANUAL_MENU, com.yuriscat.echowarrior.compat.client.TutorialManualScreen1201::new);
            com.yuriscat.echowarrior.compat.client.InventoryInsertionClient1201.initialize();
            com.yuriscat.echowarrior.compat.network.InventoryNetwork1201.destructionSender = EchoNetworking1201Forge.CHANNEL::sendToServer;
            com.yuriscat.echowarrior.compat.network.InventoryNetwork1201.creativeSender = EchoNetworking1201Forge.CHANNEL::sendToServer;
            com.yuriscat.echowarrior.compat.item.TooltipShiftState1201.setClientShiftDownSupplier(
                    net.minecraft.client.gui.screens.Screen::hasShiftDown);
            EchoNetwork1201.installClientSender(EchoNetworking1201Forge.CHANNEL::sendToServer);
            net.minecraft.client.gui.screens.MenuScreens.register(
                    com.yuriscat.echowarrior.compat.ModContent1201.SUMMONER_MENU,
                    com.yuriscat.echowarrior.compat.client.SummonerScreen1201::new);
        });
    }
    private static void renderHud(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
        com.yuriscat.echowarrior.compat.client.EchoCompassPulseHud1201.render(event.getGuiGraphics());
    }
    @SubscribeEvent public static void blockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register(com.yuriscat.echowarrior.compat.client.CompatColorProviders1201::suspiciousGrass,
                com.yuriscat.echowarrior.compat.ModContent1201.SUSPICIOUS_GRASS_BLOCK);
    }
    @SubscribeEvent public static void itemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register(com.yuriscat.echowarrior.compat.client.CompatColorProviders1201::suspiciousGrassItem,
                com.yuriscat.echowarrior.compat.ModContent1201.SUSPICIOUS_GRASS_BLOCK_ITEM);
        event.register(com.yuriscat.echowarrior.compat.client.CompatColorProviders1201::echoCompass,
                com.yuriscat.echowarrior.compat.ModContent1201.ECHO_COMPASS);
    }
}
