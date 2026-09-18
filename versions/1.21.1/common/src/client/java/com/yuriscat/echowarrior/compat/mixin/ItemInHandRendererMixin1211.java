package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.EchoSummonerItem1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1211;
import com.yuriscat.echowarrior.compat.tutorial.TutorialManualStackData1211;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps component updates on one summoner from looking like repeated item swaps in first person. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin1211 {
    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;

    @Inject(method = "tick", at = @At("HEAD"))
    private void echoWarrior$keepPhysicalSummonerEquipped(CallbackInfo callback) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack currentMainHand = player.getMainHandItem();
        ItemStack currentOffHand = player.getOffhandItem();
        if (EchoSummonerItem1211.isSamePhysicalSummoner(this.mainHandItem, currentMainHand)
                || TutorialManualStackData1211.isSamePhysicalManual(this.mainHandItem, currentMainHand)
                || KnowledgeStackData1211.isSamePhysicalCollection(this.mainHandItem, currentMainHand)) {
            this.mainHandItem = currentMainHand;
        }
        if (EchoSummonerItem1211.isSamePhysicalSummoner(this.offHandItem, currentOffHand)
                || TutorialManualStackData1211.isSamePhysicalManual(this.offHandItem, currentOffHand)
                || KnowledgeStackData1211.isSamePhysicalCollection(this.offHandItem, currentOffHand)) {
            this.offHandItem = currentOffHand;
        }
    }
}
