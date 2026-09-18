package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1211;
import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1211;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAccessoryMixin1211 {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void echoWarrior1211$applyAccessoryDodge(DamageSource source, float amount,
                                                      CallbackInfoReturnable<Boolean> callback) {
        LivingEntity victim = (LivingEntity)(Object)this;
        if (!victim.level().isClientSide() && !EchoAccessorySystem1211.allowDamage(victim, source, amount)) {
            callback.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float echoWarrior1211$applyOutgoingModifiers(float amount, DamageSource source) {
        LivingEntity victim = (LivingEntity)(Object)this;
        if (!(victim.level() instanceof ServerLevel level)) return amount;
        var echo = EchoAccessorySystem1211.resolveAttackingEcho(source);
        float talented = echo == null ? amount : EchoTalentSystem1211.modifyOutgoingDamage(echo, victim, level, amount);
        return EchoAccessorySystem1211.modifyOutgoingDamage(victim, level, source, talented);
    }
}
