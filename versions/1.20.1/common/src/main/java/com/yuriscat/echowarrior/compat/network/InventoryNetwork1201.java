package com.yuriscat.echowarrior.compat.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

public final class InventoryNetwork1201 {
    public static final ResourceLocation CREATIVE_INSERT = new ResourceLocation("echo_warrior", "creative_insert_v1");
    public static final ResourceLocation CREATIVE_REPLY = new ResourceLocation("echo_warrior", "creative_reply_v1");
    public static final ResourceLocation FEEDBACK = new ResourceLocation("echo_warrior", "insertion_feedback_v1");
    public static final ResourceLocation CREATIVE_DESTROY = new ResourceLocation("echo_warrior", "creative_destroy_v1");
    public static Consumer<CreativeDestructionRequest1201> destructionSender;
    public static Consumer<CreativeInsertRequest1201> creativeSender;
    public static Consumer<CreativeInsertReply1201> creativeReceiver = ignored -> {};
    public static Consumer<InsertionFeedback1201> feedbackReceiver = ignored -> {};
    public static BiConsumer<ServerPlayer, InsertionFeedback1201> feedbackSender = (player, packet) -> {};
    private InventoryNetwork1201() {}
    public static void feedback(ServerPlayer player, InsertionFeedback1201 packet) {
        // Fake players/server-only test fixtures have no client to render this optional feedback.
        if (player.connection != null) feedbackSender.accept(player, packet);
    }
}
