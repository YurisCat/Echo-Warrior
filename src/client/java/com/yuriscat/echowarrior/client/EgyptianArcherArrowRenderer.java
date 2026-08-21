package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.entity.EgyptianArcherArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;

public final class EgyptianArcherArrowRenderer extends ArrowRenderer<EgyptianArcherArrowEntity, ArrowRenderState> {
	private static final Identifier ARROW_TEXTURE = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");

	public EgyptianArcherArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected Identifier getTextureLocation(ArrowRenderState state) {
		return ARROW_TEXTURE;
	}

	@Override
	public ArrowRenderState createRenderState() {
		return new ArrowRenderState();
	}
}
