package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.world.BattlefieldSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
abstract class BlockBehaviourMixin {
	@Inject(method = "affectNeighborsAfterRemoval", at = @At("HEAD"))
	private void echoWarrior$trackBattlefieldRelicRemoval(
			BlockState state,
			ServerLevel level,
			BlockPos pos,
			boolean movedByPiston,
			CallbackInfo callbackInfo
	) {
		BattlefieldSystem.onBrushableRemoved(level, pos, state);
	}
}
