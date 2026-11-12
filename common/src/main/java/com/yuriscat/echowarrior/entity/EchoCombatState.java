package com.yuriscat.echowarrior.entity;

import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-victim state: no global entity references, independently expiring sources, saved with the victim. */
public final class EchoCombatState {
    public interface Holder {
        EchoCombatState echoWarrior$combatState();
    }

    public record Bleed(UUID caster, UUID owner, UUID summoner, float damage, long expiresAt, int assist) { }

    private final Map<UUID, Bleed> leaf = new HashMap<>();
    private final Map<UUID, Bleed> obsidian = new HashMap<>();
    private long nextLeafTick;
    private long nextObsidianTick;
    private long restrictedUntil;

    public static EchoCombatState of(net.minecraft.world.entity.LivingEntity entity) {
        return ((Holder)entity).echoWarrior$combatState();
    }

    public Map<UUID, Bleed> sources(boolean wound) { return wound ? obsidian : leaf; }
    public void clear(boolean wound) {
        sources(wound).clear();
        if (wound) nextObsidianTick = 0; else nextLeafTick = 0;
    }
    public void apply(boolean wound, Bleed bleed, long now) {
        var sources = sources(wound);
        sources.values().removeIf(entry -> entry.expiresAt() < now);
        if (sources.isEmpty()) {
            if (wound) nextObsidianTick = now + 20; else nextLeafTick = now + 40;
        }
        sources.put(bleed.caster(), bleed);
    }
    public Bleed strongest(boolean wound, long now) {
        sources(wound).values().removeIf(entry -> entry.expiresAt() < now);
        return sources(wound).values().stream().max(java.util.Comparator.comparingDouble(Bleed::damage)
                .thenComparing(entry -> entry.caster().toString())).orElse(null);
    }
    public boolean tickDue(boolean wound, long now) {
        long next = wound ? nextObsidianTick : nextLeafTick;
        if (next == 0 || now < next) return false;
        // Never replay unloaded time as a burst of damage.
        if (wound) nextObsidianTick = now + 20; else nextLeafTick = now + 40;
        return true;
    }
    public void restrictReach(long now) { restrictedUntil = Math.max(restrictedUntil, now + 40); }
    public boolean reachRestricted(long now) { return now < restrictedUntil; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Leaf", saveSources(leaf));
        tag.put("Obsidian", saveSources(obsidian));
        tag.putLong("NextLeaf", nextLeafTick);
        tag.putLong("NextObsidian", nextObsidianTick);
        tag.putLong("RestrictedUntil", restrictedUntil);
        return tag;
    }
    private static CompoundTag saveSources(Map<UUID, Bleed> sources) {
        CompoundTag result = new CompoundTag();
        for (Bleed source : sources.values()) {
            CompoundTag entry = new CompoundTag();
            if (source.owner() != null) entry.putString("Owner", source.owner().toString());
            if (source.summoner() != null) entry.putString("Summoner", source.summoner().toString());
            entry.putFloat("Damage", source.damage());
            entry.putLong("Expires", source.expiresAt());
            entry.putInt("Assist", source.assist());
            result.put(source.caster().toString(), entry);
        }
        return result;
    }
    public void load(CompoundTag tag) {
        leaf.clear(); obsidian.clear();
        readSources(tag.getCompoundOrEmpty("Leaf"), leaf);
        readSources(tag.getCompoundOrEmpty("Obsidian"), obsidian);
        nextLeafTick = tag.getLongOr("NextLeaf", 0);
        nextObsidianTick = tag.getLongOr("NextObsidian", 0);
        restrictedUntil = tag.getLongOr("RestrictedUntil", 0);
    }
    private static void readSources(CompoundTag tag, Map<UUID, Bleed> into) {
        for (String key : tag.keySet()) {
            UUID caster = uuid(key);
            CompoundTag entry = tag.getCompoundOrEmpty(key);
            float damage = entry.getFloatOr("Damage", 1);
            if (caster == null || !Float.isFinite(damage) || damage < 0) continue;
            into.put(caster, new Bleed(caster, uuid(entry.getStringOr("Owner", "")),
                    uuid(entry.getStringOr("Summoner", "")), damage, entry.getLongOr("Expires", 0),
                    entry.getIntOr("Assist", 1)));
        }
    }
    private static UUID uuid(String value) {
        try { return UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; }
    }
}
