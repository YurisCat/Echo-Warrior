package com.yuriscat.echowarrior.compat.client;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class CreativeSummonerDestructionSender1211 {
    private static Consumer<List<UUID>> sender = summonerIds -> {
        throw new IllegalStateException("Creative summoner destruction network sender is not registered");
    };

    private CreativeSummonerDestructionSender1211() {
    }

    public static void setSender(Consumer<List<UUID>> sender) {
        CreativeSummonerDestructionSender1211.sender = Objects.requireNonNull(sender);
    }

    public static void send(List<UUID> summonerIds) {
        sender.accept(List.copyOf(summonerIds));
    }
}
