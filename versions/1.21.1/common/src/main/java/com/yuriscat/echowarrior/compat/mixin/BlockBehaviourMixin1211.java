package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin1211 {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void echoWarrior$trackBattlefieldRemoval(BlockState state, Level level, BlockPos pos,
                                                    BlockState replacement, boolean movedByPiston, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel && !state.is(replacement.getBlock())) {
            BattlefieldSystem1211.onBrushableRemoved(serverLevel, pos, state, replacement);
        }
    }
}
