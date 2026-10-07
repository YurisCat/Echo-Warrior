package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;

/** Releases the mouse after automated Quick Play enters a world. */
public final class AutomatedTestPauseController {
	private static final boolean ENABLED = Boolean.getBoolean("echo_warrior.auto_pause_after_quick_play");
	private static boolean opened;
	private static java.util.concurrent.CompletableFuture<Void> growthCheck;

	private AutomatedTestPauseController() {
	}

	public static void tick(Minecraft client) {
		if (!ENABLED || opened || client.player == null || client.level == null || client.screen != null) return;
		if (client.getSingleplayerServer() != null) {
			if (growthCheck == null) {
				var server = client.getSingleplayerServer();
				growthCheck = java.util.concurrent.CompletableFuture.runAsync(
						() -> com.yuriscat.echowarrior.item.EchoProgressionSelfTest.run(server.overworld()), server);
				return;
			}
			if (!growthCheck.isDone()) return;
			growthCheck.join();
		}
		GuandaoPresentationClientSelfTest.run(client);
		client.setScreen(new PauseScreen(true));
		opened = true;
		EchoWarrior.LOGGER.info("Automated client smoke test opened the pause menu to release the mouse.");
	}
}
