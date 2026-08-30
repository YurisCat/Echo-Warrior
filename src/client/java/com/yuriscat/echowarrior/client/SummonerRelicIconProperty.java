package com.yuriscat.echowarrior.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import com.yuriscat.echowarrior.item.TooltipShiftState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record SummonerRelicIconProperty() implements SelectItemModelProperty<String> {
	public static final Codec<String> VALUE_CODEC = Codec.STRING;
	public static final SelectItemModelProperty.Type<SummonerRelicIconProperty, String> TYPE =
			SelectItemModelProperty.Type.create(MapCodec.unit(new SummonerRelicIconProperty()), VALUE_CODEC);

	@Override
	public @Nullable String get(
			ItemStack itemStack,
			@Nullable ClientLevel level,
			@Nullable LivingEntity owner,
			int seed,
			ItemDisplayContext displayContext
	) {
		if (displayContext != ItemDisplayContext.GUI || !TooltipShiftState.isShiftDown()) {
			return null;
		}
		ItemStack relic = TestEchoSummonerItem.relicStack(itemStack);
		return relic.getItem() instanceof EchoRelicItem echoRelic ? echoRelic.heroType().id() : null;
	}

	@Override
	public Codec<String> valueCodec() {
		return VALUE_CODEC;
	}

	@Override
	public SelectItemModelProperty.Type<SummonerRelicIconProperty, String> type() {
		return TYPE;
	}
}
