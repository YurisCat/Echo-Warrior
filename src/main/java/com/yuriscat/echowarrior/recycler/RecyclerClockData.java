package com.yuriscat.echowarrior.recycler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Monotonic midnight sequence shared by every dimension.
 *
 * <p>The highest observed Overworld midnight index never moves backwards, so
 * repeated {@code /time set} calls cannot duplicate a natural recycling run.
 * A large forward jump advances the sequence only once.</p>
 */
public final class RecyclerClockData extends SavedData {
	private static final Codec<RecyclerClockData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("initialized", false).forGetter(data -> data.initialized),
			Codec.LONG.optionalFieldOf("highest_midnight_index", -1L).forGetter(data -> data.highestMidnightIndex),
			Codec.LONG.optionalFieldOf("midnight_sequence", 0L).forGetter(data -> data.midnightSequence)
	).apply(instance, RecyclerClockData::new));
	private static final SavedDataType<RecyclerClockData> TYPE = new SavedDataType<>(
			EchoWarrior.id("recycler_clock"), RecyclerClockData::new, CODEC, DataFixTypes.LEVEL);

	private boolean initialized;
	private long highestMidnightIndex = -1L;
	private long midnightSequence;

	public RecyclerClockData() {
	}

	private RecyclerClockData(boolean initialized, long highestMidnightIndex, long midnightSequence) {
		this.initialized = initialized;
		this.highestMidnightIndex = highestMidnightIndex;
		this.midnightSequence = Math.max(0L, midnightSequence);
	}

	public static RecyclerClockData get(ServerLevel overworld) {
		return overworld.getDataStorage().computeIfAbsent(TYPE);
	}

	public void observe(long overworldDayTime) {
		long currentIndex = Math.floorDiv(overworldDayTime - 18_000L, 24_000L);
		if (!this.initialized) {
			this.initialized = true;
			this.highestMidnightIndex = currentIndex;
			setDirty();
			return;
		}
		if (currentIndex <= this.highestMidnightIndex) return;
		this.highestMidnightIndex = currentIndex;
		this.midnightSequence++;
		setDirty();
	}

	public long midnightSequence() {
		return this.midnightSequence;
	}
}
