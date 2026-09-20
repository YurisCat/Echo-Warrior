package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1201;
import com.yuriscat.echowarrior.compat.item.TalentExperienceHolder1201;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerTalentMixin1201 implements TalentExperienceHolder1201 {
    @Unique private float echoWarrior1201$wiseExperienceRemainder;
    @Unique private float echoWarrior1201$mentorExperienceRemainder;

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void echoWarrior1201$blockPinnedPlayerAttack(Entity target, CallbackInfo callback) {
        Player self = (Player)(Object)this;
        if (!self.level().isClientSide() && JapaneseSamuraiEchoEntity1201.isTemporarilyPinned(self)) {
            callback.cancel();
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void echoWarrior1201$saveTalentExperienceRemainders(CompoundTag tag, CallbackInfo callback) {
        tag.putFloat("EchoWarriorWiseExperienceRemainder", this.echoWarrior1201$wiseExperienceRemainder);
        tag.putFloat("EchoWarriorMentorExperienceRemainder", this.echoWarrior1201$mentorExperienceRemainder);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void echoWarrior1201$loadTalentExperienceRemainders(CompoundTag tag, CallbackInfo callback) {
        this.echoWarrior1201$wiseExperienceRemainder = tag.getFloat("EchoWarriorWiseExperienceRemainder");
        this.echoWarrior1201$mentorExperienceRemainder = tag.getFloat("EchoWarriorMentorExperienceRemainder");
    }

    @Override
    public int echoWarrior1201$consumeWiseBonus(int baseAmount) {
        if (baseAmount <= 0) return Math.max(0, baseAmount);
        float exact = baseAmount * 0.25F + this.echoWarrior1201$wiseExperienceRemainder;
        int bonus = (int)Math.floor(exact);
        this.echoWarrior1201$wiseExperienceRemainder = exact - bonus;
        return baseAmount + bonus;
    }

    @Override
    public int echoWarrior1201$consumeMentorBonus(int baseAmount) {
        if (baseAmount <= 0) return 0;
        float exact = baseAmount * 0.50F + this.echoWarrior1201$mentorExperienceRemainder;
        int bonus = (int)Math.floor(exact);
        this.echoWarrior1201$mentorExperienceRemainder = exact - bonus;
        return bonus;
    }
}
