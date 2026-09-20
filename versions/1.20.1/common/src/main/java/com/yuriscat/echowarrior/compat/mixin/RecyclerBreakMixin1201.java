package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.recycler.RecyclerSystem1201;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class RecyclerBreakMixin1201 {
    @Shadow protected ServerLevel level;
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$keepSealedTransaction(BlockPos pos, CallbackInfoReturnable<Boolean> result) {
        if (!RecyclerSystem1201.allowBreak(level.getBlockEntity(pos))) result.setReturnValue(false);
    }
}
