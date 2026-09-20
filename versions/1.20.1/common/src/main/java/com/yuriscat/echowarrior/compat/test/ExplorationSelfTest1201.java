package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.item.EchoCompassItem1201;
import com.yuriscat.echowarrior.compat.knowledge.*;
import com.yuriscat.echowarrior.compat.world.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Runs only in explicitly opted-in isolated dedicated worlds. Loot is rolled by vanilla, not JSON simulated. */
public final class ExplorationSelfTest1201 {
    private static int checks;
    private ExplorationSelfTest1201() {}
    public static void run(MinecraftServer server) {
        checks = 0;
        var level = server.overworld();
        check(ModContent1201.items().size() == 43 && ModContent1201.blocks().size() == 3, "full content counts");
        check(server.getRecipeManager().getRecipes().stream().filter(r -> r.getId().getNamespace().equals("echo_warrior")).count() == 31,
                "31 parsed recipes, including both custom serializers");
        var recipe = server.getRecipeManager().byKey(ModContent1201.id("craft_legacy_repair")).orElseThrow();
        var craftingMenu = new RecipeMenu();
        var grid = new TransientCraftingContainer(craftingMenu, 2, 2);
        var tool = new ItemStack(Items.IRON_SWORD); tool.setDamageValue(100);
        tool.getOrCreateTag().putString("Foreign", "preserved");
        grid.setItem(0, tool); grid.setItem(1, new ItemStack(ModContent1201.CRAFT_LEGACY));
        var repair = (com.yuriscat.echowarrior.compat.recipe.CraftLegacyRepairRecipe1201)recipe;
        check(repair.matches(grid, level), "repair match");
        var repaired = repair.assemble(grid, level.registryAccess());
        check(repaired.getDamageValue() == 50 && repaired.getTag().getString("Foreign").equals("preserved")
                && tool.getDamageValue() == 100, "20 percent repair, metadata and source preserved");
        grid.setItem(2, new ItemStack(Items.STICK));
        check(!repair.matches(grid, level), "extra repair ingredient rejected");
        var trade = CompassTrade1201.create();
        check(trade.getBaseCostA().is(Items.EMERALD) && trade.getBaseCostA().getCount() == 4
                && trade.getCostB().is(Items.COMPASS) && trade.getResult().is(ModContent1201.ECHO_COMPASS)
                && trade.getMaxUses() == 2, "cartographer trade");
        var lootParams = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
        for (var culture : BattlefieldCulture1201.values()) {
            var table = server.getLootData().getLootTable(ModContent1201.id("gameplay/knowledge_fragment/" + culture.id()));
            Set<String> seen = new HashSet<>();
            var seeds = net.minecraft.util.RandomSource.create(0xEC401201L + culture.ordinal());
            for (int roll = 0; roll < 256; roll++) {
                // Adjacent java.util.Random seeds have correlated first draws; use a fixed seed stream.
                var output = table.getRandomItems(lootParams, seeds.nextLong());
                check(output.size() == 1 && KnowledgeStackData1201.fragmentId(output.get(0)).isPresent(), "real set_nbt knowledge loot " + culture);
                seen.add(KnowledgeStackData1201.fragmentId(output.get(0)).orElseThrow());
            }
            check(seen.size() == 8, "eight knowledge entries per culture " + culture + ": " + seen);
            check(server.getLootData().getLootTable(culture.commonLoot()) != LootTable.EMPTY, "common archaeology table " + culture);
            brush(level, server, culture, culture.ordinal() % 2 == 0 ? ModContent1201.SUSPICIOUS_GRASS_BLOCK : ModContent1201.SUSPICIOUS_DIRT);
        }
        check(server.getLootData().getLootTable(ModContent1201.id("gameplay/knowledge_fragment/random"))
                .getRandomItems(lootParams, 42).size() == 1, "old nested table name");
        var data = new BattlefieldSavedData1201();
        var proto = new net.minecraft.world.level.chunk.ProtoChunk(new ChunkPos(1000, 1000),
                net.minecraft.world.level.chunk.UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME), null);
        var generatedChunk = new net.minecraft.world.level.chunk.LevelChunk(level, proto, null);
        var readChunk = new net.minecraft.world.level.chunk.LevelChunk(level, new ChunkPos(1001, 1000));
        check(((GeneratedChunk1201)generatedChunk).echoWarrior$wasJustGenerated()
                && !((GeneratedChunk1201)readChunk).echoWarrior$wasJustGenerated(), "new chunk conversion distinguished from disk load");
        var newRegion = data.noteNaturalChunkLoad(level, generatedChunk.getPos(), true);
        var oldRegion = data.noteNaturalChunkLoad(level, new ChunkPos(2000, 2000), false);
        check(newRegion.readyAt() - level.getGameTime() < 2400
                && oldRegion.readyAt() - level.getGameTime() >= 2400, "new/old region initialization delay");
        var chunk = new ChunkPos(-50, 2);
        var state = data.noteNaturalChunkLoad(level, chunk, true);
        check(state.regionX() == -2, "negative region floor division");
        BlockPos center = new BlockPos(-800, 65, 32), relic = center.offset(1, 0, 0), extra = center.offset(2, 0, 0);
        data.activate(state.key(), center, relic, "roman", List.of(relic, extra));
        check(data.nearestActive(center, 2048).relic().equals(relic), "indexed nearest site");
        check(data.nearestActive(center.offset(3000, 0, 0), 2048) == null, "search radius");
        check(!data.isFarEnoughFromKnownSites(center.offset(100, 0, 0), 512), "minimum spacing");
        var removal = data.removeBrushableAt(relic, 1000);
        check(removal.relicCompleted() && removal.remaining().equals(List.of(extra)), "guaranteed completion and salvage");
        check(data.region(state.key()).readyAt() == 25000 && data.replacementJobCount() == 1, "cooldown and immediate replacement");
        UUID tracker = UUID.randomUUID(); data.setSalvageTracker(tracker, center); data.markPlayerModified(chunk);
        var restored = BattlefieldSavedData1201.load(data.save(new CompoundTag()));
        check(restored.salvageCenter(tracker).orElseThrow() == center.asLong() && restored.isPlayerModified(chunk)
                && restored.replacementJobCount() == 1, "salvage, edited chunks and replacement NBT");
        restored.removeBrushableAt(extra, 1100);
        check(restored.salvageCenter(tracker).isEmpty() && restored.salvageSites().isEmpty(), "last salvage clears tracker");
        ItemStack compass = new ItemStack(ModContent1201.ECHO_COMPASS);
        check(EchoCompassItem1201.isOutsideSoundEnabled(compass) && EchoCompassItem1201.trackingMode(compass) == 0
                && !compass.hasTag(), "compass reads cannot mutate NBT");
        compass.getOrCreateTag().putString("Foreign", "preserved");
        EchoCompassItem1201.writeTracking(compass, EchoCompassItem1201.MODE_INNER, center.asLong());
        CompoundTag tag = compass.getTag();
        EchoCompassItem1201.writeTracking(compass, EchoCompassItem1201.MODE_INNER, center.asLong());
        check(compass.getTag() == tag && tag.getString("Foreign").equals("preserved"), "unchanged compass state does not replace held NBT");
        var loadedCompass = ItemStack.of(compass.save(new CompoundTag()));
        check(EchoCompassItem1201.trackingTarget(loadedCompass) == center.asLong(), "compass NBT round trip");
        EchoWarrior1201.LOGGER.info("[Compat1201] EXPLORATION SELFTEST PASSED checks={} recipes=31 cultures=5 brushing=actual", checks);
    }
    private static void brush(net.minecraft.server.level.ServerLevel level, MinecraftServer server, BattlefieldCulture1201 culture, Block block) {
        BlockPos pos = new BlockPos(34, 128, 34);
        var previous = level.getBlockState(pos);
        if (level.getBlockEntity(pos) != null) throw new IllegalStateException("Refusing existing archaeology fixture block entity");
        var bounds = new AABB(pos).inflate(2);
        Set<UUID> existing = new HashSet<>();
        for (var item : level.getEntitiesOfClass(ItemEntity.class, bounds)) existing.add(item.getUUID());
        try {
            level.setBlock(pos, block.defaultBlockState(), 3);
            var brushable = (BrushableBlockEntity)level.getBlockEntity(pos);
            check(brushable != null && brushable.getType().isValid(block.defaultBlockState()), "vanilla brushable type accepts custom block");
            brushable.setLootTable(culture.guaranteedLoot(), 42);
            CompoundTag saved = brushable.saveWithFullMetadata();
            var reloaded = (BrushableBlockEntity)BlockEntity.loadStatic(pos, block.defaultBlockState(), saved);
            check(reloaded != null, "brushable NBT preserves vanilla registered type");
            reloaded.setLevel(level); level.setBlockEntity(reloaded);
            var player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "ArchaeologyTest"));
            player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(server,
                    new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player);
            player.setPos(34, 129, 35);
            for (int hit = 0; hit < 10; hit++) reloaded.brush(level.getGameTime() + hit * 10L, player, Direction.UP);
            check(level.getBlockState(pos).is(block == ModContent1201.SUSPICIOUS_GRASS_BLOCK ? Blocks.GRASS_BLOCK : Blocks.DIRT), "brush restores original terrain");
            var drops = level.getEntitiesOfClass(ItemEntity.class, bounds, item -> !existing.contains(item.getUUID()));
            String[] hero = {"roman_legionary", "aztec_warrior", "egyptian_archer", "guandao_warrior", "japanese_samurai"};
            check(drops.size() == 1 && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(drops.get(0).getItem().getItem())
                    .equals(ModContent1201.id(hero[culture.ordinal()] + "_relic")), "exact culture relic produced by vanilla brushing");
        } finally {
            for (var item : level.getEntitiesOfClass(ItemEntity.class, bounds, item -> !existing.contains(item.getUUID()))) item.discard();
            level.setBlock(pos, previous, 3);
        }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Exploration selftest: " + message);
        checks++;
    }
    private static final class RecipeMenu extends AbstractContainerMenu {
        private RecipeMenu() { super(null, 0); }
        @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p, int index) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player p) { return true; }
    }
}
