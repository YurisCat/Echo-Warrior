package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.combat.FormationAura1211;
import com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1211;
import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1211;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin1211 {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void echoWarrior1211$freezeSamuraiStabTarget(Vec3 input, CallbackInfo callback) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!self.level().isClientSide() && JapaneseSamuraiEchoEntity1211.isTemporarilyPinned(self)) {
            self.setDeltaMovement(Vec3.ZERO);
            callback.cancel();
        }
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void echoWarrior1211$blockPinnedMeleeAttack(Entity target,
                                                         CallbackInfoReturnable<Boolean> callback) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (JapaneseSamuraiEchoEntity1211.isTemporarilyPinned(self)) callback.setReturnValue(false);
    }

    @Inject(method = "setLastHurtMob", at = @At("HEAD"), cancellable = true)
    private void echoWarrior1211$rememberMultipartBoss(Entity target, CallbackInfo callback) {
        if ((Object)this instanceof Player && target instanceof EnderDragonPart dragonPart) {
            ((LivingEntity)(Object)this).setLastHurtMob(dragonPart.parentMob);
            callback.cancel();
        }
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true)
    private void echoWarrior1211$applyFormationShield(DamageSource source, float damage,
                                                       CallbackInfoReturnable<Float> callback) {
        LivingEntity victim = (LivingEntity)(Object)this;
        float formationAdjusted = FormationAura1211.modifyFinalIncomingDamage(victim, callback.getReturnValue());
        callback.setReturnValue(EchoTalentSystem1211.modifyFinalIncomingDamage(victim, source, formationAdjusted));
    }
}
