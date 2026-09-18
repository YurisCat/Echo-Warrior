package com.yuriscat.echowarrior.compat.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.phys.BlockHitResult;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.world.EchoCompassSystem1211;

import java.util.List;

public final class EchoCompassItem1211 extends Item {
    private static final String SOUND_ENABLED = "EchoWarriorCompassSoundEnabled";
    private static final String TRACKING_MODE = "EchoWarriorCompassMode";
    private static final String TRACKING_TARGET = "EchoWarriorCompassTarget";
    public static final int MODE_INACTIVE = 0;
    public static final int MODE_OUTSIDE = 1;
    public static final int MODE_INNER = 2;
    public static final int MODE_SALVAGE = 3;
    public static final int MODE_NO_TARGET = 4;

    public EchoCompassItem1211(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (shouldPrioritizeBrush(level, player, hand) || shouldYieldSoundToggle(level, player, hand, stack)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            boolean enabled = !tag.contains(SOUND_ENABLED) || tag.getBoolean(SOUND_ENABLED);
            boolean next = !enabled;
            CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putBoolean(SOUND_ENABLED, next));
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                EchoCompassSystem1211.sendToggleMessage(serverPlayer, next);
                EchoCompassSystem1211.playToggle(serverPlayer, next);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static boolean shouldYieldSoundToggle(Level level, Player player, InteractionHand compassHand,
                                                  ItemStack compass) {
        InteractionHand otherHand = otherHand(compassHand);
        if (player.getItemInHand(otherHand).isEmpty()) return false;
        boolean inside = level.isClientSide
                ? trackingMode(compass) == MODE_INNER || trackingMode(compass) == MODE_SALVAGE
                : player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && EchoCompassSystem1211.isInsideBattlefieldMode(serverPlayer);
        return inside || compassHand == InteractionHand.MAIN_HAND;
    }

    private static boolean shouldPrioritizeBrush(Level level, Player player, InteractionHand compassHand) {
        if (!player.getItemInHand(otherHand(compassHand)).is(Items.BRUSH)) return false;
        var hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        return hit instanceof BlockHitResult blockHit
                && level.getBlockState(blockHit.getBlockPos()).getBlock() instanceof BrushableBlock;
    }

    private static InteractionHand otherHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    public static boolean isOutsideSoundEnabled(ItemStack stack) {
        if (!stack.is(ModContent1211.ECHO_COMPASS)) return false;
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return !tag.contains(SOUND_ENABLED) || tag.getBoolean(SOUND_ENABLED);
    }

    public static int trackingMode(ItemStack stack) {
        if (!stack.is(ModContent1211.ECHO_COMPASS)) return MODE_INACTIVE;
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(TRACKING_MODE);
    }

    public static long trackingTarget(ItemStack stack) {
        if (!stack.is(ModContent1211.ECHO_COMPASS)) return 0L;
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong(TRACKING_TARGET);
    }

    public static void writeTracking(ItemStack stack, int mode, long target) {
        if (!stack.is(ModContent1211.ECHO_COMPASS)) return;
        var current = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (current.getInt(TRACKING_MODE) == mode && current.getLong(TRACKING_TARGET) == target) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putInt(TRACKING_MODE, mode);
            tag.putLong(TRACKING_TARGET, target);
        });
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(ChatFormatting.GOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.echo_warrior.echo_compass.tooltip.guide_prefix")
                .append(Component.translatable("item.echo_warrior.echo_compass.tooltip.battlefield")
                        .withStyle(style -> style.withColor(0xE6E6E6)))
                .append(Component.translatable("item.echo_warrior.echo_compass.tooltip.guide_suffix"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.echo_warrior.echo_compass.tooltip.brush_prefix")
                .append(Component.translatable("item.echo_warrior.echo_compass.tooltip.brush")
                        .withStyle(style -> style.withColor(0xEEC39A)))
                .append(Component.translatable("item.echo_warrior.echo_compass.tooltip.brush_suffix"))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.echo_warrior.echo_compass.tooltip.sound_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
