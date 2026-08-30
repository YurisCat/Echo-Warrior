package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.item.EchoAccessorySystem;
import com.yuriscat.echowarrior.item.EchoTalentSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAccessoryMixin {
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float echoWarrior$applyAccessoryCritical(float amount, ServerLevel level, DamageSource source) {
		LivingEntity victim = (LivingEntity)(Object)this;
		float talented = EchoTalentSystem.modifyOutgoingDamage(victim, level, source, amount);
		return EchoAccessorySystem.modifyOutgoingDamage(victim, level, source, talented);
	}
}
