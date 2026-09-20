package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.entity.EchoCombatEvents1201;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201;
import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1201;
import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1201;
import com.yuriscat.echowarrior.compat.progress.EchoExperienceSystem1201;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.20.1 Fabric has no AFTER_DAMAGE callback. One common hook avoids double dispatch on Forge. */
@Mixin({LivingEntity.class, net.minecraft.world.entity.player.Player.class})
public abstract class LivingCombatEventsMixin1201 {
    @Unique private final java.util.ArrayDeque<Float> echoWarrior1201$beforeDamage = new java.util.ArrayDeque<>();

    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void echoWarrior1201$captureDamage(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        echoWarrior1201$beforeDamage.push(self.getHealth() + self.getAbsorptionAmount());
    }

    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void echoWarrior1201$afterDamage(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (echoWarrior1201$beforeDamage.isEmpty()) return;
        float previous = echoWarrior1201$beforeDamage.pop();
        if (self.level().isClientSide) return;
        float damage = Math.max(0, previous - self.getHealth() - self.getAbsorptionAmount());
        EchoExperienceSystem1201.afterDamage(self, source, damage, false);
        EchoTalentSystem1201.afterDamage(self, source, damage, false);
        EchoAccessorySystem1201.afterDamage(self, source, damage, false);
        EchoCombatEvents1201.afterDamage(self, source, damage, false);
    }

}
