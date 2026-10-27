package com.yuriscat.echowarrior.compat.integration;

import com.yuriscat.echowarrior.compat.binding.EchoBindingSavedData1211;
import com.yuriscat.echowarrior.compat.binding.EchoBindingSystem1211;
import com.yuriscat.echowarrior.compat.entity.EchoWarriorEntity1211;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transfers only reviewed collar state across Echo entity generations.
 * The installed relic in binding SavedData is authoritative; entity NBT and physical stacks
 * are never used to replace that authority. No third-party class is a linkage dependency.
 */
public final class PetCollarCompat1211 {
    public static final String KEY = "EchoWarriorPetCollars";
    private static final String ENCHANTMENTS = "StoredPetEnchantments";
    private static final String COLLAR = "HasPetCollar";
    private static final String[] COOLDOWNS = {
            "PetShadowPunchCooldown", "PetPsychicWallCooldown", "PetIntimidationCooldown",
            "PetBlazingProtectionCooldown"
    };
    // CollarSwapCooldown is a write-only legacy marker in the audited official jars.
    // Turning its constant 20 into now+20 would dirty the relic every tick forever.
    private static final Logger LOG = LoggerFactory.getLogger("echo_warrior");
    private static final ThreadLocal<Boolean> RESTORING = ThreadLocal.withInitial(() -> false);
    private static List<Api> adapters;

    private PetCollarCompat1211() {}

    public static boolean installed() { return !apis().isEmpty(); }

