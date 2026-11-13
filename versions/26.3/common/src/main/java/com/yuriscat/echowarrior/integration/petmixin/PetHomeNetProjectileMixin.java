package com.yuriscat.echowarrior.integration.petmixin;
import com.yuriscat.echowarrior.integration.PetCollarCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.server.entity.NetEntity", remap = false)
public abstract class PetHomeNetProjectileMixin {
    @Inject(method = {"onHit"}, at = @At("HEAD"), cancellable = true)
    private void echoWarrior$returnLegacyCapturedItem(HitResult hit, CallbackInfo ci) {
        if (PetCollarCompat.rejectNetProjectile((Entity)(Object)this)) ci.cancel();
    }
}
