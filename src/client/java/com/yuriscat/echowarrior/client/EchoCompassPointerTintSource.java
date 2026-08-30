package com.yuriscat.echowarrior.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record EchoCompassPointerTintSource() implements ItemTintSource {
	private static final int CYAN = 0xFF5AEEFF;
	public static final MapCodec<EchoCompassPointerTintSource> MAP_CODEC =
			MapCodec.unit(new EchoCompassPointerTintSource());

	@Override
	public int calculate(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
		return CYAN;
	}

	@Override
	public MapCodec<EchoCompassPointerTintSource> type() {
		return MAP_CODEC;
	}
}
