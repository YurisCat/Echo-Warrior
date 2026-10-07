package com.yuriscat.echowarrior.compat.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EchoProgressionPayload1211(int maxLevel) implements CustomPacketPayload {
    public static final Type<EchoProgressionPayload1211> TYPE = new Type<>(com.yuriscat.echowarrior.compat.ModContent1211.id("progression"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EchoProgressionPayload1211> STREAM_CODEC =
            CustomPacketPayload.codec(EchoProgressionPayload1211::write, EchoProgressionPayload1211::new);
    private EchoProgressionPayload1211(RegistryFriendlyByteBuf input) { this(input.readVarInt()); }
    private void write(RegistryFriendlyByteBuf output) { output.writeVarInt(maxLevel); }
    public void apply() { com.yuriscat.echowarrior.compat.item.EchoProgressionConfig1211.receiveServerMaximum(maxLevel); }
    @Override public Type<EchoProgressionPayload1211> type() { return TYPE; }
}