    /** A stale entity must not write to a different generation or a newly installed relic. */
    public static boolean capture(LivingEntity living) {
        if (RESTORING.get() || !(living instanceof EchoWarriorEntity1211 echo)
                || !(living.level() instanceof ServerLevel level) || echo.getSummonerUuid() == null
                || apis().isEmpty()) return false;
        var binding = EchoBindingSavedData1211.get(level.getServer()).get(echo.getSummonerUuid());
        if (binding == null || !binding.active()
                || binding.generation() != echo.getBindingGeneration()
                || !living.getUUID().equals(binding.spiritId())) return false;
        ItemStack relic = binding.relic();
        CompoundTag original = read(relic);
        CompoundTag collars = compound(original, KEY).copy();
        long now = level.getServer().overworld().getGameTime();
        for (Api api : apis()) {
            CompoundTag data = api.read(living);
            // Absence on an ordinary fresh entity does not erase unavailable historical data.
            if (!bool(data, COLLAR) && !data.contains(ENCHANTMENTS)) continue;
            CompoundTag state = new CompoundTag();
            state.putInt("Schema", 1);
            state.putBoolean(COLLAR, bool(data, COLLAR));
            Tag enchantments = data.get(ENCHANTMENTS);
            if (enchantments instanceof ListTag list) state.put(ENCHANTMENTS, list.copy());
            if (living.hasCustomName()) {
                state.putString("Name", living.getCustomName().getString());
                net.minecraft.network.chat.ComponentSerialization.CODEC.encodeStart(
                        net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, level.registryAccess()),
                        living.getCustomName()).result().ifPresent(value -> state.put("NameComponent", value));
            }
            for (String key : COOLDOWNS) {
                int remaining = Math.max(0, integer(data, key));
                if (remaining > 0) state.putLong(key, Math.addExact(now, remaining));
            }
            if (data.contains("PetSonicBoomTime")) state.putLong("PetSonicBoomTime", number(data, "PetSonicBoomTime"));
            int healing = api.healing(living);
            if (healing > 0) state.putLong("PetHealingAuraTime", Math.addExact(now, healing));
            if (data.contains("PetBlazingProtectionBars"))
                state.putInt("PetBlazingProtectionBars", Math.max(0, integer(data, "PetBlazingProtectionBars")));
            collars.put(api.id, state);
        }
        if (collars.isEmpty() || collars.equals(compound(original, KEY))) return false;
        original.put(KEY, collars);
        write(relic, original);
        EchoBindingSystem1211.persistRelic(level, echo.getSummonerUuid(), relic);
        return true;
    }

    /** Called once for a newly created hero, before its health is assigned or it joins the world. */
    public static boolean restore(LivingEntity living, ItemStack relic) {
        CompoundTag collars = compound(read(relic), KEY);
        if (collars.isEmpty()) return true;
        long now = living.level() instanceof ServerLevel level
                ? level.getServer().overworld().getGameTime() : 0L;
        boolean previous = RESTORING.get();
        RESTORING.set(true);
        try {
            for (Api api : apis()) {
                CompoundTag state = compound(collars, api.id);
                if (state.isEmpty()) continue;
                if (!validStoredState(state)) {
                    LOG.warn("[Pet collar compatibility] Refusing to replace unsupported stored {} collar data", api.id);
                    return false;
                }
                CompoundTag data = api.read(living).copy();
                data.putBoolean(COLLAR, bool(state, COLLAR));
                Tag enchantments = state.get(ENCHANTMENTS);
                data.put(ENCHANTMENTS, enchantments instanceof ListTag list ? list.copy() : new ListTag());
                for (String key : COOLDOWNS) data.putInt(key, remaining(state, key, now));
                if (state.contains("PetBlazingProtectionBars"))
                    data.putInt("PetBlazingProtectionBars", Math.max(0, integer(state, "PetBlazingProtectionBars")));
                if (state.contains("PetSonicBoomTime")) data.putLong("PetSonicBoomTime", number(state, "PetSonicBoomTime"));
                api.write(living, data);
                // The mod's own update path removes/replaces fixed-ID modifiers and syncs its data.
                api.refresh(living);
                api.healing(living, remaining(state, "PetHealingAuraTime", now));
                if (state.contains("Name")) living.setCustomName(Component.literal(string(state, "Name")));
                if (state.contains("NameComponent")) net.minecraft.network.chat.ComponentSerialization.CODEC.parse(
                        net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, living.registryAccess()),
                        state.get("NameComponent")).result().ifPresent(living::setCustomName);
            }
        } finally {
            RESTORING.set(previous);
        }
        return true;
    }

    private static boolean validStoredState(CompoundTag state) {
        if (integer(state, "Schema") != 1) return false;
        Tag value = state.get(ENCHANTMENTS);
        if (value == null) return true;
        if (!(value instanceof ListTag list) || list.size() > 256) return false;
        for (Tag entry : list) {
            if (!(entry instanceof CompoundTag tag) || !string(tag, "id").matches("[a-z0-9_.-]+:[a-z0-9/._-]+")
                    || integer(tag, "lvl") < 0 || integer(tag, "lvl") > Short.MAX_VALUE) return false;
        }
        return true;
    }

    private static final Map<LivingEntity, Long> LAST_RESTRICTION = new java.util.WeakHashMap<>();
    public static void explainRestriction(LivingEntity entity) {
        if (!(entity instanceof EchoWarriorEntity1211 echo) || !(entity.level() instanceof ServerLevel level)
                || echo.getOwnerUuid() == null) return;
        long now = level.getServer().overworld().getGameTime();
        if (now - LAST_RESTRICTION.getOrDefault(entity, Long.MIN_VALUE / 2) < 100) return;
        LAST_RESTRICTION.put(entity, now);
        var player = level.getServer().getPlayerList().getPlayer(echo.getOwnerUuid());
        if (player != null && player.connection != null)
            player.displayClientMessage(Component.translatable("message.echo_warrior.pet_management_restricted"), true);
    }

    public static boolean isEchoRequest(Object request) {
        try {
            Method accessor;
            try { accessor = request.getClass().getMethod("getEntityTypeLoc"); }
            catch (NoSuchMethodException recordSchema) { accessor = request.getClass().getMethod("entityType"); }
            return ((String)accessor.invoke(request)).startsWith("echo_warrior:");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Unsupported third-party pet request", failure);
        }
    }

    public static boolean rejectInteraction(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.entity.Entity entity, ItemStack stack) {
        if (!(entity instanceof EchoWarriorEntity1211 echo)) return false;
        String item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!item.equals("pet_home:deed_of_ownership") && !item.equals("domesticationinnovation:deed_of_ownership")
                && !item.equals("pet_home:net") && !item.equals("pet_home:net_item")) return false;
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && serverPlayer.connection != null)
            serverPlayer.displayClientMessage(Component.translatable("message.echo_warrior.pet_management_restricted"), true);
        return true;
    }

    public static boolean rejectEventInteraction(Object event) {
        try {
            Class<?> type = event.getClass();
            var player = (net.minecraft.world.entity.player.Player)type.getMethod("getEntity").invoke(event);
            var entity = (net.minecraft.world.entity.Entity)type.getMethod("getTarget").invoke(event);
            var stack = (ItemStack)type.getMethod("getItemStack").invoke(event);
            if (!rejectInteraction(player, entity, stack)) return false;
            type.getMethod("setCanceled", boolean.class).invoke(event, true);
            type.getMethod("setCancellationResult", net.minecraft.world.InteractionResult.class)
                    .invoke(event, net.minecraft.world.InteractionResult.FAIL);
            return true;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Unsupported third-party interaction event", failure);
        }
    }

    private static UUID petEntryId(EchoBindingSavedData1211.Binding binding) {
        return UUID.nameUUIDFromBytes(("echo_warrior:pet_home:" + binding.summonerId() + ":"
                + com.yuriscat.echowarrior.compat.item.EchoRelicState1211.relicId(binding.relic()))
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** The compass can display a generation only after its existing summon transaction commits. */
    public static boolean preparePetRecord(LivingEntity living) {
        if (!(living instanceof EchoWarriorEntity1211 echo)) return true;
        if (!(living.level() instanceof ServerLevel level) || echo.getSummonerUuid() == null) return false;
        var binding = EchoBindingSavedData1211.get(level.getServer()).get(echo.getSummonerUuid());
        if (binding == null || !binding.active() || binding.generation() != echo.getBindingGeneration()
                || !living.getUUID().equals(binding.spiritId())) return false;
        for (Api api : apis()) {
            if (!api.id.equals("pet_home")) continue;
            CompoundTag tag = api.read(living);
            String id = petEntryId(binding).toString();
            if (!id.equals(string(tag, "PetId"))
                    || !binding.controllerId().toString().equals(string(tag, "EchoWarriorOwner"))) {
                tag = tag.copy();
                tag.putString("PetId", id);
                tag.putString("EchoWarriorOwner", binding.controllerId().toString());
                api.write(living, tag);
            }
        }
        return true;
    }

    /** The third-party record is a display hint. Current world authority decides every action. */
    public static boolean compassAction(net.minecraft.server.level.ServerPlayer player, Object record, boolean toPlayer) {
        try {
            String type = (String)record.getClass().getField("entityType").get(record);
            if (!type.startsWith("echo_warrior:")) return false;
            UUID id = (UUID)record.getClass().getField("petId").get(record);
            var binding = EchoBindingSavedData1211.get(player.server).bindings().stream()
                    .filter(b -> id.equals(petEntryId(b))).findFirst().orElse(null);
            if (binding == null || !player.getUUID().equals(binding.controllerId())
                    || !player.isAlive() || player.isSpectator()) {
                compassUnavailable(player);
                return true;
            }
            if (toPlayer) {
                var result = EchoExternalCompanion1211.summon(player, EchoExternalCompanion1211.entryId(binding));
                if (!result.succeeded()) compassUnavailable(player);
                return true;
            }
            // Player-to-pet may use Pet Home's safe-position logic only for a current,
            // loaded generation. Stale records never force-load an obsolete incarnation.
            UUID current = binding.spiritId();
            if (!binding.active() || current == null || !current.toString().equals(
                    (String)record.getClass().getField("entityUuid").get(record))) {
                compassUnavailable(player);
                return true;
            }
            for (var level : player.server.getAllLevels()) {
                if (level.getEntity(current) instanceof EchoWarriorEntity1211 echo
                        && echo.getBindingGeneration() == binding.generation()) return false;
            }
            compassUnavailable(player);
            return true;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unsupported pet compass record", error);
        }
    }

    private static void compassUnavailable(net.minecraft.server.level.ServerPlayer player) {
        if (player.connection != null) player.displayClientMessage(Component.translatable("message.echo_warrior.pet_compass_unavailable"), true);
    }

    public static void appendTooltip(ItemStack relic, java.util.function.Consumer<Component> out) {
        CompoundTag collars = compound(read(relic), KEY);
        for (String mod : java.util.List.of("pet_home", "domesticationinnovation")) {
            CompoundTag state = compound(collars, mod);
            if (state.isEmpty()) continue;
            out.accept(Component.translatable("tooltip.echo_warrior.pet_collar_saved")
                    .withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
            boolean limited = false;
            boolean immaturity = false;
            Tag value = state.get(ENCHANTMENTS);
            if (value instanceof ListTag list) for (Tag entry : list) {
                if (!(entry instanceof CompoundTag tag)) continue;
                String id = string(tag, "id");
                limited |= id.endsWith(":total_recall") || id.endsWith(":undead_curse");
                immaturity |= id.endsWith(":immaturity_curse");
            }
            if (limited) out.accept(Component.translatable("tooltip.echo_warrior.pet_collar_limits")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    public static boolean rejectStoredNet(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        try {
            Class<?> type = Class.forName("com.github.yzqdev.pethome.server.item.NetItem");
            CompoundTag data = (CompoundTag)type.getMethod("getEntityData", ItemStack.class).invoke(null, stack);
            if (!string(data, "id").startsWith("echo_warrior:")) return false;
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && serverPlayer.connection != null)
                serverPlayer.displayClientMessage(Component.translatable("message.echo_warrior.pet_management_restricted"), true);
            return true;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unsupported captured-pet storage API", error);
        }
    }

    public static boolean rejectNetProjectile(net.minecraft.world.entity.Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        try {
            var field = Class.forName("com.github.yzqdev.pethome.server.entity.NetEntity").getDeclaredField("itemStack");
            field.setAccessible(true);
            var stack = (ItemStack)field.get(entity);
            var owner = entity instanceof net.minecraft.world.entity.projectile.Projectile projectile ? projectile.getOwner() : null;
            if (!rejectStoredNet(stack, owner instanceof net.minecraft.world.entity.player.Player player ? player : null)) return false;
            if (entity.isRemoved() || !entity.isAlive()) return true;
            var returned = stack.copy();
            returned.setCount(1);
            var drop = new net.minecraft.world.entity.item.ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), returned);
            if (level.addFreshEntity(drop)) entity.discard();
            return true;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unsupported captured-pet projectile API", error);
        }
    }

    private static final ThreadLocal<Boolean> PACKET_PASSTHROUGH = ThreadLocal.withInitial(() -> false);

    /** Inspect entity identity on the server thread; never trust a client's collar snapshot. */
    public static boolean interceptRawPacket(Object message, Object context, String handlerType) {
        if (PACKET_PASSTHROUGH.get()) return false;
        try {
            var property = message.getClass().getDeclaredField("propertyID");
            property.setAccessible(true);
            String id = (String)property.get(message);
            if (!"PetHomeTagUpdate".equals(id) && !"CitadelTagUpdate".equals(id)
                    && !"CitadelPatreonConfig".equals(id)) return false;
            Object actual = context instanceof java.util.function.Supplier<?> supplier ? supplier.get() : context;
            Object sender;
            if (actual instanceof net.minecraft.world.entity.player.Player) sender = actual;
            else {
                Method getter;
                try { getter = actual.getClass().getMethod("getSender"); }
                catch (NoSuchMethodException otherLoader) { getter = actual.getClass().getMethod("player"); }
                getter.setAccessible(true);
                sender = getter.invoke(actual);
            }
            if (!(sender instanceof net.minecraft.server.level.ServerPlayer player)) return false;
            var entityField = message.getClass().getDeclaredField("entityID");
            entityField.setAccessible(true);
            int entityId = entityField.getInt(message);
            var server = ((ServerLevel)player.level()).getServer();
            server.execute(() -> {
                if (player.level().getEntity(entityId) instanceof EchoWarriorEntity1211) return;
                boolean previous = PACKET_PASSTHROUGH.get();
                PACKET_PASSTHROUGH.set(true);
                try {
                    for (Method method : Class.forName(handlerType).getDeclaredMethods()) {
                        if ((method.getName().equals("handleServer") || method.getName().equals("handle"))
                                && method.getParameterCount() == 2 && method.getParameterTypes()[1].isInstance(context)) {
                            method.setAccessible(true);
                            method.invoke(null, message, context);
                            return;
                        }
                    }
                    throw new NoSuchMethodException("Original pet packet handler");
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException("Unable to preserve ordinary pet packet handling", error);
                } finally { PACKET_PASSTHROUGH.set(previous); }
            });
            try {
                Method handled = actual.getClass().getMethod("setPacketHandled", boolean.class);
                handled.setAccessible(true);
                handled.invoke(actual, true);
            } catch (NoSuchMethodException notForge) { /* Fabric/NeoForge have no handled flag */ }
            return true;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unsupported pet packet authority API", error);
        }
    }

    public static UUID ownerId(net.minecraft.world.entity.Entity entity) {
        if (!(entity instanceof EchoWarriorEntity1211 echo)) return null;
        if (entity.level() instanceof ServerLevel level) {
            if (echo.getSummonerUuid() == null) return null;
            var binding = EchoBindingSavedData1211.get(level.getServer()).get(echo.getSummonerUuid());
            return binding != null && binding.active() && binding.generation() == echo.getBindingGeneration()
                    && entity.getUUID().equals(binding.spiritId()) ? binding.controllerId() : null;
        }
        UUID nativeOwner = echo.getOwnerUuid();
        if (nativeOwner != null) return nativeOwner;
        for (Api api : apis()) {
            String value = string(api.read(echo.livingEntity()), "EchoWarriorOwner");
            try { if (!value.isEmpty()) return UUID.fromString(value); }
            catch (IllegalArgumentException invalid) { /* wait for authoritative owner synchronization */ }
        }
        return null;
    }

    public static UUID comparisonOwner(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof EchoWarriorEntity1211) return ownerId(entity);
        if (entity instanceof net.minecraft.world.entity.player.Player) return entity.getUUID();
        for (Api api : apis()) {
            var owner = api.owner(entity);
            if (owner != null) return owner.getUUID();
        }
        return null;
    }

    private static int remaining(CompoundTag state, String key, long now) {
        long end = number(state, key);
        return end <= now ? 0 : (int)Math.min(Integer.MAX_VALUE, end - now);
    }

    private static List<Api> apis() {
        if (adapters != null) return adapters;
        List<Api> found = new ArrayList<>();
        discover(found, "pet_home", "com.github.yzqdev.pethome.util.TameableUtils",
                "com.github.yzqdev.pethome.util.CitadelEntityData");
        adapters = List.copyOf(found);
        return adapters;
    }

    private static void discover(List<Api> found, String id, String utility, String storage) {
        try {
            Class<?> type = Class.forName(utility, false, PetCollarCompat1211.class.getClassLoader());
            Class<?> data = Class.forName(storage, false, PetCollarCompat1211.class.getClassLoader());
            Method get = data.getDeclaredMethod("getCitadelTag", LivingEntity.class);
            Method put = data.getDeclaredMethod("setCitadelTag", LivingEntity.class, CompoundTag.class);
            Method update = type.getDeclaredMethod("onUpdateEnchants", Map.class, LivingEntity.class);
            Method healingGet = type.getDeclaredMethod("getHealingAuraTime", LivingEntity.class);
            Method healingPut = type.getDeclaredMethod("setHealingAuraTime", LivingEntity.class, int.class);
            Method owner = type.getDeclaredMethod("getOwnerOf", net.minecraft.world.entity.Entity.class);
            if (get.getReturnType() != CompoundTag.class || put.getReturnType() != void.class
                    || update.getReturnType() != void.class || healingGet.getReturnType() != int.class
                    || healingPut.getReturnType() != void.class)
                throw new NoSuchMethodException("Incompatible collar API return types");
            for (Method method : List.of(get, put, update, healingGet, healingPut, owner)) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers()))
                    throw new NoSuchMethodException("Expected static collar API: " + method.getName());
                method.setAccessible(true);
            }
            found.add(new Api(id, get, put, update, healingGet, healingPut, owner));
            LOG.info("[Pet collar compatibility] {} storage API verified", id);
        } catch (ClassNotFoundException absent) {
            // Optional dependency, including on dedicated servers.
        } catch (ReflectiveOperationException | LinkageError incompatible) {
            LOG.error("[Pet collar compatibility] {} API is unsupported; saved data is retained", id, incompatible);
        }
    }

    private record Api(String id, Method get, Method put, Method update, Method healingGet, Method healingPut, Method owner) {
        net.minecraft.world.entity.Entity owner(net.minecraft.world.entity.Entity entity) { return (net.minecraft.world.entity.Entity)invoke(owner, entity); }
        CompoundTag read(LivingEntity entity) {
            Object value = invoke(get, entity);
            return value instanceof CompoundTag tag ? tag : new CompoundTag();
        }
        void write(LivingEntity entity, CompoundTag data) { invoke(put, entity, data); }
        void refresh(LivingEntity entity) { invoke(update, null, entity); }
        int healing(LivingEntity entity) { return (Integer)invoke(healingGet, entity); }
        void healing(LivingEntity entity, int value) { invoke(healingPut, entity, value); }
        private Object invoke(Method method, Object... args) {
            try { return method.invoke(null, args); }
            catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Collar API failed: " + id + "." + method.getName(), error);
            }
        }
    }

    private static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
    private static void write(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
    private static CompoundTag compound(CompoundTag tag, String key) { return tag.getCompound(key); }
    private static boolean bool(CompoundTag tag, String key) { return tag.getBoolean(key); }
    private static int integer(CompoundTag tag, String key) { return tag.getInt(key); }
    private static long number(CompoundTag tag, String key) { return tag.getLong(key); }
    private static String string(CompoundTag tag, String key) { return tag.getString(key); }
}
