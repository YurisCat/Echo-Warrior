package com.yuriscat.echowarrior.compat.integration.petmixin;
import com.yuriscat.echowarrior.compat.integration.PetCollarCompat1201;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.server.misc.PetCompassTeleport", remap = false)
public abstract class PetHomeCompassActionMixin1201 {
    @Inject(method = "teleportPetToPlayer", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$recall(ServerPlayer player, @Coerce Object record, CallbackInfo ci) {
        if (PetCollarCompat1201.compassAction(player, record, true)) ci.cancel();
    }
    @Inject(method = "teleportPlayerToPet", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$locate(ServerPlayer player, @Coerce Object record, CallbackInfo ci) {
        if (PetCollarCompat1201.compassAction(player, record, false)) ci.cancel();
    }
}
