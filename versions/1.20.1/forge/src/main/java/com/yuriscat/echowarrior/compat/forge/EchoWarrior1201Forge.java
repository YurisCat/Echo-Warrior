package com.yuriscat.echowarrior.compat.forge;

import com.yuriscat.echowarrior.compat.EchoWarrior1201;
import com.yuriscat.echowarrior.compat.ModContent1201;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

@Mod(EchoWarrior1201.MOD_ID)
public final class EchoWarrior1201Forge {
    public EchoWarrior1201Forge() {
        net.minecraftforge.fml.ModList.get().getModContainerById("trulybestfriends").ifPresent(mod ->
                com.yuriscat.echowarrior.compat.integration.TbfBridge1201.initialize(mod.getModInfo().getVersion().toString()));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ServerTickEvent event) -> { if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) com.yuriscat.echowarrior.compat.integration.TbfBridge1201.tick(event.getServer()); });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent event) -> com.yuriscat.echowarrior.compat.integration.TbfBridge1201.clear());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::register);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::attributes);
        com.yuriscat.echowarrior.compat.binding.EchoBindingConfig1201.load(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
        EchoWarrior1201.initialize("Forge");
        EchoNetworking1201Forge.initialize();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(this::chunkLoad);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(this::chunkUnload);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(this::blockBreak);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(this::lootLoad);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(this::trades);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(this::commands);
    }

    private void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent1201.blocks().forEach(helper::register));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            ModContent1201.RECYCLER_CHEST = net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                    com.yuriscat.echowarrior.compat.block.entity.RecyclerChestBlockEntity1201::new, ModContent1201.ECHO_RECYCLER).build(null);
            helper.register(ModContent1201.id("echo_recycler"), ModContent1201.RECYCLER_CHEST);
        });
        event.register(Registries.RECIPE_SERIALIZER, helper -> helper.register(ModContent1201.id("knowledge_fragment_collection"), ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION_SERIALIZER));
        event.register(Registries.RECIPE_SERIALIZER, helper -> helper.register(ModContent1201.id("craft_legacy_repair"), ModContent1201.CRAFT_LEGACY_REPAIR_SERIALIZER));
        event.register(Registries.ENTITY_TYPE, helper -> ModContent1201.entities().forEach(helper::register));
        event.register(Registries.MOB_EFFECT, helper -> {
            com.yuriscat.echowarrior.compat.ModEffects1201.effects().forEach(helper::register);
            com.yuriscat.echowarrior.compat.ModEffects1201.bindHolders();
        });
        event.register(Registries.ITEM, helper -> ModContent1201.items().forEach(helper::register));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> {
            helper.register(ModContent1201.id("echo_warrior_knowledge"), com.yuriscat.echowarrior.compat.ModCreativeTabs1201.knowledge(net.minecraft.world.item.CreativeModeTab.builder()));
            helper.register(new net.minecraft.resources.ResourceLocation("echo_warrior", "echo_warrior"),
                    com.yuriscat.echowarrior.compat.ModCreativeTabs1201.echoes(net.minecraft.world.item.CreativeModeTab.builder()));
            helper.register(new net.minecraft.resources.ResourceLocation("echo_warrior", "echo_warrior_accessories"),
                    com.yuriscat.echowarrior.compat.ModCreativeTabs1201.accessories(net.minecraft.world.item.CreativeModeTab.builder()));
        });
        event.register(Registries.MENU, helper -> {
            ModContent1201.RECYCLER_MENU = net.minecraftforge.common.extensions.IForgeMenuType.create(
                    (id, inventory, buffer) -> new com.yuriscat.echowarrior.compat.menu.RecyclerMenu1201(id, inventory));
            helper.register(ModContent1201.id("echo_recycler"), ModContent1201.RECYCLER_MENU);
            ModContent1201.SUMMONER_MENU = net.minecraftforge.common.extensions.IForgeMenuType.create(
                    (id, inventory, buffer) -> new com.yuriscat.echowarrior.compat.menu.SummonerMenu1201(id, inventory));
            helper.register(ModContent1201.MENU_ID, ModContent1201.SUMMONER_MENU);
            ModContent1201.KNOWLEDGE_READER_MENU = net.minecraftforge.common.extensions.IForgeMenuType.create(
                    (id, inventory, buffer) -> new com.yuriscat.echowarrior.compat.menu.KnowledgeReaderMenu1201(id, inventory));
            ModContent1201.TUTORIAL_MANUAL_MENU = net.minecraftforge.common.extensions.IForgeMenuType.create(
                    (id, inventory, buffer) -> new com.yuriscat.echowarrior.compat.menu.TutorialManualMenu1201(id, inventory));
            helper.register(ModContent1201.id("knowledge_reader"), ModContent1201.KNOWLEDGE_READER_MENU);
            helper.register(ModContent1201.id("tutorial_manual"), ModContent1201.TUTORIAL_MANUAL_MENU);
        });
    }
    private void attributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ModContent1201.ROMAN_LEGIONARY_ECHO, com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1201.createAttributes().build());
        event.put(ModContent1201.AZTEC_WARRIOR_ECHO, com.yuriscat.echowarrior.compat.entity.AztecWarriorEchoEntity1201.createAttributes().build());
        event.put(ModContent1201.GUANDAO_WARRIOR_ECHO, com.yuriscat.echowarrior.compat.entity.GuandaoWarriorEchoEntity1201.createAttributes().build());
        event.put(ModContent1201.JAPANESE_SAMURAI_ECHO, com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1201.createAttributes().build());
        event.put(ModContent1201.EGYPTIAN_ARCHER_ECHO, com.yuriscat.echowarrior.compat.entity.EgyptianArcherEchoEntity1201.createAttributes().build());
    }
    private void chunkLoad(net.minecraftforge.event.level.ChunkEvent.Load event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
                && event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)
            com.yuriscat.echowarrior.compat.world.BattlefieldSystem1201.noteChunk(level, chunk, event.isNewChunk());
    }
    private void chunkUnload(net.minecraftforge.event.level.ChunkEvent.Unload event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level
                && event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk)
            com.yuriscat.echowarrior.compat.world.BattlefieldSystem1201.forgetChunk(level, chunk);
    }
    private void blockBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
            com.yuriscat.echowarrior.compat.world.BattlefieldSystem1201.markPlayerModified(level, event.getPos());
    }
    private void lootLoad(net.minecraftforge.event.LootTableLoadEvent event) {
        com.yuriscat.echowarrior.compat.knowledge.KnowledgeLootSystem1201.modify(event.getName(),
                pool -> event.getTable().addPool(pool.build()));
    }
    private void trades(net.minecraftforge.event.village.VillagerTradesEvent event) {
        if (event.getType() == net.minecraft.world.entity.npc.VillagerProfession.CARTOGRAPHER)
            event.getTrades().get(3).add((entity, random) -> com.yuriscat.echowarrior.compat.world.CompassTrade1201.create());
    }
    private void commands(net.minecraftforge.event.RegisterCommandsEvent event) {
        com.yuriscat.echowarrior.compat.command.GameplayTestCommands1201.register(event.getDispatcher());
    }
}
