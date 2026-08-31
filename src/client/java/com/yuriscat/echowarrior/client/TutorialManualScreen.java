package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.item.EchoAccessoryItem;
import com.yuriscat.echowarrior.menu.TutorialManualMenu;
import com.yuriscat.echowarrior.tutorial.TutorialManualCatalog;
import com.yuriscat.echowarrior.tutorial.TutorialManualCatalog.Chapter;
import com.yuriscat.echowarrior.tutorial.TutorialManualCatalog.Page;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TutorialManualScreen extends AbstractContainerScreen<TutorialManualMenu> {
	private static final int IMAGE_WIDTH = 220;
	private static final int IMAGE_HEIGHT = 205;
	private static final int PAPER_X = 48;
	private static final int PAPER_Y = 2;
	private static final int PAPER_WIDTH = 165;
	private static final int PAPER_HEIGHT = 201;
	private static final int CONTENT_LEFT = PAPER_X + 14;
	private static final int CONTENT_WIDTH = PAPER_WIDTH - 28;
	private static final int PAGE_CENTER_X = PAPER_X + PAPER_WIDTH / 2;
	private static final int TITLE_Y = PAPER_Y + 16;
	private static final int BODY_Y = PAPER_Y + 32;
	private static final int INK = 0xFF745B44;
	private static final int MUTED_INK = 0xFFA48C67;
	private static final int POSITIVE = 0xFF628F5A;
	private static final int NEGATIVE = 0xFFB95C5C;
	private static final int BUTTON_HIT_PADDING = 2;
	private static final int CLOSE_X = PAPER_X + 150;
	private static final int CLOSE_Y = PAPER_Y + 4;
	private static final int CLOSE_SIZE = 11;
	private static final int PREVIOUS_X = PAPER_X + 2;
	private static final int NEXT_X = PAPER_X + 148;
	private static final int PAGE_BUTTON_Y = PAPER_Y + 176;
	private static final int PREVIOUS_WIDTH = 15;
	private static final int NEXT_WIDTH = 15;
	private static final int PAGE_BUTTON_HEIGHT = 13;
	private static final int TAB_START_Y = PAPER_Y + 8;
	private static final int TAB_WIDTH = 43;
	private static final int TAB_HEIGHT = 24;
	private static final int TAB_ICON_X = 4;
	private static final int TAB_ICON_Y = 4;
	private static final int TAB_BASE_X = PAPER_X - TAB_ICON_X - 16 - 1;
	private static final int TAB_HOVER_OFFSET = -4;
	private static final int TAB_ACTIVE_OFFSET = -6;
	private static final int RECIPE_WIDTH = 95;
	private static final int RECIPE_HEIGHT = 54;
	private static final int RECIPE_X = PAGE_CENTER_X - RECIPE_WIDTH / 2;
	private static final int RECIPE_Y = PAPER_Y + 137;
	private static final int ACCESSORY_RECIPE_Y = PAPER_Y + 112;
	private static final int ACCESSORY_EFFECT_Y = PAPER_Y + 62;
	private static final int ACCESSORY_EFFECT_WIDTH = CONTENT_WIDTH - 8;
	private static final int ACCESSORY_EFFECT_HEIGHT = ACCESSORY_RECIPE_Y - 12 - ACCESSORY_EFFECT_Y;
	private static final int CREDITS_IMAGE_SIZE = 128;
	private static final int CREDITS_IMAGE_SOURCE_SIZE = 450;
	private static final int CREDITS_IMAGE_TEXT_GAP = 8;
	private static final int CREDITS_TEXT_HEIGHT = 9;
	private static final float PARENTHESES_SCALE = 8.0F / 9.0F;
	private static final int ENGLISH_TEAM_LINE_HEIGHT = 8;
	private static final int ENGLISH_TEAM_ENTRY_GAP = 1;

	private static final Identifier PAPER_SHADOW = texture("paper_shadow.png");
	private static final Identifier PAPER = texture("paper.png");
	private static final Identifier PREVIOUS = texture("previous.png");
	private static final Identifier PREVIOUS_SHADOW = texture("previous_shadow.png");
	private static final Identifier NEXT = texture("next.png");
	private static final Identifier NEXT_SHADOW = texture("next_shadow.png");
	private static final Identifier CLOSE = texture("close.png");
	private static final Identifier CLOSE_SHADOW = texture("close_shadow.png");
	private static final Identifier CHAPTER_TAB = texture("chapter_tab.png");
	private static final Identifier RECIPE = texture("recipe.png");
	private static final Identifier CREDITS_PORTRAIT = texture("credits_portrait.png");

	private static final String[][] HERO_SKILL_PATHS = {
			{"roman_legionary/soldier_formation", "roman_legionary/legionary_bulwark",
					"roman_legionary/shield_charge", "roman_legionary/legion_endures"},
			{"aztec_warrior/quetzalcoatls_curse", "aztec_warrior/huitzilopochtlis_blessing",
					"aztec_warrior/obsidian_wound", "aztec_warrior/pursuit", "aztec_warrior/macuahuitl_mastery"},
			{"egyptian_archer/cat_god", "egyptian_archer/leaf_arrow", "egyptian_archer/cone_arrow",
					"egyptian_archer/chariot_volley", "egyptian_archer/backstep"},
			{"guandao_warrior/armor_clad", "guandao_warrior/growing_valor", "guandao_warrior/crescent_blade",
					"guandao_warrior/guandao_combo"},
			{"japanese_samurai/zanshin", "japanese_samurai/fumikomi", "japanese_samurai/zan",
					"japanese_samurai/stab"}
	};
	private static final String[][] HERO_SKILL_KEYS = {
			{"gui.echo_warrior.tutorial.skill.roman.formation", "gui.echo_warrior.tutorial.skill.roman.bulwark",
					"gui.echo_warrior.tutorial.skill.roman.charge", "gui.echo_warrior.tutorial.skill.roman.endures"},
			{"gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse",
					"gui.echo_warrior.summoner.skill.aztec.huitzilopochtlis_blessing",
					"gui.echo_warrior.summoner.skill.aztec.obsidian_wound",
					"gui.echo_warrior.summoner.skill.aztec.pursuit", "gui.echo_warrior.summoner.skill.aztec.macuahuitl"},
			{"gui.echo_warrior.summoner.skill.egyptian.cat_god",
					"gui.echo_warrior.summoner.skill.egyptian.leaf_arrow",
					"gui.echo_warrior.summoner.skill.egyptian.cone_arrow",
					"gui.echo_warrior.summoner.skill.egyptian.chariot_volley",
					"gui.echo_warrior.summoner.skill.egyptian.backstep"},
			{"gui.echo_warrior.summoner.skill.guandao.armor_clad",
					"gui.echo_warrior.summoner.skill.guandao.growing_valor",
					"gui.echo_warrior.summoner.skill.guandao.crescent_blade",
					"gui.echo_warrior.summoner.skill.guandao.combo"},
			{"gui.echo_warrior.summoner.skill.samurai.zanshin",
					"gui.echo_warrior.summoner.skill.samurai.fumikomi",
					"gui.echo_warrior.summoner.skill.samurai.zan",
					"gui.echo_warrior.summoner.skill.samurai.stab"}
	};
	private static final int[][] HERO_SKILL_LINES = {
			{2, 1, 2, 2}, {1, 2, 2, 2, 2}, {3, 2, 2, 2, 2}, {2, 3, 2, 5}, {4, 4, 3, 5}
	};
	private static final String[] HERO_RELICS = {
			"roman_legionary_relic", "aztec_warrior_relic", "egyptian_archer_relic",
			"guandao_warrior_relic", "japanese_samurai_relic"
	};
	private static final String[] LEGACIES = {
			"courage_legacy", "fortitude_legacy", "purity_legacy", "wisdom_legacy", "craft_legacy"
	};
	private static final String[] ENGLISH_TEAM_LINES = {
			"producer", "developer", "art_header", "art_calypso", "art_tomato", "art_colon",
			"test_header", "test_calypso", "test_chloris", "test_tomato"
	};
	private static final String[] CHINESE_TEAM_LINES = {
			"producer_header", "producer_name", "developer_header", "developer_credit",
			"art_header", "art_calypso", "art_tomato", "art_colon", "test_header", "test_all"
	};

	private final float[] tabOffsets = new float[Chapter.values().length];
	private int hoveredChapter = -1;

	public TutorialManualScreen(TutorialManualMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
		this.titleLabelX = -1000;
		this.titleLabelY = -1000;
		this.inventoryLabelX = -1000;
		this.inventoryLabelY = -1000;
	}

	private static Identifier texture(String path) {
		return EchoWarrior.id("textures/gui/tutorial/" + path);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		Chapter active = currentPage().chapter();
		for (int index = 0; index < this.tabOffsets.length; index++) {
			float target = index == active.ordinal() ? TAB_ACTIVE_OFFSET
					: index == this.hoveredChapter ? TAB_HOVER_OFFSET : 0.0F;
			this.tabOffsets[index] += (target - this.tabOffsets[index]) * 0.45F;
			if (Math.abs(target - this.tabOffsets[index]) < 0.08F) this.tabOffsets[index] = target;
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractTransparentBackground(graphics);
		this.hoveredChapter = chapterAt(mouseX, mouseY);
		renderTabs(graphics, mouseX, mouseY);
		blit(graphics, PAPER_SHADOW, this.leftPos + PAPER_X + 2, this.topPos + PAPER_Y + 2, PAPER_WIDTH, PAPER_HEIGHT);
		blit(graphics, PAPER, this.leftPos + PAPER_X, this.topPos + PAPER_Y, PAPER_WIDTH, PAPER_HEIGHT);
	}

	private void renderTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		for (Chapter chapter : Chapter.values()) {
			int x = this.leftPos + tabX(chapter.ordinal());
			int y = this.topPos + tabY(chapter.ordinal());
			blit(graphics, CHAPTER_TAB, x, y, TAB_WIDTH, TAB_HEIGHT);
			ItemStack icon = stack(chapter.icon());
			graphics.fakeItem(icon, x + TAB_ICON_X, y + TAB_ICON_Y);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Page page = currentPage();
		switch (page.kind()) {
			case COVER -> renderCover(graphics);
			case PROSE -> renderProsePage(graphics, page);
			case RECIPE -> renderRecipePage(graphics, page, mouseX, mouseY);
			case DISCOVERIES -> renderDiscoveries(graphics, page, mouseX, mouseY);
			case KNOWLEDGE -> renderKnowledge(graphics, page, mouseX, mouseY);
			case LEGACY -> renderLegacy(graphics, page, mouseX, mouseY);
			case HERO -> renderHero(graphics, page, mouseX, mouseY);
			case ACCESSORY -> renderAccessory(graphics, page, mouseX, mouseY);
			case THANKS -> renderThanks(graphics);
			case TEAM -> renderTeam(graphics);
			case SOURCES -> renderProsePage(graphics, page);
		}
		renderControls(graphics, mouseX, mouseY);
		renderTabTooltips(graphics, mouseX, mouseY);
	}

	private void renderCover(GuiGraphicsExtractor graphics) {
		centeredFittedEnglish(graphics, Component.translatable("gui.echo_warrior.tutorial.cover.title"),
				PAGE_CENTER_X, PAPER_Y + 50, CONTENT_WIDTH, INK);
		centeredFittedEnglish(graphics, Component.translatable("gui.echo_warrior.tutorial.cover.author"),
				PAGE_CENTER_X, PAPER_Y + 93, CONTENT_WIDTH, INK);
		centeredFittedEnglish(graphics, Component.translatable("gui.echo_warrior.tutorial.cover.by"),
				PAGE_CENTER_X, PAPER_Y + 108, CONTENT_WIDTH, MUTED_INK);
		centeredFittedEnglish(graphics, Component.translatable("gui.echo_warrior.tutorial.page_number",
				this.menu.currentPage() + 1, TutorialManualCatalog.pageCount()),
				PAGE_CENTER_X, PAPER_Y + 184, CONTENT_WIDTH, MUTED_INK);
	}

	private void renderProsePage(GuiGraphicsExtractor graphics, Page page) {
		renderTitle(graphics, Component.translatable(page.titleKey()));
		renderParagraphs(graphics, page, BODY_Y, PAPER_Y + 174);
	}

	private void renderRecipePage(GuiGraphicsExtractor graphics, Page page, int mouseX, int mouseY) {
		renderTitle(graphics, Component.translatable(page.titleKey()));
		renderParagraphs(graphics, page, BODY_Y, RECIPE_Y - 5);
		renderRecipe(graphics, page.subjectId(), RECIPE_X, RECIPE_Y, mouseX, mouseY);
	}

	private void renderDiscoveries(GuiGraphicsExtractor graphics, Page page, int mouseX, int mouseY) {
		renderTitle(graphics, Component.translatable(page.titleKey()));
		renderParagraphs(graphics, page, BODY_Y, PAPER_Y + 137);
		long gameTime = gameTime();
		String legacy = LEGACIES[(int)(gameTime / 40L % LEGACIES.length)];
		List<String> accessories = TutorialManualCatalog.accessoryIds();
		String accessory = accessories.get((int)(gameTime / 40L % accessories.size()));
		renderAttachment(graphics, "knowledge_fragment", PAPER_X + 32, PAPER_Y + 151, mouseX, mouseY);
		renderAttachment(graphics, legacy, PAPER_X + 74, PAPER_Y + 151, mouseX, mouseY);
		renderAttachment(graphics, accessory, PAPER_X + 116, PAPER_Y + 151, mouseX, mouseY);
		centeredFittedEnglish(graphics, Component.translatable("gui.echo_warrior.tutorial.attachment.click"),
				PAGE_CENTER_X, PAPER_Y + 171, CONTENT_WIDTH, MUTED_INK);
	}

	private void renderKnowledge(GuiGraphicsExtractor graphics, Page page, int mouseX, int mouseY) {
		renderTitle(graphics, Component.translatable(page.titleKey()));
		renderParagraphs(graphics, page, BODY_Y, PAPER_Y + 138);
		renderAttachment(graphics, "knowledge_fragment", PAPER_X + 58, PAPER_Y + 151, mouseX, mouseY);
		renderAttachment(graphics, "knowledge_fragment_collection", PAPER_X + 91, PAPER_Y + 151, mouseX, mouseY);
	}

	private void renderLegacy(GuiGraphicsExtractor graphics, Page page, int mouseX, int mouseY) {
		renderTitle(graphics, Component.translatable(page.titleKey()));
		renderParagraphs(graphics, page, BODY_Y, PAPER_Y + 138);
		int startX = PAGE_CENTER_X - (LEGACIES.length * 16 + (LEGACIES.length - 1) * 5) / 2;
		for (int index = 0; index < LEGACIES.length; index++) {
			renderAttachment(graphics, LEGACIES[index], startX + index * 21, PAPER_Y + 151, mouseX, mouseY);
		}
	}

	private void renderHero(GuiGraphicsExtractor graphics, Page page, int mouseX, int mouseY) {
		int hero = heroIndex(page.subjectId());
		renderTitle(graphics, Component.translatable(page.titleKey()));
		renderParagraphs(graphics, page, BODY_Y, PAPER_Y + 138);
		int skillCount = HERO_SKILL_PATHS[hero].length;
		int totalWidth = 16 + 7 + skillCount * 16 + (skillCount - 1) * 4;
		int x = PAGE_CENTER_X - totalWidth / 2;
		renderAttachment(graphics, HERO_RELICS[hero], x, PAPER_Y + 151, mouseX, mouseY);
		x += 23;
		for (int index = 0; index < skillCount; index++) {
			int skillX = x + index * 20;
			Identifier texture = EchoWarrior.id("textures/gui/summoner/skills/" + HERO_SKILL_PATHS[hero][index] + ".png");
			blit(graphics, texture, skillX, PAPER_Y + 151, 16, 16);
			if (insideLocal(mouseX, mouseY, skillX, PAPER_Y + 151, 16, 16)) {
				renderSkillTooltip(graphics, hero, index, mouseX, mouseY);
			}
		}
	}

	private void renderAccessory(GuiGraphicsExtractor graphics, Page page, int mouseX, int mouseY) {
		ItemStack accessory = stack(EchoWarrior.id(page.subjectId()));
		renderTitle(graphics, accessory.getHoverName());
		renderItem(graphics, accessory, CONTENT_LEFT + 5, PAPER_Y + 39, mouseX, mouseY);
		if (accessory.getItem() instanceof EchoAccessoryItem accessoryItem) {
			Component rarity = Component.translatable("gui.echo_warrior.tutorial.accessory.rarity",
					Component.translatable("gui.echo_warrior.tutorial.rarity." + accessoryItem.type().rarity().name().toLowerCase()));
			drawFittedEnglish(graphics, rarity, CONTENT_LEFT + 28, PAPER_Y + 41, CONTENT_WIDTH - 28, MUTED_INK);
			List<ColoredText> effects = new ArrayList<>();
			for (int index = 0; index < accessoryItem.type().effectCount(); index++) {
				boolean positive = accessoryItem.type().effectIsPositive(index);
				String key = "item.echo_warrior.accessory." + accessoryItem.type().id() + ".effect." + (index + 1);
				Component line = Component.literal(positive ? "+ " : "- ").append(Component.translatable(key));
				effects.add(new ColoredText(line, positive ? POSITIVE : NEGATIVE));
			}
			renderAccessoryEffects(graphics, effects);
		}
		drawFittedEnglish(graphics, Component.translatable("gui.echo_warrior.tutorial.recipe"),
				RECIPE_X, ACCESSORY_RECIPE_Y - 11, RECIPE_WIDTH, MUTED_INK);
		renderRecipe(graphics, page.subjectId(), RECIPE_X, ACCESSORY_RECIPE_Y, mouseX, mouseY);
	}

	private void renderAccessoryEffects(GuiGraphicsExtractor graphics, List<ColoredText> effects) {
		List<ColoredLine> lines = wrapColoredLines(effects, ACCESSORY_EFFECT_WIDTH);
		if (isEnglishLanguage() && lines.size() * 9 > ACCESSORY_EFFECT_HEIGHT) {
			float scale = 0.65F;
			lines = wrapColoredLines(effects, (int)Math.floor(ACCESSORY_EFFECT_WIDTH / scale));
			for (int percent = 98; percent >= 65; percent--) {
				float candidate = percent / 100.0F;
				int wrapWidth = (int)Math.floor(ACCESSORY_EFFECT_WIDTH / candidate);
				List<ColoredLine> candidateLines = wrapColoredLines(effects, wrapWidth);
				if (candidateLines.size() * 9 * candidate <= ACCESSORY_EFFECT_HEIGHT) {
					scale = candidate;
					lines = candidateLines;
					break;
				}
			}
			graphics.pose().pushMatrix();
			graphics.pose().translate(CONTENT_LEFT + 4, ACCESSORY_EFFECT_Y);
			graphics.pose().scale(scale, scale);
			for (int index = 0; index < lines.size(); index++) {
				ColoredLine line = lines.get(index);
				graphics.text(this.font, line.text(), 0, index * 9, line.color(), false);
			}
			graphics.pose().popMatrix();
			return;
		}
		for (int index = 0; index < lines.size(); index++) {
			ColoredLine line = lines.get(index);
			graphics.text(this.font, line.text(), CONTENT_LEFT + 4, ACCESSORY_EFFECT_Y + index * 9, line.color(), false);
		}
	}

	private List<ColoredLine> wrapColoredLines(List<ColoredText> texts, int width) {
		List<ColoredLine> lines = new ArrayList<>();
		for (ColoredText text : texts) {
			for (FormattedCharSequence line : this.font.split(text.text(), width)) {
				lines.add(new ColoredLine(line, text.color()));
			}
		}
		return lines;
	}

	private void renderThanks(GuiGraphicsExtractor graphics) {
		String[] thanksLines = Component.translatable("gui.echo_warrior.tutorial.thanks").getString().split("\\n", -1);
		int textHeight = thanksLines.length * CREDITS_TEXT_HEIGHT;
		int groupHeight = CREDITS_IMAGE_SIZE + CREDITS_IMAGE_TEXT_GAP + textHeight;
		int x = PAGE_CENTER_X - CREDITS_IMAGE_SIZE / 2;
		int y = PAPER_Y + (PAPER_HEIGHT - groupHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, CREDITS_PORTRAIT, x, y,
				0.0F, 0.0F, CREDITS_IMAGE_SIZE, CREDITS_IMAGE_SIZE,
				CREDITS_IMAGE_SOURCE_SIZE, CREDITS_IMAGE_SOURCE_SIZE,
				CREDITS_IMAGE_SOURCE_SIZE, CREDITS_IMAGE_SOURCE_SIZE);
		int textY = y + CREDITS_IMAGE_SIZE + CREDITS_IMAGE_TEXT_GAP;
		for (int line = 0; line < thanksLines.length; line++) {
			centeredFittedEnglish(graphics, Component.literal(thanksLines[line]), PAGE_CENTER_X,
					textY + line * CREDITS_TEXT_HEIGHT, CONTENT_WIDTH, INK);
		}
	}

	private void renderTeam(GuiGraphicsExtractor graphics) {
		renderTitle(graphics, Component.translatable("gui.echo_warrior.tutorial.page.team.title"));
		if (isEnglishLanguage()) {
			renderEnglishTeam(graphics);
			return;
		}
		int y = BODY_Y;
		for (String key : CHINESE_TEAM_LINES) {
			int color = key.endsWith("header") ? MUTED_INK : INK;
			String line = Component.translatable("gui.echo_warrior.tutorial.team." + key).getString();
			renderChineseTeamLine(graphics, line, y, color);
			y += 9;
			y += key.endsWith("header") ? 0 : 2;
		}
	}

	private void renderChineseTeamLine(GuiGraphicsExtractor graphics, String text, int y, int color) {
		int opening = text.indexOf('（');
		int closing = opening >= 0 ? text.indexOf('）', opening + 1) : -1;
		if (opening < 0 || closing < 0) {
			opening = text.indexOf('(');
			closing = opening >= 0 ? text.indexOf(')', opening + 1) : -1;
		}

		if (opening < 0 || closing < 0) {
			float scale = Math.min(1.0F, CONTENT_WIDTH / (float)Math.max(1, this.font.width(text)));
			drawScaledText(graphics, text, CONTENT_LEFT, y, scale, color);
			return;
		}

		String prefix = text.substring(0, opening + 1);
		String parenthetical = text.substring(opening + 1, closing);
		String suffix = text.substring(closing);
		float mixedWidth = this.font.width(prefix)
				+ this.font.width(parenthetical) * PARENTHESES_SCALE
				+ this.font.width(suffix);
		float fitScale = Math.min(1.0F, CONTENT_WIDTH / Math.max(1.0F, mixedWidth));
		float x = CONTENT_LEFT;
		drawScaledText(graphics, prefix, x, y, fitScale, color);
		x += this.font.width(prefix) * fitScale;
		drawScaledText(graphics, parenthetical, x, y, fitScale * PARENTHESES_SCALE, color);
		x += this.font.width(parenthetical) * fitScale * PARENTHESES_SCALE;
		drawScaledText(graphics, suffix, x, y, fitScale, color);
	}

	private void renderEnglishTeam(GuiGraphicsExtractor graphics) {
		int y = BODY_Y;
		for (String key : ENGLISH_TEAM_LINES) {
			int color = key.endsWith("header") ? MUTED_INK : INK;
			String text = Component.translatable("gui.echo_warrior.tutorial.team." + key).getString();
			for (List<TeamWord> line : wrapEnglishTeamLine(text)) {
				renderEnglishTeamLine(graphics, line, y, color);
				y += ENGLISH_TEAM_LINE_HEIGHT;
			}
			if (!key.endsWith("header")) y += ENGLISH_TEAM_ENTRY_GAP;
		}
	}

	private List<List<TeamWord>> wrapEnglishTeamLine(String text) {
		int parenthesis = text.indexOf('(');
		List<TeamWord> words = new ArrayList<>();
		int cursor = 0;
		for (String word : text.trim().split("\\s+")) {
			int index = text.indexOf(word, cursor);
			cursor = index + word.length();
			float scale = parenthesis >= 0 && index >= parenthesis ? PARENTHESES_SCALE : 1.0F;
			words.add(new TeamWord(word, scale));
		}

		List<List<TeamWord>> lines = new ArrayList<>();
		List<TeamWord> current = new ArrayList<>();
		float width = 0.0F;
		for (TeamWord word : words) {
			float space = current.isEmpty() ? 0.0F : this.font.width(" ") * word.scale();
			float wordWidth = this.font.width(word.text()) * word.scale();
			if (!current.isEmpty() && width + space + wordWidth > CONTENT_WIDTH) {
				lines.add(List.copyOf(current));
				current.clear();
				width = 0.0F;
				space = 0.0F;
			}
			current.add(word);
			width += space + wordWidth;
		}
		if (!current.isEmpty()) lines.add(List.copyOf(current));
		return lines;
	}

	private void renderEnglishTeamLine(GuiGraphicsExtractor graphics, List<TeamWord> words, int y, int color) {
		float x = CONTENT_LEFT;
		for (int index = 0; index < words.size(); index++) {
			TeamWord word = words.get(index);
			if (index > 0) x += this.font.width(" ") * word.scale();
			drawScaledText(graphics, word.text(), x, y, word.scale(), color);
			x += this.font.width(word.text()) * word.scale();
		}
	}

	private void renderParagraphs(GuiGraphicsExtractor graphics, Page page, int startY, int maximumY) {
		List<List<FormattedCharSequence>> paragraphs = wrapParagraphs(page, CONTENT_WIDTH);
		int lineCount = paragraphLineCount(paragraphs);
		int available = maximumY - startY;
		int paragraphGaps = Math.max(0, page.paragraphCount() - 1);
		int lineHeight = lineCount * 9 + paragraphGaps * 3 <= available ? 9 : 8;
		int paragraphGap = lineHeight == 9 ? 3 : 1;
		if (lineCount * lineHeight + paragraphGaps * paragraphGap > available) paragraphGap = 0;
		if (isEnglishLanguage() && lineCount * lineHeight + paragraphGaps * paragraphGap > available) {
			renderScaledEnglishParagraphs(graphics, page, startY, available);
			return;
		}

		int y = startY;
		for (int paragraph = 0; paragraph < paragraphs.size(); paragraph++) {
			List<FormattedCharSequence> lines = paragraphs.get(paragraph);
			for (FormattedCharSequence line : lines) {
				if (y + lineHeight > maximumY) return;
				graphics.text(this.font, line, CONTENT_LEFT, y, INK, false);
				y += lineHeight;
			}
			if (paragraph < paragraphs.size() - 1) y += paragraphGap;
		}
	}

	private void renderScaledEnglishParagraphs(GuiGraphicsExtractor graphics, Page page, int startY, int available) {
		ScaledParagraphLayout layout = findEnglishParagraphLayout(page, available);
		graphics.pose().pushMatrix();
		graphics.pose().translate(CONTENT_LEFT, startY);
		graphics.pose().scale(layout.scale(), layout.scale());
		int y = 0;
		for (int paragraph = 0; paragraph < layout.paragraphs().size(); paragraph++) {
			for (FormattedCharSequence line : layout.paragraphs().get(paragraph)) {
				graphics.text(this.font, line, 0, y, INK, false);
				y += 9;
			}
			if (paragraph < layout.paragraphs().size() - 1) y += 2;
		}
		graphics.pose().popMatrix();
	}

	private ScaledParagraphLayout findEnglishParagraphLayout(Page page, int available) {
		for (int percent = 98; percent >= 65; percent--) {
			float scale = percent / 100.0F;
			int wrapWidth = Math.max(CONTENT_WIDTH, (int)Math.floor(CONTENT_WIDTH / scale));
			List<List<FormattedCharSequence>> paragraphs = wrapParagraphs(page, wrapWidth);
			int lineCount = paragraphLineCount(paragraphs);
			int unscaledHeight = lineCount * 9 + Math.max(0, page.paragraphCount() - 1) * 2;
			if (unscaledHeight * scale <= available) return new ScaledParagraphLayout(paragraphs, scale);
		}
		float scale = 0.65F;
		return new ScaledParagraphLayout(
				wrapParagraphs(page, (int)Math.floor(CONTENT_WIDTH / scale)), scale);
	}

	private List<List<FormattedCharSequence>> wrapParagraphs(Page page, int width) {
		List<List<FormattedCharSequence>> paragraphs = new ArrayList<>(page.paragraphCount());
		for (int paragraph = 0; paragraph < page.paragraphCount(); paragraph++) {
			paragraphs.add(this.font.split(Component.translatable(page.paragraphKey(paragraph)), width));
		}
		return paragraphs;
	}

	private static int paragraphLineCount(List<List<FormattedCharSequence>> paragraphs) {
		int count = 0;
		for (List<FormattedCharSequence> paragraph : paragraphs) count += paragraph.size();
		return count;
	}

	private void renderRecipe(GuiGraphicsExtractor graphics, String id, int x, int y, int mouseX, int mouseY) {
		TutorialRecipeCatalog.RecipeSpec recipe = TutorialRecipeCatalog.recipe(id);
		if (recipe == null) return;
		blit(graphics, RECIPE, x, y, RECIPE_WIDTH, RECIPE_HEIGHT);
		for (int slot = 0; slot < 9; slot++) {
			Identifier ingredient = recipe.ingredient(slot, gameTime());
			if (ingredient == null) continue;
			int itemX = x + 1 + slot % 3 * 18;
			int itemY = y + 1 + slot / 3 * 18;
			renderItem(graphics, stack(ingredient), itemX, itemY, mouseX, mouseY);
		}
		renderItem(graphics, stack(recipe.output()), x + 78, y + 19, mouseX, mouseY);
	}

	private void renderAttachment(GuiGraphicsExtractor graphics, String id, int x, int y, int mouseX, int mouseY) {
		renderItem(graphics, stack(EchoWarrior.id(id)), x, y, mouseX, mouseY);
	}

	private void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
		graphics.fakeItem(stack, x, y);
		if (insideLocal(mouseX, mouseY, x, y, 16, 16)) {
			graphics.setTooltipForNextFrame(this.font, stack.getHoverName(), mouseX, mouseY);
		}
	}

	private void renderControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		renderControl(graphics, CLOSE, CLOSE_SHADOW, CLOSE_X, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE, mouseX, mouseY,
				Component.translatable("gui.echo_warrior.tutorial.close"));
		int page = this.menu.currentPage();
		if (page > 0) {
			renderPageButton(graphics, PREVIOUS, PREVIOUS_SHADOW, PREVIOUS_X, PAGE_BUTTON_Y, PREVIOUS_WIDTH, mouseX, mouseY,
					Component.translatable("gui.echo_warrior.tutorial.previous"));
		}
		if (page < TutorialManualCatalog.pageCount() - 1) {
			renderPageButton(graphics, NEXT, NEXT_SHADOW, NEXT_X, PAGE_BUTTON_Y, NEXT_WIDTH, mouseX, mouseY,
					Component.translatable("gui.echo_warrior.tutorial.next"));
		}
	}

	private void renderControl(GuiGraphicsExtractor graphics, Identifier texture, Identifier shadow, int x, int y, int width, int height,
			int mouseX, int mouseY, Component tooltip) {
		boolean hovered = insideLocal(mouseX, mouseY, x - BUTTON_HIT_PADDING, y - BUTTON_HIT_PADDING,
				width + BUTTON_HIT_PADDING * 2, height + BUTTON_HIT_PADDING * 2);
		if (hovered) blit(graphics, shadow, x - 1, y + 1, width, height);
		blit(graphics, texture, x, y, width, height);
		if (hovered) graphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
	}

	private void renderPageButton(GuiGraphicsExtractor graphics, Identifier texture, Identifier shadow, int x, int y, int width,
			int mouseX, int mouseY, Component tooltip) {
		boolean hovered = insideLocal(mouseX, mouseY, x - BUTTON_HIT_PADDING, y - BUTTON_HIT_PADDING,
				width + BUTTON_HIT_PADDING * 2, PAGE_BUTTON_HEIGHT + BUTTON_HIT_PADDING * 2);
		if (hovered) blit(graphics, shadow, x - 1, y + 1, width, PAGE_BUTTON_HEIGHT);
		blit(graphics, texture, x, y, width, PAGE_BUTTON_HEIGHT);
		if (hovered) graphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
	}

	private void renderTabTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int chapterIndex = chapterAt(mouseX, mouseY);
		if (chapterIndex < 0) return;
		Chapter chapter = Chapter.values()[chapterIndex];
		List<Component> tooltip = List.of(
				Component.translatable(chapter.titleKey()).withStyle(ChatFormatting.GOLD),
				Component.translatable(chapter.subtitleKey()).withStyle(ChatFormatting.GRAY));
		graphics.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
	}

	private void renderSkillTooltip(GuiGraphicsExtractor graphics, int hero, int skill, int mouseX, int mouseY) {
		String key = HERO_SKILL_KEYS[hero][skill];
		List<Component> tooltip = new ArrayList<>();
		tooltip.add(Component.translatable(key + ".name").withStyle(ChatFormatting.GOLD));
		for (int line = 1; line <= HERO_SKILL_LINES[hero][skill]; line++) {
			tooltip.add(Component.translatable(key + ".description." + line).withStyle(ChatFormatting.GRAY));
		}
		graphics.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
		int x = (int)Math.floor(event.x() - this.leftPos);
		int y = (int)Math.floor(event.y() - this.topPos);
		if (isInsideButton(x, y, CLOSE_X, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE)) {
			this.onClose();
			return true;
		}
		int page = this.menu.currentPage();
		if (page > 0 && isInsideButton(x, y, PREVIOUS_X, PAGE_BUTTON_Y, PREVIOUS_WIDTH, PAGE_BUTTON_HEIGHT)) {
			jumpTo(page - 1);
			return true;
		}
		if (page < TutorialManualCatalog.pageCount() - 1
				&& isInsideButton(x, y, NEXT_X, PAGE_BUTTON_Y, NEXT_WIDTH, PAGE_BUTTON_HEIGHT)) {
			jumpTo(page + 1);
			return true;
		}
		int chapter = chapterAt(event.x(), event.y());
		if (chapter >= 0) {
			jumpTo(TutorialManualCatalog.firstPage(Chapter.values()[chapter]));
			return true;
		}
		if (currentPage().kind() == TutorialManualCatalog.PageKind.DISCOVERIES && y >= PAPER_Y + 149 && y < PAPER_Y + 171) {
			if (x >= PAPER_X + 29 && x < PAPER_X + 52) {
				jumpTo(TutorialManualCatalog.indexOf("knowledge_fragments"));
				return true;
			}
			if (x >= PAPER_X + 71 && x < PAPER_X + 94) {
				jumpTo(TutorialManualCatalog.indexOf("legacy"));
				return true;
			}
			if (x >= PAPER_X + 113 && x < PAPER_X + 136) {
				List<String> accessories = TutorialManualCatalog.accessoryIds();
				String accessory = accessories.get((int)(gameTime() / 40L % accessories.size()));
				jumpTo(TutorialManualCatalog.indexOf(accessory));
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void jumpTo(int page) {
		this.menu.selectClientPage(page);
		if (this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(
					this.menu.containerId, TutorialManualMenu.BUTTON_JUMP_START + page);
		}
	}

	private Page currentPage() {
		return TutorialManualCatalog.page(this.menu.currentPage());
	}

	private int chapterAt(double mouseX, double mouseY) {
		for (Chapter chapter : Chapter.values()) {
			int x = this.leftPos + tabX(chapter.ordinal());
			int y = this.topPos + tabY(chapter.ordinal());
			if (isInside(mouseX, mouseY, x, y, TAB_WIDTH, TAB_HEIGHT)) return chapter.ordinal();
		}
		return -1;
	}

	private int tabX(int chapter) {
		return TAB_BASE_X + Math.round(this.tabOffsets[chapter]);
	}

	private static int tabY(int chapter) {
		return TAB_START_Y + chapter * TAB_HEIGHT;
	}

	private long gameTime() {
		return this.minecraft != null && this.minecraft.level != null ? this.minecraft.level.getGameTime() : 0L;
	}

	private int heroIndex(String heroId) {
		return switch (heroId) {
			case "roman_legionary" -> 0;
			case "aztec_warrior" -> 1;
			case "egyptian_archer" -> 2;
			case "guandao_warrior" -> 3;
			case "japanese_samurai" -> 4;
			default -> 0;
		};
	}

	private static ItemStack stack(Identifier id) {
		Item item = BuiltInRegistries.ITEM.getValue(id);
		return item == null ? ItemStack.EMPTY : new ItemStack(item);
	}

	private void renderTitle(GuiGraphicsExtractor graphics, Component title) {
		centeredFittedEnglish(graphics, title, PAGE_CENTER_X, TITLE_Y, CONTENT_WIDTH, INK);
	}

	private void centered(GuiGraphicsExtractor graphics, Component text, int y, int color) {
		centeredAt(graphics, text, PAGE_CENTER_X, y, color);
	}

	private void centeredAt(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int color) {
		centeredAt(graphics, text.getVisualOrderText(), centerX, y, color);
	}

	private void centeredAt(GuiGraphicsExtractor graphics, FormattedCharSequence text, int centerX, int y, int color) {
		graphics.text(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
	}

	private void centeredFittedEnglish(GuiGraphicsExtractor graphics, Component text,
			int centerX, int y, int maximumWidth, int color) {
		FormattedCharSequence sequence = text.getVisualOrderText();
		int width = this.font.width(sequence);
		if (isEnglishLanguage() && width > maximumWidth) {
			drawScaledCentered(graphics, sequence, centerX, y, maximumWidth / (float)width, color);
		} else {
			centeredAt(graphics, sequence, centerX, y, color);
		}
	}

	private void drawFittedEnglish(GuiGraphicsExtractor graphics, Component text,
			int x, int y, int maximumWidth, int color) {
		FormattedCharSequence sequence = text.getVisualOrderText();
		int width = this.font.width(sequence);
		if (isEnglishLanguage() && width > maximumWidth) {
			drawScaledText(graphics, sequence, x, y, maximumWidth / (float)width, color);
		} else {
			graphics.text(this.font, sequence, x, y, color, false);
		}
	}

	private void drawScaledCentered(GuiGraphicsExtractor graphics, FormattedCharSequence text,
			int centerX, int y, float scale, int color) {
		float width = this.font.width(text) * scale;
		graphics.pose().pushMatrix();
		graphics.pose().translate(centerX - width / 2.0F, y + (9.0F - 9.0F * scale) / 2.0F);
		graphics.pose().scale(scale, scale);
		graphics.text(this.font, text, 0, 0, color, false);
		graphics.pose().popMatrix();
	}

	private void drawScaledText(GuiGraphicsExtractor graphics, String text, float x, int y, float scale, int color) {
		if (scale == 1.0F) {
			graphics.text(this.font, text, Math.round(x), y, color, false);
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y + (1.0F - scale) * 7.0F);
		graphics.pose().scale(scale, scale);
		graphics.text(this.font, text, 0, 0, color, false);
		graphics.pose().popMatrix();
	}

	private void drawScaledText(GuiGraphicsExtractor graphics, FormattedCharSequence text,
			float x, int y, float scale, int color) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y + (9.0F - 9.0F * scale) / 2.0F);
		graphics.pose().scale(scale, scale);
		graphics.text(this.font, text, 0, 0, color, false);
		graphics.pose().popMatrix();
	}

	private boolean isEnglishLanguage() {
		return this.minecraft != null && this.minecraft.getLanguageManager().getSelected().startsWith("en_");
	}

	private boolean insideLocal(double mouseX, double mouseY, int x, int y, int width, int height) {
		return isInside(mouseX, mouseY, this.leftPos + x, this.topPos + y, width, height);
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static boolean isInsideButton(double mouseX, double mouseY, int x, int y, int width, int height) {
		return isInside(mouseX, mouseY, x - BUTTON_HIT_PADDING, y - BUTTON_HIT_PADDING,
				width + BUTTON_HIT_PADDING * 2, height + BUTTON_HIT_PADDING * 2);
	}

	private static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height, width, height);
	}

	private record TeamWord(String text, float scale) {
	}

	private record ScaledParagraphLayout(List<List<FormattedCharSequence>> paragraphs, float scale) {
	}

	private record ColoredText(Component text, int color) {
	}

	private record ColoredLine(FormattedCharSequence text, int color) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}
}
