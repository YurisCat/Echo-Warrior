package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.world.EchoCompassSystem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class EchoCompassItem extends Item {
	private static final String SOUND_MUTED = "EchoWarriorCompassSoundMuted";
	private static final String LEGACY_ENABLED = "EchoWarriorCompassEnabled";

	public EchoCompassItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean soundEnabled = !isOutsideSoundEnabled(stack);
		if (!level.isClientSide()) {
			setOutsideSoundEnabled(stack, soundEnabled);
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.sendOverlayMessage(Component.translatable(soundEnabled
						? "message.echo_warrior.echo_compass.sound_enabled"
						: "message.echo_warrior.echo_compass.sound_disabled"));
				EchoCompassSystem.playToggle(serverPlayer, soundEnabled);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return isOutsideSoundEnabled(stack) || super.isFoil(stack);
	}

	public static boolean isOutsideSoundEnabled(ItemStack stack) {
		return stack.is(ModItems.ECHO_COMPASS)
				&& !stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getBooleanOr(SOUND_MUTED, false);
	}

	public static void setOutsideSoundEnabled(ItemStack stack, boolean enabled) {
		if (!stack.is(ModItems.ECHO_COMPASS)) return;
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putBoolean(SOUND_MUTED, !enabled);
			tag.remove(LEGACY_ENABLED);
		});
	}
}
