package com.yuriscat.echowarrior.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.entity.EgyptianArcherEchoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class EgyptianArcherEchoRenderer extends GeoEntityRenderer<EgyptianArcherEchoEntity, EntityRenderState> {
	public EgyptianArcherEchoRenderer(EntityRendererProvider.Context context) {
		super(context, ModEntities.EGYPTIAN_ARCHER_ECHO);
		this.shadowRadius = 0.45F;
		this.shadowStrength = 0.7F;
	}
}
