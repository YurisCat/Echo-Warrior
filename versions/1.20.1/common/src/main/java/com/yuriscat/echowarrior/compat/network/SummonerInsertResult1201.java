package com.yuriscat.echowarrior.compat.network;

import net.minecraft.network.FriendlyByteBuf;

public record SummonerInsertResult1201(int requestId, Outcome outcome, int inserted, long revision) {
    public static final int WIRE_BYTES = 20;

    // Protocol v1: append only, or change the channel protocol version.
    public enum Outcome { INSERTED, INVALID_REQUEST, PLAYER_UNAVAILABLE, WRONG_MENU, WRONG_SUMMONER,
        NOT_REGISTERED, STALE_REVISION, INVALID_SOURCE, UNSUPPORTED_ITEM, NO_SPACE }

    public static SummonerInsertResult1201 decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != WIRE_BYTES) throw new IllegalArgumentException("Invalid summoner response size");
        int request = buffer.readInt();
        int outcome = buffer.readInt();
        int inserted = buffer.readInt();
        long revision = buffer.readLong();
        if (outcome < 0 || outcome >= Outcome.values().length || inserted < 0 || inserted > 64 || revision < -1) {
            throw new IllegalArgumentException("Invalid summoner response fields");
        }
        return new SummonerInsertResult1201(request, Outcome.values()[outcome], inserted, revision);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(requestId).writeInt(outcome.ordinal()).writeInt(inserted).writeLong(revision);
    }
}
