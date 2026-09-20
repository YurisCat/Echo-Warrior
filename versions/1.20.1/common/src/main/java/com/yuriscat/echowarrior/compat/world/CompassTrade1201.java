package com.yuriscat.echowarrior.compat.world;

import com.yuriscat.echowarrior.compat.ModContent1201;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

public final class CompassTrade1201 {
    private CompassTrade1201() {}
    public static MerchantOffer create() {
        return new MerchantOffer(new ItemStack(Items.EMERALD, 4), new ItemStack(Items.COMPASS),
                new ItemStack(ModContent1201.ECHO_COMPASS), 2, 10, 0.05F);
    }
}
