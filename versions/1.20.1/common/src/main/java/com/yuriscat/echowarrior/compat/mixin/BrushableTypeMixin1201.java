package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.block.StableBrushableBlock1201;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.20.1 has no shared add-valid-blocks event. Preserve the vanilla brushable type and renderer. */
@Mixin(BlockEntityType.class)
public abstract class BrushableTypeMixin1201 {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$acceptStableBrushables(BlockState state, CallbackInfoReturnable<Boolean> callback) {
        if ((Object)this == BlockEntityType.BRUSHABLE_BLOCK && state.getBlock() instanceof StableBrushableBlock1201) {
            callback.setReturnValue(true);
        }
    }
}
