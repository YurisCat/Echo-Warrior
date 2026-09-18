package com.yuriscat.echowarrior.compat.entity;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.ModEffects1211;
import com.yuriscat.echowarrior.compat.EchoHeroType1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1211;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregates every permanent Echo aura from live, loaded sources and removes
 * effects (including their permanent attribute modifiers) when no source owns
 * them anymore.
 */
public final class EchoAuraAuditSystem1211 {
	private static final double FORMATION_ENTER_SQR = 8.0 * 8.0;
	private static final double FORMATION_EXIT_SQR = 8.75 * 8.75;
	private static final double SUN_ENTER_SQR = 32.0 * 32.0;
	private static final double SUN_EXIT_SQR = 34.0 * 34.0;
	private static final int EXIT_GRACE_TICKS = 10;
	private static final ResourceLocation FORMATION_ATTACK_ID = EchoWarrior1211.id("soldier_formation_attack");
	private static final ResourceLocation SUN_ATTACK_ID = EchoWarrior1211.id("huitzilopochtli_attack");

	private static final Map<Link, Integer> FORMATION_LINKS = new HashMap<>();
	private static final Map<Link, Integer> SUN_OWNER_LINKS = new HashMap<>();
	private static final Map<UUID, Long> SUN_HEAL_TIMES = new HashMap<>();

	private EchoAuraAuditSystem1211() {
	}

	public static void initialize() {
	}

	public static void tick(MinecraftServer server) {
		Map<UUID, LivingEntity> living = new HashMap<>();
		Map<ServerLevel, List<RomanLegionaryEchoEntity1211>> romans = new HashMap<>();
		Map<ServerLevel, List<AztecWarriorEchoEntity1211>> aztecs = new HashMap<>();
		for (ServerLevel level : server.getAllLevels()) {
			List<RomanLegionaryEchoEntity1211> levelRomans = new ArrayList<>();
			List<AztecWarriorEchoEntity1211> levelAztecs = new ArrayList<>();
			for (Entity entity : level.getAllEntities()) {
				if (entity instanceof LivingEntity livingEntity) living.put(livingEntity.getUUID(), livingEntity);
				if (entity instanceof RomanLegionaryEchoEntity1211 roman && roman.isAlive()) levelRomans.add(roman);
				if (entity instanceof AztecWarriorEchoEntity1211 aztec && aztec.isAlive()) levelAztecs.add(aztec);
			}
			romans.put(level, levelRomans);
			aztecs.put(level, levelAztecs);
		}

		Map<UUID, Integer> desiredFormation = collectFormation(server, romans);
		Set<UUID> desiredSun = collectSun(server, aztecs);
		boolean reverseAudit = server.getTickCount() % 10 == 0;
		for (LivingEntity beneficiary : living.values()) {
			applyFormation(beneficiary, desiredFormation.get(beneficiary.getUUID()), reverseAudit);
			applySun(beneficiary, desiredSun.contains(beneficiary.getUUID()), reverseAudit);
		}
		if (reverseAudit) SUN_HEAL_TIMES.keySet().retainAll(living.keySet());
	}

	public static void clear() {
		FORMATION_LINKS.clear();
		SUN_OWNER_LINKS.clear();
		SUN_HEAL_TIMES.clear();
	}

