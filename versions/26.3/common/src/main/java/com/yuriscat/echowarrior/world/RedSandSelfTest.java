package com.yuriscat.echowarrior.world;

import com.yuriscat.echowarrior.ModBlocks;
import com.yuriscat.echowarrior.EchoWarrior;
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
public final class RedSandSelfTest {
    private static final net.minecraft.world.level.ChunkPos CHUNK = new net.minecraft.world.level.ChunkPos(2, 2);
    private static final net.minecraft.server.level.TicketType TEST_TICKET =
            new net.minecraft.server.level.TicketType(200L,
                    net.minecraft.server.level.TicketType.FLAG_LOADING | net.minecraft.server.level.TicketType.FLAG_SIMULATION);
    private static long preparationStarted = -1L;
    private RedSandSelfTest() {}

    /** Do not mistake a loaded block chunk for a visible entity section when counting actual drops. */
    public static boolean prepare(ServerLevel level) {
        if (preparationStarted < 0L) {
            preparationStarted = level.getGameTime();
            level.getChunkSource().addTicketWithRadius(TEST_TICKET, CHUNK, 2);
        }
        if (level.areEntitiesActuallyLoadedAndTicking(CHUNK)
                && level.isPositionEntityTicking(new BlockPos(34, 128, 34))) return true;
        if (level.getGameTime() - preparationStarted > 180L) {
            release(level);
            throw new IllegalStateException("Red sand fixture entity chunk never became ready");
        }
        return false;
    }

    private static void release(ServerLevel level) {
        level.getChunkSource().removeTicketWithRadius(TEST_TICKET, CHUNK, 2);
        preparationStarted = -1L;
    }
    public static void run(ServerLevel level) {
        BlockPos pos = new BlockPos(34, 128, 34);
        BlockState original = level.getBlockState(pos);
        BlockState support = level.getBlockState(pos.below());
        require(level.getBlockEntity(pos) == null && level.getBlockEntity(pos.below()) == null, "empty fixture");
        AABB bounds = new AABB(pos).inflate(2);
        var previous = new HashSet<UUID>();
        level.getEntitiesOfClass(ItemEntity.class, bounds).forEach(item -> previous.add(item.getUUID()));
        try {
            var selector = BattlefieldSystem.class.getDeclaredMethod("suspiciousFor", BlockState.class);
            selector.setAccessible(true);
            require(selector.invoke(null, Blocks.RED_SAND.defaultBlockState()) == ModBlocks.SUSPICIOUS_RED_SAND, "red terrain selected before sand tag");
            require(selector.invoke(null, Blocks.SAND.defaultBlockState()) == Blocks.SUSPICIOUS_SAND, "yellow sand unchanged");
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(pos, ModBlocks.SUSPICIOUS_RED_SAND.defaultBlockState(), 3);
            var brushable = (BrushableBlockEntity)level.getBlockEntity(pos);
            require(brushable != null && brushable.getType().isValid(brushable.getBlockState()), "registered vanilla brushable entity");
            brushable.setLootTable(BattlefieldCulture.ROMAN.guaranteedLoot(), 42);
            var player = new net.minecraft.world.entity.monster.zombie.Zombie(level);
            for (int hit = 0; hit < 10; hit++) {
                boolean done = brushable.brush(level.getGameTime() + hit * 10L, level, player, Direction.UP, new ItemStack(Items.BRUSH));
                require(done == (hit == 9), "only the tenth stroke completes");
                if (hit < 9) require(level.getBlockState(pos).is(ModBlocks.SUSPICIOUS_RED_SAND), "intermediate stage remains brushable");
            }
            require(level.getBlockState(pos).is(Blocks.RED_SAND) && level.getBlockEntity(pos) == null, "completion restores red sand");
            var drops = level.getEntitiesOfClass(ItemEntity.class, bounds, item -> !previous.contains(item.getUUID()));
            require(drops.size() == 1 && drops.get(0).getItem().getCount() == 1, "one actual reward");
            EchoWarrior.LOGGER.info("RED SAND SELFTEST PASSED generation=red brush=10 loot=1 restore=red-sand");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot exercise red sand generation", error);
        } finally {
            level.getEntitiesOfClass(ItemEntity.class, bounds, item -> !previous.contains(item.getUUID())).forEach(ItemEntity::discard);
            level.setBlock(pos, original, 3);
            level.setBlock(pos.below(), support, 3);
            release(level);
        }
    }
    private static void require(boolean value, String detail) {
        if (!value) throw new IllegalStateException("Red sand: " + detail);
    }
}
