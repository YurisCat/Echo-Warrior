package com.yuriscat.echowarrior.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ShulkerBullet.class)
public interface ShulkerBulletAccessor {
	@Accessor("finalTarget")
	void echoWarrior$setFinalTarget(@Nullable EntityReference<Entity> target);

	@Invoker("selectNextMoveDirection")
	void echoWarrior$selectNextMoveDirection(Direction.@Nullable Axis avoidAxis, @Nullable Entity target);
}
