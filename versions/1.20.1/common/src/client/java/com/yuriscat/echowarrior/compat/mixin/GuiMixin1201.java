package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.client.EchoCompassPulseHud1201;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin1201 {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$renderCompassMessages(Component component, boolean animateColor,
                                                    CallbackInfo callback) {
        if (EchoCompassPulseHud1201.acceptOverlayMessage(component)) callback.cancel();
    }
}
