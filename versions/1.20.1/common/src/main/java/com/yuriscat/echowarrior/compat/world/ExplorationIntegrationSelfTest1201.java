package com.yuriscat.echowarrior.compat.world;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.item.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;

/** Package-local production entry points, isolated flat-world fixture; no simulated placement algorithm. */
public final class ExplorationIntegrationSelfTest1201 {
    private ExplorationIntegrationSelfTest1201() {}
    public static void run(MinecraftServer server) {
        var level = server.overworld();
        for (int x = 12; x <= 14; x++) for (int z = 12; z <= 14; z++) level.getChunk(x, z);
        var center = new BlockPos(216, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 216, 216) - 1, 216);
        var saved = new LinkedHashMap<BlockPos, BlockState>();
        var clean = new BattlefieldSavedData1201();
        for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) for (int y = 0; y <= 4; y++) {
            var pos = center.offset(x, y, z);
            check(level.getBlockEntity(pos) == null, "fixture has no existing block entities");
            saved.put(pos, level.getBlockState(pos));
        }
        try {
            check(BattlefieldSystem1201.isSafeSite(level, clean, center, 8), "natural flat site eligible");
            var modified = new BattlefieldSavedData1201(); modified.markPlayerModified(new ChunkPos(center));
            check(!BattlefieldSystem1201.isSafeSite(level, modified, center, 8), "player-edited chunk rejected");
            level.setBlock(center.above(), Blocks.CHEST.defaultBlockState(), 3);
            check(!BattlefieldSystem1201.isSafeSite(level, clean, center, 8), "block entity rejected");
            level.setBlock(center.above(), saved.get(center.above()), 3);
            level.setBlock(center.above(), Blocks.WATER.defaultBlockState(), 3);
            check(!BattlefieldSystem1201.isSafeSite(level, clean, center, 8), "fluid rejected");
            level.setBlock(center.above(), saved.get(center.above()), 3);
            level.setBlock(center.above(3), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            check(!BattlefieldSystem1201.isSafeSite(level, clean, center, 8), "height gap rejected");
            level.setBlock(center.above(3), saved.get(center.above(3)), 3);
            level.setBlock(center.above(), Blocks.OAK_LOG.defaultBlockState(), 3);
            check(!BattlefieldSystem1201.isSafeSite(level, clean, center, 8), "tree rejected without cutting it");
            level.setBlock(center.above(), saved.get(center.above()), 3);
            check(!BattlefieldSystem1201.isSafeSite(level, clean, center, 64), "unloaded surrounding chunks rejected");
            var placement = BattlefieldSystem1201.placeSite(level, center, 8, RandomSource.create(0x201201L));
            check(placement != null && placement.brushables().size() >= 6 && placement.brushables().size() <= 9,
                    "actual placement has six to nine brushables");
            int guaranteed = 0;
            for (var pos : placement.brushables()) {
                check(level.getBlockEntity(pos) instanceof BrushableBlockEntity, "registered brushable created");
                var nbt = level.getBlockEntity(pos).saveWithFullMetadata();
                if (nbt.getString("LootTable").equals(placement.culture().guaranteedLoot().toString())) guaranteed++;
                else check(nbt.getString("LootTable").equals(placement.culture().commonLoot().toString()), "culture-specific common loot");
            }
            check(guaranteed == 1, "one and only one guaranteed relic");
            // Exercise real placement/neighbor updates under all snow-layer thicknesses, not a copy of the algorithm.
            for (int layers = 1; layers <= 8; layers++) {
                saved.forEach((pos, originalState) -> level.setBlock(pos, originalState, 3));
                BlockState snow = Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, layers);
                for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
                    level.setBlock(center.offset(x, 1, z), snow, 3);
                }
                check(BattlefieldSystem1201.isSafeSite(level, clean, center, 8), "snow-covered site remains eligible: " + layers);
                var snowyPlacement = BattlefieldSystem1201.placeSite(level, center, 8, RandomSource.create(0x201201L + layers));
                check(snowyPlacement != null && snowyPlacement.brushables().size() >= 6, "snowy site still generates archaeology");
                for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
                    check(level.getBlockState(center.offset(x, 1, z)).equals(snow), "snow cover/layers preserved: " + layers);
                }
                for (var pos : snowyPlacement.brushables()) {
                    check(pos.getY() == center.getY() && level.getBlockEntity(pos) instanceof BrushableBlockEntity,
                            "archaeology replaces ground below snow, not snow itself");
                }
            }
            saved.forEach((pos, originalState) -> level.setBlock(pos, originalState, 3));
            level.setBlock(center.above(), Blocks.SNOW_BLOCK.defaultBlockState(), 3);
            check(!BattlefieldSystem1201.isSafeSite(level, clean, center, 8), "full snow block is terrain, never cleared");
            check(level.getBlockState(center.above()).is(Blocks.SNOW_BLOCK), "solid snow retained");
            EchoWarrior1201.LOGGER.info("[Compat1201] BATTLEFIELD SNOW SELFTEST PASSED layers=1-8 placement=actual cover=preserved solid-snow=untouched");
        } finally { saved.forEach((pos, state) -> level.setBlock(pos, state, 3)); }

        var player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "CompassInstanceTest"));
        var original = new ItemStack(ModContent1201.ECHO_COMPASS);
        player.getInventory().setItem(0, original);
        var state = new EchoCompassSystem1201.RenderState(EchoCompassItem1201.MODE_OUTSIDE, center.asLong());
        EchoCompassSystem1201.syncRenderState(player, state); CompoundTag originalTag = original.getTag();
        var newlyCarried = new ItemStack(ModContent1201.ECHO_COMPASS);
        var cursor = new ItemStack(ModContent1201.ECHO_COMPASS);
        var offhand = new ItemStack(ModContent1201.ECHO_COMPASS);
        player.getInventory().setItem(3, newlyCarried); player.getInventory().setItem(40, offhand);
        player.containerMenu.setCarried(cursor);
        var nested = new ItemStack(ModContent1201.ECHO_COMPASS);
        var shulker = new ItemStack(Items.SHULKER_BOX);
        var contents = NonNullList.withSize(27, ItemStack.EMPTY); contents.set(0, nested);
        var container = new CompoundTag(); net.minecraft.world.ContainerHelper.saveAllItems(container, contents);
        shulker.getOrCreateTag().put("BlockEntityTag", container); player.getInventory().setItem(4, shulker);
        var before = shulker.save(new CompoundTag());
        EchoCompassSystem1201.syncRenderState(player, state);
        for (var compass : List.of(original, newlyCarried, cursor, offhand)) {
            check(EchoCompassItem1201.trackingMode(compass) == state.mode()
                    && EchoCompassItem1201.trackingTarget(compass) == state.target(), "same target reaches newly carried instances");
        }
        check(original.getTag() == originalTag && before.equals(shulker.save(new CompoundTag())),
                "unchanged held state and nested compass untouched");
        EchoCompassSystem1201.clear();
        EchoWarrior1201.LOGGER.info("[Compat1201] EXPLORATION INTEGRATION SELFTEST PASSED placement=actual safety=edited-fluid-tree-slope-unloaded compass=new-instances");
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException("Exploration integration: " + message);
    }
}
