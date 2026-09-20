package com.yuriscat.echowarrior.compat.test;

import com.mojang.authlib.GameProfile;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.entity.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Captures real server-to-observer packets, not a counter in the effect implementation. */
public final class DepartureEffectsSelfTest1211 {
    private DepartureEffectsSelfTest1211() {}

    public static void run(MinecraftServer server) {
        ServerLevel level = server.overworld();
        var packets = new ArrayList<Packet<?>>();
        var profile = new GameProfile(UUID.randomUUID(), "DepartureTest");
        var observer = new ServerPlayer(server, level, profile, net.minecraft.server.level.ClientInformation.createDefault());
        observer.connection = new PacketRecorder(server, observer, packets);
        // The smoke world's spawn is not necessarily in chunk 0,0.
        var origin = level.getSharedSpawnPos();
        level.getChunk(origin);
        observer.moveTo(origin.getX() + .5, Math.min(level.getMaxBuildHeight() - 8, origin.getY() + 5),
                origin.getZ() + .5, 0, 0);
        var fixtures = new ArrayList<Entity>();
        var bindings = new ArrayList<UUID>();
        // Synchronous, isolated-server fixture: no login, chunks, playerdata or network socket.
        int heroes = 0;
        try {
            level.players().add(observer);
            for (var type : ModContent1211.entityTypes().values()) {
                if (!(type.create(level) instanceof EchoWarriorEntity1211)) continue;
                heroes++;
                for (int mode = 0; mode < 4; mode++) {
                    var echo = (EchoWarriorEntity1211)type.create(level);
                    var living = echo.livingEntity();
                    fixtures.add(living);
                    living.moveTo(observer.getX() + 1, observer.getY(), observer.getZ(), 0, 0);
                    living.setNoGravity(true);
                    ((Mob)living).setNoAi(true);
                    var stack = new ItemStack(ModContent1211.ECHO_SUMMONER);
                    var binding = EchoBindingSystem1211.synchronize(level, stack);
                    bindings.add(binding.summonerId());
                    binding.activate(observer.getUUID(), living.getUUID(), level.dimension().location().toString(),
                            living.getX(), living.getY(), living.getZ(), living.getHealth());
                    echo.bindTo(observer, binding.summonerId(), binding.generation());
                    require(level.addFreshEntity(living), "fixture spawn");
                    require(level.getEntity(living.getUUID()) == living && living.isAlive(), "fixture in a loaded chunk");

                    packets.clear();
                    if (mode == 0) {
                        require(EchoBindingSystem1211.dismiss(level, stack), "item dismissal");
                    } else if (mode == 1) {
                        require(EchoBindingSystem1211.dismiss(server, binding.summonerId()), "id dismissal");
                    } else if (mode == 2) {
                        require(EchoBindingSystem1211.destroySummoner(level, binding.summonerId()), "destroy summoner");
                    } else {
                        living.setHealth(0);
                        living.die(level.damageSources().generic());
                        require(!binding.active(), "death immediately invalidates binding");
                        require(EchoBindingSystem1211.validateAndTrack(echo), "dying entity retains death presentation");
                        for (int tick = 0; tick < 19; tick++) living.tick();
                        require(!living.isRemoved() && soulPackets(packets).isEmpty(), "keep death pose before final tick");
                        living.tick();
                    }
                    require(living.isRemoved(), "entity removed for mode " + mode);
                    var souls = soulPackets(packets);
                    require(souls.size() == 1 && souls.get(0).getCount() == 24,
                            "one 24-soul departure packet for " + echo.heroType() + " mode=" + mode);
                    var burst = souls.get(0);
                    require(Math.abs(burst.getX() - living.getX()) < .01
                                    && Math.abs(burst.getY() - living.getY() - 1) < .01
                                    && Math.abs(burst.getZ() - living.getZ()) < .01,
                            "departure particles centered on entity");
                    packets.clear();
                    echo.dismiss();
                    require(soulPackets(packets).isEmpty(), "repeated removal must not repeat particles");
                }
            }
            require(heroes == 5, "cover all five heroes");
            var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
            fixtures.add(pig);
            pig.moveTo(observer.getX() + 1, observer.getY(), observer.getZ(), 0, 0);
            pig.setNoGravity(true);
            pig.setNoAi(true);
            require(level.addFreshEntity(pig), "vanilla fixture");
            packets.clear();
            pig.setHealth(0);
            pig.die(level.damageSources().generic());
            for (int tick = 0; tick < 20; tick++) pig.tick();
            require(soulPackets(packets).isEmpty(), "vanilla deaths unchanged");
            // Silent rollback/cleanup must not become a gameplay dismissal.
            var silent = ModContent1211.ROMAN_LEGIONARY_ECHO.create(level);
            fixtures.add(silent);
            silent.moveTo(observer.getX() + 1, observer.getY(), observer.getZ(), 0, 0);
            require(level.addFreshEntity(silent), "rollback fixture");
            packets.clear();
            silent.discard();
            require(soulPackets(packets).isEmpty(), "silent discard remains silent");
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info(
                    "[Compat1211] DEPARTURE EFFECTS SELFTEST PASSED heroes=5 modes=item-id-destroy-death packets=24-soul once=true vanilla=unchanged");
        } finally {
            level.players().remove(observer);
            fixtures.forEach(entity -> { if (entity != null && !entity.isRemoved()) entity.discard(); });
            bindings.forEach(id -> EchoBindingSavedData1211.get(server).remove(id));
        }
    }

    private static List<ClientboundLevelParticlesPacket> soulPackets(List<Packet<?>> packets) {
        return packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance)
                .map(ClientboundLevelParticlesPacket.class::cast)
                .filter(packet -> packet.getParticle().getType() == ParticleTypes.SOUL).toList();
    }

    private static final class PacketRecorder extends ServerGamePacketListenerImpl {
        private final List<Packet<?>> packets;
        PacketRecorder(MinecraftServer server, ServerPlayer observer, List<Packet<?>> packets) {
            super(server, new Connection(PacketFlow.SERVERBOUND), observer, net.minecraft.server.network.CommonListenerCookie.createInitial(observer.getGameProfile(), false));
            this.packets = packets;
        }
        @Override public void send(Packet<?> packet) { packets.add(packet); }
    }

    private static void require(boolean valid, String detail) {
        if (!valid) throw new IllegalStateException("Departure effects: " + detail);
    }
}
