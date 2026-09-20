package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1201;
import com.yuriscat.echowarrior.compat.item.SummonerStackContents1201;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Expiry / void removal does not call Item.onDestroyed in 1.20.1. */
@Mixin(ItemEntity.class)
public abstract class ItemEntityLifetimeMixin1201 {
    @Inject(method = "tick", at = @At("RETURN"))
    private void echoWarrior$expiredSummoner(CallbackInfo callback) {
        var entity = (ItemEntity)(Object)this;
        if (!(entity.level() instanceof ServerLevel level) || entity.getRemovalReason() != Entity.RemovalReason.DISCARDED
                || entity.getItem().isEmpty()) return;
        // Pickup and merging empty the source; unload is a different removal reason.
        var contents = SummonerStackContents1201.scan(entity.getItem());
        if (contents.complete()) contents.ids().forEach(id -> EchoBindingSystem1201.destroySummoner(level, id));
    }
}
