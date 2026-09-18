package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.entity.EgyptianArcherEchoEntity1211;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class EgyptianArcherRenderer1211 extends GeoEntityRenderer<EgyptianArcherEchoEntity1211> {
    public EgyptianArcherRenderer1211(EntityRendererProvider.Context context) {
        super(context, new EgyptianArcherModel1211());
        this.shadowRadius = 0.45F;
        this.shadowStrength = 0.7F;
    }
}
