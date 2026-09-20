package com.yuriscat.echowarrior.compat.network;
import net.minecraft.network.FriendlyByteBuf;

public record CreativeInsertReply1201(int requestId, int inserted) {
    public void encode(FriendlyByteBuf buffer) { buffer.writeInt(requestId); buffer.writeInt(inserted); }
    public static CreativeInsertReply1201 decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() != 8) throw new IllegalArgumentException("Invalid creative reply size");
        int id = buffer.readInt(), inserted = buffer.readInt();
        if (inserted < 0 || inserted > 64) throw new IllegalArgumentException("Invalid inserted count");
        return new CreativeInsertReply1201(id, inserted);
    }
}
