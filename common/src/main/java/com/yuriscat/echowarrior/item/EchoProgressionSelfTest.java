package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.item.EchoHeroType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Runs against registered game items and the real transformed attribute classes. */
public final class EchoProgressionSelfTest {
    public static void run(ServerLevel level) {
        com.yuriscat.echowarrior.menu.GrowthMenuSelfTest.run();
        int original = EchoProgressionConfig.maxLevel();
        try {
            EchoProgressionConfig.receiveServerMaximum(100);
            ItemStack relic = new ItemStack(ModItems.ROMAN_LEGIONARY_RELIC);
            check(EchoRelicProgress.level(relic) == 1, "initial level");
            EchoRelicProgress.addExperience(relic, 1304);
            check(EchoRelicProgress.level(relic) == 29 && EchoRelicProgress.experience(relic) == 72, "level 29 boundary");
            EchoRelicProgress.addExperience(relic, 1);
            check(EchoRelicProgress.level(relic) == 30 && EchoRelicProgress.experienceNeeded(30) == 75, "level 30 is not a cap");
            EchoRelicProgress.addExperience(relic, 75);
            check(EchoRelicProgress.level(relic) == 31, "continuous curve after 30");
            EchoRelicProgress.addExperience(relic, 10005);
            check(EchoRelicProgress.level(relic) == 100 && EchoRelicProgress.experience(relic) == 0, "11385 total XP to 100");
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, relic, tag -> { tag.putInt("EchoWarriorLevel", 100); tag.putInt("EchoWarriorExperience", 42); });
            EchoProgressionConfig.receiveServerMaximum(30);
            check(EchoRelicProgress.level(relic) == 30 && EchoRelicProgress.storedLevel(relic) == 100, "effective cap preserves cultivation");
            EchoRelicProgress.addExperience(relic, Integer.MAX_VALUE);
            EchoProgressionConfig.receiveServerMaximum(101);
            check(EchoRelicProgress.level(relic) == 100 && EchoRelicProgress.experience(relic) == 42, "lowered cap preserves within-level XP");
            for (int maximum : new int[]{1, 30, 100, 1000, 5000}) {
                EchoProgressionConfig.receiveServerMaximum(maximum);
                ItemStack fresh = new ItemStack(ModItems.ROMAN_LEGIONARY_RELIC);
                EchoRelicProgress.addExperience(fresh, 16);
                EchoRelicProgress.addExperience(fresh, Integer.MAX_VALUE);
                check(EchoRelicProgress.level(fresh) == maximum && EchoRelicProgress.experience(fresh) == 0, "overflow-safe XP at " + maximum);
            }
            EchoProgressionConfig.receiveServerMaximum(5000);
            for (var type : EchoHeroType.values()) {
                double health = type.baseMaximumHealth();
                double damage = type.baseAttackDamage();
                for (int value : new int[]{1, 30, 50, 70, 100, 1000, 5000}) {
                    check(Math.abs(EchoRelicProgress.maximumHealth(type, value) / health - (1 + (value - 1) / 29.0)) < 1e-8, "health curve");
                    check(Math.abs(EchoRelicProgress.attackDamage(type, value) / damage - (1 + (value - 1) / 29.0)) < 1e-8, "damage curve");
                }
            }
            var entity = com.yuriscat.echowarrior.ModEntities.ROMAN_LEGIONARY_ECHO.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            check(entity != null, "registered entity factory");
            entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);
            entity.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5000);
            check(entity.getMaxHealth() == 10000 && entity.getAttributeValue(Attributes.ATTACK_DAMAGE) == 5000, "Echo attributes exceed vanilla cap");
            entity.setHealth(7);
            entity.applyRelicState(relic, true);
            EchoProgressionConfig.receiveServerMaximum(30);
            entity.applyRelicState(relic, true);
            EchoProgressionConfig.receiveServerMaximum(5000);
            entity.applyRelicState(relic, true);
            check(entity.getHealth() == 7, "config decrease/increase and re-equip do not heal");
            // Non-Echo attribute instances retain their vanilla clamp.
            var ordinary = new net.minecraft.world.entity.ai.attributes.AttributeInstance(Attributes.MAX_HEALTH, ignored -> {});
            ordinary.setBaseValue(10000);
            check(ordinary.getValue() == 1024, "ordinary mob range unchanged");
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info("[EchoProgressionSelfTest] PASS cap, save preservation, XP overflow, five hero curves and isolated attribute range");
        } finally { EchoProgressionConfig.receiveServerMaximum(original); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Echo progression: " + message);
    }
}
