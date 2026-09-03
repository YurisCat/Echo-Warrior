package com.yuriscat.echowarrior.fabric;

import com.mojang.serialization.Codec;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.platform.PlatformServices;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.chunk.LevelChunk;

import java.nio.file.Path;

final class FabricPlatformServices implements PlatformServices.ServerBridge {
	private static final AttachmentType<Boolean> PLAYER_MODIFIED = AttachmentRegistry.createPersistent(
			EchoWarrior.id("player_modified_chunk"), Codec.BOOL);

	@Override
	public Path configDirectory() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		if (!ServerPlayNetworking.canSend(player, payload.type())) return false;
		ServerPlayNetworking.send(player, payload);
		return true;
	}

	@Override
	public void openIntMenu(
			ServerPlayer player,
			Component title,
			PlatformServices.MenuFactory factory,
			int data
	) {
		player.openMenu(new ExtendedMenuProvider<Integer>() {
			@Override
			public Integer getScreenOpeningData(ServerPlayer openingPlayer) {
				return data;
			}

			@Override
			public Component getDisplayName() {
				return title;
			}

			@Override
			public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player openingPlayer) {
				return factory.create(containerId, inventory);
			}
		});
	}

	@Override
	public void markPlayerModified(LevelChunk chunk) {
		chunk.setAttached(PLAYER_MODIFIED, true);
	}

	@Override
	public boolean isPlayerModified(LevelChunk chunk) {
		return chunk.getAttachedOrElse(PLAYER_MODIFIED, false);
	}
}
