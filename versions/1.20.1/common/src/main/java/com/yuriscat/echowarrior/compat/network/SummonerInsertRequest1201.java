package com.yuriscat.echowarrior.compat.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.UUID;

/** Inventory indices, not GUI slot numbers. No client ItemStack/NBT is accepted as authority. */
public record SummonerInsertRequest1201(int requestId, int menuId, int summonerSlot, int sourceSlot,
                                       UUID summonerId, long expectedRevision, int count) {
    public static final int WIRE_BYTES = 44;

    public static SummonerInsertRequest1201 decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != WIRE_BYTES) throw new IllegalArgumentException("Invalid summoner request size");
        return new SummonerInsertRequest1201(buffer.readInt(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                buffer.readUUID(), buffer.readLong(), buffer.readInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(requestId).writeInt(menuId).writeInt(summonerSlot).writeInt(sourceSlot);
        buffer.writeUUID(summonerId).writeLong(expectedRevision).writeInt(count);
    }
}
