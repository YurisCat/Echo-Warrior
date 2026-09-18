package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;

/** Releases the mouse after automated Quick Play enters a world. */
public final class AutomatedTestPauseController {
	private static final boolean ENABLED = Boolean.getBoolean("echo_warrior.auto_pause_after_quick_play");
	private static boolean opened;

	private AutomatedTestPauseController() {
	}

	public static void tick(Minecraft client) {
		if (!ENABLED || opened || client.player == null || client.level == null || client.screen != null) return;
		client.setScreen(new PauseScreen(true));
		opened = true;
		EchoWarrior.LOGGER.info("Automated client smoke test opened the pause menu to release the mouse.");
	}
}
