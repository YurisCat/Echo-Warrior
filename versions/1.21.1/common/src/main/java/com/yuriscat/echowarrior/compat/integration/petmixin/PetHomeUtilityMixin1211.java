package com.yuriscat.echowarrior.compat.integration.petmixin;

import com.yuriscat.echowarrior.compat.integration.PetCollarCompat1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.util.TameableUtils", remap = false)
public abstract class PetHomeUtilityMixin1211 {
    @Inject(target = @Desc(value = "setHealingAuraTime", args = {LivingEntity.class, int.class}), at = @At("RETURN"))
    private static void echoWarrior$saveHealingCooldown(LivingEntity entity, int time, CallbackInfo ci) {
        PetCollarCompat1211.capture(entity);
    }

    @Inject(target = @Desc(value = "getPetBedPos", args = {LivingEntity.class}, ret = BlockPos.class),
            at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$noIndependentRespawn(LivingEntity entity, CallbackInfoReturnable<BlockPos> ci) {
        if (entity instanceof EchoWarriorEntity1211) ci.setReturnValue(null);
    }

    @Inject(target = @Desc(value = "setPetBedPos", args = {LivingEntity.class, BlockPos.class}),
            at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$rejectBedBinding(LivingEntity entity, BlockPos bed, CallbackInfo ci) {
        if (entity instanceof EchoWarriorEntity1211) {
            PetCollarCompat1211.explainRestriction(entity);
            ci.cancel();
        }
    }

    @Inject(target = @Desc(value = "isValidTeleporter", args = {LivingEntity.class, Mob.class}, ret = boolean.class),
            at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$keepBindingTeleport(LivingEntity owner, Mob animal, CallbackInfoReturnable<Boolean> ci) {
        if (animal instanceof EchoWarriorEntity1211) ci.setReturnValue(false);
    }
    @Inject(method = "isTamed", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$currentOwner(net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<Boolean> ci) {
        if (entity instanceof EchoWarriorEntity1211) ci.setReturnValue(PetCollarCompat1211.ownerId(entity) != null);
    }
    @Inject(method = "couldBeTamed", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$recognizeOwnedWarrior(net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<Boolean> ci) {
        if (entity instanceof EchoWarriorEntity1211) ci.setReturnValue(true);
    }
    @Inject(method = "getOwnerUUIDOf", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$ownerUuid(net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<java.util.UUID> ci) {
        if (entity instanceof EchoWarriorEntity1211) ci.setReturnValue(PetCollarCompat1211.ownerId(entity));
    }
    @Inject(method = "getOwnerOf", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$owner(net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<net.minecraft.world.entity.Entity> ci) {
        if (entity instanceof EchoWarriorEntity1211) {
            var id = PetCollarCompat1211.ownerId(entity);
            ci.setReturnValue(id == null ? null : entity.level().getPlayerByUUID(id));
        }
    }
    @Inject(method = "isPetOf", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$interactionOwner(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<Boolean> ci) {
        if (entity instanceof EchoWarriorEntity1211) ci.setReturnValue(player.getUUID().equals(PetCollarCompat1211.ownerId(entity)));
    }
    @Inject(method = "hasSameOwnerAsOneWay", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$sharedOwner(net.minecraft.world.entity.Entity first, net.minecraft.world.entity.Entity second,
            CallbackInfoReturnable<Boolean> ci) {
        if (first instanceof EchoWarriorEntity1211 || second instanceof EchoWarriorEntity1211) {
            var owner = PetCollarCompat1211.comparisonOwner(first);
            ci.setReturnValue(owner != null && owner.equals(PetCollarCompat1211.comparisonOwner(second)));
        }
    }
}
