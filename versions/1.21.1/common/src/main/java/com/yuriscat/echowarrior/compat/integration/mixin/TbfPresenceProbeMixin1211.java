package com.yuriscat.echowarrior.compat.integration.mixin;

import com.yuriscat.echowarrior.compat.integration.TbfBridge1211;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.whidte.trulybestfriends.network.PetPresenceProbe", remap = false)
public abstract class TbfPresenceProbeMixin1211 {
    @Inject(method = "shouldProbe", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$logicalEntryIsNotPhysicalEntity(ServerPlayer player, UUID id,
            CompoundTag snapshot, CallbackInfoReturnable<Boolean> callback) {
        // Binding authority owns these display records. Ordinary pets retain TBF's original probe.
        if (TbfBridge1211.enabled() && snapshot.hasUUID(TbfBridge1211.MARKER))
            callback.setReturnValue(false);
    }
}
