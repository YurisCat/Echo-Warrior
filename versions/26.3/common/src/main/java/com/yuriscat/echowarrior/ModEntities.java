package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.entity.RomanLegionaryEchoEntity;
import com.yuriscat.echowarrior.entity.AztecWarriorEchoEntity;
import com.yuriscat.echowarrior.entity.EgyptianArcherEchoEntity;
import com.yuriscat.echowarrior.entity.EgyptianArcherArrowEntity;
import com.yuriscat.echowarrior.entity.GuandaoWarriorEchoEntity;
import com.yuriscat.echowarrior.entity.JapaneseSamuraiEchoEntity;
import com.yuriscat.echowarrior.platform.RegistryRegistrar;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class ModEntities {
	private static final Map<Identifier, EntityType<?>> ENTITIES = new LinkedHashMap<>();
	public static final EntityType<RomanLegionaryEchoEntity> ROMAN_LEGIONARY_ECHO = register(
			"roman_legionary_echo",
			EntityType.Builder.of(RomanLegionaryEchoEntity::new, MobCategory.CREATURE)
					.sized(0.75F, 1.95F)
					.eyeHeight(1.75F)
					.noLootTable()
					.noSave()
					.clientTrackingRange(10)
	);
	public static final EntityType<AztecWarriorEchoEntity> AZTEC_WARRIOR_ECHO = register(
			"aztec_warrior_echo",
			EntityType.Builder.of(AztecWarriorEchoEntity::new, MobCategory.CREATURE)
					.sized(0.8F, 2.0F)
					.eyeHeight(1.78F)
					.noLootTable()
					.noSave()
					.clientTrackingRange(10)
	);
	public static final EntityType<EgyptianArcherEchoEntity> EGYPTIAN_ARCHER_ECHO = register(
			"egyptian_archer_echo",
			EntityType.Builder.of(EgyptianArcherEchoEntity::new, MobCategory.CREATURE)
					.sized(0.75F, 1.95F)
					.eyeHeight(1.74F)
					.noLootTable()
					.noSave()
					.clientTrackingRange(10)
	);
	public static final EntityType<GuandaoWarriorEchoEntity> GUANDAO_WARRIOR_ECHO = register(
			"guandao_warrior_echo",
			EntityType.Builder.of(GuandaoWarriorEchoEntity::new, MobCategory.CREATURE)
					.sized(0.85F, 2.1F)
					.eyeHeight(1.85F)
					.noLootTable()
					.noSave()
					.clientTrackingRange(10)
	);
	public static final EntityType<JapaneseSamuraiEchoEntity> JAPANESE_SAMURAI_ECHO = register(
			"japanese_samurai_echo",
			EntityType.Builder.of(JapaneseSamuraiEchoEntity::new, MobCategory.CREATURE)
					.sized(0.75F, 1.95F)
					.eyeHeight(1.75F)
					.noLootTable()
					.noSave()
					.clientTrackingRange(10)
	);
	public static final EntityType<EgyptianArcherArrowEntity> EGYPTIAN_ARCHER_ARROW = register(
			"egyptian_archer_arrow",
			EntityType.Builder.of(EgyptianArcherArrowEntity::new, MobCategory.MISC)
					.sized(0.5F, 0.5F)
					.noSave()
					.clientTrackingRange(4)
					.updateInterval(1)
	);

	private ModEntities() {
	}

	public static void register(RegistryRegistrar<EntityType<?>> registrar) {
		ENTITIES.forEach(registrar::register);
	}

	public static void registerAttributes(BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier> registrar) {
		registrar.accept(ROMAN_LEGIONARY_ECHO, RomanLegionaryEchoEntity.createAttributes().build());
		registrar.accept(AZTEC_WARRIOR_ECHO, AztecWarriorEchoEntity.createAttributes().build());
		registrar.accept(EGYPTIAN_ARCHER_ECHO, EgyptianArcherEchoEntity.createAttributes().build());
		registrar.accept(GUANDAO_WARRIOR_ECHO, GuandaoWarriorEchoEntity.createAttributes().build());
		registrar.accept(JAPANESE_SAMURAI_ECHO, JapaneseSamuraiEchoEntity.createAttributes().build());
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
		Identifier id = EchoWarrior.id(path);
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		EntityType<T> type = builder.build(key);
		ENTITIES.put(id, type);
		return type;
	}
}
