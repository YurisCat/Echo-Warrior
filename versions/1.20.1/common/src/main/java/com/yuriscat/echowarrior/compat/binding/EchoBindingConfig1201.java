package com.yuriscat.echowarrior.compat.binding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yuriscat.echowarrior.compat.EchoWarrior1201;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Shared server config for the 1.20.1 persistent Echo limit. */
public final class EchoBindingConfig1201 {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static EchoBindingConfig1201 instance = new EchoBindingConfig1201();

    /** Zero means unlimited. */
    public int maxLivingEchoesPerController = 0;

    private EchoBindingConfig1201() {
    }

    public static void load(Path configDirectory) {
        com.yuriscat.echowarrior.compat.item.EchoProgressionConfig1201.load(configDirectory);
        Path path = configDirectory.resolve("echo_warrior-bindings.json");
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    EchoBindingConfig1201 loaded = GSON.fromJson(reader, EchoBindingConfig1201.class);
                    if (loaded != null) instance = loaded;
                }
            } else {
                Files.createDirectories(path.getParent());
                try (Writer writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(instance, writer);
                }
            }
        } catch (IOException | RuntimeException exception) {
            EchoWarrior1201.LOGGER.error(
                    "Unable to load 1.20.1 Echo binding config; using unlimited default", exception);
            instance = new EchoBindingConfig1201();
        }
        instance.maxLivingEchoesPerController = Math.max(0, instance.maxLivingEchoesPerController);
    }

    public static int maxLivingEchoesPerController() {
        return instance.maxLivingEchoesPerController;
    }
}
