package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.entity.EgyptianArcherArrowEntity1211;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class EgyptianArcherArrowRenderer1211 extends ArrowRenderer<EgyptianArcherArrowEntity1211> {
    private static final ResourceLocation ARROW_TEXTURE = ResourceLocation.withDefaultNamespace(
            "textures/entity/projectiles/arrow.png");

    public EgyptianArcherArrowRenderer1211(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EgyptianArcherArrowEntity1211 entity) {
        return ARROW_TEXTURE;
    }
}
