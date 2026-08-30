package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Runtime evaluation for conditional Echo talents and owner support talents. */
public final class EchoTalentSystem {
	public static final double OWNER_SUPPORT_RANGE = 32.0;
	private static final long BAD_TEMPER_TICKS = 100L;
	private static final Identifier CONDITIONAL_SPEED_ID = EchoWarrior.id("talent_conditional_speed");
	private static final Map<UUID, Long> BAD_TEMPER_UNTIL = new HashMap<>();

	private static final TagKey<Biome> WOODLAND = biomeTag("talent_affinity/woodland");
	private static final TagKey<Biome> WASTELAND = biomeTag("talent_affinity/wasteland");
	private static final TagKey<Biome> COLD = biomeTag("talent_affinity/cold");
	private static final TagKey<Biome> WATERS = biomeTag("talent_affinity/waters");
	private static final TagKey<Biome> UNDERGROUND = biomeTag("talent_affinity/underground");

	private EchoTalentSystem() {
	}

	public static void initialize() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(EchoTalentSystem::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(EchoTalentSystem::afterDeath);
		ServerTickEvents.END_LEVEL_TICK.register(EchoTalentSystem::tickLevel);
	}

	public static float modifyOutgoingDamage(LivingEntity victim, ServerLevel level, DamageSource source, float amount) {
		EchoWarriorEntity echo = EchoAccessorySystem.resolveAttackingEcho(source);
		if (echo == null || amount <= 0.0F) return amount;
		ItemStack relic = echo.activeRelic();
		if (relic.isEmpty()) return amount;

		LivingEntity attacker = echo.livingEntity();
		float bonus = 0.0F;
		if (EchoRelicState.hasTrait(relic, EchoTrait.UNDEAD_SLAYER)
				&& victim.getType().builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_SMITE)) bonus += 0.20F;
		if (EchoRelicState.hasTrait(relic, EchoTrait.ARTHROPOD_SLAYER)
				&& victim.getType().builtInRegistryHolder().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) bonus += 0.20F;
		if (EchoRelicState.hasTrait(relic, EchoTrait.RAIDER_SLAYER)
				&& victim.getType().builtInRegistryHolder().is(EntityTypeTags.RAIDERS)) bonus += 0.20F;
		if (EchoRelicState.hasTrait(relic, EchoTrait.GIANT_SLAYER) && victim.getMaxHealth() > 50.0F) bonus += 0.15F;
		ResourceKey<Level> dimension = level.dimension();
		if (EchoRelicState.hasTrait(relic, EchoTrait.NETHER_REAPER) && Level.NETHER.equals(dimension)) bonus += 0.15F;
		if (EchoRelicState.hasTrait(relic, EchoTrait.END_REAPER) && Level.END.equals(dimension)) bonus += 0.15F;
		if (EchoRelicState.hasTrait(relic, EchoTrait.OTHERWORLD_REAPER) && !Level.OVERWORLD.equals(dimension)) bonus += 0.10F;
		if (EchoRelicState.hasTrait(relic, EchoTrait.BIOME_AFFINITY) && matchesBiomeAffinity(attacker, relic)) bonus += 0.10F;
		if (EchoRelicState.hasTrait(relic, EchoTrait.NIGHT_OWL) && isNaturalNight(level)) bonus += 0.15F;
		if (EchoRelicState.hasTrait(relic, EchoTrait.PERFECTIONIST) && isFullHealth(attacker)) bonus += 0.15F;
		if (EchoRelicState.hasTrait(relic, EchoTrait.BAD_TEMPER)
				&& BAD_TEMPER_UNTIL.getOrDefault(attacker.getUUID(), Long.MIN_VALUE) >= level.getGameTime()) bonus += 0.15F;
		if (EchoRelicState.hasTrait(relic, EchoTrait.LAST_STAND) && attacker.getMaxHealth() > 0.0F) {
			bonus += (1.0F - attacker.getHealth() / attacker.getMaxHealth()) * 0.25F;
		}
		return amount * (1.0F + Math.max(0.0F, bonus));
	}

	public static float modifyFinalIncomingDamage(LivingEntity victim, DamageSource source, float amount) {
		if (!(victim instanceof EchoWarriorEntity echo) || amount <= 0.0F
				|| source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return amount;
		ItemStack relic = echo.activeRelic();
		if (relic.isEmpty() || !EchoRelicState.hasTrait(relic, EchoTrait.UNYIELDING) || victim.getMaxHealth() <= 0.0F) return amount;
		float missing = Math.clamp(1.0F - victim.getHealth() / victim.getMaxHealth(), 0.0F, 1.0F);
		return amount * (1.0F - missing * 0.30F);
	}

	public static int attackIntervalTicks(EchoWarriorEntity echo, ItemStack relic) {
		int interval = EchoRelicState.attackIntervalTicks(relic);
		if (!EchoRelicState.hasTrait(relic, EchoTrait.PERFECTIONIST) || !isFullHealth(echo.livingEntity())) return interval;
		int minimum = echo.heroType() == EchoHeroType.JAPANESE_SAMURAI ? 24
				: echo.heroType() == EchoHeroType.EGYPTIAN_ARCHER ? 24 : 4;
		return Math.max(minimum, Math.round(interval / 1.10F));
	}

	public static boolean hasNearbyTalent(Player player, EchoTrait trait) {
		if (!(player.level() instanceof ServerLevel level) || !player.isAlive()) return false;
		double rangeSqr = OWNER_SUPPORT_RANGE * OWNER_SUPPORT_RANGE;
		for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(OWNER_SUPPORT_RANGE),
				candidate -> candidate instanceof EchoWarriorEntity)) {
			if (!(entity instanceof EchoWarriorEntity echo) || !echo.livingEntity().isAlive()
					|| !player.getUUID().equals(echo.getOwnerUuid()) || entity.distanceToSqr(player) > rangeSqr) continue;
			ItemStack relic = echo.activeRelic();
			if (!relic.isEmpty() && EchoRelicState.hasTrait(relic, trait)) return true;
		}
		return false;
	}

	private static void afterDamage(LivingEntity victim, DamageSource source, float baseDamageTaken,
			float damageTaken, boolean blocked) {
		if (blocked || damageTaken <= 0.0F || source.getEntity() == null
				|| !(victim instanceof EchoWarriorEntity echo)) return;
		ItemStack relic = echo.activeRelic();
		if (!relic.isEmpty() && EchoRelicState.hasTrait(relic, EchoTrait.BAD_TEMPER)) {
			BAD_TEMPER_UNTIL.put(victim.getUUID(), victim.level().getGameTime() + BAD_TEMPER_TICKS);
		}
	}

	private static void afterDeath(LivingEntity victim, DamageSource source) {
		if (!(victim.level() instanceof ServerLevel level) || !(source.getEntity() instanceof ServerPlayer player)
				|| victim == player || victim.wasExperienceConsumed() || !victim.shouldDropExperience()
				|| !level.getGameRules().get(GameRules.MOB_DROPS)
				|| !hasNearbyTalent(player, EchoTrait.MENTOR)) return;
		int reward = victim.getExperienceReward(level, player);
		int extra = TalentExperienceHolder.of(player).echoWarrior$consumeMentorBonus(reward);
		if (extra > 0) ExperienceOrb.award(level, victim.position(), extra);
	}

	private static void tickLevel(ServerLevel level) {
		long now = level.getGameTime();
		if (now % 10L != 0L) return;
		if (now % 200L == 0L) BAD_TEMPER_UNTIL.entrySet().removeIf(entry -> entry.getValue() < now);
		for (Entity entity : level.getAllEntities()) {
			if (!(entity instanceof EchoWarriorEntity echo) || !echo.livingEntity().isAlive()) continue;
			LivingEntity living = echo.livingEntity();
			ItemStack relic = echo.activeRelic();
			float speedBonus = 0.0F;
			if (!relic.isEmpty()) {
				if (EchoRelicState.hasTrait(relic, EchoTrait.BIOME_AFFINITY) && matchesBiomeAffinity(living, relic)) speedBonus += 0.10F;
				if (EchoRelicState.hasTrait(relic, EchoTrait.NIGHT_OWL) && isNaturalNight(level)) speedBonus += 0.10F;
			}
			applySpeedModifier(living, speedBonus);
		}
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
		long time = Math.floorMod(level.getDefaultClockTime(), 24000L);
		return time >= 13000L && time < 23000L;
	}

	private static boolean matchesBiomeAffinity(LivingEntity entity, ItemStack relic) {
		if (!(entity.level() instanceof ServerLevel level) || !Level.OVERWORLD.equals(level.dimension())) return false;
		Holder<Biome> biome = level.getBiome(entity.blockPosition());
		return classifyBiome(biome) == EchoRelicState.biomeAffinity(relic);
	}

	private static EchoBiomeAffinity classifyBiome(Holder<Biome> biome) {
		if (biome.is(UNDERGROUND)) return EchoBiomeAffinity.UNDERGROUND;
		if (biome.is(COLD)) return EchoBiomeAffinity.COLD;
		if (biome.is(WATERS)) return EchoBiomeAffinity.WATERS;
		if (biome.is(WASTELAND)) return EchoBiomeAffinity.WASTELAND;
		if (biome.is(WOODLAND)) return EchoBiomeAffinity.WOODLAND;
		return EchoBiomeAffinity.OPENLAND;
	}

	private static TagKey<Biome> biomeTag(String path) {
		return TagKey.create(Registries.BIOME, EchoWarrior.id(path));
	}
}
