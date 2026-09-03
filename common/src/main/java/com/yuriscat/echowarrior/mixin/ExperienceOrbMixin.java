package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.item.EchoTalentSystem;
import com.yuriscat.echowarrior.item.EchoTrait;
import com.yuriscat.echowarrior.item.TalentExperienceHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
	@Unique private ServerPlayer echoWarrior$touchingPlayer;

	@Inject(method = "playerTouch", at = @At("HEAD"))
	private void echoWarrior$captureTouchingPlayer(Player player, CallbackInfo callback) {
		this.echoWarrior$touchingPlayer = player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
	}

	@ModifyArg(
			method = "playerTouch",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;repairPlayerItems(Lnet/minecraft/server/level/ServerPlayer;I)I"),
			index = 1
	)
	private int echoWarrior$applyWiseExperience(int amount) {
		ServerPlayer player = this.echoWarrior$touchingPlayer;
		if (player == null || !EchoTalentSystem.hasNearbyTalent(player, EchoTrait.WISE)) return amount;
		return TalentExperienceHolder.of(player).echoWarrior$consumeWiseBonus(amount);
	}

	@Inject(method = "playerTouch", at = @At("RETURN"))
	private void echoWarrior$clearTouchingPlayer(Player player, CallbackInfo callback) {
		this.echoWarrior$touchingPlayer = null;
	}
}
