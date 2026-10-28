package com.yuriscat.echowarrior.compat.integration.petmixin;
import com.yuriscat.echowarrior.compat.integration.PetCollarCompat1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
@Pseudo
@Mixin(targets = "com.github.yzqdev.pethome.server.event.PlayerInteractEntityHandler", remap = false)
public abstract class PetHomeFabricInteractionGuardMixin1211 {
    @Inject(target = @Desc(value = "onInteractWithEntity", args = {Player.class, InteractionHand.class, Entity.class}, ret = InteractionResult.class),
            at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$protectControl(Player player, InteractionHand hand, Entity entity, CallbackInfoReturnable<InteractionResult> ci) {
        if (PetCollarCompat1211.rejectInteraction(player, entity, player.getItemInHand(hand)))
            ci.setReturnValue(InteractionResult.FAIL);
    }
}
