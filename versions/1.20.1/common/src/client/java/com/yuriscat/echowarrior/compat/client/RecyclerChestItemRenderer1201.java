package com.yuriscat.echowarrior.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders the recycler item through the exact same block-entity renderer as the placed chest. */
public final class RecyclerChestItemRenderer1201 extends BlockEntityWithoutLevelRenderer {
    private static RecyclerChestItemRenderer1201 instance;
    private final BlockEntityRenderDispatcher dispatcher;
    private final RecyclerChestBlockEntity1201 itemChest;

    public RecyclerChestItemRenderer1201(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
        this.dispatcher = dispatcher;
        this.itemChest = new RecyclerChestBlockEntity1201(
                BlockPos.ZERO, ModContent1201.ECHO_RECYCLER.defaultBlockState());
    }

    /** NeoForge requests client extensions before vanilla model layers are baked, so construction must stay lazy. */
    public static RecyclerChestItemRenderer1201 getInstance() {
        if (instance == null) {
            Minecraft client = Minecraft.getInstance();
            instance = new RecyclerChestItemRenderer1201(
                    client.getBlockEntityRenderDispatcher(), client.getEntityModels());
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        this.dispatcher.renderItem(this.itemChest, poseStack, buffers, packedLight, packedOverlay);
    }
}
