package com.yuriscat.echowarrior.network;

import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EchoCompassMessagePayload(Message message, int value) implements CustomPacketPayload {
	public static final Type<EchoCompassMessagePayload> TYPE = new Type<>(EchoWarrior.id("echo_compass_message"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EchoCompassMessagePayload> STREAM_CODEC =
			CustomPacketPayload.codec(EchoCompassMessagePayload::write, EchoCompassMessagePayload::new);

	public EchoCompassMessagePayload {
		value = Math.max(0, value);
	}

	public EchoCompassMessagePayload(Message message) {
		this(message, 0);
	}

	private EchoCompassMessagePayload(RegistryFriendlyByteBuf input) {
		this(Message.byNetworkId(input.readUnsignedByte()), input.readVarInt());
	}

	private void write(RegistryFriendlyByteBuf output) {
		output.writeByte(this.message.ordinal());
		output.writeVarInt(this.value);
	}

	public Component component() {
		return this.message.usesValue
				? Component.translatable(this.message.translationKey, this.value)
				: Component.translatable(this.message.translationKey);
	}

	@Override
	public Type<EchoCompassMessagePayload> type() {
		return TYPE;
	}

	public enum Message {
		SOUND_ENABLED("message.echo_warrior.echo_compass.sound_enabled", false),
		SOUND_DISABLED("message.echo_warrior.echo_compass.sound_disabled", false),
		ECHO_DETECTED("message.echo_warrior.echo_compass.echo_detected", false),
		NO_NEARBY_SITE("message.echo_warrior.echo_compass.no_nearby_site", false),
		REMAINING_ECHOES("message.echo_warrior.echo_compass.remaining_echoes", true),
		SITE_QUIET("message.echo_warrior.echo_compass.site_quiet", false);

		private static final Message[] VALUES = values();
		private final String translationKey;
		private final boolean usesValue;

		Message(String translationKey, boolean usesValue) {
			this.translationKey = translationKey;
			this.usesValue = usesValue;
		}

		private static Message byNetworkId(int id) {
			return id >= 0 && id < VALUES.length ? VALUES[id] : NO_NEARBY_SITE;
		}
	}
}
