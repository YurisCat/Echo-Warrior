package com.yuriscat.echowarrior.compat.item;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EchoTalentSystem1211 {
    public static final double OWNER_SUPPORT_RANGE = 32.0;
    private static final long BAD_TEMPER_TICKS = 100L;
    private static final net.minecraft.resources.ResourceLocation CONDITIONAL_SPEED_ID =
            ModContent1211.id("talent_conditional_speed");
    private static final Map<UUID, Long> BAD_TEMPER_UNTIL = new HashMap<>();

    private static final TagKey<Biome> WOODLAND = biomeTag("talent_affinity/woodland");
    private static final TagKey<Biome> WASTELAND = biomeTag("talent_affinity/wasteland");
    private static final TagKey<Biome> COLD = biomeTag("talent_affinity/cold");
    private static final TagKey<Biome> WATERS = biomeTag("talent_affinity/waters");
    private static final TagKey<Biome> UNDERGROUND = biomeTag("talent_affinity/underground");

    private EchoTalentSystem1211() {
    }

    public static float modifyOutgoingDamage(
            EchoWarriorEntity1211 attacker,
            LivingEntity victim,
            ServerLevel level,
            float amount
    ) {
        ItemStack relic = attacker.activeRelic();
        if (relic.isEmpty() || amount <= 0.0F) return amount;
        LivingEntity living = attacker.livingEntity();
        float bonus = 0.0F;
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.UNDEAD_SLAYER)
                && victim.getType().builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_SMITE)) bonus += 0.20F;
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.ARTHROPOD_SLAYER)
                && victim.getType().builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) bonus += 0.20F;
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.RAIDER_SLAYER)
                && victim.getType().builtInRegistryHolder().is(EntityTypeTags.RAIDERS)) bonus += 0.20F;
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.GIANT_SLAYER) && victim.getMaxHealth() > 50.0F) bonus += 0.15F;
        ResourceKey<Level> dimension = level.dimension();
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.NETHER_REAPER) && Level.NETHER.equals(dimension)) bonus += 0.15F;
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.END_REAPER) && Level.END.equals(dimension)) bonus += 0.15F;
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.OTHERWORLD_REAPER) && !Level.OVERWORLD.equals(dimension)) bonus += 0.10F;
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.BIOME_AFFINITY)
                && matchesBiomeAffinity(living, relic)) bonus += 0.10F;
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.NIGHT_OWL) && isNaturalNight(level)) bonus += 0.15F;
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.PERFECTIONIST)
                && isFullHealth(living)) bonus += 0.15F;
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.BAD_TEMPER)
                && BAD_TEMPER_UNTIL.getOrDefault(living.getUUID(), Long.MIN_VALUE) >= level.getGameTime()) bonus += 0.15F;
        if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.LAST_STAND) && living.getMaxHealth() > 0.0F) {
            bonus += (1.0F - living.getHealth() / living.getMaxHealth()) * 0.25F;
        }
        return amount * (1.0F + Math.max(0.0F, bonus));
    }

    public static float modifyFinalIncomingDamage(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof EchoWarriorEntity1211 echo) || amount <= 0.0F
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
        ItemStack relic = echo.activeRelic();
        if (relic.isEmpty() || !EchoRelicState1211.hasTrait(relic, EchoTrait1211.UNYIELDING)
                || victim.getMaxHealth() <= 0.0F) return amount;
        float missing = Math.max(0.0F, Math.min(1.0F, 1.0F - victim.getHealth() / victim.getMaxHealth()));
        return amount * (1.0F - missing * 0.30F);
    }

    public static int attackIntervalTicks(EchoWarriorEntity1211 echo, ItemStack relic) {
        int interval = EchoRelicState1211.attackIntervalTicks(relic);
        if (!EchoRelicState1211.hasTrait(relic, EchoTrait1211.PERFECTIONIST)
                || !isFullHealth(echo.livingEntity())) return interval;
        int minimum = echo.heroType() == com.yuriscat.echowarrior.compat.EchoHeroType1211.JAPANESE_SAMURAI
                || echo.heroType() == com.yuriscat.echowarrior.compat.EchoHeroType1211.EGYPTIAN_ARCHER ? 24 : 4;
        return Math.max(minimum, Math.round(interval / 1.10F));
    }

    public static boolean hasNearbyTalent(Player player, EchoTrait1211 trait) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive()) return false;
        double rangeSqr = OWNER_SUPPORT_RANGE * OWNER_SUPPORT_RANGE;
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (!(entity instanceof EchoWarriorEntity1211 echo) || !echo.livingEntity().isAlive()) continue;
            if (player.getUUID().equals(echo.getOwnerUUID())
                    && echo.livingEntity().distanceToSqr(player) <= rangeSqr
                    && EchoRelicState1211.hasTrait(echo.activeRelic(), trait)) return true;
        }
        return false;
    }

    public static void afterDamage(LivingEntity victim, DamageSource source, float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F || source.getEntity() == null
                || !(victim instanceof EchoWarriorEntity1211 echo)) return;
        ItemStack relic = echo.activeRelic();
        if (!relic.isEmpty() && EchoRelicState1211.hasTrait(relic, EchoTrait1211.BAD_TEMPER)) {
            BAD_TEMPER_UNTIL.put(victim.getUUID(), victim.level().getGameTime() + BAD_TEMPER_TICKS);
        }
    }

    public static void afterDeath(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level) || !(source.getEntity() instanceof ServerPlayer player)
                || victim == player || victim.wasExperienceConsumed() || !victim.shouldDropExperience()
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
                || !hasNearbyTalent(player, EchoTrait1211.MENTOR)) return;
        int reward = victim.getExperienceReward(level, player);
        int extra = TalentExperienceHolder1211.of(player).echoWarrior1211$consumeMentorBonus(reward);
        if (extra > 0) ExperienceOrb.award(level, victim.position(), extra);
    }

    public static void tickLevel(ServerLevel level) {
        long now = level.getGameTime();
        if (now % 10L != 0L) return;
        if (now % 200L == 0L) BAD_TEMPER_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (!(entity instanceof EchoWarriorEntity1211 echo) || !echo.livingEntity().isAlive()) continue;
            ItemStack relic = echo.activeRelic();
            float speedBonus = 0.0F;
            if (!relic.isEmpty()) {
                if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.BIOME_AFFINITY)
                        && matchesBiomeAffinity(echo.livingEntity(), relic)) speedBonus += 0.10F;
                if (EchoRelicState1211.hasTrait(relic, EchoTrait1211.NIGHT_OWL)
                        && isNaturalNight(level)) speedBonus += 0.10F;
            }
            applySpeedModifier(echo.livingEntity(), speedBonus);
        }
    }

    public static void clearForSelfTest(UUID entityId) {
        BAD_TEMPER_UNTIL.remove(entityId);
    }

    private static void applySpeedModifier(LivingEntity living, float amount) {
        AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        speed.removeModifier(CONDITIONAL_SPEED_ID);
        if (amount > 0.0F) speed.addTransientModifier(new AttributeModifier(
                CONDITIONAL_SPEED_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static boolean isFullHealth(LivingEntity entity) {
        return entity.getHealth() >= entity.getMaxHealth();
    }

    private static boolean isNaturalNight(ServerLevel level) {
        if (level.dimensionType().hasFixedTime() || !level.dimensionType().hasSkyLight()) return false;
        long time = Math.floorMod(level.getDayTime(), 24000L);
        return time >= 13000L && time < 23000L;
    }

    private static boolean matchesBiomeAffinity(LivingEntity entity, ItemStack relic) {
        if (!(entity.level() instanceof ServerLevel level) || !Level.OVERWORLD.equals(level.dimension())) return false;
        Holder<Biome> biome = level.getBiome(entity.blockPosition());
        return classifyBiome(biome) == EchoRelicState1211.biomeAffinity(relic);
    }

    private static EchoBiomeAffinity1211 classifyBiome(Holder<Biome> biome) {
        if (biome.is(UNDERGROUND)) return EchoBiomeAffinity1211.UNDERGROUND;
        if (biome.is(COLD)) return EchoBiomeAffinity1211.COLD;
        if (biome.is(WATERS)) return EchoBiomeAffinity1211.WATERS;
        if (biome.is(WASTELAND)) return EchoBiomeAffinity1211.WASTELAND;
        if (biome.is(WOODLAND)) return EchoBiomeAffinity1211.WOODLAND;
        return EchoBiomeAffinity1211.OPENLAND;
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, ModContent1211.id(path));
    }
}
