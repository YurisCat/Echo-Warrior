package com.yuriscat.echowarrior.compat.entity;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModDamageTypes1201;
import com.yuriscat.echowarrior.compat.ModEffects1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicProgress1201;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1201;
import com.yuriscat.echowarrior.compat.item.EchoSkillScaling1201;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Registered entities, real attribute/effect/damage Mixins, save payloads and range predicates. */
public final class SkillUpdateSelfTest1201 {
    private SkillUpdateSelfTest1201() { }

    public static void run(ServerLevel level) {
        List<LivingEntity> fixtures = new ArrayList<>();
        try {
            long now = level.getGameTime();
            var owner = new ServerPlayer(level.getServer(), level,
                    new com.mojang.authlib.GameProfile(UUID.randomUUID(), "SkillUpdateTest"));
            LivingEntity villager = create(level, "minecraft:villager", fixtures);
            LivingEntity golem = create(level, "minecraft:iron_golem", fixtures);
            LivingEntity cow = create(level, "minecraft:cow", fixtures);
            LivingEntity wolf = create(level, "minecraft:wolf", fixtures);
            ((TamableAnimal)wolf).tame(owner);
            LivingEntity horse = create(level, "minecraft:horse", fixtures);
            ((net.minecraft.world.entity.animal.horse.AbstractHorse)horse).setTamed(true);
            LivingEntity zombie = create(level, "minecraft:zombie", fixtures);
            check(EchoAssistPolicy1201.isProtected(wolf) && EchoAssistPolicy1201.isProtected(horse), "offline-owned pet and tamed mount");
            check(!EchoAssistPolicy1201.isProtected(cow), "farm animal remains a valid target");
            check(!EchoAssistPolicy1201.allowsStoredTarget(level, wolf, null, owner.getUUID(), 2),
                    "unloaded source still cannot hurt its owner's pet in All");
            check(!EchoAssistPolicy1201.allowsStoredTarget(level, villager, null, null, 1)
                    && EchoAssistPolicy1201.allowsStoredTarget(level, villager, null, null, 2),
                    "unloaded source still respects Assist mode");

            for (String hero : new String[]{"roman_legionary", "aztec_warrior", "egyptian_archer", "guandao_warrior", "japanese_samurai"}) {
                EchoWarriorEntity1201 echo = (EchoWarriorEntity1201)create(level, "echo_warrior:" + hero + "_echo", fixtures);
                ItemStack relic = new ItemStack(BuiltInRegistries.ITEM.get(EchoWarrior1201.id(hero + "_relic")));
                check(EchoRelicState1201.assistMode(relic) == EchoRelicState1201.AssistMode.SAFE, "old relic defaults to Protect");
                echo.bindTo(owner, UUID.randomUUID());
                echo.applyRelicState(relic, false);
                Mob mob = (Mob)echo.livingEntity();
                for (EchoRelicState1201.AssistMode mode : EchoRelicState1201.AssistMode.values()) {
                    EchoRelicState1201.setAssistMode(relic, mode);
                    check(EchoRelicState1201.assistMode(relic.copy()) == mode, "relic copy retains Assist");
                    echo.applyRelicState(relic, false);
                    check(echo.assistMode() == mode && EchoAssistPolicy1201.followsOwner(echo) == (mode != EchoRelicState1201.AssistMode.OFF),
                            hero + " owner-follow switch");
                    for (LivingEntity protectedTarget : List.of(villager, golem, wolf, horse)) {
                        check(EchoAssistPolicy1201.allowsTarget(echo, protectedTarget) == (mode == EchoRelicState1201.AssistMode.ALL),
                                hero + " protected list in " + mode);
                        if (mode != EchoRelicState1201.AssistMode.ALL) {
                            check(!mob.canAttack(protectedTarget), "target acquisition guard");
                            float before = protectedTarget.getHealth();
                            protectedTarget.invulnerableTime = 0;
                            protectedTarget.hurt(ModDamageTypes1201.source(level,
                                    ModDamageTypes1201.SPIKED_ARMOR_REFLECTION, mob, owner), 3);
                            check(protectedTarget.getHealth() == before, "counterattack cannot bypass protection");
                        }
                    }
                }
                mob.getAttribute(Attributes.ARMOR).setBaseValue(100);
                check(close(mob.getAttributeValue(Attributes.ARMOR), 100), hero + " armor above vanilla cap");
            }
            zombie.getAttribute(Attributes.ARMOR).setBaseValue(100);
            check(close(zombie.getAttributeValue(Attributes.ARMOR), 30), "ordinary armor cap preserved");
            zombie.getAttribute(Attributes.ARMOR).setBaseValue(0);
            testBleeding(level, now, zombie);
            testFormation(level, fixtures);
            testLiveLegionArmor(level, fixtures);
            testCurse(level, fixtures);
            testReach(level, zombie, cow);
            testArrows(level, fixtures, villager);
            EchoCombatState1201.of(zombie).apply(true, new EchoCombatState1201.Bleed(UUID.randomUUID(), null, null, 7, now + 120, 1), now);
            LivingEntity restored = create(level, "minecraft:zombie", fixtures);
            loadEntity(level, restored, saveEntity(level, zombie));
            check(EchoCombatState1201.of(restored).strongest(true, now).damage() == 7
                    && EchoCombatState1201.of(restored).reachRestricted(now), "real entity NBT retains bleed and hidden timer");
            check(close(EchoSkillScaling1201.legionArmor(100), 22) && close(EchoSkillScaling1201.legionArmor(200), 32), "armor growth beyond 100");
            check(close(EchoSkillScaling1201.legionRelease(100), 2), "200% accumulated release at level 100");
            check(close(EchoSkillScaling1201.remainingArmor(EchoSkillScaling1201.conePenetration(150)), 0)
                    && close(EchoSkillScaling1201.penetrationDamage(EchoSkillScaling1201.conePenetration(150)), 1.1), "overflow penetration");
            check(EchoSkillScaling1201.conePierceChance(200) > 1, "pierce chance has no artificial cap");
            EchoWarrior1201.LOGGER.info("SKILL UPDATE SELFTEST PASSED heroes=5 assist=3 sources=independent curse=flat reach=contact armor=uncapped");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Skill update reflection fixture failed: " + failure + "; cause=" + failure.getCause(), failure);
        } finally {
            for (LivingEntity fixture : fixtures) fixture.discard();
        }
    }

