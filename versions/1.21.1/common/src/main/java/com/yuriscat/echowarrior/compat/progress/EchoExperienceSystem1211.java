package com.yuriscat.echowarrior.compat.progress;

import com.yuriscat.echowarrior.compat.EchoHeroType1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicItem1211;
import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicProgress1211;
import com.yuriscat.echowarrior.compat.item.EchoRelicState1211;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

public final class EchoExperienceSystem1211 {
    private static final long CREDIT_WINDOW_TICKS = 20L * 10L;
    private static final Map<LivingEntity, Participation> LAST_ECHO_CREDIT = new WeakHashMap<>();

    private EchoExperienceSystem1211() {
    }

    public static void afterDamage(LivingEntity victim, DamageSource source, float damageTaken, boolean blocked) {
        if (damageTaken <= 0.0F || blocked) return;
        EchoWarriorEntity1211 echo = resolveAttackingEcho(source);
        if (echo != null) {
            markParticipation(echo, victim);
        } else if (source.getEntity() != null) {
            LAST_ECHO_CREDIT.remove(victim);
        }
    }

    public static void afterDeath(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level)) return;
        EchoWarriorEntity1211 direct = resolveAttackingEcho(source);
        Participation credit = LAST_ECHO_CREDIT.remove(victim);
        if (direct != null) credit = participationFor(direct);
        if (credit == null || level.getGameTime() - credit.lastParticipationTick() > CREDIT_WINDOW_TICKS) return;

        EchoWarriorEntity1211 echo = EchoBindingSystem1211.findLoaded(
                level.getServer(), credit.echoUuid());
        if (echo != null && EchoAccessorySystem1211.has(echo, ModContent1211.VICTORS_LAUREL_ACCESSORY)) {
            echo.livingEntity().heal(EchoAccessorySystem1211.victorHealAmount(echo.livingEntity().getMaxHealth()));
        }
        tryDropLegacy(level, victim, credit.summonerUuid());
        int reward = victim.getExperienceReward(level, echo == null ? null : echo.livingEntity());
        if (reward <= 0) return;
        int worldReward = EchoAccessorySystem1211.worldExperienceReward(reward,
                hasAccessory(level, credit.summonerUuid(), ModContent1211.LIGHT_GATHERING_MAGNET_ACCESSORY));
        ExperienceOrb.award(level, victim.position(), worldReward);
        int growthReward = EchoAccessorySystem1211.growthExperienceReward(reward,
                hasAccessory(level, credit.summonerUuid(), ModContent1211.TRAINING_NOTES_ACCESSORY));
        awardGrowthExperience(level, credit, growthReward);
    }

    public static void markParticipation(EchoWarriorEntity1211 echo, LivingEntity target) {
        if (!(echo.livingEntity().level() instanceof ServerLevel) || target == echo.livingEntity() || !target.isAlive()) return;
        Participation participation = participationFor(echo);
        if (participation != null) LAST_ECHO_CREDIT.put(target, participation);
    }

    private static EchoWarriorEntity1211 resolveAttackingEcho(DamageSource source) {
        if (source.getEntity() instanceof EchoWarriorEntity1211 echo) return echo;
        if (source.getDirectEntity() instanceof EchoWarriorEntity1211 echo) return echo;
        return source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof EchoWarriorEntity1211 echo ? echo : null;
    }

    private static boolean hasAccessory(ServerLevel level, UUID summonerId, net.minecraft.world.item.Item item) {
        return EchoBindingSystem1211.accessories(level, summonerId).stream().anyMatch(stack -> stack.is(item));
    }

    private static void tryDropLegacy(ServerLevel level, LivingEntity victim, UUID summonerId) {
        if (!hasAccessory(level, summonerId, ModContent1211.MEMORY_RITUAL_KNIFE_ACCESSORY)
                || level.getRandom().nextFloat() >= 0.005F) return;
        List<net.minecraft.world.item.Item> legacies = List.of(
                ModContent1211.COURAGE_LEGACY,
                ModContent1211.FORTITUDE_LEGACY,
                ModContent1211.PURITY_LEGACY,
                ModContent1211.WISDOM_LEGACY,
                ModContent1211.CRAFT_LEGACY
        );
        ItemStack drop = new ItemStack(legacies.get(level.getRandom().nextInt(legacies.size())));
        ItemEntity entity = new ItemEntity(level, victim.getX(), victim.getY() + 0.35, victim.getZ(), drop);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    private static Participation participationFor(EchoWarriorEntity1211 echo) {
        if (!(echo.livingEntity().level() instanceof ServerLevel level) || echo.getOwnerUUID() == null
                || echo.getSummonerId() == null) return null;
        return new Participation(echo.livingEntity().getUUID(), echo.getOwnerUUID(), echo.getSummonerId(), level.getGameTime());
    }

    private static void awardGrowthExperience(ServerLevel level, Participation participation, int amount) {
        ItemStack relic = EchoBindingSystem1211.relic(level, participation.summonerUuid());
        if (!(relic.getItem() instanceof EchoRelicItem1211 relicItem)) return;
        amount = EchoRelicState1211.addWiseGrowthExperience(relic, amount);
        EchoRelicProgress1211.ProgressResult result = EchoRelicProgress1211.addExperience(relic, amount);
        EchoBindingSystem1211.persistRelic(level, participation.summonerUuid(), relic);
        if (result.levelsGained() <= 0) return;

        EchoWarriorEntity1211 echo = EchoBindingSystem1211.findLoaded(
                level.getServer(), participation.echoUuid());
        if (echo != null && echo.livingEntity().isAlive()) echo.applyRelicState(relic, true);
        UUID controllerId = EchoBindingSystem1211.controllerId(level, participation.summonerUuid());
        ServerPlayer owner = controllerId == null ? null
                : level.getServer().getPlayerList().getPlayer(controllerId);
        if (owner == null) return;
        EchoHeroType1211 heroType = relicItem.heroType();
        owner.sendSystemMessage(Component.translatable(
                "message.echo_warrior.relic.level_up",
                Component.translatable(heroType.translationKey()),
                result.newLevel()), true);
        level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                0.65F, Math.min(1.8F, 0.95F + result.newLevel() * 0.02F));
    }

    private record Participation(UUID echoUuid, UUID ownerUuid, UUID summonerUuid,
                                 long lastParticipationTick) {
    }
}
