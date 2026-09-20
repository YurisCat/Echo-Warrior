package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.entity.EgyptianArcherArrowEntity1201;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class EgyptianArcherArrowRenderer1201 extends ArrowRenderer<EgyptianArcherArrowEntity1201> {
    private static final ResourceLocation ARROW_TEXTURE = new ResourceLocation("minecraft",
            "textures/entity/projectiles/arrow.png");

    public EgyptianArcherArrowRenderer1201(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EgyptianArcherArrowEntity1201 entity) {
        return ARROW_TEXTURE;
    }
}
