package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.binding.EchoBindingSystem;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModDamageTypes;
import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Single source of truth for all fixed accessory bonuses and runtime effects. */
public final class EchoAccessorySystem {
	private static final Identifier ATTACK_ID = EchoWarrior.id("accessory_attack");
	private static final Identifier ARMOR_ID = EchoWarrior.id("accessory_armor");
	private static final Identifier HEALTH_ID = EchoWarrior.id("accessory_health");
	private static final Identifier SPEED_ID = EchoWarrior.id("accessory_speed");
	private static final Identifier WATER_SPEED_ID = EchoWarrior.id("accessory_water_speed");
	private static final int DAY_NIGHT_HEAL_INTERVAL_TICKS = 50;
	private static final float DAY_NIGHT_HEAL_AMOUNT = 1.0F;
	private static final Map<AttackWindow, Boolean> CRITICAL_WINDOWS = new HashMap<>();
	private static final Map<AttackWindow, Boolean> HEAL_WINDOWS = new HashMap<>();

	private EchoAccessorySystem() {
	}

	public static void initialize() {
	}

	public static void apply(EchoWarriorEntity echo) {
		LivingEntity living = echo.livingEntity();
		float previousHealth = living.getHealth();
		applyModifier(living.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, attackBonus(installedAccessories(echo)),
				AttributeModifier.Operation.ADD_VALUE);
		applyModifier(living.getAttribute(Attributes.ARMOR), ARMOR_ID, armorBonus(installedAccessories(echo)),
				AttributeModifier.Operation.ADD_VALUE);
		applyModifier(living.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, maximumHealthBonus(installedAccessories(echo)),
				AttributeModifier.Operation.ADD_VALUE);
		applyModifier(living.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID,
				movementMultiplier(installedAccessories(echo)) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		living.setHealth(Math.min(previousHealth, living.getMaxHealth()));
	}

	private static void applyModifier(AttributeInstance attribute, Identifier id, double amount,
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
			if (!(entity instanceof EchoWarriorEntity echo) || !echo.livingEntity().isAlive()) continue;
			LivingEntity living = echo.livingEntity();
			List<ItemStack> accessories = installedAccessories(echo);
			AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
			boolean waterBoost = contains(accessories, ModItems.TOMATO_FISH_ACCESSORY) && living.isInWater();
			applyModifier(speed, WATER_SPEED_ID, waterBoost ? 0.50 : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

			if (!level.dimensionType().hasSkyLight()) continue;
			long dayTime = Math.floorMod(level.getDefaultClockTime(), 24000L);
			boolean day = dayTime < 13000L;
			boolean periodicHealing = day && contains(accessories, ModItems.SUNWHEEL_GARLAND_ACCESSORY)
					|| !day && contains(accessories, ModItems.MOONDEW_BOTTLE_ACCESSORY);
			if (periodicHealing) tryApplyDayNightHealing(level, living, now);
		}
	}

	private static void tryApplyDayNightHealing(ServerLevel level, LivingEntity living, long now) {
		long phase = Math.floorMod(living.getUUID().getLeastSignificantBits(), (long)DAY_NIGHT_HEAL_INTERVAL_TICKS);
		if (Math.floorMod(now, (long)DAY_NIGHT_HEAL_INTERVAL_TICKS) != phase
				|| living.getHealth() >= living.getMaxHealth()) return;
		float previousHealth = living.getHealth();
		living.heal(DAY_NIGHT_HEAL_AMOUNT);
		if (living.getHealth() <= previousHealth) return;
		level.sendParticles(ParticleTypes.HEART,
				living.getX(), living.getY() + living.getBbHeight() * 0.75, living.getZ(),
				2, 0.22, 0.15, 0.22, 0.01);
	}

	/** Called by the LivingEntity mixin before vanilla mitigation. */
	public static float modifyOutgoingDamage(LivingEntity victim, ServerLevel level, DamageSource source, float amount) {
		EchoWarriorEntity echo = resolveAttackingEcho(source);
		if (echo == null || amount <= 0.0F || !has(echo, ModItems.CRACK_RING_HAMMER_CHARM_ACCESSORY)
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

	public static boolean allowDamage(LivingEntity victim, DamageSource source, float amount) {
		if (!(victim instanceof EchoWarriorEntity echo) || amount <= 0.0F
				|| !has(echo, ModItems.SUBSTITUTE_DOLL_ACCESSORY) || !isDodgeEligible(source)) return true;
		if (victim.getRandom().nextFloat() >= 0.10F) return true;
		echo.onAccessoryDodge(source);
		return false;
	}

	public static void afterDamage(LivingEntity victim, DamageSource source, float baseDamageTaken,
			float damageTaken, boolean blocked) {
		if (blocked || damageTaken <= 0.0F) return;
		EchoWarriorEntity echo = resolveAttackingEcho(source);
		if (echo == null || !has(echo, ModItems.BLOOD_PACT_FANG_ACCESSORY) || !isDirectAttack(source, echo)) return;
		AttackWindow window = attackWindow(echo, source, echo.livingEntity().level().getGameTime());
		if (HEAL_WINDOWS.putIfAbsent(window, Boolean.TRUE) != null) return;
		if (echo.livingEntity().getRandom().nextFloat() < 0.30F) echo.livingEntity().heal(4.0F);
	}

	private static boolean isDirectAttack(DamageSource source, EchoWarriorEntity echo) {
		if (source.is(ModDamageTypes.SPIKED_ARMOR_REFLECTION) || source.is(ModDamageTypes.BLEEDING)
				|| source.is(ModDamageTypes.OBSIDIAN_WOUND) || source.is(DamageTypeTags.IS_EXPLOSION)) return false;
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
				&& !source.is(ModDamageTypes.BLEEDING) && !source.is(ModDamageTypes.OBSIDIAN_WOUND);
	}

	public static EchoWarriorEntity resolveAttackingEcho(DamageSource source) {
		if (source.getEntity() instanceof EchoWarriorEntity echo) return echo;
		if (source.getDirectEntity() instanceof Projectile projectile
				&& projectile.getOwner() instanceof EchoWarriorEntity echo) return echo;
		return null;
	}

	private static AttackWindow attackWindow(EchoWarriorEntity echo, DamageSource source, long tick) {
		Entity direct = source.getDirectEntity();
		UUID directUuid = direct == null ? echo.livingEntity().getUUID() : direct.getUUID();
		AttackWindow probe = new AttackWindow(echo.livingEntity().getUUID(), directUuid, tick);
		for (AttackWindow existing : CRITICAL_WINDOWS.keySet()) if (existing.equals(probe)) return existing;
		for (AttackWindow existing : HEAL_WINDOWS.keySet()) if (existing.equals(probe)) return existing;
		return probe;
	}

	public static void reflectMeleeDamage(EchoWarriorEntity echo, ServerLevel level, DamageSource source, float previousHealth) {
		float actualHealthDamage = previousHealth - echo.livingEntity().getHealth();
		if (actualHealthDamage <= 0.0F || source.is(ModDamageTypes.SPIKED_ARMOR_REFLECTION)
				|| source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION)
				|| source.is(DamageTypes.MAGIC) || source.is(DamageTypeTags.BYPASSES_ARMOR)) return;
		Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity livingAttacker) || source.getDirectEntity() != attacker
				|| !has(echo, ModItems.SPIKED_ARMOR_ACCESSORY)) return;
		LivingEntity direct = echo.livingEntity();
		LivingEntity owner = echo.getOwner();
		livingAttacker.hurtServer(level,
				level.damageSources().source(ModDamageTypes.SPIKED_ARMOR_REFLECTION, direct, owner == null ? direct : owner),
				actualHealthDamage);
	}

