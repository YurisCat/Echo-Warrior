package com.yuriscat.echowarrior.item;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Dedicated-server-safe bridge for client-only inventory feedback.
 */
public final class SummonerFuelInsertFeedback {
	private static BiConsumer<Slot, Feedback> clientHandler = (slot, feedback) -> {
	};

	private SummonerFuelInsertFeedback() {
	}

	public static void setClientHandler(BiConsumer<Slot, Feedback> handler) {
		clientHandler = Objects.requireNonNull(handler);
	}

	public static void playFuel(Slot slot, ItemStack fuel) {
		clientHandler.accept(slot, new Feedback(Effect.FUEL, fuel.copyWithCount(1)));
	}

	public static void playPolish(Slot slot) {
		clientHandler.accept(slot, new Feedback(Effect.POLISH, ItemStack.EMPTY));
	}

	public enum Effect {
		FUEL,
		POLISH
	}

	public record Feedback(Effect effect, ItemStack item) {
	}
}
