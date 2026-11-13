package com.yuriscat.echowarrior.integration.petmixin;
import com.yuriscat.echowarrior.integration.PetCollarCompat;
import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.server.item.NetItem", remap = false)
public abstract class PetHomeNetMixin {
    @Inject(target = @Desc(value = "canCatchMob", args = {Entity.class}, ret = boolean.class),
            at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$rejectSnapshotCapture(Entity entity, CallbackInfoReturnable<Boolean> ci) {
        if (entity instanceof EchoWarriorEntity echo) {
            PetCollarCompat.explainRestriction(echo.livingEntity());
            ci.setReturnValue(false);
        }
    }
    // Names verified in the pinned production JARs; this is a vanilla override on an optional target.
    @Inject(method = {"useOn"}, at = @At("HEAD"), cancellable = true)
    private void echoWarrior$keepLegacyCapturedItem(net.minecraft.world.item.context.UseOnContext context,
            CallbackInfoReturnable<net.minecraft.world.InteractionResult> ci) {
        if (PetCollarCompat.rejectStoredNet(context.getItemInHand(), context.getPlayer()))
            ci.setReturnValue(net.minecraft.world.InteractionResult.FAIL);
    }
}
