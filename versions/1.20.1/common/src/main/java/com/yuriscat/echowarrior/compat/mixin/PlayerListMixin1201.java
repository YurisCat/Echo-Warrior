package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.test.ClientTestProtection1201;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin1201 {
    @Inject(method = "remove", at = @At("HEAD"))
    private void echoWarrior$restoreTestFixture(ServerPlayer player, CallbackInfo callback) {
        // Logout saves the player before stopServer: restore fixture flags before that earlier save.
        com.yuriscat.echowarrior.compat.test.HeroClientFixture1201.restoreBeforeLogout(player);
        ClientTestProtection1201.restoreBeforeLogout(player);
        com.yuriscat.echowarrior.compat.binding.CreativeSummonerDestroyTracker1201.clearPlayer(player.getUUID());
    }
}
