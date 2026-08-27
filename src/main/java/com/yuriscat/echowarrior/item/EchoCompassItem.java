package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.world.EchoCompassSystem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class EchoCompassItem extends Item {
	private static final String ENABLED = "EchoWarriorCompassEnabled";

	public EchoCompassItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean enabled = !isEnabled(stack);
		if (!level.isClientSide()) {
			if (enabled) {
				disableOtherCompasses(player, stack);
			}
			setEnabled(stack, enabled);
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.sendOverlayMessage(Component.translatable(enabled
						? "message.echo_warrior.echo_compass.enabled"
						: "message.echo_warrior.echo_compass.disabled"));
				EchoCompassSystem.playToggle(serverPlayer, enabled);
				EchoCompassSystem.forceReacquire(serverPlayer);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return isEnabled(stack) || super.isFoil(stack);
	}

	public static boolean isEnabled(ItemStack stack) {
		return stack.is(ModItems.ECHO_COMPASS)
				&& stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getBooleanOr(ENABLED, false);
	}

	public static void setEnabled(ItemStack stack, boolean enabled) {
		if (!stack.is(ModItems.ECHO_COMPASS)) return;
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(ENABLED, enabled));
	}

	private static void disableOtherCompasses(Player player, ItemStack selected) {
		for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
			ItemStack candidate = player.getInventory().getItem(slot);
			if (candidate != selected && candidate.is(ModItems.ECHO_COMPASS)) {
				setEnabled(candidate, false);
			}
		}
	}
}
