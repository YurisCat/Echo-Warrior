package com.yuriscat.echowarrior.compat.recycler;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public final class RecyclerClockData1211 extends SavedData {
    private static final String FILE_ID = "echo_warrior_recycler_clock_1211";
    private static final Factory<RecyclerClockData1211> FACTORY = new Factory<>(
            RecyclerClockData1211::new, RecyclerClockData1211::load, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    private boolean initialized;
    private long highestMidnightIndex = -1L;
    private long midnightSequence;

    public static RecyclerClockData1211 get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public void observe(long dayTime) {
        long currentIndex = Math.floorDiv(dayTime - 18_000L, 24_000L);
        if (!this.initialized) {
            this.initialized = true;
            this.highestMidnightIndex = currentIndex;
            setDirty();
        } else if (currentIndex > this.highestMidnightIndex) {
            this.highestMidnightIndex = currentIndex;
            this.midnightSequence++;
            setDirty();
        }
    }

    public long midnightSequence() {
        return this.midnightSequence;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("Initialized", this.initialized);
        tag.putLong("HighestMidnightIndex", this.highestMidnightIndex);
        tag.putLong("MidnightSequence", this.midnightSequence);
        return tag;
    }

    private static RecyclerClockData1211 load(CompoundTag tag, HolderLookup.Provider registries) {
        RecyclerClockData1211 data = new RecyclerClockData1211();
        data.initialized = tag.getBoolean("Initialized");
        data.highestMidnightIndex = tag.contains("HighestMidnightIndex") ? tag.getLong("HighestMidnightIndex") : -1L;
        data.midnightSequence = Math.max(0L, tag.getLong("MidnightSequence"));
        return data;
    }
}
