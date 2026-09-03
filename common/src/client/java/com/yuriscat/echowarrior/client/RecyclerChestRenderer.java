package com.yuriscat.echowarrior.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.block.entity.RecyclerChestBlockEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;

public final class RecyclerChestRenderer extends ChestRenderer<RecyclerChestBlockEntity> {
	private static final SpriteId TEXTURE = Sheets.CHEST_MAPPER.apply(EchoWarrior.id("recycler"));
	private final SpriteGetter sprites;
	private final ChestModel model;

	public RecyclerChestRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
		this.sprites = context.sprites();
		this.model = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
	}

	@Override
	public void submit(
			ChestRenderState state,
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			CameraRenderState camera
	) {
		poseStack.pushPose();
		poseStack.mulPose(ChestRenderer.modelTransformation(state.facing));
		float open = 1.0F - state.open;
		open = 1.0F - open * open * open;
		submitNodeCollector.submitModel(
				this.model,
				open,
				poseStack,
				state.lightCoords,
				OverlayTexture.NO_OVERLAY,
				-1,
				TEXTURE,
				this.sprites,
				0,
				state.breakProgress
		);
		poseStack.popPose();
	}
}
