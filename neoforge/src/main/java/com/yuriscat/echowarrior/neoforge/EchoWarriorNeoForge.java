package com.yuriscat.echowarrior.neoforge;

import com.mojang.serialization.Codec;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModBlockEntities;
import com.yuriscat.echowarrior.ModBlocks;
import com.yuriscat.echowarrior.ModCreativeTabs;
import com.yuriscat.echowarrior.ModEffects;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.ModMenus;
import com.yuriscat.echowarrior.ModRecipes;
import com.yuriscat.echowarrior.binding.CreativeSummonerDestroyTracker;
import com.yuriscat.echowarrior.block.entity.RecyclerChestBlockEntity;
import com.yuriscat.echowarrior.menu.KnowledgeReaderMenu;
import com.yuriscat.echowarrior.menu.RecyclerMenu;
import com.yuriscat.echowarrior.menu.SummonerMenu;
import com.yuriscat.echowarrior.menu.TutorialManualMenu;
import com.yuriscat.echowarrior.network.CreativeSummonerDestroyPayload;
import com.yuriscat.echowarrior.network.EchoCompassMessagePayload;
import com.yuriscat.echowarrior.network.EchoCompassPulsePayload;
import com.yuriscat.echowarrior.network.EchoCompassStatePayload;
import com.yuriscat.echowarrior.platform.PlatformServices;
import com.yuriscat.echowarrior.platform.RegistryRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(EchoWarrior.MOD_ID)
public final class EchoWarriorNeoForge {
	static final AttachmentType<Boolean> PLAYER_MODIFIED_CHUNK = AttachmentType.builder(() -> false)
			.serialize(Codec.BOOL.fieldOf("value"))
			.build();

	public EchoWarriorNeoForge(IEventBus modBus) {
		PlatformServices.install(new NeoForgePlatformServices());
		modBus.addListener(this::registerContent);
		modBus.addListener(this::registerAttributes);
		modBus.addListener(this::registerPayloads);
		modBus.addListener(this::addBrushableBlocks);
		modBus.addListener(this::commonSetup);
		NeoForgeEventRegistrar.register(NeoForge.EVENT_BUS);
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			EchoWarriorNeoForgeClient.register(modBus, NeoForge.EVENT_BUS);
		}
	}

	private void registerContent(RegisterEvent event) {
		event.register(Registries.MOB_EFFECT, helper -> ModEffects.register(registrar(helper)));
		event.register(Registries.ENTITY_TYPE, helper -> ModEntities.register(registrar(helper)));
		event.register(Registries.BLOCK, helper -> ModBlocks.registerBlocks(registrar(helper)));
		event.register(Registries.BLOCK_ENTITY_TYPE, helper -> ModBlockEntities.register(
				registrar(helper),
				new BlockEntityType<RecyclerChestBlockEntity>(RecyclerChestBlockEntity::new, ModBlocks.ECHO_RECYCLER)
		));
		event.register(Registries.ITEM, helper -> {
			ModBlocks.registerItems(registrar(helper));
			ModItems.register(registrar(helper));
		});
		event.register(Registries.RECIPE_SERIALIZER, helper -> ModRecipes.register(registrar(helper)));
		event.register(Registries.MENU, helper -> ModMenus.register(
				registrar(helper),
				IMenuTypeExtension.create((containerId, inventory, data) ->
						new SummonerMenu(containerId, inventory, data.readVarInt())),
				IMenuTypeExtension.create((containerId, inventory, data) ->
						new KnowledgeReaderMenu(containerId, inventory, data.readVarInt())),
				IMenuTypeExtension.create((containerId, inventory, data) ->
						new TutorialManualMenu(containerId, inventory, data.readVarInt())),
				new MenuType<RecyclerMenu>(RecyclerMenu::new, FeatureFlags.VANILLA_SET)
		));
		event.register(Registries.CREATIVE_MODE_TAB, helper -> ModCreativeTabs.register(
				registrar(helper),
				index -> CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, index)
		));
		event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, helper ->
				helper.register(EchoWarrior.id("player_modified_chunk"), PLAYER_MODIFIED_CHUNK));
	}

	private void registerAttributes(EntityAttributeCreationEvent event) {
		ModEntities.registerAttributes(event::put);
	}

	private void registerPayloads(RegisterPayloadHandlersEvent event) {
		var registrar = event.registrar("1");
		registrar.playToClient(EchoCompassStatePayload.TYPE, EchoCompassStatePayload.STREAM_CODEC);
		registrar.playToClient(EchoCompassPulsePayload.TYPE, EchoCompassPulsePayload.STREAM_CODEC);
		registrar.playToClient(EchoCompassMessagePayload.TYPE, EchoCompassMessagePayload.STREAM_CODEC);
		registrar.playToServer(
				CreativeSummonerDestroyPayload.TYPE,
				CreativeSummonerDestroyPayload.STREAM_CODEC,
				(payload, context) -> {
					if (context.player() instanceof ServerPlayer player) {
						CreativeSummonerDestroyTracker.requestCreativeTrash(player, payload.summonerIds());
					}
				}
		);
	}

	private void addBrushableBlocks(BlockEntityTypeAddBlocksEvent event) {
		event.modify(
				BlockEntityType.BRUSHABLE_BLOCK,
				ModBlocks.SUSPICIOUS_GRASS_BLOCK,
				ModBlocks.SUSPICIOUS_DIRT
		);
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		EchoWarrior.initialize();
	}

	private static <T> RegistryRegistrar<T> registrar(RegisterEvent.RegisterHelper<T> helper) {
		return (id, value) -> {
			helper.register(id, value);
			return value;
		};
	}
}
