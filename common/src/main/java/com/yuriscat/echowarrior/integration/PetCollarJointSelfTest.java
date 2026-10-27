package com.yuriscat.echowarrior.integration;

import com.yuriscat.echowarrior.binding.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.UUID;

/** Installed-mod integration: the ordinary player interaction dispatch equips real collar items. */
public final class PetCollarJointSelfTest {
    private PetCollarJointSelfTest() {}

    public static void run(MinecraftServer server) {
        if (!Boolean.getBoolean("echo_warrior.pet_collar_test"))
            throw new IllegalStateException("Pet collar test requires an isolated test-server flag");
        if (!PetCollarCompat.installed()) throw new IllegalStateException("Required Pet Home adapter absent");
        var level = server.overworld();
        var player = new ServerPlayer(server, level,
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "EchoCollarTest"),
                net.minecraft.server.level.ClientInformation.createDefault());
        var previous = new java.util.LinkedHashMap<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState>();
        var summoner = new ItemStack(com.yuriscat.echowarrior.ModItems.TEST_ECHO_SUMMONER);
        try {
            for (int x = 34; x <= 46; x++) for (int z = 34; z <= 46; z++) for (int y = 95; y <= 99; y++) {
                var pos = new net.minecraft.core.BlockPos(x, y, z);
                check(level.getBlockEntity(pos) == null, "isolated fixture has no block entities");
                previous.put(pos, level.getBlockState(pos));
                level.setBlock(pos, y == 95 ? net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
            player.snapTo(40.5, 96, 40.5, 0, 0);
            var relic = new ItemStack(com.yuriscat.echowarrior.ModItems.ROMAN_LEGIONARY_RELIC);
            com.yuriscat.echowarrior.item.EchoRelicState.ensureInitialized(relic, player.getRandom(), level.getGameTime());
            com.yuriscat.echowarrior.item.TestEchoSummonerItem.setRelicStack(summoner, relic);
            com.yuriscat.echowarrior.item.SummonerFuel.setAmount(summoner, 300);
            player.getInventory().setItem(1, summoner);
            check(summon(player, summoner).succeeded(), "initial mainline summon");
            var binding = EchoBindingSavedData.get(server).get(
                    com.yuriscat.echowarrior.item.TestEchoSummonerItem.getSummonerId(summoner).orElseThrow());
            run(server, player, binding, summoner);
        } finally {
            com.yuriscat.echowarrior.item.TestEchoSummonerItem.getSummonerId(summoner)
                    .ifPresent(id -> EchoBindingSystem.dismiss(server, id, "collar_test_cleanup"));
            previous.forEach((pos, state) -> level.setBlock(pos, state, 2));
        }
    }

    private static com.yuriscat.echowarrior.entity.EchoWarriorEntity findLoaded(MinecraftServer server, UUID id) {
        for (var level : server.getAllLevels())
            if (level.getEntity(id) instanceof com.yuriscat.echowarrior.entity.EchoWarriorEntity echo) return echo;
        return null;
    }
    private static boolean dismiss(MinecraftServer server, UUID id) {
        return EchoBindingSystem.dismiss(server, id, "collar_test");
    }
    private record Result(boolean succeeded) {}
    private static Result summon(ServerPlayer player, ItemStack stack) {
        return new Result(com.yuriscat.echowarrior.item.TestEchoSummonerItem.summonFromMenu(player, stack)
                == com.yuriscat.echowarrior.item.TestEchoSummonerItem.SummonResult.SUMMONED);
    }
    private static void fund(ServerPlayer player, ItemStack stack, int amount) {
        EchoBindingSystem.registerOrSynchronize(player.level(), stack);
        com.yuriscat.echowarrior.item.SummonerFuel.setAmount(stack, amount);
        EchoBindingSystem.commitPhysicalStack(player.level(), stack);
    }

    public static void run(MinecraftServer server, ServerPlayer player,
                           EchoBindingSavedData.Binding binding, ItemStack summoner) {
        if (!PetCollarCompat.installed()) return;
        var level = player.level();
        var oldConnection = player.connection;
        player.connection = new Sink(server, player);
        // TamableAnimal resolves ownership through the level's player list. This fixture does
        // not join a network server or claim to test real multiplayer.
        boolean added = !level.players().contains(player);
        if (added) level.players().add(player);
        var area = new AABB(30, 90, 30, 50, 105, 50);
        var previousDrops = new HashSet<UUID>();
        for (var drop : level.getEntitiesOfClass(ItemEntity.class, area)) previousDrops.add(drop.getUUID());
        try {
            try {
                Class<?> trackerType = Class.forName("com.github.yzqdev.pethome.server.misc.CollarTickTracker");
                Object tracker = trackerType.getConstructor().newInstance();
                var add = trackerType.getMethod("addBlockedEntityTick", UUID.class, int.class);
                add.invoke(tracker, UUID.randomUUID(), 0);
                add.invoke(tracker, UUID.randomUUID(), 0);
                trackerType.getMethod("tick").invoke(tracker);
                var field = trackerType.getDeclaredField("blockedCollarTagUpdates");
                field.setAccessible(true);
                check(((java.util.Map<?, ?>)field.get(tracker)).isEmpty(), "two collar timers expire safely together");
            } catch (ClassNotFoundException domesticationOnly) { /* no Pet Home tracker */ }
            var first = findLoaded(server, binding.entityId());
            check(first != null, "initial entity visible");
            LivingEntity living = first.livingEntity();
            double baseMaximum = living.getMaxHealth();
            equip(player, living, "health_boost", 2);
            check(close(living.getMaxHealth(), baseMaximum + 20), "actual equipped health modifier");
            check(!com.yuriscat.echowarrior.item.TestEchoSummonerItem.relicStack(binding.summonerState()).getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag()
                    .getCompoundOrEmpty(PetCollarCompat.KEY).getCompoundOrEmpty("pet_home").contains("CollarSwapCooldown"),
                    "write-only upstream marker must not become an ever-extending relic deadline");
            PetCollarDiskSelfTest.verify(server, com.yuriscat.echowarrior.item.TestEchoSummonerItem.relicStack(binding.summonerState()), living.getMaxHealth());
            verifyRequests(player);
            verifyRawPacketAuthority(server, player, living, "pet_home");
            check(com.yuriscat.echowarrior.item.TestEchoSummonerItem.relicStack(binding.summonerState()).getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains(PetCollarCompat.KEY),
                    "actual item interaction immediately persists collar");
            UUID stable = binding.summonerId();
            var collarItem = BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath("pet_home", "collar_tag")).orElseThrow().value();
            int dropsBeforeRecall = collarDrops(living, collarItem);
            for (int i = 0; i < 25; i++) {
                var old = findLoaded(server, binding.entityId());
                check(dismiss(server, binding.summonerId()), "dismiss " + i);
                fund(player, summoner, 300);
                check(summon(player, summoner).succeeded(), "resummon " + i);
                living = findLoaded(server, binding.entityId()).livingEntity();
                check(close(living.getMaxHealth(), baseMaximum + 20)
                        && close(living.getHealth(), living.getMaxHealth()), "restored health, no modifier stacking " + i);
                check(!PetCollarCompat.capture(old.livingEntity()), "old incarnation cannot overwrite relic");
                check(EchoBindingSystem.countActive(server, player.getUUID(), null) == 1, "one active binding");
            }
            check(collarDrops(living, collarItem) == dropsBeforeRecall, "dismissal retains collar without returning a duplicate item");
            verifyCompass(server, player, binding, summoner);
            living = findLoaded(server, binding.entityId()).livingEntity();
            var renamed = net.minecraft.network.chat.Component.literal("Collar transfer test")
                    .withStyle(net.minecraft.ChatFormatting.GOLD);
            living.setCustomName(renamed);
            var recipient = new ServerPlayer(server, level,
                    new com.mojang.authlib.GameProfile(UUID.randomUUID(), "CollarRecipient"),
                    net.minecraft.server.level.ClientInformation.createDefault());
            recipient.connection = new Sink(server, recipient);
            recipient.snapTo(player.getX(), player.getY(), player.getZ(), 0, 0);
            level.players().add(recipient);
            try {
                var departed = living;
                check(EchoBindingSystem.transferToFollowing(recipient, binding.summonerId()), "native controller transfer");
                living = findLoaded(server, binding.entityId()).livingEntity();
                check(departed.isRemoved() && renamed.equals(living.getCustomName())
                        && close(living.getMaxHealth(), baseMaximum + 20)
                        && recipient.getUUID().equals(PetCollarCompat.ownerId(living))
                        && !PetCollarCompat.capture(departed), "transfer preserves collar and recent styled name, revokes old generation");
                check(EchoBindingSystem.transferToFollowing(player, binding.summonerId()), "native controller transfer back");
                living = findLoaded(server, binding.entityId()).livingEntity();
                check(player.getUUID().equals(PetCollarCompat.ownerId(living)), "pet ownership follows current native controller");
            } finally { level.players().remove(recipient); }
            // Actual death must keep the collar while ending the old binding.
            var dead = living;
            living.hurtServer(level, living.damageSources().genericKill(), Float.MAX_VALUE);
            check(!binding.active() && dead.isDeadOrDying(), "actual death ends old binding");
            check(collarDrops(dead, collarItem) == dropsBeforeRecall, "death retains collar without a duplicate drop");
            fund(player, summoner, 300);
            check(summon(player, summoner).succeeded(), "summon after actual death");
            living = findLoaded(server, binding.entityId()).livingEntity();
            check(close(living.getMaxHealth(), baseMaximum + 20), "collar survives actual death");
            check(!PetCollarCompat.capture(dead), "dead incarnation cannot write into new generation");
            check(dismiss(server, binding.summonerId()), "dismiss before insufficient fuel");
            var preserved = com.yuriscat.echowarrior.item.TestEchoSummonerItem.relicStack(binding.summonerState()).copy();
            fund(player, summoner, 0);
            check(!summon(player, summoner).succeeded() && !binding.active()
                    && ItemStack.matches(preserved, com.yuriscat.echowarrior.item.TestEchoSummonerItem.relicStack(binding.summonerState())), "failed summon preserves collar and inactive state");
            fund(player, summoner, 300);
            check(summon(player, summoner).succeeded(), "retry after insufficient fuel");
            living = findLoaded(server, binding.entityId()).livingEntity();
            check(dismiss(server, binding.summonerId()), "dismiss before unknown-schema test");
            var validRelic = com.yuriscat.echowarrior.item.TestEchoSummonerItem.relicStack(binding.summonerState()).copy();
            var unsupported = validRelic.copy();
            var unsupportedTag = unsupported.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            unsupportedTag.getCompoundOrEmpty(PetCollarCompat.KEY).getCompoundOrEmpty("pet_home").putInt("Schema", 999);
            unsupported.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(unsupportedTag));
            EchoBindingSystem.persistRelic(level, binding.summonerId(), unsupported);
            fund(player, summoner, 300);
            check(!summon(player, summoner).succeeded() && !binding.active() && com.yuriscat.echowarrior.item.SummonerFuel.amount(binding.summonerState()) == 300
                    && ItemStack.matches(unsupported, com.yuriscat.echowarrior.item.TestEchoSummonerItem.relicStack(binding.summonerState())),
                    "unknown schema refuses creation without consuming fuel or erasing saved collar");
            EchoBindingSystem.persistRelic(level, binding.summonerId(), validRelic);
            check(summon(player, summoner).succeeded(), "valid schema recovers after rejected future data");
            living = findLoaded(server, binding.entityId()).livingEntity();
            equip(player, living, "speedster", 1);
            check(close(living.getMaxHealth(), baseMaximum), "replacement removes health modifier");
            double speed = living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            check(dismiss(server, binding.summonerId()), "dismiss replacement");
            fund(player, summoner, 300);
            check(summon(player, summoner).succeeded(), "resummon replacement");
            living = findLoaded(server, binding.entityId()).livingEntity();
            check(close(living.getMaxHealth(), baseMaximum)
                    && close(living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED), speed),
                    "replacement state survives without stale health enchant");
            var utility = Class.forName("com.github.yzqdev.pethome.util.TameableUtils");
            utility.getMethod("setPsychicWallCooldown", LivingEntity.class, int.class).invoke(null, living, 137);
            check(dismiss(server, binding.summonerId()), "dismiss cooldown");
            fund(player, summoner, 300);
            check(summon(player, summoner).succeeded(), "resummon cooldown");
            living = findLoaded(server, binding.entityId()).livingEntity();
            check((Integer)utility.getMethod("getPsychicWallCooldown", LivingEntity.class).invoke(null, living) == 137,
                    "dismiss does not reset earned cooldown");
            utility.getMethod("setPetBedPos", LivingEntity.class, net.minecraft.core.BlockPos.class)
                    .invoke(null, living, living.blockPosition());
            check(utility.getMethod("getPetBedPos", LivingEntity.class).invoke(null, living) == null,
                    "external bed cannot create a separate resurrection path");
            try {
                var netType = Class.forName("com.github.yzqdev.pethome.server.item.NetItem");
                check(Boolean.FALSE.equals(netType.getMethod("canCatchMob", net.minecraft.world.entity.Entity.class)
                        .invoke(null, living)), "actual capture gate protects bound hero");
                var legacy = new ItemStack(BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath("pet_home", "net_has_item")).orElseThrow().value());
                var captured = new net.minecraft.nbt.CompoundTag();
                captured.putString("id", "echo_warrior:roman_legionary_echo");
                Object holder;
                try { holder = Class.forName("com.github.yzqdev.pethome.server.PHDataComponents").getField("ENTITY_HOLDER").get(null); }
                catch (ClassNotFoundException missingSchema) { throw new IllegalStateException("Known net schema missing", missingSchema); }
                if (holder instanceof java.util.function.Supplier<?> deferred) holder = deferred.get();
                legacy.set((net.minecraft.core.component.DataComponentType<net.minecraft.nbt.CompoundTag>)holder, captured);
                var originalLegacy = legacy.copy();
                player.setItemInHand(InteractionHand.MAIN_HAND, legacy);
                var hit = new net.minecraft.world.phys.BlockHitResult(living.position(), net.minecraft.core.Direction.UP,
                        living.blockPosition(), false);
                var use = new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit);
                check(legacy.getItem().useOn(use) == net.minecraft.world.InteractionResult.FAIL
                        && ItemStack.matches(legacy, originalLegacy), "legacy captured Echo is not reconstructed or consumed");
                Class<?> projectileType = Class.forName("com.github.yzqdev.pethome.server.entity.NetEntity");
                var projectile = (net.minecraft.world.entity.Entity)projectileType.getConstructor(double.class, double.class,
                        double.class, net.minecraft.world.level.Level.class, ItemStack.class)
                        .newInstance(player.getX(), player.getY(), player.getZ(), level, originalLegacy.copy());
                level.addFreshEntity(projectile);
                var collision = java.util.Arrays.stream(projectileType.getDeclaredMethods()).filter(m ->
                        m.getParameterCount() == 1 && m.getParameterTypes()[0] == net.minecraft.world.phys.HitResult.class)
                        .findFirst().orElseThrow();
                collision.setAccessible(true);
                int beforeReturned = level.getEntitiesOfClass(ItemEntity.class, area,
                        e -> ItemStack.matches(e.getItem(), originalLegacy)).size();
                collision.invoke(projectile, hit);
                collision.invoke(projectile, hit);
                check(projectile.isRemoved() && level.getEntitiesOfClass(ItemEntity.class, area,
                        e -> ItemStack.matches(e.getItem(), originalLegacy)).size() == beforeReturned + 1,
                        "legacy net projectile returns its stored item exactly once");
            } catch (ClassNotFoundException domesticationOnly) { /* original mod has no capture net */ }
            var oreTag = net.minecraft.tags.TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", "ores"));
            check(net.minecraft.world.level.block.Blocks.IRON_ORE.defaultBlockState().is(oreTag),
                    "ore scenting resolves actual iron ore tag across loaders");
            var ordinary = net.minecraft.world.entity.EntityType.WOLF.create(level, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
            check(ordinary != null, "ordinary wolf fixture");
            try {
                utility.getMethod("setPetBedPos", LivingEntity.class, net.minecraft.core.BlockPos.class)
                        .invoke(null, ordinary, living.blockPosition());
                check(living.blockPosition().equals(utility.getMethod("getPetBedPos", LivingEntity.class).invoke(null, ordinary)),
                        "ordinary pets retain their bed binding");
            } finally { ordinary.discard(); }
            int catalogueCount = 0;
            for (var enchantId : player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElementIds().map(net.minecraft.resources.ResourceKey::identifier).toList()) {
                if (!enchantId.getNamespace().equals("pet_home")) continue;
                equip(player, living, enchantId.getPath(), 1);
                var equipped = (java.util.Map<?, ?>)utility.getMethod("getEnchants", LivingEntity.class).invoke(null, living);
                check(Integer.valueOf(1).equals(equipped.get(enchantId)), "catalogue item applied: " + enchantId);
                check(dismiss(server, binding.summonerId()), "catalogue dismissal: " + enchantId);
                fund(player, summoner, 300);
                check(summon(player, summoner).succeeded(), "catalogue resummon: " + enchantId);
                living = findLoaded(server, binding.entityId()).livingEntity();
                var restored = (java.util.Map<?, ?>)utility.getMethod("getEnchants", LivingEntity.class).invoke(null, living);
                check(equipped.equals(restored), "catalogue persisted without loss: " + enchantId);
                int actual = (Integer)utility.getMethod("getEnchantLevel", LivingEntity.class, net.minecraft.resources.ResourceKey.class)
                        .invoke(null, living, net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT, enchantId));
                boolean restricted = enchantId.getPath().equals("total_recall") || enchantId.getPath().equals("undead_curse")
                        || enchantId.getPath().equals("immaturity_curse");
                check(actual == (restricted ? 0 : 1), "catalogue effect lookup policy: " + enchantId);
                catalogueCount++;
            }
            check(catalogueCount >= 25, "full installed enchantment catalogue was exercised");
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info(
                    "[PetCollarCatalogueSelfTest] PASS entries={} persistence=actual-interaction restrictions=preserved", catalogueCount);
            // Remove bonuses through the real empty-collar replacement path before the
            // containing lifecycle fixture continues its unrelated assertions.
            equip(player, living, null, 0);
            check(close(living.getMaxHealth(), baseMaximum), "empty collar clears bonuses");
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info(
                    "[PetCollarJointSelfTest] PASS: actual item interaction, 25 resummons, replacement, death, failed summon, cooldown, capture/bed guards, stale-generation guard");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Pet collar installed API test failed", failure);
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (added) level.players().remove(player);
            // Vanilla trackers may still hold this test player until the enclosing fixture disposes its entities.
            if (oldConnection != null) player.connection = oldConnection;
            for (var drop : level.getEntitiesOfClass(ItemEntity.class, area))
                if (!previousDrops.contains(drop.getUUID())) drop.discard();
        }
    }

    @SuppressWarnings("unchecked")
    private static void verifyRequests(ServerPlayer player) throws ReflectiveOperationException {
        String prefix = "com.github.yzqdev.pethome.server.misc.";
        Class<?> worldType = Class.forName(prefix + "PHWorldData");
        Object world = worldType.getMethod("get", net.minecraft.world.level.Level.class).invoke(null, player.level());
        for (boolean lantern : new boolean[]{false, true}) {
            Class<?> requestType = Class.forName(prefix + (lantern ? "LanternRequest" : "RespawnRequest"));
            var field = worldType.getDeclaredField(lantern ? "lanternRequestList" : "respawnRequestList");
            field.setAccessible(true);
            var stored = (java.util.List<Object>)field.get(world);
            var prior = new java.util.ArrayList<>(stored);
            Object echo = petRequest(requestType, lantern, player, "echo_warrior:roman_legionary_echo");
            Object wolf = petRequest(requestType, lantern, player, "minecraft:wolf");
            var add = worldType.getMethod(lantern ? "addLanternRequest" : "addRespawnRequest", requestType);
            try {
                add.invoke(world, echo);
                check(stored.size() == prior.size(), "no new external Echo resurrection/lantern records accumulate");
                add.invoke(world, wolf);
                check(stored.contains(wolf), "ordinary pet request remains supported");
                // Simulate an old saved request; retrieval filters it without deleting player data.
                stored.add(echo);
                Object results = lantern
                        ? worldType.getMethod("getLanternRequestsFor", UUID.class).invoke(world, player.getUUID())
                        : worldType.getMethod("getRespawnRequestsFor", net.minecraft.world.level.Level.class,
                            net.minecraft.core.BlockPos.class).invoke(world, player.level(), player.blockPosition());
                check(results instanceof java.util.List<?> list && list.contains(wolf) && !list.contains(echo),
                        "old Echo requests cannot spawn independently; ordinary requests still resolve");
            } finally { stored.clear(); stored.addAll(prior); }
        }
        org.slf4j.LoggerFactory.getLogger("echo_warrior").info("[PetCollarRequestSelfTest] PASS new-blocked old-filtered ordinary-preserved");
    }

    private static Object petRequest(Class<?> type, boolean lantern, ServerPlayer player, String entityType)
            throws ReflectiveOperationException {
        if (!lantern) return type.getConstructor(String.class, String.class, net.minecraft.nbt.CompoundTag.class,
                net.minecraft.core.BlockPos.class, long.class, String.class).newInstance(entityType,
                player.level().dimension().toString(), new net.minecraft.nbt.CompoundTag(), player.blockPosition(), 0L, "Collar test");
        for (var constructor : type.getConstructors()) {
            if (constructor.getParameterCount() == 6) return constructor.newInstance(UUID.randomUUID(), entityType,
                    player.getUUID(), player.blockPosition(), 0L, "Collar test");
            if (constructor.getParameterCount() == 7) return constructor.newInstance(UUID.randomUUID(), entityType,
                    player.getUUID(), player.blockPosition(), 0L, "Collar test", "minecraft:overworld");
        }
        throw new NoSuchMethodException("Known lantern request constructor");
    }

    private static void verifyCompass(MinecraftServer server, ServerPlayer player,
            EchoBindingSavedData.Binding binding, ItemStack summoner) throws ReflectiveOperationException {
        Class<?> tracker;
        try { tracker = Class.forName("com.github.yzqdev.pethome.server.misc.PetCompassTracker"); }
        catch (ClassNotFoundException originalDomestication) { return; }
        var living = findLoaded(server, binding.entityId()).livingEntity();
        tracker.getMethod("updateRecord", LivingEntity.class).invoke(null, living);
        var storage = Class.forName("com.github.yzqdev.pethome.util.CitadelEntityData");
        var data = (net.minecraft.nbt.CompoundTag)storage.getMethod("getCitadelTag", LivingEntity.class).invoke(null, living);
        UUID id = UUID.fromString(data.getStringOr("PetId", ""));
        var worldType = Class.forName("com.github.yzqdev.pethome.server.misc.PHWorldData");
        Object world = worldType.getMethod("get", net.minecraft.world.level.Level.class).invoke(null, player.level());
        Object record = worldType.getMethod("getPetRecord", UUID.class).invoke(world, id);
        check(record != null, "committed Echo has a real compass record");
        var teleporter = Class.forName("com.github.yzqdev.pethome.server.misc.PetCompassTeleport");
        var recall = teleporter.getDeclaredMethod("teleportPetToPlayer", ServerPlayer.class, record.getClass());
        var locate = teleporter.getDeclaredMethod("teleportPlayerToPet", ServerPlayer.class, record.getClass());
        recall.setAccessible(true); locate.setAccessible(true);
        UUID entityId = binding.entityId();
        int initialFuel = com.yuriscat.echowarrior.item.SummonerFuel.amount(binding.summonerState());
        var stranger = new ServerPlayer(server, player.level(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "CollarStranger"), net.minecraft.server.level.ClientInformation.createDefault());
        stranger.connection = new Sink(server, stranger);
        recall.invoke(null, stranger, record);
        check(entityId.equals(binding.entityId()) && com.yuriscat.echowarrior.item.SummonerFuel.amount(binding.summonerState()) == initialFuel, "foreign compass caller cannot change Echo authority");
        record.getClass().getField("entityUuid").set(record, UUID.randomUUID().toString());
        var position = player.position();
        locate.invoke(null, player, record);
        check(player.position().equals(position), "stale compass location cannot teleport its owner");
        recall.invoke(null, player, record);
        check(entityId.equals(binding.entityId()) && com.yuriscat.echowarrior.item.SummonerFuel.amount(binding.summonerState()) == initialFuel, "compass recall uses live binding without charging twice");
        check(dismiss(server, binding.summonerId()), "compass dismissal");
        fund(player, summoner, 0);
        recall.invoke(null, player, record);
        check(!binding.active() && com.yuriscat.echowarrior.item.SummonerFuel.amount(binding.summonerState()) == 0, "compass cannot bypass summon fuel");
        fund(player, summoner, 300);
        recall.invoke(null, player, record);
        check(binding.active() && com.yuriscat.echowarrior.item.SummonerFuel.amount(binding.summonerState()) < 300 && !entityId.equals(binding.entityId()), "compass uses native charged summon");
        living = findLoaded(server, binding.entityId()).livingEntity();
        tracker.getMethod("updateRecord", LivingEntity.class).invoke(null, living);
        data = (net.minecraft.nbt.CompoundTag)storage.getMethod("getCitadelTag", LivingEntity.class).invoke(null, living);
        check(id.toString().equals(data.getStringOr("PetId", "")), "compass identity survives entity recreation");
        org.slf4j.LoggerFactory.getLogger("echo_warrior").info("[PetCollarCompassSelfTest] PASS stable-entry permission fuel stale-location native-recall");
    }

    private static int collarDrops(LivingEntity entity, net.minecraft.world.item.Item item) {
        return entity.level().getEntitiesOfClass(ItemEntity.class, new AABB(30, 90, 30, 50, 105, 50),
                drop -> drop.getItem().is(item)).stream().mapToInt(drop -> drop.getItem().getCount()).sum();
    }

    private static void equip(ServerPlayer player, LivingEntity target, String enchantment, int level) {
        var item = BuiltInRegistries.ITEM.get(Identifier.fromNamespaceAndPath("pet_home", "collar_tag")).orElseThrow().value();
        check(item != net.minecraft.world.item.Items.AIR, "registered collar");
        int dropsBefore = collarDrops(target, item);
        boolean replacing;
        try { replacing = (Boolean)Class.forName("com.github.yzqdev.pethome.util.TameableUtils").getMethod("hasCollar", LivingEntity.class).invoke(null, target); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Actual collar state", error); }
        ItemStack collar = new ItemStack(item);
        if (enchantment != null) {
            var enchant = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(
                    net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,
                            Identifier.fromNamespaceAndPath("pet_home", enchantment)));
            collar.enchant(enchant, level);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, collar);
        interact(player, target);
        check(player.getMainHandItem().isEmpty(), "actual interaction consumes one collar");
        check(collarDrops(target, item) == dropsBefore + (replacing ? 1 : 0),
                "actual replacement returns exactly one old collar, first equip returns none");
    }

    private static void interact(ServerPlayer player, LivingEntity target) {
        try {
            Class<?> callback = Class.forName("net.fabricmc.fabric.api.event.player.UseEntityCallback");
            Object event = callback.getField("EVENT").get(null);
            Class<?> eventType = Class.forName("net.fabricmc.fabric.api.event.Event");
            Object invoker = eventType.getMethod("invoker").invoke(event);
            var method = java.util.Arrays.stream(callback.getMethods())
                    .filter(m -> m.getName().equals("interact") && m.getParameterCount() == 5).findFirst().orElseThrow();
            Object result = method.invoke(invoker, player, player.level(), InteractionHand.MAIN_HAND, target, null);
            if (result != net.minecraft.world.InteractionResult.PASS) return;
        } catch (ClassNotFoundException forge) {
            // Forge/NeoForge dispatch the event from Player.interactOn itself.
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Actual Fabric interaction callback failed", error);
        }
        player.interactOn(target, InteractionHand.MAIN_HAND, null);
    }

    private static void verifyRawPacketAuthority(MinecraftServer server, ServerPlayer player, LivingEntity target,
            String mod) throws ReflectiveOperationException {
        String storageName = mod.equals("pet_home") ? "com.github.yzqdev.pethome.util.CitadelEntityData"
                : "com.github.alexthe666.citadel.server.entity.CitadelEntityData";
        String packetName = mod.equals("pet_home") ? "com.github.yzqdev.pethome.network.PropertiesMessage"
                : "com.github.alexthe666.citadel.server.message.PropertiesMessage";
        Class<?> packet = Class.forName(packetName);
        Class<?> handler = mod.equals("pet_home") ? packet : Class.forName(packetName + "$Handler");
        var handle = java.util.Arrays.stream(handler.getDeclaredMethods()).filter(m ->
                (m.getName().equals("handleServer") || m.getName().equals("handle")) && m.getParameterCount() == 2)
                .findFirst().orElseThrow();
        handle.setAccessible(true);
        Object context = rawPacketContext(handle.getParameterTypes()[1], player, server);
        var getter = Class.forName(storageName).getMethod("getCitadelTag", LivingEntity.class);
        var before = ((net.minecraft.nbt.CompoundTag)getter.invoke(null, target)).copy();
        var forged = before.copy();
        forged.putInt("EchoCollarForgedPacket", 987654);
        String property = mod.equals("pet_home") ? "PetHomeTagUpdate" : "CitadelTagUpdate";
        var constructor = packet.getConstructor(String.class, net.minecraft.nbt.CompoundTag.class, int.class);
        handle.invoke(null, constructor.newInstance(property, forged, target.getId()), context);
        drainPacketTasks(server);
        check(before.equals(getter.invoke(null, target)), "owner raw packet cannot overwrite an Echo collar");
        var ordinary = net.minecraft.world.entity.EntityType.WOLF.create(player.level(), net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
        check(ordinary != null, "ordinary packet target");
        ordinary.setOwnerReference(net.minecraft.world.entity.EntityReference.of(player));
        ordinary.setTame(true, false);
        ordinary.snapTo(player.getX(), player.getY(), player.getZ(), 0, 0);
        player.level().addFreshEntity(ordinary);
        try {
            var data = ((net.minecraft.nbt.CompoundTag)getter.invoke(null, ordinary)).copy();
            data.putInt("EchoOrdinaryPacketProbe", 123);
            handle.invoke(null, constructor.newInstance(property, data, ordinary.getId()), context);
            drainPacketTasks(server);
            check(((net.minecraft.nbt.CompoundTag)getter.invoke(null, ordinary)).contains("EchoOrdinaryPacketProbe"),
                    "ordinary owned pet packet still reaches original handler");
        } finally { ordinary.discard(); }
        org.slf4j.LoggerFactory.getLogger("echo_warrior").info("[PetCollarPacketSelfTest] PASS echo=server-owned ordinary=passthrough");
    }

    private static void drainPacketTasks(MinecraftServer server) {
        // The real handler may enqueue a second task from inside our first server task.
        // A queued barrier exercises that work before assertions or fixture cleanup.
        var completed = new java.util.concurrent.atomic.AtomicBoolean();
        server.schedule(new net.minecraft.server.TickTask(server.getTickCount(), () -> completed.set(true)));
        server.managedBlock(completed::get);
    }

    private static Object rawPacketContext(Class<?> type, ServerPlayer player, MinecraftServer server)
            throws ReflectiveOperationException {
        if (type.isInstance(player)) return player;
        if (java.util.function.Supplier.class.isAssignableFrom(type)) {
            Connection wire = null;
            for (Class<?> parent = player.connection.getClass(); parent != null; parent = parent.getSuperclass()) {
                for (var field : parent.getDeclaredFields()) if (field.getType() == Connection.class) {
                    field.setAccessible(true); wire = (Connection)field.get(player.connection); break;
                }
                if (wire != null) break;
            }
            check(wire != null, "recording Forge connection");
            Class<?> direction = Class.forName("net.minecraftforge.network.NetworkDirection");
            @SuppressWarnings({"rawtypes", "unchecked"}) Object inbound = Enum.valueOf((Class)direction, "PLAY_TO_SERVER");
            var constructor = Class.forName("net.minecraftforge.network.NetworkEvent$Context")
                    .getDeclaredConstructor(Connection.class, direction, int.class);
            constructor.setAccessible(true);
            Object context = constructor.newInstance(wire, inbound, 0);
            return (java.util.function.Supplier<Object>)() -> context;
        }
        if (!type.isInterface()) throw new IllegalStateException("Unsupported packet context " + type);
        return java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "player" -> player;
                case "server" -> server;
                case "enqueueWork" -> { ((Runnable)args[0]).run(); yield java.util.concurrent.CompletableFuture.completedFuture(null); }
                case "toString" -> "Echo collar packet test context";
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });
    }

    private static boolean close(double a, double b) { return Math.abs(a - b) < 0.001; }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Pet collar joint test: " + message);
    }
    private static final class Sink extends ServerGamePacketListenerImpl {
        Sink(MinecraftServer server, ServerPlayer player) {
            super(server, new Connection(PacketFlow.SERVERBOUND), player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }
        @Override public void send(Packet<?> packet) {}
    }
}
