package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.entity.CatGodCreeperSystem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperMixin {
	@Shadow @Final private static EntityDataAccessor<Boolean> DATA_IS_IGNITED;
	@Shadow private int oldSwell;
	@Shadow private int swell;

	@Inject(method = "tick", at = @At("HEAD"))
	private void echoWarrior$cancelIgnition(CallbackInfo callback) {
		Creeper creeper = (Creeper)(Object)this;
		if (CatGodCreeperSystem.shouldSuppressIgnition(creeper)) clearFuse(creeper);
	}

	@Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
	private void echoWarrior$protectCatGodAura(CallbackInfo callback) {
		Creeper creeper = (Creeper)(Object)this;
		if (CatGodCreeperSystem.shouldCancelExplosion(creeper)) {
			clearFuse(creeper);
			callback.cancel();
		}
	}

	private void clearFuse(Creeper creeper) {
		this.oldSwell = 0;
		this.swell = 0;
		creeper.setSwellDir(-1);
		creeper.getEntityData().set(DATA_IS_IGNITED, false);
	}
}
