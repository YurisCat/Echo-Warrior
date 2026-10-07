package com.yuriscat.echowarrior.compat.item;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.LoggerFactory;

/** Server-owned startup setting; clients receive the value for the current connection. */
public final class EchoProgressionConfig1201 {
    public static final int DEFAULT_MAX_LEVEL = 100;
    // Keeps fixed-point menu attributes and per-level XP representable as signed ints.
    public static final int HIGHEST_SUPPORTED_LEVEL = 1_000_000;
    private static volatile int localMaximum = DEFAULT_MAX_LEVEL;
    private static volatile int effectiveMaximum = DEFAULT_MAX_LEVEL;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final class Settings {
        int schemaVersion = 1;
        int maxLevel = DEFAULT_MAX_LEVEL;
    }
    private EchoProgressionConfig1201() {}

    public static void load(Path directory) {
        Settings settings = new Settings();
        Path path = directory.resolve("echo_warrior-progression.json");
        try {
            if (Files.exists(path)) {
                try (var reader = Files.newBufferedReader(path)) {
                    settings = GSON.fromJson(reader, Settings.class);
                }
                if (settings == null || settings.schemaVersion != 1
                        || settings.maxLevel < 1 || settings.maxLevel > HIGHEST_SUPPORTED_LEVEL)
                    throw new IllegalArgumentException("Expected schemaVersion 1 and maxLevel in 1.." + HIGHEST_SUPPORTED_LEVEL);
            } else {
                Files.createDirectories(directory);
                try (var writer = Files.newBufferedWriter(path)) { GSON.toJson(settings, writer); }
            }
        } catch (Exception exception) {
            LoggerFactory.getLogger("echo_warrior").error("Unable to load {}; using maxLevel=100", path, exception);
            settings = new Settings();
        }
        localMaximum = settings.maxLevel;
        resetConnection();
    }
    public static int maxLevel() { return effectiveMaximum; }
    public static int serverMaxLevel() { return localMaximum; }
    public static void receiveServerMaximum(int maximum) {
        if (maximum < 1 || maximum > HIGHEST_SUPPORTED_LEVEL)
            throw new IllegalArgumentException("Invalid Echo progression maximum: " + maximum);
        effectiveMaximum = maximum;
    }
    public static void resetConnection() { effectiveMaximum = localMaximum; }
}
