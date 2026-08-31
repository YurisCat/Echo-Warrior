package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.knowledge.KnowledgeCatalog;
import com.yuriscat.echowarrior.knowledge.KnowledgeStackData;
import com.yuriscat.echowarrior.menu.KnowledgeReaderMenu;
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

import java.util.List;

public final class KnowledgeReaderScreen extends AbstractContainerScreen<KnowledgeReaderMenu> {
	private static final int IMAGE_WIDTH = 192;
	private static final int IMAGE_HEIGHT = 197;
	private static final int INK = 0xFF745B44;
	private static final int MUTED_INK = 0xFFA48C67;
	private static final int ILLUSTRATION_HIGHLIGHT = 0xFFCEB785;
	private static final int BUTTON_HIT_PADDING = 2;
	private static final int CLOSE_WIDTH = 11;
	private static final int CLOSE_HEIGHT = 11;
	private static final int PAGE_BUTTON_WIDTH = 15;
	private static final int PAGE_BUTTON_HEIGHT = 13;
	private static final int EXTRACT_WIDTH = 16;
	private static final int EXTRACT_HEIGHT = 13;
	private static final int PAGE_CENTER_X = 96;
	private static final int CONTENT_LEFT = 38;
	private static final int CONTENT_WIDTH = 124;
	private static final int CULTURE_Y = 24;
	private static final int TITLE_Y = 38;
	private static final int BODY_Y = 55;
	private static final int BODY_LINE_HEIGHT = 9;
	private static final int COUNT_X = 31;
	private static final int COUNT_Y = 24;
	private static final int ILLUSTRATION_Y = 154;
	private static final int BODY_BOTTOM_WITH_ILLUSTRATION = ILLUSTRATION_Y - 5;
	private static final int BODY_BOTTOM_WITHOUT_ILLUSTRATION = 174;
	private static final int MINIMUM_ENGLISH_BODY_SCALE_PERCENT = 65;

	private static final Identifier COLLECTION_BACKGROUND = EchoWarrior.id("textures/gui/knowledge/knowledge_collection.png");
	private static final Identifier FRAGMENT_BACKGROUND = EchoWarrior.id("textures/gui/knowledge/knowledge_fragment.png");
	private static final Identifier PREVIOUS_BUTTON = EchoWarrior.id("textures/gui/knowledge/knowledge_previous.png");
	private static final Identifier NEXT_BUTTON = EchoWarrior.id("textures/gui/knowledge/knowledge_next.png");
	private static final Identifier EXTRACT_BUTTON = EchoWarrior.id("textures/gui/knowledge/knowledge_extract.png");
	private static final Identifier CLOSE_BUTTON = EchoWarrior.id("textures/gui/knowledge/knowledge_close.png");
	private static final Identifier PREVIOUS_BUTTON_SHADOW = EchoWarrior.id("textures/gui/knowledge/knowledge_previous_shadow.png");
	private static final Identifier NEXT_BUTTON_SHADOW = EchoWarrior.id("textures/gui/knowledge/knowledge_next_shadow.png");
	private static final Identifier EXTRACT_BUTTON_SHADOW = EchoWarrior.id("textures/gui/knowledge/knowledge_extract_shadow.png");
	private static final Identifier CLOSE_BUTTON_SHADOW = EchoWarrior.id("textures/gui/knowledge/knowledge_close_shadow.png");

	private static final int COLLECTION_X = 14;
	private static final int COLLECTION_Y = 2;
	private static final int COLLECTION_WIDTH = 163;
	private static final int COLLECTION_HEIGHT = 193;
	private static final int FRAGMENT_X = 23;
	private static final int FRAGMENT_Y = 8;
	private static final int FRAGMENT_WIDTH = 146;
	private static final int FRAGMENT_HEIGHT = 181;
	private static final int PREVIOUS_X = 6;
	private static final int NEXT_X = 171;
	private static final int PAGE_BUTTON_Y = 86;
	private static final int EXTRACT_X = 171;
	private static final int EXTRACT_Y = 176;
	private static final int CLOSE_X = 152;
	private static final int CLOSE_Y = 13;

	private String selectedKnowledgeId;

	public KnowledgeReaderScreen(KnowledgeReaderMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
		this.titleLabelX = -1000;
		this.titleLabelY = -1000;
		this.inventoryLabelX = -1000;
		this.inventoryLabelY = -1000;
		this.selectedKnowledgeId = menu.currentKnowledgeId();
	}

