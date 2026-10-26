package com.yuriscat.echowarrior.compat.world;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.integration.LootrArchaeology1211;
import com.yuriscat.echowarrior.compat.integration.LootrMixinPlugin1211;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Runs against the actual optional release JAR, including its persistent per-player inventory. */
public final class LootrArchaeologySelfTest1211 {
    private LootrArchaeologySelfTest1211() {}
    public static void run(ServerLevel level) {
        Class<?> api;
        try {
            api = Class.forName(LootrArchaeology1211.API);
        } catch (ClassNotFoundException absent) {
            EchoWarrior1211.LOGGER.info("LOOTR ARCHAEOLOGY SELFTEST SKIPPED absent");
            return;
        }
        require(LootrMixinPlugin1211.compatible(), "installed Lootr must enable the adapter");
        var originalData = BattlefieldSavedData1211.get(level);
        var data = new BattlefieldSavedData1211();
        BlockPos pos = new BlockPos(8, 140, 8);
        var original = level.getBlockState(pos);
        var support = level.getBlockState(pos.below());
        require(level.getBlockEntity(pos) == null && level.getBlockEntity(pos.below()) == null, "empty fixture");
        var bounds = new AABB(pos).inflate(2);
        Set<UUID> previousDrops = new HashSet<>();
        var outgoing = new java.util.ArrayList<net.minecraft.network.protocol.Packet<?>>();
        level.getEntitiesOfClass(ItemEntity.class, bounds).forEach(item -> previousDrops.add(item.getUUID()));
        try {
            var blacklist = api.getMethod("isLootTableBlacklisted", ResourceKey.class);
            for (var culture : BattlefieldCulture1211.values()) {
                require((boolean)blacklist.invoke(null, culture.commonLoot()), "ordinary shared pool excluded");
                require((boolean)blacklist.invoke(null, culture.guaranteedLoot()), "guaranteed shared pool excluded");
            }
            for (String legacy : List.of("battlefield_common", "battlefield_guaranteed_relic")) {
                require((boolean)blacklist.invoke(null, table("echo_warrior", "archaeology/" + legacy)), "legacy pool excluded");
            }
            require(!(boolean)blacklist.invoke(null, table("minecraft", "archaeology/desert_pyramid")),
                    "vanilla archaeology still uses Lootr");
            require(!(boolean)blacklist.invoke(null, table("echo_warrior", "recycler/common")),
                    "unrelated own loot tables not excluded");
            level.getDataStorage().set("echo_warrior_battlefield_sites", data);
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            ServerPlayer player = new ServerPlayer(level.getServer(), level,
                    new GameProfile(UUID.randomUUID(), "LootrBrushTest"),
                    net.minecraft.server.level.ClientInformation.createDefault());
            player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                    new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                // This is a server state test with no negotiated client channel. Record the packets at
                // the listener boundary, before NeoForge's real-network channel validation.
                @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { outgoing.add(packet); }
            };
            player.setPos(8, 141, 9);
            var region = data.prepareForcedRegion(level, new ChunkPos(pos));
            for (String terrain : List.of("sand", "gravel")) {
                var foreign = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("lootr", "suspicious_" + terrain));
                require(foreign != Blocks.AIR, "actual Lootr archaeology registered");
                for (int strokes : new int[]{0, 1, 10}) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    level.setBlock(pos, foreign.defaultBlockState(), 3);
                    var entity = level.getBlockEntity(pos);
                    require(entity != null && entity.getClass().getName().equals(LootrArchaeology1211.BRUSHABLE), "actual Lootr entity");
                    entity.getClass().getMethod("setLootTableInternal", ResourceKey.class, long.class)
                            .invoke(entity, BattlefieldCulture1211.ROMAN.guaranteedLoot(), 42L);
                    data.activate(region.key(), pos, pos, "roman", List.of(pos));
                    var brush = entity.getClass().getMethod("IBrushable$brush", long.class, Player.class, Direction.class);
                    for (int hit = 0; hit < strokes; hit++) {
                        boolean complete = (boolean)brush.invoke(entity, level.getGameTime() + hit * 10L, player, Direction.UP);
                        require(complete == (hit == 9), "actual Lootr brushing completion");
                    }
                    // Reproduce the report with the real third-party completion method.
                    require(level.getBlockState(pos).is(foreign) && data.findActiveByCenter(pos.asLong()) != null,
                            "Lootr retained block leaves old index active");
                    var persisted = entity.saveWithFullMetadata(level.registryAccess());
                    var restoredEntity = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                            pos, entity.getBlockState(), persisted, level.registryAccess());
                    require(restoredEntity != null, "actual Lootr NBT reload");
                    restoredEntity.setLevel(level);
                    level.setBlockEntity(restoredEntity);
                    LootrArchaeology1211.restoreTrackedBlock(level, level.getChunkAt(pos), pos);
                    if (strokes < 10) {
                        require(level.getBlockState(pos).is(terrain.equals("sand") ? Blocks.SUSPICIOUS_SAND : Blocks.SUSPICIOUS_GRAVEL),
                                "uncompleted old block restored as brushable");
                        require(data.findActiveByCenter(pos.asLong()) != null, "first stroke is not completion");
                        var vanilla = (BrushableBlockEntity)level.getBlockEntity(pos);
                        var saved = vanilla.saveWithFullMetadata(level.registryAccess());
                        require(saved.getString("LootTable").equals(BattlefieldCulture1211.ROMAN.guaranteedLoot().location().toString())
                                        && saved.getLong("LootTableSeed") == 42L,
                                "pending table and seed survive conversion");
                        for (int hit = 0; hit < 10; hit++) vanilla.brush(level.getGameTime() + hit * 10L, player, Direction.UP);
                    }
                    require(level.getBlockState(pos).is(terrain.equals("sand") ? Blocks.SAND : Blocks.GRAVEL),
                            "spent block restores original terrain");
                    require(data.findActiveByCenter(pos.asLong()) == null, "shared target released");
                    int jobs = data.replacementJobCount();
                    LootrArchaeology1211.restoreTrackedBlock(level, level.getChunkAt(pos), pos);
                    require(data.replacementJobCount() == jobs, "repair idempotent");
                    var drops = level.getEntitiesOfClass(ItemEntity.class, bounds, item -> !previousDrops.contains(item.getUUID()));
                    require(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1, "exactly one reward; repair creates none");
                    drops.forEach(ItemEntity::discard);
                }
            }
            require(outgoing.stream().anyMatch(packet ->
                            packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket payload
                                    && payload.payload().type().id().getNamespace().equals("lootr")),
                    "actual Lootr completion emitted its client notification");
            EchoWarrior1211.LOGGER.info("LOOTR ARCHAEOLOGY SELFTEST PASSED pools=12 ordinary=unchanged sand=0-1-10 gravel=0-1-10 rewards=once");
        } catch (ReflectiveOperationException error) {
            EchoWarrior1211.LOGGER.error("Actual Lootr archaeology regression failed", error);
            throw new IllegalStateException("Actual Lootr archaeology regression failed: " + error.getCause(), error);
        } finally {
            level.getEntitiesOfClass(ItemEntity.class, bounds, item -> !previousDrops.contains(item.getUUID())).forEach(ItemEntity::discard);
            level.setBlock(pos, original, 3);
            level.setBlock(pos.below(), support, 3);
            level.getDataStorage().set("echo_warrior_battlefield_sites", originalData);
        }
    }
    private static ResourceKey<LootTable> table(String namespace, String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
    private static void require(boolean value, String detail) {
        if (!value) throw new IllegalStateException("Lootr archaeology: " + detail);
    }
}
