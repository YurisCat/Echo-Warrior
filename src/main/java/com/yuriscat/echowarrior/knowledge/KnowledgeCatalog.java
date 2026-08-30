package com.yuriscat.echowarrior.knowledge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yuriscat.echowarrior.EchoWarrior;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stable, authored ordering and presentation metadata for every knowledge page.
 *
 * <p>The catalog is deliberately loaded from a bundled JSON resource rather than
 * encoded into item classes. Item stacks only persist the stable entry id, so
 * wording, sources and illustrations may be revised without rewriting saves.</p>
 */
public final class KnowledgeCatalog {
	private static final String RESOURCE = "/data/echo_warrior/knowledge/entries.json";
	private static final List<String> CULTURE_ORDER = List.of("roman", "aztec", "egyptian", "chinese", "japanese");
	private static final List<Entry> ENTRIES = load();
	private static final Map<String, Entry> BY_ID = indexById(ENTRIES);
	private static final Map<String, List<Entry>> BY_CULTURE = indexByCulture(ENTRIES);

	private KnowledgeCatalog() {
	}

	public static List<Entry> entries() {
		return ENTRIES;
	}

	public static Optional<Entry> entry(String id) {
		return Optional.ofNullable(BY_ID.get(id));
	}

	public static boolean contains(String id) {
		return BY_ID.containsKey(id);
	}

	public static List<Entry> entriesForCulture(String culture) {
		return BY_CULTURE.getOrDefault(culture, List.of());
	}

	public static Entry randomForCulture(String culture, RandomSource random) {
		List<Entry> entries = entriesForCulture(culture);
		if (entries.isEmpty()) return ENTRIES.getFirst();
		return entries.get(random.nextInt(entries.size()));
	}

	public static Entry random(RandomSource random) {
		String culture = CULTURE_ORDER.get(random.nextInt(CULTURE_ORDER.size()));
		return randomForCulture(culture, random);
	}

	public static int canonicalIndex(String id) {
		for (int index = 0; index < ENTRIES.size(); index++) {
			if (ENTRIES.get(index).id().equals(id)) return index;
		}
		return Integer.MAX_VALUE;
	}

	public static List<String> presentIds(Map<String, Integer> counts) {
		List<String> result = new ArrayList<>();
		for (Entry entry : ENTRIES) {
			if (counts.getOrDefault(entry.id(), 0) > 0) result.add(entry.id());
		}
		return List.copyOf(result);
	}

	public static String cultureTranslationKey(String culture) {
		return "knowledge.echo_warrior.culture." + culture;
	}

	private static List<Entry> load() {
		try (InputStream stream = KnowledgeCatalog.class.getResourceAsStream(RESOURCE)) {
			if (stream == null) throw new IllegalStateException("Missing knowledge catalog " + RESOURCE);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				JsonArray array = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("entries");
				List<Entry> entries = new ArrayList<>(array.size());
				Map<String, Boolean> ids = new LinkedHashMap<>();
				for (JsonElement element : array) {
					JsonObject object = element.getAsJsonObject();
					String id = object.get("id").getAsString();
					if (ids.put(id, Boolean.TRUE) != null) throw new IllegalStateException("Duplicate knowledge id " + id);
					String culture = object.get("culture").getAsString();
					if (!CULTURE_ORDER.contains(culture)) throw new IllegalStateException("Unknown knowledge culture " + culture);
					List<Illustration> illustrations = new ArrayList<>();
					String illustrationBinding = object.has("illustration_binding")
							? object.get("illustration_binding").getAsString()
							: "none";
					if (!"none".equals(illustrationBinding) && !"high".equals(illustrationBinding)) {
						throw new IllegalStateException("Unknown illustration binding " + illustrationBinding + " for " + id);
					}
					JsonArray illustrationArray = "high".equals(illustrationBinding) && object.has("illustrations")
							? object.getAsJsonArray("illustrations")
							: new JsonArray();
					for (JsonElement illustrationElement : illustrationArray) {
						JsonObject illustration = illustrationElement.getAsJsonObject();
						String type = illustration.get("type").getAsString();
						Identifier resource = Identifier.parse(illustration.get("resource").getAsString());
						String nameKey = illustration.has("name_key") ? illustration.get("name_key").getAsString() : "";
						illustrations.add(new Illustration(type, resource, nameKey));
					}
					entries.add(new Entry(
							id,
							culture,
							object.get("order").getAsInt(),
							object.get("title_key").getAsString(),
							object.get("body_key").getAsString(),
							List.copyOf(illustrations)
					));
				}
				entries.sort((left, right) -> {
					int culture = Integer.compare(CULTURE_ORDER.indexOf(left.culture()), CULTURE_ORDER.indexOf(right.culture()));
					return culture != 0 ? culture : Integer.compare(left.order(), right.order());
				});
				if (entries.size() != 40) throw new IllegalStateException("Knowledge catalog must contain exactly 40 entries, found " + entries.size());
				for (String culture : CULTURE_ORDER) {
					long count = entries.stream().filter(entry -> entry.culture().equals(culture)).count();
					if (count != 8) throw new IllegalStateException("Culture " + culture + " must contain exactly 8 entries, found " + count);
				}
				return Collections.unmodifiableList(entries);
			}
		} catch (IOException | RuntimeException exception) {
			throw new IllegalStateException("Could not load bundled knowledge catalog", exception);
		}
	}

	private static Map<String, Entry> indexById(List<Entry> entries) {
		Map<String, Entry> result = new LinkedHashMap<>();
		for (Entry entry : entries) result.put(entry.id(), entry);
		return Collections.unmodifiableMap(result);
	}

	private static Map<String, List<Entry>> indexByCulture(List<Entry> entries) {
		Map<String, List<Entry>> result = new LinkedHashMap<>();
		for (String culture : CULTURE_ORDER) {
			result.put(culture, entries.stream().filter(entry -> entry.culture().equals(culture)).toList());
		}
		return Collections.unmodifiableMap(result);
	}

	public record Entry(
			String id,
			String culture,
			int order,
			String titleKey,
			String bodyKey,
			List<Illustration> illustrations
	) {
	}

	public record Illustration(String type, Identifier resource, String nameKey) {
		public boolean isItem() {
			return "item".equals(type);
		}
	}
}
