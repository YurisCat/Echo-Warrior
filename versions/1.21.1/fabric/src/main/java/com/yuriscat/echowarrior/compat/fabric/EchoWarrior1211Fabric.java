package com.yuriscat.echowarrior.compat.fabric;

import com.yuriscat.echowarrior.compat.EchoWarrior1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.ModCreativeTabs1211;
import com.yuriscat.echowarrior.compat.ModEffects1211;
import com.yuriscat.echowarrior.compat.combat.ShieldChargeCreeperControl1211;
import com.yuriscat.echowarrior.compat.command.CompatSelfTestCommand1211;
import com.yuriscat.echowarrior.compat.command.GameplayTestCommands1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingConfig1211;
import com.yuriscat.echowarrior.compat.binding.CreativeSummonerDestroyTracker1211;
import com.yuriscat.echowarrior.compat.entity.CatGodCreeperSystem1211;
import com.yuriscat.echowarrior.compat.entity.EchoCombatEvents1211;
import com.yuriscat.echowarrior.compat.entity.EchoAuraAuditSystem1211;
import com.yuriscat.echowarrior.compat.item.EchoAccessorySystem1211;
import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1211;
import com.yuriscat.echowarrior.compat.progress.EchoExperienceSystem1211;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.yuriscat.echowarrior.compat.network.CreativeSummonerDestructionPayload1211;
import com.yuriscat.echowarrior.compat.network.CreativeSummonerInsertionPayload1211;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;

