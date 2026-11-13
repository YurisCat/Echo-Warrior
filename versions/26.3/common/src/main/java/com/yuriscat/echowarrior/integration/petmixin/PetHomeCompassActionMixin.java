package com.yuriscat.echowarrior.integration.petmixin;
import com.yuriscat.echowarrior.integration.PetCollarCompat;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.server.misc.PetCompassTeleport", remap = false)
public abstract class PetHomeCompassActionMixin {
    @Inject(method = "teleportPetToPlayer", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$recall(ServerPlayer player, @Coerce Object record, CallbackInfo ci) {
        if (PetCollarCompat.compassAction(player, record, true)) ci.cancel();
    }
    @Inject(method = "teleportPlayerToPet", at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$locate(ServerPlayer player, @Coerce Object record, CallbackInfo ci) {
        if (PetCollarCompat.compassAction(player, record, false)) ci.cancel();
    }
}
