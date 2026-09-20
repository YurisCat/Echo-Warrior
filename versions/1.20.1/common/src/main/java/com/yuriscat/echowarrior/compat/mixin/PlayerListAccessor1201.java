package com.yuriscat.echowarrior.compat.mixin;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Scoped packet-observer fixtures: Forge exposes getPlayers() as an immutable view. */
@Mixin(PlayerList.class)
public interface PlayerListAccessor1201 {
    @Accessor("players")
    List<ServerPlayer> echoWarrior1201$mutablePlayers();
}
