package com.yuriscat.echowarrior.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yuriscat.echowarrior.compat.entity.GuandaoWarriorEchoEntity1211;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class GuandaoWarriorRenderer1211 extends GeoEntityRenderer<GuandaoWarriorEchoEntity1211> {
    private long lastValorParticleTick = Long.MIN_VALUE;

    public GuandaoWarriorRenderer1211(EntityRendererProvider.Context context) {
        super(context, new GuandaoWarriorModel1211());
        this.shadowRadius = 0.5F;
        this.shadowStrength = 0.75F;
    }

    @Override
    public void render(GuandaoWarriorEchoEntity1211 entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        int stacks = entity.getValorStacks();
        long gameTime = entity.level().getGameTime();
        int interval = stacks >= 5 ? 1 : stacks == 4 ? 2 : stacks == 3 ? 3 : stacks == 2 ? 4 : 6;
        if (stacks <= 0 || gameTime == this.lastValorParticleTick || gameTime % interval != 0) return;
        this.lastValorParticleTick = gameTime;
        Vec3 forward = entity.getLookAngle().multiply(0.65, 0.0, 0.65);
        double x = entity.getX() + forward.x;
        double y = entity.getY() + 1.15;
        double z = entity.getZ() + forward.z;
        ParticleOptions particle = stacks >= 5
                ? new DustParticleOptions(new Vector3f(1.0F, 0.45F, 0.14F), 0.72F)
                : ParticleTypes.FLAME;
        entity.level().addParticle(particle, x, y, z, 0.0, 0.015, 0.0);
    }
}
