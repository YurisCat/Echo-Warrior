package com.yuriscat.echowarrior.compat.integration.petmixin;

import com.yuriscat.echowarrior.compat.integration.PetCollarCompat1201;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexthe666.citadel.server.entity.CitadelEntityData", remap = false)
public abstract class DomesticationDataMixin1201 {
    @Inject(target = @Desc(value = "setCitadelTag", args = {LivingEntity.class, CompoundTag.class}), at = @At("RETURN"))
    private static void echoWarrior$saveCollar(LivingEntity entity, CompoundTag tag, CallbackInfo ci) {
        PetCollarCompat1201.capture(entity);
    }
}
