package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1201;
import com.yuriscat.echowarrior.compat.item.EchoTrait1201;
import com.yuriscat.echowarrior.compat.item.TalentExperienceHolder1201;
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
public abstract class ExperienceOrbTalentMixin1201 {
    @Unique private ServerPlayer echoWarrior1201$touchingPlayer;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void echoWarrior1201$captureTouchingPlayer(Player player, CallbackInfo callback) {
        this.echoWarrior1201$touchingPlayer = player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    @ModifyArg(
            method = "playerTouch",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;repairPlayerItems(Lnet/minecraft/world/entity/player/Player;I)I"),
            index = 1
    )
    private int echoWarrior1201$applyWiseExperience(int amount) {
        ServerPlayer player = this.echoWarrior1201$touchingPlayer;
        if (player == null || !EchoTalentSystem1201.hasNearbyTalent(player, EchoTrait1201.WISE)) return amount;
        return TalentExperienceHolder1201.of(player).echoWarrior1201$consumeWiseBonus(amount);
    }

    @Inject(method = "playerTouch", at = @At("RETURN"))
    private void echoWarrior1201$clearTouchingPlayer(Player player, CallbackInfo callback) {
        this.echoWarrior1201$touchingPlayer = null;
    }
}
