package com.yuriscat.echowarrior.compat.world;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.item.EchoCompassItem1211;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

public final class EchoCompassSystem1211 {
    private static final double SEARCH_RADIUS = 2048.0;
    private static final double OUTSIDE_SOUND_RADIUS = 320.0;
    private static final double INNER_ENTER_RADIUS = 24.0;
    private static final double INNER_RELEASE_RADIUS = 48.0;
    private static final long SEARCH_INTERVAL = 40L;
    private static final long OUTSIDE_DETECTION_REARM_DELAY = 100L;
    private static final long SALVAGE_MESSAGE_INTERVAL = 60L;
    private static final long SALVAGE_MESSAGE_WINDOW = 200L;
    private static final long SALVAGE_MESSAGE_DISPLAY_DURATION = 48L;
    private static final long SALVAGE_RELEASE_DELAY = 60L;
    private static final Map<UUID, Tracking> TRACKING = new HashMap<>();
    private static final Map<UUID, RenderState> LAST_RENDER_STATES = new HashMap<>();
    private static final Holder<SoundEvent> HUM = BuiltInRegistries.SOUND_EVENT
            .wrapAsHolder(SoundEvents.AMETHYST_BLOCK_RESONATE);

    private EchoCompassSystem1211() {
    }

    public static void clear() {
        TRACKING.clear();
        LAST_RENDER_STATES.clear();
    }

    public static void playToggle(ServerPlayer player, boolean enabled) {
        player.connection.send(new ClientboundSoundPacket(HUM, SoundSource.PLAYERS,
                player.getX(), player.getY() + 1.0, player.getZ(), 0.7F, enabled ? 1.28F : 0.68F,
                player.getRandom().nextLong()));
    }

    public static boolean isInsideBattlefieldMode(ServerPlayer player) {
        Tracking tracking = TRACKING.get(player.getUUID());
        return tracking != null && (tracking.mode == Mode.INNER || tracking.mode == Mode.SALVAGE);
    }

