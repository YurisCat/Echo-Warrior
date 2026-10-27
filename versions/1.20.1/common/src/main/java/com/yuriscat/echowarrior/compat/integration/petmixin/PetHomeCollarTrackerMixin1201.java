package com.yuriscat.echowarrior.compat.integration.petmixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/** Only selected for the old keySet/remove implementation, never the corrected iterator version. */
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.server.misc.CollarTickTracker", remap = false)
public abstract class PetHomeCollarTrackerMixin1201 {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;"))
    private Set<UUID> echoWarrior$stableExpirationIteration(Set<UUID> keys) { return new HashSet<>(keys); }
}
