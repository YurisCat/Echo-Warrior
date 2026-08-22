package com.yuriscat.echowarrior.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "setLastHurtMob", at = @At("HEAD"), cancellable = true)
	private void echoWarrior$rememberMultipartBoss(Entity target, CallbackInfo callback) {
		if ((Object)this instanceof Player && target instanceof EnderDragonPart dragonPart) {
			((LivingEntity)(Object)this).setLastHurtMob(dragonPart.parentMob);
			callback.cancel();
		}
	}
}
