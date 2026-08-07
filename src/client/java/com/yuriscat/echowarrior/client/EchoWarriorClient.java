package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class EchoWarriorClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(
				ModEntities.ROMAN_LEGIONARY_ECHO,
				RomanLegionaryEchoRenderer::new
		);
	}
}
