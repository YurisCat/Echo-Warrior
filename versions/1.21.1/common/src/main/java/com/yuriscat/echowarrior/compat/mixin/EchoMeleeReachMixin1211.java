package com.yuriscat.echowarrior.compat.mixin;
import com.yuriscat.echowarrior.compat.entity.EchoMeleeReach1211;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Mob.class)
public abstract class EchoMeleeReachMixin1211 {
    @Inject(method = "getAttackBoundingBox", at = @At("RETURN"), cancellable = true)
    private void echoWarrior$reduceReach(CallbackInfoReturnable<AABB> callback) {
        callback.setReturnValue(EchoMeleeReach1211.reduceBox((Mob)(Object)this, callback.getReturnValue()));
    }
}
