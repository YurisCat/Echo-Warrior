package com.yuriscat.echowarrior.progress;

import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.binding.EchoBindingSystem;
import com.yuriscat.echowarrior.entity.EchoWarriorEntity;
import com.yuriscat.echowarrior.item.EchoAccessorySystem;
import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.item.EchoRelicProgress;
import com.yuriscat.echowarrior.item.EchoRelicState;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
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
		EchoWarriorEntity echo = EchoBindingSystem.findLoadedSpirit(level.getServer(), credit.summonerUuid());

		if (echo != null && EchoAccessorySystem.has(echo, ModItems.VICTORS_LAUREL_ACCESSORY)) {
			echo.livingEntity().heal(echo.livingEntity().getMaxHealth() * 0.10F);
		}
		if (hasAccessory(level, credit.summonerUuid(), ModItems.MEMORY_RITUAL_KNIFE_ACCESSORY)
				&& victim.getRandom().nextFloat() < 0.005F) {
			ItemStack legacy = new ItemStack(switch (victim.getRandom().nextInt(5)) {
				case 0 -> ModItems.COURAGE_LEGACY;
				case 1 -> ModItems.FORTITUDE_LEGACY;
				case 2 -> ModItems.PURITY_LEGACY;
				case 3 -> ModItems.WISDOM_LEGACY;
				default -> ModItems.CRAFT_LEGACY;
			});
			level.addFreshEntity(new ItemEntity(level, victim.getX(), victim.getY() + 0.35, victim.getZ(), legacy));
		}

		int reward = victim.getExperienceReward(level, echo == null ? null : echo.livingEntity());
		if (reward <= 0) return;
		int worldReward = scaledReward(reward,
				hasAccessory(level, credit.summonerUuid(), ModItems.LIGHT_GATHERING_MAGNET_ACCESSORY));
		int growthReward = scaledReward(reward,
				hasAccessory(level, credit.summonerUuid(), ModItems.TRAINING_NOTES_ACCESSORY));
		ExperienceOrb.award(level, victim.position(), worldReward);
		awardExperience(level, credit, growthReward);
	}

	private static int scaledReward(int base, boolean boosted) {
		return Math.max(1, boosted ? Math.round(base * 1.5F) : base);
	}

	private static boolean hasAccessory(ServerLevel level, UUID summonerUuid, net.minecraft.world.item.Item item) {
		return EchoBindingSystem.accessories(level, summonerUuid).stream().anyMatch(stack -> stack.is(item));
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
		ItemStack relic = EchoBindingSystem.relic(level, participation.summonerUuid());
		if (!(relic.getItem() instanceof EchoRelicItem)) {
			return;
		}
		amount = EchoRelicState.addWiseGrowthExperience(relic, amount);

		EchoRelicProgress.ProgressResult result = EchoRelicProgress.addExperience(relic, amount);
		EchoBindingSystem.persistRelic(level, participation.summonerUuid(), relic);

		if (result.levelsGained() <= 0) {
			return;
		}
		EchoWarriorEntity echo = EchoBindingSystem.findLoadedSpirit(level.getServer(), participation.summonerUuid());
		if (echo != null && echo.livingEntity().isAlive()) {
			applyRelicProgress(echo, relic, true);
		}
		UUID controllerId = EchoBindingSystem.controllerId(level, participation.summonerUuid());
		ServerPlayer owner = controllerId == null ? null : level.getServer().getPlayerList().getPlayer(controllerId);
		if (owner == null) return;
		EchoHeroType heroType = EchoHeroType.fromRelic(relic);
		owner.sendOverlayMessage(Component.translatable(
				"message.echo_warrior.relic.level_up",
				Component.translatable(heroType.nameTranslationKey()),
				result.newLevel()
		));
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
