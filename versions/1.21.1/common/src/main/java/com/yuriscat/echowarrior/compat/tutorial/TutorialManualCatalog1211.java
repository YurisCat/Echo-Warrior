package com.yuriscat.echowarrior.compat.tutorial;

import com.yuriscat.echowarrior.compat.ModContent1211;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class TutorialManualCatalog1211 {
    private static final List<String> ACCESSORY_IDS = List.of(
            "plate_armor_accessory", "chainmail_armor_accessory", "spiked_armor_accessory",
            "battle_worn_whetstone_accessory", "mountain_burden_blade_accessory",
            "fractured_crystal_blade_accessory", "twin_oath_badge_accessory", "battle_blindfold_accessory",
            "crack_ring_hammer_charm_accessory", "victors_laurel_accessory", "blood_pact_fang_accessory",
            "memory_ritual_knife_accessory", "substitute_doll_accessory", "heart_sprout_amber_accessory",
            "feast_ham_accessory", "peacemaker_accessory", "sunwheel_garland_accessory",
            "moondew_bottle_accessory", "tomato_fish_accessory", "cat_bell_fish_charm_accessory",
            "light_gathering_magnet_accessory", "training_notes_accessory", "hawkeye_lens_accessory",
            "windchaser_feather_accessory", "hollow_bird_bone_accessory");
    private static final List<Page> PAGES = buildPages();

    private TutorialManualCatalog1211() {
    }

    public static List<Page> pages() { return PAGES; }
    public static List<String> accessoryIds() { return ACCESSORY_IDS; }
    public static int pageCount() { return PAGES.size(); }
    public static Page page(int index) { return PAGES.get(Math.clamp(index, 0, PAGES.size() - 1)); }

    public static int indexOf(String id) {
        for (int index = 0; index < PAGES.size(); index++) {
            if (PAGES.get(index).id().equals(id)) return index;
        }
        return 0;
    }

    public static int firstPage(Chapter chapter) {
        for (int index = 0; index < PAGES.size(); index++) {
            if (PAGES.get(index).chapter() == chapter) return index;
        }
        return 0;
    }

    private static List<Page> buildPages() {
        List<Page> pages = new ArrayList<>();
        pages.add(new Page("cover", Chapter.TITLE, PageKind.COVER, "", 0));
        pages.add(new Page("introduction", Chapter.TITLE, PageKind.PROSE, "", 5));
        pages.add(new Page("battlefield", Chapter.SEARCH, PageKind.PROSE, "", 3));
        pages.add(new Page("compass_and_brush", Chapter.SEARCH, PageKind.RECIPE, "echo_compass", 4));
        pages.add(new Page("discoveries", Chapter.SEARCH, PageKind.DISCOVERIES, "", 4));
        pages.add(new Page("knowledge_fragments", Chapter.SEARCH, PageKind.KNOWLEDGE, "", 5));
        pages.add(new Page("legacy", Chapter.SEARCH, PageKind.LEGACY, "", 3));
        pages.add(new Page("recycler", Chapter.SEARCH, PageKind.RECIPE, "echo_recycler", 5));
        pages.add(new Page("summoner", Chapter.HEROES, PageKind.RECIPE, "test_echo_summoner", 6));
        pages.add(new Page("roman_legionary", Chapter.HEROES, PageKind.HERO, "roman_legionary", 3));
        pages.add(new Page("aztec_warrior", Chapter.HEROES, PageKind.HERO, "aztec_warrior", 4));
        pages.add(new Page("egyptian_archer", Chapter.HEROES, PageKind.HERO, "egyptian_archer", 4));
        pages.add(new Page("guandao_warrior", Chapter.HEROES, PageKind.HERO, "guandao_warrior", 4));
        pages.add(new Page("japanese_samurai", Chapter.HEROES, PageKind.HERO, "japanese_samurai", 4));
        pages.add(new Page("accessories_overview", Chapter.ACCESSORIES, PageKind.PROSE, "", 3));
        ACCESSORY_IDS.forEach(id -> pages.add(new Page(id, Chapter.ACCESSORIES, PageKind.ACCESSORY, id, 0)));
        pages.add(new Page("thanks", Chapter.CREDITS, PageKind.THANKS, "", 0));
        pages.add(new Page("team", Chapter.CREDITS, PageKind.TEAM, "", 0));
        pages.add(new Page("sources_1", Chapter.CREDITS, PageKind.SOURCES, "", 6));
        pages.add(new Page("sources_2", Chapter.CREDITS, PageKind.SOURCES, "", 6));
        return List.copyOf(pages);
    }

    public record Page(String id, Chapter chapter, PageKind kind, String subjectId, int paragraphCount) {
        public String titleKey() { return "gui.echo_warrior.tutorial.page." + this.id + ".title"; }
        public String paragraphKey(int index) {
            return "gui.echo_warrior.tutorial.page." + this.id + ".body." + (index + 1);
        }
    }

    public enum PageKind {
        COVER, PROSE, RECIPE, DISCOVERIES, KNOWLEDGE, LEGACY, HERO, ACCESSORY, THANKS, TEAM, SOURCES
    }

    public enum Chapter {
        TITLE("title", ModContent1211.id("tutorial_manual")),
        SEARCH("search", ModContent1211.id("echo_compass")),
        HEROES("heroes", ModContent1211.id("test_echo_summoner")),
        ACCESSORIES("accessories", ModContent1211.id("memory_ritual_knife_accessory")),
        CREDITS("credits", ResourceLocation.withDefaultNamespace("egg"));

        private final String id;
        private final ResourceLocation icon;

        Chapter(String id, ResourceLocation icon) {
            this.id = id;
            this.icon = icon;
        }

        public String id() { return this.id; }
        public ResourceLocation icon() { return this.icon; }
        public String titleKey() { return "gui.echo_warrior.tutorial.chapter." + this.id + ".title"; }
        public String subtitleKey() { return "gui.echo_warrior.tutorial.chapter." + this.id + ".subtitle"; }
    }
}
