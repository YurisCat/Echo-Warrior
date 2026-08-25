package com.yuriscat.echowarrior;

import com.yuriscat.echowarrior.entity.RomanLegionaryEchoEntity;
import com.yuriscat.echowarrior.entity.AztecWarriorEchoEntity;
import com.yuriscat.echowarrior.entity.EgyptianArcherEchoEntity;
import com.yuriscat.echowarrior.entity.EgyptianArcherArrowEntity;
import com.yuriscat.echowarrior.entity.GuandaoWarriorEchoEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
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

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(ROMAN_LEGIONARY_ECHO, RomanLegionaryEchoEntity.createAttributes().build());
		FabricDefaultAttributeRegistry.register(AZTEC_WARRIOR_ECHO, AztecWarriorEchoEntity.createAttributes().build());
		FabricDefaultAttributeRegistry.register(EGYPTIAN_ARCHER_ECHO, EgyptianArcherEchoEntity.createAttributes().build());
		FabricDefaultAttributeRegistry.register(GUANDAO_WARRIOR_ECHO, GuandaoWarriorEchoEntity.createAttributes().build());
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
		Identifier id = EchoWarrior.id(path);
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(key));
	}
}
