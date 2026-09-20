package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1201;
import com.yuriscat.echowarrior.compat.knowledge.*;
import com.yuriscat.echowarrior.compat.recycler.RecyclerClockData1201;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Explosion;
import java.util.*;

/** Actual block entity transactions with real data-pack loot; dedicated isolated test world only. */
public final class RecyclerSelfTest1201 {
    private static int checks;
    private RecyclerSelfTest1201() {}
    public static void run(MinecraftServer server) {
        checks = 0;
        var level = server.overworld();
        BlockPos pos = new BlockPos(30, 128, 30);
        var previous = level.getBlockState(pos);
        if (level.getBlockEntity(pos) != null) throw new IllegalStateException("Recycler fixture refuses existing block entity");
        try {
            check(level.setBlock(pos, ModContent1201.ECHO_RECYCLER.defaultBlockState(), 3), "place recycler");
            var recycler = (RecyclerChestBlockEntity1201)level.getBlockEntity(pos);
            check(recycler != null && recycler.getType() == ModContent1201.RECYCLER_CHEST, "registered block entity");
            String id = KnowledgeCatalog1201.entries().get(0).id();
            var fragments = KnowledgeStackData1201.fragment(id); fragments.setCount(32);
            recycler.setItem(0, fragments);
            check(fragments.is(ModTags1201.RECYCLER_KNOWLEDGE), "plural old item tags loaded");
            check(recycler.triggerManual(level) && recycler.isSealed(), "start real loot plan");
            check(!recycler.triggerManual(level), "no duplicate transaction");
            check(recycler.getItem(0).getCount() == 32, "physical input retained until commit");
            check(!recycler.canPlaceItem(0, fragments) && !recycler.canTakeItem(new SimpleContainer(1), 0, fragments)
                    && recycler.removeItem(0, 1).isEmpty(), "sealed insertion/extraction blocked");
            var player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "RecyclerSelfTest"));
            check(!player.gameMode.destroyBlock(pos) && level.getBlockState(pos).is(ModContent1201.ECHO_RECYCLER), "sealed player destruction rejected");
            var explosion = new Explosion(level, null, pos.getX(), pos.getY(), pos.getZ(), 0, false, Explosion.BlockInteraction.DESTROY);
            explosion.getToBlow().add(pos);
            explosion.finalizeExplosion(false);
            check(level.getBlockState(pos).is(ModContent1201.ECHO_RECYCLER) && explosion.getToBlow().isEmpty(), "sealed explosion list protected");
            for (int tick = 0; tick < 10; tick++) RecyclerChestBlockEntity1201.serverTick(level, pos, recycler.getBlockState(), recycler);
            CompoundTag pending = recycler.saveWithoutMetadata();
            check(pending.getBoolean("RecyclerPending") && pending.getInt("RecyclerSealTicks") == 30, "pending NBT captures remaining seal");
            var restored = new RecyclerChestBlockEntity1201(pos, recycler.getBlockState());
            restored.load(pending.copy()); restored.setLevel(level); level.setBlockEntity(restored);
            check(restored.isSealed() && restored.getItem(0).getCount() == 32, "pending transaction restored");
            for (int tick = 0; tick < 30; tick++) RecyclerChestBlockEntity1201.serverTick(level, pos, restored.getBlockState(), restored);
            check(!restored.isSealed(), "commit after remaining ticks");
            int rewards = 0;
            for (int slot = 0; slot < 27; slot++) {
                ItemStack stack = restored.getItem(slot);
                check(!stack.is(ModContent1201.KNOWLEDGE_FRAGMENT), "input removed on commit " + slot);
                rewards += stack.getCount();
            }
            check(rewards == 32, "one common reward per knowledge unit");
            check(!restored.triggerManual(level), "rewards do not recursively recycle");
            restored.clearContent();
            // 26 occupied slots and a two-page book: neither freed space nor reward-compatible stack exists.
            for (int slot = 1; slot < 27; slot++) restored.setItem(slot, new ItemStack(Items.DIAMOND, 64));
            restored.setItem(0, KnowledgeStackData1201.collection(Map.of(id, 2), id));
            check(restored.triggerManual(level), "space-blocked plan seals for failure feedback");
            for (int tick = 0; tick < 40; tick++) RecyclerChestBlockEntity1201.serverTick(level, pos, restored.getBlockState(), restored);
            check(KnowledgeStackData1201.totalCount(restored.getItem(0)) == 2 && restored.getItem(1).getCount() == 64,
                    "no space consumes nothing");
            check(restored.saveWithoutMetadata().getBoolean("RecyclerSpaceNotice"), "space failure feedback persisted");
            var clock = new RecyclerClockData1201();
            clock.observe(17999); check(clock.midnightSequence() == 0, "initial clock");
            clock.observe(18000); check(clock.midnightSequence() == 1, "midnight advances once");
            clock.observe(18001); clock.observe(0); clock.observe(18000);
            check(clock.midnightSequence() == 1, "rewind cannot replay midnight");
            clock.observe(18000 + 24000 * 10); check(clock.midnightSequence() == 2, "time skip coalesces to one transaction");
            check(clock.save(new CompoundTag()).getLong("MidnightSequence") == 2, "clock saved");
            EchoWarrior1201.LOGGER.info("[Compat1201] RECYCLER SELFTEST PASSED checks={} transaction=nbt-restored loot=actual sealed=protected", checks);
        } finally {
            if (level.getBlockEntity(pos) instanceof RecyclerChestBlockEntity1201 recycler) {
                // Remove fixture contents before restoring terrain, including a failed pending test.
                var empty = recycler.saveWithoutMetadata(); empty.putBoolean("RecyclerPending", false); empty.remove("Items");
                recycler.load(empty);
                recycler.clearContent();
            }
            level.setBlock(pos, previous, 3);
        }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Recycler selftest: " + message);
        checks++;
    }
}
