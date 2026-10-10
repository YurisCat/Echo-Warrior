package com.yuriscat.echowarrior.compat.integration;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Fail closed before optional targets are loaded, using their current mapped bytecode. */
public final class TbfMixinPlugin1201 implements IMixinConfigPlugin {
    private static TbfCompatibility1201.Result api = TbfCompatibility1201.Result.disabled("not inspected");
    public static boolean compatible() { return api.compatible(); }
    public static TbfCompatibility1201.Result api() { return api; }
    @Override public void onLoad(String pkg) {
        try {
            var provider = MixinService.getService().getBytecodeProvider();
            api = TbfCompatibility1201.inspect(provider::getClassNode,
                    provider.getClassNode("com.yuriscat.echowarrior.compat.integration.TbfApiTypes1201"));
        } catch (Exception | LinkageError unavailable) {
            api = TbfCompatibility1201.Result.disabled(unavailable.getClass().getSimpleName() + ": " + unavailable.getMessage());
        }
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (!api.compatible()) return false;
        if (mixin.contains("TbfPresenceProbeMixin")) return api.presenceProbe();
        if (mixin.contains("TbfSnapshotOwnerMixin")) return api.ownerHintRestore();
        if (mixin.contains("TbfFabric")) return api.context().equals("Lcom/whidte/trulybestfriends/network/PacketContext;");
        if (mixin.contains("TbfForge")) return api.context().equals("Ljava/util/function/Supplier;");
        if (mixin.contains("TbfNeoforge")) return api.context().equals("Lnet/neoforged/neoforge/network/handling/IPayloadContext;");
        return true;
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
