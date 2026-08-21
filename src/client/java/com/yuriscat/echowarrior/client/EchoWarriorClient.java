package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.ModMenus;
import com.yuriscat.echowarrior.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public final class EchoWarriorClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(
				ModEntities.ROMAN_LEGIONARY_ECHO,
				RomanLegionaryEchoRenderer::new
		);
		EntityRendererRegistry.register(ModEntities.AZTEC_WARRIOR_ECHO, AztecWarriorEchoRenderer::new);
		EntityRendererRegistry.register(ModEntities.EGYPTIAN_ARCHER_ECHO, EgyptianArcherEchoRenderer::new);
		EntityRendererRegistry.register(ModEntities.EGYPTIAN_ARCHER_ARROW, EgyptianArcherArrowRenderer::new);
		MenuScreens.register(ModMenus.SUMMONER, SummonerPreviewScreen::new);
	}
}
