package com.yuriscat.echowarrior.compat.network;

import net.minecraft.network.FriendlyByteBuf;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

/** Candidates are not authority: the server must independently prove creative removal and absence. */
public record CreativeDestructionRequest1201(List<UUID> ids) {
    public static final int MAX_IDS = 1024;
    public CreativeDestructionRequest1201 {
        ids = List.copyOf(ids);
        if (ids.size() > MAX_IDS) throw new IllegalArgumentException("Too many destruction candidates");
    }
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(ids.size());
        ids.forEach(buffer::writeUUID);
    }
    public static CreativeDestructionRequest1201 decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        if (size < 0 || size > MAX_IDS || buffer.readableBytes() != size * 16) throw new IllegalArgumentException("Invalid destruction request length");
        var ids = new ArrayList<UUID>(size);
        for (int index = 0; index < size; index++) ids.add(buffer.readUUID());
        return new CreativeDestructionRequest1201(ids);
    }
}
