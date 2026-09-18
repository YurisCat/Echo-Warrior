package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1211;
import com.yuriscat.echowarrior.compat.item.TalentExperienceHolder1211;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerTalentMixin1211 implements TalentExperienceHolder1211 {
    @Unique private float echoWarrior1211$wiseExperienceRemainder;
    @Unique private float echoWarrior1211$mentorExperienceRemainder;

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void echoWarrior1211$blockPinnedPlayerAttack(Entity target, CallbackInfo callback) {
        Player self = (Player)(Object)this;
        if (!self.level().isClientSide() && JapaneseSamuraiEchoEntity1211.isTemporarilyPinned(self)) {
            callback.cancel();
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void echoWarrior1211$saveTalentExperienceRemainders(CompoundTag tag, CallbackInfo callback) {
        tag.putFloat("EchoWarriorWiseExperienceRemainder", this.echoWarrior1211$wiseExperienceRemainder);
        tag.putFloat("EchoWarriorMentorExperienceRemainder", this.echoWarrior1211$mentorExperienceRemainder);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void echoWarrior1211$loadTalentExperienceRemainders(CompoundTag tag, CallbackInfo callback) {
        this.echoWarrior1211$wiseExperienceRemainder = tag.getFloat("EchoWarriorWiseExperienceRemainder");
        this.echoWarrior1211$mentorExperienceRemainder = tag.getFloat("EchoWarriorMentorExperienceRemainder");
    }

    @Override
    public int echoWarrior1211$consumeWiseBonus(int baseAmount) {
        if (baseAmount <= 0) return Math.max(0, baseAmount);
        float exact = baseAmount * 0.25F + this.echoWarrior1211$wiseExperienceRemainder;
        int bonus = (int)Math.floor(exact);
        this.echoWarrior1211$wiseExperienceRemainder = exact - bonus;
        return baseAmount + bonus;
    }

    @Override
    public int echoWarrior1211$consumeMentorBonus(int baseAmount) {
        if (baseAmount <= 0) return 0;
        float exact = baseAmount * 0.50F + this.echoWarrior1211$mentorExperienceRemainder;
        int bonus = (int)Math.floor(exact);
        this.echoWarrior1211$mentorExperienceRemainder = exact - bonus;
        return bonus;
    }
}
