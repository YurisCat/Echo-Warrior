package com.yuriscat.echowarrior.compat.integration.petmixin;
import com.yuriscat.echowarrior.compat.integration.PetCollarCompat1201;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import java.util.List;
import java.util.UUID;
@Pseudo
@Mixin(targets = "com.github.alexthe668.domesticationinnovation.server.misc.DIWorldData", remap = false)
public abstract class DomesticationRequestGuardMixin1201 {
    @Inject(method = "addRespawnRequest", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$noNewRespawn(@Coerce Object request, CallbackInfo ci) {
        if (PetCollarCompat1201.isEchoRequest(request)) ci.cancel();
    }
    @Inject(method = "addLanternRequest", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$noNewLantern(@Coerce Object request, CallbackInfo ci) {
        if (PetCollarCompat1201.isEchoRequest(request)) ci.cancel();
    }
    @Inject(target = @Desc(value = "getRespawnRequestsFor", args = {Level.class, BlockPos.class}, ret = List.class),
            at = @At("RETURN"), cancellable = true)
    private void echoWarrior$filterRespawns(Level level, BlockPos pos, CallbackInfoReturnable<List<?>> ci) {
        var original = ci.getReturnValue();
        var filtered = original.stream().filter(r -> !PetCollarCompat1201.isEchoRequest(r)).toList();
        if (filtered.size() != original.size()) ci.setReturnValue(new java.util.ArrayList<>(filtered));
    }
    @Inject(target = @Desc(value = "getLanternRequestsFor", args = {UUID.class}, ret = List.class),
            at = @At("RETURN"), cancellable = true)
    private void echoWarrior$filterLanterns(UUID owner, CallbackInfoReturnable<List<?>> ci) {
        var original = ci.getReturnValue();
        var filtered = original.stream().filter(r -> !PetCollarCompat1201.isEchoRequest(r)).toList();
        if (filtered.size() != original.size()) ci.setReturnValue(new java.util.ArrayList<>(filtered));
    }
}
