package com.yuriscat.echowarrior.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record EchoCompassAngleProperty() implements RangeSelectItemModelProperty {
	public static final MapCodec<EchoCompassAngleProperty> MAP_CODEC = MapCodec.unit(new EchoCompassAngleProperty());

	@Override
	public float get(
			ItemStack itemStack,
			@Nullable ClientLevel level,
			@Nullable ItemOwner owner,
			int seed
	) {
		return EchoCompassClientState.angle(level, owner);
	}

	@Override
	public MapCodec<EchoCompassAngleProperty> type() {
		return MAP_CODEC;
	}
}
