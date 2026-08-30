package com.yuriscat.echowarrior.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record EchoCompassIronFrameProperty() implements ConditionalItemModelProperty {
	public static final MapCodec<EchoCompassIronFrameProperty> MAP_CODEC =
			MapCodec.unit(new EchoCompassIronFrameProperty());

	@Override
	public boolean get(
			ItemStack itemStack,
			@Nullable ClientLevel level,
			@Nullable LivingEntity owner,
			int seed,
			ItemDisplayContext displayContext
	) {
		return EchoCompassClientState.usesIronFrame(itemStack, level, owner);
	}

	@Override
	public MapCodec<EchoCompassIronFrameProperty> type() {
		return MAP_CODEC;
	}
}
