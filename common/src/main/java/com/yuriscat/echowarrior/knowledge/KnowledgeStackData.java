package com.yuriscat.echowarrior.knowledge;

import com.yuriscat.echowarrior.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class KnowledgeStackData {
	private static final String FRAGMENT_ID = "EchoWarriorKnowledgeId";
	private static final String COLLECTION_PAGES = "EchoWarriorKnowledgePages";
	private static final String COLLECTION_BOOKMARK = "EchoWarriorKnowledgeBookmark";
	private static final String PAGE_ID = "Id";
	private static final String PAGE_COUNT = "Count";

	private KnowledgeStackData() {
	}

	public static ItemStack fragment(String id) {
		ItemStack stack = new ItemStack(ModItems.KNOWLEDGE_FRAGMENT);
		setFragmentId(stack, id);
		return stack;
	}

	public static Optional<String> fragmentId(ItemStack stack) {
		if (!stack.is(ModItems.KNOWLEDGE_FRAGMENT)) return Optional.empty();
		String id = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getStringOr(FRAGMENT_ID, "");
		return KnowledgeCatalog.contains(id) ? Optional.of(id) : Optional.empty();
	}

	public static void setFragmentId(ItemStack stack, String id) {
		if (!KnowledgeCatalog.contains(id)) throw new IllegalArgumentException("Unknown knowledge id " + id);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(FRAGMENT_ID, id));
	}

	public static ItemStack collection(Map<String, Integer> counts, String bookmark) {
		ItemStack stack = new ItemStack(ModItems.KNOWLEDGE_FRAGMENT_COLLECTION);
		writeCollection(stack, counts, bookmark);
		return stack;
	}

	public static LinkedHashMap<String, Integer> collectionCounts(ItemStack stack) {
		LinkedHashMap<String, Integer> raw = new LinkedHashMap<>();
		if (!stack.is(ModItems.KNOWLEDGE_FRAGMENT_COLLECTION)) return raw;
		ListTag pages = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getListOrEmpty(COLLECTION_PAGES);
		for (Tag tag : pages) {
			if (!(tag instanceof CompoundTag page)) continue;
			String id = page.getStringOr(PAGE_ID, "");
			int count = page.getIntOr(PAGE_COUNT, 0);
			if (KnowledgeCatalog.contains(id) && count > 0) merge(raw, id, count);
		}
		LinkedHashMap<String, Integer> canonical = new LinkedHashMap<>();
		for (KnowledgeCatalog.Entry entry : KnowledgeCatalog.entries()) {
			int count = raw.getOrDefault(entry.id(), 0);
			if (count > 0) canonical.put(entry.id(), count);
		}
		return canonical;
	}

	public static String bookmark(ItemStack stack) {
		if (!stack.is(ModItems.KNOWLEDGE_FRAGMENT_COLLECTION)) return "";
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
				.copyTag().getStringOr(COLLECTION_BOOKMARK, "");
	}

	public static void writeCollection(ItemStack stack, Map<String, Integer> counts, String requestedBookmark) {
		LinkedHashMap<String, Integer> canonical = canonicalCounts(counts);
		if (totalCount(canonical) < 2) throw new IllegalArgumentException("A knowledge collection must contain at least two fragments");
		String bookmark = normalizedBookmark(canonical, requestedBookmark);
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			ListTag pages = new ListTag();
			for (Map.Entry<String, Integer> entry : canonical.entrySet()) {
				CompoundTag page = new CompoundTag();
				page.putString(PAGE_ID, entry.getKey());
				page.putInt(PAGE_COUNT, entry.getValue());
				pages.add(page);
			}
			tag.remove(FRAGMENT_ID);
			tag.put(COLLECTION_PAGES, pages);
			tag.putString(COLLECTION_BOOKMARK, bookmark);
		});
	}

	public static void setBookmark(ItemStack stack, String id) {
		LinkedHashMap<String, Integer> counts = collectionCounts(stack);
		if (!counts.containsKey(id)) return;
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(COLLECTION_BOOKMARK, id));
	}

	public static long totalCount(Map<String, Integer> counts) {
		long total = 0L;
		for (int count : counts.values()) total += Math.max(0, count);
		return total;
	}

	public static int uniqueCount(ItemStack stack) {
		return collectionCounts(stack).size();
	}

	public static long totalCount(ItemStack stack) {
		return totalCount(collectionCounts(stack));
	}

	public static String initialPage(ItemStack stack) {
		Optional<String> fragment = fragmentId(stack);
		if (fragment.isPresent()) return fragment.get();
		LinkedHashMap<String, Integer> counts = collectionCounts(stack);
		return normalizedBookmark(counts, bookmark(stack));
	}

	public static String normalizedBookmark(Map<String, Integer> counts, String requested) {
		if (requested != null && counts.getOrDefault(requested, 0) > 0) return requested;
		List<String> present = KnowledgeCatalog.presentIds(counts);
		return present.isEmpty() ? "" : present.getFirst();
	}

	public static String pageAfterRemoval(Map<String, Integer> remaining, String removedId) {
		List<String> present = KnowledgeCatalog.presentIds(remaining);
		if (present.isEmpty()) return "";
		int removedIndex = KnowledgeCatalog.canonicalIndex(removedId);
		for (String id : present) {
			if (KnowledgeCatalog.canonicalIndex(id) > removedIndex) return id;
		}
		return present.getLast();
	}

	public static void merge(Map<String, Integer> counts, String id, int added) {
		if (!KnowledgeCatalog.contains(id) || added <= 0) return;
		long sum = (long)counts.getOrDefault(id, 0) + added;
		counts.put(id, (int)Math.min(Integer.MAX_VALUE, sum));
	}

	private static LinkedHashMap<String, Integer> canonicalCounts(Map<String, Integer> counts) {
		LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
		for (KnowledgeCatalog.Entry entry : KnowledgeCatalog.entries()) {
			int count = counts.getOrDefault(entry.id(), 0);
			if (count > 0) result.put(entry.id(), count);
		}
		return result;
	}
}
