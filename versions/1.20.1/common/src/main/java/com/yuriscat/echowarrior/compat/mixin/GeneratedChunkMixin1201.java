package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.world.GeneratedChunk1201;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class GeneratedChunkMixin1201 implements GeneratedChunk1201 {
    @Unique private boolean echoWarrior$justGenerated;

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At("RETURN"))
    private void echoWarrior$recordGeneration(CallbackInfo ci) { echoWarrior$justGenerated = true; }

    @Override public boolean echoWarrior$wasJustGenerated() { return echoWarrior$justGenerated; }
}
