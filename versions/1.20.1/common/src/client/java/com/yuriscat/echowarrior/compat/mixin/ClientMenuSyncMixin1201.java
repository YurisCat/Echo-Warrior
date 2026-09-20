package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.menu.SummonerMenu1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientMenuSyncMixin1201 {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void echoWarrior$acknowledgeCursorState(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        // 1.20.1 increments stateId for cursor packets server-side, but vanilla only applies the stack
        // client-side. A strict stale-click guard otherwise rejects every click after a withdrawal.
        if (packet.getContainerId() == -1 && minecraft.player != null
                && minecraft.player.containerMenu instanceof SummonerMenu1201 menu) {
            menu.initializeContents(packet.getStateId(), menu.getItems(), packet.getItem());
        }
    }
}
