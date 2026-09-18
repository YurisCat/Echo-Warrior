package com.yuriscat.echowarrior.compat.network;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.binding.CreativeSummonerDestroyTracker1211;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Reports summoner IDs involved in one creative inventory trash action. */
public record CreativeSummonerDestructionPayload1211(List<UUID> summonerIds) implements CustomPacketPayload {
    private static final int MAX_SUMMONER_IDS = 4096;
    public static final Type<CreativeSummonerDestructionPayload1211> TYPE =
            new Type<>(ModContent1211.id("creative_summoner_destruction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CreativeSummonerDestructionPayload1211> STREAM_CODEC =
            CustomPacketPayload.codec(
                    CreativeSummonerDestructionPayload1211::write,
                    CreativeSummonerDestructionPayload1211::new);

    public CreativeSummonerDestructionPayload1211 {
        if (summonerIds.size() > MAX_SUMMONER_IDS) {
            throw new IllegalArgumentException("Too many destroyed summoner IDs: " + summonerIds.size());
        }
        summonerIds = List.copyOf(summonerIds);
    }

    private CreativeSummonerDestructionPayload1211(RegistryFriendlyByteBuf input) {
        this(readIds(input));
    }

    private static List<UUID> readIds(RegistryFriendlyByteBuf input) {
        int size = input.readVarInt();
        if (size < 0 || size > MAX_SUMMONER_IDS) {
            throw new IllegalArgumentException("Invalid destroyed summoner ID count: " + size);
        }
        List<UUID> ids = new ArrayList<>(size);
        for (int index = 0; index < size; index++) ids.add(input.readUUID());
        return ids;
    }

    private void write(RegistryFriendlyByteBuf output) {
        output.writeVarInt(this.summonerIds.size());
        for (UUID summonerId : this.summonerIds) output.writeUUID(summonerId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ServerPlayer player, CreativeSummonerDestructionPayload1211 payload) {
        EchoWarrior1211.LOGGER.info(
                "[CreativeSummonerDelete] server received destruction request: player={} ids={}",
                player.getGameProfile().getName(), payload.summonerIds);
        CreativeSummonerDestroyTracker1211.requestCreativeTrash(player, payload.summonerIds);
    }
}
