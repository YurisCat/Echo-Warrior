package com.yuriscat.echowarrior.compat.fabric;

import com.yuriscat.echowarrior.compat.menu.SummonerInsertion1201;
import com.yuriscat.echowarrior.compat.network.EchoNetwork1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertRequest1201;
import com.yuriscat.echowarrior.compat.network.InventoryNetwork1201;
import com.yuriscat.echowarrior.compat.network.CreativeInsertRequest1201;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class EchoNetworking1201Fabric {
    private EchoNetworking1201Fabric() {}
    public static void initialize() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var buffer = PacketByteBufs.create();
            new com.yuriscat.echowarrior.compat.network.EchoProgressionPacket1201(com.yuriscat.echowarrior.compat.item.EchoProgressionConfig1201.serverMaxLevel()).encode(buffer);
            ServerPlayNetworking.send(handler.player, com.yuriscat.echowarrior.compat.network.EchoProgressionPacket1201.ID, buffer);
        });
        ServerPlayNetworking.registerGlobalReceiver(InventoryNetwork1201.CREATIVE_DESTROY, (server, player, handler, buffer, sender) -> {
            var request = com.yuriscat.echowarrior.compat.network.CreativeDestructionRequest1201.decode(buffer);
            server.execute(() -> {
                if (!player.hasDisconnected()) com.yuriscat.echowarrior.compat.binding.CreativeSummonerDestroyTracker1201.requestCreativeTrash(player, request.ids());
            });
        });
        InventoryNetwork1201.feedbackSender = (player, packet) -> {
            var buffer = PacketByteBufs.create();
            packet.encode(buffer);
            ServerPlayNetworking.send(player, InventoryNetwork1201.FEEDBACK, buffer);
        };
        ServerPlayNetworking.registerGlobalReceiver(InventoryNetwork1201.CREATIVE_INSERT, (server, player, handler, buffer, sender) -> {
            var request = CreativeInsertRequest1201.decode(buffer);
            server.execute(() -> {
                if (player.hasDisconnected()) return;
                var reply = CreativeInsertRequest1201.handle(player, request);
                var response = PacketByteBufs.create();
                reply.encode(response);
                ServerPlayNetworking.send(player, InventoryNetwork1201.CREATIVE_REPLY, response);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(EchoNetwork1201.INSERT, (server, player, handler, buffer, sender) -> {
            // Decode on the network thread; do not retain a reference-counted buffer across enqueue.
            SummonerInsertRequest1201 request = SummonerInsertRequest1201.decode(buffer);
            server.execute(() -> {
                if (player.hasDisconnected()) return;
                var result = SummonerInsertion1201.handle(player, request);
                var response = PacketByteBufs.create();
                result.encode(response);
                ServerPlayNetworking.send(player, EchoNetwork1201.RESULT, response);
                player.inventoryMenu.broadcastChanges();
            });
        });
    }
}
