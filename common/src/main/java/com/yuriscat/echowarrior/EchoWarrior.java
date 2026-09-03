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
import com.yuriscat.echowarrior.recycler.RecyclerSystem;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EchoWarrior {
	public static final String MOD_ID = "echo_warrior";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private EchoWarrior() {
	}

	public static void initialize() {
		ModBlocks.initialize();
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
