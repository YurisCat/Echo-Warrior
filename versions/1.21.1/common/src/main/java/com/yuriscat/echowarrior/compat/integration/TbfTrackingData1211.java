package com.yuriscat.echowarrior.compat.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Adapter-only untracking preferences; authoritative Echo bindings stay in their original save. */
public final class TbfTrackingData1211 extends SavedData {
    private final Set<String> muted = new HashSet<>();
    public static TbfTrackingData1211 get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(TbfTrackingData1211::new, (tag, registries) -> load(tag), net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE), "echo_warrior_tbf"); }
    public boolean muted(UUID owner, UUID entry) { return muted.contains(owner + ":" + entry); }
    public void mute(UUID owner, UUID entry, boolean value) {
        boolean changed = value ? muted.add(owner + ":" + entry) : muted.remove(owner + ":" + entry);
        if (changed) setDirty();
    }
    private static TbfTrackingData1211 load(CompoundTag tag) {
        var data = new TbfTrackingData1211();
        ListTag entries = tag.getList("Muted", 8);
        for (int i = 0; i < entries.size(); i++) data.muted.add(entries.getString(i));
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        muted.stream().sorted().forEach(value -> entries.add(StringTag.valueOf(value)));
        tag.put("Muted", entries);
        return tag;
    }
}
