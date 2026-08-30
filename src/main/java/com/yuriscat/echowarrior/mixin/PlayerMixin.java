package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.entity.JapaneseSamuraiEchoEntity;
import com.yuriscat.echowarrior.item.TalentExperienceHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin implements TalentExperienceHolder {
	@Unique private float echoWarrior$wiseExperienceRemainder;
	@Unique private float echoWarrior$mentorExperienceRemainder;

	@Inject(method = "attack", at = @At("HEAD"), cancellable = true)
	private void echoWarrior$blockPinnedPlayerAttack(Entity target, CallbackInfo callback) {
		Player self = (Player)(Object)this;
		if (!self.level().isClientSide() && JapaneseSamuraiEchoEntity.isTemporarilyPinned(self)) {
			callback.cancel();
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void echoWarrior$saveTalentExperienceRemainders(ValueOutput output, CallbackInfo callback) {
		output.putFloat("EchoWarriorWiseExperienceRemainder", this.echoWarrior$wiseExperienceRemainder);
		output.putFloat("EchoWarriorMentorExperienceRemainder", this.echoWarrior$mentorExperienceRemainder);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void echoWarrior$loadTalentExperienceRemainders(ValueInput input, CallbackInfo callback) {
		this.echoWarrior$wiseExperienceRemainder = input.getFloatOr("EchoWarriorWiseExperienceRemainder", 0.0F);
		this.echoWarrior$mentorExperienceRemainder = input.getFloatOr("EchoWarriorMentorExperienceRemainder", 0.0F);
	}

	@Override
	public int echoWarrior$consumeWiseBonus(int baseAmount) {
		if (baseAmount <= 0) return Math.max(0, baseAmount);
		float exact = baseAmount * 0.25F + this.echoWarrior$wiseExperienceRemainder;
		int bonus = (int)Math.floor(exact);
		this.echoWarrior$wiseExperienceRemainder = exact - bonus;
		return baseAmount + bonus;
	}

	@Override
	public int echoWarrior$consumeMentorBonus(int baseAmount) {
		if (baseAmount <= 0) return 0;
		float exact = baseAmount * 0.50F + this.echoWarrior$mentorExperienceRemainder;
		int bonus = (int)Math.floor(exact);
		this.echoWarrior$mentorExperienceRemainder = exact - bonus;
		return bonus;
	}
}
