package com.yuriscat.echowarrior.compat.integration;

import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import org.spongepowered.asm.service.MixinService;

/** Optional targets are inspected as bytecode; no optional classes are linked at bootstrap. */
public final class PetCollarMixinPlugin1201 implements IMixinConfigPlugin {
    @Override public void onLoad(String pkg) {}
    @Override public List<String> getMixins() {
        List<String> result = new ArrayList<>();
        add(result, "PetHomeDataMixin1201", "com.github.yzqdev.pethome.server.entity.CitadelEntityData");
        add(result, "PetHomeUtilityMixin1201", "com.github.yzqdev.pethome.server.entity.TameableUtils");
        add(result, "DomesticationDataMixin1201", "com.github.alexthe666.citadel.server.entity.CitadelEntityData");
        add(result, "DomesticationUtilityMixin1201", "com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils");
        add(result, "PetHomeNetMixin1201", "com.github.yzqdev.pethome.server.item.NetItem");
        add(result, "PetHomeEnchantmentGuardMixin1201", "com.github.yzqdev.pethome.server.entity.TameableUtils");
        add(result, "DomesticationEnchantmentGuardMixin1201", "com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils");
        add(result, "PetHomeRequestGuardMixin1201", "com.github.yzqdev.pethome.server.misc.DIWorldData");
        add(result, "DomesticationRequestGuardMixin1201", "com.github.alexthe668.domesticationinnovation.server.misc.DIWorldData");
        add(result, "PetHomeInteractionGuardMixin1201", "com.github.yzqdev.pethome.server.handler.EntityInteractHandler");
        add(result, "DomesticationInteractionGuardMixin1201", "com.github.alexthe668.domesticationinnovation.server.CommonProxy");
        add(result, "PetHomeFabricInteractionGuardMixin1201", "com.github.yzqdev.pethome.server.event.PlayerInteractEntityHandler");
        addLegacyTracker(result, "PetHomeCollarTrackerMixin1201", "com.github.yzqdev.pethome.server.misc.CollarTickTracker");
        addLegacyTracker(result, "DomesticationCollarTrackerMixin1201", "com.github.alexthe668.domesticationinnovation.server.misc.CollarTickTracker");
        add(result, "PetHomeCompassTrackingMixin1201", "com.github.yzqdev.pethome.server.misc.PetCompassTracker");
        add(result, "PetHomeCompassActionMixin1201", "com.github.yzqdev.pethome.server.misc.PetCompassTeleport");
        addServerPacketGuard(result);
        add(result, "PetHomeNetProjectileMixin1201", "com.github.yzqdev.pethome.server.entity.NetEntity");
        try {
            MixinService.getService().getBytecodeProvider().getClassNode("com.github.yzqdev.pethome.network.PropertiesMessage$Handler");
            result.add("PetHomeForgePacketAuthorityMixin1201");
        } catch (ClassNotFoundException | java.io.IOException notForge) {
            add(result, "PetHomePacketAuthorityMixin1201", "com.github.yzqdev.pethome.network.PropertiesMessage");
        }
        add(result, "CitadelPacketAuthorityMixin1201", "com.github.alexthe666.citadel.server.message.PropertiesMessage$Handler");
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
    private static void addLegacyTracker(List<String> result, String mixin, String target) {
        try {
            var node = MixinService.getService().getBytecodeProvider()
                    .getClassNode(target);
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
    private static void addServerPacketGuard(List<String> result) {
        if (org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().getSide()
                != org.spongepowered.asm.mixin.MixinEnvironment.Side.SERVER) return;
        try {
            var node = MixinService.getService().getBytecodeProvider()
                    .getClassNode("com.github.yzqdev.pethome.network.PropertiesMessage");
            if (node.methods.stream().anyMatch(m -> m.name.equals("handleClient")
                    && m.desc.equals("(Lcom/github/yzqdev/pethome/network/PropertiesMessage;)V")
                    && (m.access & org.objectweb.asm.Opcodes.ACC_STATIC) != 0))
                result.add("PetHomeServerPacketMixin1201");
        } catch (ClassNotFoundException | java.io.IOException absent) { /* optional Fabric implementation */ }
    }

    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (!mixin.endsWith("PetHomeServerPacketMixin1201")) return;
        // A HEAD cancellation still leaves LocalPlayer -> Player verification in the
        // original body. Remove only this client-only body on the dedicated server.
        for (var method : node.methods) {
            if (!method.name.equals("handleClient")
                    || !method.desc.equals("(Lcom/github/yzqdev/pethome/network/PropertiesMessage;)V")) continue;
            method.instructions.clear();
            method.instructions.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.RETURN));
            method.tryCatchBlocks.clear();
            if (method.localVariables != null) method.localVariables.clear();
            method.visibleLocalVariableAnnotations = null;
            method.invisibleLocalVariableAnnotations = null;
            method.maxStack = 0;
            method.maxLocals = 1;
        }
    }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
