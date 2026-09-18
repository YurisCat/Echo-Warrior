package com.yuriscat.echowarrior.compat;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EchoWarrior1211 {
    public static final String MOD_ID = "echo_warrior";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private EchoWarrior1211() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void initialize() {
        LOGGER.info("Initializing Echo Warrior five-hero compatibility line for Minecraft 1.21.1");
    }
}
