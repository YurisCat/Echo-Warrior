package com.yuriscat.echowarrior.compat.integration.mixin;

import com.yuriscat.echowarrior.compat.integration.TbfBridge1211;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.whidte.trulybestfriends.network.PetEntitySnapshot", remap = false)
public abstract class TbfSnapshotOwnerMixin1211 {
    // Class literals follow JAR remapping, including Fabric's production namespace.
    @Inject(target = @Desc(value = "restore",
            args = {CompoundTag.class, UUID.class, ServerLevel.class, UUID.class}, ret = Entity.class),
            at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$rejectRestoreWithOwner(CompoundTag snapshot, UUID id, ServerLevel level,
            UUID owner, CallbackInfoReturnable<Entity> callback) {
        if (TbfBridge1211.enabled() && TbfBridge1211.isEchoSnapshot(snapshot)) callback.setReturnValue(null);
    }
}
