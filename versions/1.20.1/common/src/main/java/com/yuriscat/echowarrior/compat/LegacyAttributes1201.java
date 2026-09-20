package com.yuriscat.echowarrior.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Stable old-API modifier identities; never allocate a fresh UUID during reapplication. */
public final class LegacyAttributes1201 {
    private LegacyAttributes1201() {}
    public static UUID id(ResourceLocation key) { return UUID.nameUUIDFromBytes(key.toString().getBytes(StandardCharsets.UTF_8)); }
    public static AttributeModifier create(ResourceLocation key, double amount, AttributeModifier.Operation operation) {
        return new AttributeModifier(id(key), key.toString(), amount, operation);
    }
}
