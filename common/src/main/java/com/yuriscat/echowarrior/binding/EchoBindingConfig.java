package com.yuriscat.echowarrior.binding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.platform.PlatformServices;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Small dependency-free server config for persistent Echo limits. */
public final class EchoBindingConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static EchoBindingConfig instance = new EchoBindingConfig();

	/** Zero means unlimited. */
	public int maxLivingEchoesPerController = 0;

	private EchoBindingConfig() {
	}

	public static void load() {
		Path path = PlatformServices.configDirectory().resolve("echo_warrior-bindings.json");
		try {
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path)) {
					EchoBindingConfig loaded = GSON.fromJson(reader, EchoBindingConfig.class);
					if (loaded != null) instance = loaded;
				}
			} else {
				Files.createDirectories(path.getParent());
				try (Writer writer = Files.newBufferedWriter(path)) {
					GSON.toJson(instance, writer);
				}
			}
		} catch (IOException | RuntimeException exception) {
			EchoWarrior.LOGGER.error("Unable to load Echo binding config; using unlimited default", exception);
			instance = new EchoBindingConfig();
		}
		instance.maxLivingEchoesPerController = Math.max(0, instance.maxLivingEchoesPerController);
	}

	public static int maxLivingEchoesPerController() {
		return instance.maxLivingEchoesPerController;
	}
}
