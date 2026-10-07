package com.yuriscat.echowarrior.compat.integration.mixin;

import com.yuriscat.echowarrior.compat.integration.TbfBridge1211;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.whidte.trulybestfriends.network.PetDeathState", remap = false)
public abstract class TbfUntrackingMixin1211 {
    // The bare name selects only one overload. Match both without embedding mapped Minecraft names.
    @Inject(method = "shouldReleaseBeforeUntracking*", at = @At("HEAD"), cancellable = true, require = 2, allow = 2)
    private static void echoWarrior$onlyRemoveTracking(CallbackInfoReturnable<Boolean> callback) {
        if (TbfBridge1211.removingMirror()) callback.setReturnValue(false);
    }
}
