package com.yuriscat.echowarrior.client;

import com.geckolib.renderer.GeoEntityRenderer;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.entity.AztecWarriorEchoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public final class AztecWarriorEchoRenderer extends GeoEntityRenderer<AztecWarriorEchoEntity, EntityRenderState> {
	public AztecWarriorEchoRenderer(EntityRendererProvider.Context context) {
		super(context, ModEntities.AZTEC_WARRIOR_ECHO);
		this.shadowRadius = 0.48F;
		this.shadowStrength = 0.72F;
	}
}
