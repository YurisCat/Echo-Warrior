package com.yuriscat.echowarrior.compat.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ShulkerBullet.class)
public interface ShulkerBulletAccessor1211 {
    @Accessor("finalTarget")
    Entity echoWarrior1211$getFinalTarget();

    @Accessor("finalTarget")
    void echoWarrior1211$setFinalTarget(Entity target);

    @Invoker("selectNextMoveDirection")
    void echoWarrior1211$selectNextMoveDirection(Direction.Axis avoidAxis);
}
