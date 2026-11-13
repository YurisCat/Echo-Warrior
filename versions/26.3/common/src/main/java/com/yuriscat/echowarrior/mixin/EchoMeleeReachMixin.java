package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.entity.EchoMeleeReach;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Mob.class)
public abstract class EchoMeleeReachMixin {
    @ModifyVariable(method = "getAttackBoundingBox", at = @At("HEAD"), argsOnly = true)
    private double echoWarrior$reduceExternalReach(double reach) {
        return EchoMeleeReach.restricted((Mob)(Object)this) ? reach * 0.2 : reach;
    }
}
