package com.yuriscat.echowarrior.compat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.client.RecyclerChestItemRenderer1201;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Both old loaders use the vanilla default BEWLR unless an item provides its own extension. */
@Mixin(BlockEntityWithoutLevelRenderer.class)
public abstract class RecyclerItemRendererMixin1201 {
    @Inject(method = "renderByItem", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$renderRecycler(ItemStack stack, ItemDisplayContext context, PoseStack poses,
            MultiBufferSource buffers, int light, int overlay, CallbackInfo callback) {
        if (stack.is(ModContent1201.ECHO_RECYCLER_ITEM)) {
            RecyclerChestItemRenderer1201.getInstance().renderByItem(stack, context, poses, buffers, light, overlay);
            callback.cancel();
        }
    }
}
