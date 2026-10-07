package com.yuriscat.echowarrior.compat.integration;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Optional targets are inspected as bytecode, never loaded on an absent-mod dedicated server. */
public final class TbfMixinPlugin1211 implements IMixinConfigPlugin {
    private static boolean compatible;
    public static boolean compatible() { return compatible; }
    @Override public void onLoad(String pkg) {
        compatible = false;
        try {
            var provider = MixinService.getService().getBytecodeProvider();
            ClassNode root = provider.getClassNode("com.whidte.trulybestfriends.trulybestfriends");
            for (String name : new String[]{"getCompatOwnerUUID", "tryForceLoadPet"})
                if (root.methods.stream().noneMatch(m -> m.name.equals(name))) return;
            for (String name : new String[]{"RecallPetPacket", "SummonPetPacket", "TeleportPetToPlayerPacket",
                    "DirectTeleportPetToPlayerPacket", "ReleaseRecalledPetPacket", "RevivePetPacket", "HealPetPacket",
                    "DeletePetDataPacket", "SetPriorityPacket", "RequestPetDataPacket", "AreaRecallPacket"}) {
                ClassNode packet = provider.getClassNode("com.whidte.trulybestfriends.network." + name);
                if (packet.methods.stream().noneMatch(m -> m.name.equals("handle")
                        && org.objectweb.asm.Type.getArgumentTypes(m.desc).length == 2)) return;
                if ((name.equals("RecallPetPacket") || name.equals("TeleportPetToPlayerPacket"))
                        && packet.methods.stream().noneMatch(m -> m.name.equals("handleWithoutRideSwap"))) return;
                if (name.equals("RequestPetDataPacket") && packet.methods.stream().noneMatch(m -> m.name.equals("shouldMarkLost"))) return;
            }
            ClassNode death = provider.getClassNode("com.whidte.trulybestfriends.network.PetDeathState");
            if (death.methods.stream().filter(m -> m.name.equals("shouldReleaseBeforeUntracking")
                    && m.desc.endsWith(")Z")).count() != 2) return;
            ClassNode snapshot = provider.getClassNode("com.whidte.trulybestfriends.network.PetEntitySnapshot");
            compatible = snapshot.methods.stream().anyMatch(m -> m.name.equals("restore"));
        } catch (Exception absentOrUnsupported) { compatible = false; }
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return compatible; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
