package com.yuriscat.echowarrior.compat.integration;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/** Checks the external API we use, without initializing TBF or Minecraft classes. */
public final class TbfCompatibility1201 {
    public static final String ROOT = "com.whidte.trulybestfriends.";
    private static final String UUID = "Ljava/util/UUID;";
    private static final String PATH = "Ljava/nio/file/Path;";
    private static final String FILE = "Ljava/io/File;";
    private static final Set<String> CONTEXTS = Set.of(
            "Ljava/util/function/Supplier;",
            "Lnet/neoforged/neoforge/network/handling/IPayloadContext;",
            "Lcom/whidte/trulybestfriends/network/PacketContext;");

    @FunctionalInterface public interface Bytecode {
        ClassNode read(String name) throws Exception;
    }
    public record Result(boolean compatible, boolean presenceProbe, boolean ownerHintRestore,
            boolean ownerTag, String context, String reason) {
        public static Result disabled(String reason) {
            return new Result(false, false, false, false, "", reason);
        }
    }
    private TbfCompatibility1201() {}

    /** Numeric components, not lexicographic order; no upper-version allowlist. */
    public static boolean versionSupported(String version, boolean legacyFabric022) {
        if (version == null || !version.matches("[0-9]+(?:\\.[0-9]+)*(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?"))
            return false;
        if (legacyFabric022 && version.equals("0.2.2")) return true;
        String[] numbers = version.split("[-+]", 2)[0].split("\\.");
        int[] minimum = {0, 2, 3};
        for (int i = 0; i < Math.max(numbers.length, minimum.length); i++) {
            BigInteger actual = i < numbers.length ? new BigInteger(numbers[i]) : BigInteger.ZERO;
            BigInteger floor = BigInteger.valueOf(i < minimum.length ? minimum[i] : 0);
            int comparison = actual.compareTo(floor);
            if (comparison != 0) return comparison > 0;
        }
        return !version.split("\\+", 2)[0].contains("-");
    }

