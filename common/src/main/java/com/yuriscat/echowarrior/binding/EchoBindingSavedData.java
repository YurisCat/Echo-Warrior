package com.yuriscat.echowarrior.binding;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Server-authoritative state for every Echo Summoner.
 *
 * <p>The physical ItemStack is deliberately only a mirror. Keeping the active
 * binding and mutable relic/fuel state here lets a loaded Echo keep operating
 * while its summoner is in an unloaded container.</p>
 */
public final class EchoBindingSavedData extends SavedData {
	public static final int SCHEMA_VERSION = 1;
	private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
	private static final Codec<EchoBindingSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.optionalFieldOf("schema_version", SCHEMA_VERSION).forGetter(data -> data.schemaVersion),
			Binding.CODEC.listOf().optionalFieldOf("bindings", List.of())
					.forGetter(data -> List.copyOf(data.bindings.values())),
			WarningCooldown.CODEC.listOf().optionalFieldOf("warning_cooldowns", List.of())
					.forGetter(data -> data.warningCooldowns.entrySet().stream()
							.map(entry -> new WarningCooldown(entry.getKey(), entry.getValue())).toList())
	).apply(instance, EchoBindingSavedData::new));
	private static final SavedDataType<EchoBindingSavedData> TYPE = new SavedDataType<>(
			EchoWarrior.id("echo_bindings"), EchoBindingSavedData::new, CODEC, DataFixTypes.LEVEL);

	private final Map<UUID, Binding> bindings = new HashMap<>();
	private final Map<UUID, Long> warningCooldowns = new HashMap<>();
	private int schemaVersion = SCHEMA_VERSION;

	public EchoBindingSavedData() {
	}

	private EchoBindingSavedData(int schemaVersion, List<Binding> bindings, List<WarningCooldown> warningCooldowns) {
		if (schemaVersion != SCHEMA_VERSION) {
			throw new IllegalStateException("Unsupported Echo binding schema " + schemaVersion
					+ "; this pre-release build requires a new world");
		}
		this.schemaVersion = schemaVersion;
		for (Binding binding : bindings) this.bindings.put(binding.summonerId, binding);
		for (WarningCooldown cooldown : warningCooldowns) {
			this.warningCooldowns.put(cooldown.playerId(), cooldown.lastWarningEpochMillis());
		}
	}

	public static EchoBindingSavedData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public Binding get(UUID summonerId) {
		return this.bindings.get(summonerId);
	}

	public Binding getOrCreate(UUID summonerId, ItemStack initialState) {
		Binding existing = this.bindings.get(summonerId);
		if (existing != null) return existing;
		Binding created = Binding.create(summonerId, initialState);
		this.bindings.put(summonerId, created);
		setDirty();
		return created;
	}

	public Iterable<Binding> bindings() {
		return List.copyOf(this.bindings.values());
	}

	public long lastWarning(UUID playerId) {
		return this.warningCooldowns.getOrDefault(playerId, Long.MIN_VALUE / 2L);
	}

	public void noteWarning(UUID playerId, long epochMillis) {
		this.warningCooldowns.put(playerId, epochMillis);
		setDirty();
	}

	public void changed(Binding binding) {
		binding.revision++;
		setDirty();
	}

	public void changedState(Binding binding) {
		binding.stateRevision++;
		changed(binding);
	}

	public static final class Binding {
		private static final Codec<Binding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUID_CODEC.fieldOf("summoner_id").forGetter(binding -> binding.summonerId),
				ItemStack.OPTIONAL_CODEC.optionalFieldOf("summoner_state", ItemStack.EMPTY)
						.forGetter(binding -> binding.summonerState),
				UUID_CODEC.optionalFieldOf("controller_id").forGetter(binding -> Optional.ofNullable(binding.controllerId)),
				UUID_CODEC.optionalFieldOf("entity_id").forGetter(binding -> Optional.ofNullable(binding.entityId)),
				Codec.LONG.optionalFieldOf("generation", 0L).forGetter(binding -> binding.generation),
				Codec.BOOL.optionalFieldOf("active", false).forGetter(binding -> binding.active),
				Codec.LONG.optionalFieldOf("revision", 0L).forGetter(binding -> binding.revision),
				Codec.LONG.optionalFieldOf("state_revision", 0L).forGetter(binding -> binding.stateRevision),
				Snapshot.CODEC.optionalFieldOf("snapshot", Snapshot.EMPTY).forGetter(binding -> binding.snapshot)
		).apply(instance, Binding::new));

		private final UUID summonerId;
		private ItemStack summonerState;
		private UUID controllerId;
		private UUID entityId;
		private long generation;
		private boolean active;
		private long revision;
		private long stateRevision;
		private Snapshot snapshot;

		private Binding(
				UUID summonerId,
				ItemStack summonerState,
				Optional<UUID> controllerId,
				Optional<UUID> entityId,
				long generation,
				boolean active,
				long revision,
				long stateRevision,
				Snapshot snapshot
		) {
			this.summonerId = summonerId;
			this.summonerState = summonerState.copy();
			this.controllerId = controllerId.orElse(null);
			this.entityId = entityId.orElse(null);
			this.generation = Math.max(0L, generation);
			this.active = active;
			this.revision = Math.max(0L, revision);
			this.stateRevision = Math.max(0L, stateRevision);
			this.snapshot = snapshot;
		}

		private static Binding create(UUID summonerId, ItemStack initialState) {
			return new Binding(summonerId, initialState.copy(), Optional.empty(), Optional.empty(),
					0L, false, 0L, 0L, Snapshot.EMPTY);
		}

		public UUID summonerId() { return this.summonerId; }
		public ItemStack summonerState() { return this.summonerState; }
		public UUID controllerId() { return this.controllerId; }
		public UUID entityId() { return this.entityId; }
		public long generation() { return this.generation; }
		public boolean active() { return this.active; }
		public long revision() { return this.revision; }
		public long stateRevision() { return this.stateRevision; }
		public Snapshot snapshot() { return this.snapshot; }

		void setSummonerState(ItemStack state) { this.summonerState = state.copy(); }
		void setControllerId(UUID controllerId) { this.controllerId = controllerId; }
		void setEntityId(UUID entityId) { this.entityId = entityId; }
		void setGeneration(long generation) { this.generation = Math.max(0L, generation); }
		void setActive(boolean active) { this.active = active; }
		void setSnapshot(Snapshot snapshot) { this.snapshot = snapshot; }
	}

	public record Snapshot(
			String dimension,
			double x,
			double y,
			double z,
			float health,
			float absorption,
			int remainingFireTicks,
			int ticksFrozen,
			int airSupply,
			CompoundTag migrationState
	) {
		private static final Snapshot EMPTY = new Snapshot("", 0.0, 0.0, 0.0, 0.0F, 0.0F, 0, 0, 300,
				new CompoundTag());
		private static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.optionalFieldOf("dimension", "").forGetter(Snapshot::dimension),
				Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(Snapshot::x),
				Codec.DOUBLE.optionalFieldOf("y", 0.0).forGetter(Snapshot::y),
				Codec.DOUBLE.optionalFieldOf("z", 0.0).forGetter(Snapshot::z),
				Codec.FLOAT.optionalFieldOf("health", 0.0F).forGetter(Snapshot::health),
				Codec.FLOAT.optionalFieldOf("absorption", 0.0F).forGetter(Snapshot::absorption),
				Codec.INT.optionalFieldOf("remaining_fire_ticks", 0).forGetter(Snapshot::remainingFireTicks),
				Codec.INT.optionalFieldOf("ticks_frozen", 0).forGetter(Snapshot::ticksFrozen),
				Codec.INT.optionalFieldOf("air_supply", 300).forGetter(Snapshot::airSupply),
				CompoundTag.CODEC.optionalFieldOf("migration_state", new CompoundTag())
						.forGetter(snapshot -> snapshot.migrationState.copy())
		).apply(instance, Snapshot::new));

		public Snapshot {
			migrationState = migrationState.copy();
		}

		@Override
		public CompoundTag migrationState() {
			return this.migrationState.copy();
		}
	}

	private record WarningCooldown(UUID playerId, long lastWarningEpochMillis) {
		private static final Codec<WarningCooldown> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUID_CODEC.fieldOf("player_id").forGetter(WarningCooldown::playerId),
				Codec.LONG.fieldOf("last_warning_epoch_millis").forGetter(WarningCooldown::lastWarningEpochMillis)
		).apply(instance, WarningCooldown::new));
	}
}
