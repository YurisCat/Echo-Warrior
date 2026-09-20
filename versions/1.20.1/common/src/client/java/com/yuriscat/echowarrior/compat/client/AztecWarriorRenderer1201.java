package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.entity.AztecWarriorEchoEntity1201;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class AztecWarriorRenderer1201 extends GeoEntityRenderer<AztecWarriorEchoEntity1201> {
    public AztecWarriorRenderer1201(EntityRendererProvider.Context context) {
        super(context, new AztecWarriorModel1201());
        this.shadowRadius = 0.45F;
        this.shadowStrength = 0.7F;
    }
}
