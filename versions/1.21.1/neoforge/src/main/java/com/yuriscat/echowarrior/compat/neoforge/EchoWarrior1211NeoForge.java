package com.yuriscat.echowarrior.compat.neoforge;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.ModCreativeTabs1211;
import com.yuriscat.echowarrior.compat.ModEffects1211;
import com.yuriscat.echowarrior.compat.combat.ShieldChargeCreeperControl1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingConfig1211;
import com.yuriscat.echowarrior.compat.binding.CreativeSummonerDestroyTracker1211;
import com.yuriscat.echowarrior.compat.entity.CatGodCreeperSystem1211;
import com.yuriscat.echowarrior.compat.entity.EchoCombatEvents1211;
import com.yuriscat.echowarrior.compat.entity.EchoAuraAuditSystem1211;
import com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.AztecWarriorEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.GuandaoWarriorEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1211;
import com.yuriscat.echowarrior.compat.entity.EgyptianArcherEchoEntity1211;
import com.yuriscat.echowarrior.compat.command.CompatSelfTestCommand1211;
import com.yuriscat.echowarrior.compat.command.GameplayTestCommands1211;
import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1211;
import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1211;
import com.yuriscat.echowarrior.compat.progress.EchoExperienceSystem1211;
import com.yuriscat.echowarrior.compat.network.CreativeSummonerDestructionPayload1211;
import com.yuriscat.echowarrior.compat.network.CreativeSummonerInsertionPayload1211;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.server.level.ServerLevel;

