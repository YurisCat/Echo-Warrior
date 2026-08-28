package com.yuriscat.echowarrior.progress;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import com.yuriscat.echowarrior.item.EchoAccessorySystem;
import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.EchoRelicProgress;
import com.yuriscat.echowarrior.item.EchoRelicState;
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
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class EchoExperienceSystem {
	private static final long CREDIT_WINDOW_TICKS = 20L * 10L;
	private static final Map<LivingEntity, Participation> LAST_ECHO_CREDIT = new WeakHashMap<>();

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
		if (damageTaken <= 0.0F || blocked) return;
		EchoWarriorEntity echo = EchoAccessorySystem.resolveAttackingEcho(source);
		if (echo != null) {
			markParticipation(echo, victim);
		} else if (source.getEntity() != null) {
			// Preserve recent Echo credit through entity-less follow-up damage such as
			// falling, fire, lava, drowning, or Echo-applied damage-over-time effects.
			LAST_ECHO_CREDIT.remove(victim);
		}
	}

	private static void afterDeath(LivingEntity victim, DamageSource source) {
		if (!(victim.level() instanceof ServerLevel level)) {
			return;
		}

		EchoWarriorEntity direct = EchoAccessorySystem.resolveAttackingEcho(source);
		Participation credit = LAST_ECHO_CREDIT.remove(victim);
		if (direct != null) {
			credit = participationFor(direct);
		}
		if (credit == null || level.getGameTime() - credit.lastParticipationTick() > CREDIT_WINDOW_TICKS) return;
		Entity active = level.getEntity(credit.echoUuid());
		if (!(active instanceof EchoWarriorEntity echo) || !echo.livingEntity().isAlive()) return;

		if (EchoAccessorySystem.has(echo, ModItems.VICTORS_LAUREL_ACCESSORY)) {
			echo.livingEntity().heal(echo.livingEntity().getMaxHealth() * 0.10F);
		}
		if (EchoAccessorySystem.has(echo, ModItems.MEMORY_RITUAL_KNIFE_ACCESSORY)
				&& echo.livingEntity().getRandom().nextFloat() < 0.005F) {
			ItemStack legacy = new ItemStack(switch (echo.livingEntity().getRandom().nextInt(5)) {
				case 0 -> ModItems.COURAGE_LEGACY;
				case 1 -> ModItems.FORTITUDE_LEGACY;
				case 2 -> ModItems.PURITY_LEGACY;
				case 3 -> ModItems.WISDOM_LEGACY;
				default -> ModItems.CRAFT_LEGACY;
			});
			level.addFreshEntity(new ItemEntity(level, victim.getX(), victim.getY() + 0.35, victim.getZ(), legacy));
		}

		int reward = victim.getExperienceReward(level, echo.livingEntity());
		if (reward <= 0) return;
		int worldReward = scaledReward(reward,
				EchoAccessorySystem.has(echo, ModItems.LIGHT_GATHERING_MAGNET_ACCESSORY));
		int growthReward = scaledReward(reward,
				EchoAccessorySystem.has(echo, ModItems.TRAINING_NOTES_ACCESSORY));
		ExperienceOrb.award(level, victim.position(), worldReward);
		awardExperience(level, credit, growthReward);
	}

	private static int scaledReward(int base, boolean boosted) {
		return Math.max(1, boosted ? Math.round(base * 1.5F) : base);
	}

	public static void markParticipation(EchoWarriorEntity echo, LivingEntity target) {
		LivingEntity echoEntity = echo.livingEntity();
		if (!(echoEntity.level() instanceof ServerLevel level) || target == echoEntity || !target.isAlive()) {
			return;
		}
		Participation participation = participationFor(echo);
		if (participation != null) LAST_ECHO_CREDIT.put(target, participation);
	}

	private static Participation participationFor(EchoWarriorEntity echo) {
		LivingEntity echoEntity = echo.livingEntity();
		if (!(echoEntity.level() instanceof ServerLevel level)) return null;
		UUID ownerUuid = echo.getOwnerUuid();
		UUID summonerUuid = echo.getSummonerUuid();
		return ownerUuid == null || summonerUuid == null ? null : new Participation(
				echoEntity.getUUID(), ownerUuid, summonerUuid, level.getGameTime());
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
		if (active instanceof EchoWarriorEntity echo && echo.livingEntity().isAlive()) {
			applyRelicProgress(echo, relic, true);
		}
		owner.sendOverlayMessage(Component.literal(com.yuriscat.echowarrior.item.EchoHeroType.fromRelic(relic).chineseName()
				+ "升至 " + result.newLevel() + " 级"));
		level.playSound(
				null,
				owner.blockPosition(),
				SoundEvents.PLAYER_LEVELUP,
				SoundSource.PLAYERS,
				0.65F,
				Math.min(1.8F, 0.95F + result.newLevel() * 0.02F)
		);
	}

	public static void applyRelicProgress(EchoWarriorEntity echo, ItemStack relic, boolean preserveHealthGain) {
		LivingEntity entity = echo.livingEntity();
		double newMaximumHealth = EchoRelicState.maximumHealth(relic);
		AttributeInstance maximumHealth = entity.getAttribute(Attributes.MAX_HEALTH);
		AttributeInstance attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
		double oldBaseMaximumHealth = maximumHealth == null ? newMaximumHealth : maximumHealth.getBaseValue();
		if (maximumHealth != null) {
			maximumHealth.setBaseValue(newMaximumHealth);
		}
		if (attackDamage != null) {
			attackDamage.setBaseValue(EchoRelicState.attackDamage(relic));
		}
		echo.applyRelicState(relic, false);
		if (preserveHealthGain) {
			float gained = (float)Math.max(0.0, newMaximumHealth - oldBaseMaximumHealth);
			entity.setHealth(Math.min(entity.getMaxHealth(), entity.getHealth() + gained));
		} else {
			entity.setHealth(entity.getMaxHealth());
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
