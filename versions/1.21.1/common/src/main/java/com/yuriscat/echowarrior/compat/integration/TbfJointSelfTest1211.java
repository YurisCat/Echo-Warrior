package com.yuriscat.echowarrior.compat.integration;

import com.yuriscat.echowarrior.compat.binding.*;
import com.yuriscat.echowarrior.compat.item.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.nbt.CompoundTag;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Runs the installed TBF handlers and transformed classes, with a recording server connection. */
public final class TbfJointSelfTest1211 {
    private static final String ROOT = "com.whidte.trulybestfriends.";
    public static void run(MinecraftServer server, ServerPlayer player, EchoBindingSavedData1211.Binding binding, UUID id) {
        if (!TbfBridge1211.enabled()) {
            if (Boolean.getBoolean("echoWarrior.tbfRequired"))
                throw new IllegalStateException("TBF joint test required, but adapter did not enable");
            return;
        }
        List<Packet<?>> outgoing = new ArrayList<>();
        Connection connection = new RecordingConnection(outgoing);
        player.connection = new Recorder(server, connection, player, outgoing);

        try {
            Object context = context(player, connection);
            TbfBridge1211.refresh(player, true);
            Path directory = (Path)TbfBridge1211.call("network.PetIOUtil", "getOwnerDir", player);
            Path file = directory.resolve(id + ".nbt");
            check(Files.isRegularFile(file), "stable display entry created");
            int beforeList = outgoing.size();
            packet("RequestPetDataPacket", context, 0, null);
            check(outgoing.size()>beforeList,"actual full-list request sends client snapshot");
            String color = (String)TbfBridge1211.call("network.PetTeamData", "colorAt", 0);
            TbfBridge1211.call("network.PetTeamData", "setMember", directory, color, 1, id);
            int cost = SummonerFuel1211.summonCost(binding.relic());
            for (int i=0; i<100; i++) {
                EchoExternalCompanion1211.dismiss(player, id);
                binding.setFuel(300);
                packet("SummonPetPacket", context, id);
                check(binding.active() && binding.fuel()==300-cost, "packet summon charged once, cycle " + i);
                var echo = EchoBindingSystem1211.findLoaded(server,binding.spiritId());
                echo.livingEntity().setHealth(7);
                packet("TeleportPetToPlayerPacket", context, id);
                check(binding.fuel()==300-cost && echo.livingEntity().getHealth()==7, "packet recall preserves life/fuel");
                check(id.equals(EchoExternalCompanion1211.entryId(binding)), "stable logical identity");
                CompoundTag snapshot = read(file);
                check(!snapshot.getBoolean("Lost") && !snapshot.getBoolean("Dead") && !snapshot.getBoolean("Recalled"), "active state");
                check(Boolean.FALSE.equals(TbfBridge1211.call("network.RequestPetDataPacket", "shouldMarkLost", snapshot, false)), "unloaded is not lost");
                check(TbfBridge1211.call("network.PetEntitySnapshot", "restore", snapshot, id, player.serverLevel())==null, "external snapshot never restored");
                var team = (CompoundTag)TbfBridge1211.call("network.PetTeamData", "teamData", directory);
                check(((List<?>)TbfBridge1211.call("network.PetTeamData", "memberUuids", team, color)).contains(id), "team retains stable entry");
            }
            try (var files = Files.list(directory)) {
                check(files.filter(p -> p.getFileName().toString().endsWith(".nbt"))
                        .filter(p -> !p.getFileName().toString().equals("team.nbt"))
                        .filter(p -> read(p).hasUUID(TbfBridge1211.MARKER)).count()==1, "no stale incarnations");
            }
            packet("RecallPetPacket", context, id);
            check(!binding.active(), "TBF recall dismisses core");
            binding.setFuel(300);
            packet("SummonTeamPacket", context, 0);
            check(binding.active() && binding.fuel()==300-cost, "team summon reaches core");
            var echo = EchoBindingSystem1211.findLoaded(server,binding.spiritId());
            echo.livingEntity().setHealth(7);
            packet("HealPetPacket", context, id, false);
            check(echo.livingEntity().getHealth()==7, "third-party healing cannot bypass core");
            check(TbfBridge1211.call("trulybestfriends", "getCompatOwnerUUID", echo.livingEntity())==null, "physical UUID excluded from automatic tracking/death capture");
            packet("DeletePetDataPacket", context, id);
            check(!Files.exists(file) && binding.active(), "untracking keeps current Echo, no restored duplicate");
            TbfBridge1211.refresh(player,true);
            check(!Files.exists(file), "untracking persists across refresh");
            check(TbfBridge1211.call("trulybestfriends", "tryForceLoadPet", echo.livingEntity(), player, player.serverLevel()).toString().equals("OK"), "force-track restores stable entry");
            check(Files.exists(file), "retracked entry");
            var team = (CompoundTag)TbfBridge1211.call("network.PetTeamData", "teamData", directory);
            check(!((List<?>)TbfBridge1211.call("network.PetTeamData", "memberUuids", team, color)).contains(id), "untracking also cleaned team");

            // Ordinary pet dispatch must still reach the original TBF handler.
            UUID ordinary = UUID.randomUUID();
            Path ordinaryFile = directory.resolve(ordinary+".nbt");
            CompoundTag ordinaryNbt = new CompoundTag();
            ordinaryNbt.putString("id","minecraft:wolf");
            ordinaryNbt.putString("EntityType","minecraft:wolf");
            ordinaryNbt.putUUID("UUID", ordinary);
            ordinaryNbt.putString("OwnerUUID",player.getUUID().toString());
            ordinaryNbt.putBoolean("Recalled",true);
            try {
                TbfBridge1211.call("network.NbtFileIO","writeCompressed",ordinaryNbt,ordinaryFile.toFile());
                packet("SetPriorityPacket", context, ordinary, 5);
                check(read(ordinaryFile).getInt("Priority")==5, "ordinary pet packet passes through");
            } finally { Files.deleteIfExists(ordinaryFile); }
            check(!outgoing.isEmpty(), "real TBF client update packets emitted");
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info("[TbfJointSelfTest] PASS installed handlers, 100 cycles, one entry, teams, snapshot guard, untracking, ordinary pets");
        } catch (ReflectiveOperationException | java.io.IOException e) {
            throw new IllegalStateException("TBF joint fixture failed",e);
        } finally {
            EchoExternalCompanion1211.dismiss(player,id);
            // Remove only the positively identified entry through the real untracking operation.
            try { packet("DeletePetDataPacket", context(player,connection), id); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
    }
    private static final class RecordingConnection extends Connection {
        private final List<Packet<?>> outgoing;
        RecordingConnection(List<Packet<?>> outgoing) { super(PacketFlow.SERVERBOUND); this.outgoing=outgoing; }
        @Override public boolean isConnected() { return true; }
        @Override public void send(Packet<?> packet) { outgoing.add(packet); }
        @Override public void send(Packet<?> packet, net.minecraft.network.PacketSendListener listener) { outgoing.add(packet); }
        @Override public void send(Packet<?> packet, net.minecraft.network.PacketSendListener listener, boolean flush) { outgoing.add(packet); }
    }
    private static final class Recorder extends ServerGamePacketListenerImpl {
        private final List<Packet<?>> outgoing;
        Recorder(MinecraftServer server, Connection connection, ServerPlayer player, List<Packet<?>> outgoing) {
            super(server, connection, player, net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
            this.outgoing=outgoing;
        }
        @Override public void send(Packet<?> packet) { outgoing.add(packet); }
    }
    private static CompoundTag read(Path path) {
        return (CompoundTag)TbfBridge1211.call("network.NbtFileIO","readCompressed",path.toFile());
    }
    private static void packet(String name, Object context, Object... args) throws ReflectiveOperationException {
        Class<?> type = Class.forName(ROOT+"network."+name);
        for (Constructor<?> constructor : type.getConstructors()) {
            if (constructor.getParameterCount()!=args.length) continue;
            Object message;
            try { message=constructor.newInstance(args); }
            catch (IllegalArgumentException wrongOverload) { continue; }
            TbfBridge1211.call("network."+name,"handle",message,context);
            return;
        }
        throw new NoSuchMethodException(name+" packet constructor");
    }
    private static Object context(ServerPlayer player, Connection connection) throws ReflectiveOperationException {
        Class<?> type=Class.forName("net.neoforged.neoforge.network.handling.IPayloadContext");
        return Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)-> {
            return switch(method.getName()) {
                case "player" -> player;
                case "enqueueWork" -> { ((Runnable)args[0]).run(); yield CompletableFuture.completedFuture(null); }
                case "toString" -> "Echo TBF test context";
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });
    }
    private static void check(boolean valid,String label) {
        if(!valid) throw new IllegalStateException("TBF joint test: "+label);
    }
}
