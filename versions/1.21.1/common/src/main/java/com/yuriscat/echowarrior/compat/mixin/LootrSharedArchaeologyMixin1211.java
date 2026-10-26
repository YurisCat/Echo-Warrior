package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.integration.LootrArchaeology1211;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "noobanidus.mods.lootr.common.api.LootrAPI", remap = false)
public abstract class LootrSharedArchaeologyMixin1211 {
    @Inject(method = "isLootTableBlacklisted", at = @At("HEAD"), cancellable = true, require = 1)
    private static void echoWarrior$keepSharedBattlefieldLoot(ResourceKey<LootTable> table,
                                                              CallbackInfoReturnable<Boolean> callback) {
        if (LootrArchaeology1211.isSharedTable(table)) callback.setReturnValue(true);
    }
}
