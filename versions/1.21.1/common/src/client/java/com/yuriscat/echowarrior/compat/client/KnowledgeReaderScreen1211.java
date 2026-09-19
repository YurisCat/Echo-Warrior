package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeCatalog1211;
import com.yuriscat.echowarrior.compat.knowledge.KnowledgeStackData1211;
import com.yuriscat.echowarrior.compat.menu.KnowledgeReaderMenu1211;
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

import java.util.List;

public final class KnowledgeReaderScreen1211 extends AbstractContainerScreen<KnowledgeReaderMenu1211> {
    private static final int IMAGE_WIDTH = 192;
    private static final int IMAGE_HEIGHT = 197;
    private static final int INK = 0x745B44;
    private static final int MUTED_INK = 0xA48C67;
    private static final int HIGHLIGHT = 0xCEB785;
    private static final int CONTENT_LEFT = 38;
    private static final int CONTENT_WIDTH = 124;
    private static final int BODY_Y = 55;
    private static final int BODY_LINE_HEIGHT = 9;
    private static final int MINIMUM_BODY_SCALE_PERCENT = 65;
    private static final int ILLUSTRATION_Y = 154;
    private static final int BUTTON_PADDING = 2;
    private static final ResourceLocation COLLECTION = texture("knowledge_collection.png");
    private static final ResourceLocation FRAGMENT = texture("knowledge_fragment.png");
    private static final ResourceLocation PREVIOUS = texture("knowledge_previous.png");
    private static final ResourceLocation PREVIOUS_SHADOW = texture("knowledge_previous_shadow.png");
    private static final ResourceLocation NEXT = texture("knowledge_next.png");
    private static final ResourceLocation NEXT_SHADOW = texture("knowledge_next_shadow.png");
    private static final ResourceLocation EXTRACT = texture("knowledge_extract.png");
    private static final ResourceLocation EXTRACT_SHADOW = texture("knowledge_extract_shadow.png");
    private static final ResourceLocation CLOSE = texture("knowledge_close.png");
    private static final ResourceLocation CLOSE_SHADOW = texture("knowledge_close_shadow.png");
    private String selectedKnowledgeId;

    public KnowledgeReaderScreen1211(KnowledgeReaderMenu1211 menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
        this.titleLabelX = -1000;
        this.titleLabelY = -1000;
        this.inventoryLabelX = -1000;
        this.inventoryLabelY = -1000;
        this.selectedKnowledgeId = menu.currentKnowledgeId();
    }

    private static ResourceLocation texture(String name) {
        return ModContent1211.id("textures/gui/knowledge/" + name);
    }

    @Override
    protected void init() {
        super.init();
        reconcilePage();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        reconcilePage();
    }