    private static void testBleeding(ServerLevel level, long now, LivingEntity target) {
        for (boolean wound : new boolean[]{false, true}) {
            EchoCombatState1201 state = EchoCombatState1201.of(target);
            UUID strong = UUID.randomUUID(), weak = UUID.randomUUID();
            state.apply(wound, new EchoCombatState1201.Bleed(strong, null, null, 8, now + 40, 1), now);
            state.apply(wound, new EchoCombatState1201.Bleed(weak, null, null, 3, now + 80, 1), now);
            check(state.strongest(wound, now + 39).damage() == 8, "strongest active source");
            state.apply(wound, new EchoCombatState1201.Bleed(weak, null, null, 4, now + 100, 1), now + 39);
            check(state.strongest(wound, now + 41).damage() == 4, "weak hit cannot extend strong source");
            EchoCombatState1201 restored = new EchoCombatState1201();
            restored.load(state.save());
            check(restored.strongest(wound, now + 41).damage() == 4 && restored.strongest(wound, now + 101) == null,
                    "per-source expiry and snapshot round trip");
            state.clear(wound);
            state.apply(wound, new EchoCombatState1201.Bleed(strong, null, null, 3, now + 80, 1), now);
            CompoundTag tag = state.save();
            tag.putLong(wound ? "NextObsidian" : "NextLeaf", -1L); // Due even at game time 0/1; zero means unset.
            state.load(tag);
            target.invulnerableTime = 0;
            target.setHealth(target.getMaxHealth());
            float before = target.getHealth();
            boolean ticked = EchoBleeding1201.tick(level, target, wound);
            check(ticked && close(before - target.getHealth(), 3), "real periodic damage wound=" + wound + " time=" + now + " actual=" + (before - target.getHealth()));
            check(!EchoBleeding1201.tick(level, target, wound), "refresh never adds a second tick at same time");
            var effect = wound ? ModEffects1201.OBSIDIAN_WOUND : ModEffects1201.BLEEDING;
            target.addEffect(new MobEffectInstance(effect, 80));
            target.removeEffect(effect);
            check(state.strongest(wound, now) == null, "cleansing cannot resurrect old sources on reapplication");
            state.clear(wound);
        }
    }

