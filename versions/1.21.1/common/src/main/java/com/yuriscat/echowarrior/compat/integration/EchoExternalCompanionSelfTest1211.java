package com.yuriscat.echowarrior.compat.integration;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.item.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Tests the real existing transaction through the external entry point, without requiring TBF. */
public final class EchoExternalCompanionSelfTest1211 {
    public static void run(MinecraftServer server) {
        var level = server.overworld();
        ServerPlayer player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "EchoBridgeTest"), net.minecraft.server.level.ClientInformation.createDefault());
        Map<BlockPos, BlockState> previous = new LinkedHashMap<>();
        ItemStack summoner = new ItemStack(ModContent1211.ECHO_SUMMONER);
        var binding = EchoBindingSystem1211.synchronize(level, summoner);
        try {
            for (int x=34; x<=46; x++) for (int z=34; z<=46; z++) for (int y=95; y<=99; y++) {
                BlockPos pos = new BlockPos(x,y,z); previous.put(pos, level.getBlockState(pos));
                level.setBlock(pos, y==95 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            }
            player.moveTo(40.5,96,40.5,0,0);
            ItemStack relic = new ItemStack(ModContent1211.ROMAN_LEGIONARY_RELIC);
            EchoRelicState1211.ensureInitialized(relic, player.getRandom(), level.getGameTime());
            EchoRelicState1211.setTraitsForSelfTest(relic, 0, EchoBiomeAffinity1211.OPENLAND);
            EchoSummonerItem1211.setRelicStack(summoner, relic);
            EchoBindingSystem1211.updateRelic(level, summoner);
            binding.setFuel(300);
            var initial = EchoBindingSystem1211.summonNew(level, player, summoner);
            check(initial.succeeded(), "initial ordinary summon");
            UUID id = EchoExternalCompanion1211.entryId(binding);
            check(player.getInventory().isEmpty(), "external fixture carries no summoner");
            // Physical item stays in a real chest; the adapter receives only a stable entry ID.
            BlockPos chestPos = new BlockPos(46,96,46);
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
            var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chestPos);
            chest.setItem(0, summoner);
            int cost = SummonerFuel1211.summonCost(binding.relic());
            Set<UUID> incarnations = new HashSet<>();
            for (int i=0; i<100; i++) {
                check(EchoExternalCompanion1211.dismiss(player,id), "external dismissal " + i);
                check(!binding.active(), "dismissal deactivates authority");
                binding.setFuel(300);
                var result = EchoExternalCompanion1211.summon(player,id);
                check(result.succeeded() && binding.fuel()==300-cost, "single charged summon " + i + " " + result.failure());
                check(id.equals(EchoExternalCompanion1211.entryId(binding)) && incarnations.add(binding.spiritId()), "stable entry, fresh entity UUID");
                var live = EchoBindingSystem1211.findLoaded(server,binding.spiritId());
                live.livingEntity().setHealth(7);
                check(EchoExternalCompanion1211.summon(player,id).succeeded()
                        && binding.fuel()==300-cost && live.livingEntity().getHealth()==7, "recall preserves health and charges no fuel");
                check(EchoBindingSystem1211.countActive(server,player.getUUID(),null)==1, "one authoritative active Echo");
            }
            var old = EchoBindingSystem1211.findLoaded(server,binding.spiritId());
            old.livingEntity().setHealth(7);
            long generation = binding.generation();
            int recallFuel = binding.fuel();
            var destination = server.getLevel(net.minecraft.world.level.Level.NETHER);
            Map<BlockPos, BlockState> otherBlocks = new LinkedHashMap<>();
            try {
                for (int x=36;x<=44;x++) for (int z=36;z<=44;z++) for (int y=95;y<=99;y++) {
                    BlockPos pos = new BlockPos(x,y,z);
                    check(destination.getBlockEntity(pos)==null,"safe disposable cross-dimension fixture");
                    otherBlocks.put(pos,destination.getBlockState(pos));
                    destination.setBlock(pos,y==95 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),2);
                }
                ServerPlayer remote = new ServerPlayer(server,destination,player.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
                remote.moveTo(40.5,96,40.5,0,0);
                check(EchoExternalCompanion1211.summon(remote,id).succeeded(),"cross-dimension external recall without item");
                // Fresh non-ticking fixture chunks are not yet in ServerLevel.getEntity visibility.
                // The committed binding records the real spawn transaction before that next-tick promotion.
                check(binding.dimension().equals(destination.dimension().location().toString()) && binding.health()==7
                        && binding.generation()==generation+1 && binding.fuel()==recallFuel && old.livingEntity().isRemoved(),
                        "cross-dimension authority, health, fuel and old-entity removal");
                check(EchoExternalCompanion1211.summon(player,id).succeeded(),"return recall without item");
            } finally { otherBlocks.forEach((pos,state)->destination.setBlock(pos,state,2)); }
            var missing = EchoBindingSystem1211.findLoaded(server,binding.spiritId());
            EchoBindingSystem1211.track(missing);
            UUID missingUuid = missing.livingEntity().getUUID();
            missing.livingEntity().discard();
            check(binding.active() && EchoExternalCompanion1211.summon(player,id).succeeded(),"missing entity reconstructs from authoritative state");
            check(!missingUuid.equals(binding.spiritId()) && binding.fuel()==recallFuel
                    && EchoBindingSystem1211.findLoaded(server,binding.spiritId()).livingEntity().getHealth()==7,
                    "reconstruction changes incarnation, preserves fuel and life");
            TbfJointSelfTest1211.run(server,player,binding,id);
            ServerPlayer intruder = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "EchoBridgeTest"), net.minecraft.server.level.ClientInformation.createDefault());
            check(!EchoExternalCompanion1211.summon(intruder,id).succeeded(), "foreign controller denied");
            EchoExternalCompanion1211.dismiss(player,id);
            binding.setFuel(0);
            check(!EchoExternalCompanion1211.summon(player,id).succeeded() && binding.fuel()==0 && !binding.active(), "insufficient fuel");
            binding.setFuel(300);
            player.moveTo(40.5,level.getMinBuildHeight()-30,40.5,0,0);
            check(!EchoExternalCompanion1211.summon(player,id).succeeded() && binding.fuel()==300 && !binding.active(), "unsafe destination refunds or retains fuel");
            player.moveTo(40.5,96,40.5,0,0);
            EchoBindingSystem1211.synchronize(level, chest.getItem(0));
            check(binding.fuel()==300, "stale chest mirror cannot overwrite fuel");
            EchoSummonerItem1211.setRelicStack(summoner, ItemStack.EMPTY);
            EchoBindingSystem1211.updateRelic(level, summoner);
            check(!EchoExternalCompanion1211.summon(player,id).succeeded(), "removed relic revokes entry");
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info("[EchoExternalCompanionSelfTest] PASS 100 cycles, stable entry, chest, dimensions, reconstruction, permissions, fuel and revocation");
        } finally {
            EchoBindingSystem1211.destroySummoner(level, binding.summonerId());
            var blockEntity = level.getBlockEntity(new BlockPos(46,96,46));
            if (blockEntity instanceof net.minecraft.world.Container container) container.clearContent();
            previous.forEach((pos,state)->level.setBlock(pos,state,2));
        }
    }
    private static void check(boolean valid,String label) { if(!valid) throw new IllegalStateException("External companion: "+label); }
}
