package com.yuriscat.echowarrior.item;

public enum EchoBiomeAffinity {
	WOODLAND("woodland"),
	OPENLAND("openland"),
	WASTELAND("wasteland"),
	COLD("cold"),
	WATERS("waters"),
	UNDERGROUND("underground");

	private final String translationKey;

	EchoBiomeAffinity(String id) {
		this.translationKey = "trait.echo_warrior.biome_affinity." + id;
	}

	public String nameTranslationKey() {
		return this.translationKey;
	}

	public static EchoBiomeAffinity byOrdinal(int value) {
		return values()[Math.clamp(value, 0, values().length - 1)];
	}
}