	private static Map<UUID, Integer> collectFormation(
			MinecraftServer server,
			Map<ServerLevel, List<RomanLegionaryEchoEntity1211>> romans
	) {
		Map<UUID, Integer> desired = new HashMap<>();
		Set<Link> seen = new HashSet<>();
		for (Map.Entry<ServerLevel, List<RomanLegionaryEchoEntity1211>> entry : romans.entrySet()) {
			ServerLevel level = entry.getKey();
			List<RomanLegionaryEchoEntity1211> levelRomans = entry.getValue();
			for (RomanLegionaryEchoEntity1211 source : levelRomans) {
				if (!source.isFormationActive()) continue;
				UUID controllerId = source.getOwnerUuid();
				ServerPlayer controller = controllerId == null ? null : server.getPlayerList().getPlayer(controllerId);
				boolean controllerAvailable = controller != null && controller.isAlive() && !controller.isSpectator()
						&& controller.level() == level;
				int strength = controllerAvailable && source.distanceToSqr(controller) <= FORMATION_ENTER_SQR
						&& (controller.getMainHandItem().getItem() instanceof ShieldItem
						|| controller.getOffhandItem().getItem() instanceof ShieldItem) ? 1 : 0;
				considerLink(FORMATION_LINKS, seen, desired, source, source,
						strength, FORMATION_ENTER_SQR, FORMATION_EXIT_SQR);
				if (controllerId == null) continue;
				if (controllerAvailable) {
					considerLink(FORMATION_LINKS, seen, desired, source, controller,
							strength, FORMATION_ENTER_SQR, FORMATION_EXIT_SQR);
				}
				for (RomanLegionaryEchoEntity1211 beneficiary : levelRomans) {
					if (beneficiary == source || !controllerId.equals(beneficiary.getOwnerUuid())) continue;
					considerLink(FORMATION_LINKS, seen, desired, source, beneficiary,
							strength, FORMATION_ENTER_SQR, FORMATION_EXIT_SQR);
				}
			}
		}
		FORMATION_LINKS.keySet().removeIf(link -> !seen.contains(link));
		return desired;
	}

	private static Set<UUID> collectSun(
			MinecraftServer server,
			Map<ServerLevel, List<AztecWarriorEchoEntity1211>> aztecs
	) {
		Set<UUID> desired = new HashSet<>();
		Set<Link> seen = new HashSet<>();
		long now = server.overworld().getGameTime();
		for (Map.Entry<ServerLevel, List<AztecWarriorEchoEntity1211>> entry : aztecs.entrySet()) {
			ServerLevel level = entry.getKey();
			for (AztecWarriorEchoEntity1211 source : entry.getValue()) {
				ItemStack relic = source.activeRelic();
				boolean enabled = !relic.isEmpty() && EchoHeroType1211.fromRelic(relic) == EchoHeroType1211.AZTEC_WARRIOR
						&& EchoRelicState1211.skillEnabled(relic, 0);
				if (!enabled) continue;
				if (isSunlit(level, source)) {
					desired.add(source.getUUID());
					healFromSun(source, now);
				}
				UUID controllerId = source.getOwnerUuid();
				if (controllerId == null) continue;
				ServerPlayer controller = server.getPlayerList().getPlayer(controllerId);
				if (controller == null || !controller.isAlive() || controller.isSpectator()
						|| controller.level() != level || !isSunlit(level, controller)) continue;
				Link link = new Link(source.getUUID(), controller.getUUID());
				seen.add(link);
				if (linkActive(SUN_OWNER_LINKS, link, source.distanceToSqr(controller), SUN_ENTER_SQR, SUN_EXIT_SQR)) {
					desired.add(controller.getUUID());
					healFromSun(controller, now);
				}
			}
		}
		SUN_OWNER_LINKS.keySet().removeIf(link -> !seen.contains(link));
		return desired;
	}

	private static void considerLink(
			Map<Link, Integer> links,
			Set<Link> seen,
			Map<UUID, Integer> desired,
			LivingEntity source,
			LivingEntity beneficiary,
			int strength,
			double enterSqr,
			double exitSqr
	) {
		Link link = new Link(source.getUUID(), beneficiary.getUUID());
		seen.add(link);
		if (linkActive(links, link, source.distanceToSqr(beneficiary), enterSqr, exitSqr)) {
			desired.merge(beneficiary.getUUID(), strength, Math::max);
		}
	}

	private static boolean linkActive(
			Map<Link, Integer> links,
			Link link,
			double distanceSqr,
			double enterSqr,
			double exitSqr
	) {
		Integer outsideTicks = links.get(link);
		if (outsideTicks == null) {
			if (distanceSqr > enterSqr) return false;
			links.put(link, 0);
			return true;
		}
		if (distanceSqr <= exitSqr) {
			links.put(link, 0);
			return true;
		}
		int next = outsideTicks + 1;
		if (next >= EXIT_GRACE_TICKS) {
			links.remove(link);
			return false;
		}
		links.put(link, next);
		return true;
	}

