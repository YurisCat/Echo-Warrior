package com.yuriscat.echowarrior.progress;

import com.yuriscat.echowarrior.entity.RomanLegionaryEchoEntity;
import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.EchoRelicProgress;
import com.yuriscat.echowarrior.item.TestEchoSummonerItem;
import com.yuriscat.echowarrior.menu.SummonerMenu;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class EchoExperienceSystem {
	private static final long PARTICIPATION_WINDOW_TICKS = 20L * 10L;
	private static final Map<LivingEntity, Map<UUID, Participation>> PARTICIPATION = new WeakHashMap<>();

	private EchoExperienceSystem() {
	}

	public static void initialize() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(EchoExperienceSystem::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(EchoExperienceSystem::afterDeath);
	}

	private static void afterDamage(
			LivingEntity victim,
			DamageSource source,
			float baseDamageTaken,
			float damageTaken,
			boolean blocked
	) {
		Entity attacker = source.getEntity();
		if (attacker instanceof RomanLegionaryEchoEntity echo) {
			markParticipation(echo, victim);
		}
		if (victim instanceof RomanLegionaryEchoEntity echo && attacker instanceof LivingEntity opponent) {
			markParticipation(echo, opponent);
		}
	}

	private static void afterDeath(LivingEntity victim, DamageSource source) {
		if (!(victim.level() instanceof ServerLevel level)) {
			return;
		}

		Entity killer = source.getEntity();
		if (killer instanceof RomanLegionaryEchoEntity echo) {
			markParticipation(echo, victim);
		}

		UUID creditedOwner = null;
		UUID killingEcho = null;
		if (killer instanceof RomanLegionaryEchoEntity echo) {
			creditedOwner = echo.getOwnerUuid();
			killingEcho = echo.getUUID();
		} else if (killer instanceof ServerPlayer player) {
			creditedOwner = player.getUUID();
		}
		if (creditedOwner == null) {
			PARTICIPATION.remove(victim);
			return;
		}

		Map<UUID, Participation> records = PARTICIPATION.remove(victim);
		if (records == null || records.isEmpty()) {
			return;
		}
		long now = level.getGameTime();
		UUID finalCreditedOwner = creditedOwner;
		UUID finalKillingEcho = killingEcho;
		List<Participation> eligible = records.values().stream()
				.filter(record -> record.ownerUuid().equals(finalCreditedOwner))
				.filter(record -> now - record.lastParticipationTick() <= PARTICIPATION_WINDOW_TICKS)
				.sorted(Comparator
						.comparing((Participation record) -> !record.echoUuid().equals(finalKillingEcho))
						.thenComparing(record -> record.echoUuid().toString()))
				.toList();
		if (eligible.isEmpty()) {
			return;
		}

		int reward = victim.getExperienceReward(level, killer);
		if (reward <= 0) {
			return;
		}
		int share = reward / eligible.size();
		int remainder = reward % eligible.size();
		for (int index = 0; index < eligible.size(); index++) {
			int awarded = share + (index < remainder ? 1 : 0);
			if (awarded > 0) {
				awardExperience(level, eligible.get(index), awarded);
			}
		}
	}

	public static void markParticipation(RomanLegionaryEchoEntity echo, LivingEntity target) {
		if (!(echo.level() instanceof ServerLevel level) || target == echo || !target.isAlive()) {
			return;
		}
		UUID ownerUuid = echo.getOwnerUuid();
		UUID summonerUuid = echo.getSummonerUuid();
		if (ownerUuid == null || summonerUuid == null) {
			return;
		}
		PARTICIPATION.computeIfAbsent(target, ignored -> new java.util.HashMap<>())
				.put(echo.getUUID(), new Participation(
						echo.getUUID(),
						ownerUuid,
						summonerUuid,
						level.getGameTime()
				));
	}

	private static void awardExperience(ServerLevel level, Participation participation, int amount) {
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(participation.ownerUuid());
		if (owner == null) {
			return;
		}
		ItemStack summoner = TestEchoSummonerItem.findSummonerStack(owner, participation.summonerUuid());
		if (summoner.isEmpty()) {
			return;
		}

		ItemStack relic;
		SummonerMenu openMenu = owner.containerMenu instanceof SummonerMenu menu && menu.matchesSummoner(participation.summonerUuid())
				? menu
				: null;
		SimpleContainer storedContents = null;
		if (openMenu != null) {
			relic = openMenu.relicStackForProgress();
		} else {
			storedContents = new SimpleContainer(SummonerMenu.CUSTOM_SLOT_COUNT);
			summoner.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
					.copyInto(storedContents.getItems());
			relic = storedContents.getItem(SummonerMenu.RELIC_SLOT);
		}
		if (!(relic.getItem() instanceof EchoRelicItem)) {
			return;
		}

		EchoRelicProgress.ProgressResult result = EchoRelicProgress.addExperience(relic, amount);
		if (openMenu != null) {
			openMenu.markRelicProgressChanged();
		} else if (storedContents != null) {
			summoner.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(storedContents.getItems()));
		}

		if (result.levelsGained() <= 0) {
			return;
		}
		Entity active = level.getEntity(participation.echoUuid());
		if (active instanceof RomanLegionaryEchoEntity echo && echo.isAlive()) {
			applyRelicProgress(echo, relic, true);
		}
		owner.sendOverlayMessage(Component.literal("罗马军团兵升至 " + result.newLevel() + " 级"));
		level.playSound(
				null,
				owner.blockPosition(),
				SoundEvents.PLAYER_LEVELUP,
				SoundSource.PLAYERS,
				0.65F,
				Math.min(1.8F, 0.95F + result.newLevel() * 0.02F)
		);
	}

	public static void applyRelicProgress(RomanLegionaryEchoEntity echo, ItemStack relic, boolean preserveHealthGain) {
		int level = relic.getItem() instanceof EchoRelicItem ? EchoRelicProgress.level(relic) : 1;
		double newMaximumHealth = EchoRelicProgress.maximumHealth(level);
		double oldMaximumHealth = echo.getMaxHealth();
		AttributeInstance maximumHealth = echo.getAttribute(Attributes.MAX_HEALTH);
		AttributeInstance attackDamage = echo.getAttribute(Attributes.ATTACK_DAMAGE);
		if (maximumHealth != null) {
			maximumHealth.setBaseValue(newMaximumHealth);
		}
		if (attackDamage != null) {
			attackDamage.setBaseValue(EchoRelicProgress.attackDamage(level));
		}
		if (preserveHealthGain) {
			float gained = (float)Math.max(0.0, newMaximumHealth - oldMaximumHealth);
			echo.setHealth(Math.min(echo.getMaxHealth(), echo.getHealth() + gained));
		} else {
			echo.setHealth(echo.getMaxHealth());
		}
	}

	private record Participation(
			UUID echoUuid,
			UUID ownerUuid,
			UUID summonerUuid,
			long lastParticipationTick
	) {
	}
}
