package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.binding.EchoBindingSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityItemTerminalMixin {
	@Inject(method = "onBelowWorld", at = @At("HEAD"))
	private void echoWarrior$destroySummonerBelowWorld(CallbackInfo ci) {
		if ((Object)this instanceof ItemEntity item && item.level() instanceof ServerLevel level) {
			EchoBindingSystem.destroySummonersIn(level.getServer(), item.getItem(), "item_voided");
		}
	}

	@Inject(method = "kill", at = @At("HEAD"))
	private void echoWarrior$destroyKilledSummoner(ServerLevel level, CallbackInfo ci) {
		if ((Object)this instanceof ItemEntity item) {
			EchoBindingSystem.destroySummonersIn(level.getServer(), item.getItem(), "item_killed");
		}
	}
}
