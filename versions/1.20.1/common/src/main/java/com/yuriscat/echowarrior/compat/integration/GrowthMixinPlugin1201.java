package com.yuriscat.echowarrior.compat.integration;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Select a constructor boundary without loading Minecraft or optional mod classes. */
public final class GrowthMixinPlugin1201 implements IMixinConfigPlugin {
    private boolean apothicOwner;

    @Override public void onLoad(String pkg) {
        try {
            MixinService.getService().getBytecodeProvider()
                    .getClassNode("dev.shadowsoffire.attributeslib.util.IEntityOwned");
            apothicOwner = true;
        } catch (ClassNotFoundException | java.io.IOException absent) {
            apothicOwner = false;
        }
        org.slf4j.LoggerFactory.getLogger("echo_warrior").info(
                "[Compat1201] Growth initialization: {}",
                apothicOwner ? "Mob constructor after Apothic owner binding" : "LivingEntity constructor");
    }

    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith(".EchoGrowthEntityMixin1201")) return !apothicOwner;
        if (mixin.endsWith(".ApothicGrowthEntityMixin1201")) return apothicOwner;
        return true;
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
