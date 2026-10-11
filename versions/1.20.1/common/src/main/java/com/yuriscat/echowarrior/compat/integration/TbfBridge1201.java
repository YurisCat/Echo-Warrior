package com.yuriscat.echowarrior.compat.integration;

import com.yuriscat.echowarrior.compat.ModContent1201;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1201;
import com.yuriscat.echowarrior.compat.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

/** TBF-specific presentation and dispatch. Its files are display mirrors, never summon authority. */
public final class TbfBridge1201 {
    public static final String MARKER = "EchoWarriorExternalBinding";
    private static final String ROOT = "com.whidte.trulybestfriends.";
    private static final ThreadLocal<Boolean> UNTRACKING = ThreadLocal.withInitial(() -> false);
    public static boolean removingMirror() { return UNTRACKING.get(); }
    private static final ThreadLocal<Boolean> PASSTHROUGH = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Map<UUID, CompoundTag>> LAST_SENT = new HashMap<>();
    private static final Set<UUID> SCANNED = new HashSet<>();
    private static final Map<UUID, Long> LAST_TOGGLE = new HashMap<>();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("echo_warrior");
    private static boolean enabled;

    private TbfBridge1201() {}
    public static void initialize(String version) {
        var api = TbfMixinPlugin1201.api();
        boolean legacyFabric = api.context().equals("Lcom/whidte/trulybestfriends/network/PacketContext;");
        boolean eligible = TbfCompatibility1201.versionSupported(version, legacyFabric);
        enabled = eligible && api.compatible();
        if (enabled) LOG.info("[TBF compatibility] enabled for {} after API checks (presence probe: {}, owner-hint restore: {})",
                version, api.presenceProbe(), api.ownerHintRestore());
        else LOG.warn("[TBF compatibility] adapter disabled for {}: {}", version,
                eligible ? api.reason() : "version is below 0.2.3 or unparseable");
    }
    public static boolean enabled() { return enabled; }
    public static void clear() { LAST_SENT.clear(); LAST_TOGGLE.clear(); SCANNED.clear(); }

