package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.entity.EchoCombatState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class EchoCombatStateMixin implements EchoCombatState.Holder {
    @Unique private final EchoCombatState echoWarrior$combatState = new EchoCombatState();
    @Override public EchoCombatState echoWarrior$combatState() { return echoWarrior$combatState; }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void echoWarrior$saveCombatState(ValueOutput output, CallbackInfo callback) {
        output.store("EchoWarriorCombatState", CompoundTag.CODEC, echoWarrior$combatState.save());
    }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void echoWarrior$loadCombatState(ValueInput input, CallbackInfo callback) {
        echoWarrior$combatState.load(input.read("EchoWarriorCombatState", CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }

    @Inject(method = "onEffectsRemoved", at = @At("TAIL"))
    private void echoWarrior$clearRemovedBleeds(java.util.Collection<net.minecraft.world.effect.MobEffectInstance> effects, CallbackInfo callback) {
        for (var effect : effects) {
            if (effect.getEffect().equals(com.yuriscat.echowarrior.ModEffects.BLEEDING)) echoWarrior$combatState.clear(false);
            if (effect.getEffect().equals(com.yuriscat.echowarrior.ModEffects.OBSIDIAN_WOUND)) echoWarrior$combatState.clear(true);
        }
    }
}
