package com.yuriscat.echowarrior.compat.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.ClickType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Explicit local acceptance harness. Uses ordinary client actions and packets;
 * it never edits server state or creates an integrated-server fake player.
 * Disabled unless both the test flag and a local queue directory are supplied.
 */
public final class PetAcceptanceClient1201 {
    private static final String DIRECTORY = System.getProperty("echo_warrior.acceptance_queue", "");
    private static final boolean ENABLED = Boolean.getBoolean("echo_warrior.acceptance_client") && !DIRECTORY.isBlank();
    private static int next = 1;
    private static boolean layoutChecked;
    private PetAcceptanceClient1201() {}

    public static void tick(Minecraft client) {
        if (!ENABLED) return;
        if (!layoutChecked && client.player != null && client.level != null) {
            PetFeedbackLayout1201.selfTest(client);
            layoutChecked = true;
        }
        Path root = Path.of(DIRECTORY);
        Path request = root.resolve(String.format("%05d.json", next));
        if (!Files.isRegularFile(request)) return;
        JsonObject result = new JsonObject();
        boolean stop = false;
        try {
            JsonObject action = JsonParser.parseString(Files.readString(request, StandardCharsets.UTF_8)).getAsJsonObject();
            if (action.has("at_ms") && System.currentTimeMillis() < action.get("at_ms").getAsLong()) return;
            String kind = action.get("action").getAsString();
            if (!kind.equals("stop") && !kind.equals("snapshot")
                    && (client.player == null || client.level == null || client.gameMode == null)) return;
            switch (kind) {
                case "snapshot" -> {}
                case "command" -> {
                    String command = action.get("command").getAsString();
                    if (command.contains("\n") || command.startsWith("/")) throw new IllegalArgumentException("Bare single command required");
                    client.player.connection.sendCommand(command);
                }
                case "select" -> {
                    int slot = action.get("slot").getAsInt();
                    if (slot < 0 || slot > 8) throw new IllegalArgumentException("Hotbar slot required");
                    client.player.getInventory().selected = slot;
                }
                case "use" -> client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                case "interact", "attack" -> {
                    UUID id = UUID.fromString(action.get("uuid").getAsString());
                    Entity target = null;
                    for (Entity entity : client.level.entitiesForRendering())
                        if (entity.getUUID().equals(id)) { target = entity; break; }
                    if (target == null) throw new IllegalStateException("Target not visible to this client");
                    if (kind.equals("attack")) client.gameMode.attack(client.player, target);
                    else client.gameMode.interact(client.player, target, InteractionHand.MAIN_HAND);
                }
                case "button" -> client.gameMode.handleInventoryButtonClick(
                        client.player.containerMenu.containerId, action.get("id").getAsInt());
                case "slot" -> client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId,
                        action.get("slot").getAsInt(), action.has("button") ? action.get("button").getAsInt() : 0,
                        ClickType.valueOf(action.has("type") ? action.get("type").getAsString() : "PICKUP"), client.player);
                case "close" -> client.player.closeContainer();
                case "disconnect" -> client.player.connection.getConnection().disconnect(Component.literal("Acceptance reconnect check"));
                case "stop" -> stop = true;
                default -> throw new IllegalArgumentException("Unknown acceptance action: " + kind);
            }
            result.addProperty("action", kind);
            result.addProperty("passed", true);
            result.addProperty("time_ms", System.currentTimeMillis());
            result.addProperty("connected", client.player != null && client.level != null);
            result.addProperty("screen", client.screen == null ? "" : client.screen.getClass().getName());
            if (client.player != null && client.level != null) {
                result.addProperty("player", client.player.getName().getString());
                result.addProperty("uuid", client.player.getUUID().toString());
                result.addProperty("dimension", client.level.dimension().toString());
                result.addProperty("menu", client.player.containerMenu.getClass().getName());
                result.addProperty("container_id", client.player.containerMenu.containerId);
                var items = new com.google.gson.JsonArray();
                for (int slot = 0; slot < 36; slot++) items.add(client.player.getInventory().getItem(slot).toString());
                result.add("inventory", items);
                var entities = new com.google.gson.JsonArray();
                for (Entity entity : client.level.entitiesForRendering()) {
                    String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                    if (!(entity instanceof LivingEntity)) continue;
                    JsonObject value = new JsonObject();
                    value.addProperty("uuid", entity.getUUID().toString());
                    value.addProperty("type", type);
                    value.addProperty("x", entity.getX());
                    value.addProperty("y", entity.getY());
                    value.addProperty("z", entity.getZ());
                    if (entity instanceof LivingEntity living) {
                        value.addProperty("health", living.getHealth());
                        value.addProperty("max_health", living.getMaxHealth());
                    }
                    entities.add(value);
                }
                result.add("entities", entities);
            }
        } catch (Exception failure) {
            result.addProperty("passed", false);
            result.addProperty("error", failure.toString());
        }
        try {
            Files.writeString(root.resolve(String.format("%05d-result.json", next)), result.toString(), StandardCharsets.UTF_8);
        } catch (java.io.IOException failure) {
            throw new IllegalStateException("Cannot record client acceptance result", failure);
        }
        next++;
        if (stop) client.stop();
    }
}
