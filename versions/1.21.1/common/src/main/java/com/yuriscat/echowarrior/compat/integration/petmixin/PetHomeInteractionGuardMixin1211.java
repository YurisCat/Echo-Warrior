package com.yuriscat.echowarrior.compat.integration.petmixin;
import com.yuriscat.echowarrior.compat.integration.PetCollarCompat1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.server.event.PlayerInteractEntityHandler", remap = false)
public abstract class PetHomeInteractionGuardMixin1211 {
    @Inject(method = "onInteractWithEntity", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$protectControl(@Coerce Object event, CallbackInfo ci) {
        if (PetCollarCompat1211.rejectEventInteraction(event)) ci.cancel();
    }
}