	@Override
	protected void init() {
		super.init();
		reconcileSelectedPage();
	}

	@Override
	public void containerTick() {
		super.containerTick();
		reconcileSelectedPage();
	}

	private void reconcileSelectedPage() {
		List<String> pages = this.menu.visiblePages();
		if (pages.isEmpty()) {
			this.onClose();
			return;
		}
		if (!pages.contains(this.selectedKnowledgeId)) {
			String bookmark = KnowledgeStackData.bookmark(this.menu.sourceStack());
			this.selectedKnowledgeId = pages.contains(bookmark) ? bookmark : pages.getFirst();
			this.menu.selectClientPage(this.selectedKnowledgeId);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractTransparentBackground(graphics);
		PageGeometry page = pageGeometry();
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				page.collection() ? COLLECTION_BACKGROUND : FRAGMENT_BACKGROUND,
				this.leftPos + page.x(),
				this.topPos + page.y(),
				0.0F,
				0.0F,
				page.width(),
				page.height(),
				page.textureWidth(),
				page.textureHeight(),
				page.textureWidth(),
				page.textureHeight()
		);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		KnowledgeCatalog.entry(this.selectedKnowledgeId).ifPresent(entry -> {
			PageGeometry page = pageGeometry();
			drawCenteredFittedEnglish(
					graphics,
					Component.translatable(KnowledgeCatalog.cultureTranslationKey(entry.culture())),
					CULTURE_Y,
					MUTED_INK
			);
			drawCenteredFittedEnglish(graphics, Component.translatable(entry.titleKey()), TITLE_Y, INK);
			renderBody(graphics, entry);

			int count = this.menu.pageCount(this.selectedKnowledgeId);
			if (count > 1) {
				graphics.text(this.font, "x" + count, COUNT_X, COUNT_Y, MUTED_INK, false);
			}
			renderIllustrations(graphics, entry, page, mouseX, mouseY);
			renderControls(graphics, page, mouseX, mouseY);
		});
	}

	private void renderBody(GuiGraphicsExtractor graphics, KnowledgeCatalog.Entry entry) {
		Component body = Component.translatable(entry.bodyKey());
		List<FormattedCharSequence> lines = this.font.split(body, CONTENT_WIDTH);
		int bodyBottom = entry.illustrations().isEmpty()
				? BODY_BOTTOM_WITHOUT_ILLUSTRATION
				: BODY_BOTTOM_WITH_ILLUSTRATION;
		int availableHeight = bodyBottom - BODY_Y;
		if (!isEnglishLanguage() || lines.size() * BODY_LINE_HEIGHT <= availableHeight) {
			renderBodyLines(graphics, lines, 1.0F);
			return;
		}

		float scale = MINIMUM_ENGLISH_BODY_SCALE_PERCENT / 100.0F;
		lines = this.font.split(body, (int)Math.floor(CONTENT_WIDTH / scale));
		for (int percent = 99; percent >= MINIMUM_ENGLISH_BODY_SCALE_PERCENT; percent--) {
			float candidate = percent / 100.0F;
			List<FormattedCharSequence> candidateLines = this.font.split(
					body,
					(int)Math.floor(CONTENT_WIDTH / candidate)
			);
			if (candidateLines.size() * BODY_LINE_HEIGHT * candidate <= availableHeight) {
				scale = candidate;
				lines = candidateLines;
				break;
			}
		}
		renderBodyLines(graphics, lines, scale);
	}

