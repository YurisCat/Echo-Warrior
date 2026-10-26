package com.yuriscat.echowarrior.compat.integration;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.world.BattlefieldCulture1211;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootTable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/** Battlefield rewards are shared once; ordinary Lootr containers retain their own rules. */
public final class LootrArchaeology1211 {
    private static final Set<ResourceLocation> TABLES = tables();
    // Bytecode-only optional Mixin validation reads this mapped descriptor without loading game classes.
    private static ResourceKey<LootTable> apiTableType;
    public static final String API = "noobanidus.mods.lootr.common.api.LootrAPI";
    public static final String BRUSHABLE = "noobanidus.mods.lootr.common.block.entity.LootrBrushableBlockEntity";
    private static boolean warned;

    private LootrArchaeology1211() {}

    private static Set<ResourceLocation> tables() {
        Set<ResourceLocation> result = new HashSet<>();
        result.add(EchoWarrior1211.id("archaeology/battlefield_common"));
        result.add(EchoWarrior1211.id("archaeology/battlefield_guaranteed_relic"));
        for (BattlefieldCulture1211 culture : BattlefieldCulture1211.values()) {
            result.add(culture.commonLoot().location());
            result.add(culture.guaranteedLoot().location());
        }
        return Set.copyOf(result);
    }

    public static boolean isSharedTable(ResourceKey<LootTable> table) {
        return table != null && TABLES.contains(table.location());
    }

    /**
     * Only called for positions already present in our persistent index, in an already loaded chunk.
     * Actual openers are recorded on completion, unlike hasBeenOpened which is set on the first stroke.
     * Do not read/generate a player's inventory and do not emit loot while repairing an old save.
     */
    public static void restoreTrackedBlock(ServerLevel level, LevelChunk chunk, BlockPos pos) {
        BlockEntity entity = chunk.getBlockEntity(pos);
        if (entity == null || !entity.getClass().getName().equals(BRUSHABLE)
                || !LootrMixinPlugin1211.compatible()) return;
        try {
            Class<?> type = entity.getClass();
            Method tableGetter = type.getMethod("getInfoLootTable");
            Method seedGetter = type.getMethod("getInfoLootSeed");
            Method openersGetter = type.getMethod("getActualOpeners");
            if (tableGetter.getReturnType() != ResourceKey.class || seedGetter.getReturnType() != long.class
                    || openersGetter.getReturnType() != Set.class) {
                throw new IllegalStateException("Lootr archaeology getter signature changed");
            }
            Object value = tableGetter.invoke(entity);
            if (!(value instanceof ResourceKey<?> key) || !TABLES.contains(key.location())) return;
            @SuppressWarnings("unchecked") ResourceKey<LootTable> table = (ResourceKey<LootTable>)key;
            long seed = (long)seedGetter.invoke(entity);
            Object openers = openersGetter.invoke(entity);
            if (openers != null && !(openers instanceof Set<?>)) {
                throw new IllegalStateException("Lootr returned invalid completion metadata");
            }
            boolean completed = openers instanceof Set<?> set && !set.isEmpty();
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock());
            Block target;
            if (id.equals(ResourceLocation.fromNamespaceAndPath("lootr", "suspicious_sand"))) {
                target = completed ? Blocks.SAND : Blocks.SUSPICIOUS_SAND;
            } else if (id.equals(ResourceLocation.fromNamespaceAndPath("lootr", "suspicious_gravel"))) {
                target = completed ? Blocks.GRAVEL : Blocks.SUSPICIOUS_GRAVEL;
            } else {
                return;
            }
            // Both pending states remain brushable, preserving the index through the replacement.
            // The existing removal callback completes the index when restoring an already spent block.
            if (!level.setBlock(pos, target.defaultBlockState(), Block.UPDATE_ALL)) return;
            if (!completed && chunk.getBlockEntity(pos) instanceof BrushableBlockEntity restored) {
                restored.setLootTable(table, seed);
                restored.setChanged();
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            if (!warned) {
                warned = true;
                EchoWarrior1211.LOGGER.warn("Could not restore existing Lootr battlefield archaeology; retained its data.", error);
            }
        }
    }
}
