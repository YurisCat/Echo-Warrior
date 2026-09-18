package com.yuriscat.echowarrior.compat.client;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Shared vanilla color providers used by both 1.21.1 loaders. */
public final class CompatColorProviders1211 {
    private static final int ECHO_COMPASS_POINTER_CYAN = 0xFF5AEEFF;

    private CompatColorProviders1211() {
    }

    public static int suspiciousGrass(BlockState state, @Nullable BlockAndTintGetter level,
                                      @Nullable BlockPos pos, int tintIndex) {
        return level != null && pos != null
                ? BiomeColors.getAverageGrassColor(level, pos)
                : GrassColor.getDefaultColor();
    }

    public static int suspiciousGrassItem(ItemStack stack, int tintIndex) {
        return tintIndex == 0 ? GrassColor.getDefaultColor() : -1;
    }

    public static int echoCompass(ItemStack stack, int tintIndex) {
        return tintIndex == 1 ? ECHO_COMPASS_POINTER_CYAN : -1;
    }
}
