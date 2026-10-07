package com.yuriscat.echowarrior.compat.integration.mixin;

import com.yuriscat.echowarrior.compat.integration.TbfBridge1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.UUID;

@Pseudo
@Mixin(targets = "com.whidte.trulybestfriends.trulybestfriends", remap = false)
public abstract class TbfOwnershipMixin1211 {
    @Inject(method = "getCompatOwnerUUID", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$externalOwnership(Entity entity, CallbackInfoReturnable<UUID> callback) {
        if (TbfBridge1211.enabled() && entity instanceof EchoWarriorEntity1211) callback.setReturnValue(null);
    }
    @Inject(method = "tryForceLoadPet", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$trackAuthoritativeCompanion(Entity entity, ServerPlayer player, ServerLevel level,
            CallbackInfoReturnable<Object> callback) {
        Object result = TbfBridge1211.forceTrack(entity, player);
        if (result != null) callback.setReturnValue(result);
    }
}
