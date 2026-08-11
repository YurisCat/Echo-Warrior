package com.yuriscat.echowarrior.layout;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yuriscat.echowarrior.EchoWarrior;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public final class SummonerLayout {
	public static final int GUI_WIDTH = 241;
	public static final int GUI_HEIGHT = 201;
	private static final int FILE_VERSION = 2;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("echo_warrior")
			.resolve("summoner_layout.json");
	private static final SummonerLayout INSTANCE = new SummonerLayout();

	private final EnumMap<Element, Offset> offsets = new EnumMap<>(Element.class);

	private SummonerLayout() {
		reset();
		load();
	}

	public static SummonerLayout get() {
		return INSTANCE;
	}

	public synchronized int x(Element element, int baseX) {
		return baseX + offsetX(element);
	}

	public synchronized int y(Element element, int baseY) {
		return baseY + offsetY(element);
	}

	public synchronized int offsetX(Element element) {
		return this.offsets.get(element).x;
	}

	public synchronized int offsetY(Element element) {
		return this.offsets.get(element).y;
	}

	public synchronized void setOffset(Element element, int x, int y) {
		int minX = -element.baseX;
		int maxX = GUI_WIDTH - element.baseX - element.width;
		int minY = -element.baseY;
		int maxY = GUI_HEIGHT - element.baseY - element.height;
		this.offsets.put(element, new Offset(
				Math.clamp(x, minX, maxX),
				Math.clamp(y, minY, maxY)
		));
	}

	public synchronized void move(Element element, int deltaX, int deltaY) {
		setOffset(element, offsetX(element) + deltaX, offsetY(element) + deltaY);
	}

	public synchronized EnumMap<Element, Offset> snapshot() {
		EnumMap<Element, Offset> copy = new EnumMap<>(Element.class);
		for (Map.Entry<Element, Offset> entry : this.offsets.entrySet()) {
			copy.put(entry.getKey(), entry.getValue().copy());
		}
		return copy;
	}

	public synchronized void restore(Map<Element, Offset> snapshot) {
		this.offsets.clear();
		for (Element element : Element.values()) {
			Offset offset = snapshot.get(element);
			this.offsets.put(element, offset == null ? new Offset() : offset.copy());
		}
	}

	public synchronized void reset() {
		this.offsets.clear();
		for (Element element : Element.values()) {
			this.offsets.put(element, new Offset());
		}
	}

	public synchronized void save() {
		JsonObject root = new JsonObject();
		root.addProperty("version", FILE_VERSION);
		JsonObject elements = new JsonObject();
		for (Element element : Element.values()) {
			Offset offset = this.offsets.get(element);
			JsonObject value = new JsonObject();
			value.addProperty("x", offset.x);
			value.addProperty("y", offset.y);
			elements.add(element.serializedName, value);
		}
		root.add("elements", elements);

		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			EchoWarrior.LOGGER.error("Failed to save summoner GUI layout to {}", FILE, exception);
		}
	}

	private synchronized void load() {
		if (!Files.isRegularFile(FILE)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
			if (!root.has("version") || root.get("version").getAsInt() != FILE_VERSION) {
				return;
			}
			JsonObject elements = root.getAsJsonObject("elements");
			if (elements == null) {
				return;
			}
			for (Element element : Element.values()) {
				JsonElement raw = elements.get(element.serializedName);
				if (raw == null || !raw.isJsonObject()) {
					continue;
				}
				JsonObject value = raw.getAsJsonObject();
				int x = value.has("x") ? value.get("x").getAsInt() : 0;
				int y = value.has("y") ? value.get("y").getAsInt() : 0;
				setOffset(element, x, y);
			}
		} catch (RuntimeException | IOException exception) {
			EchoWarrior.LOGGER.error("Failed to load summoner GUI layout from {}", FILE, exception);
		}
	}

	public enum Element {
		TITLE("英灵名称", 7, 6, 116, 11),
		TALENTS("标题天赋", 124, 6, 44, 11),
		MODEL("角色模型", 7, 19, 51, 72),
		BASIC_INFO("八项基础属性", 61, 19, 108, 50),
		SKILLS("技能", 61, 71, 108, 20),
		MODULES("模块槽", 7, 93, 162, 18),
		EXPERIENCE("经验条", 7, 113, 162, 4),
		PLAYER_INVENTORY("玩家物品栏", 7, 119, 162, 75),
		ACTIVITY("行动模式", 178, 77, 56, 31),
		ALERT("警戒状态", 178, 110, 56, 31),
		SUMMON_BUTTON("召唤按钮", 178, 143, 56, 19),
		FUEL_BAR("燃料条", 178, 164, 56, 5),
		FUEL_SLOT("燃料槽", 178, 171, 18, 18),
		RELIC_SLOT("遗物槽", 216, 171, 18, 18);

		public final String serializedName;
		public final int baseX;
		public final int baseY;
		public final int width;
		public final int height;

		Element(String serializedName, int baseX, int baseY, int width, int height) {
			this.serializedName = serializedName;
			this.baseX = baseX;
			this.baseY = baseY;
			this.width = width;
			this.height = height;
		}
	}

	public static final class Offset {
		public int x;
		public int y;

		public Offset() {
			this(0, 0);
		}

		public Offset(int x, int y) {
			this.x = x;
			this.y = y;
		}

		private Offset copy() {
			return new Offset(this.x, this.y);
		}
	}
}
