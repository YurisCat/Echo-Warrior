package com.yuriscat.echowarrior.item;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModDamageTypes;
import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Applies summoner modules exclusively to their bound Echo. */
public final class EchoModuleSystem {
	private static final Identifier PLATE_ARMOR_ID = EchoWarrior.id("plate_armor_module");
	private static final Identifier CHAINMAIL_ARMOR_ID = EchoWarrior.id("chainmail_armor_module");
	private static final Identifier SPIKED_ARMOR_ID = EchoWarrior.id("spiked_armor_module");
	private static final Identifier SPEED_ID = EchoWarrior.id("summoner_module_speed");
	private static final AttributeModifier PLATE_ARMOR = new AttributeModifier(
			PLATE_ARMOR_ID, 2.0, AttributeModifier.Operation.ADD_VALUE);
	private static final AttributeModifier CHAINMAIL_ARMOR = new AttributeModifier(
			CHAINMAIL_ARMOR_ID, 4.0, AttributeModifier.Operation.ADD_VALUE);
	private static final AttributeModifier SPIKED_ARMOR = new AttributeModifier(
			SPIKED_ARMOR_ID, 2.0, AttributeModifier.Operation.ADD_VALUE);
	private static final AttributeModifier CHAINMAIL_SPEED = new AttributeModifier(
			SPEED_ID, -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

	private EchoModuleSystem() {
	}

	public static void apply(EchoWarriorEntity echo) {
		LivingEntity living = echo.livingEntity();
		AttributeInstance armor = living.getAttribute(Attributes.ARMOR);
		AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
		if (armor != null) {
			armor.removeModifier(PLATE_ARMOR_ID);
			armor.removeModifier(CHAINMAIL_ARMOR_ID);
			armor.removeModifier(SPIKED_ARMOR_ID);
		}
		if (speed != null) speed.removeModifier(SPEED_ID);

		List<ItemStack> modules = installedModules(echo);
		if (armor != null) {
			if (contains(modules, ModItems.PLATE_ARMOR_MODULE)) armor.addTransientModifier(PLATE_ARMOR);
			if (contains(modules, ModItems.CHAINMAIL_ARMOR_MODULE)) armor.addTransientModifier(CHAINMAIL_ARMOR);
			if (contains(modules, ModItems.SPIKED_ARMOR_MODULE)) armor.addTransientModifier(SPIKED_ARMOR);
		}
		if (speed != null && contains(modules, ModItems.CHAINMAIL_ARMOR_MODULE)) {
			speed.addTransientModifier(CHAINMAIL_SPEED);
		}
	}

	public static double armorBonus(SimpleContainer contents) {
		return armorBonus(contents.getItems().subList(0, Math.min(6, contents.getContainerSize())));
	}

	public static double armorBonus(ItemStack summoner) {
		return armorBonus(TestEchoSummonerItem.moduleStacks(summoner));
	}

	private static double armorBonus(List<ItemStack> modules) {
		double bonus = 0.0;
		if (contains(modules, ModItems.PLATE_ARMOR_MODULE)) bonus += 2.0;
		if (contains(modules, ModItems.CHAINMAIL_ARMOR_MODULE)) bonus += 4.0;
		if (contains(modules, ModItems.SPIKED_ARMOR_MODULE)) bonus += 2.0;
		return bonus;
	}

	public static double movementMultiplier(SimpleContainer contents) {
		return contains(contents.getItems(), ModItems.CHAINMAIL_ARMOR_MODULE) ? 0.90 : 1.0;
	}

	public static double movementMultiplier(ItemStack summoner) {
		return contains(TestEchoSummonerItem.moduleStacks(summoner), ModItems.CHAINMAIL_ARMOR_MODULE) ? 0.90 : 1.0;
	}

	public static void reflectMeleeDamage(EchoWarriorEntity echo, ServerLevel level, DamageSource source, float previousHealth) {
		float actualHealthDamage = previousHealth - echo.livingEntity().getHealth();
		if (actualHealthDamage <= 0.0F || source.is(ModDamageTypes.SPIKED_ARMOR_REFLECTION)
				|| source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_EXPLOSION)
				|| source.is(DamageTypes.MAGIC) || source.is(DamageTypeTags.BYPASSES_ARMOR)) return;
		Entity attacker = source.getEntity();
		if (!(attacker instanceof LivingEntity livingAttacker) || source.getDirectEntity() != attacker) return;
		if (!contains(installedModules(echo), ModItems.SPIKED_ARMOR_MODULE)) return;

		LivingEntity direct = echo.livingEntity();
		LivingEntity owner = echo.getOwner();
		livingAttacker.hurtServer(level,
				level.damageSources().source(ModDamageTypes.SPIKED_ARMOR_REFLECTION, direct, owner == null ? direct : owner),
				actualHealthDamage);
	}

	private static List<ItemStack> installedModules(EchoWarriorEntity echo) {
		LivingEntity owner = echo.getOwner();
		if (!(owner instanceof Player player) || echo.getSummonerUuid() == null) return List.of();
		return TestEchoSummonerItem.moduleStacks(TestEchoSummonerItem.findSummonerStack(player, echo.getSummonerUuid()));
	}

	private static boolean contains(List<ItemStack> modules, net.minecraft.world.item.Item item) {
		return modules.stream().anyMatch(stack -> stack.is(item));
	}
}
