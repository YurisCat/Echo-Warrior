package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.EchoSummonerItem1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** NBT mirrors are the same physical held summoner, not a repeated equip action. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin1201 {
    @Shadow private ItemStack mainHandItem;
    @Shadow private ItemStack offHandItem;

    @Inject(method = "tick", at = @At("HEAD"))
    private void echoWarrior$keepPhysicalSummonerEquipped(CallbackInfo callback) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (echoWarrior$sameHeldItem(mainHandItem, player.getMainHandItem())) mainHandItem = player.getMainHandItem();
        if (echoWarrior$sameHeldItem(offHandItem, player.getOffhandItem())) offHandItem = player.getOffhandItem();
    }
    @org.spongepowered.asm.mixin.Unique
    private static boolean echoWarrior$sameHeldItem(ItemStack previous, ItemStack current) {
        return EchoSummonerItem1201.isSamePhysicalSummoner(previous, current)
                || com.yuriscat.echowarrior.compat.tutorial.TutorialManualStackData1201.isSamePhysicalManual(previous, current)
                || com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1201.isSamePhysicalCollection(previous, current);
    }
}
