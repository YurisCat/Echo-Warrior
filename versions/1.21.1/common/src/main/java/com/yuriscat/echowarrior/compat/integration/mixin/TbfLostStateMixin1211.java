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
@Mixin(targets = "com.whidte.trulybestfriends.network.RequestPetDataPacket", remap = false)
public abstract class TbfLostStateMixin1211 {
    @Inject(method = "shouldMarkLost", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$resolveExternalStatus(CompoundTag snapshot, boolean loaded, CallbackInfoReturnable<Boolean> callback) {
        if (TbfBridge1211.enabled() && snapshot.hasUUID(TbfBridge1211.MARKER)) callback.setReturnValue(false);
    }
}
