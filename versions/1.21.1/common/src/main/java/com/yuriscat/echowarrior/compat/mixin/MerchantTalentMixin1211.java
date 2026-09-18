package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.DiscountedMerchant1211;
import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1211;
import com.yuriscat.echowarrior.compat.item.EchoTrait1211;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;

@Mixin(Merchant.class)
public interface MerchantTalentMixin1211 {
    @Inject(method = "openTradingScreen", at = @At("HEAD"), cancellable = true)
    private void echoWarrior1211$openDiscountedScreen(Player player, Component title, int level,
                                                      CallbackInfo callback) {
        Merchant original = (Merchant)(Object)this;
        if (!(original instanceof Villager)
                || !EchoTalentSystem1211.hasNearbyTalent(player, EchoTrait1211.ELOQUENCE)) return;
        DiscountedMerchant1211 session = new DiscountedMerchant1211(original);
        OptionalInt containerId = player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new MerchantMenu(id, inventory, session), title));
        if (containerId.isPresent()) {
            MerchantOffers offers = session.getOffers();
            if (!offers.isEmpty()) player.sendMerchantOffers(containerId.getAsInt(), offers, level,
                    session.getVillagerXp(), session.showProgressBar(), session.canRestock());
        }
        callback.cancel();
    }
}
