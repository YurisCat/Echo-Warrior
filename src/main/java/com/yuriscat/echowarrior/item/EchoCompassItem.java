package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.network.EchoCompassMessagePayload;
import com.yuriscat.echowarrior.world.EchoCompassSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Consumer;
import java.util.function.BooleanSupplier;
import java.util.Objects;

public final class EchoCompassItem extends Item {
	private static final String SOUND_MUTED = "EchoWarriorCompassSoundMuted";
	private static final String LEGACY_ENABLED = "EchoWarriorCompassEnabled";
	private static BooleanSupplier clientInsideBattlefield = () -> false;

	public EchoCompassItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.GOLD);
	}

	@Override
	public void appendHoverText(
			ItemStack stack,
			TooltipContext context,
			TooltipDisplay display,
			Consumer<Component> builder,
			TooltipFlag flag
	) {
		builder.accept(Component.translatable("item.echo_warrior.echo_compass.tooltip.guide_prefix")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.translatable("item.echo_warrior.echo_compass.tooltip.battlefield")
						.withStyle(style -> style.withColor(0xE6E6E6)))
				.append(Component.translatable("item.echo_warrior.echo_compass.tooltip.guide_suffix")
						.withStyle(ChatFormatting.GRAY)));
		builder.accept(Component.translatable("item.echo_warrior.echo_compass.tooltip.brush_prefix")
				.withStyle(ChatFormatting.GRAY)
				.append(Component.translatable("item.echo_warrior.echo_compass.tooltip.brush")
						.withStyle(style -> style.withColor(0xEEC39A)))
				.append(Component.translatable("item.echo_warrior.echo_compass.tooltip.brush_suffix")
						.withStyle(ChatFormatting.GRAY)));
		builder.accept(Component.empty());
		builder.accept(Component.translatable("item.echo_warrior.echo_compass.tooltip.sound_hint")
				.withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (shouldPrioritizeBrush(level, player, hand)) return InteractionResult.PASS;
		if (shouldYieldSoundToggle(level, player, hand)) return InteractionResult.PASS;
		ItemStack stack = player.getItemInHand(hand);
		boolean soundEnabled = !isOutsideSoundEnabled(stack);
		if (!level.isClientSide()) {
			setOutsideSoundEnabled(stack, soundEnabled);
			if (player instanceof ServerPlayer serverPlayer) {
				EchoCompassSystem.sendMessage(serverPlayer, soundEnabled
						? EchoCompassMessagePayload.Message.SOUND_ENABLED
						: EchoCompassMessagePayload.Message.SOUND_DISABLED);
				EchoCompassSystem.playToggle(serverPlayer, soundEnabled);
			}
		}
		return InteractionResult.SUCCESS;
	}

	private static boolean shouldYieldSoundToggle(
			Level level,
			Player player,
			InteractionHand compassHand
	) {
		InteractionHand otherHand = otherHand(compassHand);
		if (player.getItemInHand(otherHand).isEmpty()) return false;

		boolean insideBattlefield = level.isClientSide()
				? clientInsideBattlefield.getAsBoolean()
				: player instanceof ServerPlayer serverPlayer
						&& EchoCompassSystem.isInsideBattlefieldMode(serverPlayer);
		if (insideBattlefield) return true;

		// Outside a ruin, a main-hand compass yields to any occupied offhand. An
		// offhand compass is already naturally lower priority because Minecraft has
		// offered the interaction to the occupied main hand before calling this use.
		return compassHand == InteractionHand.MAIN_HAND;
	}

	public static void setClientInsideBattlefieldSupplier(BooleanSupplier supplier) {
		clientInsideBattlefield = Objects.requireNonNull(supplier);
	}

	private static boolean shouldPrioritizeBrush(Level level, Player player, InteractionHand compassHand) {
		InteractionHand otherHand = otherHand(compassHand);
		if (!player.getItemInHand(otherHand).is(Items.BRUSH)) return false;
		var hitResult = ProjectileUtil.getHitResultOnViewVector(
				player, EntitySelector.CAN_BE_PICKED, player.blockInteractionRange());
		return hitResult instanceof BlockHitResult blockHit
				&& level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof BrushableBlock;
	}

	private static InteractionHand otherHand(InteractionHand hand) {
		return hand == InteractionHand.MAIN_HAND
				? InteractionHand.OFF_HAND
				: InteractionHand.MAIN_HAND;
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
