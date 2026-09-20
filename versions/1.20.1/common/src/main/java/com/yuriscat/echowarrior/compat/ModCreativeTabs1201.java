package com.yuriscat.echowarrior.compat;

import com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1201.AccessoryType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs1201 {
    private ModCreativeTabs1201() {}
    public static CreativeModeTab echoes(CreativeModeTab.Builder builder) {
        return builder.title(Component.translatable("itemGroup.echo_warrior.echo_warrior"))
                .icon(() -> new ItemStack(ModContent1201.ECHO_SUMMONER))
                .displayItems((parameters, output) -> {
                    output.accept(ModContent1201.ECHO_SUMMONER);
                    output.accept(ModContent1201.ECHO_COMPASS);
                    output.accept(ModContent1201.SUSPICIOUS_GRASS_BLOCK_ITEM);
                    output.accept(ModContent1201.SUSPICIOUS_DIRT_ITEM);
                    output.accept(ModContent1201.TUTORIAL_MANUAL);
                    output.accept(ModContent1201.ECHO_RECYCLER_ITEM);
                    for (var hero : EchoHeroType1201.values()) output.accept(ModContent1201.relic(hero));
                    for (var legacy : java.util.List.of(ModContent1201.COURAGE_LEGACY, ModContent1201.FORTITUDE_LEGACY,
                            ModContent1201.PURITY_LEGACY, ModContent1201.WISDOM_LEGACY, ModContent1201.CRAFT_LEGACY)) output.accept(legacy);
                }).build();
    }
    public static CreativeModeTab accessories(CreativeModeTab.Builder builder) {
        return builder.title(Component.translatable("itemGroup.echo_warrior.accessories"))
                .icon(() -> new ItemStack(ModContent1201.accessory(AccessoryType.MEMORY_RITUAL_KNIFE)))
                .displayItems((parameters, output) -> {
                    for (var type : AccessoryType.values()) output.accept(ModContent1201.accessory(type));
                }).build();
    }
    public static CreativeModeTab knowledge(CreativeModeTab.Builder builder) {
        return builder.title(Component.translatable("itemGroup.echo_warrior.knowledge_fragments"))
                .icon(() -> new ItemStack(ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION))
                .displayItems((parameters, output) -> {
                    var pages = new java.util.LinkedHashMap<String, Integer>();
                    com.yuriscat.echowarrior.compat.knowledge.KnowledgeCatalog1201.entries()
                            .forEach(entry -> pages.put(entry.id(), 1));
                    output.accept(com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1201.collection(
                            pages, pages.keySet().iterator().next()));
                    for (var entry : com.yuriscat.echowarrior.compat.knowledge.KnowledgeCatalog1201.entries()) {
                        output.accept(com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1201.fragment(entry.id()));
                    }
                }).build();
    }
}
