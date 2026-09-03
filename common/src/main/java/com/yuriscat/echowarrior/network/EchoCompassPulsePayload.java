package com.yuriscat.echowarrior.network;

import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EchoCompassPulsePayload(float closeness, boolean directional) implements CustomPacketPayload {
	public static final Type<EchoCompassPulsePayload> TYPE = new Type<>(EchoWarrior.id("echo_compass_pulse"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EchoCompassPulsePayload> STREAM_CODEC =
			CustomPacketPayload.codec(EchoCompassPulsePayload::write, EchoCompassPulsePayload::new);

	public EchoCompassPulsePayload {
		closeness = Math.clamp(closeness, 0.0F, 1.0F);
	}

	private EchoCompassPulsePayload(RegistryFriendlyByteBuf input) {
		this(input.readUnsignedByte() / 255.0F, input.readBoolean());
	}

	private void write(RegistryFriendlyByteBuf output) {
		output.writeByte(Math.round(this.closeness * 255.0F));
		output.writeBoolean(this.directional);
	}

	@Override
	public Type<EchoCompassPulsePayload> type() {
		return TYPE;
	}
}