    public static Object call(String type, String method, Object... arguments) {
        try {
            Class<?> target = Class.forName(ROOT + type);
            for (Method candidate : target.getDeclaredMethods()) {
                if (candidate.getName().equals(method) && candidate.getParameterCount() == arguments.length
                        && fits(candidate.getParameterTypes(), arguments)) {
                    candidate.setAccessible(true);
                    return candidate.invoke(null, arguments);
                }
            }
            throw new NoSuchMethodException(type + "." + method);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("TBF adapter API mismatch", failure); }
    }
    private static boolean fits(Class<?>[] types, Object[] values) {
        for (int i = 0; i < types.length; i++) {
            if (values[i] == null) { if (types[i].isPrimitive()) return false; }
            else if (!types[i].isInstance(values[i]) && !(types[i] == int.class && values[i] instanceof Integer)
                    && !(types[i] == boolean.class && values[i] instanceof Boolean)) return false;
        }
        return true;
    }
    private static Object field(Object packet, String name) {
        try { Field field = packet.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(packet); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("TBF packet field " + name, e); }
    }
    private static Object context(Object context) { return context instanceof Supplier<?> supplier ? supplier.get() : context; }
    private static ServerPlayer sender(Object context) {
        try {
            Object ctx = context(context);
            Method getter;
            try { getter = ctx.getClass().getMethod("getSender"); }
            catch (NoSuchMethodException neoForge) { getter = ctx.getClass().getMethod("player"); }
            getter.setAccessible(true);
            return (ServerPlayer) getter.invoke(ctx);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    private static void enqueue(Object context, Runnable runnable) {
        try { Object ctx = context(context); Method enqueue = ctx.getClass().getMethod("enqueueWork", Runnable.class);
            enqueue.setAccessible(true); enqueue.invoke(ctx, runnable);
            try { Method handled = ctx.getClass().getMethod("setPacketHandled", boolean.class); handled.setAccessible(true); handled.invoke(ctx, true); }
            catch (NoSuchMethodException neoForge) { /* NeoForge payload contexts need no handled flag. */ } }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    /** Called at TBF packet entry, before any file, fuel, healing or entity operation. */
    public static boolean intercept(Object packet, Object context) {
        if (!enabled || PASSTHROUGH.get()) return false;
        enqueue(context, () -> {
            ServerPlayer player = sender(context);
            if (player == null || player.hasDisconnected()) return;
            try {
                refresh(player, false);
                String kind = packet.getClass().getSimpleName();
                if (kind.equals("RequestPetDataPacket")) {
                    passthrough(packet, context);
                    return;
                }
                if (kind.equals("AreaRecallPacket")) {
                    int range = Math.max(1, Math.min(16, (Integer)field(packet, "range")));
                    for (var binding : EchoBindingSavedData1201.get(player.server).bindings()) {
                        if (!player.getUUID().equals(binding.controllerId()) || !binding.active()) continue;
                        var echo = EchoBindingSystem1201.findLoaded(player.server, binding.spiritId());
                        UUID id = EchoExternalCompanion1201.entryId(binding);
                        if (echo != null && echo.livingEntity().level() == player.level()
                                && echo.livingEntity().distanceTo(player) <= range && !TbfTrackingData1201.get(player.server).muted(player.getUUID(), id))
                            EchoExternalCompanion1201.dismiss(player, id);
                    }
                    refresh(player, true);
                    passthrough(packet, context);
                    return;
                }
                UUID id = (UUID)field(packet, "petUuid");
                if (id == null || !isExternalFile(player, id)) { passthrough(packet, context); return; }
                if (kind.equals("DeletePetDataPacket")) { untrack(player, id); return; }
                var binding = EchoExternalCompanion1201.resolve(player, id);
                if (binding == null) { feedback(player, "unavailable"); return; }
                switch (kind) {
                    case "RecallPetPacket" -> {
                        // Two queued toggles from a double click must not dismiss then charge for a new summon.
                        long now = player.server.overworld().getGameTime();
                        if (now - LAST_TOGGLE.getOrDefault(id, Long.MIN_VALUE / 2) < 5) return;
                        LAST_TOGGLE.put(id, now);
                        if (binding.active()) EchoExternalCompanion1201.dismiss(player, id);
                        else summon(player, id);
                    }
                    case "SummonPetPacket", "TeleportPetToPlayerPacket", "DirectTeleportPetToPlayerPacket",
                            "ReleaseRecalledPetPacket", "RevivePetPacket" -> summon(player, id);
                    case "HealPetPacket" -> feedback(player, "healing_disabled");
                    case "DeletePetDataPacket" -> untrack(player, id);
                    case "SetPriorityPacket" -> { passthrough(packet, context); return; }
                    default -> throw new IllegalStateException("Unreviewed external pet operation " + kind);
                }
                refresh(player, true);
            } catch (RuntimeException error) {
                LOG.error("[TBF compatibility] operation failed", error);
                feedback(player, "unavailable");
            }
        });
        return true;
    }
    private static void passthrough(Object packet, Object ctx) {
        boolean previous = PASSTHROUGH.get();
        PASSTHROUGH.set(true);
        try { call("network." + packet.getClass().getSimpleName(), "handle", packet, ctx); }
        finally { PASSTHROUGH.set(previous); }
    }
    public static boolean summonWithoutRideSwap(UUID id, Object ctx) {
        if (!enabled) return false;
        ServerPlayer player = sender(ctx);
        if (player == null || !player.server.isSameThread() || !isExternalFile(player, id)) return false;
        summon(player, id);
        refresh(player, true);
        return true;
    }
    private static void summon(ServerPlayer player, UUID id) {
        var result = EchoExternalCompanion1201.summon(player, id);
        if (!result.succeeded()) {
            String key = switch (result.failure()) {
                case "NOT_ENOUGH_FUEL" -> "gui.echo_warrior.summoner.feedback.not_enough_fuel";
                case "LIMIT_REACHED" -> "gui.echo_warrior.summoner.feedback.limit_reached";
                case "NO_SAFE_POSITION" -> "gui.echo_warrior.summoner.feedback.no_safe_position";
                default -> "message.echo_warrior.tbf.unavailable";
            };
            player.displayClientMessage(Component.translatable(key), true);
        }
    }
    private static Path directory(ServerPlayer player) { return (Path)call("network.PetIOUtil", "getOwnerDir", player); }
    private static CompoundTag read(Path path) { return (CompoundTag)call("network.NbtFileIO", "readCompressed", path.toFile()); }
    private static void write(Path path, CompoundTag tag) { call("network.NbtFileIO", "writeCompressed", tag, path.toFile()); }
    private static boolean isExternalFile(ServerPlayer player, UUID id) {
        Path path = directory(player).resolve(id + ".nbt");
        return Files.isRegularFile(path) && isEchoSnapshot(read(path));
    }
    public static boolean isEchoSnapshot(CompoundTag snapshot) {
        return snapshot.hasUUID(MARKER) || snapshot.getString("EntityType").startsWith("echo_warrior:")
                || snapshot.getString("id").startsWith("echo_warrior:");
    }
    private static void feedback(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.echo_warrior.tbf." + key), true);
    }
    private static void removeMirror(ServerPlayer player, UUID id) {
        Path file = directory(player).resolve(id + ".nbt");
        // Only a positively identified compatibility display file can be removed here.
        if (Files.isRegularFile(file) && !isEchoSnapshot(read(file))) return;
        UNTRACKING.set(true);
        try {
            if (Files.isRegularFile(file) && !Boolean.TRUE.equals(call("trulybestfriends", "deletePetData", player, id)))
                throw new IllegalStateException("TBF could not delete the external display entry " + id);
        } finally { UNTRACKING.set(false); }
        send(player, "delete", id);
    }
    private static void untrack(ServerPlayer player, UUID id) {
        TbfTrackingData1201.get(player.server).mute(player.getUUID(), id, true);
        removeMirror(player, id);
        LAST_SENT.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).remove(id);
    }
    /** Called only by TBF's explicit item/command entry; its caller retains item cost and OP checks. */
    public static Integer manualTrack(net.minecraft.commands.CommandSourceStack source, ServerPlayer player,
            Entity entity, boolean informAdmins) {
        Object result = forceTrack(entity, player);
        if (result == null) return null; // Ordinary pets must keep TBF's own validation and save path.
        return (Integer)call("command.ModCommands", "reportLoadResult", source, entity, result,
                "trulybestfriends.load.success", "trulybestfriends.load.not_a_pet", informAdmins);
    }
    public static Object forceTrack(Entity entity, ServerPlayer player) {
        if (!enabled || !(entity instanceof EchoWarriorEntity1201 echo)) return null;
        UUID summoner = echo.getSummonerId();
        var binding = summoner == null ? null : EchoBindingSavedData1201.get(player.server).get(summoner);
        String result = "NOT_A_PET";
        if (binding != null && player.getUUID().equals(binding.controllerId())
                && binding.active() && entity.getUUID().equals(binding.spiritId())
                && echo.getBindingGeneration() == binding.generation()) {
            TbfTrackingData1201.get(player.server).mute(player.getUUID(), EchoExternalCompanion1201.entryId(binding), false);
            refresh(player, true);
            result = "OK";
        }
        try {
            @SuppressWarnings({"rawtypes", "unchecked"}) Object value = Enum.valueOf((Class)Class.forName(ROOT + "trulybestfriends$LoadResult"), result);
            return value;
        } catch (ClassNotFoundException e) { throw new IllegalStateException(e); }
    }
    private static void send(ServerPlayer player, String action, Object... arguments) {
        Object packet = call("network.SyncPetDataPacket", action, arguments);
        call("network.SyncPetDataPacket", "sendToPlayer", player, packet);
    }
    public static void tick(MinecraftServer server) {
        if (!enabled || server.getTickCount() % 20 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try { refresh(player, false); }
            catch (RuntimeException e) { LOG.error("[TBF compatibility] synchronization failed", e); }
        }
        LAST_SENT.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        SCANNED.removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }
    public static void refresh(ServerPlayer player, boolean force) {
        if (!enabled || !player.server.isSameThread()) return;
        Path dir = directory(player);
        try { Files.createDirectories(dir); } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        Map<UUID, CompoundTag> previous = LAST_SENT.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        Set<UUID> current = new HashSet<>();
        for (var binding : EchoBindingSavedData1201.get(player.server).bindings()) {
            if (!player.getUUID().equals(binding.controllerId()) || !(binding.relic().getItem() instanceof EchoRelicItem1201)) continue;
            UUID id = EchoExternalCompanion1201.entryId(binding);
            if (TbfTrackingData1201.get(player.server).muted(player.getUUID(), id)) continue;
            current.add(id);
            // Existing old-version automatic recovery checks carried items. Only this optional adapter
            // adds box-backed FOLLOW recovery; the standalone Echo recovery policy is unchanged.
            var live = EchoBindingSystem1201.findLoaded(player.server, binding.spiritId());
            if (binding.active() && binding.activityMode() == EchoRelicState1201.ActivityMode.FOLLOW.ordinal()
                    && (live == null || live.livingEntity().level() != player.level()) && player.isAlive() && !player.isSpectator()) {
                EchoExternalCompanion1201.summon(player, id);
                live = EchoBindingSystem1201.findLoaded(player.server, binding.spiritId());
            }
            Path file = dir.resolve(id + ".nbt");
            CompoundTag old = Files.isRegularFile(file) ? read(file) : new CompoundTag();
            // A colliding ordinary pet file is never overwritten.
            if (!old.isEmpty() && !old.hasUUID(MARKER)) continue;
            CompoundTag view = snapshot(player, binding, live, id);
            view.putInt("Priority", old.contains("Priority") ? old.getInt("Priority") : 3);
            if (!view.equals(old)) write(file, view);
            if (force || !view.equals(previous.get(id))) send(player, "update", id, view.copy());
            previous.put(id, view.copy());
        }
        if (SCANNED.add(player.getUUID())) {
            try (var files = Files.list(dir)) {
                for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".nbt")).toList()) {
                    UUID id;
                    try { id = UUID.fromString(file.getFileName().toString().replace(".nbt", "")); }
                    catch (IllegalArgumentException ignored) { continue; }
                    if (!current.contains(id) && read(file).hasUUID(MARKER)) previous.putIfAbsent(id, new CompoundTag());
                }
            } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        }
        for (UUID id : new HashSet<>(previous.keySet())) {
            if (!current.contains(id)) { removeMirror(player, id); previous.remove(id); }
        }
    }

    private static CompoundTag snapshot(ServerPlayer player, EchoBindingSavedData1201.Binding binding,
            EchoWarriorEntity1201 live, UUID id) {
        CompoundTag nbt = new CompoundTag();
        if (live != null) live.livingEntity().saveAsPassenger(nbt);
        // Inactive entries need only a renderable entity type and summary attributes, not a restorable save.
        var type = ((EchoRelicItem1201)binding.relic().getItem()).heroType();
        String entityType = "echo_warrior:" + type.id() + "_echo";
        nbt.putString("id", entityType);
        nbt.putString("EntityType", entityType);
        nbt.putUUID("UUID", id);
        nbt.putUUID(MARKER, binding.summonerId());
        nbt.putString("EchoWarriorExternalRelic", EchoRelicState1201.relicId(binding.relic()));
        if (TbfMixinPlugin1201.api().ownerTag()) call("TbfOwnerTag", "write", nbt, player.getUUID());
        else nbt.putString("OwnerUUID", player.getUUID().toString());
        nbt.putString("Dimension", binding.active() ? binding.snapshot().dimension() : player.serverLevel().dimension().location().toString());
        nbt.putBoolean("Recalled", !binding.active());
        nbt.putBoolean("Lost", false);
        nbt.putBoolean("Dead", false);
        nbt.putFloat("Health", live != null ? live.livingEntity().getHealth()
                : binding.active() ? binding.snapshot().health() : (float)EchoRelicState1201.maximumHealth(binding.relic()));
        nbt.putFloat("MaxHealth", live == null ? (float)EchoRelicState1201.maximumHealth(binding.relic()) : live.livingEntity().getMaxHealth());
        ListTag position = new ListTag();
        var pos = live != null ? live.livingEntity().position() : binding.active()
                ? new net.minecraft.world.phys.Vec3(binding.snapshot().x(), binding.snapshot().y(), binding.snapshot().z()) : player.position();
        position.add(DoubleTag.valueOf(pos.x)); position.add(DoubleTag.valueOf(pos.y)); position.add(DoubleTag.valueOf(pos.z));
        nbt.put("Pos", position);
        nbt.remove("Passengers");
        return nbt;
    }
}
