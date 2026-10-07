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
@Mixin(targets = "com.whidte.trulybestfriends.network.RecallPetPacket", remap = false)
public abstract class TbfNeoforgeRecallPetPacketMixin1211 {
    @Inject(method = "handle(Lcom/whidte/trulybestfriends/network/RecallPetPacket;Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$dispatch(@Coerce Object packet, @Coerce Object context, CallbackInfo callback) {
        if (TbfBridge1211.intercept(packet, context)) callback.cancel();
    }

    @Inject(method = "handleWithoutRideSwap", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$teamSummon(UUID uuid, @Coerce Object context, CallbackInfo callback) {
        if (TbfBridge1211.summonWithoutRideSwap(uuid, context)) callback.cancel();
    }
}
