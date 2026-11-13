package com.yuriscat.echowarrior.integration.petmixin;

import com.yuriscat.echowarrior.integration.PetCollarCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.util.CitadelEntityData", remap = false)
public abstract class PetHomeDataMixin {
    @Inject(target = @Desc(value = "setCitadelTag", args = {LivingEntity.class, CompoundTag.class}), at = @At("RETURN"))
    private static void echoWarrior$saveCollar(LivingEntity entity, CompoundTag tag, CallbackInfo ci) {
        PetCollarCompat.capture(entity);
    }
}