    private static void testFormation(ServerLevel level, List<LivingEntity> fixtures) {
        LivingEntity recipient = create(level, "echo_warrior:aztec_warrior_echo", fixtures);
        var attack = recipient.getAttribute(Attributes.ATTACK_DAMAGE);
        attack.setBaseValue(10);
        attack.addTransientModifier(com.yuriscat.echowarrior.compat.LegacyAttributes1201.create(EchoWarrior1201.id("skill_test_add"), 5, AttributeModifier.Operation.ADDITION));
        attack.addTransientModifier(com.yuriscat.echowarrior.compat.LegacyAttributes1201.create(EchoWarrior1201.id("skill_test_multi"), 0.2, AttributeModifier.Operation.MULTIPLY_TOTAL));
        recipient.addEffect(new MobEffectInstance(ModEffects1201.WEAPONS_RAISED, 100));
        check(close(attack.getValue(), 23.4), "formation includes other additive and multiplicative gains");
        recipient.addEffect(new MobEffectInstance(ModEffects1201.WEAPONS_RAISED, 100));
        check(close(attack.getValue(), 23.4), "same-name aura does not stack");
        recipient.removeEffect(ModEffects1201.WEAPONS_RAISED);
        check(close(attack.getValue(), 18), "formation removes cleanly");
    }


    private static void testLiveLegionArmor(ServerLevel level, List<LivingEntity> fixtures) throws ReflectiveOperationException {
        var roman = (RomanLegionaryEchoEntity1201)create(level, "echo_warrior:roman_legionary_echo", fixtures);
        ItemStack relic = new ItemStack(BuiltInRegistries.ITEM.get(EchoWarrior1201.id("roman_legionary_relic")));
        EchoRelicProgress1201.addExperience(relic, 11385);
        var until = RomanLegionaryEchoEntity1201.class.getDeclaredField("legionEnduresUntil");
        until.setAccessible(true);
        until.setLong(roman, level.getGameTime() + 100);
        roman.applyRelicState(relic, false);
        check(close(roman.getAttributeValue(Attributes.ARMOR), 30), "active level-100 Legion armor");
        EchoRelicProgress1201.addExperience(relic, 215);
        roman.applyRelicState(relic, false);
        check(close(roman.getAttributeValue(Attributes.ARMOR), 30.1), "level-up refreshes active armor above vanilla cap");
    }

