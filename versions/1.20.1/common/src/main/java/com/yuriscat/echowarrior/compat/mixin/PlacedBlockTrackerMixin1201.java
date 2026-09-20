package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.world.BattlefieldSystem1201;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class PlacedBlockTrackerMixin1201 {
    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void echoWarrior$trackSuccessfulPlacement(BlockPlaceContext context, BlockState state,
                                                      CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue() && context.getPlayer() != null && context.getLevel() instanceof ServerLevel level)
            BattlefieldSystem1201.markPlayerModified(level, context.getClickedPos());
    }
}
