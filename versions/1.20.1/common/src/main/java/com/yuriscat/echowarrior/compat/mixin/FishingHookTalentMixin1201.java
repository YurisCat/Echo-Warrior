package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1201;
import com.yuriscat.echowarrior.compat.item.EchoTrait1201;
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

@Mixin(FishingHook.class)
public abstract class FishingHookTalentMixin1201 {
    @Shadow @Final private int luck;
    @Shadow private int timeUntilLured;
    @Unique private int echoWarrior1201$previousLureWait;
    @Unique private float echoWarrior1201$lureAccelerationRemainder;

    @ModifyArg(method = "retrieve", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootParams$Builder;withLuck(F)Lnet/minecraft/world/level/storage/loot/LootParams$Builder;"))
    private float echoWarrior1201$applyVirtualLuck(float originalLuck) {
        Player owner = ((FishingHook)(Object)this).getPlayerOwner();
        return owner != null && this.luck < 4
                && EchoTalentSystem1201.hasNearbyTalent(owner, EchoTrait1201.FISHING)
                ? originalLuck + 1.0F : originalLuck;
    }

    @Inject(method = "catchingFish", at = @At("HEAD"))
    private void echoWarrior1201$captureWait(BlockPos pos, CallbackInfo callback) {
        this.echoWarrior1201$previousLureWait = this.timeUntilLured;
    }

    @Inject(method = "catchingFish", at = @At("TAIL"))
    private void echoWarrior1201$shortenWait(BlockPos pos, CallbackInfo callback) {
        Player owner = ((FishingHook)(Object)this).getPlayerOwner();
        if (owner == null || !EchoTalentSystem1201.hasNearbyTalent(owner, EchoTrait1201.FISHING)) {
            this.echoWarrior1201$lureAccelerationRemainder = 0.0F;
            return;
        }
        int normalDecrease = Math.max(0, this.echoWarrior1201$previousLureWait - this.timeUntilLured);
        if (normalDecrease <= 0 || this.timeUntilLured <= 0) return;
        this.echoWarrior1201$lureAccelerationRemainder += normalDecrease * 0.25F;
        int extra = (int)Math.floor(this.echoWarrior1201$lureAccelerationRemainder);
        this.echoWarrior1201$lureAccelerationRemainder -= extra;
        this.timeUntilLured = Math.max(0, this.timeUntilLured - extra);
    }
}