    private static void testCurse(ServerLevel level, List<LivingEntity> fixtures) throws ReflectiveOperationException {
        var aztec = (AztecWarriorEchoEntity1201)create(level, "echo_warrior:aztec_warrior_echo", fixtures);
        LivingEntity target = create(level, "minecraft:zombie", fixtures);
        aztec.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100));
        aztec.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100));
        target.setRemainingFireTicks(100);
        target.setTicksFrozen(100);
        EchoCombatState1201.of(target).restrictReach(level.getGameTime());
        check(aztec.curseEffectCount(target) == 4, "types counted once; fire, freeze and hidden reach excluded");
        target.getAttribute(Attributes.ARMOR).setBaseValue(30);
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100));
        ItemStack relic = new ItemStack(BuiltInRegistries.ITEM.get(EchoWarrior1201.id("aztec_warrior_relic")));
        EchoRelicProgress1201.addExperience(relic, 11385);
        check(EchoRelicProgress1201.level(relic) == 100, "curse fixture level");
        var method = AztecWarriorEchoEntity1201.class.getDeclaredMethod("applyOffensiveCurse", ServerLevel.class, LivingEntity.class, ItemStack.class);
        method.setAccessible(true);
        target.setDeltaMovement(0.03, 0, -0.02);
        Vec3 beforeMotion = target.getDeltaMovement();
        float before = target.getHealth();
        method.invoke(aztec, level, target, relic);
        check(close(before - target.getHealth(), 6.4), "flat 8 magic bypasses armor, retains resistance");
        check(target.getDeltaMovement().equals(beforeMotion), "curse creates no extra knockback");
        LivingEntity witch = create(level, "minecraft:witch", fixtures);
        witch.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100));
        witch.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100));
        before = witch.getHealth();
        method.invoke(aztec, level, witch, relic);
        check(close(before - witch.getHealth(), 1.2), "curse respects the witch's native magic resistance");
    }

    private static void testReach(ServerLevel level, LivingEntity zombie, LivingEntity target) {
        zombie.setPos(0, 100, 0);
        target.setPos(1.1, 100, 0);
        Mob mob = (Mob)zombie;
        check(mob.isWithinMeleeAttackRange(target), "range fixture initially reachable");
        EchoCombatState1201.of(zombie).restrictReach(level.getGameTime());
        check(!mob.isWithinMeleeAttackRange(target), "80% external reach reduction");
        check(!EchoMeleeReach1201.allowsDamage(target, level.damageSources().mobAttack(mob)), "delayed hit guarded");
        target.setPos(0.5, 100, 0);
        check(mob.isWithinMeleeAttackRange(target), "body contact still permits melee");
        check(EchoMeleeReach1201.allowsDamage(target, level.damageSources().mobAttack(mob)), "contact is not invulnerability");
        check(!EchoCombatState1201.of(zombie).reachRestricted(level.getGameTime() + 40), "hidden effect expires at two seconds");
    }

    private static void testArrows(ServerLevel level, List<LivingEntity> fixtures, LivingEntity villager)
            throws ReflectiveOperationException {
        var archer = (EgyptianArcherEchoEntity1201)create(level, "echo_warrior:egyptian_archer_echo", fixtures);
        var type = BuiltInRegistries.ENTITY_TYPE.get(EchoWarrior1201.id("egyptian_archer_arrow"));
        var launchLevel = EgyptianArcherArrowEntity1201.class.getDeclaredField("launchLevel");
        launchLevel.setAccessible(true);
        for (int value : new int[]{100, 150}) {
            var arrow = (EgyptianArcherArrowEntity1201)type.create(level);
            try {
                arrow.configure(archer, EchoRelicState1201.EgyptianArrowMode.CONE, 10, false);
                launchLevel.setInt(arrow, value);
                archer.discard(); // Older Projectile.setOwner(null) does not clear its cached owner.
                check(arrow.getOwner() == null, "source removal fixture really resolves no projectile owner");
                float friendlyHealth = villager.getHealth();
                arrow.onHitEntity(new net.minecraft.world.phys.EntityHitResult(villager));
                check(villager.getHealth() == friendlyHealth, "in-flight ownerless arrow preserves Protect");
                LivingEntity target = create(level, "minecraft:zombie", fixtures);
                target.getAttribute(Attributes.ARMOR).setBaseValue(20);
                float before = target.getHealth();
                arrow.onHitEntity(new net.minecraft.world.phys.EntityHitResult(target));
                check(close(before - target.getHealth(), value == 100 ? 9.76 : 11),
                        "real cone armor and overflow at level " + value + " actual=" + (before - target.getHealth()));
            } finally { arrow.discard(); }
        }
        var arrow = (EgyptianArcherArrowEntity1201)type.create(level);
        try {
            arrow.configure(archer, EchoRelicState1201.EgyptianArrowMode.LEAF, 10, false);
            archer.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(100);
            archer.discard();
            check(arrow.getOwner() == null, "leaf source removal fixture");
            LivingEntity target = create(level, "minecraft:zombie", fixtures);
            arrow.onHitEntity(new net.minecraft.world.phys.EntityHitResult(target));
            check(EchoCombatState1201.of(target).strongest(false, level.getGameTime()).damage() == 2,
                    "leaf uses launch attack, including after source unloading");
        } finally { arrow.discard(); }
    }

    private static CompoundTag saveEntity(ServerLevel level, LivingEntity entity) {
        return entity.saveWithoutId(new CompoundTag());
    }
    private static void loadEntity(ServerLevel level, LivingEntity entity, CompoundTag tag) {
        entity.load(tag);
    }

    private static LivingEntity create(ServerLevel level, String id, List<LivingEntity> fixtures) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(new net.minecraft.resources.ResourceLocation(id));
        var entity = (LivingEntity)type.create(level);
        check(entity != null, "registered factory " + id);
        entity.setPos(0, 100, 0);
        fixtures.add(entity);
        return entity;
    }
    private static boolean close(double a, double b) { return Math.abs(a - b) < 0.001; }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Skill update: " + message);
    }
}
