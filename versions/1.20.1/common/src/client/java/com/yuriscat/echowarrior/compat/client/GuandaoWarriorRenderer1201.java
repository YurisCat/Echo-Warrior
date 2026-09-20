package com.yuriscat.echowarrior.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yuriscat.echowarrior.compat.entity.GuandaoWarriorEchoEntity1201;
import com.yuriscat.echowarrior.compat.entity.GuandaoValorParticles1201;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class GuandaoWarriorRenderer1201 extends GeoEntityRenderer<GuandaoWarriorEchoEntity1201> {
    private final GuandaoValorParticles1201 valorParticles = new GuandaoValorParticles1201();

    public GuandaoWarriorRenderer1201(EntityRendererProvider.Context context) {
        super(context, new GuandaoWarriorModel1201());
        this.shadowRadius = 0.5F;
        this.shadowStrength = 0.75F;
    }

    @Override
    public void renderRecursively(PoseStack poseStack, GuandaoWarriorEchoEntity1201 entity,
                                  GeoBone bone, RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        boolean anchor = "WeaponParticleAnchor".equals(bone.getName());
        // Enable BEFORE this bone is traversed, then consume its current-frame world matrix.
        if (anchor) bone.setTrackingMatrices(true);
        super.renderRecursively(poseStack, entity, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        if (!anchor || isReRender) return;
        int stacks = entity.getValorStacks();
        long gameTime = entity.level().getGameTime();
        var emission = this.valorParticles.claim(entity.getUUID(), gameTime, stacks,
                !entity.isRemoved() && entity.level().getEntity(entity.getId()) == entity);
        if (emission.flames() == 0) return;
        var position = bone.getWorldPosition();
        var random = entity.getRandom();
        for (int i = 0; i < emission.flames(); i++) {
            double spread = stacks >= 3 ? 0.055 : 0.035;
            entity.level().addParticle(ParticleTypes.FLAME,
                    position.x + (random.nextDouble() - 0.5) * spread,
                    position.y + (random.nextDouble() - 0.5) * spread,
                    position.z + (random.nextDouble() - 0.5) * spread,
                    (random.nextDouble() - 0.5) * 0.015,
                    0.012 + random.nextDouble() * 0.018,
                    (random.nextDouble() - 0.5) * 0.015);
        }
        double phase = random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < emission.sparks(); i++) {
            double angle = phase + i * Math.PI * 0.25;
            double radialX = Math.cos(angle), radialZ = Math.sin(angle);
            int color = i % 2 == 0 ? 0xE43A1A : 0xFF9A24;
            var spark = new DustParticleOptions(new Vector3f(((color >> 16) & 255) / 255.0F,
                    ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F), 0.72F);
            entity.level().addParticle(spark,
                    position.x + radialX * 0.045,
                    position.y + (random.nextDouble() - 0.5) * 0.045,
                    position.z + radialZ * 0.045,
                    radialX * 0.025, 0.018 + random.nextDouble() * 0.012, radialZ * 0.025);
        }
    }
}
