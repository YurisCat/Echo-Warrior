package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.binding.EchoBindingSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityLifecycleMixin {
	@Inject(method = "tick", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V", ordinal = 1))
	private void echoWarrior$detectNaturalDespawn(CallbackInfo ci) {
		ItemEntity self = (ItemEntity)(Object)this;
		if (self.level() instanceof ServerLevel level) {
			EchoBindingSystem.destroySummonersIn(level.getServer(), self.getItem(), "item_despawned");
		}
	}
}
