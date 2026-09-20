package com.yuriscat.echowarrior.compat.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-warrior render emission budget, never shared as one last-tick value. */
public final class GuandaoValorParticles1201 {
    private final Map<UUID, Long> lastTicks = new HashMap<>();
    public record Emission(int flames, int sparks) {}
    private static final Emission NONE = new Emission(0, 0);

    public Emission claim(UUID warrior, long tick, int stacks, boolean inWorld) {
        if (!inWorld || stacks <= 0) return NONE;
        int interval = stacks >= 5 ? 1 : stacks == 4 ? 2 : stacks == 3 ? 3 : stacks == 2 ? 4 : 6;
        if (tick % interval != 0 || this.lastTicks.getOrDefault(warrior, Long.MIN_VALUE) == tick) return NONE;
        if (this.lastTicks.size() > 256 && !this.lastTicks.containsKey(warrior)) this.lastTicks.clear();
        this.lastTicks.put(warrior, tick);
        return new Emission(stacks >= 5 ? 3 : stacks >= 4 ? 2 : 1, stacks >= 5 && tick % 6 == 0 ? 8 : 0);
    }
}
