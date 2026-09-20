package com.yuriscat.echowarrior.compat.world;

import com.yuriscat.echowarrior.compat.ModContent1201;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;

public enum BattlefieldCulture1201 {
    ROMAN("roman"),
    AZTEC("aztec"),
    EGYPTIAN("egyptian"),
    CHINESE("chinese"),
    JAPANESE("japanese");

    private final String id;

    BattlefieldCulture1201(String id) { this.id = id; }
    public String id() { return this.id; }

    public ResourceLocation commonLoot() {
        return ModContent1201.id("archaeology/battlefield_common_" + this.id);
    }

    public ResourceLocation guaranteedLoot() {
        return ModContent1201.id("archaeology/battlefield_guaranteed_" + this.id);
    }

    public static BattlefieldCulture1201 random(RandomSource random) {
        BattlefieldCulture1201[] values = values();
        return values[random.nextInt(values.length)];
    }
}
