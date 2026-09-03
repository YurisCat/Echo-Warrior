package com.yuriscat.echowarrior.fabric;

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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class EchoWarriorFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		PlatformServices.install(new FabricPlatformServices());
		registerContent();
		registerNetworking();
		FabricEventRegistrar.register();
		EchoWarrior.initialize();
	}

	private static void registerContent() {
		ModEffects.register((id, value) -> Registry.register(BuiltInRegistries.MOB_EFFECT, id, value));
		ModEntities.register((id, value) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, id, value));
		ModBlocks.registerBlocks((id, value) -> Registry.register(BuiltInRegistries.BLOCK, id, value));
		ModBlockEntities.register(
				(id, value) -> Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, value),
				FabricBlockEntityTypeBuilder.create(RecyclerChestBlockEntity::new, ModBlocks.ECHO_RECYCLER).build()
		);
		ModBlocks.registerItems((id, value) -> Registry.register(BuiltInRegistries.ITEM, id, value));
		ModItems.register((id, value) -> Registry.register(BuiltInRegistries.ITEM, id, value));
		ModRecipes.register((id, value) -> Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, value));
		ModMenus.register(
				(id, value) -> Registry.register(BuiltInRegistries.MENU, id, value),
				new ExtendedMenuType<SummonerMenu, Integer>(SummonerMenu::new, ByteBufCodecs.VAR_INT),
				new ExtendedMenuType<KnowledgeReaderMenu, Integer>(KnowledgeReaderMenu::new, ByteBufCodecs.VAR_INT),
				new ExtendedMenuType<TutorialManualMenu, Integer>(TutorialManualMenu::new, ByteBufCodecs.VAR_INT),
				new MenuType<RecyclerMenu>(RecyclerMenu::new, FeatureFlags.VANILLA_SET)
		);
		ModCreativeTabs.register(
				(id, value) -> Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id, value),
				ignored -> FabricCreativeModeTab.builder()
		);
		ModEntities.registerAttributes(FabricDefaultAttributeRegistry::register);
		BlockEntityType.BRUSHABLE_BLOCK.addValidBlock(ModBlocks.SUSPICIOUS_GRASS_BLOCK);
		BlockEntityType.BRUSHABLE_BLOCK.addValidBlock(ModBlocks.SUSPICIOUS_DIRT);
	}

	private static void registerNetworking() {
		PayloadTypeRegistry.clientboundPlay().register(EchoCompassStatePayload.TYPE, EchoCompassStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EchoCompassPulsePayload.TYPE, EchoCompassPulsePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EchoCompassMessagePayload.TYPE, EchoCompassMessagePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(
				CreativeSummonerDestroyPayload.TYPE, CreativeSummonerDestroyPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CreativeSummonerDestroyPayload.TYPE, (payload, context) ->
				context.server().execute(() -> CreativeSummonerDestroyTracker.requestCreativeTrash(
						context.player(), payload.summonerIds())));
	}
}
