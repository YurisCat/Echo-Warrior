package com.yuriscat.echowarrior.client;

import com.yuriscat.echowarrior.EchoWarrior;
import com.yuriscat.echowarrior.ModEntities;
import com.yuriscat.echowarrior.ModItems;
import com.yuriscat.echowarrior.item.EchoHeroType;
import com.yuriscat.echowarrior.item.EchoRelicItem;
import com.yuriscat.echowarrior.layout.SummonerLayout;
import com.yuriscat.echowarrior.layout.SummonerLayout.Element;
import com.yuriscat.echowarrior.layout.SummonerLayout.Offset;
import com.yuriscat.echowarrior.menu.SummonerMenu;
import com.yuriscat.echowarrior.item.SummonerFuel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public final class SummonerPreviewScreen extends AbstractContainerScreen<SummonerMenu> {
	private static final int IMAGE_WIDTH = SummonerLayout.GUI_WIDTH;
	private static final int IMAGE_HEIGHT = SummonerLayout.GUI_HEIGHT;
	private static final int PRIMARY_TEXT_COLOR = 0xFFFFFFFF;
	private static final int HEADER_TEXT_COLOR = 0xFFE0E0E0;
	private static final int[] MODULE_SLOT_X = {8, 37, 66, 94, 123, 152};
	private static final int[] ATTRIBUTE_ICON_X = {61, 134, 61, 116, 61, 116, 61, 116};
	private static final int[] ATTRIBUTE_ICON_Y = {19, 19, 32, 32, 45, 45, 58, 58};

	private static final Identifier BACKGROUND = EchoWarrior.id("textures/gui/summoner/summoner_screen.png");
	private static final Identifier[] ATTRIBUTE_ICONS = {
			icon("attributes/health.png"),
			icon("attributes/level.png"),
			icon("attributes/attack_damage.png"),
			icon("attributes/attack_speed.png"),
			icon("attributes/armor.png"),
			icon("attributes/movement_speed.png"),
			icon("attributes/alert_range.png"),
			icon("attributes/summon_cost_ratio.png")
	};
	private static final Identifier[][] SKILL_ICONS = {
			{
					icon("skills/roman_legionary/soldier_formation.png"),
					icon("skills/roman_legionary/shield_charge.png"),
					icon("skills/roman_legionary/legion_endures.png")
			},
			{
					icon("skills/aztec_warrior/quetzalcoatls_curse.png"),
					icon("skills/aztec_warrior/huitzilopochtlis_blessing.png"),
					icon("skills/aztec_warrior/obsidian_wound.png"),
					icon("skills/aztec_warrior/pursuit.png"),
					icon("skills/aztec_warrior/macuahuitl_mastery.png")
			},
			{
					icon("skills/egyptian_archer/cat_god.png"),
					icon("skills/egyptian_archer/leaf_arrow.png"),
					icon("skills/egyptian_archer/chariot_volley.png"),
					icon("skills/egyptian_archer/backstep.png")
			}
	};
	private static final Identifier EGYPTIAN_LEAF_ARROW_ICON = icon("skills/egyptian_archer/leaf_arrow.png");
	private static final Identifier EGYPTIAN_CONE_ARROW_ICON = icon("skills/egyptian_archer/cone_arrow.png");
	private static final String[] AZTEC_SKILL_TRANSLATION_KEYS = {
			"gui.echo_warrior.summoner.skill.aztec.quetzalcoatls_curse",
			"gui.echo_warrior.summoner.skill.aztec.huitzilopochtlis_blessing",
			"gui.echo_warrior.summoner.skill.aztec.obsidian_wound",
			"gui.echo_warrior.summoner.skill.aztec.pursuit",
			"gui.echo_warrior.summoner.skill.aztec.macuahuitl"
	};
	private static final int[] AZTEC_SKILL_DESCRIPTION_LINES = {1, 2, 2, 2, 2};
	private static final Identifier[] TALENT_ICONS = {
			icon("traits/bad_temper.png"),
			icon("traits/lazy.png"),
			icon("traits/courage.png"),
			icon("traits/skinny.png"),
			icon("traits/sturdy.png")
	};
	private static final Identifier[] ACTIVITY_ICONS = {
			icon("modes/activity/follow.png"),
			icon("modes/activity/wait.png"),
			icon("modes/activity/wander.png")
	};
	private static final Identifier[] ALERT_ICONS = {
			icon("modes/alert/aggressive.png"),
			icon("modes/alert/defensive.png"),
			icon("modes/alert/peaceful.png")
	};
	private static final Identifier FUEL_HINT = icon("slot_hints/fuel_slot_hint.png");
	private static final Identifier RELIC_HINT = icon("slot_hints/relic_slot_hint.png");
	private static final Identifier MODE_DEFAULT = icon("widgets/mode_default.png");
	private static final Identifier MODE_HOVER = icon("widgets/mode_hover.png");
	private static final Identifier MODE_PRESSED = icon("widgets/mode_pressed.png");
	private static final Identifier SKILL_OCCUPIED = icon("widgets/skill_occupied.png");
	private static final Identifier SKILL_EMPTY = icon("widgets/skill_empty.png");
	private static final Identifier SUMMON_DEFAULT = icon("widgets/summon_default.png");
	private static final Identifier SUMMON_HOVER = icon("widgets/summon_hover.png");
	private static final Identifier SUMMON_PRESSED = icon("widgets/summon_pressed.png");
	private static final Identifier EXPERIENCE_BACKGROUND = icon("bars/experience_background.png");
	private static final Identifier EXPERIENCE_FILL = icon("bars/experience_fill.png");
	private static final Identifier FUEL_FILL = icon("bars/fuel_fill.png");

	private final SummonerLayout layout = SummonerLayout.get();
	private LivingEntity previewEntity;
	private int previewHeroType = -1;
	private Button layoutButton;
	private Button saveButton;
	private Button undoButton;
	private Button resetButton;
	private boolean editingLayout;
	private Element selectedElement;
	private Element draggingElement;
	private EnumMap<Element, Offset> editSnapshot;
	private double dragStartMouseX;
	private double dragStartMouseY;
	private int dragStartOffsetX;
	private int dragStartOffsetY;
	private boolean summonButtonHeld;
	private int dismissConfirmTicks;
	private int feedbackTicks;
	private int lastFeedbackValue;
	private int feedbackCode;
	private final List<FuelTransferParticle> fuelTransferParticles = new java.util.ArrayList<>();

	public SummonerPreviewScreen(SummonerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, IMAGE_WIDTH, IMAGE_HEIGHT);
		this.titleLabelX = -1000;
		this.titleLabelY = -1000;
		this.inventoryLabelX = -1000;
		this.inventoryLabelY = -1000;
	}

	private static Identifier icon(String path) {
		return EchoWarrior.id("textures/gui/summoner/" + path);
	}

	@Override
	protected void init() {
		super.init();
		refreshPreviewEntity();

		this.layoutButton = this.addRenderableWidget(Button.builder(
				Component.literal("布局"),
				button -> beginLayoutEditing()
		).bounds(this.leftPos + IMAGE_WIDTH + 4, this.topPos + 3, 27, 12).build());

		int editorX = Math.min(this.width - 50, this.leftPos + IMAGE_WIDTH + 4);
		this.saveButton = this.addRenderableWidget(Button.builder(
				Component.literal("保存"),
				button -> saveLayoutAndClose()
		).bounds(editorX, this.topPos + 4, 46, 16).build());
		this.undoButton = this.addRenderableWidget(Button.builder(
				Component.literal("撤销"),
				button -> undoLayoutChanges()
		).bounds(editorX, this.topPos + 23, 46, 16).build());
		this.resetButton = this.addRenderableWidget(Button.builder(
				Component.literal("重置"),
				button -> this.layout.reset()
		).bounds(editorX, this.topPos + 42, 46, 16).build());
		updateEditorButtonVisibility();
	}

	private void beginLayoutEditing() {
		this.editSnapshot = this.layout.snapshot();
		this.editingLayout = true;
		this.selectedElement = Element.BASIC_INFO;
		this.draggingElement = null;
		updateEditorButtonVisibility();
	}

	private void saveLayoutAndClose() {
		this.layout.save();
		this.editingLayout = false;
		this.draggingElement = null;
		updateEditorButtonVisibility();
		this.onClose();
	}

	private void undoLayoutChanges() {
		if (this.editSnapshot != null) {
			this.layout.restore(this.editSnapshot);
		}
	}

	private void cancelLayoutEditing() {
		undoLayoutChanges();
		this.editingLayout = false;
		this.selectedElement = null;
		this.draggingElement = null;
		updateEditorButtonVisibility();
	}

	private void updateEditorButtonVisibility() {
		if (this.layoutButton == null) {
			return;
		}
		this.layoutButton.visible = !this.editingLayout;
		this.saveButton.visible = this.editingLayout;
		this.undoButton.visible = this.editingLayout;
		this.resetButton.visible = this.editingLayout;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		refreshPreviewEntity();
		this.extractTransparentBackground(graphics);
		graphics.blit(
				RenderPipelines.GUI_TEXTURED,
				BACKGROUND,
				this.leftPos,
				this.topPos,
				0.0F,
				0.0F,
				IMAGE_WIDTH,
				IMAGE_HEIGHT,
				IMAGE_WIDTH,
				IMAGE_HEIGHT
		);

		boolean relicLoaded = hasRelicLoaded();
		if (relicLoaded && this.previewEntity != null) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(
					graphics,
					x(Element.MODEL, 8),
					y(Element.MODEL, 20),
					x(Element.MODEL, 57),
					y(Element.MODEL, 90),
					25,
					0.0F,
					mouseX,
					mouseY,
					this.previewEntity
			);
		}

		renderAttributeIcons(graphics, relicLoaded);
		renderSkillIcons(graphics, relicLoaded);
		renderTalentIcons(graphics, relicLoaded);
		renderModeButtons(graphics, relicLoaded, mouseX, mouseY);
		renderSummonButton(graphics, relicLoaded, mouseX, mouseY);
		renderProgressFills(graphics, relicLoaded);
		renderFuelTransferParticles(graphics);
		renderEmptySlotHints(graphics);
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractContents(graphics, mouseX, mouseY, partialTick);
		if (this.editingLayout) {
			renderLayoutEditor(graphics);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		boolean relicLoaded = hasRelicLoaded();
		String fullName = relicLoaded
				? EchoHeroType.values()[Math.clamp(this.menu.heroType(), 0, EchoHeroType.values().length - 1)].chineseName()
				: "未载入英灵";
		String visibleName = fitText(fullName, 115);
		graphics.text(this.font, visibleName, rx(Element.TITLE, 8), ry(Element.TITLE, 7), PRIMARY_TEXT_COLOR, true);
		if (isInside(mouseX, mouseY, x(Element.TITLE, 7), y(Element.TITLE, 6), 116, 11)
				&& !visibleName.equals(fullName)) {
			graphics.setTooltipForNextFrame(this.font, Component.literal(fullName), mouseX, mouseY);
		}
		if (relicLoaded) {
			graphics.text(
					this.font,
					decimal(this.menu.spiritHealth()) + "/" + decimal(this.menu.spiritMaximumHealth()),
					rx(Element.BASIC_INFO, 72),
					ry(Element.BASIC_INFO, 21),
					PRIMARY_TEXT_COLOR,
					true
			);
			graphics.text(this.font, Integer.toString(this.menu.relicLevel()), rx(Element.BASIC_INFO, 145), ry(Element.BASIC_INFO, 21), PRIMARY_TEXT_COLOR, true);
			graphics.text(this.font, decimal(this.menu.spiritAttackDamage()), rx(Element.BASIC_INFO, 72), ry(Element.BASIC_INFO, 34), PRIMARY_TEXT_COLOR, true);
			graphics.text(this.font, this.menu.spiritAttackSpeed() + "%", rx(Element.BASIC_INFO, 127), ry(Element.BASIC_INFO, 34), PRIMARY_TEXT_COLOR, true);
			graphics.text(this.font, decimal(this.menu.spiritArmor()), rx(Element.BASIC_INFO, 72), ry(Element.BASIC_INFO, 47), PRIMARY_TEXT_COLOR, true);
			graphics.text(this.font, this.menu.spiritMovement() + "%", rx(Element.BASIC_INFO, 127), ry(Element.BASIC_INFO, 47), PRIMARY_TEXT_COLOR, true);
			graphics.text(this.font, this.menu.heroType() == EchoHeroType.EGYPTIAN_ARCHER.ordinal() ? "24" : "16",
					rx(Element.BASIC_INFO, 72), ry(Element.BASIC_INFO, 60), PRIMARY_TEXT_COLOR, true);
			graphics.text(this.font, this.menu.summonCostPercent() + "%", rx(Element.BASIC_INFO, 127), ry(Element.BASIC_INFO, 60), PRIMARY_TEXT_COLOR, true);
		}

		graphics.text(this.font, "行动模式", rx(Element.ACTIVITY, 179), ry(Element.ACTIVITY, 78), PRIMARY_TEXT_COLOR, true);
		graphics.text(this.font, "警戒状态", rx(Element.ALERT, 179), ry(Element.ALERT, 111), PRIMARY_TEXT_COLOR, true);
		graphics.centeredText(
				this.font,
				summonButtonText(relicLoaded),
				rx(Element.SUMMON_BUTTON, 206),
				ry(Element.SUMMON_BUTTON, 148),
				HEADER_TEXT_COLOR
		);
		renderAttributeTooltips(graphics, mouseX, mouseY, relicLoaded);
		renderInteractiveTooltips(graphics, mouseX, mouseY, relicLoaded);
		renderFeedbackToast(graphics);
	}

	private void renderFeedbackToast(GuiGraphicsExtractor graphics) {
		String message = feedbackText();
		if (message == null) return;
		int left = 178;
		int top = 4;
		int width = 63;
		boolean error = this.feedbackCode == SummonerMenu.ACTION_NO_RELIC
				|| this.feedbackCode == SummonerMenu.ACTION_INVALID_SUMMONER
				|| this.feedbackCode == SummonerMenu.ACTION_CREATE_FAILED
				|| this.feedbackCode == SummonerMenu.ACTION_NOT_ENOUGH_FUEL
				|| this.feedbackCode == SummonerMenu.ACTION_NO_SAFE_POSITION;
		List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(Component.literal(message), width - 12);
		int visibleLines = Math.min(lines.size(), 6);
		int height = 8 + visibleLines * 10;
		graphics.fill(left, top, left + width, top + height, error ? 0xDD3B2025 : 0xDD26382D);
		graphics.fill(left, top, left + 2, top + height, error ? 0xFFE47777 : 0xFF7DCE91);
		for (int index = 0; index < visibleLines; index++) {
			graphics.text(this.font, lines.get(index), left + 7, top + 5 + index * 10, 0xFFF4F0E8, false);
		}
	}

	@Override
	protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (!this.editingLayout) {
			super.extractSlots(graphics, mouseX, mouseY);
			return;
		}

		for (int index = 0; index < this.menu.slots.size(); index++) {
			Slot slot = this.menu.slots.get(index);
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			int[] position = editorSlotPosition(index);
			graphics.item(stack, position[0], position[1]);
			graphics.itemDecorations(this.font, stack, position[0], position[1]);
		}
	}

	private int[] editorSlotPosition(int menuSlot) {
		if (menuSlot < SummonerMenu.MODULE_SLOT_COUNT) {
			return new int[] {
					rx(Element.MODULES, MODULE_SLOT_X[menuSlot]),
					ry(Element.MODULES, 94)
			};
		}
		if (menuSlot == SummonerMenu.FUEL_SLOT) {
			return new int[] {rx(Element.FUEL_SLOT, 179), ry(Element.FUEL_SLOT, 172)};
		}
		if (menuSlot == SummonerMenu.RELIC_SLOT) {
			return new int[] {rx(Element.RELIC_SLOT, 217), ry(Element.RELIC_SLOT, 172)};
		}

		int playerSlot = menuSlot - SummonerMenu.CUSTOM_SLOT_COUNT;
		if (playerSlot < 27) {
			int row = playerSlot / 9;
			int column = playerSlot % 9;
			return new int[] {
					rx(Element.PLAYER_INVENTORY, 8 + column * 18),
					ry(Element.PLAYER_INVENTORY, 120 + row * 18)
			};
		}
		int column = playerSlot - 27;
		return new int[] {
				rx(Element.PLAYER_INVENTORY, 8 + column * 18),
				ry(Element.PLAYER_INVENTORY, 177)
		};
	}

	private boolean hasRelicLoaded() {
		ItemStack relic = this.menu.summonerContainer().getItem(SummonerMenu.RELIC_SLOT);
		return relic.getItem() instanceof EchoRelicItem
				&& this.menu.relicSyncToken() != 0
				&& this.menu.relicSyncToken() == SummonerMenu.relicSyncToken(relic);
	}

	private void refreshPreviewEntity() {
		if (this.minecraft == null || this.minecraft.level == null || !hasRelicLoaded()) return;
		int hero = this.menu.heroType();
		if (this.previewEntity != null && this.previewHeroType == hero) return;
		this.previewHeroType = hero;
		this.previewEntity = switch (EchoHeroType.values()[Math.clamp(hero, 0, EchoHeroType.values().length - 1)]) {
			case ROMAN_LEGIONARY -> ModEntities.ROMAN_LEGIONARY_ECHO.create(this.minecraft.level, EntitySpawnReason.LOAD);
			case AZTEC_WARRIOR -> ModEntities.AZTEC_WARRIOR_ECHO.create(this.minecraft.level, EntitySpawnReason.LOAD);
			case EGYPTIAN_ARCHER -> ModEntities.EGYPTIAN_ARCHER_ECHO.create(this.minecraft.level, EntitySpawnReason.LOAD);
		};
	}

	private void renderAttributeIcons(GuiGraphicsExtractor graphics, boolean active) {
		if (!active) {
			return;
		}
		for (int index = 0; index < ATTRIBUTE_ICONS.length; index++) {
			blitSized(
					graphics,
					ATTRIBUTE_ICONS[index],
					x(Element.BASIC_INFO, ATTRIBUTE_ICON_X[index]),
					y(Element.BASIC_INFO, ATTRIBUTE_ICON_Y[index]),
					11,
					11
			);
		}
	}

	private void renderSkillIcons(GuiGraphicsExtractor graphics, boolean active) {
		Identifier[] icons = SKILL_ICONS[Math.clamp(this.menu.heroType(), 0, SKILL_ICONS.length - 1)].clone();
		if (this.menu.heroType() == EchoHeroType.EGYPTIAN_ARCHER.ordinal() && icons.length > 1) {
			icons[1] = this.menu.egyptianArrowMode() == 2 ? EGYPTIAN_CONE_ARROW_ICON : EGYPTIAN_LEAF_ARROW_ICON;
		}
		int count = active ? Math.min(this.menu.skillCount(), icons.length) : 0;
		for (int index = 0; index < 5; index++) {
			Identifier frame = active && index < count ? SKILL_OCCUPIED : SKILL_EMPTY;
			blitSized(graphics, frame, x(Element.SKILLS, 61 + index * 22), y(Element.SKILLS, 71), 20, 20);
		}
		if (!active) {
			return;
		}
		for (int index = 0; index < count; index++) {
			int iconX = x(Element.SKILLS, 63 + index * 22);
			int iconY = y(Element.SKILLS, 73);
			blit16(graphics, icons[index], iconX, iconY);
			boolean activeChargeSkill = switch (EchoHeroType.values()[Math.clamp(this.menu.heroType(), 0, EchoHeroType.values().length - 1)]) {
				case ROMAN_LEGIONARY -> index == 1;
				case AZTEC_WARRIOR, EGYPTIAN_ARCHER -> index == 3;
			};
			int maximumCharges = this.menu.heroType() == EchoHeroType.ROMAN_LEGIONARY.ordinal() ? 3 : 2;
			if (activeChargeSkill && this.menu.shieldCharges() < maximumCharges) {
				renderRadialCooldown(graphics, iconX, iconY, this.menu.shieldChargeProgress() / 1000.0F);
			}
			if (index == 2 && this.menu.legionCooldownTicks() > 0) {
				renderRadialCooldown(graphics, iconX, iconY, 1.0F - this.menu.legionCooldownTicks() / 400.0F);
			}
			if ((this.menu.enabledSkills() & 1 << index) == 0) {
				graphics.fill(iconX, iconY, iconX + 16, iconY + 16, 0x99000000);
				for (int pixel = 1; pixel < 15; pixel++) {
					graphics.fill(iconX + pixel, iconY + pixel, iconX + pixel + 1, iconY + pixel + 1, 0xFFE05050);
				}
			}
			if (index == 2 && this.menu.legionActive()) {
				drawBorder(graphics, iconX - 1, iconY - 1, iconX + 17, iconY + 17, 0xFFD8B55A);
			}
			if (activeChargeSkill) {
				graphics.text(this.font, Integer.toString(this.menu.shieldCharges()), iconX + 10, iconY + 8, 0xFFFFFFFF, true);
			}
		}
	}

	private static void renderRadialCooldown(GuiGraphicsExtractor graphics, int left, int top, float progress) {
		float reveal = Math.clamp(progress, 0.0F, 1.0F) * (float)(Math.PI * 2.0);
		for (int py = 0; py < 16; py++) {
			for (int px = 0; px < 16; px++) {
				double angle = Math.atan2(px - 7.5, 7.5 - py);
				if (angle < 0.0) angle += Math.PI * 2.0;
				if (angle > reveal) graphics.fill(left + px, top + py, left + px + 1, top + py + 1, 0xA8000000);
			}
		}
	}

	private void renderTalentIcons(GuiGraphicsExtractor graphics, boolean active) {
		if (!active) {
			return;
		}
		int count = Integer.bitCount(this.menu.traitMask());
		int displayed = 0;
		for (int talentIndex = 0; talentIndex < TALENT_ICONS.length; talentIndex++) {
			if ((this.menu.traitMask() & 1 << talentIndex) == 0) continue;
			int slotX = 157 - (count - 1 - displayed) * 11;
			blitSized(
					graphics,
					TALENT_ICONS[talentIndex],
					x(Element.TALENTS, slotX),
					y(Element.TALENTS, 6),
					11,
					11
			);
			displayed++;
		}
	}

	private void renderModeButtons(GuiGraphicsExtractor graphics, boolean active, int mouseX, int mouseY) {
		for (int index = 0; index < 3; index++) {
			int activityX = x(Element.ACTIVITY, 178 + index * 19);
			int activityY = y(Element.ACTIVITY, 90);
			int alertX = x(Element.ALERT, 178 + index * 19);
			int alertY = y(Element.ALERT, 123);
			Identifier activityFrame = modeFrame(active, index == this.menu.activityMode(), mouseX, mouseY, activityX, activityY);
			Identifier alertFrame = modeFrame(active, index == this.menu.alertMode(), mouseX, mouseY, alertX, alertY);
			blitSized(graphics, activityFrame, activityX, activityY, 18, 18);
			blitSized(graphics, alertFrame, alertX, alertY, 18, 18);
			if (active) {
				blit16(graphics, ACTIVITY_ICONS[index], activityX + 1, activityY + 1);
				blit16(graphics, ALERT_ICONS[index], alertX + 1, alertY + 1);
			}
		}
	}

	private Identifier modeFrame(boolean active, boolean selected, int mouseX, int mouseY, int x, int y) {
		if (active && selected) {
			return MODE_PRESSED;
		}
		if (active && isInside(mouseX, mouseY, x, y, 18, 18)) {
			return MODE_HOVER;
		}
		return MODE_DEFAULT;
	}

	private void renderSummonButton(GuiGraphicsExtractor graphics, boolean active, int mouseX, int mouseY) {
		int buttonX = x(Element.SUMMON_BUTTON, 178);
		int buttonY = y(Element.SUMMON_BUTTON, 143);
		Identifier texture = SUMMON_DEFAULT;
		if (active && (this.summonButtonHeld || this.dismissConfirmTicks > 0 && this.menu.isSpiritPresent())) {
			texture = SUMMON_PRESSED;
		} else if (active && isInside(mouseX, mouseY, buttonX, buttonY, 56, 19)) {
			texture = SUMMON_HOVER;
		}
		blitSized(graphics, texture, buttonX, buttonY, 56, 19);
	}

	private void renderProgressFills(GuiGraphicsExtractor graphics, boolean active) {
		blitSized(
				graphics,
				EXPERIENCE_BACKGROUND,
				x(Element.EXPERIENCE, 8),
				y(Element.EXPERIENCE, 114),
				160,
				2
		);
		if (active) {
			int needed = this.menu.relicExperienceNeeded();
			int experienceWidth = this.menu.relicLevel() >= 30
					? 160
					: needed <= 0 ? 0 : Math.clamp(Math.round(this.menu.relicExperience() * 160.0F / needed), 0, 160);
			if (experienceWidth > 0) {
				blitRegion(graphics, EXPERIENCE_FILL, x(Element.EXPERIENCE, 8), y(Element.EXPERIENCE, 114), experienceWidth, 2, 160, 2);
			}
		}
		int fuelX = x(Element.FUEL_BAR, 179);
		int fuelY = y(Element.FUEL_BAR, 165);
		int fuelWidth = Math.clamp(Math.round(this.menu.fuelAmount() * 54.0F / SummonerFuel.CAPACITY), 0, 54);
		if (fuelWidth > 0) {
			blitRegion(graphics, FUEL_FILL, fuelX, fuelY, fuelWidth, 3, 54, 3);
			renderFuelParticles(graphics, fuelX, fuelY, fuelWidth);
		}
	}

	private void renderFuelParticles(GuiGraphicsExtractor graphics, int left, int top, int fillWidth) {
		long time = this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime();
		int[] seeds = {1, 5, 9, 14, 19, 23, 29, 34, 41, 48};
		for (int index = 0; index < seeds.length; index++) {
			int particleX = Math.floorMod(seeds[index] + (int)(time / (3 + index % 3)), 54);
			if (particleX >= fillWidth) {
				continue;
			}
			int particleY = index % 3;
			int color = index % 2 == 0 ? 0xFF91FFFF : 0xFF57CBCD;
			graphics.fill(left + particleX, top + particleY, left + particleX + 1, top + particleY + 1, color);
		}
	}

	private void renderFuelTransferParticles(GuiGraphicsExtractor graphics) {
		for (FuelTransferParticle particle : this.fuelTransferParticles) {
			float progress = particle.age / (float)particle.lifetime;
			double eased = progress * progress * (3.0 - 2.0 * progress);
			double x = particle.startX + (particle.endX - particle.startX) * eased;
			double y = particle.startY + (particle.endY - particle.startY) * eased - Math.sin(progress * Math.PI) * particle.arc;
			graphics.fill((int)Math.round(x), (int)Math.round(y), (int)Math.round(x) + 1, (int)Math.round(y) + 1, particle.color);
		}
	}

	private void spawnFuelTransferParticles(boolean soulSand) {
		int count = 3 + this.minecraft.level.getRandom().nextInt(4);
		for (int index = 0; index < count && this.fuelTransferParticles.size() < 24; index++) {
			int startX = x(Element.FUEL_SLOT, 187) + this.minecraft.level.getRandom().nextInt(7) - 3;
			int startY = y(Element.FUEL_SLOT, 180) + this.minecraft.level.getRandom().nextInt(7) - 3;
			int endX = x(Element.FUEL_BAR, 179) + Math.max(1, Math.round(this.menu.fuelAmount() * 53.0F / SummonerFuel.CAPACITY));
			int endY = y(Element.FUEL_BAR, 166);
			int lifetime = 8 + this.minecraft.level.getRandom().nextInt(5);
			int color = soulSand ? (index % 2 == 0 ? 0xFF77D6CE : 0xFFB7F4E9) : (index % 2 == 0 ? 0xFF8B4A42 : 0xFFB66A55);
			this.fuelTransferParticles.add(new FuelTransferParticle(startX, startY, endX, endY, 3 + this.minecraft.level.getRandom().nextInt(5), lifetime, color));
		}
	}

	private void renderEmptySlotHints(GuiGraphicsExtractor graphics) {
		if (this.menu.summonerContainer().getItem(SummonerMenu.FUEL_SLOT).isEmpty()) {
			blit16(graphics, FUEL_HINT, x(Element.FUEL_SLOT, 179), y(Element.FUEL_SLOT, 172));
		}
		if (this.menu.summonerContainer().getItem(SummonerMenu.RELIC_SLOT).isEmpty()) {
			blit16(graphics, RELIC_HINT, x(Element.RELIC_SLOT, 217), y(Element.RELIC_SLOT, 172));
		}
	}

	private void renderAttributeTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean active) {
		if (!active) {
			return;
		}
		int[][] boxes = {
				{61, 19, 71, 11}, {134, 19, 35, 11},
				{61, 32, 53, 11}, {116, 32, 53, 11},
				{61, 45, 53, 11}, {116, 45, 53, 11},
				{61, 58, 53, 11}, {116, 58, 53, 11}
		};
		String[] descriptions = {
				"生命值：" + decimal(this.menu.spiritHealth()) + "/" + decimal(this.menu.spiritMaximumHealth())
						+ (this.menu.isSpiritPresent() ? "" : "（未召唤）"),
				"等级：" + this.menu.relicLevel() + "/30",
				"攻击力：" + decimal(this.menu.spiritAttackDamage()),
				"攻击速度：" + this.menu.spiritAttackSpeed() + "%",
				"护甲：" + decimal(this.menu.spiritArmor()),
				"移动速度：" + this.menu.spiritMovement() + "%",
				"警戒范围：16格",
				"召唤与生命恢复燃料消耗：" + this.menu.summonCostPercent() + "%"
		};
		for (int index = 0; index < boxes.length; index++) {
			int[] box = boxes[index];
			if (isInside(mouseX, mouseY, x(Element.BASIC_INFO, box[0]), y(Element.BASIC_INFO, box[1]), box[2], box[3])) {
				graphics.setTooltipForNextFrame(this.font, Component.literal(descriptions[index]), mouseX, mouseY);
				return;
			}
		}
	}

	private void renderInteractiveTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean active) {
		if (active) {
			String[] talentNames = {"坏脾气", "慵懒", "勇气", "瘦削", "壮硕"};
			String[] talentEffects = {
					"召唤与自然恢复燃料消耗+20%，攻击力+4",
					"召唤与自然恢复燃料消耗-20%，移动速度-25%",
					"攻击力+2",
					"生命值-25%，移动速度和攻击速度+25%",
					"护甲+4，移动速度-25%"
			};
			int traitCount = Integer.bitCount(this.menu.traitMask());
			int displayed = 0;
			for (int talentIndex = 0; talentIndex < TALENT_ICONS.length; talentIndex++) {
				if ((this.menu.traitMask() & 1 << talentIndex) == 0) continue;
				int slotX = 157 - (traitCount - 1 - displayed) * 11;
				if (isInside(mouseX, mouseY, x(Element.TALENTS, slotX), y(Element.TALENTS, 6), 11, 11)) {
					showTooltip(graphics, mouseX, mouseY, talentNames[talentIndex], talentEffects[talentIndex]);
					return;
				}
				displayed++;
			}

			boolean aztec = this.menu.heroType() == EchoHeroType.AZTEC_WARRIOR.ordinal();
			boolean egyptian = this.menu.heroType() == EchoHeroType.EGYPTIAN_ARCHER.ordinal();
			if (aztec) {
				for (int index = 0; index < Math.min(this.menu.skillCount(), AZTEC_SKILL_TRANSLATION_KEYS.length); index++) {
					if (isInside(mouseX, mouseY, x(Element.SKILLS, 61 + index * 22), y(Element.SKILLS, 71), 20, 20)) {
						showAztecSkillTooltip(graphics, mouseX, mouseY, index, (this.menu.enabledSkills() & 1 << index) != 0);
						return;
					}
				}
			} else if (egyptian) {
				String[] skillNames = {"赞美猫神", this.menu.egyptianArrowMode() == 2 ? "锥锋箭" : "叶形箭", "战车与齐射之魂", "后撤步"};
				String[][] skillDescriptions = {
						{"30格球形范围内的苦力怕无法完成爆炸。", "尝试点燃或受到直接伤害时会慌乱逃窜。"},
						{"点击循环：关闭 → 叶形箭 → 锥锋箭。", "叶形箭造成流血与减速；锥锋箭忽略35%护甲并可能贯穿。"},
						{"允许移动射击并保持8—14格距离。", "敌人越多，越可能向另一个目标追加一箭。"},
						{"敌人靠近4格时向最安全方向跳跃5格。", "拥有2点充能，每6秒恢复1点，并齐射至多6个目标。"}
				};
				for (int index = 0; index < Math.min(this.menu.skillCount(), skillNames.length); index++) {
					if (isInside(mouseX, mouseY, x(Element.SKILLS, 61 + index * 22), y(Element.SKILLS, 71), 20, 20)) {
						String state = index == 1
								? "当前：" + switch (this.menu.egyptianArrowMode()) { case 1 -> "叶形箭"; case 2 -> "锥锋箭"; default -> "关闭"; } + "（点击切换）"
								: ((this.menu.enabledSkills() & 1 << index) != 0 ? "当前：已启用（点击禁用）" : "当前：已禁用（点击启用）");
						showTooltip(graphics, mouseX, mouseY, skillNames[index], skillDescriptions[index][0], skillDescriptions[index][1], state);
						return;
					}
				}
			} else {
				String[] skillNames = {"士兵阵列！", "举盾冲锋！", "军团永存！"};
				String[][] skillDescriptions = {
							{"开启后常驻，为自身和友军提供力量。", "离开光环立即失效；附近有持盾玩家时额外减伤。"},
							{"冲向威胁主人的投射物或即将爆炸的苦力怕。", "弹开投射物；每5秒恢复1次充能，最多3次。"},
							{"举盾防御并嘲讽周围敌人。", "5秒后返还减免前所受伤害并击退敌人。"}
				};
				for (int index = 0; index < Math.min(this.menu.skillCount(), skillNames.length); index++) {
					if (isInside(mouseX, mouseY, x(Element.SKILLS, 61 + index * 22), y(Element.SKILLS, 71), 20, 20)) {
						showTooltip(
								graphics,
								mouseX,
								mouseY,
								skillNames[index],
								skillDescriptions[index][0],
								skillDescriptions[index][1],
								(this.menu.enabledSkills() & 1 << index) != 0 ? "当前：已启用（点击禁用）" : "当前：已禁用（点击启用）"
						);
						return;
					}
				}
			}
		}

		String[] activityNames = {"跟随", "等待", "闲逛"};
		String[] activityDescriptions = {
				"跟随主人行动；距离过远时传送到主人附近。",
				"停留在当前位置附近，不跟随也不会远距离传送。",
				"以当前位置为中心自由活动，不跟随主人。"
		};
		String[] alertNames = {"主动出击", "被动防御", "和平模式"};
		String[] alertDescriptions = {
				"主动攻击范围内的敌对生物，并响应主人攻击的目标。",
				"只反击伤害主人、自身或被主人攻击的生物。",
				"不主动攻击；自身受到直接攻击时仍可自卫。"
		};
		for (int index = 0; index < 3; index++) {
			if (isInside(mouseX, mouseY, x(Element.ACTIVITY, 178 + index * 19), y(Element.ACTIVITY, 90), 18, 18)) {
				showTooltip(graphics, mouseX, mouseY, activityNames[index], activityDescriptions[index],
						active ? (index == this.menu.activityMode() ? "当前已选择" : "点击切换") : "需要先装入英灵遗物");
				return;
			}
			if (isInside(mouseX, mouseY, x(Element.ALERT, 178 + index * 19), y(Element.ALERT, 123), 18, 18)) {
				showTooltip(graphics, mouseX, mouseY, alertNames[index], alertDescriptions[index],
						active ? (index == this.menu.alertMode() ? "当前已选择" : "点击切换") : "需要先装入英灵遗物");
				return;
			}
		}

		if (isInside(mouseX, mouseY, x(Element.EXPERIENCE, 7), y(Element.EXPERIENCE, 113), 162, 4)) {
			if (!active) {
				showTooltip(graphics, mouseX, mouseY, "英灵经验", "装入遗物后显示经验进度");
			} else if (this.menu.relicLevel() >= 30) {
				showTooltip(graphics, mouseX, mouseY, "英灵经验", "等级30 · 已达到最高等级");
			} else {
				showTooltip(
						graphics,
						mouseX,
						mouseY,
						"英灵经验",
						"等级" + this.menu.relicLevel() + " · 经验"
								+ this.menu.relicExperience() + "/" + this.menu.relicExperienceNeeded()
				);
			}
			return;
		}

		if (isInside(mouseX, mouseY, x(Element.SUMMON_BUTTON, 178), y(Element.SUMMON_BUTTON, 143), 56, 19)) {
			showTooltip(graphics, mouseX, mouseY, summonButtonTooltip(active));
			return;
		}
		if (isInside(mouseX, mouseY, x(Element.FUEL_BAR, 179), y(Element.FUEL_BAR, 165), 54, 3)) {
			int summonCost = (int)Math.ceil(SummonerFuel.BASE_SUMMON_COST * this.menu.summonCostPercent() / 100.0);
			double healCost = SummonerFuel.BASE_HEAL_COST * this.menu.summonCostPercent() / 100.0;
			showTooltip(graphics, mouseX, mouseY, "英灵燃料",
					this.menu.fuelAmount() + "/" + SummonerFuel.CAPACITY,
					"召唤消耗：" + summonCost,
					"自然恢复每点生命：" + String.format(java.util.Locale.ROOT, "%.1f", healCost),
					"当前可召唤：" + this.menu.fuelAmount() / summonCost + "次");
			return;
		}
		if (this.menu.summonerContainer().getItem(SummonerMenu.FUEL_SLOT).isEmpty()
				&& isInside(mouseX, mouseY, x(Element.FUEL_SLOT, 179), y(Element.FUEL_SLOT, 172), 16, 16)) {
			showTooltip(graphics, mouseX, mouseY, "燃料输入槽", "腐肉：+20燃料", "灵魂沙/灵魂土：+50燃料", "容量不足时不会消耗物品");
			return;
		}
		for (int index = 0; index < SummonerMenu.MODULE_SLOT_COUNT; index++) {
			if (isInside(mouseX, mouseY, x(Element.MODULES, MODULE_SLOT_X[index]), y(Element.MODULES, 94), 16, 16)) {
				showTooltip(graphics, mouseX, mouseY, "升级模块槽", "模块系统尚未实装，当前不接受新物品。");
				return;
			}
		}
	}

	private static String decimal(int tenths) {
		return tenths % 10 == 0
				? Integer.toString(tenths / 10)
				: String.format(java.util.Locale.ROOT, "%.1f", tenths / 10.0);
	}

	private void showTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, String title, String... descriptions) {
		List<Component> lines = new java.util.ArrayList<>();
		lines.add(Component.literal(title).withStyle(ChatFormatting.GOLD));
		for (String description : descriptions) {
			lines.add(Component.literal(description).withStyle(ChatFormatting.GRAY));
		}
		graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
	}

	private void showAztecSkillTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int skill, boolean enabled) {
		String key = AZTEC_SKILL_TRANSLATION_KEYS[skill];
		List<Component> lines = new java.util.ArrayList<>();
		lines.add(Component.translatable(key + ".name").withStyle(ChatFormatting.GOLD));
		for (int line = 1; line <= AZTEC_SKILL_DESCRIPTION_LINES[skill]; line++) {
			lines.add(Component.translatable(key + ".description." + line).withStyle(ChatFormatting.GRAY));
		}
		lines.add(Component.translatable(enabled
				? "gui.echo_warrior.summoner.skill.enabled"
				: "gui.echo_warrior.summoner.skill.disabled").withStyle(ChatFormatting.GRAY));
		graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
	}

	private void showTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, String[] tooltip) {
		if (tooltip.length == 0) {
			return;
		}
		List<Component> lines = new java.util.ArrayList<>();
		lines.add(Component.literal(tooltip[0]).withStyle(ChatFormatting.GOLD));
		for (int index = 1; index < tooltip.length; index++) {
			lines.add(Component.literal(tooltip[index]).withStyle(ChatFormatting.GRAY));
		}
		graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
	}

	private String summonButtonText(boolean relicLoaded) {
		if (!relicLoaded) {
			return "需要遗物";
		}
		if (this.menu.isSpiritPresent()) {
			return this.dismissConfirmTicks > 0 ? "再次确认" : "收回英灵";
		}
		return "召唤英灵";
	}

	private String[] summonButtonTooltip(boolean relicLoaded) {
		String feedback = feedbackText();
		if (!relicLoaded) {
			return feedback == null
					? new String[] {"召唤英灵", "需要先在右下角装入英灵遗物。"}
					: new String[] {"召唤英灵", "需要先在右下角装入英灵遗物。", feedback};
		}
		if (this.menu.isSpiritPresent()) {
			String instruction = this.dismissConfirmTicks > 0
					? "再次点击会将当前英灵收回并遣散。"
					: "点击一次进入确认状态，3秒内再次点击执行收回。";
			return feedback == null
					? new String[] {"收回英灵", instruction}
					: new String[] {"收回英灵", instruction, feedback};
		}
		return feedback == null
				? new String[] {"召唤英灵", "将英灵召唤到玩家前方，界面保持开启。"}
				: new String[] {"召唤英灵", "将英灵召唤到玩家前方，界面保持开启。", feedback};
	}

	private String feedbackText() {
		if (this.feedbackTicks <= 0) {
			return null;
		}
		return switch (this.feedbackCode) {
			case SummonerMenu.ACTION_SUMMONED -> "召唤成功。";
			case SummonerMenu.ACTION_DISMISSED -> "英灵已收回。";
			case SummonerMenu.ACTION_NO_RELIC -> "召唤失败：没有装入英灵遗物。";
			case SummonerMenu.ACTION_INVALID_SUMMONER -> "操作失败：召唤器实例已经失效。";
			case SummonerMenu.ACTION_CREATE_FAILED -> "召唤失败：无法在当前位置生成英灵。";
			case SummonerMenu.ACTION_NOT_ENOUGH_FUEL -> "召唤失败：燃料不足。";
			case SummonerMenu.ACTION_NO_SAFE_POSITION -> "召唤失败：附近没有安全位置。";
			case SummonerMenu.ACTION_MODE_CHANGED -> "英灵模式已更新。";
			case SummonerMenu.ACTION_SKILL_CHANGED -> "技能启用状态已更新。";
			default -> null;
		};
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (this.dismissConfirmTicks > 0) {
			this.dismissConfirmTicks--;
		}
		if (this.feedbackTicks > 0) {
			this.feedbackTicks--;
		}
		this.fuelTransferParticles.removeIf(particle -> ++particle.age > particle.lifetime);
		if (!this.menu.isSpiritPresent()) {
			this.dismissConfirmTicks = 0;
		}

		int feedbackValue = this.menu.actionFeedbackValue();
		if (feedbackValue != 0 && feedbackValue != this.lastFeedbackValue) {
			this.lastFeedbackValue = feedbackValue;
			this.feedbackCode = feedbackValue & 0xFF;
			if (this.feedbackCode == SummonerMenu.ACTION_FUEL_ROTTEN_FLESH || this.feedbackCode == SummonerMenu.ACTION_FUEL_SOUL_SAND) {
				if (this.minecraft.level != null) spawnFuelTransferParticles(this.feedbackCode == SummonerMenu.ACTION_FUEL_SOUL_SAND);
				this.feedbackTicks = 0;
			} else {
				this.feedbackTicks = 60;
			}
		}
	}

	private static final class FuelTransferParticle {
		private final int startX;
		private final int startY;
		private final int endX;
		private final int endY;
		private final int arc;
		private final int lifetime;
		private final int color;
		private int age;

		private FuelTransferParticle(int startX, int startY, int endX, int endY, int arc, int lifetime, int color) {
			this.startX = startX;
			this.startY = startY;
			this.endX = endX;
			this.endY = endY;
			this.arc = arc;
			this.lifetime = lifetime;
			this.color = color;
		}
	}

	private String fitText(String value, int maxWidth) {
		if (this.font.width(value) <= maxWidth) {
			return value;
		}
		String suffix = "…";
		int suffixWidth = this.font.width(suffix);
		StringBuilder result = new StringBuilder();
		for (int offset = 0; offset < value.length();) {
			int codePoint = value.codePointAt(offset);
			String candidate = result.toString() + new String(Character.toChars(codePoint));
			if (this.font.width(candidate) + suffixWidth > maxWidth) {
				break;
			}
			result.appendCodePoint(codePoint);
			offset += Character.charCount(codePoint);
		}
		return result + suffix;
	}

	private void renderLayoutEditor(GuiGraphicsExtractor graphics) {
		for (Element element : Element.values()) {
			int left = x(element, element.baseX);
			int top = y(element, element.baseY);
			int right = left + element.width;
			int bottom = top + element.height;
			int color = element == this.selectedElement ? 0xFFFFFF40 : 0xAA49D7FF;
			drawBorder(graphics, left, top, right, bottom, color);
		}

		int panelX = Math.min(this.width - 122, this.leftPos + IMAGE_WIDTH + 4);
		int panelY = this.topPos + 62;
		graphics.fill(panelX - 2, panelY - 2, panelX + 120, panelY + 39, 0xCC111111);
		graphics.text(this.font, "拖动区域调整位置", panelX, panelY, 0xFFFFFFFF, false);
		if (this.selectedElement != null) {
			graphics.text(this.font, "选中：" + this.selectedElement.serializedName, panelX, panelY + 11, 0xFFFFFF55, false);
			graphics.text(
					this.font,
					"x=" + this.layout.x(this.selectedElement, this.selectedElement.baseX)
							+ "  y=" + this.layout.y(this.selectedElement, this.selectedElement.baseY),
					panelX,
					panelY + 22,
					0xFFFFFFFF,
					false
			);
		}
	}

	private static void drawBorder(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
		graphics.fill(left, top, right, top + 1, color);
		graphics.fill(left, bottom - 1, right, bottom, color);
		graphics.fill(left, top, left + 1, bottom, color);
		graphics.fill(right - 1, top, right, bottom, color);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (!this.editingLayout) {
			if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				if (hasRelicLoaded()) {
					for (int index = 0; index < 3; index++) {
						if (isInside(event.x(), event.y(), x(Element.ACTIVITY, 178 + index * 19), y(Element.ACTIVITY, 90), 18, 18)) {
							sendMenuButton(SummonerMenu.BUTTON_ACTIVITY_START + index);
							return true;
						}
						if (isInside(event.x(), event.y(), x(Element.ALERT, 178 + index * 19), y(Element.ALERT, 123), 18, 18)) {
							sendMenuButton(SummonerMenu.BUTTON_ALERT_START + index);
							return true;
						}
					}
					for (int index = 0; index < this.menu.skillCount(); index++) {
						if (isInside(event.x(), event.y(), x(Element.SKILLS, 61 + index * 22), y(Element.SKILLS, 71), 20, 20)) {
							sendMenuButton(SummonerMenu.BUTTON_SKILL_START + index);
							return true;
						}
					}
				}
				if (isInside(event.x(), event.y(), x(Element.SUMMON_BUTTON, 178), y(Element.SUMMON_BUTTON, 143), 56, 19)) {
					this.summonButtonHeld = true;
					if (this.menu.isSpiritPresent() && hasRelicLoaded()) {
						if (this.dismissConfirmTicks <= 0) {
							this.dismissConfirmTicks = 60;
							return true;
						}
						this.dismissConfirmTicks = 0;
					}
					sendSummonAction();
					return true;
				}
			}
			return super.mouseClicked(event, doubleClick);
		}

		if (isOverEditorButton(event.x(), event.y())) {
			return super.mouseClicked(event, doubleClick);
		}
		if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return true;
		}

		Element hit = findElement(event.x(), event.y());
		this.selectedElement = hit;
		this.draggingElement = hit;
		if (hit != null) {
			this.dragStartMouseX = event.x();
			this.dragStartMouseY = event.y();
			this.dragStartOffsetX = this.layout.offsetX(hit);
			this.dragStartOffsetY = this.layout.offsetY(hit);
		}
		return true;
	}

	private void sendSummonAction() {
		sendMenuButton(SummonerMenu.BUTTON_SUMMON_OR_DISMISS);
	}

	private void sendMenuButton(int button) {
		if (this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(
					this.menu.containerId,
					button
			);
		}
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (!this.editingLayout || this.draggingElement == null) {
			return this.editingLayout || super.mouseDragged(event, dragX, dragY);
		}
		int newX = this.dragStartOffsetX + (int)Math.round(event.x() - this.dragStartMouseX);
		int newY = this.dragStartOffsetY + (int)Math.round(event.y() - this.dragStartMouseY);
		this.layout.setOffset(this.draggingElement, newX, newY);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		this.summonButtonHeld = false;
		if (this.editingLayout) {
			this.draggingElement = null;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (!this.editingLayout) {
			return super.keyPressed(event);
		}
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			cancelLayoutEditing();
			return true;
		}
		if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
			saveLayoutAndClose();
			return true;
		}
		if (this.selectedElement == null) {
			return true;
		}

		int step = event.hasShiftDown() ? 5 : 1;
		switch (event.key()) {
			case GLFW.GLFW_KEY_LEFT -> this.layout.move(this.selectedElement, -step, 0);
			case GLFW.GLFW_KEY_RIGHT -> this.layout.move(this.selectedElement, step, 0);
			case GLFW.GLFW_KEY_UP -> this.layout.move(this.selectedElement, 0, -step);
			case GLFW.GLFW_KEY_DOWN -> this.layout.move(this.selectedElement, 0, step);
			default -> {
				return true;
			}
		}
		return true;
	}

	private boolean isOverEditorButton(double mouseX, double mouseY) {
		return this.saveButton.isMouseOver(mouseX, mouseY)
				|| this.undoButton.isMouseOver(mouseX, mouseY)
				|| this.resetButton.isMouseOver(mouseX, mouseY);
	}

	private Element findElement(double mouseX, double mouseY) {
		Element[] values = Element.values();
		for (int index = values.length - 1; index >= 0; index--) {
			Element element = values[index];
			int left = x(element, element.baseX);
			int top = y(element, element.baseY);
			if (mouseX >= left && mouseX < left + element.width
					&& mouseY >= top && mouseY < top + element.height) {
				return element;
			}
		}
		return null;
	}

	private int x(Element element, int baseX) {
		return this.leftPos + this.layout.x(element, baseX);
	}

	private int y(Element element, int baseY) {
		return this.topPos + this.layout.y(element, baseY);
	}

	private int rx(Element element, int baseX) {
		return this.layout.x(element, baseX);
	}

	private int ry(Element element, int baseY) {
		return this.layout.y(element, baseY);
	}

	private static void blit16(GuiGraphicsExtractor graphics, Identifier texture, int x, int y) {
		blitSized(graphics, texture, x, y, 16, 16);
	}

	private static void blitSized(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int width, int height) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height, width, height);
	}

	private static void blitRegion(
			GuiGraphicsExtractor graphics,
			Identifier texture,
			int x,
			int y,
			int width,
			int height,
			int textureWidth,
			int textureHeight
	) {
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, width, height, textureWidth, textureHeight);
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
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
