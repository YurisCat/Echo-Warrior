package com.yuriscat.echowarrior.compat.fabric;

import com.yuriscat.echowarrior.compat.network.EchoNetwork1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertResult1201;
import com.yuriscat.echowarrior.compat.network.InventoryNetwork1201;
import com.yuriscat.echowarrior.compat.network.CreativeInsertReply1201;
import com.yuriscat.echowarrior.compat.network.InsertionFeedback1201;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public final class EchoClient1201Fabric implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(com.yuriscat.echowarrior.compat.network.EchoProgressionPacket1201.ID, (client, handler, buffer, sender) -> {
            var packet = com.yuriscat.echowarrior.compat.network.EchoProgressionPacket1201.decode(buffer);
            client.execute(packet::apply);
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> com.yuriscat.echowarrior.compat.item.EchoProgressionConfig1201.resetConnection());
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, partialTick) ->
                com.yuriscat.echowarrior.compat.client.EchoCompassPulseHud1201.render(graphics));
        com.yuriscat.echowarrior.compat.client.EchoCompassClientProperties1201.register(net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry::register);
        com.yuriscat.echowarrior.compat.client.SummonerRelicIconProperty1201.register(net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry::register);
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
                com.yuriscat.echowarrior.compat.client.CompatColorProviders1201::suspiciousGrass,
                com.yuriscat.echowarrior.compat.ModContent1201.SUSPICIOUS_GRASS_BLOCK);
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
                com.yuriscat.echowarrior.compat.client.CompatColorProviders1201::suspiciousGrassItem,
                com.yuriscat.echowarrior.compat.ModContent1201.SUSPICIOUS_GRASS_BLOCK_ITEM);
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
                com.yuriscat.echowarrior.compat.client.CompatColorProviders1201::echoCompass,
                com.yuriscat.echowarrior.compat.ModContent1201.ECHO_COMPASS);
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(
                com.yuriscat.echowarrior.compat.ModContent1201.SUSPICIOUS_GRASS_BLOCK, net.minecraft.client.renderer.RenderType.cutoutMipped());
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(com.yuriscat.echowarrior.compat.ModContent1201.RECYCLER_CHEST, com.yuriscat.echowarrior.compat.client.RecyclerChestRenderer1201::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.yuriscat.echowarrior.compat.ModContent1201.RECYCLER_MENU, com.yuriscat.echowarrior.compat.client.RecyclerScreen1201::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.yuriscat.echowarrior.compat.ModContent1201.KNOWLEDGE_READER_MENU, com.yuriscat.echowarrior.compat.client.KnowledgeReaderScreen1201::new);
        net.minecraft.client.gui.screens.MenuScreens.register(com.yuriscat.echowarrior.compat.ModContent1201.TUTORIAL_MANUAL_MENU, com.yuriscat.echowarrior.compat.client.TutorialManualScreen1201::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.yuriscat.echowarrior.compat.ModContent1201.ROMAN_LEGIONARY_ECHO, com.yuriscat.echowarrior.compat.client.RomanLegionaryRenderer1201::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.yuriscat.echowarrior.compat.ModContent1201.AZTEC_WARRIOR_ECHO, com.yuriscat.echowarrior.compat.client.AztecWarriorRenderer1201::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.yuriscat.echowarrior.compat.ModContent1201.GUANDAO_WARRIOR_ECHO, com.yuriscat.echowarrior.compat.client.GuandaoWarriorRenderer1201::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.yuriscat.echowarrior.compat.ModContent1201.JAPANESE_SAMURAI_ECHO, com.yuriscat.echowarrior.compat.client.JapaneseSamuraiRenderer1201::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.yuriscat.echowarrior.compat.ModContent1201.EGYPTIAN_ARCHER_ECHO, com.yuriscat.echowarrior.compat.client.EgyptianArcherRenderer1201::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(com.yuriscat.echowarrior.compat.ModContent1201.EGYPTIAN_ARCHER_ARROW, com.yuriscat.echowarrior.compat.client.EgyptianArcherArrowRenderer1201::new);
        com.yuriscat.echowarrior.compat.client.InventoryInsertionClient1201.initialize();
        InventoryNetwork1201.destructionSender = request -> {
            var buffer = PacketByteBufs.create();
            request.encode(buffer);
            ClientPlayNetworking.send(InventoryNetwork1201.CREATIVE_DESTROY, buffer);
        };
        InventoryNetwork1201.creativeSender = request -> {
            var buffer = PacketByteBufs.create();
            request.encode(buffer);
            ClientPlayNetworking.send(InventoryNetwork1201.CREATIVE_INSERT, buffer);
        };
        ClientPlayNetworking.registerGlobalReceiver(InventoryNetwork1201.CREATIVE_REPLY, (client, handler, buffer, sender) -> {
            var reply = CreativeInsertReply1201.decode(buffer);
            client.execute(() -> InventoryNetwork1201.creativeReceiver.accept(reply));
        });
        ClientPlayNetworking.registerGlobalReceiver(InventoryNetwork1201.FEEDBACK, (client, handler, buffer, sender) -> {
            var reply = InsertionFeedback1201.decode(buffer);
            client.execute(() -> InventoryNetwork1201.feedbackReceiver.accept(reply));
        });
        com.yuriscat.echowarrior.compat.item.TooltipShiftState1201.setClientShiftDownSupplier(
                net.minecraft.client.gui.screens.Screen::hasShiftDown);
        net.minecraft.client.gui.screens.MenuScreens.register(
                com.yuriscat.echowarrior.compat.ModContent1201.SUMMONER_MENU,
                com.yuriscat.echowarrior.compat.client.SummonerScreen1201::new);
        EchoNetwork1201.installClientSender(request -> {
            var buffer = PacketByteBufs.create();
            request.encode(buffer);
            ClientPlayNetworking.send(EchoNetwork1201.INSERT, buffer);
        });
        ClientPlayNetworking.registerGlobalReceiver(EchoNetwork1201.RESULT, (client, handler, buffer, sender) -> {
            SummonerInsertResult1201 result = SummonerInsertResult1201.decode(buffer);
            client.execute(() -> EchoNetwork1201.receiveOnClientThread(result));
        });
    }
}
