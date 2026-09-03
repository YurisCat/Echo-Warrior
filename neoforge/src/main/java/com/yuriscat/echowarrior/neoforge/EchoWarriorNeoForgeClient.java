package com.yuriscat.echowarrior.neoforge;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModBlockEntities;
import com.yuriscat.echowarrior.ModBlocks;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.ModMenus;
import com.yuriscat.echowarrior.client.AztecWarriorEchoRenderer;
import com.yuriscat.echowarrior.client.EchoCompassAngleProperty;
import com.yuriscat.echowarrior.client.EchoCompassClientState;
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
import com.yuriscat.echowarrior.network.EchoCompassMessagePayload;
import com.yuriscat.echowarrior.network.EchoCompassPulsePayload;
import com.yuriscat.echowarrior.network.EchoCompassStatePayload;
import com.yuriscat.echowarrior.platform.ClientPlatformServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSources;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RegisterSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

import java.util.List;

final class EchoWarriorNeoForgeClient {
	private EchoWarriorNeoForgeClient() {
	}

	static void register(IEventBus modBus, IEventBus gameBus) {
		ClientPlatformServices.install(payload -> {
			if (Minecraft.getInstance().getConnection() == null) return false;
			ClientPacketDistributor.sendToServer(payload);
			return true;
		});
		modBus.addListener(EchoWarriorNeoForgeClient::onClientSetup);
		modBus.addListener(EchoWarriorNeoForgeClient::onClientPayloads);
		modBus.addListener(EchoWarriorNeoForgeClient::onRenderers);
		modBus.addListener(EchoWarriorNeoForgeClient::onMenuScreens);
		modBus.addListener(EchoWarriorNeoForgeClient::onGuiLayers);
		modBus.addListener(EchoWarriorNeoForgeClient::onBlockTints);
		modBus.addListener(EchoWarriorNeoForgeClient::onItemTints);
		modBus.addListener(EchoWarriorNeoForgeClient::onRangeProperties);
		modBus.addListener(EchoWarriorNeoForgeClient::onConditionalProperties);
		modBus.addListener(EchoWarriorNeoForgeClient::onSelectProperties);
		modBus.addListener(EchoWarriorNeoForgeClient::onRenderPipelines);
		gameBus.addListener(EchoWarriorNeoForgeClient::onLogin);
		gameBus.addListener(EchoWarriorNeoForgeClient::onLogout);
	}

	private static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(EchoWarriorClient::initializeState);
	}

	private static void onClientPayloads(RegisterClientPayloadHandlersEvent event) {
		event.register(EchoCompassStatePayload.TYPE, (payload, context) -> EchoCompassClientState.accept(payload));
		event.register(EchoCompassPulsePayload.TYPE, (payload, context) ->
				EchoCompassPulseHud.pulse(payload.closeness(), payload.directional()));
		// Use Minecraft's action bar on NeoForge so one-shot compass status messages
		// remain visible even if a custom HUD layer is suppressed by another mod.
		event.register(EchoCompassMessagePayload.TYPE, (payload, context) ->
				Minecraft.getInstance().gui.setOverlayMessage(payload.component(), false));
	}

	private static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntities.ROMAN_LEGIONARY_ECHO, RomanLegionaryEchoRenderer::new);
		event.registerEntityRenderer(ModEntities.AZTEC_WARRIOR_ECHO, AztecWarriorEchoRenderer::new);
		event.registerEntityRenderer(ModEntities.EGYPTIAN_ARCHER_ECHO, EgyptianArcherEchoRenderer::new);
		event.registerEntityRenderer(ModEntities.GUANDAO_WARRIOR_ECHO, GuandaoWarriorEchoRenderer::new);
		event.registerEntityRenderer(ModEntities.JAPANESE_SAMURAI_ECHO, JapaneseSamuraiEchoRenderer::new);
		event.registerEntityRenderer(ModEntities.EGYPTIAN_ARCHER_ARROW, EgyptianArcherArrowRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.RECYCLER_CHEST, RecyclerChestRenderer::new);
	}

	private static void onMenuScreens(RegisterMenuScreensEvent event) {
		event.register(ModMenus.SUMMONER, SummonerPreviewScreen::new);
		event.register(ModMenus.KNOWLEDGE_READER, KnowledgeReaderScreen::new);
		event.register(ModMenus.TUTORIAL_MANUAL, TutorialManualScreen::new);
		event.register(ModMenus.RECYCLER, RecyclerScreen::new);
	}

	private static void onGuiLayers(RegisterGuiLayersEvent event) {
		event.registerAbove(
				VanillaGuiLayers.OVERLAY_MESSAGE,
				EchoWarrior.id("echo_compass_message"),
				(graphics, deltaTracker) -> {
					EchoCompassPulseHud.renderMessage(graphics);
					EchoCompassPulseHud.renderDirectionalPulse(graphics);
				}
		);
	}

	private static void onBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
		event.register(List.of(BlockTintSources.grassBlock()), ModBlocks.SUSPICIOUS_GRASS_BLOCK);
	}

	private static void onItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
		event.register(EchoWarrior.id("echo_compass_pointer"), EchoCompassPointerTintSource.MAP_CODEC);
	}

	private static void onRangeProperties(RegisterRangeSelectItemModelPropertyEvent event) {
		event.register(EchoWarrior.id("echo_compass_angle"), EchoCompassAngleProperty.MAP_CODEC);
	}

	private static void onConditionalProperties(RegisterConditionalItemModelPropertyEvent event) {
		event.register(EchoWarrior.id("echo_compass_gold_frame"), EchoCompassGoldFrameProperty.MAP_CODEC);
		event.register(EchoWarrior.id("echo_compass_iron_frame"), EchoCompassIronFrameProperty.MAP_CODEC);
	}

	private static void onSelectProperties(RegisterSelectItemModelPropertyEvent event) {
		event.register(EchoWarrior.id("summoner_relic_icon"), SummonerRelicIconProperty.TYPE);
	}

	private static void onRenderPipelines(RegisterRenderPipelinesEvent event) {
		JapaneseSamuraiEchoRenderer.registerPipelines(event::registerPipeline);
	}

	private static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
		EchoWarriorClient.clearConnectionState();
	}

	private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		EchoWarriorClient.clearConnectionState();
	}
}
