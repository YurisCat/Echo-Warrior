package com.yuriscat.echowarrior.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1211;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class RecyclerChestRenderer1211 implements BlockEntityRenderer<RecyclerChestBlockEntity1211> {
    private static final Material TEXTURE = new Material(Sheets.CHEST_SHEET, ModContent1211.id("entity/chest/recycler"));
    private final ModelPart lid;
    private final ModelPart bottom;
    private final ModelPart lock;

    public RecyclerChestRenderer1211(BlockEntityRendererProvider.Context context) {
        ModelPart model = context.bakeLayer(ModelLayers.CHEST);
        this.bottom = model.getChild("bottom");
        this.lid = model.getChild("lid");
        this.lock = model.getChild("lock");
    }

    @Override
    public void render(RecyclerChestBlockEntity1211 blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        // Vanilla chest items face south while they have no level so their latch points toward the item camera.
        BlockState state = blockEntity.getLevel() == null
                ? ModContent1211.ECHO_RECYCLER.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH)
                : blockEntity.getBlockState();
        poseStack.pushPose();
        float facing = state.getValue(ChestBlock.FACING).toYRot();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        float open = 1.0F - blockEntity.getOpenNess(partialTick);
        open = 1.0F - open * open * open;
        this.lid.xRot = -(open * ((float)Math.PI / 2.0F));
        this.lock.xRot = this.lid.xRot;
        VertexConsumer consumer = TEXTURE.buffer(buffers, RenderType::entityCutout);
        this.lid.render(poseStack, consumer, packedLight, packedOverlay);
        this.lock.render(poseStack, consumer, packedLight, packedOverlay);
        this.bottom.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
