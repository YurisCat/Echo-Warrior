package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.binding.EchoBindingSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemStackDestructionMixin {
	@Inject(method = "onDestroyed", at = @At("HEAD"))
	private void echoWarrior$destroyNestedSummoners(ItemEntity itemEntity, CallbackInfo ci) {
		if (itemEntity.level() instanceof ServerLevel level) {
			EchoBindingSystem.destroySummonersIn(level.getServer(), (ItemStack)(Object)this, "item_destroyed");
		}
	}
}
