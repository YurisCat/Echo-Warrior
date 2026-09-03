package com.yuriscat.echowarrior.neoforge;

import com.yuriscat.echowarrior.binding.CreativeSummonerDestroyTracker;
import com.yuriscat.echowarrior.binding.EchoBindingSystem;
import com.yuriscat.echowarrior.command.VisualDebugCommands;
import com.yuriscat.echowarrior.entity.CatGodCreeperSystem;
import com.yuriscat.echowarrior.entity.EchoAuraAuditSystem;
import com.yuriscat.echowarrior.entity.EchoCombatEvents;
import com.yuriscat.echowarrior.item.EchoAccessorySystem;
import com.yuriscat.echowarrior.item.EchoTalentSystem;
import com.yuriscat.echowarrior.knowledge.KnowledgeLootSystem;
import com.yuriscat.echowarrior.progress.EchoExperienceSystem;
import com.yuriscat.echowarrior.recycler.RecyclerSystem;
import com.yuriscat.echowarrior.world.BattlefieldSystem;
import com.yuriscat.echowarrior.world.EchoCompassSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

final class NeoForgeEventRegistrar {
	private NeoForgeEventRegistrar() {
	}

	static void register(IEventBus eventBus) {
		eventBus.addListener(NeoForgeEventRegistrar::onServerTick);
		eventBus.addListener(NeoForgeEventRegistrar::onLevelTick);
		eventBus.addListener(NeoForgeEventRegistrar::onServerStopped);
		eventBus.addListener(NeoForgeEventRegistrar::onPlayerLogin);
		eventBus.addListener(NeoForgeEventRegistrar::onPlayerLogout);
		eventBus.addListener(NeoForgeEventRegistrar::onIncomingDamage);
		eventBus.addListener(NeoForgeEventRegistrar::onDamageApplied);
		eventBus.addListener(NeoForgeEventRegistrar::onDeath);
		eventBus.addListener(NeoForgeEventRegistrar::onChunkLoad);
		eventBus.addListener(NeoForgeEventRegistrar::onChunkUnload);
		eventBus.addListener(EventPriority.LOWEST, true, BreakBlockEvent.class, NeoForgeEventRegistrar::onBreakBlock);
		eventBus.addListener(NeoForgeEventRegistrar::onRightClickBlock);
		eventBus.addListener(NeoForgeEventRegistrar::onLootTableLoad);
		eventBus.addListener(NeoForgeEventRegistrar::onRegisterCommands);
	}

	private static void onServerTick(ServerTickEvent.Post event) {
		var server = event.getServer();
		CreativeSummonerDestroyTracker.tick(server);
		EchoBindingSystem.tick(server);
		EchoAuraAuditSystem.tick(server);
		BattlefieldSystem.tick(server);
		EchoCompassSystem.tick(server);
		RecyclerSystem.tick(server);
	}

	private static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		CatGodCreeperSystem.tickPanickingCreepers(level);
		EchoAccessorySystem.tickLevel(level);
		EchoTalentSystem.tickLevel(level);
	}

	private static void onServerStopped(ServerStoppedEvent event) {
		CreativeSummonerDestroyTracker.clearAll();
		EchoBindingSystem.onServerStopped();
		EchoAuraAuditSystem.clear();
		BattlefieldSystem.clear();
	}

	private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		EchoBindingSystem.onPlayerJoin(player);
		EchoCombatEvents.clearPersistentCombatEffects(player);
	}

	private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) return;
		CreativeSummonerDestroyTracker.clearPlayer(player.getUUID());
		EchoCombatEvents.clearPersistentCombatEffects(player);
	}

	private static void onIncomingDamage(LivingIncomingDamageEvent event) {
		float adjusted = EchoCombatEvents.modifyIncomingDamage(event.getEntity(), event.getSource(), event.getAmount());
		if (!EchoAccessorySystem.allowDamage(event.getEntity(), event.getSource(), adjusted)) {
			event.setCanceled(true);
			return;
		}
		event.setAmount(adjusted);
	}

	private static void onDamageApplied(LivingDamageEvent.Post event) {
		boolean blocked = event.getBlockedDamage() > 0.0F;
		EchoExperienceSystem.afterDamage(
				event.getEntity(), event.getSource(), event.getOriginalDamage(), event.getHealthDamage(), blocked);
		EchoCombatEvents.afterDamage(
				event.getEntity(), event.getSource(), event.getOriginalDamage(), event.getHealthDamage(), blocked);
		EchoAccessorySystem.afterDamage(
				event.getEntity(), event.getSource(), event.getOriginalDamage(), event.getHealthDamage(), blocked);
		EchoTalentSystem.afterDamage(
				event.getEntity(), event.getSource(), event.getOriginalDamage(), event.getHealthDamage(), blocked);
	}

	private static void onDeath(LivingDeathEvent event) {
		if (!(event.getEntity().level() instanceof ServerLevel)) return;
		EchoExperienceSystem.afterDeath(event.getEntity(), event.getSource());
		EchoBindingSystem.onLivingDeath(event.getEntity());
		EchoTalentSystem.afterDeath(event.getEntity(), event.getSource());
	}

	private static void onChunkLoad(ChunkEvent.Load event) {
		if (!(event.getChunk().getLevel() instanceof ServerLevel level)) return;
		level.getServer().execute(() -> BattlefieldSystem.noteChunk(level, event.getChunk(), event.isNewChunk()));
	}

	private static void onChunkUnload(ChunkEvent.Unload event) {
		if (event.getChunk().getLevel() instanceof ServerLevel level) {
			BattlefieldSystem.forgetChunk(level, event.getChunk());
		}
	}

	private static void onBreakBlock(BreakBlockEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		if (!RecyclerSystem.allowBreak(level.getBlockEntity(event.getPos()))) {
			event.setCanceled(true);
			event.setNotifyClient(true);
			return;
		}
		if (!event.isCanceled()) BattlefieldSystem.markPlayerModified(level, event.getPos());
	}

	private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		var used = event.getItemStack();
		if (used.getItem() instanceof BlockItem || used.getItem() instanceof BucketItem) {
			BattlefieldSystem.markPlayerModified(level, event.getPos());
		}
	}

	private static void onLootTableLoad(LootTableLoadEvent event) {
		KnowledgeLootSystem.modify(event.getKey(), pool -> event.getTable().addPool(pool.build()));
	}

	private static void onRegisterCommands(RegisterCommandsEvent event) {
		VisualDebugCommands.register(event.getDispatcher());
	}
}
