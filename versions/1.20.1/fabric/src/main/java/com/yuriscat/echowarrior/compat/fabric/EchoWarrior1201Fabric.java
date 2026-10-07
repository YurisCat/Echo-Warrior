package com.yuriscat.echowarrior.compat.fabric;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class EchoWarrior1201Fabric implements ModInitializer {
    @Override
    public void onInitialize() {
        net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("trulybestfriends").ifPresent(mod ->
                com.yuriscat.echowarrior.compat.integration.TbfBridge1201.initialize(mod.getMetadata().getVersion().getFriendlyString()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(com.yuriscat.echowarrior.compat.integration.TbfBridge1201::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> com.yuriscat.echowarrior.compat.integration.TbfBridge1201.clear());
        ModContent1201.blocks().forEach((id, block) -> Registry.register(BuiltInRegistries.BLOCK, id, block));
        ModContent1201.RECYCLER_CHEST = net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(
                com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1201::new, ModContent1201.ECHO_RECYCLER).build();
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModContent1201.id("echo_recycler"), ModContent1201.RECYCLER_CHEST);
        ModContent1201.RECYCLER_MENU = net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry.registerSimple(
                ModContent1201.id("echo_recycler"), com.yuriscat.echowarrior.compat.menu.RecyclerMenu1201::new);
        com.yuriscat.echowarrior.compat.binding.EchoBindingConfig1201.load(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        ModContent1201.entities().forEach((id, type) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type));
        com.yuriscat.echowarrior.compat.ModEffects1201.effects().forEach((id, effect) -> Registry.register(BuiltInRegistries.MOB_EFFECT, id, effect));
        com.yuriscat.echowarrior.compat.ModEffects1201.bindHolders();
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(ModContent1201.ROMAN_LEGIONARY_ECHO,
                com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1201.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(ModContent1201.AZTEC_WARRIOR_ECHO,
                com.yuriscat.echowarrior.compat.entity.AztecWarriorEchoEntity1201.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(ModContent1201.GUANDAO_WARRIOR_ECHO,
                com.yuriscat.echowarrior.compat.entity.GuandaoWarriorEchoEntity1201.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(ModContent1201.JAPANESE_SAMURAI_ECHO,
                com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1201.createAttributes());
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(ModContent1201.EGYPTIAN_ARCHER_ECHO,
                com.yuriscat.echowarrior.compat.entity.EgyptianArcherEchoEntity1201.createAttributes());
        ModContent1201.items().forEach((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, new net.minecraft.resources.ResourceLocation("echo_warrior", "echo_warrior"),
                com.yuriscat.echowarrior.compat.ModCreativeTabs1201.echoes(net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()));
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, new net.minecraft.resources.ResourceLocation("echo_warrior", "echo_warrior_accessories"),
                com.yuriscat.echowarrior.compat.ModCreativeTabs1201.accessories(net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()));
        ModContent1201.SUMMONER_MENU = net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry.registerSimple(
                ModContent1201.MENU_ID, com.yuriscat.echowarrior.compat.menu.SummonerMenu1201::new);
        EchoWarrior1201.initialize("Fabric");
        ModContent1201.KNOWLEDGE_READER_MENU = net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry.registerSimple(
                ModContent1201.id("knowledge_reader"), com.yuriscat.echowarrior.compat.menu.KnowledgeReaderMenu1201::new);
        ModContent1201.TUTORIAL_MANUAL_MENU = net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry.registerSimple(
                ModContent1201.id("tutorial_manual"), com.yuriscat.echowarrior.compat.menu.TutorialManualMenu1201::new);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, ModContent1201.id("knowledge_fragment_collection"), ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION_SERIALIZER);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ModContent1201.id("echo_warrior_knowledge"),
                com.yuriscat.echowarrior.compat.ModCreativeTabs1201.knowledge(net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()));
        EchoNetworking1201Fabric.initialize();
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
                com.yuriscat.echowarrior.compat.command.GameplayTestCommands1201.register(dispatcher));
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, ModContent1201.id("craft_legacy_repair"), ModContent1201.CRAFT_LEGACY_REPAIR_SERIALIZER);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register(
                com.yuriscat.echowarrior.compat.world.BattlefieldSystem1201::noteChunk);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_UNLOAD.register(
                com.yuriscat.echowarrior.compat.world.BattlefieldSystem1201::forgetChunk);
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, entity) -> {
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel)
                com.yuriscat.echowarrior.compat.world.BattlefieldSystem1201.markPlayerModified(serverLevel, pos);
        });
        net.fabricmc.fabric.api.loot.v2.LootTableEvents.MODIFY.register((manager, lootManager, id, builder, source) ->
                com.yuriscat.echowarrior.compat.knowledge.KnowledgeLootSystem1201.modify(id, builder::withPool));
        net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper.registerVillagerOffers(
                net.minecraft.world.entity.npc.VillagerProfession.CARTOGRAPHER, 3,
                offers -> offers.add((entity, random) -> com.yuriscat.echowarrior.compat.world.CompassTrade1201.create()));
    }
}
