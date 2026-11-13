package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.item.DiscountedMerchant;
import com.yuriscat.echowarrior.item.EchoTalentSystem;
import com.yuriscat.echowarrior.item.EchoTrait;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;

/** Opens ordinary villager trading with an Eloquence-discounted session merchant. */
@Mixin(Merchant.class)
public interface MerchantMixin {
	@Inject(method = "openTradingScreen", at = @At("HEAD"), cancellable = true)
	private void echoWarrior$openEloquenceTradingScreen(Player player, Component title, int level, CallbackInfo callback) {
		Merchant original = (Merchant)(Object)this;
		if (!(original instanceof Villager) || !EchoTalentSystem.hasNearbyTalent(player, EchoTrait.ELOQUENCE)) return;

		DiscountedMerchant session = new DiscountedMerchant(original);
		OptionalInt containerId = player.openMenu(new SimpleMenuProvider(
				(id, inventory, ignored) -> new MerchantMenu(id, inventory, session),
				title
		));
		if (containerId.isPresent()) {
			MerchantOffers offers = session.getOffers();
			if (!offers.isEmpty()) {
				player.sendMerchantOffers(
						containerId.getAsInt(),
						offers,
						level,
						session.getVillagerXp(),
						session.showProgressBar(),
						session.canRestock()
				);
			}
		}
		callback.cancel();
	}
}
