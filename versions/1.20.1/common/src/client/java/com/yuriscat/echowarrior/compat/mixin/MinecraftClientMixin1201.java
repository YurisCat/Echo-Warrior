package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.client.AutomatedTestController1201;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin1201 {
    @Inject(method = "tick", at = @At("TAIL"))
    private void echoWarrior$smokeTick(CallbackInfo ci) {
        AutomatedTestController1201.tick((Minecraft)(Object)this);
        com.yuriscat.echowarrior.compat.client.EchoCompassPulseHud1201.tick((Minecraft)(Object)this);
    }
}
