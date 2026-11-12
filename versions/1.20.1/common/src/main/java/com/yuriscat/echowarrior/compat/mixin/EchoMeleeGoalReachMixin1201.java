package com.yuriscat.echowarrior.compat.mixin;
import com.yuriscat.echowarrior.compat.entity.EchoMeleeReach1201;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MeleeAttackGoal.class)
public abstract class EchoMeleeGoalReachMixin1201 {
    @Shadow @Final protected PathfinderMob mob;
    @Inject(method = "getAttackReachSqr", at = @At("RETURN"), cancellable = true)
    private void echoWarrior$reduceGoalReach(LivingEntity target, CallbackInfoReturnable<Double> callback) {
        callback.setReturnValue(EchoMeleeReach1201.reduceSquaredRange(mob, target, callback.getReturnValue()));
    }
}
