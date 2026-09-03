package com.yuriscat.echowarrior.mixin;

import com.yuriscat.echowarrior.ModItems;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(AbstractVillager.class)
abstract class AbstractVillagerMixin {
	@Inject(method = "getOffers", at = @At("RETURN"))
	private void echoWarrior$guaranteeEchoCompassTrade(CallbackInfoReturnable<MerchantOffers> callbackInfo) {
		Object self = this;
		if (!(self instanceof Villager villager)
				|| villager.getVillagerData().level() < 3
				|| !villager.getVillagerData().profession().is(VillagerProfession.CARTOGRAPHER)) return;
		MerchantOffers offers = callbackInfo.getReturnValue();
		if (offers.stream().anyMatch(offer -> offer.getResult().is(ModItems.ECHO_COMPASS))) return;
		offers.add(new MerchantOffer(
				new ItemCost(Items.EMERALD, 4),
				Optional.of(new ItemCost(Items.COMPASS, 1)),
				new ItemStack(ModItems.ECHO_COMPASS),
				2,
				10,
				0.2F
		));
	}
}
