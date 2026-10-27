package com.yuriscat.echowarrior.compat.integration.petmixin;
import com.yuriscat.echowarrior.compat.integration.PetCollarCompat1211;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.server.misc.PetCompassTracker", remap = false)
public abstract class PetHomeCompassTrackingMixin1211 {
    @Inject(target = @Desc(value = "updateRecord", args = {LivingEntity.class}), at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$stableEntry(LivingEntity entity, CallbackInfo ci) {
        if (!PetCollarCompat1211.preparePetRecord(entity)) ci.cancel();
    }
}
