package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.item.EchoAccessoryItem1211;
import com.yuriscat.echowarrior.compat.menu.TutorialManualMenu1211;
import com.yuriscat.echowarrior.compat.tutorial.TutorialManualCatalog1211;
import com.yuriscat.echowarrior.compat.tutorial.TutorialManualCatalog1211.Chapter;
import com.yuriscat.echowarrior.compat.tutorial.TutorialManualCatalog1211.Page;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class TutorialManualScreen1211 extends AbstractContainerScreen<TutorialManualMenu1211> {
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

	private static final ResourceLocation PAPER_SHADOW = texture("paper_shadow.png");
	private static final ResourceLocation PAPER = texture("paper.png");
	private static final ResourceLocation PREVIOUS = texture("previous.png");
	private static final ResourceLocation PREVIOUS_SHADOW = texture("previous_shadow.png");
	private static final ResourceLocation NEXT = texture("next.png");
	private static final ResourceLocation NEXT_SHADOW = texture("next_shadow.png");
	private static final ResourceLocation CLOSE = texture("close.png");
	private static final ResourceLocation CLOSE_SHADOW = texture("close_shadow.png");
	private static final ResourceLocation CHAPTER_TAB = texture("chapter_tab.png");
	private static final ResourceLocation RECIPE = texture("recipe.png");
	private static final ResourceLocation CREDITS_PORTRAIT = texture("credits_portrait.png");

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
			{2, 1, 2, 2}, {1, 2, 1, 2, 2}, {2, 2, 2, 2, 2}, {1, 2, 2, 3}, {3, 2, 2, 3}
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
	private List<Component> deferredTooltip = List.of();
	private int deferredTooltipMouseX;
	private int deferredTooltipMouseY;

	public TutorialManualScreen1211(TutorialManualMenu1211 menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = IMAGE_WIDTH;
		this.imageHeight = IMAGE_HEIGHT;
		this.titleLabelX = -1000;
		this.titleLabelY = -1000;
		this.inventoryLabelX = -1000;
		this.inventoryLabelY = -1000;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		this.deferredTooltip = List.of();
		super.render(graphics, mouseX, mouseY, partialTick);
		if (!this.deferredTooltip.isEmpty()) {
			graphics.flush();
			graphics.renderComponentTooltip(
					this.font, this.deferredTooltip, this.deferredTooltipMouseX, this.deferredTooltipMouseY);
			this.deferredTooltip = List.of();
		}
	}

	private static ResourceLocation texture(String path) {
		return ModContent1211.id("textures/gui/tutorial/" + path);
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
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		this.hoveredChapter = chapterAt(mouseX, mouseY);
		renderTabs(graphics, mouseX, mouseY);
		blit(graphics, PAPER_SHADOW, this.leftPos + PAPER_X + 2, this.topPos + PAPER_Y + 2, PAPER_WIDTH, PAPER_HEIGHT);
		blit(graphics, PAPER, this.leftPos + PAPER_X, this.topPos + PAPER_Y, PAPER_WIDTH, PAPER_HEIGHT);
	}

	private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
		for (Chapter chapter : Chapter.values()) {
			int x = this.leftPos + tabX(chapter.ordinal());
			int y = this.topPos + tabY(chapter.ordinal());
			blit(graphics, CHAPTER_TAB, x, y, TAB_WIDTH, TAB_HEIGHT);
			ItemStack icon = stack(chapter.icon());
			graphics.renderItem(icon, x + TAB_ICON_X, y + TAB_ICON_Y);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
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

	private void renderCover(GuiGraphics graphics) {
		centeredFitted(graphics, Component.translatable("gui.echo_warrior.tutorial.cover.title"),
				PAGE_CENTER_X, PAPER_Y + 50, CONTENT_WIDTH, INK);
		centeredFitted(graphics, Component.translatable("gui.echo_warrior.tutorial.cover.author"),
				PAGE_CENTER_X, PAPER_Y + 93, CONTENT_WIDTH, INK);
		centeredFitted(graphics, Component.translatable("gui.echo_warrior.tutorial.cover.by"),
				PAGE_CENTER_X, PAPER_Y + 108, CONTENT_WIDTH, MUTED_INK);
		centeredFitted(graphics, Component.translatable("gui.echo_warrior.tutorial.page_number",
				this.menu.currentPage() + 1, TutorialManualCatalog1211.pageCount()),
				PAGE_CENTER_X, PAPER_Y + 184, CONTENT_WIDTH, MUTED_INK);
	}

	private void renderProsePage(GuiGraphics graphics, Page page) {
		renderTitle(graphics, Component.translatable(page.titleKey()));
		renderParagraphs(graphics, page, BODY_Y, PAPER_Y + 174);
	}

	private void renderRecipePage(GuiGraphics graphics, Page page, int mouseX, int mouseY) {
		renderTitle(graphics, Component.translatable(page.titleKey()));
		renderParagraphs(graphics, page, BODY_Y, RECIPE_Y - 5);
		renderRecipe(graphics, page.subjectId(), RECIPE_X, RECIPE_Y, mouseX, mouseY);
	}

	private void renderDiscoveries(GuiGraphics graphics, Page page, int mouseX, int mouseY) {
		renderTitle(graphics, Component.translatable(page.titleKey()));
		renderParagraphs(graphics, page, BODY_Y, PAPER_Y + 137);
		long gameTime = gameTime();
		String legacy = LEGACIES[(int)(gameTime / 40L % LEGACIES.length)];
		List<String> accessories = TutorialManualCatalog1211.accessoryIds();
		String accessory = accessories.get((int)(gameTime / 40L % accessories.size()));
		renderAttachment(graphics, "knowledge_fragment", PAPER_X + 32, PAPER_Y + 151, mouseX, mouseY);
		renderAttachment(graphics, legacy, PAPER_X + 74, PAPER_Y + 151, mouseX, mouseY);
		renderAttachment(graphics, accessory, PAPER_X + 116, PAPER_Y + 151, mouseX, mouseY);
		centeredFitted(graphics, Component.translatable("gui.echo_warrior.tutorial.attachment.click"),
				PAGE_CENTER_X, PAPER_Y + 171, CONTENT_WIDTH, MUTED_INK);
	}

	private void renderKnowledge(GuiGraphics graphics, Page page, int mouseX, int mouseY) {
		renderTitle(graphics, Component.translatable(page.titleKey()));
		renderParagraphs(graphics, page, BODY_Y, PAPER_Y + 138);
		renderAttachment(graphics, "knowledge_fragment", PAPER_X + 58, PAPER_Y + 151, mouseX, mouseY);
		renderAttachment(graphics, "knowledge_fragment_collection", PAPER_X + 91, PAPER_Y + 151, mouseX, mouseY);
	}

	private void renderLegacy(GuiGraphics graphics, Page page, int mouseX, int mouseY) {
		renderTitle(graphics, Component.translatable(page.titleKey()));
		renderParagraphs(graphics, page, BODY_Y, PAPER_Y + 138);
		int startX = PAGE_CENTER_X - (LEGACIES.length * 16 + (LEGACIES.length - 1) * 5) / 2;
		for (int index = 0; index < LEGACIES.length; index++) {
			renderAttachment(graphics, LEGACIES[index], startX + index * 21, PAPER_Y + 151, mouseX, mouseY);
		}
	}

	private void renderHero(GuiGraphics graphics, Page page, int mouseX, int mouseY) {
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
			ResourceLocation texture = ModContent1211.id("textures/gui/summoner/skills/" + HERO_SKILL_PATHS[hero][index] + ".png");
			blit(graphics, texture, skillX, PAPER_Y + 151, 16, 16);
			if (insideLocal(mouseX, mouseY, skillX, PAPER_Y + 151, 16, 16)) {
				renderSkillTooltip(graphics, hero, index, mouseX, mouseY);
			}
		}
	}

	private void renderAccessory(GuiGraphics graphics, Page page, int mouseX, int mouseY) {
		ItemStack accessory = stack(ModContent1211.id(page.subjectId()));
		renderTitle(graphics, accessory.getHoverName());
		renderItem(graphics, accessory, CONTENT_LEFT + 5, PAPER_Y + 39, mouseX, mouseY);
		if (accessory.getItem() instanceof EchoAccessoryItem1211 accessoryItem) {
			Component rarity = Component.translatable("gui.echo_warrior.tutorial.accessory.rarity",
					Component.translatable("gui.echo_warrior.tutorial.rarity." + accessoryItem.type().rarity().name().toLowerCase()));
			drawFitted(graphics, rarity, CONTENT_LEFT + 28, PAPER_Y + 41, CONTENT_WIDTH - 28, MUTED_INK);
			List<ColoredText> effects = new ArrayList<>();
			for (int index = 0; index < accessoryItem.type().effectCount(); index++) {
				boolean positive = accessoryItem.type().effectIsPositive(index);
				String key = "item.echo_warrior.accessory." + accessoryItem.type().id() + ".effect." + (index + 1);
				Component line = Component.literal(positive ? "+ " : "- ").append(Component.translatable(key));
				effects.add(new ColoredText(line, positive ? POSITIVE : NEGATIVE));
			}
			renderAccessoryEffects(graphics, effects);
		}
		drawFitted(graphics, Component.translatable("gui.echo_warrior.tutorial.recipe"),
				RECIPE_X, ACCESSORY_RECIPE_Y - 11, RECIPE_WIDTH, MUTED_INK);
		renderRecipe(graphics, page.subjectId(), RECIPE_X, ACCESSORY_RECIPE_Y, mouseX, mouseY);
	}

	private void renderAccessoryEffects(GuiGraphics graphics, List<ColoredText> effects) {
		List<ColoredLine> lines = wrapColoredLines(effects, ACCESSORY_EFFECT_WIDTH);
		if (lines.size() * 9 > ACCESSORY_EFFECT_HEIGHT) {
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
			graphics.pose().pushPose();
			graphics.pose().translate(CONTENT_LEFT + 4, ACCESSORY_EFFECT_Y, 0.0F);
			graphics.pose().scale(scale, scale, 1.0F);
			for (int index = 0; index < lines.size(); index++) {
				ColoredLine line = lines.get(index);
				graphics.drawString(this.font, line.text(), 0, index * 9, line.color(), false);
			}
			graphics.pose().popPose();
			return;
		}
		for (int index = 0; index < lines.size(); index++) {
			ColoredLine line = lines.get(index);
			graphics.drawString(this.font, line.text(), CONTENT_LEFT + 4, ACCESSORY_EFFECT_Y + index * 9, line.color(), false);
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

	private void renderThanks(GuiGraphics graphics) {
		String[] thanksLines = Component.translatable("gui.echo_warrior.tutorial.thanks").getString().split("\\n", -1);
		int textHeight = thanksLines.length * CREDITS_TEXT_HEIGHT;
		int groupHeight = CREDITS_IMAGE_SIZE + CREDITS_IMAGE_TEXT_GAP + textHeight;
		int x = PAGE_CENTER_X - CREDITS_IMAGE_SIZE / 2;
		int y = PAPER_Y + (PAPER_HEIGHT - groupHeight) / 2;
		graphics.blit(CREDITS_PORTRAIT, x, y,
				CREDITS_IMAGE_SIZE, CREDITS_IMAGE_SIZE, 0.0F, 0.0F,
				CREDITS_IMAGE_SOURCE_SIZE, CREDITS_IMAGE_SOURCE_SIZE,
				CREDITS_IMAGE_SOURCE_SIZE, CREDITS_IMAGE_SOURCE_SIZE);
		int textY = y + CREDITS_IMAGE_SIZE + CREDITS_IMAGE_TEXT_GAP;
		for (int line = 0; line < thanksLines.length; line++) {
			centeredFitted(graphics, Component.literal(thanksLines[line]), PAGE_CENTER_X,
					textY + line * CREDITS_TEXT_HEIGHT, CONTENT_WIDTH, INK);
		}
	}

	private void renderTeam(GuiGraphics graphics) {
		renderTitle(graphics, Component.translatable("gui.echo_warrior.tutorial.page.team.title"));
		if (usesWordWrappedTeamLayout()) {
			renderWordWrappedTeam(graphics);
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

	private void renderChineseTeamLine(GuiGraphics graphics, String text, int y, int color) {
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

	private void renderWordWrappedTeam(GuiGraphics graphics) {
		int y = BODY_Y;
		for (String key : ENGLISH_TEAM_LINES) {
			int color = key.endsWith("header") ? MUTED_INK : INK;
			String text = Component.translatable("gui.echo_warrior.tutorial.team." + key).getString();
			for (List<TeamWord> line : wrapTeamLine(text)) {
				renderTeamLine(graphics, line, y, color);
				y += ENGLISH_TEAM_LINE_HEIGHT;
			}
			if (!key.endsWith("header")) y += ENGLISH_TEAM_ENTRY_GAP;
		}
	}

	private List<List<TeamWord>> wrapTeamLine(String text) {
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

	private void renderTeamLine(GuiGraphics graphics, List<TeamWord> words, int y, int color) {
		float x = CONTENT_LEFT;
		for (int index = 0; index < words.size(); index++) {
			TeamWord word = words.get(index);
			if (index > 0) x += this.font.width(" ") * word.scale();
			drawScaledText(graphics, word.text(), x, y, word.scale(), color);
			x += this.font.width(word.text()) * word.scale();
		}
	}

	private void renderParagraphs(GuiGraphics graphics, Page page, int startY, int maximumY) {
		List<List<FormattedCharSequence>> paragraphs = wrapParagraphs(page, CONTENT_WIDTH);
		int lineCount = paragraphLineCount(paragraphs);
		int available = maximumY - startY;
		int paragraphGaps = Math.max(0, page.paragraphCount() - 1);
		int lineHeight = lineCount * 9 + paragraphGaps * 3 <= available ? 9 : 8;
		int paragraphGap = lineHeight == 9 ? 3 : 1;
		if (lineCount * lineHeight + paragraphGaps * paragraphGap > available) paragraphGap = 0;
		if (lineCount * lineHeight + paragraphGaps * paragraphGap > available) {
			renderScaledParagraphs(graphics, page, startY, available);
			return;
		}

		int y = startY;
		for (int paragraph = 0; paragraph < paragraphs.size(); paragraph++) {
			List<FormattedCharSequence> lines = paragraphs.get(paragraph);
			for (FormattedCharSequence line : lines) {
				if (y + lineHeight > maximumY) return;
				graphics.drawString(this.font, line, CONTENT_LEFT, y, INK, false);
				y += lineHeight;
			}
			if (paragraph < paragraphs.size() - 1) y += paragraphGap;
		}
	}

	private void renderScaledParagraphs(GuiGraphics graphics, Page page, int startY, int available) {
		ScaledParagraphLayout layout = findParagraphLayout(page, available);
		graphics.pose().pushPose();
		graphics.pose().translate(CONTENT_LEFT, startY, 0.0F);
		graphics.pose().scale(layout.scale(), layout.scale(), 1.0F);
		int y = 0;
		for (int paragraph = 0; paragraph < layout.paragraphs().size(); paragraph++) {
			for (FormattedCharSequence line : layout.paragraphs().get(paragraph)) {
				graphics.drawString(this.font, line, 0, y, INK, false);
				y += 9;
			}
			if (paragraph < layout.paragraphs().size() - 1) y += 2;
		}
		graphics.pose().popPose();
	}

	private ScaledParagraphLayout findParagraphLayout(Page page, int available) {
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

	private void renderRecipe(GuiGraphics graphics, String id, int x, int y, int mouseX, int mouseY) {
		TutorialRecipeCatalog1211.RecipeSpec recipe = TutorialRecipeCatalog1211.recipe(id);
		if (recipe == null) return;
		blit(graphics, RECIPE, x, y, RECIPE_WIDTH, RECIPE_HEIGHT);
		for (int slot = 0; slot < 9; slot++) {
			ResourceLocation ingredient = recipe.ingredient(slot, gameTime());
			if (ingredient == null) continue;
			int itemX = x + 1 + slot % 3 * 18;
			int itemY = y + 1 + slot / 3 * 18;
			renderItem(graphics, stack(ingredient), itemX, itemY, mouseX, mouseY);
		}
		renderItem(graphics, stack(recipe.output()), x + 78, y + 19, mouseX, mouseY);
	}

	private void renderAttachment(GuiGraphics graphics, String id, int x, int y, int mouseX, int mouseY) {
		renderItem(graphics, stack(ModContent1211.id(id)), x, y, mouseX, mouseY);
	}

	private void renderItem(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
		graphics.renderItem(stack, x, y);
		if (insideLocal(mouseX, mouseY, x, y, 16, 16)) {
			renderScreenTooltip(graphics, stack.getHoverName(), mouseX, mouseY);
		}
	}

	private void renderControls(GuiGraphics graphics, int mouseX, int mouseY) {
		renderControl(graphics, CLOSE, CLOSE_SHADOW, CLOSE_X, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE, mouseX, mouseY,
				Component.translatable("gui.echo_warrior.tutorial.close"));
		int page = this.menu.currentPage();
		if (page > 0) {
			renderPageButton(graphics, PREVIOUS, PREVIOUS_SHADOW, PREVIOUS_X, PAGE_BUTTON_Y, PREVIOUS_WIDTH, mouseX, mouseY,
					Component.translatable("gui.echo_warrior.tutorial.previous"));
		}
		if (page < TutorialManualCatalog1211.pageCount() - 1) {
			renderPageButton(graphics, NEXT, NEXT_SHADOW, NEXT_X, PAGE_BUTTON_Y, NEXT_WIDTH, mouseX, mouseY,
					Component.translatable("gui.echo_warrior.tutorial.next"));
		}
	}

	private void renderControl(GuiGraphics graphics, ResourceLocation texture, ResourceLocation shadow, int x, int y, int width, int height,
			int mouseX, int mouseY, Component tooltip) {
		boolean hovered = insideLocal(mouseX, mouseY, x - BUTTON_HIT_PADDING, y - BUTTON_HIT_PADDING,
				width + BUTTON_HIT_PADDING * 2, height + BUTTON_HIT_PADDING * 2);
		if (hovered) blit(graphics, shadow, x - 1, y + 1, width, height);
		blit(graphics, texture, x, y, width, height);
		if (hovered) renderScreenTooltip(graphics, tooltip, mouseX, mouseY);
	}

	private void renderPageButton(GuiGraphics graphics, ResourceLocation texture, ResourceLocation shadow, int x, int y, int width,
			int mouseX, int mouseY, Component tooltip) {
		boolean hovered = insideLocal(mouseX, mouseY, x - BUTTON_HIT_PADDING, y - BUTTON_HIT_PADDING,
				width + BUTTON_HIT_PADDING * 2, PAGE_BUTTON_HEIGHT + BUTTON_HIT_PADDING * 2);
		if (hovered) blit(graphics, shadow, x - 1, y + 1, width, PAGE_BUTTON_HEIGHT);
		blit(graphics, texture, x, y, width, PAGE_BUTTON_HEIGHT);
		if (hovered) renderScreenTooltip(graphics, tooltip, mouseX, mouseY);
	}

	private void renderTabTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
		int chapterIndex = chapterAt(mouseX, mouseY);
		if (chapterIndex < 0) return;
		Chapter chapter = Chapter.values()[chapterIndex];
		List<Component> tooltip = List.of(
				Component.translatable(chapter.titleKey()).withStyle(ChatFormatting.GOLD),
				Component.translatable(chapter.subtitleKey()).withStyle(ChatFormatting.GRAY));
		renderScreenTooltip(graphics, tooltip, mouseX, mouseY);
	}

	private void renderSkillTooltip(GuiGraphics graphics, int hero, int skill, int mouseX, int mouseY) {
		String key = HERO_SKILL_KEYS[hero][skill];
		List<Component> tooltip = new ArrayList<>();
		tooltip.add(Component.translatable(key + ".name").withStyle(ChatFormatting.GOLD));
		for (int line = 1; line <= HERO_SKILL_LINES[hero][skill]; line++) {
			tooltip.add(Component.translatable(key + ".description." + line).withStyle(ChatFormatting.GRAY));
		}
		renderScreenTooltip(graphics, tooltip, mouseX, mouseY);
	}

	private void renderScreenTooltip(GuiGraphics graphics, Component tooltip, int mouseX, int mouseY) {
		renderScreenTooltip(graphics, List.of(tooltip), mouseX, mouseY);
	}

	private void renderScreenTooltip(GuiGraphics graphics, List<Component> tooltip, int mouseX, int mouseY) {
		this.deferredTooltip = List.copyOf(tooltip);
		this.deferredTooltipMouseX = mouseX;
		this.deferredTooltipMouseY = mouseY;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);
		int x = (int)Math.floor(mouseX - this.leftPos);
		int y = (int)Math.floor(mouseY - this.topPos);
		if (isInsideButton(x, y, CLOSE_X, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE)) {
			this.onClose();
			return true;
		}
		int page = this.menu.currentPage();
		if (page > 0 && isInsideButton(x, y, PREVIOUS_X, PAGE_BUTTON_Y, PREVIOUS_WIDTH, PAGE_BUTTON_HEIGHT)) {
			jumpTo(page - 1);
			return true;
		}
		if (page < TutorialManualCatalog1211.pageCount() - 1
				&& isInsideButton(x, y, NEXT_X, PAGE_BUTTON_Y, NEXT_WIDTH, PAGE_BUTTON_HEIGHT)) {
			jumpTo(page + 1);
			return true;
		}
		int chapter = chapterAt(mouseX, mouseY);
		if (chapter >= 0) {
			jumpTo(TutorialManualCatalog1211.firstPage(Chapter.values()[chapter]));
			return true;
		}
		if (currentPage().kind() == TutorialManualCatalog1211.PageKind.DISCOVERIES && y >= PAPER_Y + 149 && y < PAPER_Y + 171) {
			if (x >= PAPER_X + 29 && x < PAPER_X + 52) {
				jumpTo(TutorialManualCatalog1211.indexOf("knowledge_fragments"));
				return true;
			}
			if (x >= PAPER_X + 71 && x < PAPER_X + 94) {
				jumpTo(TutorialManualCatalog1211.indexOf("legacy"));
				return true;
			}
			if (x >= PAPER_X + 113 && x < PAPER_X + 136) {
				List<String> accessories = TutorialManualCatalog1211.accessoryIds();
				String accessory = accessories.get((int)(gameTime() / 40L % accessories.size()));
				jumpTo(TutorialManualCatalog1211.indexOf(accessory));
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void jumpTo(int page) {
		this.menu.selectClientPage(page);
		if (this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(
					this.menu.containerId, TutorialManualMenu1211.BUTTON_JUMP_START + page);
		}
	}

	private Page currentPage() {
		return TutorialManualCatalog1211.page(this.menu.currentPage());
	}

	private int chapterAt(double mouseX, double mouseY) {
		int paperLeft = this.leftPos + PAPER_X;
		for (Chapter chapter : Chapter.values()) {
			int x = this.leftPos + tabX(chapter.ordinal());
			int y = this.topPos + tabY(chapter.ordinal());
			int exposedWidth = Math.clamp(paperLeft - x, 0, TAB_WIDTH);
			if (isInside(mouseX, mouseY, x, y, exposedWidth, TAB_HEIGHT)) return chapter.ordinal();
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

	private static ItemStack stack(ResourceLocation id) {
		Item item = BuiltInRegistries.ITEM.get(id);
		return item == null ? ItemStack.EMPTY : new ItemStack(item);
	}

	private void renderTitle(GuiGraphics graphics, Component title) {
		centeredFitted(graphics, title, PAGE_CENTER_X, TITLE_Y, CONTENT_WIDTH, INK);
	}

	private void centered(GuiGraphics graphics, Component text, int y, int color) {
		centeredAt(graphics, text, PAGE_CENTER_X, y, color);
	}

	private void centeredAt(GuiGraphics graphics, Component text, int centerX, int y, int color) {
		centeredAt(graphics, text.getVisualOrderText(), centerX, y, color);
	}

	private void centeredAt(GuiGraphics graphics, FormattedCharSequence text, int centerX, int y, int color) {
		graphics.drawString(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
	}

	private void centeredFitted(GuiGraphics graphics, Component text,
			int centerX, int y, int maximumWidth, int color) {
		FormattedCharSequence sequence = text.getVisualOrderText();
		int width = this.font.width(sequence);
		if (width > maximumWidth) {
			drawScaledCentered(graphics, sequence, centerX, y, maximumWidth / (float)width, color);
		} else {
			centeredAt(graphics, sequence, centerX, y, color);
		}
	}

	private void drawFitted(GuiGraphics graphics, Component text,
			int x, int y, int maximumWidth, int color) {
		FormattedCharSequence sequence = text.getVisualOrderText();
		int width = this.font.width(sequence);
		if (width > maximumWidth) {
			drawScaledText(graphics, sequence, x, y, maximumWidth / (float)width, color);
		} else {
			graphics.drawString(this.font, sequence, x, y, color, false);
		}
	}

	private void drawScaledCentered(GuiGraphics graphics, FormattedCharSequence text,
			int centerX, int y, float scale, int color) {
		float width = this.font.width(text) * scale;
		graphics.pose().pushPose();
		graphics.pose().translate(centerX - width / 2.0F, y + (9.0F - 9.0F * scale) / 2.0F, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.drawString(this.font, text, 0, 0, color, false);
		graphics.pose().popPose();
	}

	private void drawScaledText(GuiGraphics graphics, String text, float x, int y, float scale, int color) {
		if (scale == 1.0F) {
			graphics.drawString(this.font, text, Math.round(x), y, color, false);
			return;
		}
		graphics.pose().pushPose();
		graphics.pose().translate(x, y + (1.0F - scale) * 7.0F, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.drawString(this.font, text, 0, 0, color, false);
		graphics.pose().popPose();
	}

	private void drawScaledText(GuiGraphics graphics, FormattedCharSequence text,
			float x, int y, float scale, int color) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y + (9.0F - 9.0F * scale) / 2.0F, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.drawString(this.font, text, 0, 0, color, false);
		graphics.pose().popPose();
	}

	private boolean usesWordWrappedTeamLayout() {
		if (this.minecraft == null) return true;
		String language = this.minecraft.getLanguageManager().getSelected();
		return !language.startsWith("zh_") && !language.startsWith("ja_") && !language.startsWith("ko_");
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

	private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
		graphics.blit(texture, x, y, 0.0F, 0.0F, width, height, width, height);
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

}
