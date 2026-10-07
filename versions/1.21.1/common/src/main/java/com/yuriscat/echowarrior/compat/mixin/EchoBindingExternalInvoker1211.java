package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1211;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the existing transaction only. No replacement of identity, storage or spawn logic. */
@Mixin(value = EchoBindingSystem1211.class, remap = false)
public interface EchoBindingExternalInvoker1211 {
    @Invoker("spawnAndActivate")
    static EchoBindingSystem1211.SpawnAttempt echoWarrior$spawn(ServerLevel level, Player controller,
            ItemStack transientMirror, EchoBindingSavedData1211.Binding binding, float restoredHealth) {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
