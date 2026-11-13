package com.yuriscat.echowarrior.integration.petmixin;
import com.yuriscat.echowarrior.integration.PetCollarCompat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.network.PropertiesMessage", remap = false)
public abstract class PetHomePacketAuthorityMixin {
    @Inject(method = {"handleServer", "handle"}, at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$serverOwnedCollars(@Coerce Object message, @Coerce Object context, CallbackInfo ci) {
        if (PetCollarCompat.interceptRawPacket(message, context, "com.github.yzqdev.pethome.network.PropertiesMessage")) ci.cancel();
    }
}
