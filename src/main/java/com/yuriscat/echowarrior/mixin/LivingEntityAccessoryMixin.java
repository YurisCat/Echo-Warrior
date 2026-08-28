package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.item.EchoAccessorySystem;
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
		return EchoAccessorySystem.modifyOutgoingDamage((LivingEntity)(Object)this, level, source, amount);
	}
}
