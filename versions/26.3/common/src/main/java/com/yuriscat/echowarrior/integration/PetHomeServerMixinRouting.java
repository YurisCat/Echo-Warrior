package com.yuriscat.echowarrior.integration;

import com.google.gson.JsonParser;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.Config;
import org.spongepowered.asm.service.MixinService;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Corrects two client-only accessors misplaced in the pinned 26.1.2 Fabric release.
 * Does not edit the dependency JAR, disable a whole config, suppress Mixin failures,
 * or change the client. Other upstream versions with correct metadata are untouched.
 */
public final class PetHomeServerMixinRouting {
    private static final Map<String, String> CLIENT_TARGETS = Map.of(
            "ParticleEngineAccessor", "net.minecraft.client.particle.ParticleEngine",
            "SpriteContentsAccessor", "net.minecraft.client.renderer.texture.SpriteContents");
    private PetHomeServerMixinRouting() {}

    public static void correct() {
        if (MixinEnvironment.getCurrentEnvironment().getSide() != MixinEnvironment.Side.SERVER) return;
        var service = MixinService.getService();
        try (var stream = service.getResourceAsStream("pet_home.mixins.json")) {
            if (stream == null) return;
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!"com.github.yzqdev.pethome.mixin".equals(json.get("package").getAsString())) return;
            var common = json.getAsJsonArray("mixins");
            List<String> misplaced = new ArrayList<>();
            for (var entry : common) if (CLIENT_TARGETS.containsKey(entry.getAsString())) misplaced.add(entry.getAsString());
            if (misplaced.isEmpty()) return;

            for (String name : misplaced) {
                var node = service.getBytecodeProvider().getClassNode("com.github.yzqdev.pethome.mixin." + name);
                var annotation = node.invisibleAnnotations == null ? null : node.invisibleAnnotations.stream()
                        .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElse(null);
                if (annotation == null && node.visibleAnnotations != null)
                    annotation = node.visibleAnnotations.stream()
                            .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElse(null);
                boolean exactClientTarget = false;
                if (annotation != null) for (int i = 0; i < annotation.values.size(); i += 2) {
                    if ("value".equals(annotation.values.get(i)) && annotation.values.get(i + 1) instanceof List<?> values
                            && values.size() == 1 && values.getFirst() instanceof Type type
                            && CLIENT_TARGETS.get(name).equals(type.getClassName())) exactClientTarget = true;
                }
                if (!exactClientTarget) throw new IllegalStateException("Unreviewed target for " + name);
            }

            // Config.create returns the already registered config from Mixin's own cache.
            // onLoad runs during selection, before any pending config is prepared.
            var config = Config.create("pet_home.mixins.json", MixinEnvironment.getCurrentEnvironment()).getConfig();
            if (!config.getMixinPackage().replaceAll("\\.$", "").equals("com.github.yzqdev.pethome.mixin"))
                throw new IllegalStateException("Unexpected pet config identity");
            var prepared = config.getClass().getDeclaredField("prepared");
            prepared.setAccessible(true);
            if (prepared.getBoolean(config)) throw new IllegalStateException("Pet config was already prepared");
            var field = config.getClass().getDeclaredField("mixinClasses");
            field.setAccessible(true);
            Object value = field.get(config);
            if (!(value instanceof List<?> names) || !names.containsAll(misplaced))
                throw new IllegalStateException("Pet config contents changed before routing");
            var corrected = new ArrayList<>(names);
            corrected.removeAll(misplaced);
            if (corrected.size() != names.size() - misplaced.size())
                throw new IllegalStateException("Duplicate or unexpected client accessor entries");
            field.set(config, corrected);
            service.getLogger("echo_warrior").info(
                    "[Pet collar compatibility] Dedicated-server routing: {} client accessors excluded, {} common mixins retained",
                    misplaced.size(), corrected.size());
        } catch (Exception error) {
            // Never proceed with partially disabled server functionality if Mixin's structure changes.
            throw new LinkageError("Unable to isolate the known Pet Home client-only accessors on the dedicated server", error);
        }
    }
}
