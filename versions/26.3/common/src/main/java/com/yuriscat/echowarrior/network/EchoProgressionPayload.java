package com.yuriscat.echowarrior.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EchoProgressionPayload(int maxLevel) implements CustomPacketPayload {
    public static final Type<EchoProgressionPayload> TYPE = new Type<>(com.yuriscat.echowarrior.EchoWarrior.id("progression"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EchoProgressionPayload> STREAM_CODEC =
            CustomPacketPayload.codec(EchoProgressionPayload::write, EchoProgressionPayload::new);
    private EchoProgressionPayload(RegistryFriendlyByteBuf input) { this(input.readVarInt()); }
    private void write(RegistryFriendlyByteBuf output) { output.writeVarInt(maxLevel); }
    public void apply() { com.yuriscat.echowarrior.item.EchoProgressionConfig.receiveServerMaximum(maxLevel); }
    @Override public Type<EchoProgressionPayload> type() { return TYPE; }
}
