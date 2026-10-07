package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.entity.*;
import com.yuriscat.echowarrior.compat.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Isolated-server fixtures only. Actual entity creation, NBT, authority and combat; not visual acceptance. */
public final class HeroLifecycleSelfTest1201 {
    private static int checks;
    private HeroLifecycleSelfTest1201() {}

    public static void run(MinecraftServer server) {
        DepartureEffectsSelfTest1201.run(server);
        com.yuriscat.echowarrior.compat.integration.EchoExternalCompanionSelfTest1201.run(server);
        checks = 0;
        ServerLevel level = server.overworld();
        ServerPlayer player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "HeroLifecycleTest"));
        var blocks = new LinkedHashMap<BlockPos, BlockState>();
        var fixtures = new ArrayList<Entity>();
        var bindings = new ArrayList<UUID>();
        try {
            for (int x = 2; x <= 14; x++) for (int z = 2; z <= 14; z++) for (int y = 95; y <= 99; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                blocks.put(pos, level.getBlockState(pos));
                level.setBlock(pos, y == 95 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
            ModContent1201.entities().forEach((id, type) -> check(BuiltInRegistries.ENTITY_TYPE.get(id) == type, "entity registry " + id));
            ModEffects1201.effects().forEach((id, effect) -> check(BuiltInRegistries.MOB_EFFECT.get(id) == effect, "effect registry " + id));
            for (EchoHeroType1201 type : EchoHeroType1201.values()) {
                player.moveTo(8.5, 96, 8.5, 0, 0);
                ItemStack summoner = new ItemStack(ModContent1201.ECHO_SUMMONER);
                player.getInventory().setItem(0, summoner);
                var binding = EchoBindingSystem1201.synchronize(level, summoner);
                bindings.add(binding.summonerId());
                ItemStack relic = new ItemStack(ModContent1201.relic(type));
                EchoRelicState1201.ensureInitialized(relic, player.getRandom(), level.getGameTime());
                EchoRelicState1201.setTraitsForSelfTest(relic, 0, EchoBiomeAffinity1201.OPENLAND);
                var contents = new ArrayList<>(binding.contents());
                contents.set(7, relic);
                check(binding.commitEquipment(binding.stateRevision(), contents), "install relic " + type);
                binding.setFuel(300);
                binding.mirrorTo(summoner);
                long beforeRevision = binding.stateRevision();
                // Outside world bounds and below all terrain: every candidate must fail.
                player.moveTo(8.5, level.getMinBuildHeight() - 30, 8.5, 0, 0);
                var failed = EchoBindingSystem1201.summonNew(level, player, summoner);
                check(failed.failure() == EchoBindingSystem1201.SpawnFailure.NO_SAFE_POSITION
                        && binding.fuel() == 300 && binding.stateRevision() == beforeRevision && !binding.active(),
                        "unsafe creation changes no fuel/revision/generation " + type);
                player.moveTo(8.5, 96, 8.5, 0, 0);
                var attempt = EchoBindingSystem1201.summonNew(level, player, summoner);
                check(attempt.succeeded(), "real spawn " + type + " (" + attempt.failure() + ")");
                var echo = attempt.spirit();
                var living = echo.livingEntity();
                fixtures.add(living);
                check(binding.matches(living.getUUID(), echo.getBindingGeneration()) && level.getEntity(living.getUUID()) == living,
                        "entity + authoritative generation " + type);
                check(binding.fuel() == 300 - SummonerFuel1201.summonCost(relic), "charge once " + type);
                check(Math.abs(living.getMaxHealth() - type.baseMaximumHealth()) < 0.01
                        && living.getHealth() == living.getMaxHealth(), "full initial HP " + type);
                var tagged = new CompoundTag();
                check(living.save(tagged), "entity NBT save " + type);
                var restored = (EchoWarriorEntity1201)EntityType.loadEntityRecursive(tagged, level, entity -> entity);
                check(restored != null && restored.heroType() == type
                        && restored.getSummonerId().equals(binding.summonerId())
                        && restored.getBindingGeneration() == binding.generation(), "entity NBT load " + type);
                // UUID aliases are not added to the world; a mismatched physical entity must be rejected.
                restored.livingEntity().setUUID(UUID.randomUUID());
                check(!EchoBindingSystem1201.validateAndTrack(restored), "stale physical entity rejected " + type);

                double baseAttack = living.getAttributeValue(Attributes.ATTACK_DAMAGE);
                var equipped = new ArrayList<>(binding.contents());
                equipped.set(0, new ItemStack(ModContent1201.BATTLE_WORN_WHETSTONE_ACCESSORY));
                check(binding.commitEquipment(binding.stateRevision(), equipped), "accessory commit " + type);
                echo.applyBindingState(binding, false);
                double enhanced = living.getAttributeValue(Attributes.ATTACK_DAMAGE);
                echo.applyBindingState(binding, false);
                check(enhanced > baseAttack && living.getAttributeValue(Attributes.ATTACK_DAMAGE) == enhanced,
                        "stable UUID modifiers do not stack " + type);
                living.setHealth(living.getMaxHealth() - 3);
                living.setAbsorptionAmount(2);
                EchoBindingSystem1201.track(echo);
                check(binding.snapshot().health() == living.getHealth() && binding.snapshot().absorption() == 2,
                        "health snapshot " + type);
                long beforeTracking = binding.stateRevision();
                living.setPos(living.getX() + .01, living.getY(), living.getZ());
                EchoBindingSystem1201.track(echo);
                check(binding.stateRevision() == beforeTracking, "movement doesn't stale equipment clicks " + type);
                if (type == EchoHeroType1201.EGYPTIAN_ARCHER) testArrow(level, echo, fixtures);
                if (type == EchoHeroType1201.ROMAN_LEGIONARY) TalentIntegrationSelfTest1201.run(level, player, binding, echo, fixtures);
                if (living instanceof AztecWarriorEchoEntity1201 aztec)
                    com.yuriscat.echowarrior.compat.entity.behavior.MeleeReachSelfTest1201.run(level, aztec);

                var target = EntityType.ZOMBIE.create(level);
                fixtures.add(target);
                target.moveTo(8.5, 96, 12.5, 0, 0);
                target.setNoAi(true);
                level.addFreshEntity(target);
                int previousXp = EchoRelicProgress1201.experience(binding.relic());
                target.hurt(level.damageSources().mobAttack(living), 1000);
                check(target.isDeadOrDying() && EchoRelicProgress1201.experience(binding.relic()) > previousXp,
                        "kill awards relic XP through actual death hook " + type);
                target.discard();

                UUID oldSpirit = binding.spiritId();
                long oldGeneration = binding.generation();
                check(EchoBindingSystem1201.dismiss(level, summoner) && living.isRemoved() && !binding.active(),
                        "dismiss actual entity " + type);
                var next = EchoBindingSystem1201.summonNew(level, player, summoner);
                check(next.succeeded(), "resummon " + type);
                fixtures.add(next.spirit().livingEntity());
                check(binding.generation() > oldGeneration && !binding.matches(oldSpirit, oldGeneration), "old generation invalidated " + type);
                // A second holder changes FOLLOW explicitly; other editing is not a control transfer.
                var holder = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "HeroSecondHolder"));
                holder.moveTo(8.5, 96, 8.5, 0, 0);
                player.getInventory().setItem(0, ItemStack.EMPTY);
                holder.getInventory().setItem(0, summoner);
                var menu = new com.yuriscat.echowarrior.compat.menu.SummonerMenu1201(6, holder.getInventory(), 0);
                menu.clickMenuButton(holder, com.yuriscat.echowarrior.compat.menu.SummonerMenu1201.BUTTON_ALERT_START + 2);
                check(player.getUUID().equals(binding.controllerId()), "ordinary menu edit keeps controller " + type);
                long priorTransferGeneration = binding.generation();
                next.spirit().livingEntity().setHealth(next.spirit().livingEntity().getMaxHealth() - 4);
                next.spirit().livingEntity().setAbsorptionAmount(3);
                float transferHp = next.spirit().livingEntity().getHealth();
                int priorTransferFuel = binding.fuel();
                menu.clickMenuButton(holder, com.yuriscat.echowarrior.compat.menu.SummonerMenu1201.BUTTON_ACTIVITY_START);
                var transferred = EchoBindingSystem1201.findLoaded(server, binding.spiritId());
                check(transferred != null && holder.getUUID().equals(binding.controllerId())
                        && binding.generation() > priorTransferGeneration && next.spirit().livingEntity().isRemoved(), "explicit FOLLOW replaces old controller/generation " + type);
                fixtures.add(transferred.livingEntity());
                check(transferred.livingEntity().getHealth() == transferHp && transferred.livingEntity().getAbsorptionAmount() == 3
                        && binding.fuel() == priorTransferFuel, "transfer preserves HP/absorption/fuel " + type);
                holder.getInventory().setItem(0, ItemStack.EMPTY);
                ItemStack physical = summoner;
                if (type == EchoHeroType1201.EGYPTIAN_ARCHER || type == EchoHeroType1201.GUANDAO_WARRIOR
                        || type == EchoHeroType1201.JAPANESE_SAMURAI) {
                    physical = new ItemStack(net.minecraft.world.item.Items.SHULKER_BOX);
                    var nested = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
                    nested.set(0, summoner);
                    var containerTag = new CompoundTag();
                    net.minecraft.world.ContainerHelper.saveAllItems(containerTag, nested);
                    physical.getOrCreateTag().put("BlockEntityTag", containerTag);
                }
                var dropped = new net.minecraft.world.entity.item.ItemEntity(level, 10, 96, 10, physical);
                fixtures.add(dropped); level.addFreshEntity(dropped); dropped.setNeverPickUp(); dropped.tick();
                check(binding.active() && !transferred.livingEntity().isRemoved(), "ordinary physical drop retains echo " + type);
                if (type == EchoHeroType1201.EGYPTIAN_ARCHER) {
                    dropped.hurt(level.damageSources().generic(), 1000);
                    var spilled = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                            new net.minecraft.world.phys.AABB(6, 92, 6, 14, 100, 14),
                            entity -> binding.summonerId().equals(SummonerData1201.summonerId(entity.getItem())));
                    fixtures.addAll(spilled);
                    check(dropped.isRemoved() && spilled.size() == 1 && binding.active() && !transferred.livingEntity().isRemoved(),
                            "destroying shulker spills live summoner without destroying hero");
                    spilled.get(0).hurt(level.damageSources().generic(), 1000);
                } else if (type == EchoHeroType1201.JAPANESE_SAMURAI) {
                    dropped.setPos(10, level.getMinBuildHeight() - 100, 10);
                    dropped.tick();
                } else if (type == EchoHeroType1201.ROMAN_LEGIONARY) dropped.hurt(level.damageSources().generic(), 1000);
                else { dropped.makeFakeItem(); dropped.tick(); }
                check(dropped.isRemoved() && transferred.livingEntity().isRemoved()
                        && EchoBindingSavedData1201.get(server).get(binding.summonerId()) == null, "permanent destroy entity + record " + type);
            }
            check(((TalentExperienceHolder1201)player).echoWarrior1201$consumeMentorBonus(2) == 1, "player talent mixin active");
            UUID warned = UUID.randomUUID();
            var warnings = new EchoBindingSavedData1201();
            check(warnings.tryWarning(warned, 1_000_000) && !warnings.tryWarning(warned, 1_000_001), "warning throttle");
            var warningReload = EchoBindingSavedData1201.load(warnings.save(new CompoundTag()));
            check(!warningReload.tryWarning(warned, 1_000_002) && warningReload.tryWarning(warned, 4_600_000), "warning throttle survives save/load");
            EchoWarrior1201.LOGGER.info("[Compat1201] HERO LIFECYCLE SELFTEST PASSED checks={} heroes=5", checks);
        } finally {
            fixtures.forEach(entity -> { if (entity != null && !entity.isRemoved()) entity.discard(); });
            bindings.forEach(id -> EchoBindingSystem1201.destroySummoner(level, id));
            blocks.forEach((pos, state) -> level.setBlock(pos, state, 2));
            // This isolated fixture never joins the player list or writes playerdata.
        }
    }

    private static void testArrow(ServerLevel level, EchoWarriorEntity1201 echo, List<Entity> fixtures) {
        var arrow = ModContent1201.EGYPTIAN_ARCHER_ARROW.create(level);
        arrow.moveTo(10, 98, 10, 0, 0);
        arrow.configure(echo.livingEntity(), EchoRelicState1201.EgyptianArrowMode.CONE, 7, true);
        level.addFreshEntity(arrow);
        fixtures.add(arrow);
        CompoundTag state = new CompoundTag();
        check(arrow.save(state), "arrow serialized while alive");
        var copy = (EgyptianArcherArrowEntity1201)EntityType.loadEntityRecursive(state, level, entity -> entity);
        check(copy != null && copy.pickup == AbstractArrow.Pickup.DISALLOWED
                && copy.arrowMode() == EchoRelicState1201.EgyptianArrowMode.CONE, "arrow NBT preserves mode + no pickup");
        state.putLong("BindingGeneration", echo.getBindingGeneration() + 1);
        copy.readAdditionalSaveData(state);
        copy.tick();
        check(copy.isRemoved(), "stale generation arrow invalidated");
        arrow.discard();
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("[Compat1201] Hero lifecycle: " + label);
        checks++;
    }
}
