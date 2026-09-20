package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.item.EchoCompassItem1201;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class EchoCompassClientProperties1201 {
    private EchoCompassClientProperties1201() {
    }

    @FunctionalInterface
    public interface Registrar {
        void register(net.minecraft.world.item.Item item, net.minecraft.resources.ResourceLocation id,
                      net.minecraft.client.renderer.item.ClampedItemPropertyFunction function);
    }

    public static void register(Registrar registrar) {
        registrar.register(ModContent1201.ECHO_COMPASS, ModContent1201.id("echo_compass_angle"),
                (stack, level, entity, seed) -> angle(stack, level == null ? 0L : level.getGameTime(), entity));
        registrar.register(ModContent1201.ECHO_COMPASS, ModContent1201.id("echo_compass_gold_frame"),
                (stack, level, entity, seed) -> usesGoldFrame(stack, level == null ? 0L : level.getGameTime(), entity));
        registrar.register(ModContent1201.ECHO_COMPASS, ModContent1201.id("echo_compass_iron_frame"),
                (stack, level, entity, seed) -> usesIronFrame(stack) ? 1.0F : 0.0F);
    }

    private static float angle(net.minecraft.world.item.ItemStack stack, long gameTime, LivingEntity owner) {
        int mode = EchoCompassItem1201.trackingMode(stack);
        if (owner == null || mode == EchoCompassItem1201.MODE_INACTIVE) return 0.0F;
        if (mode == EchoCompassItem1201.MODE_OUTSIDE) {
            BlockPos target = BlockPos.of(EchoCompassItem1201.trackingTarget(stack));
            Vec3 position = owner.position();
            double targetAngle = Math.atan2(target.getZ() + 0.5 - position.z, target.getX() + 0.5 - position.x)
                    / (Math.PI * 2.0);
            double ownerRotation = positiveModulo(owner.getVisualRotationYInDegrees() / 360.0, 1.0);
            return (float)positiveModulo(targetAngle - ownerRotation + 0.75, 1.0);
        }
        double turnsPerSecond = 2.0;
        if (mode == EchoCompassItem1201.MODE_INNER || mode == EchoCompassItem1201.MODE_SALVAGE) {
            double distance = owner.position().distanceTo(BlockPos.of(EchoCompassItem1201.trackingTarget(stack)).getCenter());
            double closeness = net.minecraft.util.Mth.clamp(1.0 - distance / 32.0, 0.0, 1.0);
            turnsPerSecond = 0.625 + (2.5 - 0.625) * closeness;
        }
        return (float)positiveModulo(gameTime / 20.0 * turnsPerSecond, 1.0);
    }

    private static float usesGoldFrame(net.minecraft.world.item.ItemStack stack, long gameTime, LivingEntity owner) {
        if (owner == null) return 0.0F;
        int mode = EchoCompassItem1201.trackingMode(stack);
        if (mode != EchoCompassItem1201.MODE_INNER && mode != EchoCompassItem1201.MODE_SALVAGE) return 0.0F;
        double distance = owner.position().distanceTo(BlockPos.of(EchoCompassItem1201.trackingTarget(stack)).getCenter());
        if (distance <= 2.0) return 1.0F;
        if (distance >= 24.0) return 0.0F;
        double closeness = net.minecraft.util.Mth.clamp((24.0 - distance) / 22.0, 0.0, 1.0);
        double cyclesPerSecond = 0.45 + (1.5 - 0.45) * closeness;
        return positiveModulo(gameTime / 20.0 * cyclesPerSecond, 1.0) >= 0.5 ? 1.0F : 0.0F;
    }

    private static boolean usesIronFrame(net.minecraft.world.item.ItemStack stack) {
        int mode = EchoCompassItem1201.trackingMode(stack);
        return mode != EchoCompassItem1201.MODE_INNER && mode != EchoCompassItem1201.MODE_SALVAGE
                && !EchoCompassItem1201.isOutsideSoundEnabled(stack);
    }

    private static double positiveModulo(double value, double modulus) {
        return value - Math.floor(value / modulus) * modulus;
    }
}
