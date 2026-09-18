package com.yuriscat.echowarrior.compat.client;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class CreativeSummonerInsertionSender1211 {
    private static BiConsumer<Integer, ItemStack> sender = (slot, carried) -> {
        throw new IllegalStateException("Creative summoner insertion network sender is not registered");
    };

    private CreativeSummonerInsertionSender1211() {
    }

    public static void setSender(BiConsumer<Integer, ItemStack> sender) {
        CreativeSummonerInsertionSender1211.sender = Objects.requireNonNull(sender);
    }

    public static void send(int inventoryMenuSlot, ItemStack carried) {
        sender.accept(inventoryMenuSlot, carried.copy());
    }
}
