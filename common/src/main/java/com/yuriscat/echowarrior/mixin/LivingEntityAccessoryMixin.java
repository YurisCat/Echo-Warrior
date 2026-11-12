package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.item.EchoAccessorySystem;
import com.yuriscat.echowarrior.item.EchoTalentSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.yuriscat.echowarrior.entity.EchoAssistPolicy;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAccessoryMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float echoWarrior$applyAccessoryCritical(float amount, ServerLevel level, DamageSource source) {
		LivingEntity victim = (LivingEntity)(Object)this;
        if (source.is(com.yuriscat.echowarrior.ModDamageTypes.QUETZALCOATL_CURSE)
                || source.is(com.yuriscat.echowarrior.ModDamageTypes.BLEEDING)
                || source.is(com.yuriscat.echowarrior.ModDamageTypes.OBSIDIAN_WOUND)) return amount;
		float talented = EchoTalentSystem.modifyOutgoingDamage(victim, level, source, amount);
		return EchoAccessorySystem.modifyOutgoingDamage(victim, level, source, talented);
	}

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$protectAssistedTargets(ServerLevel level, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> callback) {
        if (!com.yuriscat.echowarrior.entity.EchoMeleeReach.allowsDamage((LivingEntity)(Object)this, source)
                || !EchoAssistPolicy.allowsDamage((LivingEntity)(Object)this, source)) callback.setReturnValue(false);
    }
}
