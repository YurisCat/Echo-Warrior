package com.yuriscat.echowarrior.compat;

import com.yuriscat.echowarrior.compat.knowledge.KnowledgeCatalog1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1211;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModCreativeTabs1211 {
    private static final Map<ResourceLocation, CreativeModeTab> TABS = buildTabs();

    private ModCreativeTabs1211() {
    }

    public static Map<ResourceLocation, CreativeModeTab> tabs() {
        return TABS;
    }

    private static Map<ResourceLocation, CreativeModeTab> buildTabs() {
        Map<ResourceLocation, CreativeModeTab> tabs = new LinkedHashMap<>();
        tabs.put(ModContent1211.id("echo_warrior"), CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 0)
                .title(Component.translatable("itemGroup.echo_warrior.echo_warrior"))
                .icon(() -> new ItemStack(ModContent1211.ECHO_SUMMONER))
                .displayItems((parameters, output) -> {
                    output.accept(ModContent1211.TUTORIAL_MANUAL);
                    output.accept(ModContent1211.ECHO_COMPASS);
                    output.accept(ModContent1211.ECHO_SUMMONER);
                    output.accept(ModContent1211.ECHO_RECYCLER_ITEM);
                    output.accept(ModContent1211.COURAGE_LEGACY);
                    output.accept(ModContent1211.FORTITUDE_LEGACY);
                    output.accept(ModContent1211.PURITY_LEGACY);
                    output.accept(ModContent1211.WISDOM_LEGACY);
                    output.accept(ModContent1211.CRAFT_LEGACY);
                    output.accept(ModContent1211.SUSPICIOUS_GRASS_BLOCK_ITEM);
                    output.accept(ModContent1211.SUSPICIOUS_DIRT_ITEM);
                    output.accept(ModContent1211.ROMAN_LEGIONARY_RELIC);
                    output.accept(ModContent1211.AZTEC_WARRIOR_RELIC);
                    output.accept(ModContent1211.EGYPTIAN_ARCHER_RELIC);
                    output.accept(ModContent1211.GUANDAO_WARRIOR_RELIC);
                    output.accept(ModContent1211.JAPANESE_SAMURAI_RELIC);
                }).build());
        tabs.put(ModContent1211.id("echo_warrior_accessories"), CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 1)
                .title(Component.translatable("itemGroup.echo_warrior.accessories"))
                .icon(() -> new ItemStack(ModContent1211.MEMORY_RITUAL_KNIFE_ACCESSORY))
                .displayItems((parameters, output) -> ModContent1211.accessories().forEach(output::accept))
                .build());
        tabs.put(ModContent1211.id("echo_warrior_knowledge_fragments"), CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 2)
                .title(Component.translatable("itemGroup.echo_warrior.knowledge_fragments"))
                .icon(() -> new ItemStack(ModContent1211.KNOWLEDGE_FRAGMENT_COLLECTION))
                .displayItems((parameters, output) -> {
                    LinkedHashMap<String, Integer> pages = new LinkedHashMap<>();
                    KnowledgeCatalog1211.entries().forEach(entry -> pages.put(entry.id(), 1));
                    output.accept(KnowledgeStackData1211.collection(pages, KnowledgeCatalog1211.entries().get(0).id()));
                    KnowledgeCatalog1211.entries().forEach(entry -> output.accept(KnowledgeStackData1211.fragment(entry.id())));
                }).build());
        return Map.copyOf(tabs);
    }
}