    public static void onBattlefieldBlockRemoved(ServerLevel level, BattlefieldSavedData1211.RemovalResult result) {
        BattlefieldSavedData1211 data = BattlefieldSavedData1211.get(level);
        if (result.relicCompleted()) {
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                Tracking tracking = TRACKING.get(player.getUUID());
                if (tracking == null || tracking.centerPos != result.center().asLong() || !hasCompass(player)) continue;
                if (result.remaining().isEmpty()) {
                    data.clearSalvageTracker(player.getUUID());
                    releaseToSearch(tracking, level.getGameTime());
                    sendMessage(player, Message.SITE_QUIET, 0);
                } else {
                    data.setSalvageTracker(player.getUUID(), result.center());
                    enterSalvage(tracking, result.center(), level.getGameTime());
                    sendRemainingMessage(player, tracking, result.remaining().size(), level.getGameTime());
                }
            }
        }
        if (result.remaining().isEmpty()) {
            data.clearSalvageTrackersAt(result.center());
            for (Map.Entry<UUID, Tracking> entry : TRACKING.entrySet()) {
                Tracking tracking = entry.getValue();
                if (tracking.mode != Mode.SALVAGE || tracking.centerPos != result.center().asLong()) continue;
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null) sendMessage(player, Message.SITE_QUIET, 0);
                releaseToSearch(tracking, level.getGameTime());
            }
        }
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Tracking>> iterator = TRACKING.entrySet().iterator();
        while (iterator.hasNext()) {
            if (server.getPlayerList().getPlayer(iterator.next().getKey()) == null) iterator.remove();
        }
        LAST_RENDER_STATES.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) tickPlayer(player);
    }

    private static void tickPlayer(ServerPlayer player) {
        if (!hasCompass(player) || !player.level().dimension().equals(Level.OVERWORLD)) {
            TRACKING.remove(player.getUUID());
            LAST_RENDER_STATES.remove(player.getUUID());
            if (player.level().dimension().equals(Level.OVERWORLD)) {
                BattlefieldSavedData1211.get((ServerLevel)player.level()).clearSalvageTracker(player.getUUID());
            }
            return;
        }

        ServerLevel level = (ServerLevel)player.level();
        BattlefieldSavedData1211 data = BattlefieldSavedData1211.get(level);
        long now = level.getGameTime();
        Tracking tracking = TRACKING.computeIfAbsent(player.getUUID(), ignored -> new Tracking(now));

        if (tracking.mode != Mode.SALVAGE) restoreSalvageIfValid(player, data, tracking, now);
        if (tracking.mode == Mode.SALVAGE) {
            tickSalvage(player, data, tracking, now);
            return;
        }

        BattlefieldSavedData1211.ActiveSite site = tracking.centerPos == 0L
                ? null : data.findActiveByCenter(tracking.centerPos);
        if (site != null && horizontalDistance(player.position(), site.center().getCenter()) > SEARCH_RADIUS) {
            site = null;
            releaseToSearch(tracking, now);
        }
        if (site == null && tracking.centerPos != 0L) releaseToSearch(tracking, now);

        if (site == null && now >= tracking.nextSearch) {
            site = data.nearestActive(player.blockPosition(), SEARCH_RADIUS);
            tracking.nextSearch = now + SEARCH_INTERVAL;
            if (site != null) {
                tracking.centerPos = site.center().asLong();
                tracking.mode = Mode.OUTSIDE;
                tracking.noTargetNotified = false;
                data.clearSalvageTracker(player.getUUID());
            }
        }

        if (site == null) {
            updateOutsideDetection(player, tracking, false, now);
            syncRenderState(player, new RenderState(EchoCompassItem1211.MODE_NO_TARGET, 0L));
            if (!tracking.noTargetNotified) {
                sendMessage(player, Message.NO_NEARBY_SITE, 0);
                tracking.noTargetNotified = true;
            }
            return;
        }

        double centerDistance = horizontalDistance(player.position(), site.center().getCenter());
        updateOutsideDetection(player, tracking, centerDistance <= OUTSIDE_SOUND_RADIUS, now);
        if (tracking.mode == Mode.INNER && centerDistance > INNER_RELEASE_RADIUS) tracking.mode = Mode.OUTSIDE;
        else if (tracking.mode == Mode.OUTSIDE && centerDistance <= INNER_ENTER_RADIUS) tracking.mode = Mode.INNER;

        if (tracking.mode == Mode.INNER) {
            syncRenderState(player, new RenderState(EchoCompassItem1211.MODE_INNER, site.relic().asLong()));
            playDirectionalEcho(player, tracking, site.relic(), now);
        } else {
            syncRenderState(player, new RenderState(EchoCompassItem1211.MODE_OUTSIDE, site.center().asLong()));
            playOutsideReminder(player, tracking, centerDistance, now);
        }
    }

    private static void restoreSalvageIfValid(ServerPlayer player, BattlefieldSavedData1211 data,
                                              Tracking tracking, long now) {
        OptionalLong savedCenter = data.salvageCenter(player.getUUID());
        if (savedCenter.isEmpty()) return;
        BattlefieldSavedData1211.SalvageSite salvage = data.findSalvageByCenter(savedCenter.getAsLong());
        if (salvage == null || horizontalDistance(player.position(), salvage.center().getCenter()) > INNER_RELEASE_RADIUS) {
            data.clearSalvageTracker(player.getUUID());
            return;
        }
        enterSalvage(tracking, salvage.center(), now);
    }

    private static void tickSalvage(ServerPlayer player, BattlefieldSavedData1211 data,
                                    Tracking tracking, long now) {
        BattlefieldSavedData1211.SalvageSite salvage = data.findSalvageByCenter(tracking.centerPos);
        if (salvage == null || salvage.remaining().isEmpty()) {
            data.clearSalvageTracker(player.getUUID());
            sendMessage(player, Message.SITE_QUIET, 0);
            releaseToSearch(tracking, now);
            tickPlayer(player);
            return;
        }

        double centerDistance = horizontalDistance(player.position(), salvage.center().getCenter());
        if (centerDistance > INNER_RELEASE_RADIUS) {
            if (tracking.outOfRangeSince == 0L) tracking.outOfRangeSince = now;
            else if (now - tracking.outOfRangeSince >= SALVAGE_RELEASE_DELAY) {
                data.clearSalvageTracker(player.getUUID());
                releaseToSearch(tracking, now);
                tickPlayer(player);
                return;
            }
        } else {
            tracking.outOfRangeSince = 0L;
        }

        BlockPos target = lockedOrNearest(player, tracking, salvage.remaining());
        syncRenderState(player, target == null
                ? new RenderState(EchoCompassItem1211.MODE_NO_TARGET, 0L)
                : new RenderState(EchoCompassItem1211.MODE_SALVAGE, target.asLong()));
        playDirectionalEcho(player, tracking, target, now);
        if (now + SALVAGE_MESSAGE_DISPLAY_DURATION <= tracking.messageWindowEnds
                && (tracking.lastRemainingCount != salvage.remaining().size() || now >= tracking.nextMessage)) {
            sendRemainingMessage(player, tracking, salvage.remaining().size(), now);
        }
    }

    private static BlockPos lockedOrNearest(ServerPlayer player, Tracking tracking, List<BlockPos> remaining) {
        if (tracking.lockedTarget != 0L) {
            for (BlockPos pos : remaining) if (pos.asLong() == tracking.lockedTarget) return pos;
        }
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : remaining) {
            double distance = player.position().distanceToSqr(pos.getCenter());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = pos;
            }
        }
        tracking.lockedTarget = nearest == null ? 0L : nearest.asLong();
        return nearest;
    }

    private static void sendRemainingMessage(ServerPlayer player, Tracking tracking, int count, long now) {
        sendMessage(player, Message.REMAINING_ECHOES, count);
        tracking.lastRemainingCount = count;
        tracking.nextMessage = now + SALVAGE_MESSAGE_INTERVAL;
    }

    private static void playOutsideReminder(ServerPlayer player, Tracking tracking, double distance, long now) {
        if (distance > OUTSIDE_SOUND_RADIUS) return;
        double closeness = Math.clamp(1.0 - distance / OUTSIDE_SOUND_RADIUS, 0.0, 1.0);
        long interval = Math.round(80.0 + (1.0 - closeness) * 160.0);
        acceleratePendingPulse(tracking, interval);
        if (now < tracking.nextPulse) return;
        if (!selectedSoundCompass(player).isEmpty()) {
            player.connection.send(new ClientboundSoundPacket(HUM, SoundSource.AMBIENT,
                    player.getX(), player.getY() + 1.0, player.getZ(), (float)(0.14 + closeness * 0.12),
                    (float)(0.66 + closeness * 0.18), player.getRandom().nextLong()));
        }
        scheduleNextPulse(tracking, now, interval);
    }

    private static void updateOutsideDetection(ServerPlayer player, Tracking tracking,
                                               boolean withinDetectionRange, long now) {
        if (withinDetectionRange) {
            tracking.outsideDetectionLostSince = -1L;
            if (!tracking.outsideDetectionLatched) {
                tracking.outsideDetectionLatched = true;
                sendMessage(player, Message.ECHO_DETECTED, 0);
            }
            return;
        }
        if (!tracking.outsideDetectionLatched) return;
        if (tracking.outsideDetectionLostSince < 0L) {
            tracking.outsideDetectionLostSince = now;
            return;
        }
        if (now - tracking.outsideDetectionLostSince >= OUTSIDE_DETECTION_REARM_DELAY) {
            tracking.outsideDetectionLatched = false;
            tracking.outsideDetectionLostSince = -1L;
        }
    }

    private static void playDirectionalEcho(ServerPlayer player, Tracking tracking, BlockPos target, long now) {
        if (target == null) return;
        Vec3 delta = target.getCenter().subtract(player.position());
        double distance = delta.length();
        long interval = directionalPulseInterval(distance);
        acceleratePendingPulse(tracking, interval);
        if (now < tracking.nextPulse) return;
        Vec3 direction = delta.multiply(1.0, 0.25, 1.0);
        if (direction.lengthSqr() < 1.0E-6) direction = player.getLookAngle();
        direction = direction.normalize();
        double closeness = Math.clamp(1.0 - distance / 48.0, 0.0, 1.0);
        double response = smoothstep(closeness);
        Vec3 source = player.position().add(direction.scale(4.0 + (1.0 - closeness) * 3.0)).add(0.0, 1.0, 0.0);
        player.connection.send(new ClientboundSoundPacket(HUM, SoundSource.AMBIENT,
                source.x, source.y, source.z, (float)(0.20 + response * 0.60),
                (float)(0.64 + response * 0.42), player.getRandom().nextLong()));
        scheduleNextPulse(tracking, now, interval);
    }

    private static void acceleratePendingPulse(Tracking tracking, long desiredInterval) {
        long acceleratedAt = tracking.lastPulseAt + desiredInterval;
        if (acceleratedAt < tracking.nextPulse) tracking.nextPulse = acceleratedAt;
    }

    private static void scheduleNextPulse(Tracking tracking, long now, long interval) {
        tracking.lastPulseAt = now;
        tracking.nextPulse = now + interval;
    }

    private static long directionalPulseInterval(double distance) {
        double value = Math.clamp(distance, 0.0, 48.0);
        if (value <= 4.0) return stagedInterval(value, 0.0, 4.0, 14.0, 18.0);
        if (value <= 10.0) return stagedInterval(value, 4.0, 10.0, 18.0, 24.0);
        if (value <= 20.0) return stagedInterval(value, 10.0, 20.0, 24.0, 36.0);
        if (value <= 32.0) return stagedInterval(value, 20.0, 32.0, 36.0, 52.0);
        return stagedInterval(value, 32.0, 48.0, 52.0, 72.0);
    }

    private static long stagedInterval(double value, double minimum, double maximum,
                                       double startTicks, double endTicks) {
        double progress = Math.clamp((value - minimum) / (maximum - minimum), 0.0, 1.0);
        return Math.round(startTicks + (endTicks - startTicks) * smoothstep(progress));
    }

    private static double smoothstep(double value) {
        double clamped = Math.clamp(value, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private static void syncRenderState(ServerPlayer player, RenderState state) {
        if (state.equals(LAST_RENDER_STATES.get(player.getUUID()))) return;
        for (ItemStack stack : compasses(player)) EchoCompassItem1211.writeTracking(stack, state.mode, state.target);
        LAST_RENDER_STATES.put(player.getUUID(), state);
    }

    private static boolean hasCompass(ServerPlayer player) {
        return !compasses(player).isEmpty();
    }

    private static List<ItemStack> compasses(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        if (player.containerMenu.getCarried().is(ModContent1211.ECHO_COMPASS)) {
            result.add(player.containerMenu.getCarried());
        }
        if (player.getOffhandItem().is(ModContent1211.ECHO_COMPASS)) result.add(player.getOffhandItem());
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Math.min(36, inventory.getContainerSize()); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModContent1211.ECHO_COMPASS)) result.add(stack);
        }
        return result;
    }

    private static ItemStack selectedSoundCompass(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (EchoCompassItem1211.isOutsideSoundEnabled(main)) return main;
        ItemStack offhand = player.getOffhandItem();
        if (EchoCompassItem1211.isOutsideSoundEnabled(offhand)) return offhand;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (EchoCompassItem1211.isOutsideSoundEnabled(stack)) return stack;
        }
        for (int slot = 9; slot < Math.min(36, inventory.getContainerSize()); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (EchoCompassItem1211.isOutsideSoundEnabled(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void enterSalvage(Tracking tracking, BlockPos center, long now) {
        tracking.mode = Mode.SALVAGE;
        tracking.centerPos = center.asLong();
        tracking.lockedTarget = 0L;
        tracking.outOfRangeSince = 0L;
        tracking.lastPulseAt = now;
        tracking.nextPulse = now;
        tracking.nextMessage = now;
        tracking.messageWindowEnds = now + SALVAGE_MESSAGE_WINDOW;
        tracking.lastRemainingCount = -1;
        tracking.noTargetNotified = false;
    }

    private static void releaseToSearch(Tracking tracking, long now) {
        tracking.mode = Mode.DORMANT;
        tracking.centerPos = 0L;
        tracking.lockedTarget = 0L;
        tracking.outOfRangeSince = 0L;
        tracking.nextSearch = now;
        tracking.lastPulseAt = now;
        tracking.nextPulse = now;
        tracking.nextMessage = now;
        tracking.messageWindowEnds = 0L;
        tracking.lastRemainingCount = -1;
        tracking.noTargetNotified = false;
    }

    private static void sendMessage(ServerPlayer player, Message message, int value) {
        Component component = switch (message) {
            case SOUND_ENABLED -> Component.translatable("message.echo_warrior.echo_compass.sound_enabled");
            case SOUND_DISABLED -> Component.translatable("message.echo_warrior.echo_compass.sound_disabled");
            case ECHO_DETECTED -> Component.translatable("message.echo_warrior.echo_compass.echo_detected");
            case NO_NEARBY_SITE -> Component.translatable("message.echo_warrior.echo_compass.no_nearby_site");
            case REMAINING_ECHOES -> Component.translatable("message.echo_warrior.echo_compass.remaining_echoes", value);
            case SITE_QUIET -> Component.translatable("message.echo_warrior.echo_compass.site_quiet");
        };
        player.displayClientMessage(component, true);
    }

    public static void sendToggleMessage(ServerPlayer player, boolean enabled) {
        sendMessage(player, enabled ? Message.SOUND_ENABLED : Message.SOUND_DISABLED, 0);
    }

    private static double horizontalDistance(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private enum Message { SOUND_ENABLED, SOUND_DISABLED, ECHO_DETECTED, NO_NEARBY_SITE, REMAINING_ECHOES, SITE_QUIET }
    private enum Mode { DORMANT, OUTSIDE, INNER, SALVAGE }
    private record RenderState(int mode, long target) { }

    private static final class Tracking {
        private Mode mode = Mode.DORMANT;
        private long centerPos;
        private long lockedTarget;
        private long nextSearch;
        private long lastPulseAt;
        private long nextPulse;
        private long nextMessage;
        private long messageWindowEnds;
        private long outOfRangeSince;
        private long outsideDetectionLostSince = -1L;
        private int lastRemainingCount = -1;
        private boolean noTargetNotified;
        private boolean outsideDetectionLatched;

        private Tracking(long now) {
            this.nextSearch = now;
            this.lastPulseAt = now;
            this.nextPulse = now;
            this.nextMessage = now;
        }
    }
}
