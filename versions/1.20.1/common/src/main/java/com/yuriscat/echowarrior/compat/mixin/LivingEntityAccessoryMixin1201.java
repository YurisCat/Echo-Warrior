package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1201;
import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1201;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAccessoryMixin1201 {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void echoWarrior1201$applyAccessoryDodge(DamageSource source, float amount,
                                                      CallbackInfoReturnable<Boolean> callback) {
        LivingEntity victim = (LivingEntity)(Object)this;
        if (!victim.level().isClientSide() && !EchoAccessorySystem1201.allowDamage(victim, source, amount)) {
            callback.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float echoWarrior1201$applyOutgoingModifiers(float amount, DamageSource source) {
        LivingEntity victim = (LivingEntity)(Object)this;
        if (!(victim.level() instanceof ServerLevel level)) return amount;
        var echo = EchoAccessorySystem1201.resolveAttackingEcho(source);
        float talented = echo == null ? amount : EchoTalentSystem1201.modifyOutgoingDamage(echo, victim, level, amount);
        return EchoAccessorySystem1201.modifyOutgoingDamage(victim, level, source, talented);
    }
}
