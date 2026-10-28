package com.yuriscat.echowarrior.compat.integration.petmixin;
import com.yuriscat.echowarrior.compat.integration.PetCollarCompat1201;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.server.handler.EntityInteractHandler", remap = false)
public abstract class PetHomeInteractionGuardMixin1201 {
    @Inject(method = "onInteractWithEntity", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$protectControl(@Coerce Object event, CallbackInfo ci) {
        if (PetCollarCompat1201.rejectEventInteraction(event)) ci.cancel();
    }
}
