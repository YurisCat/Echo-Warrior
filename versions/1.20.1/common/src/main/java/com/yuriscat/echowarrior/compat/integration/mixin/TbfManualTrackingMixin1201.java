package com.yuriscat.echowarrior.compat.integration.mixin;

import com.yuriscat.echowarrior.compat.integration.TbfBridge1201;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The explicit command/item path must not re-enable physical-UUID auto tracking. */
@Pseudo
@Mixin(targets = "com.whidte.trulybestfriends.command.ModCommands", remap = false)
public abstract class TbfManualTrackingMixin1201 {
    @Inject(target = @Desc(value = "loadPet", args = {
            CommandSourceStack.class, ServerPlayer.class, Entity.class, boolean.class
    }, ret = int.class), at = @At("HEAD"), cancellable = true)
    private static void echoWarrior$trackExistingBinding(CommandSourceStack source, ServerPlayer player,
            Entity entity, boolean informAdmins, CallbackInfoReturnable<Integer> callback) {
        Integer result = TbfBridge1201.manualTrack(source, player, entity, informAdmins);
        if (result != null) callback.setReturnValue(result);
    }
}
