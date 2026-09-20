package com.yuriscat.echowarrior.compat.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

public final class DiscountedMerchant1201 implements Merchant {
    private static final float COST_MULTIPLIER = 0.85F;
    private final Merchant delegate;
    private final MerchantOffers offers = new MerchantOffers();
    private final Map<MerchantOffer, MerchantOffer> originalBySessionOffer = new IdentityHashMap<>();

    public DiscountedMerchant1201(Merchant delegate) {
        this.delegate = delegate;
        for (MerchantOffer original : delegate.getOffers()) {
            MerchantOffer session = discountedCopy(original);
            this.offers.add(session);
            this.originalBySessionOffer.put(session, original);
        }
    }

    private static MerchantOffer discountedCopy(MerchantOffer original) {
        return new MerchantOffer(discountedCost(original.getCostA()), discountedCost(original.getCostB()),
                original.getResult().copy(), original.getUses(), original.getMaxUses(), original.getXp(), 0.0F);
    }

    private static ItemStack discountedCost(ItemStack original) {
        if (original.isEmpty()) return ItemStack.EMPTY;
        return original.copyWithCount(Math.max(1, (int)Math.floor(original.getCount() * COST_MULTIPLIER)));
    }

    @Override public void setTradingPlayer(Player player) { this.delegate.setTradingPlayer(player); }
    @Override public Player getTradingPlayer() { return this.delegate.getTradingPlayer(); }
    @Override public MerchantOffers getOffers() { return this.offers; }
    @Override public void overrideOffers(MerchantOffers offers) {
        this.offers.clear();
        this.offers.addAll(offers);
        this.originalBySessionOffer.clear();
    }
    @Override public void notifyTrade(MerchantOffer offer) {
        MerchantOffer original = this.originalBySessionOffer.get(offer);
        if (original == null) return;
        offer.increaseUses();
        this.delegate.notifyTrade(original);
    }
    @Override public void notifyTradeUpdated(ItemStack stack) { this.delegate.notifyTradeUpdated(stack); }
    @Override public int getVillagerXp() { return this.delegate.getVillagerXp(); }
    @Override public void overrideXp(int xp) { this.delegate.overrideXp(xp); }
    @Override public boolean showProgressBar() { return this.delegate.showProgressBar(); }
    @Override public SoundEvent getNotifyTradeSound() { return this.delegate.getNotifyTradeSound(); }
    @Override public boolean canRestock() { return this.delegate.canRestock(); }
    @Override public boolean isClientSide() { return this.delegate.isClientSide(); }
}
