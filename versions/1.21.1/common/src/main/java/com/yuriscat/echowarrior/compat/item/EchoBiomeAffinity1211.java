package com.yuriscat.echowarrior.compat.item;

public enum EchoBiomeAffinity1211 {
    WOODLAND("woodland"),
    OPENLAND("openland"),
    WASTELAND("wasteland"),
    COLD("cold"),
    WATERS("waters"),
    UNDERGROUND("underground");

    private final String id;

    EchoBiomeAffinity1211(String id) {
        this.id = id;
    }

    public String id() { return this.id; }
    public String nameTranslationKey() { return "trait.echo_warrior.biome_affinity." + this.id; }

    public static EchoBiomeAffinity1211 byOrdinal(int value) {
        return values()[Math.max(0, Math.min(values().length - 1, value))];
    }
}
