package com.yuriscat.echowarrior.integration.petmixin;
import com.yuriscat.echowarrior.integration.PetCollarCompat;
import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.server.event.PlayerInteractEntityHandler", remap = false)
public abstract class PetHomeInteractionGuardMixin {
    @Inject(method = "onInteractWithEntity", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$protectControl(@Coerce Object event, CallbackInfo ci) {
        if (PetCollarCompat.rejectEventInteraction(event)) ci.cancel();
    }
}
