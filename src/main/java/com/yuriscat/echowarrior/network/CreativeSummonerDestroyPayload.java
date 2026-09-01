package com.yuriscat.echowarrior.network;

import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CreativeSummonerDestroyPayload(List<UUID> summonerIds) implements CustomPacketPayload {
	private static final int MAX_SUMMONER_IDS = 4096;
	public static final Type<CreativeSummonerDestroyPayload> TYPE =
			new Type<>(EchoWarrior.id("creative_summoner_destroy"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CreativeSummonerDestroyPayload> STREAM_CODEC =
			CustomPacketPayload.codec(CreativeSummonerDestroyPayload::write, CreativeSummonerDestroyPayload::new);

	public CreativeSummonerDestroyPayload {
		if (summonerIds.size() > MAX_SUMMONER_IDS) {
			throw new IllegalArgumentException("Too many destroyed summoner IDs: " + summonerIds.size());
		}
		summonerIds = List.copyOf(summonerIds);
	}

	private CreativeSummonerDestroyPayload(RegistryFriendlyByteBuf input) {
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
	public Type<CreativeSummonerDestroyPayload> type() {
		return TYPE;
	}
}
