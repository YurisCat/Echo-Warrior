package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.client.AutomatedTestController1201;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin1201 {
    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$keepMouseFreeDuringSmokeTest(CallbackInfo ci) {
        if (AutomatedTestController1201.blockMouseCapture()) ci.cancel();
    }
}