	private static boolean isSunlit(ServerLevel level, LivingEntity entity) {
		long timeOfDay = Math.floorMod(level.getDayTime(), 24_000L);
		return level.dimensionType().hasSkyLight() && timeOfDay < 12_000L
				&& level.canSeeSky(entity.blockPosition());
	}

	private static void healFromSun(LivingEntity beneficiary, long now) {
		long last = SUN_HEAL_TIMES.getOrDefault(beneficiary.getUUID(), Long.MIN_VALUE / 2L);
		if (now - last < 80L || beneficiary.getHealth() >= beneficiary.getMaxHealth()) return;
		beneficiary.heal(1.0F);
		SUN_HEAL_TIMES.put(beneficiary.getUUID(), now);
	}

	private static void applyFormation(LivingEntity beneficiary, Integer desired, boolean reverseAudit) {
		boolean legacy = beneficiary.hasEffect(ModEffects1211.LEGACY_SOLDIER_FORMATION);
		boolean weapons = beneficiary.hasEffect(ModEffects1211.WEAPONS_RAISED);
		boolean shields = beneficiary.hasEffect(ModEffects1211.SHIELDS_RAISED);
		boolean wrong;
		if (desired == null) wrong = legacy || weapons || shields;
		else if (desired == 0) wrong = legacy || shields || !weapons;
		else wrong = legacy || weapons || !shields;
		if (wrong) {
			beneficiary.removeEffect(ModEffects1211.LEGACY_SOLDIER_FORMATION);
			beneficiary.removeEffect(ModEffects1211.WEAPONS_RAISED);
			beneficiary.removeEffect(ModEffects1211.SHIELDS_RAISED);
			if (desired != null) beneficiary.addEffect(new MobEffectInstance(
					desired == 0 ? ModEffects1211.WEAPONS_RAISED : ModEffects1211.SHIELDS_RAISED,
					MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
		}
		if (reverseAudit && desired == null) removeModifier(beneficiary, FORMATION_ATTACK_ID);
		else if (reverseAudit && !hasModifier(beneficiary, FORMATION_ATTACK_ID)) {
			beneficiary.removeEffect(desired == 0 ? ModEffects1211.WEAPONS_RAISED : ModEffects1211.SHIELDS_RAISED);
			beneficiary.addEffect(new MobEffectInstance(
					desired == 0 ? ModEffects1211.WEAPONS_RAISED : ModEffects1211.SHIELDS_RAISED,
					MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
		}
	}

	private static void applySun(LivingEntity beneficiary, boolean desired, boolean reverseAudit) {
		boolean active = beneficiary.hasEffect(ModEffects1211.HUITZILOPOCHTLI_BLESSING);
		if (desired && !active) {
			beneficiary.addEffect(new MobEffectInstance(ModEffects1211.HUITZILOPOCHTLI_BLESSING,
					MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
		} else if (!desired && active) {
			beneficiary.removeEffect(ModEffects1211.HUITZILOPOCHTLI_BLESSING);
		}
		if (reverseAudit && !desired) removeModifier(beneficiary, SUN_ATTACK_ID);
		else if (reverseAudit && !hasModifier(beneficiary, SUN_ATTACK_ID)) {
			beneficiary.removeEffect(ModEffects1211.HUITZILOPOCHTLI_BLESSING);
			beneficiary.addEffect(new MobEffectInstance(ModEffects1211.HUITZILOPOCHTLI_BLESSING,
					MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
		}
	}

	private static boolean hasModifier(LivingEntity entity, ResourceLocation id) {
		var attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
		return attack != null && attack.getModifier(id) != null;
	}

	private static void removeModifier(LivingEntity entity, ResourceLocation id) {
		var attack = entity.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack != null) attack.removeModifier(id);
	}

	private record Link(UUID source, UUID beneficiary) {
	}
}
