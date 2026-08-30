package com.yuriscat.echowarrior.network;

import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EchoCompassStatePayload(Mode mode, long targetPos) implements CustomPacketPayload {
	public static final Type<EchoCompassStatePayload> TYPE = new Type<>(EchoWarrior.id("echo_compass_state"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EchoCompassStatePayload> STREAM_CODEC =
			CustomPacketPayload.codec(EchoCompassStatePayload::write, EchoCompassStatePayload::new);

	private EchoCompassStatePayload(RegistryFriendlyByteBuf input) {
		this(Mode.byId(input.readUnsignedByte()), input.readLong());
	}

	private void write(RegistryFriendlyByteBuf output) {
		output.writeByte(this.mode.ordinal());
		output.writeLong(this.targetPos);
	}

	@Override
	public Type<EchoCompassStatePayload> type() {
		return TYPE;
	}

	public static EchoCompassStatePayload inactive() {
		return new EchoCompassStatePayload(Mode.INACTIVE, 0L);
	}

	public static EchoCompassStatePayload noTarget() {
		return new EchoCompassStatePayload(Mode.NO_TARGET, 0L);
	}

	public enum Mode {
		INACTIVE,
		OUTSIDE,
		INNER,
		SALVAGE,
		NO_TARGET;

		private static Mode byId(int id) {
			Mode[] values = values();
			return id >= 0 && id < values.length ? values[id] : INACTIVE;
		}
	}
}
