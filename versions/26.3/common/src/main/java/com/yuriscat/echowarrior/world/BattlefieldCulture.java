package com.yuriscat.echowarrior.world;

import com.mojang.serialization.Codec;
import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;

public enum BattlefieldCulture {
	ROMAN("roman"),
	AZTEC("aztec"),
	EGYPTIAN("egyptian"),
	CHINESE("chinese"),
	JAPANESE("japanese");

	public static final Codec<BattlefieldCulture> CODEC = Codec.STRING.xmap(BattlefieldCulture::fromId, BattlefieldCulture::id);
	private final String id;

	BattlefieldCulture(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	public ResourceKey<LootTable> commonLoot() {
		return ResourceKey.create(Registries.LOOT_TABLE, EchoWarrior.id("archaeology/battlefield_common_" + this.id));
	}

	public ResourceKey<LootTable> guaranteedLoot() {
		return ResourceKey.create(Registries.LOOT_TABLE, EchoWarrior.id("archaeology/battlefield_guaranteed_" + this.id));
	}

	public static BattlefieldCulture random(RandomSource random) {
		BattlefieldCulture[] values = values();
		return values[random.nextInt(values.length)];
	}

	public static BattlefieldCulture fromId(String id) {
		for (BattlefieldCulture culture : values()) {
			if (culture.id.equals(id)) return culture;
		}
		return ROMAN;
	}
}
