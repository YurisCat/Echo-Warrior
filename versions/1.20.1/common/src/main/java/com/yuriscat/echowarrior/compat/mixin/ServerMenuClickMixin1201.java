package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.menu.SummonerMenu1201;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerMenuClickMixin1201 {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void echoWarrior$rejectStaleClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        // Let vanilla PacketUtils enqueue first. Never touch inventories from the Netty thread.
        if (!player.server.isSameThread()) return;
        if (player.containerMenu instanceof SummonerMenu1201 menu
                && packet.getContainerId() == menu.containerId && !menu.acceptRemoteState(packet.getStateId())) {
            menu.rejectRemoteClick();
            ci.cancel();
        }
    }
}
