package com.yuriscat.echowarrior.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class ClientPlatformServices {
	private static ClientBridge bridge;

	private ClientPlatformServices() {
	}

	public static void install(ClientBridge clientBridge) {
		if (bridge != null) throw new IllegalStateException("Echo Warrior client platform services were already installed");
		bridge = clientBridge;
	}

	public static boolean sendToServer(CustomPacketPayload payload) {
		return bridge != null && bridge.sendToServer(payload);
	}

	@FunctionalInterface
	public interface ClientBridge {
		boolean sendToServer(CustomPacketPayload payload);
	}
}
