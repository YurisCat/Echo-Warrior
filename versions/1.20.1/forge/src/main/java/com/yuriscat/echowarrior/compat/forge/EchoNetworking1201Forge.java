package com.yuriscat.echowarrior.compat.forge;

import com.yuriscat.echowarrior.compat.menu.SummonerInsertion1201;
import com.yuriscat.echowarrior.compat.network.EchoNetwork1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertRequest1201;
import com.yuriscat.echowarrior.compat.network.SummonerInsertResult1201;
import com.yuriscat.echowarrior.compat.network.InventoryNetwork1201;
import com.yuriscat.echowarrior.compat.network.CreativeInsertRequest1201;
import com.yuriscat.echowarrior.compat.network.CreativeInsertReply1201;
import com.yuriscat.echowarrior.compat.network.InsertionFeedback1201;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EchoNetworking1201Forge {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            EchoNetwork1201.INSERT, () -> "1", "1"::equals, "1"::equals);
    private EchoNetworking1201Forge() {}

    public static void initialize() {
        CHANNEL.messageBuilder(com.yuriscat.echowarrior.compat.network.CreativeDestructionRequest1201.class, 5, NetworkDirection.PLAY_TO_SERVER)
                .encoder(com.yuriscat.echowarrior.compat.network.CreativeDestructionRequest1201::encode)
                .decoder(com.yuriscat.echowarrior.compat.network.CreativeDestructionRequest1201::decode)
                .consumerMainThread((request, context) -> {
                    var player = context.get().getSender();
                    if (player != null && !player.hasDisconnected())
                        com.yuriscat.echowarrior.compat.binding.CreativeSummonerDestroyTracker1201.requestCreativeTrash(player, request.ids());
                }).add();
        InventoryNetwork1201.feedbackSender = (player, packet) -> CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        CHANNEL.messageBuilder(CreativeInsertRequest1201.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CreativeInsertRequest1201::encode).decoder(CreativeInsertRequest1201::decode)
                .consumerMainThread((request, context) -> {
                    var player = context.get().getSender();
                    if (player == null || player.hasDisconnected()) return;
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), CreativeInsertRequest1201.handle(player, request));
                }).add();
        CHANNEL.messageBuilder(CreativeInsertReply1201.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CreativeInsertReply1201::encode).decoder(CreativeInsertReply1201::decode)
                .consumerMainThread((reply, context) -> InventoryNetwork1201.creativeReceiver.accept(reply)).add();
        CHANNEL.messageBuilder(InsertionFeedback1201.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(InsertionFeedback1201::encode).decoder(InsertionFeedback1201::decode)
                .consumerMainThread((reply, context) -> InventoryNetwork1201.feedbackReceiver.accept(reply)).add();
        CHANNEL.messageBuilder(SummonerInsertRequest1201.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SummonerInsertRequest1201::encode).decoder(SummonerInsertRequest1201::decode)
                .consumerMainThread((request, context) -> {
                    var player = context.get().getSender();
                    if (player == null || player.hasDisconnected()) return;
                    var result = SummonerInsertion1201.handle(player, request);
                    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), result);
                    player.inventoryMenu.broadcastChanges();
                }).add();
        CHANNEL.messageBuilder(SummonerInsertResult1201.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SummonerInsertResult1201::encode).decoder(SummonerInsertResult1201::decode)
                .consumerMainThread((result, context) -> EchoNetwork1201.receiveOnClientThread(result)).add();
    }
}