	public static double proactiveRange(EchoWarriorEntity echo, double base, boolean waiting) {
		double adjusted = has(echo, ModItems.BATTLE_BLINDFOLD_ACCESSORY) ? base * 0.25
				: has(echo, ModItems.HAWKEYE_LENS_ACCESSORY) ? base * 1.50 : base;
		return Math.min(adjusted, waiting ? 8.0 : 32.0);
	}

	public static boolean has(EchoWarriorEntity echo, Item item) {
		return contains(installedAccessories(echo), item);
	}

	public static List<ItemStack> installedAccessories(EchoWarriorEntity echo) {
		LivingEntity living = echo.livingEntity();
		if (!(living.level() instanceof ServerLevel level) || echo.getSummonerUuid() == null) return List.of();
		return EchoBindingSystem.accessories(level, echo.getSummonerUuid());
	}

	public static double attackBonus(SimpleContainer contents) { return attackBonus(contents.getItems()); }
	public static double attackBonus(ItemStack summoner) { return attackBonus(TestEchoSummonerItem.accessoryStacks(summoner)); }
	private static double attackBonus(List<ItemStack> a) {
		double n = 0;
		if (contains(a, ModItems.BATTLE_WORN_WHETSTONE_ACCESSORY)) n += 2;
		if (contains(a, ModItems.MOUNTAIN_BURDEN_BLADE_ACCESSORY)) n += 4;
		if (contains(a, ModItems.FRACTURED_CRYSTAL_BLADE_ACCESSORY)) n += 4;
		if (contains(a, ModItems.TWIN_OATH_BADGE_ACCESSORY)) n += 1;
		if (contains(a, ModItems.BATTLE_BLINDFOLD_ACCESSORY)) n += 3;
		if (contains(a, ModItems.MEMORY_RITUAL_KNIFE_ACCESSORY)) n += 2;
		if (contains(a, ModItems.PEACEMAKER_ACCESSORY)) n -= 4;
		return n;
	}

