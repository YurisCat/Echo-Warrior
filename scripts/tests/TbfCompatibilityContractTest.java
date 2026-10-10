import com.yuriscat.echowarrior.compat.integration.TbfCompatibility1201;
import com.yuriscat.echowarrior.compat.integration.TbfCompatibility1211;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/** Reads third-party bytecode as data; never initializes a third-party class. */
public final class TbfCompatibilityContractTest {
    private static final String ROOT = "com.whidte.trulybestfriends.";
    private static int checks;
    public static void main(String[] args) throws Exception {
        for (String version : new String[]{"0.2.3", "0.2.3+build.1", "0.2.4", "0.2.4.1", "0.2.10", "0.10.0",
                "1.0.0", "0.2.4-beta.1", "99999999999999999999.0.0"})
            version(version, true);
        for (String version : new String[]{null, "", "unknown", "0.2.2", "0.2", "0.2.3-rc.1", "0.1.99", "0.2.3oops"})
            version(version, false);
        check(TbfCompatibility1201.versionSupported("0.2.2", true), "preserve legacy Fabric candidate");
        check(!TbfCompatibility1201.versionSupported("0.2.1", true), "legacy exception is exact");
        check(!accept(new HashMap<>()), "absent mod");
        for (String argument : args) {
            Map<String, ClassNode> nodes = read(Path.of(argument));
            check(accept(nodes), "actual package " + argument);
            Map<String, ClassNode> changed = copy(nodes);
            changed.get(ROOT + "network.SummonPetPacket").methods.removeIf(m -> m.name.equals("handle"));
            check(!accept(changed), "missing handler");
            changed = copy(nodes);
            find(changed, "network.SummonPetPacket", "handle").desc = "(Ljava/lang/Object;Ljava/lang/Object;)V";
            check(!accept(changed), "same arity but incompatible parameters");
            changed = copy(nodes);
            find(changed, "network.SyncPetDataPacket", "update").desc =
                    "(Ljava/util/UUID;Lnet/minecraft/nbt/CompoundTag;)Ljava/lang/Object;";
            check(!accept(changed), "changed return type");
            changed = copy(nodes);
            find(changed, "network.NbtFileIO", "readCompressed").access &= ~Opcodes.ACC_STATIC;
            check(!accept(changed), "method became instance method");
            changed = copy(nodes);
            changed.get(ROOT + "network.AreaRecallPacket").fields.stream().filter(f -> f.name.equals("range"))
                    .forEach(f -> f.desc = "J");
            check(!accept(changed), "changed packet field type");
            changed = copy(nodes);
            changed.get(ROOT + "network.PetDeathState").methods.add(new MethodNode(
                    Opcodes.ACC_STATIC, "shouldReleaseBeforeUntracking", "()Z", null, null));
            check(!accept(changed), "untracking wildcard must not discover a new overload");
            changed = copy(nodes);
            changed.get(ROOT + "network.PetEntitySnapshot").methods.add(new MethodNode(
                    Opcodes.ACC_STATIC, "restore", "()Ljava/lang/Object;", null, null));
            check(!accept(changed), "unguarded restore overload");
            if (nodes.containsKey(ROOT + "network.PetPresenceProbe")) {
                changed = copy(nodes);
                changed.get(ROOT + "network.PetPresenceProbe").methods.removeIf(m -> m.name.equals("shouldProbe"));
                check(!accept(changed), "existing probe cannot silently lose its guard");
                check(TbfCompatibility1211.inspect(nodes::get, types()).presenceProbe(), "new probe detected");
                check(TbfCompatibility1211.inspect(nodes::get, types()).ownerHintRestore(), "new restore detected");
            } else {
                check(!TbfCompatibility1211.inspect(nodes::get, types()).presenceProbe(), "old package skips absent probe");
                check(!TbfCompatibility1211.inspect(nodes::get, types()).ownerHintRestore(), "old package skips absent overload");
            }
            // Simulate a production namespace change for every Minecraft descriptor.
            changed = copy(nodes);
            for (ClassNode node : changed.values()) {
                node.fields.forEach(f -> f.desc = mapped(f.desc));
                node.methods.forEach(m -> m.desc = mapped(m.desc));
            }
            ClassNode mappedTypes = types();
            mappedTypes.fields.forEach(f -> f.desc = mapped(f.desc));
            var mappedResult = TbfCompatibility1211.inspect(changed::get, mappedTypes);
            check(mappedResult.compatible(), "mapped descriptor anchors: " + mappedResult.reason());
            check(TbfCompatibility1201.inspect(changed::get, mappedTypes).compatible(), "1.20.1 mapped descriptor anchors");
            System.out.println("PASS real package API and mutation guards: " + Path.of(argument).getFileName());
        }
        System.out.println("PASS TBF compatibility contract: " + checks + " checks");
    }
    private static String mapped(String descriptor) {
        return descriptor.replace("net/minecraft/world/entity/Entity", "test/mapped/Entity")
                .replace("net/minecraft/server/level/ServerPlayer", "test/mapped/Player")
                .replace("net/minecraft/server/level/ServerLevel", "test/mapped/Level")
                .replace("net/minecraft/nbt/CompoundTag", "test/mapped/Tag");
    }
    private static void version(String version, boolean expected) {
        check(TbfCompatibility1201.versionSupported(version, false) == expected, "1.20.1 version " + version);
        check(TbfCompatibility1211.versionSupported(version, false) == expected, "1.21.1 version " + version);
    }
    private static boolean accept(Map<String, ClassNode> nodes) {
        var first = TbfCompatibility1201.inspect(nodes::get, types());
        var second = TbfCompatibility1211.inspect(nodes::get, types());
        check(first.compatible() == second.compatible(), "version-line contract parity");
        return first.compatible();
    }
    private static Map<String, ClassNode> read(Path path) throws Exception {
        Map<String, ClassNode> nodes = new HashMap<>();
        try (JarFile jar = new JarFile(path.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                ClassNode node = new ClassNode();
                new ClassReader(jar.getInputStream(entry)).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                nodes.put(node.name.replace('/', '.'), node);
            }
        }
        return nodes;
    }
    private static Map<String, ClassNode> copy(Map<String, ClassNode> nodes) {
        Map<String, ClassNode> copy = new HashMap<>();
        nodes.forEach((name, value) -> { ClassNode cloned = new ClassNode(); value.accept(cloned); copy.put(name, cloned); });
        return copy;
    }
    private static MethodNode find(Map<String, ClassNode> nodes, String owner, String name) {
        return nodes.get(ROOT + owner).methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }
    private static ClassNode types() {
        ClassNode types = new ClassNode();
        types.fields.add(new FieldNode(0, "entity", "Lnet/minecraft/world/entity/Entity;", null, null));
        types.fields.add(new FieldNode(0, "player", "Lnet/minecraft/server/level/ServerPlayer;", null, null));
        types.fields.add(new FieldNode(0, "level", "Lnet/minecraft/server/level/ServerLevel;", null, null));
        types.fields.add(new FieldNode(0, "nbt", "Lnet/minecraft/nbt/CompoundTag;", null, null));
        return types;
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
