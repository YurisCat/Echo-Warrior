package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.EchoHeroType1201;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import java.util.UUID;

/** Runs against registered game items and the real transformed attribute classes. */
public final class EchoProgressionSelfTest1201 {
    public static void run(ServerLevel level) {
        com.yuriscat.echowarrior.compat.menu.GrowthMenuSelfTest1201.run();
        int original = EchoProgressionConfig1201.maxLevel();
        try {
            EchoProgressionConfig1201.receiveServerMaximum(100);
            ItemStack relic = new ItemStack(ModContent1201.relic(EchoHeroType1201.ROMAN_LEGIONARY));
            check(EchoRelicProgress1201.level(relic) == 1, "initial level");
            EchoRelicProgress1201.addExperience(relic, 1304);
            check(EchoRelicProgress1201.level(relic) == 29 && EchoRelicProgress1201.experience(relic) == 72, "level 29 boundary");
            EchoRelicProgress1201.addExperience(relic, 1);
            check(EchoRelicProgress1201.level(relic) == 30 && EchoRelicProgress1201.experienceNeeded(30) == 75, "level 30 is not a cap");
            EchoRelicProgress1201.addExperience(relic, 75);
            check(EchoRelicProgress1201.level(relic) == 31, "continuous curve after 30");
            EchoRelicProgress1201.addExperience(relic, 10005);
            check(EchoRelicProgress1201.level(relic) == 100 && EchoRelicProgress1201.experience(relic) == 0, "11385 total XP to 100");
            RelicNbt1201.update(relic, tag -> { tag.putInt("EchoWarriorLevel", 100); tag.putInt("EchoWarriorExperience", 42); });
            EchoProgressionConfig1201.receiveServerMaximum(30);
            check(EchoRelicProgress1201.level(relic) == 30 && EchoRelicProgress1201.storedLevel(relic) == 100, "effective cap preserves cultivation");
            EchoRelicProgress1201.addExperience(relic, Integer.MAX_VALUE);
            EchoProgressionConfig1201.receiveServerMaximum(101);
            check(EchoRelicProgress1201.level(relic) == 100 && EchoRelicProgress1201.experience(relic) == 42, "lowered cap preserves within-level XP");
            for (int maximum : new int[]{1, 30, 100, 1000, 5000}) {
                EchoProgressionConfig1201.receiveServerMaximum(maximum);
                ItemStack fresh = new ItemStack(ModContent1201.relic(EchoHeroType1201.ROMAN_LEGIONARY));
                EchoRelicProgress1201.addExperience(fresh, 16);
                EchoRelicProgress1201.addExperience(fresh, Integer.MAX_VALUE);
                check(EchoRelicProgress1201.level(fresh) == maximum && EchoRelicProgress1201.experience(fresh) == 0, "overflow-safe XP at " + maximum);
            }
            EchoProgressionConfig1201.receiveServerMaximum(5000);
            for (var type : EchoHeroType1201.values()) {
                double health = type.maximumHealth();
                double damage = type.attackDamage();
                for (int value : new int[]{1, 30, 50, 70, 100, 1000, 5000}) {
                    check(Math.abs(EchoRelicProgress1201.maximumHealth(type, value) / health - (1 + (value - 1) / 29.0)) < 1e-8, "health curve");
                    check(Math.abs(EchoRelicProgress1201.attackDamage(type, value) / damage - (1 + (value - 1) / 29.0)) < 1e-8, "damage curve");
                }
            }
            // Real transformed constructors must initialize both attributes for every hero.
            for (var type : java.util.List.of(ModContent1201.ROMAN_LEGIONARY_ECHO,
                    ModContent1201.AZTEC_WARRIOR_ECHO, ModContent1201.EGYPTIAN_ARCHER_ECHO,
                    ModContent1201.GUANDAO_WARRIOR_ECHO, ModContent1201.JAPANESE_SAMURAI_ECHO)) {
                var hero = type.create(level);
                check(hero != null, "registered hero factory: " + type);
                hero.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);
                hero.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5000);
                check(hero.getMaxHealth() == 10000 && hero.getAttributeValue(Attributes.ATTACK_DAMAGE) == 5000,
                        "constructor growth range: " + type);
                var bonus = new AttributeModifier(UUID.randomUUID(), "echo_growth_regression",
                        0.5, AttributeModifier.Operation.MULTIPLY_TOTAL);
                hero.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(bonus);
                check(hero.getMaxHealth() == 15000, "hero modifier above cap: " + type);
                hero.getAttribute(Attributes.MAX_HEALTH).removeModifier(bonus.getId());
                check(hero.getMaxHealth() == 10000, "hero modifier removal refreshes cache: " + type);
            }
            var entity = ModContent1201.ROMAN_LEGIONARY_ECHO.create(level);
            check(entity != null, "registered entity factory");
            entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);
            entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5000);
            check(entity.getMaxHealth() == 10000 && entity.getAttributeValue(Attributes.ATTACK_DAMAGE) == 5000, "Echo attributes exceed vanilla cap");
            entity.setHealth(7);
            entity.applyRelicState(relic, true);
            EchoProgressionConfig1201.receiveServerMaximum(30);
            entity.applyRelicState(relic, true);
            EchoProgressionConfig1201.receiveServerMaximum(5000);
            entity.applyRelicState(relic, true);
            check(entity.getHealth() == 7, "config decrease/increase and re-equip do not heal");
            // Non-Echo attribute instances retain their vanilla clamp.
            var ordinary = new net.minecraft.world.entity.ai.attributes.AttributeInstance(Attributes.MAX_HEALTH, ignored -> {});
            ordinary.setBaseValue(10000);
            check(ordinary.getValue() == 1024, "ordinary mob range unchanged");
            verifyAttributeComposition();
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info("[EchoProgressionSelfTest] PASS cap, save preservation, XP overflow, five hero curves and isolated attribute range");
        } finally { EchoProgressionConfig1201.receiveServerMaximum(original); }
    }

    /** Exercise the transformed class, including the real optional LOL Redirect. */
    private static void verifyAttributeComposition() {
        var attribute = new CountingAttribute();
        var instance = new AttributeInstance(attribute, ignored -> {});
        instance.setBaseValue(2000);
        check(instance.getValue() == 1024 && attribute.calls == 1, "ordinary sanitization exactly once");
        check(instance.getValue() == 1024 && attribute.calls == 1, "ordinary value remains cached");
        ((EchoGrowthAttribute1201) instance).echoWarrior$enableGrowthRange();
        check(instance.getValue() == 2000 && attribute.calls == 2, "growth enable invalidates cached clamp");
        check(instance.getValue() == 2000 && attribute.calls == 2, "growth value remains cached");
        var addition = new AttributeModifier(UUID.randomUUID(), "echo_test_add", 100, AttributeModifier.Operation.ADDITION);
        var base = new AttributeModifier(UUID.randomUUID(), "echo_test_base", 0.5, AttributeModifier.Operation.MULTIPLY_BASE);
        var total = new AttributeModifier(UUID.randomUUID(), "echo_test_total", 0.25, AttributeModifier.Operation.MULTIPLY_TOTAL);
        instance.addTransientModifier(addition);
        instance.addTransientModifier(base);
        instance.addTransientModifier(total);
        check(instance.getValue() == 3937.5 && attribute.calls == 3, "all modifier operations before growth clamp");
        instance.removeModifier(addition.getId());
        check(instance.getValue() == 3750 && attribute.calls == 4, "modifier removal invalidates growth cache");
        instance.removeModifier(base.getId());
        instance.removeModifier(total.getId());
        // Preserve the attribute's own treatment of lower bounds and non-finite input.
        for (double input : new double[]{-10, 0, 20, 1024, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            double expected = attribute.sanitizeValue(input);
            int calls = attribute.calls;
            instance.setBaseValue(input);
            check(Double.compare(instance.getValue(), expected) == 0 && attribute.calls == calls + 1,
                    "original sanitization retained: " + input);
        }
        boolean lol = false;
        try {
            Class.forName("com.lolmod.LOLMod", false, EchoProgressionSelfTest1201.class.getClassLoader());
            lol = true;
        } catch (ClassNotFoundException absent) { /* Ordinary Fabric/Forge baseline. */ }
        check(!Boolean.getBoolean("echoWarrior.lolRequired") || lol, "requested LOL runtime is present");
        var ordinary = new AttributeInstance(Attributes.MAX_HEALTH, ignored -> {});
        ordinary.setBaseValue(10000);
        // This fixture uses LOL's verified modifier-name contract; it is never
        // installed on a gameplay entity or used as the compatibility fix.
        var lolModifier = new AttributeModifier(UUID.randomUUID(), "lolmod_echo_regression",
                0.5, AttributeModifier.Operation.MULTIPLY_TOTAL);
        ordinary.addTransientModifier(lolModifier);
        check(ordinary.getValue() == (lol ? 15000 : 1024), "ordinary instance preserves optional LOL overflow");
        ordinary.removeModifier(lolModifier.getId());
        check(ordinary.getValue() == 1024, "removing LOL modifier restores ordinary clamp");
        ((EchoGrowthAttribute1201) ordinary).echoWarrior$enableGrowthRange();
        ordinary.addTransientModifier(lolModifier);
        check(ordinary.getValue() == 15000, "growth and LOL modifier do not apply multiplier twice");
        ordinary.removeModifier(lolModifier.getId());
        check(ordinary.getValue() == 10000, "growth survives LOL modifier removal");
        org.slf4j.LoggerFactory.getLogger("echo_warrior").info(
                "GROWTH ATTRIBUTE SELFTEST PASSED sanitize=once cache=checked modifiers=all bounds=preserved lol={}", lol);
    }

    private static final class CountingAttribute extends RangedAttribute {
        private int calls;
        private CountingAttribute() { super("echo_test.growth", 20, 0, 1024); }
        @Override public double sanitizeValue(double value) {
            this.calls++;
            return super.sanitizeValue(value);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Echo progression: " + message);
    }
}
