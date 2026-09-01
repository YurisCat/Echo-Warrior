package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.command.VisualDebugCommands;
import com.yuriscat.echowarrior.binding.CreativeSummonerDestroyTracker;
import com.yuriscat.echowarrior.binding.EchoBindingSystem;
import com.yuriscat.echowarrior.entity.EchoCombatEvents;
import com.yuriscat.echowarrior.entity.EchoAuraAuditSystem;
import com.yuriscat.echowarrior.entity.CatGodCreeperSystem;
import com.yuriscat.echowarrior.progress.EchoExperienceSystem;
import com.yuriscat.echowarrior.world.BattlefieldSystem;
import com.yuriscat.echowarrior.world.EchoCompassSystem;
import com.yuriscat.echowarrior.item.EchoAccessorySystem;
import com.yuriscat.echowarrior.item.EchoTalentSystem;
import com.yuriscat.echowarrior.knowledge.KnowledgeLootSystem;
import com.yuriscat.echowarrior.network.EchoCompassMessagePayload;
import com.yuriscat.echowarrior.network.EchoCompassStatePayload;
import com.yuriscat.echowarrior.network.EchoCompassPulsePayload;
import com.yuriscat.echowarrior.network.CreativeSummonerDestroyPayload;
import com.yuriscat.echowarrior.recycler.RecyclerSystem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EchoWarrior implements ModInitializer {
	public static final String MOD_ID = "echo_warrior";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.clientboundPlay().register(EchoCompassStatePayload.TYPE, EchoCompassStatePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EchoCompassPulsePayload.TYPE, EchoCompassPulsePayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EchoCompassMessagePayload.TYPE, EchoCompassMessagePayload.STREAM_CODEC);
		PayloadTypeRegistry.serverboundPlay().register(
				CreativeSummonerDestroyPayload.TYPE, CreativeSummonerDestroyPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CreativeSummonerDestroyPayload.TYPE, (payload, context) ->
				context.server().execute(() -> CreativeSummonerDestroyTracker.requestCreativeTrash(
						context.player(), payload.summonerIds())));
		ModEffects.initialize();
		ModEntities.initialize();
		ModBlocks.initialize();
		ModBlockEntities.initialize();
		ModItems.initialize();
		ModRecipes.initialize();
		ModMenus.initialize();
		ModCreativeTabs.initialize();
		KnowledgeLootSystem.initialize();
		RecyclerSystem.initialize();
		BattlefieldSystem.initialize();
		EchoCompassSystem.initialize();
		EchoExperienceSystem.initialize();
		EchoBindingSystem.initialize();
		CreativeSummonerDestroyTracker.initialize();
		EchoAuraAuditSystem.initialize();
		EchoCombatEvents.initialize();
		EchoAccessorySystem.initialize();
		EchoTalentSystem.initialize();
		CatGodCreeperSystem.initialize();
		VisualDebugCommands.initialize();
		LOGGER.info("Echo Warrior is awakening.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
