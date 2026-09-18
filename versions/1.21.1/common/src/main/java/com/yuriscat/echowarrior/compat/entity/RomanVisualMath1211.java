package com.yuriscat.echowarrior.compat.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Loader-neutral visual math shared by the 1.21.1 server self-test and client model.
 */
public final class RomanVisualMath1211 {
    private RomanVisualMath1211() {
    }

    public static float calculateBlink(float now, long blinkStart, byte blinkCount) {
        if (blinkCount <= 0) return 0.0F;
        float result = blinkPulse(now - blinkStart);
        if (blinkCount > 1) result = Math.max(result, blinkPulse(now - blinkStart - 4.0F));
        return result;
    }

    public static float calculateHurtBlink(float now, long blinkStart) {
        float elapsed = now - blinkStart;
        if (elapsed < 0.0F || elapsed > 6.0F) return 0.0F;
        if (elapsed <= 1.6F) return elapsed / 1.6F;
        if (elapsed <= 2.2F) return 1.0F;
        return 1.0F - (elapsed - 2.2F) / 3.8F;
    }

    public static float approach(float current, float target, float responsiveness, float deltaTicks) {
        float weight = 1.0F - (float)Math.pow(1.0F - responsiveness, Math.max(0.0F, deltaTicks));
        return Mth.lerp(weight, current, target);
    }

    public static float worldYawToward(Vec3 delta) {
        return (float)(Math.atan2(delta.z, delta.x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    public static float worldPitchToward(Vec3 delta, double horizontal) {
        return Mth.clamp((float)(-Math.atan2(delta.y, horizontal) * Mth.RAD_TO_DEG), -35.0F, 40.0F);
    }

    public static float toRadians(float degrees) {
        return degrees * Mth.DEG_TO_RAD;
    }

    public static float parentCompensation(float inheritedRadians, boolean shieldRaised) {
        if (shieldRaised) return 1.0F;
        float degrees = Math.abs(inheritedRadians) * Mth.RAD_TO_DEG;
        float progress = Mth.clamp((degrees - 3.0F) / 5.0F, 0.0F, 1.0F);
        float smooth = progress * progress * (3.0F - 2.0F * progress);
        return 1.0F - smooth;
    }

    public static int requiredGazeTicks(double distance) {
        return 10 + (int)Math.ceil(Math.max(0.0, distance - 12.0) / 2.0);
    }

    public static boolean gazeHitsHead(Vec3 eyePosition, Vec3 lookDirection,
                                       Vec3 headCenter, double radius) {
        Vec3 towardHead = headCenter.subtract(eyePosition);
        if (lookDirection.lengthSqr() < 1.0E-8 || towardHead.lengthSqr() < 1.0E-8) return false;
        Vec3 look = lookDirection.normalize();
        double projection = look.dot(towardHead);
        if (projection <= 0.0) return false;
        double distanceFromRaySqr = Math.max(0.0,
                towardHead.lengthSqr() - projection * projection);
        return distanceFromRaySqr <= radius * radius;
    }

    private static float blinkPulse(float elapsed) {
        if (elapsed < 0.0F || elapsed > 3.0F) return 0.0F;
        return Mth.sin(elapsed / 3.0F * (float)Math.PI);
    }
}
