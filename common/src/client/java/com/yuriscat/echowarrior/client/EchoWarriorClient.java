package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.item.EchoCompassItem;
import com.yuriscat.echowarrior.item.SummonerFuelInsertFeedback;
import com.yuriscat.echowarrior.item.TooltipShiftState;
import net.minecraft.client.Minecraft;

public final class EchoWarriorClient {
	private EchoWarriorClient() {
	}

	public static void initialize() {
		initializeState();
	}

	public static void initializeState() {
		EchoCompassItem.setClientInsideBattlefieldSupplier(EchoCompassClientState::isInsideBattlefieldMode);
		TooltipShiftState.setClientShiftDownSupplier(Minecraft.getInstance()::hasShiftDown);
		SummonerFuelInsertFeedback.setClientHandler((slot, feedback) -> {
			if (Minecraft.getInstance().screen instanceof SummonerFuelParticleHost host) {
				switch (feedback.effect()) {
					case FUEL -> host.echoWarrior$spawnFuelInsertionParticles(slot, feedback.item());
					case POLISH -> host.echoWarrior$spawnInsertionPolish(slot);
				}
			}
		});
	}

	public static void clearConnectionState() {
		EchoCompassClientState.clear();
		EchoCompassPulseHud.clear();
	}
}
