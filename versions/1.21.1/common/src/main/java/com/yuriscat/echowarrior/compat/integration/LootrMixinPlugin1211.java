package com.yuriscat.echowarrior.compat.integration;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Inspect the optional API without initializing Lootr, Minecraft or registries during Mixin selection. */
public final class LootrMixinPlugin1211 implements IMixinConfigPlugin {
    private static boolean compatible;
    public static boolean compatible() { return compatible; }

    @Override public void onLoad(String pkg) {
        try {
            var provider = MixinService.getService().getBytecodeProvider();
            ClassNode anchor = provider.getClassNode("com.yuriscat.echowarrior.compat.integration.LootrArchaeology1211");
            String key = anchor.fields.stream().filter(f -> f.name.equals("apiTableType")).findFirst().orElseThrow().desc;
            ClassNode api = provider.getClassNode("noobanidus.mods.lootr.common.api.LootrAPI");
            compatible = api.methods.stream().anyMatch(m -> m.name.equals("isLootTableBlacklisted")
                    && m.desc.equals("(" + key + ")Z") && (m.access & Opcodes.ACC_STATIC) != 0);
            if (!compatible) System.err.println("[Echo Warrior] Lootr archaeology adapter disabled: unsupported blacklist API.");
        } catch (ClassNotFoundException absent) {
            compatible = false;
        } catch (Exception | LinkageError error) {
            compatible = false;
            System.err.println("[Echo Warrior] Lootr archaeology adapter disabled: " + error);
        }
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) { return compatible; }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
