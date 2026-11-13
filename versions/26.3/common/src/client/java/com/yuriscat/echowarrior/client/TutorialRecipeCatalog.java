package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TutorialRecipeCatalog {
	private static final List<Identifier> PLANKS = vanilla(
			"oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks",
			"dark_oak_planks", "mangrove_planks", "cherry_planks", "bamboo_planks", "pale_oak_planks");
	private static final List<Identifier> FISHES = vanilla("cod", "salmon", "tropical_fish", "pufferfish");
	private static final List<Identifier> WOOL = vanilla(
			"white_wool", "orange_wool", "magenta_wool", "light_blue_wool", "yellow_wool", "lime_wool",
			"pink_wool", "gray_wool", "light_gray_wool", "cyan_wool", "purple_wool", "blue_wool",
			"brown_wool", "green_wool", "red_wool", "black_wool");
	private static final List<Identifier> LEAVES = vanilla(
			"oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves", "acacia_leaves",
			"dark_oak_leaves", "mangrove_leaves", "cherry_leaves", "azalea_leaves",
			"flowering_azalea_leaves", "pale_oak_leaves");

	private static final Map<String, RecipeSpec> RECIPES = buildRecipes();

	private TutorialRecipeCatalog() {
	}

	static RecipeSpec recipe(String id) {
		return RECIPES.get(id);
	}

	private static Map<String, RecipeSpec> buildRecipes() {
		Map<String, RecipeSpec> recipes = new LinkedHashMap<>();
		put(recipes, "echo_compass", rows("CAC", "AXA", "CAC"),
				key('C', vanilla("copper_ingot"), 'A', vanilla("amethyst_shard"), 'X', vanilla("compass")));
		put(recipes, "echo_recycler", rows("IPI", "PKP", "IPI"),
				key('I', vanilla("iron_ingot"), 'P', PLANKS, 'K', mod("knowledge_fragment")));
		put(recipes, "test_echo_summoner", rows("IGI", "IEI", "ICI"),
				key('I', vanilla("iron_ingot"), 'G', vanilla("glass_pane"), 'E', vanilla("ender_pearl"),
						'C', vanilla("chest")));

		put(recipes, "battle_blindfold_accessory", rows("WWW", "W W", "CWT"),
				key('W', vanilla("cyan_wool"), 'C', mod("courage_legacy"), 'T', mod("craft_legacy")));
		put(recipes, "battle_worn_whetstone_accessory", rows("  C", "FI ", "SS "),
				key('C', mod("courage_legacy"), 'F', vanilla("flint"), 'I', vanilla("iron_ingot"), 'S', vanilla("stone")));
		put(recipes, "blood_pact_fang_accessory", rows("GCG", "BPB", " B "),
				key('G', vanilla("ghast_tear"), 'C', mod("courage_legacy"), 'B', vanilla("bone"), 'P', mod("purity_legacy")));
		put(recipes, "cat_bell_fish_charm_accessory", rows(" F ", "CEC", " C "),
				key('F', FISHES, 'C', mod("craft_legacy"), 'E', vanilla("emerald")));
		put(recipes, "chainmail_armor_accessory", rows("WWW", "FCF", "WWW"),
				key('W', WOOL, 'F', mod("fortitude_legacy"), 'C', vanilla("iron_chestplate")));
		put(recipes, "crack_ring_hammer_charm_accessory", rows(" F ", "FFF", "CCC"),
				key('F', vanilla("flint"), 'C', mod("courage_legacy")));
		put(recipes, "feast_ham_accessory", rows(" KB", "PKK", "KP "),
				key('K', vanilla("cooked_porkchop"), 'B', vanilla("bone"), 'P', mod("purity_legacy")));
		put(recipes, "fractured_crystal_blade_accessory", rows("AC ", "AOA", " CA"),
				key('A', vanilla("amethyst_shard"), 'C', mod("courage_legacy"), 'O', vanilla("obsidian")));
		put(recipes, "hawkeye_lens_accessory", rows(" G ", "FSF", " W "),
				key('G', vanilla("glass"), 'F', vanilla("feather"), 'S', vanilla("spyglass"), 'W', mod("wisdom_legacy")));
		put(recipes, "heart_sprout_amber_accessory", rows(" E ", "GPG", " G "),
				key('E', vanilla("emerald"), 'G', vanilla("gold_nugget"), 'P', mod("purity_legacy")));
		put(recipes, "hollow_bird_bone_accessory", rows(" BB", "BWP", " BB"),
				key('B', vanilla("bone"), 'W', mod("wisdom_legacy"), 'P', vanilla("phantom_membrane")));
		put(recipes, "light_gathering_magnet_accessory", rows("LRL", "IDI", "WIW"),
				key('L', vanilla("lapis_lazuli"), 'R', vanilla("redstone"), 'I', vanilla("iron_ingot"),
						'D', vanilla("diamond"), 'W', mod("wisdom_legacy")));
		put(recipes, "memory_ritual_knife_accessory", rows("TBT", "TCT", "AAA"),
				key('T', vanilla("tuff"), 'B', vanilla("bone"), 'C', mod("courage_legacy"), 'A', mod("craft_legacy")));
		put(recipes, "moondew_bottle_accessory", rows("TPT", "TPT", "TTT"),
				key('T', vanilla("terracotta"), 'P', mod("purity_legacy")));
		put(recipes, "mountain_burden_blade_accessory", rows("BSB", "C C", "BSB"),
				key('B', vanilla("bamboo"), 'S', vanilla("string"), 'C', mod("courage_legacy")));
		put(recipes, "peacemaker_accessory", rows(" G ", "PEP", " P "),
				key('G', vanilla("ghast_tear"), 'P', mod("purity_legacy"), 'E', vanilla("emerald")));
		put(recipes, "plate_armor_accessory", rows(" S ", "CFC", "C C"),
				key('S', vanilla("string"), 'C', vanilla("copper_ingot"), 'F', mod("fortitude_legacy")));
		put(recipes, "spiked_armor_accessory", rows("OCO", "AOF", "OCO"),
				key('O', vanilla("obsidian"), 'C', vanilla("cactus"), 'A', mod("courage_legacy"), 'F', mod("fortitude_legacy")));
		put(recipes, "substitute_doll_accessory", rows(" S ", "PPP", "FPF"),
				key('S', vanilla("string"), 'P', PLANKS, 'F', mod("fortitude_legacy")));
		put(recipes, "sunwheel_garland_accessory", rows("SSS", "P P", "SSS"),
				key('S', vanilla("sunflower"), 'P', mod("purity_legacy")));
		put(recipes, "tomato_fish_accessory", rows("DWD", "WPW", "CWD"),
				key('D', vanilla("red_dye"), 'W', PLANKS, 'P', mod("purity_legacy"), 'C', mod("craft_legacy")));
		put(recipes, "training_notes_accessory", rows(" GF", "GWG", "WG "),
				key('G', vanilla("gold_ingot"), 'F', vanilla("feather"), 'W', mod("wisdom_legacy")));
		put(recipes, "twin_oath_badge_accessory", rows("ESE", "F F", "C T"),
				key('E', vanilla("emerald"), 'S', vanilla("string"), 'F', FISHES,
						'C', mod("courage_legacy"), 'T', mod("fortitude_legacy")));
		put(recipes, "victors_laurel_accessory", rows("LCL", "P P", "LGL"),
				key('L', LEAVES, 'C', mod("courage_legacy"), 'P', mod("purity_legacy"), 'G', vanilla("golden_apple")));
		put(recipes, "windchaser_feather_accessory", rows("FFF", "SSF", "WSF"),
				key('F', vanilla("feather"), 'S', vanilla("stick"), 'W', mod("wisdom_legacy")));
		return Map.copyOf(recipes);
	}

	private static void put(Map<String, RecipeSpec> recipes, String output, String[] pattern,
			Map<Character, List<Identifier>> key) {
		List<List<Identifier>> ingredients = new ArrayList<>(9);
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				char symbol = pattern[row].charAt(column);
				ingredients.add(symbol == ' ' ? List.of() : key.getOrDefault(symbol, List.of()));
			}
		}
		recipes.put(output, new RecipeSpec(List.copyOf(ingredients), EchoWarrior.id(output)));
	}

	private static String[] rows(String first, String second, String third) {
		return new String[] {first, second, third};
	}

	@SafeVarargs
	private static Map<Character, List<Identifier>> key(Object... entries) {
		Map<Character, List<Identifier>> result = new LinkedHashMap<>();
		for (int index = 0; index < entries.length; index += 2) {
			@SuppressWarnings("unchecked")
			List<Identifier> values = (List<Identifier>)entries[index + 1];
			result.put((Character)entries[index], values);
		}
		return result;
	}

	private static List<Identifier> vanilla(String... paths) {
		List<Identifier> ids = new ArrayList<>(paths.length);
		for (String path : paths) ids.add(Identifier.withDefaultNamespace(path));
		return List.copyOf(ids);
	}

	private static List<Identifier> mod(String path) {
		return List.of(EchoWarrior.id(path));
	}

	record RecipeSpec(List<List<Identifier>> ingredients, Identifier output) {
		Identifier ingredient(int slot, long gameTime) {
			List<Identifier> choices = this.ingredients.get(slot);
			if (choices.isEmpty()) return null;
			return choices.get((int)(gameTime / 40L % choices.size()));
		}
	}
}
