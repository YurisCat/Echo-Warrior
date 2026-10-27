package com.yuriscat.echowarrior.compat.integration.petmixin;
import com.yuriscat.echowarrior.compat.integration.PetCollarCompat1211;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.server.entity.NetEntity", remap = false)
public abstract class PetHomeNetProjectileMixin1211 {
    @Inject(method = {"onHit", "method_7488"}, at = @At("HEAD"), cancellable = true)
    private void echoWarrior$returnLegacyCapturedItem(HitResult hit, CallbackInfo ci) {
        if (PetCollarCompat1211.rejectNetProjectile((Entity)(Object)this)) ci.cancel();
    }
}