    public static Result inspect(Bytecode source, ClassNode mappedTypes) {
        try {
            Map<String, String> types = new HashMap<>();
            for (var field : mappedTypes.fields) types.put(field.name, field.desc);
            String entity = type(types, "entity"), player = type(types, "player");
            String level = type(types, "level"), nbt = type(types, "nbt");
            ClassNode root = required(source, "trulybestfriends");
            method(root, "getCompatOwnerUUID", "(" + entity + ")" + UUID);
            method(root, "tryForceLoadPet", "(" + entity + player + level + ")" + object("trulybestfriends$LoadResult"));
            method(root, "deletePetData", "(" + player + UUID + ")Z");
            String commandSource = type(types, "commandSource");
            ClassNode commands = required(source, "command.ModCommands");
            method(commands, "loadPet", "(" + commandSource + player + entity + "Z)I");
            method(commands, "reportLoadResult", "(" + commandSource + entity
                    + object("trulybestfriends$LoadResult") + "Ljava/lang/String;Ljava/lang/String;Z)I");
            ClassNode result = required(source, "trulybestfriends$LoadResult");
            for (String value : new String[]{"OK", "NOT_A_PET"}) field(result, value, object("trulybestfriends$LoadResult"), true);

            ClassNode recall = required(source, "network.RecallPetPacket");
            String context = "";
            for (String candidate : CONTEXTS) {
                if (hasMethod(recall, "handle", "(" + object("network.RecallPetPacket") + candidate + ")V")) {
                    if (!context.isEmpty()) throw new IllegalArgumentException("ambiguous packet context");
                    context = candidate;
                }
            }
            if (context.isEmpty()) throw new IllegalArgumentException("RecallPetPacket.handle context changed");
            for (String name : new String[]{"RecallPetPacket", "SummonPetPacket", "TeleportPetToPlayerPacket",
                    "DirectTeleportPetToPlayerPacket", "ReleaseRecalledPetPacket", "RevivePetPacket", "HealPetPacket",
                    "DeletePetDataPacket", "SetPriorityPacket", "RequestPetDataPacket", "AreaRecallPacket"}) {
                ClassNode packet = required(source, "network." + name);
                method(packet, "handle", "(" + object("network." + name) + context + ")V");
                if (name.equals("AreaRecallPacket")) field(packet, "range", "I", false);
                else if (!name.equals("RequestPetDataPacket")) field(packet, "petUuid", UUID, false);
                if (name.equals("RecallPetPacket") || name.equals("TeleportPetToPlayerPacket"))
                    method(packet, "handleWithoutRideSwap", "(" + UUID + context + ")V");
            }
            method(required(source, "network.RequestPetDataPacket"), "shouldMarkLost", "(" + nbt + "Z)Z");
            ClassNode death = required(source, "network.PetDeathState");
            method(death, "shouldReleaseBeforeUntracking", "(" + nbt + "Z)Z");
            method(death, "shouldReleaseBeforeUntracking", "(" + nbt + "ZZ)Z");
            // The wildcard injection has require=allow=2. New overloads need review.
            if (death.methods.stream().filter(m -> m.name.equals("shouldReleaseBeforeUntracking")).count() != 2)
                throw new IllegalArgumentException("untracking overload count changed");

            ClassNode snapshot = required(source, "network.PetEntitySnapshot");
            String restore = "(" + nbt + UUID + level + ")" + entity;
            String restoreWithOwner = "(" + nbt + UUID + level + UUID + ")" + entity;
            method(snapshot, "restore", restore);
            boolean ownerHint = hasMethod(snapshot, "restore", restoreWithOwner);
            if (ownerHint) method(snapshot, "restore", restoreWithOwner);
            for (MethodNode candidate : snapshot.methods) {
                if (candidate.name.equals("restore") && !candidate.desc.equals(restore) && !candidate.desc.equals(restoreWithOwner))
                    throw new IllegalArgumentException("unguarded restore overload");
            }
            method(required(source, "network.PetIOUtil"), "getOwnerDir", "(" + player + ")" + PATH);
            ClassNode io = required(source, "network.NbtFileIO");
            method(io, "readCompressed", "(" + FILE + ")" + nbt);
            method(io, "writeCompressed", "(" + nbt + FILE + ")V");
            ClassNode sync = required(source, "network.SyncPetDataPacket");
            String syncType = object("network.SyncPetDataPacket");
            method(sync, "update", "(" + UUID + nbt + ")" + syncType);
            method(sync, "delete", "(" + UUID + ")" + syncType);
            method(sync, "sendToPlayer", "(" + player + syncType + ")V");

            ClassNode probe = optional(source, "network.PetPresenceProbe");
            if (probe != null) method(probe, "shouldProbe", "(" + player + UUID + nbt + ")Z");
            ClassNode owner = optional(source, "TbfOwnerTag");
            if (owner != null) {
                method(owner, "write", "(" + nbt + UUID + ")V");
                method(owner, "read", "(" + nbt + ")" + UUID);
            }
            return new Result(true, probe != null, ownerHint, owner != null, context, "required API verified");
        } catch (Exception | LinkageError mismatch) {
            return Result.disabled(mismatch.getClass().getSimpleName() + ": " + mismatch.getMessage());
        }
    }
    private static String object(String name) { return "L" + (ROOT + name).replace('.', '/') + ";"; }
    private static String type(Map<String, String> types, String name) {
        String descriptor = types.get(name);
        if (descriptor == null || Type.getType(descriptor).getSort() != Type.OBJECT)
            throw new IllegalArgumentException("missing mapped type " + name);
        return descriptor;
    }
    private static ClassNode required(Bytecode source, String name) throws Exception {
        ClassNode node = source.read(ROOT + name);
        if (node == null) throw new ClassNotFoundException(ROOT + name);
        return node;
    }
    private static ClassNode optional(Bytecode source, String name) throws Exception {
        try { return source.read(ROOT + name); }
        catch (ClassNotFoundException absent) { return null; }
    }
    private static boolean hasMethod(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(descriptor));
    }
    private static void method(ClassNode owner, String name, String descriptor) {
        if (owner.methods.stream().noneMatch(m -> m.name.equals(name) && m.desc.equals(descriptor)
                && (m.access & Opcodes.ACC_STATIC) != 0))
            throw new IllegalArgumentException(owner.name + "." + name + descriptor);
    }
    private static void field(ClassNode owner, String name, String descriptor, boolean isStatic) {
        if (owner.fields.stream().noneMatch(f -> f.name.equals(name) && f.desc.equals(descriptor)
                && ((f.access & Opcodes.ACC_STATIC) != 0) == isStatic))
            throw new IllegalArgumentException(owner.name + "." + name + ":" + descriptor);
    }
}
