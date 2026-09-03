package com.yuriscat.echowarrior.fabric;

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
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;

final class FabricEventRegistrar {
	private FabricEventRegistrar() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			CreativeSummonerDestroyTracker.tick(server);
			EchoBindingSystem.tick(server);
			EchoAuraAuditSystem.tick(server);
			BattlefieldSystem.tick(server);
			EchoCompassSystem.tick(server);
			RecyclerSystem.tick(server);
		});
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			CatGodCreeperSystem.tickPanickingCreepers(level);
			EchoAccessorySystem.tickLevel(level);
			EchoTalentSystem.tickLevel(level);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			CreativeSummonerDestroyTracker.clearAll();
			EchoBindingSystem.onServerStopped();
			EchoAuraAuditSystem.clear();
			BattlefieldSystem.clear();
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			EchoBindingSystem.onPlayerJoin(handler.getPlayer());
			EchoCombatEvents.clearPersistentCombatEffects(handler.getPlayer());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			CreativeSummonerDestroyTracker.clearPlayer(handler.getPlayer().getUUID());
			EchoCombatEvents.clearPersistentCombatEffects(handler.getPlayer());
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
				EchoCombatEvents.allowDamage(entity, source, amount)
						&& EchoAccessorySystem.allowDamage(entity, source, amount));
		ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamageTaken, damageTaken, blocked) -> {
			EchoExperienceSystem.afterDamage(victim, source, baseDamageTaken, damageTaken, blocked);
			EchoCombatEvents.afterDamage(victim, source, baseDamageTaken, damageTaken, blocked);
			EchoAccessorySystem.afterDamage(victim, source, baseDamageTaken, damageTaken, blocked);
			EchoTalentSystem.afterDamage(victim, source, baseDamageTaken, damageTaken, blocked);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			EchoExperienceSystem.afterDeath(entity, source);
			EchoBindingSystem.onLivingDeath(entity);
			EchoTalentSystem.afterDeath(entity, source);
		});
		ServerChunkEvents.CHUNK_GENERATE.register((level, chunk) -> BattlefieldSystem.noteChunk(level, chunk, true));
		ServerChunkEvents.CHUNK_LOAD.register(BattlefieldSystem::noteChunk);
		ServerChunkEvents.CHUNK_UNLOAD.register(BattlefieldSystem::forgetChunk);
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
				RecyclerSystem.allowBreak(blockEntity));
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel serverLevel) BattlefieldSystem.markPlayerModified(serverLevel, pos);
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level instanceof ServerLevel serverLevel) {
				var used = player.getItemInHand(hand);
				if (used.getItem() instanceof BlockItem || used.getItem() instanceof BucketItem) {
					BattlefieldSystem.markPlayerModified(serverLevel, hit.getBlockPos());
				}
			}
			return InteractionResult.PASS;
		});
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) ->
				KnowledgeLootSystem.modify(key, tableBuilder::withPool));
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) ->
				VisualDebugCommands.register(dispatcher));
	}
}
