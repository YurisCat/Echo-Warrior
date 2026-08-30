package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.ModBlocks;
import com.yuriscat.echowarrior.ModMenus;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.item.EchoCompassItem;
import com.yuriscat.echowarrior.network.EchoCompassMessagePayload;
import com.yuriscat.echowarrior.network.EchoCompassStatePayload;
import com.yuriscat.echowarrior.network.EchoCompassPulsePayload;
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
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;

import java.util.List;

public final class EchoWarriorClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EchoCompassItem.setClientInsideBattlefieldSupplier(EchoCompassClientState::isInsideBattlefieldMode);
		RangeSelectItemModelProperties.ID_MAPPER.put(
				EchoWarrior.id("echo_compass_angle"), EchoCompassAngleProperty.MAP_CODEC);
		ConditionalItemModelProperties.ID_MAPPER.put(
				EchoWarrior.id("echo_compass_gold_frame"), EchoCompassGoldFrameProperty.MAP_CODEC);
		ConditionalItemModelProperties.ID_MAPPER.put(
				EchoWarrior.id("echo_compass_iron_frame"), EchoCompassIronFrameProperty.MAP_CODEC);
		ItemTintSources.ID_MAPPER.put(
				EchoWarrior.id("echo_compass_pointer"), EchoCompassPointerTintSource.MAP_CODEC);
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
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			EchoCompassClientState.clear();
			EchoCompassPulseHud.clear();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			EchoCompassClientState.clear();
			EchoCompassPulseHud.clear();
		});
		BlockColorRegistry.register(List.of(BlockTintSources.grassBlock()), ModBlocks.SUSPICIOUS_GRASS_BLOCK);
		EntityRendererRegistry.register(
				ModEntities.ROMAN_LEGIONARY_ECHO,
				RomanLegionaryEchoRenderer::new
		);
		EntityRendererRegistry.register(ModEntities.AZTEC_WARRIOR_ECHO, AztecWarriorEchoRenderer::new);
		EntityRendererRegistry.register(ModEntities.EGYPTIAN_ARCHER_ECHO, EgyptianArcherEchoRenderer::new);
		EntityRendererRegistry.register(ModEntities.GUANDAO_WARRIOR_ECHO, GuandaoWarriorEchoRenderer::new);
		EntityRendererRegistry.register(ModEntities.JAPANESE_SAMURAI_ECHO, JapaneseSamuraiEchoRenderer::new);
		EntityRendererRegistry.register(ModEntities.EGYPTIAN_ARCHER_ARROW, EgyptianArcherArrowRenderer::new);
		MenuScreens.register(ModMenus.SUMMONER, SummonerPreviewScreen::new);
		MenuScreens.register(ModMenus.KNOWLEDGE_READER, KnowledgeReaderScreen::new);
	}
}
