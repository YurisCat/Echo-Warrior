package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.entity.JapaneseSamuraiEchoEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void echoWarrior$blockPinnedPlayerAttack(Entity target, CallbackInfo callback) {
		Player self = (Player)(Object)this;
		if (!self.level().isClientSide() && JapaneseSamuraiEchoEntity.isTemporarilyPinned(self)) {
			callback.cancel();
		}
	}
}
