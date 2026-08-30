package com.yuriscat.echowarrior.item;

public enum EchoTrait {
	BAD_TEMPER(0, "bad_temper"),
	LAZY(1, "lazy"),
	COURAGE(2, "courage"),
	SKINNY(3, "skinny"),
	STURDY(4, "sturdy"),
	UNDEAD_SLAYER(5, "undead_slayer"),
	ARTHROPOD_SLAYER(6, "arthropod_slayer"),
	RAIDER_SLAYER(7, "raider_slayer"),
	GIANT_SLAYER(8, "giant_slayer"),
	NETHER_REAPER(9, "nether_reaper"),
	END_REAPER(10, "end_reaper"),
	OTHERWORLD_REAPER(11, "otherworld_reaper"),
	BIOME_AFFINITY(12, "biome_affinity"),
	NIGHT_OWL(13, "night_owl"),
	PERFECTIONIST(14, "perfectionist"),
	LAST_STAND(15, "last_stand"),
	UNYIELDING(16, "unyielding"),
	ELOQUENCE(17, "eloquence"),
	WISE(18, "wise"),
	MENTOR(19, "mentor"),
	LUCKY(20, "lucky"),
	FISHING(21, "fishing");

	private final int bit;
	private final String translationKey;

	EchoTrait(int bit, String id) {
		this.bit = bit;
		this.translationKey = "trait.echo_warrior." + id;
	}

	public int mask() {
		return 1 << this.bit;
	}

	public String nameTranslationKey() {
		return this.translationKey + ".name";
	}

	public String descriptionTranslationKey() {
		return this.translationKey + ".description";
	}

	public boolean conflictsWith(EchoTrait other) {
		if (this == other) return true;
		return isCreatureSlayer(this) && isCreatureSlayer(other)
				|| isRealmReaper(this) && isRealmReaper(other)
				|| this == SKINNY && other == STURDY
				|| this == STURDY && other == SKINNY;
	}

	private static boolean isCreatureSlayer(EchoTrait trait) {
		return trait == UNDEAD_SLAYER || trait == ARTHROPOD_SLAYER || trait == RAIDER_SLAYER;
	}

	private static boolean isRealmReaper(EchoTrait trait) {
		return trait == NETHER_REAPER || trait == END_REAPER || trait == OTHERWORLD_REAPER;
	}
}
