package com.yuriscat.echowarrior.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.yuriscat.echowarrior.compat.entity.JapaneseSamuraiEchoEntity1211;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * GeckoLib 4 renderer with model-shaped samurai afterimages.
 *
 * <p>Minecraft 1.21.1 does not expose the frozen-bone render pipeline used by
 * the 26.1 branch, so this renderer reuses the pose evaluated for the current
 * frame. The server-authored particle trail remains a fallback, while the
 * synchronized event origin, direction, colour and lifetime are preserved.</p>
 */
public final class JapaneseSamuraiRenderer1211 extends GeoEntityRenderer<JapaneseSamuraiEchoEntity1211> {
    private static final int PER_SAMURAI_LIMIT = 8;
    private static final int GLOBAL_LIMIT = 64;
    private static final int ZANSHIN_RGB = 0x38BFEF;
    private static final int FUMIKOMI_RGB = 0xE9A72F;

    private final Map<Integer, Integer> lastSequences = new HashMap<>();
    private final List<Afterimage> afterimages = new ArrayList<>();

    public JapaneseSamuraiRenderer1211(EntityRendererProvider.Context context) {
        super(context, new JapaneseSamuraiModel1211());
        this.shadowRadius = 0.45F;
        this.shadowStrength = 0.7F;
    }

    @Override
    public void renderFinal(
            PoseStack poseStack,
            JapaneseSamuraiEchoEntity1211 samurai,
            BakedGeoModel model,
            MultiBufferSource bufferSource,
            @Nullable VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour
    ) {
        super.renderFinal(poseStack, samurai, model, bufferSource, buffer, partialTick,
                packedLight, packedOverlay, colour);

        double now = samurai.level().getGameTime() + partialTick;
        pruneExpired(now);
        captureAfterimageEvent(samurai);

        Vec3 currentPosition = new Vec3(
                Mth.lerp(partialTick, samurai.xo, samurai.getX()),
                Mth.lerp(partialTick, samurai.yo, samurai.getY()),
                Mth.lerp(partialTick, samurai.zo, samurai.getZ()));
        float currentYaw = Mth.rotLerp(partialTick, samurai.yBodyRotO, samurai.yBodyRot);
        RenderType renderType = RenderType.entityTranslucent(getTextureLocation(samurai));

        for (Afterimage afterimage : List.copyOf(this.afterimages)) {
            if (afterimage.entityId != samurai.getId()) continue;
            float life = (float)((now - afterimage.createdAt) / afterimage.lifetime);
            if (life < 0.0F || life >= 1.0F) continue;

            int alpha = Mth.clamp(Math.round(Mth.lerp(life, afterimage.startAlpha, 0.0F) * 255.0F),
                    0, 255);
            if (alpha <= 0) continue;
            int rgb = afterimage.neutral ? 0xFFFFFF
                    : afterimage.kind == JapaneseSamuraiEchoEntity1211.AFTERIMAGE_FUMIKOMI
                            ? FUMIKOMI_RGB : ZANSHIN_RGB;

            poseStack.pushPose();
            Vec3 offset = afterimage.position.subtract(currentPosition);
            poseStack.translate(offset.x, offset.y, offset.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(currentYaw - afterimage.yaw));
            float stretch = 1.0F + (1.0F - life) * 0.018F;
            poseStack.scale(stretch, 1.0F - (1.0F - life) * 0.008F, stretch);
            VertexConsumer afterimageBuffer = bufferSource.getBuffer(renderType);
            reRender(model, poseStack, bufferSource, samurai, renderType, afterimageBuffer,
                    partialTick, packedLight, packedOverlay, alpha << 24 | rgb);
            poseStack.popPose();
        }
    }

    private void captureAfterimageEvent(JapaneseSamuraiEchoEntity1211 samurai) {
        int entityId = samurai.getId();
        int sequence = samurai.afterimageSequence();
        int previous = this.lastSequences.getOrDefault(entityId, 0);
        if (sequence == previous) return;
        this.lastSequences.put(entityId, sequence);

        byte kind = samurai.afterimageKind();
        Vec3 origin = samurai.afterimageOrigin();
        Vec3 direction = samurai.afterimageDirection();
        float yaw = samurai.afterimageYaw();
        boolean neutral = samurai.isAfterimageNeutral();
        double createdAt = samurai.level().getGameTime();

        if (kind == JapaneseSamuraiEchoEntity1211.AFTERIMAGE_ZANSHIN_PHANTOM) {
            for (int stage = 0; stage < 3; stage++) {
                addAfterimage(new Afterimage(entityId, origin.add(direction.scale(0.35 * (stage + 1))),
                        yaw, kind, neutral, createdAt, 8.0F - stage, 0.48F - stage * 0.08F));
            }
        } else {
            addAfterimage(new Afterimage(entityId, origin, yaw, kind, neutral, createdAt, 10.0F,
                    kind == JapaneseSamuraiEchoEntity1211.AFTERIMAGE_FUMIKOMI ? 0.42F : 0.46F));
        }
    }

    private void addAfterimage(Afterimage afterimage) {
        this.afterimages.add(afterimage);
        while (countForEntity(afterimage.entityId) > PER_SAMURAI_LIMIT) {
            for (Iterator<Afterimage> iterator = this.afterimages.iterator(); iterator.hasNext();) {
                if (iterator.next().entityId == afterimage.entityId) {
                    iterator.remove();
                    break;
                }
            }
        }
        while (this.afterimages.size() > GLOBAL_LIMIT) this.afterimages.remove(0);
    }

    private int countForEntity(int entityId) {
        int count = 0;
        for (Afterimage afterimage : this.afterimages) {
            if (afterimage.entityId == entityId) count++;
        }
        return count;
    }

    private void pruneExpired(double now) {
        this.afterimages.removeIf(afterimage -> now - afterimage.createdAt >= afterimage.lifetime);
        if (this.lastSequences.size() > 256) this.lastSequences.clear();
    }

    private record Afterimage(
            int entityId,
            Vec3 position,
            float yaw,
            byte kind,
            boolean neutral,
            double createdAt,
            float lifetime,
            float startAlpha
    ) {}
}
