package com.yuriscat.echowarrior.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/** A single trading-screen snapshot whose payment costs are discounted without mutating villager offers. */
public final class DiscountedMerchant implements Merchant {
	private static final float COST_MULTIPLIER = 0.85F;

	private final Merchant delegate;
	private final MerchantOffers offers = new MerchantOffers();
	private final Map<MerchantOffer, MerchantOffer> originalBySessionOffer = new IdentityHashMap<>();

	public DiscountedMerchant(Merchant delegate) {
		this.delegate = delegate;
		for (MerchantOffer original : delegate.getOffers()) {
			MerchantOffer sessionOffer = discountedCopy(original);
			this.offers.add(sessionOffer);
			this.originalBySessionOffer.put(sessionOffer, original);
		}
	}

	private static MerchantOffer discountedCopy(MerchantOffer original) {
		ItemCost costA = discountedCost(original.getItemCostA(), original.getCostA().getCount());
		Optional<ItemCost> costB = original.getItemCostB()
				.map(cost -> discountedCost(cost, cost.count()));
		return new MerchantOffer(
				costA,
				costB,
				original.getResult().copy(),
				original.getUses(),
				original.getMaxUses(),
				original.getXp(),
				0.0F
		);
	}

	private static ItemCost discountedCost(ItemCost original, int displayedCount) {
		int discountedCount = Math.max(1, (int)Math.floor(displayedCount * COST_MULTIPLIER));
		return new ItemCost(original.item(), discountedCount, original.components());
	}

	@Override
	public void setTradingPlayer(@Nullable Player player) {
		this.delegate.setTradingPlayer(player);
	}

	@Override
	public @Nullable Player getTradingPlayer() {
		return this.delegate.getTradingPlayer();
	}

	@Override
	public MerchantOffers getOffers() {
		return this.offers;
	}

	@Override
	public void overrideOffers(MerchantOffers offers) {
		this.offers.clear();
		this.offers.addAll(offers);
		this.originalBySessionOffer.clear();
	}

	@Override
	public void notifyTrade(MerchantOffer sessionOffer) {
		MerchantOffer original = this.originalBySessionOffer.get(sessionOffer);
		if (original == null) return;
		sessionOffer.increaseUses();
		this.delegate.notifyTrade(original);
	}

	@Override
	public void notifyTradeUpdated(ItemStack itemStack) {
		this.delegate.notifyTradeUpdated(itemStack);
	}

	@Override
	public int getVillagerXp() {
		return this.delegate.getVillagerXp();
	}

	@Override
	public void overrideXp(int xp) {
		this.delegate.overrideXp(xp);
	}

	@Override
	public boolean showProgressBar() {
		return this.delegate.showProgressBar();
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return this.delegate.getNotifyTradeSound();
	}

	@Override
	public boolean canRestock() {
		return this.delegate.canRestock();
	}

	@Override
	public boolean isClientSide() {
		return this.delegate.isClientSide();
	}

	@Override
	public boolean stillValid(Player player) {
		return this.delegate.stillValid(player);
	}
}
