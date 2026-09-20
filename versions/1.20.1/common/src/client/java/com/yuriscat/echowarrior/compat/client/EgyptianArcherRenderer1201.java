package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.entity.EgyptianArcherEchoEntity1201;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class EgyptianArcherRenderer1201 extends GeoEntityRenderer<EgyptianArcherEchoEntity1201> {
    public EgyptianArcherRenderer1201(EntityRendererProvider.Context context) {
        super(context, new EgyptianArcherModel1201());
        this.shadowRadius = 0.45F;
        this.shadowStrength = 0.7F;
    }
}
