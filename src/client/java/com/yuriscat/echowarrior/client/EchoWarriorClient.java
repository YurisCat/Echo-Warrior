package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.ModBlocks;
import com.yuriscat.echowarrior.ModMenus;
import com.yuriscat.echowarrior.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.gui.screens.MenuScreens;

import java.util.List;

public final class EchoWarriorClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
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
	}
}
