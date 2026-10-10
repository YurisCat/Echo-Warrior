package com.yuriscat.echowarrior.compat.integration.mixin;

import com.yuriscat.echowarrior.compat.integration.TbfBridge1201;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201;
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
@Mixin(targets = "com.whidte.trulybestfriends.network.PetEntitySnapshot", remap = false)
public abstract class TbfSnapshotMixin1201 {
    @Inject(target = @Desc(value = "restore",
            args = {CompoundTag.class, UUID.class, ServerLevel.class}, ret = Entity.class),
            at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$rejectGenericRestore(CompoundTag snapshot, UUID expected, ServerLevel level,
            CallbackInfoReturnable<Entity> callback) {
        if (TbfBridge1201.enabled() && (snapshot.hasUUID(TbfBridge1201.MARKER)
                || snapshot.getString("EntityType").startsWith("echo_warrior:")
                || snapshot.getString("id").startsWith("echo_warrior:"))) callback.setReturnValue(null);
    }
}
