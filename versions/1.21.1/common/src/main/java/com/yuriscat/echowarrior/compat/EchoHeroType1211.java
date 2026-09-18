package com.yuriscat.echowarrior.compat;

import com.yuriscat.echowarrior.compat.item.EchoRelicItem1211;
import net.minecraft.world.item.ItemStack;

public enum EchoHeroType1211 {
    ROMAN_LEGIONARY("roman_legionary", 30.0, 6.0, 8.0, 0.28, 0.30, 20, 4),
    AZTEC_WARRIOR("aztec_warrior", 34.0, 8.0, 4.0, 0.30, 0.20, 29, 5),
    EGYPTIAN_ARCHER("egyptian_archer", 28.0, 5.0, 0.0, 0.30, 0.15, 42, 4),
    GUANDAO_WARRIOR("guandao_warrior", 30.0, 7.0, 12.0, 0.26, 0.30, 34, 4),
    JAPANESE_SAMURAI("japanese_samurai", 28.0, 6.0, 6.0, 0.32, 0.20, 40, 4);

    private final String id;
    private final double maximumHealth;
    private final double attackDamage;
    private final double armor;
    private final double movementSpeed;
    private final double knockbackResistance;
    private final int baseAttackIntervalTicks;
    private final int skillCount;

    EchoHeroType1211(String id, double maximumHealth, double attackDamage, double armor,
                     double movementSpeed, double knockbackResistance,
                     int baseAttackIntervalTicks, int skillCount) {
        this.id = id;
        this.maximumHealth = maximumHealth;
        this.attackDamage = attackDamage;
        this.armor = armor;
        this.movementSpeed = movementSpeed;
        this.knockbackResistance = knockbackResistance;
        this.baseAttackIntervalTicks = baseAttackIntervalTicks;
        this.skillCount = skillCount;
    }

    public String id() { return this.id; }
    public String translationKey() { return "hero.echo_warrior." + this.id; }
    public double maximumHealth() { return this.maximumHealth; }
    public double attackDamage() { return this.attackDamage; }
    public double armor() { return this.armor; }
    public double movementSpeed() { return this.movementSpeed; }
    public double knockbackResistance() { return this.knockbackResistance; }
    public double baseMaximumHealth() { return this.maximumHealth; }
    public double baseAttackDamage() { return this.attackDamage; }
    public double baseArmor() { return this.armor; }
    public double baseMovementSpeed() { return this.movementSpeed; }
    public double baseKnockbackResistance() { return this.knockbackResistance; }
    public int baseAttackIntervalTicks() { return this.baseAttackIntervalTicks; }
    public int skillCount() { return this.skillCount; }
    public int allSkillsEnabledMask() { return (1 << this.skillCount) - 1; }
    public int defaultEnabledSkillsMask() {
        return this == EGYPTIAN_ARCHER
                ? allSkillsEnabledMask() & ~(1 << 1)
                : allSkillsEnabledMask();
    }

    public static EchoHeroType1211 fromRelic(ItemStack relic) {
        return relic.getItem() instanceof EchoRelicItem1211 relicItem
                ? relicItem.heroType()
                : ROMAN_LEGIONARY;
    }
}