@Mod(EchoWarrior1211.MOD_ID)
public final class EchoWarrior1211NeoForge {
    public EchoWarrior1211NeoForge(IEventBus modBus) {
        EchoBindingConfig1211.load(FMLPaths.CONFIGDIR.get());
        modBus.addListener(this::registerContent);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::addBrushableBlocks);
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::onLevelTick);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onDamageApplied);
        NeoForge.EVENT_BUS.addListener(this::onDeath);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(this::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(this::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(this::onLootTableLoad);
        if (FMLEnvironment.dist == Dist.CLIENT) EchoWarrior1211NeoForgeClient.register(modBus, NeoForge.EVENT_BUS);
        EchoWarrior1211.initialize();
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(
                CreativeSummonerInsertionPayload1211.TYPE,
                CreativeSummonerInsertionPayload1211.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        CreativeSummonerInsertionPayload1211.handle(player, payload);
                    }
                });
        registrar.playToServer(
                CreativeSummonerDestructionPayload1211.TYPE,
                CreativeSummonerDestructionPayload1211.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer player) {
                        CreativeSummonerDestructionPayload1211.handle(player, payload);
                    }
                });
    }

    private void registerContent(RegisterEvent event) {
        event.register(Registries.ITEM, helper ->
                ModContent1211.items().forEach(helper::register));
        event.register(Registries.BLOCK, helper ->
                ModContent1211.blocks().forEach(helper::register));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper ->
                ModContent1211.blockEntityTypes().forEach(helper::register));
        event.register(Registries.CREATIVE_MODE_TAB, helper ->
                ModCreativeTabs1211.tabs().forEach(helper::register));
        event.register(Registries.ENTITY_TYPE, helper ->
                ModContent1211.entityTypes().forEach(helper::register));
        event.register(Registries.MOB_EFFECT, helper -> {
            ModEffects1211.effects().forEach(helper::register);
            ModEffects1211.bindHolders();
        });
        event.register(Registries.MENU, helper -> {
            helper.register(ModContent1211.id(ModContent1211.SUMMONER_MENU_ID), ModContent1211.SUMMONER_MENU);
            helper.register(ModContent1211.id(ModContent1211.RECYCLER_MENU_ID), ModContent1211.RECYCLER_MENU);
            helper.register(ModContent1211.id(ModContent1211.KNOWLEDGE_READER_MENU_ID), ModContent1211.KNOWLEDGE_READER_MENU);
            helper.register(ModContent1211.id(ModContent1211.TUTORIAL_MANUAL_MENU_ID), ModContent1211.TUTORIAL_MANUAL_MENU);
        });
        event.register(Registries.RECIPE_SERIALIZER, helper -> {
            helper.register(ModContent1211.id(ModContent1211.CRAFT_LEGACY_REPAIR_RECIPE_ID),
                    ModContent1211.CRAFT_LEGACY_REPAIR_SERIALIZER);
            helper.register(ModContent1211.id(ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION_RECIPE_ID),
                    ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION_SERIALIZER);
        });
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModContent1211.ROMAN_LEGIONARY_ECHO, RomanLegionaryEchoEntity1211.createAttributes().build());
        event.put(ModContent1211.AZTEC_WARRIOR_ECHO, AztecWarriorEchoEntity1211.createAttributes().build());
        event.put(ModContent1211.GUANDAO_WARRIOR_ECHO, GuandaoWarriorEchoEntity1211.createAttributes().build());
        event.put(ModContent1211.JAPANESE_SAMURAI_ECHO, JapaneseSamuraiEchoEntity1211.createAttributes().build());
        event.put(ModContent1211.EGYPTIAN_ARCHER_ECHO, EgyptianArcherEchoEntity1211.createAttributes().build());
    }

    private void addBrushableBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(net.minecraft.world.level.block.entity.BlockEntityType.BRUSHABLE_BLOCK,
                ModContent1211.SUSPICIOUS_GRASS_BLOCK, ModContent1211.SUSPICIOUS_DIRT);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        CompatSelfTestCommand1211.register(event.getDispatcher());
        GameplayTestCommands1211.register(event.getDispatcher());
    }

    private void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ShieldChargeCreeperControl1211.tick(level);
            CatGodCreeperSystem1211.tickPanickingCreepers(level);
            EchoAccessorySystem1211.tickLevel(level);
            EchoTalentSystem1211.tickLevel(level);
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        com.yuriscat.echowarrior.compat.recycler.RecyclerSystem1211.tick(event.getServer());
        com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.tick(event.getServer());
        com.yuriscat.echowarrior.compat.world.EchoCompassSystem1211.tick(event.getServer());
        CreativeSummonerDestroyTracker1211.tick(event.getServer());
        EchoBindingSystem1211.tick(event.getServer());
        EchoAuraAuditSystem1211.tick(event.getServer());
    }

    private void onDamageApplied(LivingDamageEvent.Post event) {
        boolean blocked = event.getBlockedDamage() > 0.0F;
        EchoExperienceSystem1211.afterDamage(event.getEntity(), event.getSource(), event.getNewDamage(), blocked);
        EchoTalentSystem1211.afterDamage(event.getEntity(), event.getSource(), event.getNewDamage(), blocked);
        EchoAccessorySystem1211.afterDamage(event.getEntity(), event.getSource(), event.getNewDamage(), blocked);
        EchoCombatEvents1211.afterDamage(event.getEntity(), event.getSource(), event.getNewDamage(), blocked);
    }

    private void onDeath(LivingDeathEvent event) {
        EchoExperienceSystem1211.afterDeath(event.getEntity(), event.getSource());
        EchoTalentSystem1211.afterDeath(event.getEntity(), event.getSource());
        if (event.getEntity() instanceof com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211 echo) {
            EchoBindingSystem1211.deactivate(echo);
        }
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            EchoBindingSystem1211.onPlayerJoin(player);
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            CreativeSummonerDestroyTracker1211.clearPlayer(player.getUUID());
        }
    }

    private void onServerStopped(ServerStoppedEvent event) {
        CreativeSummonerDestroyTracker1211.clearAll();
        EchoBindingSystem1211.onServerStopped();
        com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.clear();
        com.yuriscat.echowarrior.compat.world.EchoCompassSystem1211.clear();
    }

    private void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk) {
            com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.noteChunk(level, chunk, event.isNewChunk());
        }
    }

    private void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk) {
            com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.forgetChunk(level, chunk);
        }
    }

    private void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.markPlayerModified(level, event.getPos());
        }
    }

    private void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) {
            com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.markPlayerModified(level, event.getPos());
        }
    }

    private void onLootTableLoad(LootTableLoadEvent event) {
        com.yuriscat.echowarrior.compat.knowledge.KnowledgeLootSystem1211.modify(
                event.getKey(), pool -> event.getTable().addPool(pool.build()));
    }
}
