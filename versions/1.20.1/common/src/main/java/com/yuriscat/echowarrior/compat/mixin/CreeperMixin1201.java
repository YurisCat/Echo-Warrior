package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.entity.CatGodCreeperSystem1201;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperMixin1201 {
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_IS_IGNITED;
    @Shadow private int oldSwell;
    @Shadow private int swell;

    @Inject(method = "tick", at = @At("HEAD"))
    private void echoWarrior1201$cancelIgnition(CallbackInfo callback) {
        Creeper creeper = (Creeper)(Object)this;
        if (CatGodCreeperSystem1201.shouldSuppressIgnition(creeper)) clearFuse(creeper);
    }

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void echoWarrior1201$protectCatGodAura(CallbackInfo callback) {
        Creeper creeper = (Creeper)(Object)this;
        if (CatGodCreeperSystem1201.shouldCancelExplosion(creeper)) {
            clearFuse(creeper);
            callback.cancel();
        }
    }

    private void clearFuse(Creeper creeper) {
        oldSwell = 0;
        swell = 0;
        creeper.setSwellDir(-1);
        creeper.getEntityData().set(DATA_IS_IGNITED, false);
    }
}
