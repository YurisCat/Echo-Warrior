package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.recycler.RecyclerSystem1201;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class RecyclerExplosionMixin1201 {
    @Shadow @Final private Level level;
    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void echoWarrior$keepSealedTransaction(boolean particles, CallbackInfo callback) {
        ((Explosion)(Object)this).getToBlow().removeIf(pos -> !RecyclerSystem1201.allowBreak(level.getBlockEntity(pos)));
    }
}
