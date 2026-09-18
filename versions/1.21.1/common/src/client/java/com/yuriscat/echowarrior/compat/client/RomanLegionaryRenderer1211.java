package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1211;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class RomanLegionaryRenderer1211 extends GeoEntityRenderer<RomanLegionaryEchoEntity1211> {
    public RomanLegionaryRenderer1211(EntityRendererProvider.Context context) {
        super(context, new RomanLegionaryModel1211());
        this.shadowRadius = 0.45F;
        this.shadowStrength = 0.7F;
    }
}