	public static double armorBonus(SimpleContainer contents) { return armorBonus(contents.getItems()); }
	public static double armorBonus(ItemStack summoner) { return armorBonus(TestEchoSummonerItem.accessoryStacks(summoner)); }
	private static double armorBonus(List<ItemStack> a) {
		double n = 0;
		if (contains(a, ModItems.PLATE_ARMOR_ACCESSORY)) n += 2;
		if (contains(a, ModItems.CHAINMAIL_ARMOR_ACCESSORY)) n += 4;
		if (contains(a, ModItems.SPIKED_ARMOR_ACCESSORY)) n += 1;
		if (contains(a, ModItems.FRACTURED_CRYSTAL_BLADE_ACCESSORY)) n -= 6;
		if (contains(a, ModItems.TWIN_OATH_BADGE_ACCESSORY)) n += 2;
		return n;
	}

	public static double maximumHealthBonus(SimpleContainer contents) { return maximumHealthBonus(contents.getItems()); }
	public static double maximumHealthBonus(ItemStack summoner) { return maximumHealthBonus(TestEchoSummonerItem.accessoryStacks(summoner)); }
	private static double maximumHealthBonus(List<ItemStack> a) {
		double n = 0;
		if (contains(a, ModItems.HEART_SPROUT_AMBER_ACCESSORY)) n += 6;
		if (contains(a, ModItems.FEAST_HAM_ACCESSORY)) n += 12;
		if (contains(a, ModItems.PEACEMAKER_ACCESSORY)) n += 20;
		if (contains(a, ModItems.HOLLOW_BIRD_BONE_ACCESSORY)) n -= 8;
		return n;
	}

	public static double movementMultiplier(SimpleContainer contents) { return movementMultiplier(contents.getItems()); }
	public static double movementMultiplier(ItemStack summoner) { return movementMultiplier(TestEchoSummonerItem.accessoryStacks(summoner)); }
	private static double movementMultiplier(List<ItemStack> a) {
		double n = 1.0;
		if (contains(a, ModItems.CHAINMAIL_ARMOR_ACCESSORY)) n -= 0.15;
		if (contains(a, ModItems.MOUNTAIN_BURDEN_BLADE_ACCESSORY)) n -= 0.20;
		if (contains(a, ModItems.FEAST_HAM_ACCESSORY)) n -= 0.15;
		if (contains(a, ModItems.WINDCHASER_FEATHER_ACCESSORY)) n += 0.10;
		if (contains(a, ModItems.HOLLOW_BIRD_BONE_ACCESSORY)) n += 0.20;
		return Math.max(0.25, n);
	}

	private static boolean contains(List<ItemStack> accessories, Item item) {
		return accessories.stream().anyMatch(stack -> stack.is(item));
	}

	private static final class AttackWindow {
		private final UUID echo;
		private final UUID direct;
		private final long tick;
		private final java.util.concurrent.atomic.AtomicBoolean feedback = new java.util.concurrent.atomic.AtomicBoolean();

		private AttackWindow(UUID echo, UUID direct, long tick) {
			this.echo = echo;
			this.direct = direct;
			this.tick = tick;
		}

		@Override public boolean equals(Object other) {
			return other instanceof AttackWindow window && this.tick == window.tick
					&& this.echo.equals(window.echo) && this.direct.equals(window.direct);
		}

		@Override public int hashCode() { return java.util.Objects.hash(this.echo, this.direct, this.tick); }
	}
}
