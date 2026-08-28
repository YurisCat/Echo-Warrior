package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.command.VisualDebugCommands;
import com.yuriscat.echowarrior.entity.EchoCombatEvents;
import com.yuriscat.echowarrior.entity.CatGodCreeperSystem;
import com.yuriscat.echowarrior.progress.EchoExperienceSystem;
import com.yuriscat.echowarrior.world.BattlefieldSystem;
import com.yuriscat.echowarrior.world.EchoCompassSystem;
import com.yuriscat.echowarrior.item.EchoAccessorySystem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EchoWarrior implements ModInitializer {
	public static final String MOD_ID = "echo_warrior";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEffects.initialize();
		ModEntities.initialize();
		ModBlocks.initialize();
		ModItems.initialize();
		ModRecipes.initialize();
		ModMenus.initialize();
		ModCreativeTabs.initialize();
		BattlefieldSystem.initialize();
		EchoCompassSystem.initialize();
		EchoExperienceSystem.initialize();
		EchoCombatEvents.initialize();
		EchoAccessorySystem.initialize();
		CatGodCreeperSystem.initialize();
		VisualDebugCommands.initialize();
		LOGGER.info("Echo Warrior is awakening.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