public final class EchoWarrior1211Fabric implements ModInitializer {
    @Override
    public void onInitialize() {
        EchoBindingConfig1211.load(FabricLoader.getInstance().getConfigDir());
        PayloadTypeRegistry.playC2S().register(
                CreativeSummonerInsertionPayload1211.TYPE,
                CreativeSummonerInsertionPayload1211.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(
                CreativeSummonerDestructionPayload1211.TYPE,
                CreativeSummonerDestructionPayload1211.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(
                CreativeSummonerInsertionPayload1211.TYPE,
                (payload, context) -> CreativeSummonerInsertionPayload1211.handle(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(
                CreativeSummonerDestructionPayload1211.TYPE,
                (payload, context) -> CreativeSummonerDestructionPayload1211.handle(context.player(), payload));
        ModContent1211.items().forEach((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        ModContent1211.blocks().forEach((id, block) -> Registry.register(BuiltInRegistries.BLOCK, id, block));
        ModContent1211.blockEntityTypes().forEach((id, type) -> Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type));
        ModCreativeTabs1211.tabs().forEach((id, tab) -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, tab));
        ModContent1211.entityTypes().forEach((id, type) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type));
        ModEffects1211.effects().forEach((id, effect) -> Registry.register(BuiltInRegistries.MOB_EFFECT, id, effect));
        ModEffects1211.bindHolders();
        Registry.register(BuiltInRegistries.MENU, ModContent1211.id(ModContent1211.SUMMONER_MENU_ID),
                ModContent1211.SUMMONER_MENU);
        Registry.register(BuiltInRegistries.MENU, ModContent1211.id(ModContent1211.RECYCLER_MENU_ID),
                ModContent1211.RECYCLER_MENU);
        Registry.register(BuiltInRegistries.MENU, ModContent1211.id(ModContent1211.KNOWLEDGE_READER_MENU_ID),
                ModContent1211.KNOWLEDGE_READER_MENU);
        Registry.register(BuiltInRegistries.MENU, ModContent1211.id(ModContent1211.TUTORIAL_MANUAL_MENU_ID),
                ModContent1211.TUTORIAL_MANUAL_MENU);
        ((FabricBlockEntityType)BlockEntityType.BRUSHABLE_BLOCK).addSupportedBlock(ModContent1211.SUSPICIOUS_GRASS_BLOCK);
        ((FabricBlockEntityType)BlockEntityType.BRUSHABLE_BLOCK).addSupportedBlock(ModContent1211.SUSPICIOUS_DIRT);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ModContent1211.id(ModContent1211.CRAFT_LEGACY_REPAIR_RECIPE_ID),
                ModContent1211.CRAFT_LEGACY_REPAIR_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                ModContent1211.id(ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION_RECIPE_ID),
                ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION_SERIALIZER);
        FabricDefaultAttributeRegistry.register(ModContent1211.ROMAN_LEGIONARY_ECHO,
                com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1211.createAttributes());
        FabricDefaultAttributeRegistry.register(ModContent1211.AZTEC_WARRIOR_ECHO,
                com.yuriscat.echowarrior.compat.entity.AztecWarriorEchoEntity1211.createAttributes());
        FabricDefaultAttributeRegistry.register(ModContent1211.GUANDAO_WARRIOR_ECHO,
                com.yuriscat.echowarrior.compat.entity.GuandaoWarriorEchoEntity1211.createAttributes());
        FabricDefaultAttributeRegistry.register(ModContent1211.JAPANESE_SAMURAI_ECHO,
                com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1211.createAttributes());
        FabricDefaultAttributeRegistry.register(ModContent1211.EGYPTIAN_ARCHER_ECHO,
                com.yuriscat.echowarrior.compat.entity.EgyptianArcherEchoEntity1211.createAttributes());
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
            CompatSelfTestCommand1211.register(dispatcher);
            GameplayTestCommands1211.register(dispatcher);
        });
        ServerTickEvents.END_WORLD_TICK.register(level -> {
            ShieldChargeCreeperControl1211.tick(level);
            CatGodCreeperSystem1211.tickPanickingCreepers(level);
            EchoAccessorySystem1211.tickLevel(level);
            EchoTalentSystem1211.tickLevel(level);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            com.yuriscat.echowarrior.compat.recycler.RecyclerSystem1211.tick(server);
            com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.tick(server);
            com.yuriscat.echowarrior.compat.world.EchoCompassSystem1211.tick(server);
            CreativeSummonerDestroyTracker1211.tick(server);
            EchoBindingSystem1211.tick(server);
            EchoAuraAuditSystem1211.tick(server);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                EchoBindingSystem1211.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CreativeSummonerDestroyTracker1211.clearPlayer(handler.player.getUUID()));
        ServerChunkEvents.CHUNK_GENERATE.register((level, chunk) ->
                com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.noteChunk(level, chunk, true));
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk) ->
                com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.noteChunk(level, chunk));
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) ->
                com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.forgetChunk(level, chunk));
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, entity) -> {
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.markPlayerModified(serverLevel, pos);
            }
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide && player.getItemInHand(hand).getItem() instanceof BlockItem
                    && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.markPlayerModified(serverLevel, hit.getBlockPos());
            }
            return InteractionResult.PASS;
        });
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) ->
                com.yuriscat.echowarrior.compat.knowledge.KnowledgeLootSystem1211.modify(key, tableBuilder::withPool));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CreativeSummonerDestroyTracker1211.clearAll();
            EchoBindingSystem1211.onServerStopped();
            com.yuriscat.echowarrior.compat.world.BattlefieldSystem1211.clear();
            com.yuriscat.echowarrior.compat.world.EchoCompassSystem1211.clear();
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamageTaken, damageTaken, blocked) -> {
            EchoExperienceSystem1211.afterDamage(victim, source, damageTaken, blocked);
            EchoTalentSystem1211.afterDamage(victim, source, damageTaken, blocked);
            EchoAccessorySystem1211.afterDamage(victim, source, damageTaken, blocked);
            EchoCombatEvents1211.afterDamage(victim, source, damageTaken, blocked);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            EchoExperienceSystem1211.afterDeath(victim, source);
            EchoTalentSystem1211.afterDeath(victim, source);
            if (victim instanceof com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211 echo) {
                EchoBindingSystem1211.deactivate(echo);
            }
        });
        EchoWarrior1211.initialize();
    }
}
