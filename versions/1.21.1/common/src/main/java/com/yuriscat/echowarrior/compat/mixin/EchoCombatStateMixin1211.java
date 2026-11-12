package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.entity.EchoCombatState1211;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class EchoCombatStateMixin1211 implements EchoCombatState1211.Holder {
    @Unique private final EchoCombatState1211 echoWarrior$combatState = new EchoCombatState1211();
    @Override public EchoCombatState1211 echoWarrior$combatState() { return echoWarrior$combatState; }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void echoWarrior$saveCombatState(CompoundTag output, CallbackInfo callback) {
        output.put("EchoWarriorCombatState", echoWarrior$combatState.save());
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void echoWarrior$loadCombatState(CompoundTag input, CallbackInfo callback) {
        echoWarrior$combatState.load(input.getCompound("EchoWarriorCombatState"));
    }

    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void echoWarrior$clearRemovedBleeds(net.minecraft.world.effect.MobEffectInstance effect, CallbackInfo callback) {

            if (effect.getEffect().equals(com.yuriscat.echowarrior.compat.ModEffects1211.BLEEDING)) echoWarrior$combatState.clear(false);
            if (effect.getEffect().equals(com.yuriscat.echowarrior.compat.ModEffects1211.OBSIDIAN_WOUND)) echoWarrior$combatState.clear(true);

    }
}
