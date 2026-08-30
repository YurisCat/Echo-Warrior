package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.item.EchoTalentSystem;
import com.yuriscat.echowarrior.item.EchoTrait;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fishing talent: virtual Luck of the Sea +1 and a 20% shorter lure wait. */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
	@Shadow @Final private int luck;
	@Shadow private int timeUntilLured;
	@Unique private int echoWarrior$previousLureWait;
	@Unique private float echoWarrior$lureAccelerationRemainder;

	@ModifyArg(
			method = "retrieve",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/storage/loot/LootParams$Builder;withLuck(F)Lnet/minecraft/world/level/storage/loot/LootParams$Builder;"
			)
	)
	private float echoWarrior$applyVirtualLuckOfTheSea(float originalLuck) {
		Player owner = ((FishingHook)(Object)this).getPlayerOwner();
		if (owner == null || this.luck >= 4 || !EchoTalentSystem.hasNearbyTalent(owner, EchoTrait.FISHING)) {
			return originalLuck;
		}
		return originalLuck + 1.0F;
	}

	@Inject(method = "catchingFish", at = @At("HEAD"))
	private void echoWarrior$captureFishingWait(BlockPos blockPos, CallbackInfo callback) {
		this.echoWarrior$previousLureWait = this.timeUntilLured;
	}

	@Inject(method = "catchingFish", at = @At("TAIL"))
	private void echoWarrior$shortenFishingWait(BlockPos blockPos, CallbackInfo callback) {
		Player owner = ((FishingHook)(Object)this).getPlayerOwner();
		if (owner == null || !EchoTalentSystem.hasNearbyTalent(owner, EchoTrait.FISHING)) {
			this.echoWarrior$lureAccelerationRemainder = 0.0F;
			return;
		}
		int normalDecrease = Math.max(0, this.echoWarrior$previousLureWait - this.timeUntilLured);
		if (normalDecrease <= 0 || this.timeUntilLured <= 0) return;
		this.echoWarrior$lureAccelerationRemainder += normalDecrease * 0.25F;
		int extraDecrease = (int)Math.floor(this.echoWarrior$lureAccelerationRemainder);
		this.echoWarrior$lureAccelerationRemainder -= extraDecrease;
		this.timeUntilLured = Math.max(0, this.timeUntilLured - extraDecrease);
	}
}
