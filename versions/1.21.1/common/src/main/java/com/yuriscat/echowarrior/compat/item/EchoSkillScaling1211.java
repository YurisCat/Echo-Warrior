package com.yuriscat.echowarrior.compat.item;

/** Shared by authoritative combat and current-value tooltips. No artificial level cap. */
public final class EchoSkillScaling1211 {
    private EchoSkillScaling1211() { }

    public static double legionArmor(int level) { return 12.0 + level * 0.1; }
    public static double legionRelease(int level) { return 1.0 + level / 100.0; }
    public static double cursePerEffect(int level) { return 1.0 + level / 100.0; }
    public static float bleedingDamage(double attack) { return (float)(1.0 + Math.max(0.0, attack) * 0.1); }
    public static double conePenetration(int level) { return 0.35 + level * 0.005; }
    public static double conePierceChance(int level) { return 0.25 + level * 0.005; }
    public static double remainingArmor(double penetration) { return Math.max(0.0, 1.0 - penetration); }
    public static double penetrationDamage(double penetration) { return 1.0 + Math.max(0.0, penetration - 1.0); }
}
