package com.yuriscat.echowarrior.compat.mixin;

import com.yuriscat.echowarrior.compat.item.DiscountedMerchant1201;
import com.yuriscat.echowarrior.compat.item.EchoTalentSystem1201;
import com.yuriscat.echowarrior.compat.item.EchoTrait1201;
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

@Mixin(Villager.class)
public abstract class MerchantTalentMixin1201 {
    // Mixin 0.8.5 cannot inject interface default methods; intercept the concrete vanilla call.
    @org.spongepowered.asm.mixin.injection.Redirect(method = "startTrading", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/npc/Villager;openTradingScreen(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;I)V"))
    private void echoWarrior1201$openDiscountedScreen(Villager original, Player player, Component title, int level) {
        if (!EchoTalentSystem1201.hasNearbyTalent(player, EchoTrait1201.ELOQUENCE)) {
            original.openTradingScreen(player, title, level);
            return;
        }
        DiscountedMerchant1201 session = new DiscountedMerchant1201(original);
        OptionalInt containerId = player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new MerchantMenu(id, inventory, session), title));
        if (containerId.isPresent()) {
            MerchantOffers offers = session.getOffers();
            if (!offers.isEmpty()) player.sendMerchantOffers(containerId.getAsInt(), offers, level,
                    session.getVillagerXp(), session.showProgressBar(), session.canRestock());
        }
    }
}
