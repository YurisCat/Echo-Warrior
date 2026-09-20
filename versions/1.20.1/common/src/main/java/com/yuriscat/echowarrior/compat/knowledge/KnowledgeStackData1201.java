package com.yuriscat.echowarrior.compat.knowledge;

import com.yuriscat.echowarrior.compat.ModContent1201;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class KnowledgeStackData1201 {
    private static net.minecraft.nbt.CompoundTag readTag(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().copy() : new net.minecraft.nbt.CompoundTag();
    }
    private static void updateTag(ItemStack stack, java.util.function.Consumer<net.minecraft.nbt.CompoundTag> update) {
        var tag = readTag(stack);
        update.accept(tag);
        stack.setTag(tag);
    }

    private static final String FRAGMENT_ID = "EchoWarriorKnowledgeId";
    private static final String COLLECTION_PAGES = "EchoWarriorKnowledgePages";
    private static final String COLLECTION_BOOKMARK = "EchoWarriorKnowledgeBookmark";
    private static final String PAGE_ID = "Id";
    private static final String PAGE_COUNT = "Count";

    private KnowledgeStackData1201() {
    }

    public static ItemStack fragment(String id) {
        ItemStack stack = new ItemStack(ModContent1201.KNOWLEDGE_FRAGMENT);
        setFragmentId(stack, id);
        return stack;
    }

    public static Optional<String> fragmentId(ItemStack stack) {
        if (!stack.is(ModContent1201.KNOWLEDGE_FRAGMENT)) return Optional.empty();
        String id = readTag(stack).getString(FRAGMENT_ID);
        return KnowledgeCatalog1201.contains(id) ? Optional.of(id) : Optional.empty();
    }

    public static void setFragmentId(ItemStack stack, String id) {
        if (!KnowledgeCatalog1201.contains(id)) throw new IllegalArgumentException("Unknown knowledge id " + id);
        updateTag(stack, tag -> tag.putString(FRAGMENT_ID, id));
    }

    public static ItemStack collection(Map<String, Integer> counts, String bookmark) {
        ItemStack stack = new ItemStack(ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION);
        writeCollection(stack, counts, bookmark);
        return stack;
    }

    public static LinkedHashMap<String, Integer> collectionCounts(ItemStack stack) {
        LinkedHashMap<String, Integer> raw = new LinkedHashMap<>();
        if (!stack.is(ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION)) return raw;
        ListTag pages = readTag(stack).getList(COLLECTION_PAGES, Tag.TAG_COMPOUND);
        for (Tag tag : pages) {
            if (!(tag instanceof CompoundTag page)) continue;
            String id = page.getString(PAGE_ID);
            int count = page.getInt(PAGE_COUNT);
            if (KnowledgeCatalog1201.contains(id) && count > 0) merge(raw, id, count);
        }
        LinkedHashMap<String, Integer> canonical = new LinkedHashMap<>();
        for (KnowledgeCatalog1201.Entry entry : KnowledgeCatalog1201.entries()) {
            int count = raw.getOrDefault(entry.id(), 0);
            if (count > 0) canonical.put(entry.id(), count);
        }
        return canonical;
    }

    public static String bookmark(ItemStack stack) {
        return readTag(stack).getString(COLLECTION_BOOKMARK);
    }

    public static void setBookmark(ItemStack stack, String id) {
        if (!stack.is(ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION)) return;
        LinkedHashMap<String, Integer> counts = collectionCounts(stack);
        if (counts.getOrDefault(id, 0) <= 0) return;
        updateTag(stack, tag -> tag.putString(COLLECTION_BOOKMARK, id));
    }

    /** Ignores the bookmark while retaining the collection's actual page contents as its hand identity. */
    public static boolean isSamePhysicalCollection(ItemStack first, ItemStack second) {
        return first.is(ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION)
                && second.is(ModContent1201.KNOWLEDGE_FRAGMENT_COLLECTION)
                && collectionCounts(first).equals(collectionCounts(second));
    }

    public static void writeCollection(ItemStack stack, Map<String, Integer> counts, String requestedBookmark) {
        LinkedHashMap<String, Integer> canonical = new LinkedHashMap<>();
        for (KnowledgeCatalog1201.Entry entry : KnowledgeCatalog1201.entries()) {
            int count = counts.getOrDefault(entry.id(), 0);
            if (count > 0) canonical.put(entry.id(), count);
        }
        if (totalCount(canonical) < 2) throw new IllegalArgumentException("A knowledge collection needs at least two fragments");
        String bookmark = normalizedBookmark(canonical, requestedBookmark);
        updateTag(stack, tag -> {
            ListTag pages = new ListTag();
            canonical.forEach((id, count) -> {
                CompoundTag page = new CompoundTag();
                page.putString(PAGE_ID, id);
                page.putInt(PAGE_COUNT, count);
                pages.add(page);
            });
            tag.remove(FRAGMENT_ID);
            tag.put(COLLECTION_PAGES, pages);
            tag.putString(COLLECTION_BOOKMARK, bookmark);
        });
    }

    public static List<String> pageIds(ItemStack stack) {
        Optional<String> fragment = fragmentId(stack);
        return fragment.<List<String>>map(List::of)
                .orElseGet(() -> KnowledgeCatalog1201.presentIds(collectionCounts(stack)));
    }

    public static long totalCount(ItemStack stack) {
        return totalCount(collectionCounts(stack));
    }

    public static int uniqueCount(ItemStack stack) {
        return collectionCounts(stack).size();
    }

    public static long totalCount(Map<String, Integer> counts) {
        long total = 0;
        for (int count : counts.values()) total += Math.max(0, count);
        return total;
    }

    public static String normalizedBookmark(Map<String, Integer> counts, String requested) {
        if (requested != null && counts.getOrDefault(requested, 0) > 0) return requested;
        List<String> ids = KnowledgeCatalog1201.presentIds(counts);
        return ids.isEmpty() ? "" : ids.get(0);
    }

    public static String initialPage(ItemStack stack) {
        Optional<String> fragment = fragmentId(stack);
        if (fragment.isPresent()) return fragment.get();
        LinkedHashMap<String, Integer> counts = collectionCounts(stack);
        return normalizedBookmark(counts, bookmark(stack));
    }

    public static String pageAfterRemoval(Map<String, Integer> counts, String removedId) {
        List<String> ids = KnowledgeCatalog1201.presentIds(counts);
        if (ids.isEmpty()) return "";
        int originalIndex = KnowledgeCatalog1201.canonicalIndex(removedId);
        for (String id : ids) {
            if (KnowledgeCatalog1201.canonicalIndex(id) >= originalIndex) return id;
        }
        return ids.get(ids.size() - 1);
    }

    public static void merge(Map<String, Integer> counts, String id, int added) {
        if (!KnowledgeCatalog1201.contains(id) || added <= 0) return;
        long sum = (long)counts.getOrDefault(id, 0) + added;
        counts.put(id, (int)Math.min(Integer.MAX_VALUE, sum));
    }
}
