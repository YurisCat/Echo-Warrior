package com.yuriscat.echowarrior.neoforge;

import com.yuriscat.echowarrior.platform.PlatformServices;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

final class NeoForgePlatformServices implements PlatformServices.ServerBridge {
	@Override
	public Path configDirectory() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		if (!player.connection.hasChannel(payload.type())) return false;
		PacketDistributor.sendToPlayer(player, payload);
		return true;
	}

	@Override
	public void openIntMenu(ServerPlayer player, Component title, PlatformServices.MenuFactory factory, int data) {
		player.openMenu(
				new SimpleMenuProvider(
						(containerId, inventory, ignored) -> factory.create(containerId, inventory),
						title
				),
				buffer -> buffer.writeVarInt(data)
		);
	}

	@Override
	public void markPlayerModified(LevelChunk chunk) {
		chunk.setData(EchoWarriorNeoForge.PLAYER_MODIFIED_CHUNK, true);
		chunk.markUnsaved();
	}

	@Override
	public boolean isPlayerModified(LevelChunk chunk) {
		return chunk.getData(EchoWarriorNeoForge.PLAYER_MODIFIED_CHUNK);
	}
}
