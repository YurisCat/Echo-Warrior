package com.yuriscat.echowarrior.compat.world;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.HashSet;
import java.util.UUID;

/** Actual generation selection, registered block entity and ten-stroke loot completion. */
public final class RedSandSelfTest1201 {
    private RedSandSelfTest1201() {}
    public static void run(ServerLevel level) {
        BlockPos pos = new BlockPos(34, 128, 34);
        BlockState original = level.getBlockState(pos);
        BlockState support = level.getBlockState(pos.below());
        require(level.getBlockEntity(pos) == null && level.getBlockEntity(pos.below()) == null, "empty fixture");
        AABB bounds = new AABB(pos).inflate(2);
        var previous = new HashSet<UUID>();
        level.getEntitiesOfClass(ItemEntity.class, bounds).forEach(item -> previous.add(item.getUUID()));
        try {
            var selector = BattlefieldSystem1201.class.getDeclaredMethod("suspiciousFor", BlockState.class);
            selector.setAccessible(true);
            require(selector.invoke(null, Blocks.RED_SAND.defaultBlockState()) == ModContent1201.SUSPICIOUS_RED_SAND, "red terrain selected before sand tag");
            require(selector.invoke(null, Blocks.SAND.defaultBlockState()) == Blocks.SUSPICIOUS_SAND, "yellow sand unchanged");
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(pos, ModContent1201.SUSPICIOUS_RED_SAND.defaultBlockState(), 3);
            var brushable = (BrushableBlockEntity)level.getBlockEntity(pos);
            require(brushable != null && brushable.getType().isValid(brushable.getBlockState()), "registered vanilla brushable entity");
            brushable.setLootTable(BattlefieldCulture1201.ROMAN.guaranteedLoot(), 42);
            var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level,
                    new com.mojang.authlib.GameProfile(UUID.randomUUID(), "RedSandTest"));
            player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                    new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player);
            player.setPos(34, 129, 35);
            for (int hit = 0; hit < 10; hit++) {
                boolean done = brushable.brush(level.getGameTime() + hit * 10L, player, Direction.UP);
                require(done == (hit == 9), "only the tenth stroke completes");
                if (hit < 9) require(level.getBlockState(pos).is(ModContent1201.SUSPICIOUS_RED_SAND), "intermediate stage remains brushable");
            }
            require(level.getBlockState(pos).is(Blocks.RED_SAND) && level.getBlockEntity(pos) == null, "completion restores red sand");
            var drops = level.getEntitiesOfClass(ItemEntity.class, bounds, item -> !previous.contains(item.getUUID()));
            require(drops.size() == 1 && drops.get(0).getItem().getCount() == 1, "one actual reward");
            EchoWarrior1201.LOGGER.info("RED SAND SELFTEST PASSED generation=red brush=10 loot=1 restore=red-sand");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot exercise red sand generation", error);
        } finally {
            level.getEntitiesOfClass(ItemEntity.class, bounds, item -> !previous.contains(item.getUUID())).forEach(ItemEntity::discard);
            level.setBlock(pos, original, 3);
            level.setBlock(pos.below(), support, 3);
        }
    }
    private static void require(boolean value, String detail) {
        if (!value) throw new IllegalStateException("Red sand: " + detail);
    }
}
