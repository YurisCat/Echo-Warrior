package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.entity.AztecWarriorEchoEntity1211;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class AztecWarriorRenderer1211 extends GeoEntityRenderer<AztecWarriorEchoEntity1211> {
    public AztecWarriorRenderer1211(EntityRendererProvider.Context context) {
        super(context, new AztecWarriorModel1211());
        this.shadowRadius = 0.45F;
        this.shadowStrength = 0.7F;
    }
}
