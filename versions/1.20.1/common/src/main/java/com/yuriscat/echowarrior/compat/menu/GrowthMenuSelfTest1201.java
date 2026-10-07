package com.yuriscat.echowarrior.compat.menu;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.world.inventory.DataSlot;
import java.util.ArrayList;

/** Real vanilla/loader packet round-trip, including the signed-short boundary. */
public final class GrowthMenuSelfTest1201 {
    public static void run() {
        var source = new WideDataSlot1201();
        var target = new WideDataSlot1201();
        var sending = new ArrayList<DataSlot>();
        var receiving = new ArrayList<DataSlot>();
        source.register(sending::add); target.register(receiving::add);
        for (int value : new int[]{0, 100, 5000, 32767, 32768, 65535, 65536, 1000000, Integer.MAX_VALUE}) {
            source.set(value);
            for (int i=0; i<sending.size(); i++) {
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    var packet = new ClientboundContainerSetDataPacket(1, i, sending.get(i).get());
                    packet.write(buffer);
                    var decoded = new ClientboundContainerSetDataPacket(buffer);
                    receiving.get(decoded.getId()).set(decoded.getValue());
                } finally { buffer.release(); }
            }
            if (target.get()!=value) throw new IllegalStateException("Growth menu packet truncated " + value + " to " + target.get());
        }
    }
}
