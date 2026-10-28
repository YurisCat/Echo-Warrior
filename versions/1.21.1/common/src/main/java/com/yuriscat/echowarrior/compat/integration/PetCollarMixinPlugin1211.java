package com.yuriscat.echowarrior.compat.integration;

import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import org.spongepowered.asm.service.MixinService;

/** Optional targets are inspected as bytecode; no optional classes are linked at bootstrap. */
public final class PetCollarMixinPlugin1211 implements IMixinConfigPlugin {
    @Override public void onLoad(String pkg) {}
    @Override public List<String> getMixins() {
        List<String> result = new ArrayList<>();
        add(result, "PetHomeDataMixin1211", "com.github.yzqdev.pethome.util.CitadelEntityData");
        add(result, "PetHomeUtilityMixin1211", "com.github.yzqdev.pethome.util.TameableUtils");
        add(result, "PetHomeNetMixin1211", "com.github.yzqdev.pethome.server.item.NetItem");
        add(result, "PetHomeEnchantmentGuardMixin1211", "com.github.yzqdev.pethome.util.TameableUtils");
        add(result, "PetHomeRequestGuardMixin1211", "com.github.yzqdev.pethome.server.misc.PHWorldData");
        addInteraction(result, "PetHomeInteractionGuardMixin1211", "com.github.yzqdev.pethome.server.event.PlayerInteractEntityHandler", 1);
        addInteraction(result, "PetHomeFabricInteractionGuardMixin1211", "com.github.yzqdev.pethome.server.event.PlayerInteractEntityHandler", 3);
        addLegacyTracker(result, "PetHomeCollarTrackerMixin1211");
        add(result, "PetHomeCompassTrackingMixin1211", "com.github.yzqdev.pethome.server.misc.PetCompassTracker");
        add(result, "PetHomeCompassActionMixin1211", "com.github.yzqdev.pethome.server.misc.PetCompassTeleport");
        add(result, "PetHomeNetProjectileMixin1211", "com.github.yzqdev.pethome.server.entity.NetEntity");
        add(result, "PetHomePacketAuthorityMixin1211", "com.github.yzqdev.pethome.network.PropertiesMessage");
        return result;
    }
    private static void add(List<String> result, String mixin, String target) {
        try {
            if (MixinService.getService().getBytecodeProvider().getClassNode(target) != null) result.add(mixin);
        } catch (ClassNotFoundException | java.io.IOException absent) { /* optional mod absent */ }
    }
    private static void addInteraction(List<String> result, String mixin, String target, int arguments) {
        try {
            var node = MixinService.getService().getBytecodeProvider().getClassNode(target);
            if (node.methods.stream().anyMatch(m -> m.name.equals("onInteractWithEntity")
                    && org.objectweb.asm.Type.getArgumentTypes(m.desc).length == arguments)) result.add(mixin);
        } catch (ClassNotFoundException | java.io.IOException absent) { /* optional target */ }
    }
    private static void addLegacyTracker(List<String> result, String mixin) {
        try {
            var node = MixinService.getService().getBytecodeProvider()
                    .getClassNode("com.github.yzqdev.pethome.server.misc.CollarTickTracker");
            for (var method : node.methods) {
                if (!method.name.equals("tick") || !method.desc.equals("()V")) continue;
                for (var insn : method.instructions) {
                    if (insn instanceof org.objectweb.asm.tree.MethodInsnNode call
                            && call.owner.equals("java/util/Map") && call.name.equals("keySet")
                            && call.desc.equals("()Ljava/util/Set;")) { result.add(mixin); return; }
                }
            }
        } catch (ClassNotFoundException | java.io.IOException absent) { /* optional target */ }
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return true; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
