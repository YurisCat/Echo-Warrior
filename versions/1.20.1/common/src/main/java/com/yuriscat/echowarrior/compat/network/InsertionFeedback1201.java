package com.yuriscat.echowarrior.compat.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** Sent only after a successful server transaction; slot coordinates remain client-owned. */
public record InsertionFeedback1201(int menuId, int slot, ItemStack item) {
    public InsertionFeedback1201 { item = item.copyWithCount(1); }
    public void encode(FriendlyByteBuf buffer) { buffer.writeInt(menuId); buffer.writeInt(slot); buffer.writeItem(item); }
    public static InsertionFeedback1201 decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > 65536) throw new IllegalArgumentException("Oversized insertion feedback");
        var result = new InsertionFeedback1201(buffer.readInt(), buffer.readInt(), buffer.readItem());
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing feedback bytes");
        return result;
    }
}
