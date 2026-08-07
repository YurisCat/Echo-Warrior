package com.yuriscat.echowarrior;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EchoWarrior implements ModInitializer {
	public static final String MOD_ID = "echo_warrior";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.initialize();
		ModItems.initialize();
		ModCreativeTabs.initialize();
		LOGGER.info("Echo Warrior is awakening.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
