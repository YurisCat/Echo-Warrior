package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.entity.RomanLegionaryEchoEntity1201;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class RomanLegionaryRenderer1201 extends GeoEntityRenderer<RomanLegionaryEchoEntity1201> {
    public RomanLegionaryRenderer1201(EntityRendererProvider.Context context) {
        super(context, new RomanLegionaryModel1201());
        this.shadowRadius = 0.45F;
        this.shadowStrength = 0.7F;
    }
}
