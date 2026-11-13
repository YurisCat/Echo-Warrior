package com.yuriscat.echowarrior.platform;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.chunk.LevelChunk;

import java.nio.file.Path;

public final class PlatformServices {
	private static ServerBridge bridge;

	private PlatformServices() {
	}

	public static void install(ServerBridge serverBridge) {
		if (bridge != null) throw new IllegalStateException("Echo Warrior platform services were already installed");
		bridge = serverBridge;
	}

	public static Path configDirectory() {
		return bridge().configDirectory();
	}

	public static boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		return bridge().sendToPlayer(player, payload);
	}

	public static void openIntMenu(
			ServerPlayer player,
			Component title,
			MenuFactory factory,
			int data
	) {
		bridge().openIntMenu(player, title, factory, data);
	}

	public static void markPlayerModified(LevelChunk chunk) {
		bridge().markPlayerModified(chunk);
	}

	public static boolean isPlayerModified(LevelChunk chunk) {
		return bridge().isPlayerModified(chunk);
	}

	private static ServerBridge bridge() {
		if (bridge == null) throw new IllegalStateException("Echo Warrior platform services are not installed");
		return bridge;
	}

	@FunctionalInterface
	public interface MenuFactory {
		AbstractContainerMenu create(int containerId, Inventory inventory);
	}

	public interface ServerBridge {
		Path configDirectory();

		boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

		void openIntMenu(ServerPlayer player, Component title, MenuFactory factory, int data);

		void markPlayerModified(LevelChunk chunk);

		boolean isPlayerModified(LevelChunk chunk);
	}
}
