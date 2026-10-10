package com.yuriscat.echowarrior.compat.client;

import com.mojang.blaze3d.vertex.*;
import com.yuriscat.echowarrior.compat.*;
import com.yuriscat.echowarrior.compat.item.EchoCompassItem1201;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;
import java.util.stream.Collectors;

/** Real baked overrides, registered colors, stitched material, and custom item-renderer vertices. */
public final class ExplorationClientSelfTest1201 {
    private static boolean done;
    private ExplorationClientSelfTest1201() {}

    public static boolean tick(Minecraft client) {
        if (done) return true;
        var compass = new ItemStack(ModContent1201.ECHO_COMPASS);
        var colors = ((com.yuriscat.echowarrior.compat.mixin.ClientColorsAccessor1201)client).echoWarrior$itemColors();
        BlockPos playerPos = client.player.blockPosition();
        EchoCompassItem1201.writeTracking(compass, EchoCompassItem1201.MODE_OUTSIDE, playerPos.north(100).asLong());
        Set<String> north = sprites(client, compass);
        EchoCompassItem1201.writeTracking(compass, EchoCompassItem1201.MODE_OUTSIDE, playerPos.east(100).asLong());
        check(!north.equals(sprites(client, compass)), "pointer rotates toward target");
        check(sprites(client, compass).stream().anyMatch(s -> s.endsWith("echo_compass_frame_copper")), "enabled copper frame");
        compass.getOrCreateTag().putBoolean("EchoWarriorCompassSoundEnabled", false);
        check(sprites(client, compass).stream().anyMatch(s -> s.endsWith("echo_compass_frame_iron")), "muted iron frame");
        EchoCompassItem1201.writeTracking(compass, EchoCompassItem1201.MODE_INNER, playerPos.asLong());
        check(sprites(client, compass).stream().anyMatch(s -> s.endsWith("echo_compass_frame_gold")), "nearby gold frame");
        check(colors.getColor(compass, 1) == 0xFF5AEEFF
                && colors.getColor(compass, 0) == -1, "registered cyan pointer tint only");
        int grass = client.getBlockColors().getColor(Blocks.GRASS_BLOCK.defaultBlockState(), client.level, playerPos, 0);
        check(client.getBlockColors().getColor(ModContent1201.SUSPICIOUS_GRASS_BLOCK.defaultBlockState(),
                client.level, playerPos, 0) == grass, "biome grass tint matches vanilla");
        check(colors.getColor(new ItemStack(ModContent1201.SUSPICIOUS_GRASS_BLOCK_ITEM), 0)
                == net.minecraft.world.level.GrassColor.getDefaultColor(), "inventory grass tint");
        var material = client.getTextureAtlas(Sheets.CHEST_SHEET).apply(ModContent1201.id("entity/chest/recycler"));
        check(material.contents().name().equals(ModContent1201.id("entity/chest/recycler")), "recycler chest atlas material");
        ModEffects1201.effects().forEach((id, effect) -> check(client.getMobEffectTextures().get(effect).contents().name().equals(id),
                "registered effect sprite is stitched: " + id));
        var recycler = new ItemStack(ModContent1201.ECHO_RECYCLER_ITEM);
        // Additional GUI passes (for example item shadows) are not chest geometry.
        // Keep the exact geometry assertion on the material used by our renderer.
        var passes = new java.util.LinkedHashMap<RenderType, CountingVertices>();
        client.getItemRenderer().render(recycler, ItemDisplayContext.GUI, false, new PoseStack(),
                type -> passes.computeIfAbsent(type, ignored -> new CountingVertices()),
                LightTexture.FULL_BRIGHT, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                client.getItemRenderer().getModel(recycler, client.level, client.player, 0));
        var primary = RenderType.entityCutout(Sheets.CHEST_SHEET);
        int primaryVertices = passes.containsKey(primary) ? passes.get(primary).count : 0;
        int auxiliaryVertices = passes.entrySet().stream().filter(entry -> entry.getKey() != primary)
                .mapToInt(entry -> entry.getValue().count).sum();
        EchoWarrior1201.LOGGER.info("[Compat1201] RECYCLER RENDER PASSES primary={} auxiliary={} passes={}",
                primaryVertices, auxiliaryVertices,
                passes.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue().count).toList());
        check(primaryVertices == 72, "recycler item renders bottom, lid and latch in its material pass: " + primaryVertices);
        var brushable = new net.minecraft.world.level.block.entity.BrushableBlockEntity(playerPos,
                ModContent1201.SUSPICIOUS_GRASS_BLOCK.defaultBlockState());
        check(client.getBlockEntityRenderDispatcher().getRenderer(brushable) != null, "vanilla brushing renderer registered");
        var graphics = new GuiGraphics(client, client.renderBuffers().bufferSource());
        graphics.renderItem(recycler, -100, -100);
        graphics.renderItem(compass, -100, -100);
        graphics.renderItem(new ItemStack(ModContent1201.SUSPICIOUS_GRASS_BLOCK_ITEM), -100, -100);
        graphics.flush();
        done = true;
        EchoWarrior1201.LOGGER.info("[Compat1201] EXPLORATION CLIENT SELFTEST PASSED compass=overrides-and-tint grass=biome recycler=72-vertices-and-atlas brushing=renderer");
        return true;
    }

    private static Set<String> sprites(Minecraft client, ItemStack stack) {
        return client.getItemRenderer().getModel(stack, client.level, client.player, 0)
                .getQuads(null, null, RandomSource.create(1)).stream()
                .map(quad -> quad.getSprite().contents().name().toString()).collect(Collectors.toSet());
    }
    private static void check(boolean value, String label) {
        if (!value) throw new IllegalStateException("Exploration client: " + label);
    }
    private static final class CountingVertices implements VertexConsumer {
        int count;
        public VertexConsumer vertex(double x, double y, double z) { return this; }
        public VertexConsumer color(int r, int g, int b, int a) { return this; }
        public VertexConsumer uv(float u, float v) { return this; }
        public VertexConsumer overlayCoords(int u, int v) { return this; }
        public VertexConsumer uv2(int u, int v) { return this; }
        public VertexConsumer normal(float x, float y, float z) { return this; }
        public void endVertex() { count++; }
        public void defaultColor(int r, int g, int b, int a) {}
        public void unsetDefaultColor() {}
    }
}
