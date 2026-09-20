package com.yuriscat.echowarrior.compat.network;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Consumer;

/** No Minecraft client classes in this bridge: dedicated servers can register both packet directions. */
public final class EchoNetwork1201 {
    public static final ResourceLocation INSERT = new ResourceLocation(EchoWarrior1201.MOD_ID, "summoner_insert_v1");
    public static final ResourceLocation RESULT = new ResourceLocation(EchoWarrior1201.MOD_ID, "summoner_insert_result_v1");
    private static Consumer<SummonerInsertRequest1201> sender;
    private static Consumer<SummonerInsertResult1201> receiver = result -> {};

    private EchoNetwork1201() {}
    public static void installClientSender(Consumer<SummonerInsertRequest1201> transport) { sender = transport; }
    public static void setClientReceiver(Consumer<SummonerInsertResult1201> consumer) { receiver = consumer; }
    public static void receiveOnClientThread(SummonerInsertResult1201 result) { receiver.accept(result); }
    public static void send(SummonerInsertRequest1201 request) {
        if (sender == null) throw new IllegalStateException("Client networking is not initialized");
        sender.accept(request);
    }
}
