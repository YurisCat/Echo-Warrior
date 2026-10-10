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
public final class TbfJointSelfTest1201 {
    private static final String ROOT = "com.whidte.trulybestfriends.";
    public static void run(MinecraftServer server, ServerPlayer player, EchoBindingSavedData1201.Binding binding, UUID id) {
        if (!TbfBridge1201.enabled()) {
            if (Boolean.getBoolean("echoWarrior.tbfRequired"))
                throw new IllegalStateException("TBF joint test required, but adapter did not enable");
            return;
        }
        List<Packet<?>> outgoing = new ArrayList<>();
        Connection connection = new RecordingConnection(outgoing);
        player.connection = new Recorder(server, connection, player, outgoing);

        try {
            Object context = context(player, connection);
            TbfBridge1201.refresh(player, true);
            Path directory = (Path)TbfBridge1201.call("network.PetIOUtil", "getOwnerDir", player);
            Path file = directory.resolve(id + ".nbt");
            check(Files.isRegularFile(file), "stable display entry created");
            int beforeList = outgoing.size();
            packet("RequestPetDataPacket", context, 0, null);
            check(outgoing.size()>beforeList,"actual full-list request sends client snapshot");
            String color = (String)TbfBridge1201.call("network.PetTeamData", "colorAt", 0);
            TbfBridge1201.call("network.PetTeamData", "setMember", directory, color, 1, id);
            TbfBridge1201.call("network.PetTeamData", "setSelectedTeam", directory, color);
            TbfBridge1201.call("network.PetTeamData", "setLastSummon", directory, color, 1);
            CompoundTag teamOnDisk = read(directory.resolve("team.nbt"));
            check(teamOnDisk.getString("SelectedTeam").equals(color)
                    && teamOnDisk.getCompound("LastSummon").getInt("Slot") == 1, "team selection and wheel slot persisted");
            int cost = SummonerFuel1201.summonCost(binding.relic());
            for (int i=0; i<100; i++) {
                EchoExternalCompanion1201.dismiss(player, id);
                binding.setFuel(300);
                packet("SummonPetPacket", context, id);
                check(binding.active() && binding.fuel()==300-cost, "packet summon charged once, cycle " + i);
                var echo = EchoBindingSystem1201.findLoaded(server,binding.spiritId());
                echo.livingEntity().setHealth(7);
                packet("TeleportPetToPlayerPacket", context, id);
                check(binding.fuel()==300-cost && echo.livingEntity().getHealth()==7, "packet recall preserves life/fuel");
                check(id.equals(EchoExternalCompanion1201.entryId(binding)), "stable logical identity");
                CompoundTag snapshot = read(file);
                check(!snapshot.getBoolean("Lost") && !snapshot.getBoolean("Dead") && !snapshot.getBoolean("Recalled"), "active state");
                check(Boolean.FALSE.equals(TbfBridge1201.call("network.RequestPetDataPacket", "shouldMarkLost", snapshot, false)), "unloaded is not lost");
                check(TbfBridge1201.call("network.PetEntitySnapshot", "restore", snapshot, id, player.serverLevel())==null, "external snapshot never restored");
                if (TbfMixinPlugin1201.api().ownerHintRestore())
                    check(TbfBridge1201.call("network.PetEntitySnapshot", "restore", snapshot, id, player.serverLevel(),
                            player.getUUID()) == null, "owner-hint overload cannot restore an external snapshot");
                var team = (CompoundTag)TbfBridge1201.call("network.PetTeamData", "teamData", directory);
                check(((List<?>)TbfBridge1201.call("network.PetTeamData", "memberUuids", team, color)).contains(id), "team retains stable entry");
            }
            try (var files = Files.list(directory)) {
                check(files.filter(p -> p.getFileName().toString().endsWith(".nbt"))
                        .filter(p -> !p.getFileName().toString().equals("team.nbt"))
                        .filter(p -> read(p).hasUUID(TbfBridge1201.MARKER)).count()==1, "no stale incarnations");
            }
            packet("RecallPetPacket", context, id);
            check(!binding.active(), "TBF recall dismisses core");
            binding.setFuel(300);
            packet("SummonTeamPacket", context, 0);
            check(binding.active() && binding.fuel()==300-cost, "team summon reaches core");
            verifyPresenceProbe(server, player, binding, id, context, directory, color);
            packet("AreaRecallPacket", context, 16);
            check(!binding.active(), "area recall reaches authoritative Echo transaction");
            binding.setFuel(300);
            packet("SummonTeamPacket", context, 0);
            check(binding.active(), "team survives area recall");
            var echo = EchoBindingSystem1201.findLoaded(server,binding.spiritId());
            echo.livingEntity().setHealth(7);
            packet("HealPetPacket", context, id, false);
            check(echo.livingEntity().getHealth()==7, "third-party healing cannot bypass core");
            check(TbfBridge1201.call("trulybestfriends", "getCompatOwnerUUID", echo.livingEntity())==null, "physical UUID excluded from automatic tracking/death capture");
            packet("DeletePetDataPacket", context, id);
            check(!Files.exists(file) && binding.active(), "untracking keeps current Echo, no restored duplicate");
            TbfBridge1201.refresh(player,true);
            check(!Files.exists(file), "untracking persists across refresh");
            check(TbfBridge1201.call("trulybestfriends", "tryForceLoadPet", echo.livingEntity(), player, player.serverLevel()).toString().equals("OK"), "force-track restores stable entry");
            check(Files.exists(file), "retracked entry");
            var team = (CompoundTag)TbfBridge1201.call("network.PetTeamData", "teamData", directory);
            check(!((List<?>)TbfBridge1201.call("network.PetTeamData", "memberUuids", team, color)).contains(id), "untracking also cleaned team");

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
                TbfBridge1201.call("network.NbtFileIO","writeCompressed",ordinaryNbt,ordinaryFile.toFile());
                packet("SetPriorityPacket", context, ordinary, 5);
                check(read(ordinaryFile).getInt("Priority")==5, "ordinary pet packet passes through");
            } finally { Files.deleteIfExists(ordinaryFile); }
            check(!outgoing.isEmpty(), "real TBF client update packets emitted");
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info("[TbfJointSelfTest] PASS installed handlers, 100 cycles, one entry, team persistence, all snapshot overloads, area recall, untracking, ordinary pets");
        } catch (ReflectiveOperationException | java.io.IOException e) {
            throw new IllegalStateException("TBF joint fixture failed",e);
        } finally {
            EchoExternalCompanion1201.dismiss(player,id);
            // Remove only the positively identified entry through the real untracking operation.
            try { packet("DeletePetDataPacket", context(player,connection), id); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
    }
    private static void verifyPresenceProbe(MinecraftServer server, ServerPlayer player,
            EchoBindingSavedData1201.Binding binding, UUID id, Object context, Path directory, String color)
            throws ReflectiveOperationException, java.io.IOException {
        if (!TbfMixinPlugin1201.api().presenceProbe()) return;
        Class<?> config = Class.forName(ROOT + "Config");
        @SuppressWarnings("unchecked") Set<String> whitelist = (Set<String>)config.getField("presenceProbeWhitelist").get(null);
        Set<String> previous = new HashSet<>(whitelist);
        // The normal fixture is not logged in. Only register it in the UUID lookup for probe ticks;
        // never create a second client or run the real login/world-save flow.
        Map<UUID, ServerPlayer> players = null;
        for (Field candidate : net.minecraft.server.players.PlayerList.class.getDeclaredFields()) {
            if (candidate.getGenericType() instanceof ParameterizedType generic
                    && generic.getRawType() == Map.class
                    && Arrays.equals(generic.getActualTypeArguments(), new Type[]{UUID.class, ServerPlayer.class})) {
                candidate.setAccessible(true);
                @SuppressWarnings("unchecked") Map<UUID, ServerPlayer> found = (Map<UUID, ServerPlayer>)candidate.get(server.getPlayerList());
                players = found;
                break;
            }
        }
        check(players != null && !players.containsKey(player.getUUID()), "isolated probe player lookup");
        players.put(player.getUUID(), player);
        UUID ordinary = UUID.randomUUID();
        Path ordinaryFile = directory.resolve(ordinary + ".nbt");
        UUID spiritBefore = binding.spiritId();
        try {
            whitelist.clear();
            whitelist.add("echo_warrior:*");
            whitelist.add("minecraft:wolf");
            CompoundTag echo = read(directory.resolve(id + ".nbt"));
            check(!id.equals(spiritBefore), "probe fixture really uses a logical UUID");
            check(Boolean.FALSE.equals(TbfBridge1201.call("network.PetPresenceProbe", "shouldProbe", player, id, echo)),
                    "whitelisted external entry skips physical UUID probing");
            var wolfEntity = net.minecraft.world.entity.EntityType.WOLF.create(player.serverLevel());
            check(wolfEntity != null, "ordinary wolf fixture created");
            wolfEntity.setUUID(ordinary);
            wolfEntity.setOwnerUUID(player.getUUID());
            wolfEntity.moveTo(player.getX(), player.getY(), player.getZ(), 0, 0);
            CompoundTag wolf = (CompoundTag)TbfBridge1201.call("network.PetEntitySnapshot", "capture",
                    wolfEntity, player.getUUID(), player.serverLevel());
            wolfEntity.discard();
            wolf.putBoolean("Recalled", false);
            check(Boolean.TRUE.equals(TbfBridge1201.call("network.PetPresenceProbe", "shouldProbe", player, ordinary, wolf)),
                    "ordinary whitelisted missing pet still probes");
            TbfBridge1201.call("network.NbtFileIO", "writeCompressed", wolf, ordinaryFile.toFile());
            TbfBridge1201.call("network.PetTeamData", "setMember", directory, color, 2, ordinary);
            check(server.getPlayerList().getPlayer(player.getUUID()) == player, "probe sees test owner");
            check(player.serverLevel().hasChunkAt(player.blockPosition()), "probe uses a loaded fixture chunk");
            packet("RequestPetDataPacket", context, 0, null);
            for (int tick = 0; tick < 25; tick++) TbfBridge1201.call("network.PetPresenceProbe", "tick", server);
            check(!Files.exists(ordinaryFile), "real probe cleans ordinary missing pet after confirmation");
            check(Files.isRegularFile(directory.resolve(id + ".nbt")) && binding.active()
                    && spiritBefore.equals(binding.spiritId()), "probe preserves external mirror and live incarnation");
            var team = (CompoundTag)TbfBridge1201.call("network.PetTeamData", "teamData", directory);
            var members = (List<?>)TbfBridge1201.call("network.PetTeamData", "memberUuids", team, color);
            check(members.contains(id) && !members.contains(ordinary), "probe preserves Echo team slot, prunes ordinary missing pet");
            var restored = TbfBridge1201.call("network.PetEntitySnapshot", "restore", wolf, ordinary, player.serverLevel(), player.getUUID());
            check(restored instanceof net.minecraft.world.entity.Entity, "ordinary owner-hint restoration still works");
            ((net.minecraft.world.entity.Entity)restored).discard();
            check(read(directory.resolve(id + ".nbt")).getString("TBF_OwnerUUID").equals(player.getUUID().toString()),
                    "new owner field written");
            org.slf4j.LoggerFactory.getLogger("echo_warrior").info("[TbfJointSelfTest] PASS presence probe: Echo wildcard protected, ordinary missing pet deleted, team and owner retained");
        } finally {
            TbfBridge1201.call("network.PetPresenceProbe", "clear");
            whitelist.clear();
            whitelist.addAll(previous);
            players.remove(player.getUUID());
            if (Files.isRegularFile(ordinaryFile)) TbfBridge1201.call("trulybestfriends", "deletePetData", player, ordinary);
        }
    }
    private static final class RecordingConnection extends Connection {
        private final List<Packet<?>> outgoing;
        RecordingConnection(List<Packet<?>> outgoing) { super(PacketFlow.SERVERBOUND); this.outgoing=outgoing; }
        @Override public boolean isConnected() { return true; }
        @Override public void send(Packet<?> packet) { outgoing.add(packet); }
        @Override public void send(Packet<?> packet, net.minecraft.network.PacketSendListener listener) { outgoing.add(packet); }
    }
    private static final class Recorder extends ServerGamePacketListenerImpl {
        private final List<Packet<?>> outgoing;
        Recorder(MinecraftServer server, Connection connection, ServerPlayer player, List<Packet<?>> outgoing) {
            super(server, connection, player);
            this.outgoing=outgoing;
        }
        @Override public void send(Packet<?> packet) { outgoing.add(packet); }
    }
    private static CompoundTag read(Path path) {
        return (CompoundTag)TbfBridge1201.call("network.NbtFileIO","readCompressed",path.toFile());
    }
    private static void packet(String name, Object context, Object... args) throws ReflectiveOperationException {
        Class<?> type = Class.forName(ROOT+"network."+name);
        for (Constructor<?> constructor : type.getConstructors()) {
            if (constructor.getParameterCount()!=args.length) continue;
            Object message;
            try { message=constructor.newInstance(args); }
            catch (IllegalArgumentException wrongOverload) { continue; }
            TbfBridge1201.call("network."+name,"handle",message,context);
            return;
        }
        throw new NoSuchMethodException(name+" packet constructor");
    }
    private static Object context(ServerPlayer player, Connection connection) throws ReflectiveOperationException {
        try {
            return Class.forName(ROOT+"network.PacketContext").getConstructor(ServerPlayer.class).newInstance(player);
        } catch (ClassNotFoundException forge) {
            Class<?> type=Class.forName("net.minecraftforge.network.NetworkEvent$Context");
            Class<?> direction=Class.forName("net.minecraftforge.network.NetworkDirection");
            @SuppressWarnings({"unchecked","rawtypes"}) Object inbound=Enum.valueOf((Class)direction,"PLAY_TO_SERVER");
            Constructor<?> constructor=type.getDeclaredConstructor(Connection.class,direction,int.class);
            constructor.setAccessible(true);
            Object context=constructor.newInstance(connection,inbound,0);
            return (java.util.function.Supplier<Object>)()->context;
        }
    }
    private static void check(boolean valid,String label) {
        if(!valid) throw new IllegalStateException("TBF joint test: "+label);
    }
}