    private void reconcilePage() {
        List<String> pages = this.menu.visiblePages();
        if (pages.isEmpty()) {
            this.onClose();
            return;
        }
        if (!pages.contains(this.selectedKnowledgeId)) {
            String bookmark = KnowledgeStackData1211.bookmark(this.menu.sourceStack());
            this.selectedKnowledgeId = pages.contains(bookmark) ? bookmark : pages.get(0);
            this.menu.selectClientPage(this.selectedKnowledgeId);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        boolean collection = this.menu.isCollection();
        int width = collection ? 163 : 146;
        int height = collection ? 193 : 181;
        int x = this.leftPos + (collection ? 14 : 23);
        int y = this.topPos + (collection ? 2 : 8);
        graphics.blit(collection ? COLLECTION : FRAGMENT, x, y, 0.0F, 0.0F, width, height, width, height);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        KnowledgeCatalog1211.entry(this.selectedKnowledgeId).ifPresent(entry -> {
            drawCenteredFitted(graphics,
                    Component.translatable(KnowledgeCatalog1211.cultureTranslationKey(entry.culture())), 24, MUTED_INK);
            drawCenteredFitted(graphics, Component.translatable(entry.titleKey()), 38, INK);
            renderBody(graphics, entry);
            int count = this.menu.pageCount(this.selectedKnowledgeId);
            if (count > 1) graphics.drawString(this.font, "x" + count, 31, 24, MUTED_INK, false);
            renderIllustrations(graphics, entry, mouseX, mouseY);
        });

        renderButton(graphics, CLOSE, CLOSE_SHADOW, 152, 13, 11, 11, mouseX, mouseY,
                Component.translatable("gui.echo_warrior.knowledge.close"));
        if (!this.menu.isCollection()) return;
        List<String> pages = this.menu.visiblePages();
        int index = pages.indexOf(this.selectedKnowledgeId);
        if (index > 0) renderButton(graphics, PREVIOUS, PREVIOUS_SHADOW, 6, 86, 15, 13, mouseX, mouseY,
                Component.translatable("gui.echo_warrior.knowledge.previous"));
        if (index >= 0 && index < pages.size() - 1) {
            renderButton(graphics, NEXT, NEXT_SHADOW, 171, 86, 15, 13, mouseX, mouseY,
                    Component.translatable("gui.echo_warrior.knowledge.next"));
        }
        renderButton(graphics, EXTRACT, EXTRACT_SHADOW, 171, 176, 16, 13, mouseX, mouseY,
                Component.translatable("gui.echo_warrior.knowledge.extract"));
    }

    private void renderBody(GuiGraphics graphics, KnowledgeCatalog1211.Entry entry) {
        Component body = Component.translatable(entry.bodyKey());
        int bodyBottom = entry.illustrations().isEmpty() ? 174 : ILLUSTRATION_Y - 5;
        int available = bodyBottom - BODY_Y;
        List<FormattedCharSequence> lines = this.font.split(body, CONTENT_WIDTH);
        float scale = 1.0F;
        if (lines.size() * BODY_LINE_HEIGHT > available) {
            scale = MINIMUM_BODY_SCALE_PERCENT / 100.0F;
            for (int percent = 99; percent >= MINIMUM_BODY_SCALE_PERCENT; percent--) {
                float candidate = percent / 100.0F;
                List<FormattedCharSequence> candidateLines = this.font.split(body,
                        (int)Math.floor(CONTENT_WIDTH / candidate));
                if (candidateLines.size() * BODY_LINE_HEIGHT * candidate <= available) {
                    scale = candidate;
                    lines = candidateLines;
                    break;
                }
            }
        }
        if (scale == 1.0F) {
            for (int index = 0; index < lines.size(); index++) {
                graphics.drawString(this.font, lines.get(index), CONTENT_LEFT,
                        BODY_Y + index * BODY_LINE_HEIGHT, INK, false);
            }
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(CONTENT_LEFT, BODY_Y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(this.font, lines.get(index), 0, index * BODY_LINE_HEIGHT, INK, false);
        }
        graphics.pose().popPose();
    }

    private void renderIllustrations(GuiGraphics graphics, KnowledgeCatalog1211.Entry entry, int mouseX, int mouseY) {
        List<KnowledgeCatalog1211.Illustration> illustrations = entry.illustrations();
        if (illustrations.isEmpty()) return;
        int gap = illustrations.size() == 1 ? 0 : 4;
        int startX = IMAGE_WIDTH / 2 - (illustrations.size() * 16 + (illustrations.size() - 1) * gap) / 2;
        for (int index = 0; index < illustrations.size(); index++) {
            KnowledgeCatalog1211.Illustration illustration = illustrations.get(index);
            int x = startX + index * (16 + gap);
            boolean hovered = inside(mouseX, mouseY, x, ILLUSTRATION_Y, 16, 16);
            if (illustration.isItem()) {
                Item item = BuiltInRegistries.ITEM.get(illustration.resource());
                ItemStack stack = new ItemStack(item);
                ResourceLocation processed = illustrationTexture(illustration.resource(), hovered ? "" : "_faded");
                graphics.blit(processed, x, ILLUSTRATION_Y, 0.0F, 0.0F, 16, 16, 16, 16);
                if (hovered) graphics.renderTooltip(this.font, stack.getHoverName(), mouseX, mouseY);
            } else {
                graphics.blit(illustration.resource(), x, ILLUSTRATION_Y, 0.0F, 0.0F, 16, 16, 16, 16);
                if (hovered && !illustration.nameKey().isEmpty()) {
                    graphics.renderTooltip(this.font, Component.translatable(illustration.nameKey()), mouseX, mouseY);
                }
            }
            if (hovered) drawHighlight(graphics, x, ILLUSTRATION_Y);
        }
    }

    private static ResourceLocation illustrationTexture(ResourceLocation item, String suffix) {
        String name = item.toString().replace(':', '_').replace('/', '_');
        return ModContent1211.id("textures/gui/knowledge/illustrations/" + name + suffix + ".png");
    }

    private static void drawHighlight(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y, HIGHLIGHT);
        graphics.fill(x - 1, y + 16, x + 17, y + 17, HIGHLIGHT);
        graphics.fill(x - 1, y, x, y + 16, HIGHLIGHT);
        graphics.fill(x + 16, y, x + 17, y + 16, HIGHLIGHT);
    }

    private void renderButton(GuiGraphics graphics, ResourceLocation texture, ResourceLocation shadow,
                              int x, int y, int width, int height, int mouseX, int mouseY, Component tooltip) {
        boolean hovered = inside(mouseX, mouseY, x - BUTTON_PADDING, y - BUTTON_PADDING,
                width + BUTTON_PADDING * 2, height + BUTTON_PADDING * 2);
        if (hovered) graphics.blit(shadow, x - 1, y + 1, 0.0F, 0.0F, width, height, width, height);
        graphics.blit(texture, x, y, 0.0F, 0.0F, width, height, width, height);
        if (hovered) graphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private void drawCenteredFitted(GuiGraphics graphics, Component text, int y, int color) {
        FormattedCharSequence sequence = text.getVisualOrderText();
        int width = this.font.width(sequence);
        if (width <= CONTENT_WIDTH) {
            graphics.drawString(this.font, sequence, IMAGE_WIDTH / 2 - width / 2, y, color, false);
            return;
        }
        float scale = CONTENT_WIDTH / (float)width;
        graphics.pose().pushPose();
        graphics.pose().translate(IMAGE_WIDTH / 2.0F - width * scale / 2.0F, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, sequence, 0, 0, color, false);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(mouseX, mouseY, button);
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        if (inside(x, y, 152 - BUTTON_PADDING, 13 - BUTTON_PADDING,
                11 + BUTTON_PADDING * 2, 11 + BUTTON_PADDING * 2)) {
            this.onClose();
            return true;
        }
        if (!this.menu.isCollection()) return super.mouseClicked(mouseX, mouseY, button);
        if (inside(x, y, 6 - BUTTON_PADDING, 86 - BUTTON_PADDING, 15 + BUTTON_PADDING * 2, 13 + BUTTON_PADDING * 2)) {
            return changePage(-1);
        }
        if (inside(x, y, 171 - BUTTON_PADDING, 86 - BUTTON_PADDING, 15 + BUTTON_PADDING * 2, 13 + BUTTON_PADDING * 2)) {
            return changePage(1);
        }
        if (inside(x, y, 171 - BUTTON_PADDING, 176 - BUTTON_PADDING, 16 + BUTTON_PADDING * 2, 13 + BUTTON_PADDING * 2)) {
            sendButton(KnowledgeReaderMenu1211.BUTTON_EXTRACT);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) return changePage(-1);
        if (keyCode == GLFW.GLFW_KEY_RIGHT) return changePage(1);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean changePage(int direction) {
        List<String> pages = this.menu.visiblePages();
        int index = pages.indexOf(this.selectedKnowledgeId);
        int next = index + direction;
        if (next < 0 || next >= pages.size()) return false;
        this.selectedKnowledgeId = pages.get(next);
        this.menu.selectClientPage(this.selectedKnowledgeId);
        sendButton(direction < 0 ? KnowledgeReaderMenu1211.BUTTON_PREVIOUS : KnowledgeReaderMenu1211.BUTTON_NEXT);
        return true;
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
