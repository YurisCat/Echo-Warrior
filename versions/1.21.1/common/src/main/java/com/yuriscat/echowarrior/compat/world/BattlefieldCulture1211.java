package com.yuriscat.echowarrior.compat.world;

import com.yuriscat.echowarrior.compat.ModContent1211;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;

public enum BattlefieldCulture1211 {
    ROMAN("roman"),
    AZTEC("aztec"),
    EGYPTIAN("egyptian"),
    CHINESE("chinese"),
    JAPANESE("japanese");

    private final String id;

    BattlefieldCulture1211(String id) { this.id = id; }
    public String id() { return this.id; }

    public ResourceKey<LootTable> commonLoot() {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ModContent1211.id("archaeology/battlefield_common_" + this.id));
    }

    public ResourceKey<LootTable> guaranteedLoot() {
        return ResourceKey.create(Registries.LOOT_TABLE,
                ModContent1211.id("archaeology/battlefield_guaranteed_" + this.id));
    }

    public static BattlefieldCulture1211 random(RandomSource random) {
        BattlefieldCulture1211[] values = values();
        return values[random.nextInt(values.length)];
    }
}
