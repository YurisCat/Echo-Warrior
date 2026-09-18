package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.ModDamageTypes1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Single source of truth for fixed 1.21.1 accessory attributes and periodic effects. */
public final class EchoAccessorySystem1211 {
    private static final ResourceLocation ATTACK_ID = ModContent1211.id("accessory_attack");
    private static final ResourceLocation ARMOR_ID = ModContent1211.id("accessory_armor");
    private static final ResourceLocation HEALTH_ID = ModContent1211.id("accessory_health");
    private static final ResourceLocation SPEED_ID = ModContent1211.id("accessory_speed");
    private static final ResourceLocation WATER_SPEED_ID = ModContent1211.id("accessory_water_speed");
    private static final int DAY_NIGHT_HEAL_INTERVAL_TICKS = 50;
    private static final Map<AttackWindow, Boolean> CRITICAL_WINDOWS = new HashMap<>();
    private static final Map<AttackWindow, Boolean> HEAL_WINDOWS = new HashMap<>();

    private EchoAccessorySystem1211() {
    }

    public static void apply(EchoWarriorEntity1211 echo) {
        apply(echo, installedAccessories(echo));
    }

    public static void apply(EchoWarriorEntity1211 echo, List<ItemStack> accessories) {
        LivingEntity living = echo.livingEntity();
        float previousHealth = living.getHealth();
        applyModifier(living.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, attackBonus(accessories),
                AttributeModifier.Operation.ADD_VALUE);
        applyModifier(living.getAttribute(Attributes.ARMOR), ARMOR_ID, armorBonus(accessories),
                AttributeModifier.Operation.ADD_VALUE);
        applyModifier(living.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, maximumHealthBonus(accessories),
                AttributeModifier.Operation.ADD_VALUE);
        applyModifier(living.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, movementMultiplier(accessories) - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        living.setHealth(Math.min(previousHealth, living.getMaxHealth()));
    }

    private static void applyModifier(AttributeInstance attribute, ResourceLocation id, double amount,
                                      AttributeModifier.Operation operation) {
        if (attribute == null) return;
        attribute.removeModifier(id);
        if (Math.abs(amount) > 1.0E-6) attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
    }

    public static void tickLevel(ServerLevel level) {
        long now = level.getGameTime();
        CRITICAL_WINDOWS.keySet().removeIf(key -> key.tick < now - 2L);
        HEAL_WINDOWS.keySet().removeIf(key -> key.tick < now - 2L);
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof EchoWarriorEntity1211 echo) || !echo.livingEntity().isAlive()) continue;
            LivingEntity living = echo.livingEntity();
            List<ItemStack> accessories = installedAccessories(echo);
            applyModifier(living.getAttribute(Attributes.MOVEMENT_SPEED), WATER_SPEED_ID,
                    contains(accessories, ModContent1211.TOMATO_FISH_ACCESSORY) && living.isInWater() ? 0.50 : 0.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            if (!level.dimensionType().hasSkyLight()) continue;
            long dayTime = Math.floorMod(level.getDayTime(), 24000L);
            boolean day = dayTime < 13000L;
            boolean heal = day && contains(accessories, ModContent1211.SUNWHEEL_GARLAND_ACCESSORY)
                    || !day && contains(accessories, ModContent1211.MOONDEW_BOTTLE_ACCESSORY);
            if (heal) tryPeriodicHeal(level, living, now);
        }
    }

    private static void tryPeriodicHeal(ServerLevel level, LivingEntity living, long now) {
        long phase = Math.floorMod(living.getUUID().getLeastSignificantBits(), (long)DAY_NIGHT_HEAL_INTERVAL_TICKS);
        if (Math.floorMod(now, (long)DAY_NIGHT_HEAL_INTERVAL_TICKS) != phase
                || living.getHealth() >= living.getMaxHealth()) return;
        float before = living.getHealth();
        living.heal(1.0F);
        if (living.getHealth() > before) level.sendParticles(ParticleTypes.HEART,
                living.getX(), living.getY() + living.getBbHeight() * 0.75, living.getZ(),
                2, 0.22, 0.15, 0.22, 0.01);
    }

    public static List<ItemStack> installedAccessories(EchoWarriorEntity1211 echo) {
        LivingEntity living = echo.livingEntity();
        if (!(living.level() instanceof ServerLevel level) || echo.getSummonerId() == null) return List.of();
        return EchoBindingSystem1211.accessories(level, echo.getSummonerId());
    }

    public static boolean has(EchoWarriorEntity1211 echo, Item item) {
        return contains(installedAccessories(echo), item);
    }
    public static boolean has(List<ItemStack> accessories, Item item) { return contains(accessories, item); }

    /** Applies direct-attack critical damage before vanilla armor mitigation. */
    public static float modifyOutgoingDamage(LivingEntity victim, ServerLevel level, DamageSource source, float amount) {
        EchoWarriorEntity1211 echo = resolveAttackingEcho(source);
        if (echo == null || amount <= 0.0F || !has(echo, ModContent1211.CRACK_RING_HAMMER_CHARM_ACCESSORY)
                || !isDirectAttack(source, echo)) return amount;
        AttackWindow window = attackWindow(echo, source, level.getGameTime());
        boolean critical = CRITICAL_WINDOWS.computeIfAbsent(window,
                ignored -> echo.livingEntity().getRandom().nextFloat() < 0.30F);
        if (!critical) return amount;
        if (window.feedback.compareAndSet(false, true)) {
            level.playSound(null, victim.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                    SoundSource.PLAYERS, 0.9F, 0.95F + level.getRandom().nextFloat() * 0.12F);
            level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.55,
                    victim.getZ(), 12, 0.25, 0.35, 0.25, 0.2);
        }
        return amount * 2.0F;
    }

    /** Cancels eligible hits when the substitute-doll dodge succeeds. */
    public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof EchoWarriorEntity1211 echo) || amount <= 0.0F
                || !has(echo, ModContent1211.SUBSTITUTE_DOLL_ACCESSORY) || !isDodgeEligible(source)) return true;
        if (victim.getRandom().nextFloat() >= 0.10F) return true;
        echo.onAccessoryDodge(source);
        return false;
    }

    /** Heals an attacking echo once per attack window when the blood-fang proc succeeds. */
    public static void afterDamage(LivingEntity victim, DamageSource source, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F) return;
        EchoWarriorEntity1211 echo = resolveAttackingEcho(source);
        if (echo == null || !has(echo, ModContent1211.BLOOD_PACT_FANG_ACCESSORY)
                || !isDirectAttack(source, echo)) return;
        AttackWindow window = attackWindow(echo, source, echo.livingEntity().level().getGameTime());
        if (HEAL_WINDOWS.putIfAbsent(window, Boolean.TRUE) != null) return;
        if (echo.livingEntity().getRandom().nextFloat() < 0.30F) echo.livingEntity().heal(4.0F);
    }

    private static boolean isDirectAttack(DamageSource source, EchoWarriorEntity1211 echo) {
        if (source.is(ModDamageTypes1211.SPIKED_ARMOR_REFLECTION)
                || source.is(ModDamageTypes1211.BLEEDING)
                || source.is(ModDamageTypes1211.OBSIDIAN_WOUND)
                || source.is(DamageTypeTags.IS_EXPLOSION)) return false;
        Entity direct = source.getDirectEntity();
        if (direct == echo.livingEntity()) return true;
        return direct instanceof Projectile projectile && projectile.getOwner() == echo.livingEntity();
    }

    private static boolean isDodgeEligible(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && !source.is(DamageTypes.IN_FIRE) && !source.is(DamageTypes.CAMPFIRE)
                && !source.is(DamageTypes.ON_FIRE) && !source.is(DamageTypes.LAVA)
                && !source.is(DamageTypes.HOT_FLOOR) && !source.is(DamageTypes.IN_WALL)
                && !source.is(DamageTypes.CRAMMING) && !source.is(DamageTypes.DROWN)
                && !source.is(DamageTypes.STARVE) && !source.is(DamageTypes.FALL)
                && !source.is(DamageTypes.FLY_INTO_WALL) && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && !source.is(DamageTypes.MAGIC) && !source.is(DamageTypes.WITHER)
                && !source.is(DamageTypes.DRY_OUT) && !source.is(DamageTypes.FREEZE)
                && !source.is(DamageTypes.OUTSIDE_BORDER) && !source.is(DamageTypes.GENERIC_KILL)
                && !source.is(ModDamageTypes1211.BLEEDING) && !source.is(ModDamageTypes1211.OBSIDIAN_WOUND);
    }

    public static EchoWarriorEntity1211 resolveAttackingEcho(DamageSource source) {
        if (source.getEntity() instanceof EchoWarriorEntity1211 echo) return echo;
        if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof EchoWarriorEntity1211 echo) return echo;
        return null;
    }

    private static AttackWindow attackWindow(EchoWarriorEntity1211 echo, DamageSource source, long tick) {
        Entity direct = source.getDirectEntity();
        UUID directUuid = direct == null ? echo.livingEntity().getUUID() : direct.getUUID();
        AttackWindow probe = new AttackWindow(echo.livingEntity().getUUID(), directUuid, tick);
        for (AttackWindow existing : CRITICAL_WINDOWS.keySet()) if (existing.equals(probe)) return existing;
        for (AttackWindow existing : HEAL_WINDOWS.keySet()) if (existing.equals(probe)) return existing;
        return probe;
    }

    public static void reflectMeleeDamage(EchoWarriorEntity1211 echo, ServerLevel level,
                                          DamageSource source, float previousHealth) {
        float actualHealthDamage = previousHealth - echo.livingEntity().getHealth();
        if (actualHealthDamage <= 0.0F || source.is(ModDamageTypes1211.SPIKED_ARMOR_REFLECTION)
                || source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypes.MAGIC) || source.is(DamageTypeTags.BYPASSES_ARMOR)) return;
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker) || source.getDirectEntity() != attacker
                || !has(echo, ModContent1211.SPIKED_ARMOR_ACCESSORY)) return;
        LivingEntity direct = echo.livingEntity();
        LivingEntity owner = echo.ownerEntity();
        livingAttacker.hurt(ModDamageTypes1211.source(level, ModDamageTypes1211.SPIKED_ARMOR_REFLECTION,
                direct, owner == null ? direct : owner), actualHealthDamage);
    }

    public static double proactiveRange(EchoWarriorEntity1211 echo, double base, boolean waiting) {
        return proactiveRange(installedAccessories(echo), base, waiting);
    }
    public static double proactiveRange(SimpleContainer contents, double base, boolean waiting) {
        return proactiveRange(contents.getItems(), base, waiting);
    }
    public static double proactiveRange(List<ItemStack> accessories, double base, boolean waiting) {
        double adjusted = contains(accessories, ModContent1211.BATTLE_BLINDFOLD_ACCESSORY) ? base * 0.25
                : contains(accessories, ModContent1211.HAWKEYE_LENS_ACCESSORY) ? base * 1.50 : base;
        return Math.min(adjusted, waiting ? 8.0 : 32.0);
    }

    public static double attackBonus(SimpleContainer contents) { return attackBonus(contents.getItems()); }
    public static double attackBonus(ItemStack summoner) { return attackBonus(EchoSummonerItem1211.accessoryStacks(summoner)); }
    public static double attackBonus(List<ItemStack> accessories) {
        double value = 0.0;
        if (contains(accessories, ModContent1211.BATTLE_WORN_WHETSTONE_ACCESSORY)) value += 2.0;
        if (contains(accessories, ModContent1211.MOUNTAIN_BURDEN_BLADE_ACCESSORY)) value += 4.0;
        if (contains(accessories, ModContent1211.FRACTURED_CRYSTAL_BLADE_ACCESSORY)) value += 4.0;
        if (contains(accessories, ModContent1211.TWIN_OATH_BADGE_ACCESSORY)) value += 1.0;
        if (contains(accessories, ModContent1211.BATTLE_BLINDFOLD_ACCESSORY)) value += 3.0;
        if (contains(accessories, ModContent1211.MEMORY_RITUAL_KNIFE_ACCESSORY)) value += 2.0;
        if (contains(accessories, ModContent1211.PEACEMAKER_ACCESSORY)) value -= 4.0;
        return value;
    }

    public static double armorBonus(SimpleContainer contents) { return armorBonus(contents.getItems()); }
    public static double armorBonus(ItemStack summoner) { return armorBonus(EchoSummonerItem1211.accessoryStacks(summoner)); }
    public static double armorBonus(List<ItemStack> accessories) {
        double value = 0.0;
        if (contains(accessories, ModContent1211.PLATE_ARMOR_ACCESSORY)) value += 2.0;
        if (contains(accessories, ModContent1211.CHAINMAIL_ARMOR_ACCESSORY)) value += 4.0;
        if (contains(accessories, ModContent1211.SPIKED_ARMOR_ACCESSORY)) value += 1.0;
        if (contains(accessories, ModContent1211.FRACTURED_CRYSTAL_BLADE_ACCESSORY)) value -= 6.0;
        if (contains(accessories, ModContent1211.TWIN_OATH_BADGE_ACCESSORY)) value += 2.0;
        return value;
    }

    public static double maximumHealthBonus(SimpleContainer contents) { return maximumHealthBonus(contents.getItems()); }
    public static double maximumHealthBonus(ItemStack summoner) { return maximumHealthBonus(EchoSummonerItem1211.accessoryStacks(summoner)); }
    public static double maximumHealthBonus(List<ItemStack> accessories) {
        double value = 0.0;
        if (contains(accessories, ModContent1211.HEART_SPROUT_AMBER_ACCESSORY)) value += 6.0;
        if (contains(accessories, ModContent1211.FEAST_HAM_ACCESSORY)) value += 12.0;
        if (contains(accessories, ModContent1211.PEACEMAKER_ACCESSORY)) value += 20.0;
        if (contains(accessories, ModContent1211.HOLLOW_BIRD_BONE_ACCESSORY)) value -= 8.0;
        return value;
    }

    public static double movementMultiplier(SimpleContainer contents) { return movementMultiplier(contents.getItems()); }
    public static double movementMultiplier(ItemStack summoner) { return movementMultiplier(EchoSummonerItem1211.accessoryStacks(summoner)); }
    public static double movementMultiplier(List<ItemStack> accessories) {
        double value = 1.0;
        if (contains(accessories, ModContent1211.CHAINMAIL_ARMOR_ACCESSORY)) value -= 0.15;
        if (contains(accessories, ModContent1211.MOUNTAIN_BURDEN_BLADE_ACCESSORY)) value -= 0.20;
        if (contains(accessories, ModContent1211.FEAST_HAM_ACCESSORY)) value -= 0.15;
        if (contains(accessories, ModContent1211.WINDCHASER_FEATHER_ACCESSORY)) value += 0.10;
        if (contains(accessories, ModContent1211.HOLLOW_BIRD_BONE_ACCESSORY)) value += 0.20;
        return Math.max(0.25, value);
    }

    public static int worldExperienceReward(int base, boolean boosted) {
        if (base <= 0) return 0;
        return Math.max(1, boosted ? Math.round(base * 1.5F) : base);
    }
    public static int growthExperienceReward(int base, boolean boosted) {
        if (base <= 0) return 0;
        return Math.max(1, boosted ? Math.round(base * 1.5F) : base);
    }
    public static float victorHealAmount(float decoratedMaximumHealth) {
        return Math.max(0.0F, decoratedMaximumHealth * 0.10F);
    }

    private static boolean contains(List<ItemStack> accessories, Item item) {
        return accessories.stream().anyMatch(stack -> stack.is(item));
    }

    private static final class AttackWindow {
        private final UUID echo;
        private final UUID direct;
        private final long tick;
        private final AtomicBoolean feedback = new AtomicBoolean();

        private AttackWindow(UUID echo, UUID direct, long tick) {
            this.echo = echo;
            this.direct = direct;
            this.tick = tick;
        }

        @Override public boolean equals(Object other) {
            return other instanceof AttackWindow window && tick == window.tick
                    && echo.equals(window.echo) && direct.equals(window.direct);
        }

        @Override public int hashCode() { return java.util.Objects.hash(echo, direct, tick); }
    }
}
