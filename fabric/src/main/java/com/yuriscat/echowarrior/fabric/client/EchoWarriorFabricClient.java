package com.yuriscat.echowarrior.fabric.client;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModBlockEntities;
import com.yuriscat.echowarrior.ModBlocks;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.ModMenus;
import com.yuriscat.echowarrior.client.AztecWarriorEchoRenderer;
import com.yuriscat.echowarrior.client.EchoCompassClientState;
import com.yuriscat.echowarrior.client.EchoCompassAngleProperty;
import com.yuriscat.echowarrior.client.EchoCompassGoldFrameProperty;
import com.yuriscat.echowarrior.client.EchoCompassIronFrameProperty;
import com.yuriscat.echowarrior.client.EchoCompassPointerTintSource;
import com.yuriscat.echowarrior.client.EchoCompassPulseHud;
import com.yuriscat.echowarrior.client.EchoWarriorClient;
import com.yuriscat.echowarrior.client.EgyptianArcherArrowRenderer;
import com.yuriscat.echowarrior.client.EgyptianArcherEchoRenderer;
import com.yuriscat.echowarrior.client.GuandaoWarriorEchoRenderer;
import com.yuriscat.echowarrior.client.JapaneseSamuraiEchoRenderer;
import com.yuriscat.echowarrior.client.KnowledgeReaderScreen;
import com.yuriscat.echowarrior.client.RecyclerChestRenderer;
import com.yuriscat.echowarrior.client.RecyclerScreen;
import com.yuriscat.echowarrior.client.RomanLegionaryEchoRenderer;
import com.yuriscat.echowarrior.client.SummonerPreviewScreen;
import com.yuriscat.echowarrior.client.SummonerRelicIconProperty;
import com.yuriscat.echowarrior.client.TutorialManualScreen;
import com.yuriscat.echowarrior.network.CreativeSummonerDestroyPayload;
import com.yuriscat.echowarrior.network.EchoCompassMessagePayload;
import com.yuriscat.echowarrior.network.EchoCompassPulsePayload;
import com.yuriscat.echowarrior.network.EchoCompassStatePayload;
import com.yuriscat.echowarrior.platform.ClientPlatformServices;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;

import java.util.List;

public final class EchoWarriorFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlatformServices.install(payload -> {
			if (!ClientPlayNetworking.canSend(payload.type())) return false;
			ClientPlayNetworking.send(payload);
			return true;
		});
		EchoWarriorClient.initialize();
		RangeSelectItemModelProperties.ID_MAPPER.put(
				EchoWarrior.id("echo_compass_angle"), EchoCompassAngleProperty.MAP_CODEC);
		ConditionalItemModelProperties.ID_MAPPER.put(
				EchoWarrior.id("echo_compass_gold_frame"), EchoCompassGoldFrameProperty.MAP_CODEC);
		ConditionalItemModelProperties.ID_MAPPER.put(
				EchoWarrior.id("echo_compass_iron_frame"), EchoCompassIronFrameProperty.MAP_CODEC);
		SelectItemModelProperties.ID_MAPPER.put(
				EchoWarrior.id("summoner_relic_icon"), SummonerRelicIconProperty.TYPE);
		ItemTintSources.ID_MAPPER.put(
				EchoWarrior.id("echo_compass_pointer"), EchoCompassPointerTintSource.MAP_CODEC);
		JapaneseSamuraiEchoRenderer.registerPipelines(RenderPipelines::register);
		ClientPlayNetworking.registerGlobalReceiver(EchoCompassStatePayload.TYPE, (payload, context) ->
				context.client().execute(() -> EchoCompassClientState.accept(payload)));
		ClientPlayNetworking.registerGlobalReceiver(EchoCompassPulsePayload.TYPE, (payload, context) ->
				context.client().execute(() -> EchoCompassPulseHud.pulse(
						payload.closeness(), payload.directional())));
		ClientPlayNetworking.registerGlobalReceiver(EchoCompassMessagePayload.TYPE, (payload, context) ->
				context.client().execute(() -> EchoCompassPulseHud.showMessage(payload)));
		HudElementRegistry.attachElementAfter(
				VanillaHudElements.OVERLAY_MESSAGE,
				EchoWarrior.id("echo_compass_message"),
				(graphics, deltaTracker) -> {
					EchoCompassPulseHud.renderMessage(graphics);
					EchoCompassPulseHud.renderDirectionalPulse(graphics);
				});
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
				EchoWarriorClient.clearConnectionState());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
				EchoWarriorClient.clearConnectionState());
		BlockColorRegistry.register(List.of(BlockTintSources.grassBlock()), ModBlocks.SUSPICIOUS_GRASS_BLOCK);
		EntityRendererRegistry.register(ModEntities.ROMAN_LEGIONARY_ECHO, RomanLegionaryEchoRenderer::new);
		EntityRendererRegistry.register(ModEntities.AZTEC_WARRIOR_ECHO, AztecWarriorEchoRenderer::new);
		EntityRendererRegistry.register(ModEntities.EGYPTIAN_ARCHER_ECHO, EgyptianArcherEchoRenderer::new);
		EntityRendererRegistry.register(ModEntities.GUANDAO_WARRIOR_ECHO, GuandaoWarriorEchoRenderer::new);
		EntityRendererRegistry.register(ModEntities.JAPANESE_SAMURAI_ECHO, JapaneseSamuraiEchoRenderer::new);
		EntityRendererRegistry.register(ModEntities.EGYPTIAN_ARCHER_ARROW, EgyptianArcherArrowRenderer::new);
		BlockEntityRenderers.register(ModBlockEntities.RECYCLER_CHEST, RecyclerChestRenderer::new);
		MenuScreens.register(ModMenus.SUMMONER, SummonerPreviewScreen::new);
		MenuScreens.register(ModMenus.KNOWLEDGE_READER, KnowledgeReaderScreen::new);
		MenuScreens.register(ModMenus.TUTORIAL_MANUAL, TutorialManualScreen::new);
		MenuScreens.register(ModMenus.RECYCLER, RecyclerScreen::new);
	}
}
