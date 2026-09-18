package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1211;
import com.yuriscat.echowarrior.compat.item.EchoTrait1211;
import com.yuriscat.echowarrior.compat.item.TalentExperienceHolder1211;
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
public abstract class ExperienceOrbTalentMixin1211 {
    @Unique private ServerPlayer echoWarrior1211$touchingPlayer;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void echoWarrior1211$captureTouchingPlayer(Player player, CallbackInfo callback) {
        this.echoWarrior1211$touchingPlayer = player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    @ModifyArg(
            method = "playerTouch",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;repairPlayerItems(Lnet/minecraft/server/level/ServerPlayer;I)I"),
            index = 1
    )
    private int echoWarrior1211$applyWiseExperience(int amount) {
        ServerPlayer player = this.echoWarrior1211$touchingPlayer;
        if (player == null || !EchoTalentSystem1211.hasNearbyTalent(player, EchoTrait1211.WISE)) return amount;
        return TalentExperienceHolder1211.of(player).echoWarrior1211$consumeWiseBonus(amount);
    }

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void echoWarrior1211$clearTouchingPlayer(Player player, CallbackInfo callback) {
        this.echoWarrior1211$touchingPlayer = null;
    }
}
