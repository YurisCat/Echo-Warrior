package com.yuriscat.echowarrior.compat.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record EchoProgressionPacket1201(int maxLevel) {
    public static final ResourceLocation ID = new ResourceLocation("echo_warrior", "progression");
    public void encode(FriendlyByteBuf buffer) { buffer.writeVarInt(maxLevel); }
    public static EchoProgressionPacket1201 decode(FriendlyByteBuf buffer) { return new EchoProgressionPacket1201(buffer.readVarInt()); }
    public void apply() { com.yuriscat.echowarrior.compat.item.EchoProgressionConfig1201.receiveServerMaximum(maxLevel); }
}
