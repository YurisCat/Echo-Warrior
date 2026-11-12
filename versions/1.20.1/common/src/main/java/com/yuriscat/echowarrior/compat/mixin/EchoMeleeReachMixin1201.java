package com.yuriscat.echowarrior.compat.mixin;
import com.yuriscat.echowarrior.compat.entity.EchoMeleeReach1201;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Mob.class)
public abstract class EchoMeleeReachMixin1201 {
    @Inject(method = "getMeleeAttackRangeSqr", at = @At("RETURN"), cancellable = true)
    private void echoWarrior$reduceReach(LivingEntity target, CallbackInfoReturnable<Double> callback) {
        callback.setReturnValue(EchoMeleeReach1201.reduceSquaredRange((Mob)(Object)this, target, callback.getReturnValue()));
    }
}
