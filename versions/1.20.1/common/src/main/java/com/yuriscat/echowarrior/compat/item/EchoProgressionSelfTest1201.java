package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.EchoHeroType1201;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;

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
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info("[EchoProgressionSelfTest] PASS cap, save preservation, XP overflow, five hero curves and isolated attribute range");
        } finally { EchoProgressionConfig1201.receiveServerMaximum(original); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Echo progression: " + message);
    }
}
