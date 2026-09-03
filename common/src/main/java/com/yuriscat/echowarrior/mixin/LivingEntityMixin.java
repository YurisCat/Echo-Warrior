package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.entity.JapaneseSamuraiEchoEntity;
import com.yuriscat.echowarrior.item.EchoTalentSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void echoWarrior$freezeSamuraiStabTarget(Vec3 input, CallbackInfo callback) {
		LivingEntity self = (LivingEntity)(Object)this;
		if (!self.level().isClientSide() && JapaneseSamuraiEchoEntity.isTemporarilyPinned(self)) {
			self.setDeltaMovement(Vec3.ZERO);
			callback.cancel();
		}
	}

	@Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
	private void echoWarrior$blockPinnedMeleeAttack(ServerLevel level, Entity target,
			CallbackInfoReturnable<Boolean> callback) {
		LivingEntity self = (LivingEntity)(Object)this;
		if (JapaneseSamuraiEchoEntity.isTemporarilyPinned(self)) callback.setReturnValue(false);
	}

	@Inject(method = "setLastHurtMob", at = @At("HEAD"), cancellable = true)
	private void echoWarrior$rememberMultipartBoss(Entity target, CallbackInfo callback) {
		if ((Object)this instanceof Player && target instanceof EnderDragonPart dragonPart) {
			((LivingEntity)(Object)this).setLastHurtMob(dragonPart.parentMob);
			callback.cancel();
		}
	}

	@Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
	private void echoWarrior$applyUnyieldingFinalReduction(DamageSource source, float damage,
			CallbackInfoReturnable<Float> callback) {
		callback.setReturnValue(EchoTalentSystem.modifyFinalIncomingDamage(
				(LivingEntity)(Object)this, source, callback.getReturnValue()));
	}
}