	private void renderBodyLines(GuiGraphicsExtractor graphics, List<FormattedCharSequence> lines, float scale) {
		if (scale == 1.0F) {
			for (int index = 0; index < lines.size(); index++) {
				graphics.text(this.font, lines.get(index), CONTENT_LEFT, BODY_Y + index * BODY_LINE_HEIGHT, INK, false);
			}
			return;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(CONTENT_LEFT, BODY_Y);
		graphics.pose().scale(scale, scale);
		for (int index = 0; index < lines.size(); index++) {
			graphics.text(this.font, lines.get(index), 0, index * BODY_LINE_HEIGHT, INK, false);
		}
		graphics.pose().popMatrix();
	}

	private void renderControls(GuiGraphicsExtractor graphics, PageGeometry page, int mouseX, int mouseY) {
		renderButton(graphics, CLOSE_BUTTON, CLOSE_BUTTON_SHADOW, CLOSE_X, CLOSE_Y, CLOSE_WIDTH, CLOSE_HEIGHT, mouseX, mouseY,
				Component.translatable("gui.echo_warrior.knowledge.close"));

		if (!page.collection()) return;
		List<String> pages = this.menu.visiblePages();
		int index = pages.indexOf(this.selectedKnowledgeId);
		if (index > 0) {
			renderButton(graphics, PREVIOUS_BUTTON, PREVIOUS_BUTTON_SHADOW, PREVIOUS_X, PAGE_BUTTON_Y, PAGE_BUTTON_WIDTH,
					PAGE_BUTTON_HEIGHT, mouseX, mouseY,
					Component.translatable("gui.echo_warrior.knowledge.previous"));
		}
		if (index >= 0 && index < pages.size() - 1) {
			renderButton(graphics, NEXT_BUTTON, NEXT_BUTTON_SHADOW, NEXT_X, PAGE_BUTTON_Y, PAGE_BUTTON_WIDTH,
					PAGE_BUTTON_HEIGHT, mouseX, mouseY,
					Component.translatable("gui.echo_warrior.knowledge.next"));
		}
		renderButton(graphics, EXTRACT_BUTTON, EXTRACT_BUTTON_SHADOW, EXTRACT_X, EXTRACT_Y, EXTRACT_WIDTH, EXTRACT_HEIGHT, mouseX, mouseY,
				Component.translatable("gui.echo_warrior.knowledge.extract"));
	}

	private void renderButton(GuiGraphicsExtractor graphics, Identifier texture, Identifier shadowTexture,
			int x, int y, int width, int height,
			int mouseX, int mouseY, Component tooltip) {
		boolean hovered = isInside(mouseX, mouseY, this.leftPos + x - BUTTON_HIT_PADDING,
				this.topPos + y - BUTTON_HIT_PADDING, width + BUTTON_HIT_PADDING * 2, height + BUTTON_HIT_PADDING * 2);
		if (hovered) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, shadowTexture, x + 1, y + 1, 0.0F, 0.0F,
					width, height, width, height);
		}
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height, width, height);
		if (hovered) graphics.setTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
	}

	private void renderIllustrations(GuiGraphicsExtractor graphics, KnowledgeCatalog.Entry entry, PageGeometry page,
			int mouseX, int mouseY) {
		List<KnowledgeCatalog.Illustration> illustrations = entry.illustrations();
		if (illustrations.isEmpty()) return;
		int size = 16;
		int gap = illustrations.size() == 1 ? 0 : 4;
		int totalWidth = illustrations.size() * size + (illustrations.size() - 1) * gap;
		int startX = PAGE_CENTER_X - totalWidth / 2;
		int y = ILLUSTRATION_Y;

		for (int index = 0; index < illustrations.size(); index++) {
			KnowledgeCatalog.Illustration illustration = illustrations.get(index);
			int x = startX + index * (size + gap);
			boolean hovered = isInside(mouseX, mouseY, this.leftPos + x, this.topPos + y, size, size);
			if (illustration.isItem()) {
				Item item = BuiltInRegistries.ITEM.getValue(illustration.resource());
				if (item == null) continue;
				ItemStack stack = new ItemStack(item);
				Identifier texture = hovered
						? processedIllustrationTexture(illustration.resource())
						: fadedIllustrationTexture(illustration.resource());
				graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size, size, size);
				if (hovered) {
					drawIllustrationHighlight(graphics, x, y, size);
					graphics.setTooltipForNextFrame(this.font, stack.getHoverName(), mouseX, mouseY);
				}
			} else {
				graphics.blit(RenderPipelines.GUI_TEXTURED, illustration.resource(), x, y, 0.0F, 0.0F,
						size, size, size, size);
				if (hovered && !illustration.nameKey().isEmpty()) {
					drawIllustrationHighlight(graphics, x, y, size);
					graphics.setTooltipForNextFrame(this.font, Component.translatable(illustration.nameKey()), mouseX, mouseY);
				}
			}
		}
	}

	private static void drawIllustrationHighlight(GuiGraphicsExtractor graphics, int x, int y, int size) {
		graphics.fill(x - 1, y - 1, x + size + 1, y, ILLUSTRATION_HIGHLIGHT);
		graphics.fill(x - 1, y + size, x + size + 1, y + size + 1, ILLUSTRATION_HIGHLIGHT);
		graphics.fill(x - 1, y, x, y + size, ILLUSTRATION_HIGHLIGHT);
		graphics.fill(x + size, y, x + size + 1, y + size, ILLUSTRATION_HIGHLIGHT);
	}

	private void drawCenteredFittedEnglish(GuiGraphicsExtractor graphics, Component text, int y, int color) {
		FormattedCharSequence sequence = text.getVisualOrderText();
		int width = this.font.width(sequence);
		if (!isEnglishLanguage() || width <= CONTENT_WIDTH) {
			graphics.text(this.font, sequence, PAGE_CENTER_X - width / 2, y, color, false);
			return;
		}
		float scale = CONTENT_WIDTH / (float)width;
		graphics.pose().pushMatrix();
		graphics.pose().translate(PAGE_CENTER_X - width * scale / 2.0F, y + (9.0F - 9.0F * scale) / 2.0F);
		graphics.pose().scale(scale, scale);
		graphics.text(this.font, sequence, 0, 0, color, false);
		graphics.pose().popMatrix();
	}

	private boolean isEnglishLanguage() {
		return this.minecraft != null && this.minecraft.getLanguageManager().getSelected().startsWith("en_");
	}

	private static Identifier processedIllustrationTexture(Identifier item) {
		String name = item.toString().replace(':', '_').replace('/', '_');
		return EchoWarrior.id("textures/gui/knowledge/illustrations/" + name + ".png");
	}

	private static Identifier fadedIllustrationTexture(Identifier item) {
		String name = item.toString().replace(':', '_').replace('/', '_');
		return EchoWarrior.id("textures/gui/knowledge/illustrations/" + name + "_faded.png");
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
		PageGeometry page = pageGeometry();
		int localX = (int)Math.floor(event.x() - this.leftPos);
		int localY = (int)Math.floor(event.y() - this.topPos);

		if (isInside(localX, localY, CLOSE_X - BUTTON_HIT_PADDING, CLOSE_Y - BUTTON_HIT_PADDING,
				CLOSE_WIDTH + BUTTON_HIT_PADDING * 2, CLOSE_HEIGHT + BUTTON_HIT_PADDING * 2)) {
			this.onClose();
			return true;
		}
		if (!page.collection()) return super.mouseClicked(event, doubleClick);

		List<String> pages = this.menu.visiblePages();
		int index = pages.indexOf(this.selectedKnowledgeId);
		if (index > 0 && isInsideButton(localX, localY, PREVIOUS_X, PAGE_BUTTON_Y, PAGE_BUTTON_WIDTH,
				PAGE_BUTTON_HEIGHT)) {
			this.selectedKnowledgeId = pages.get(index - 1);
			this.menu.selectClientPage(this.selectedKnowledgeId);
			sendMenuButton(KnowledgeReaderMenu.BUTTON_PREVIOUS);
			return true;
		}
		if (index >= 0 && index < pages.size() - 1
				&& isInsideButton(localX, localY, NEXT_X, PAGE_BUTTON_Y, PAGE_BUTTON_WIDTH, PAGE_BUTTON_HEIGHT)) {
			this.selectedKnowledgeId = pages.get(index + 1);
			this.menu.selectClientPage(this.selectedKnowledgeId);
			sendMenuButton(KnowledgeReaderMenu.BUTTON_NEXT);
			return true;
		}
		if (isInsideButton(localX, localY, EXTRACT_X, EXTRACT_Y, EXTRACT_WIDTH, EXTRACT_HEIGHT)) {
			sendMenuButton(KnowledgeReaderMenu.BUTTON_EXTRACT);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void sendMenuButton(int button) {
		if (this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
		}
	}

	private PageGeometry pageGeometry() {
		if (this.menu.isCollection()) {
			return new PageGeometry(true, COLLECTION_X, COLLECTION_Y, COLLECTION_WIDTH, COLLECTION_HEIGHT);
		}
		return new PageGeometry(false, FRAGMENT_X, FRAGMENT_Y, FRAGMENT_WIDTH, FRAGMENT_HEIGHT);
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static boolean isInsideButton(double mouseX, double mouseY, int x, int y, int width, int height) {
		return isInside(mouseX, mouseY, x - BUTTON_HIT_PADDING, y - BUTTON_HIT_PADDING,
				width + BUTTON_HIT_PADDING * 2, height + BUTTON_HIT_PADDING * 2);
	}

	private record PageGeometry(boolean collection, int x, int y, int width, int height) {
		private int textureWidth() {
			return this.width;
		}

		private int textureHeight() {
			return this.height;
		}
	}
}
