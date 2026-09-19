package com.yuriscat.echowarrior.compat.client;

import com.yuriscat.echowarrior.compat.EchoHeroType1211;
import com.yuriscat.echowarrior.compat.ModContent1211;
import com.yuriscat.echowarrior.compat.item.EchoBiomeAffinity1211;
import com.yuriscat.echowarrior.compat.item.EchoTrait1211;
import com.yuriscat.echowarrior.compat.item.SummonerFuel1211;
import com.yuriscat.echowarrior.compat.menu.SummonerMenu1211;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class SummonerScreen1211 extends AbstractContainerScreen<SummonerMenu1211> {
    private static final int IMAGE_WIDTH = 241;
    private static final int IMAGE_HEIGHT = 201;
    private static final int SUMMON_BUTTON_X = 178;
    private static final int SUMMON_BUTTON_Y = 143;
    private static final int SUMMON_BUTTON_WIDTH = 56;
    private static final int SUMMON_BUTTON_HEIGHT = 19;
    private static final int MODE_X = 178;
    private static final int ACTIVITY_Y = 90;
    private static final int ALERT_Y = 123;
    private static final int MODE_SIZE = 18;
    private static final int SKILL_X = 61;
    private static final int SKILL_Y = 71;
    private static final int SKILL_STEP = 22;
    private static final int[] ATTRIBUTE_ICON_X = {61, 134, 61, 116, 61, 116, 61, 116};
    private static final int[] ATTRIBUTE_ICON_Y = {19, 19, 32, 32, 45, 45, 58, 58};
    private static final int[] MODULE_SLOT_X = {8, 37, 66, 95, 124, 153};

    private static final ResourceLocation BACKGROUND = icon("summoner_screen.png");
    private static final ResourceLocation MODE_DEFAULT = icon("widgets/mode_default.png");
    private static final ResourceLocation MODE_HOVER = icon("widgets/mode_hover.png");
    private static final ResourceLocation MODE_PRESSED = icon("widgets/mode_pressed.png");
    private static final ResourceLocation SKILL_OCCUPIED = icon("widgets/skill_occupied.png");
    private static final ResourceLocation SKILL_EMPTY = icon("widgets/skill_empty.png");
    private static final ResourceLocation SUMMON_DEFAULT = icon("widgets/summon_default.png");
    private static final ResourceLocation SUMMON_HOVER = icon("widgets/summon_hover.png");
    private static final ResourceLocation EXPERIENCE_BACKGROUND = icon("bars/experience_background.png");
    private static final ResourceLocation EXPERIENCE_FILL = icon("bars/experience_fill.png");
    private static final ResourceLocation FUEL_FILL = icon("bars/fuel_fill.png");
    private static final ResourceLocation FUEL_HINT = icon("slot_hints/fuel_slot_hint.png");
    private static final ResourceLocation RELIC_HINT = icon("slot_hints/relic_slot_hint.png");
    private static final ResourceLocation[] ATTRIBUTE_ICONS = {
            icon("attributes/health.png"), icon("attributes/level.png"),
            icon("attributes/attack_damage.png"), icon("attributes/attack_speed.png"),
            icon("attributes/armor.png"), icon("attributes/movement_speed.png"),
            icon("attributes/alert_range.png"), icon("attributes/summon_cost_ratio.png")
    };
    private static final ResourceLocation[] ACTIVITY_ICONS = {
            icon("modes/activity/follow.png"), icon("modes/activity/wait.png"), icon("modes/activity/wander.png")
    };
    private static final ResourceLocation[] ALERT_ICONS = {
            icon("modes/alert/aggressive.png"), icon("modes/alert/defensive.png"), icon("modes/alert/peaceful.png")
    };
    private static final ResourceLocation[][] SKILL_ICONS = {
            {icon("skills/roman_legionary/soldier_formation.png"), icon("skills/roman_legionary/legionary_bulwark.png"),
                    icon("skills/roman_legionary/shield_charge.png"), icon("skills/roman_legionary/legion_endures.png")},
            {icon("skills/aztec_warrior/quetzalcoatls_curse.png"), icon("skills/aztec_warrior/huitzilopochtlis_blessing.png"),
                    icon("skills/aztec_warrior/obsidian_wound.png"), icon("skills/aztec_warrior/pursuit.png"),
                    icon("skills/aztec_warrior/macuahuitl_mastery.png")},
            {icon("skills/egyptian_archer/cat_god.png"), icon("skills/egyptian_archer/leaf_arrow.png"),
                    icon("skills/egyptian_archer/chariot_volley.png"), icon("skills/egyptian_archer/backstep.png")},
            {icon("skills/guandao_warrior/armor_clad.png"), icon("skills/guandao_warrior/growing_valor.png"),
                    icon("skills/guandao_warrior/crescent_blade.png"), icon("skills/guandao_warrior/guandao_combo.png")},
            {icon("skills/japanese_samurai/zanshin.png"), icon("skills/japanese_samurai/fumikomi.png"),
                    icon("skills/japanese_samurai/zan.png"), icon("skills/japanese_samurai/stab.png")}
    };
    private static final ResourceLocation EGYPTIAN_CONE_ARROW_ICON = icon("skills/egyptian_archer/cone_arrow.png");
    private static final String[] ACTIVITY_KEYS = {"follow", "wait", "wander"};
    private static final String[] ALERT_KEYS = {"aggressive", "defensive", "peaceful"};
    private static final String[][] SKILL_KEYS = {
            {"roman.formation", "roman.bulwark", "roman.charge", "roman.endures"},
            {"aztec.quetzalcoatls_curse", "aztec.huitzilopochtlis_blessing", "aztec.obsidian_wound", "aztec.pursuit", "aztec.macuahuitl"},
            {"egyptian.cat_god", "egyptian.leaf_arrow", "egyptian.chariot_volley", "egyptian.backstep"},
            {"guandao.armor_clad", "guandao.growing_valor", "guandao.crescent_blade", "guandao.combo"},
            {"samurai.zanshin", "samurai.fumikomi", "samurai.zan", "samurai.stab"}
    };
    private static final int[][] SKILL_DESCRIPTION_LINES = {
            {2, 1, 2, 2},
            {1, 2, 1, 2, 2},
            {2, 2, 2, 2},
            {1, 2, 2, 3},
            {3, 2, 2, 3}
    };

    private LivingEntity previewEntity;
    private int previewHero = -1;
    private int lastFuel = -1;
    private boolean lastSpirit;
    private boolean stateInitialized;
    private int lastFeedbackValue;
    private int feedbackCode;
    private int feedbackTicks;
    private final List<UiParticle> fuelParticles = new ArrayList<>();
    private final List<UiParticle> buttonParticles = new ArrayList<>();

    public SummonerScreen1211(SummonerMenu1211 menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
        this.titleLabelX = -1000;
        this.titleLabelY = -1000;
        this.inventoryLabelX = -1000;
        this.inventoryLabelY = -1000;
    }

    private static ResourceLocation icon(String path) {
        return ModContent1211.id("textures/gui/summoner/" + path);
    }

    @Override
    protected void init() {
        super.init();
        refreshPreviewEntity();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshPreviewEntity();
        if (this.feedbackTicks > 0) this.feedbackTicks--;
        int fuel = this.menu.fuelAmount();
        boolean spirit = this.menu.spiritPresent();
        if (!this.stateInitialized) {
            this.lastFuel = fuel;
            this.lastSpirit = spirit;
            this.stateInitialized = true;
        } else {
            if (fuel < this.lastFuel) spawnFuelTransferParticles(false);
            if (spirit != this.lastSpirit) spawnButtonParticles(this.lastSpirit && !spirit);
            this.lastFuel = fuel;
            this.lastSpirit = spirit;
        }
        int feedbackValue = this.menu.actionFeedbackValue();
        if (feedbackValue != 0 && feedbackValue != this.lastFeedbackValue) {
            this.lastFeedbackValue = feedbackValue;
            this.feedbackCode = feedbackValue & 0xFF;
            if (this.feedbackCode == SummonerMenu1211.ACTION_FUEL_ROTTEN_FLESH
                    || this.feedbackCode == SummonerMenu1211.ACTION_FUEL_SOUL_SAND) {
                spawnFuelTransferParticles(true,
                        this.feedbackCode == SummonerMenu1211.ACTION_FUEL_SOUL_SAND);
                this.feedbackTicks = 0;
            } else {
                this.feedbackTicks = 60;
            }
        }
        this.fuelParticles.removeIf(particle -> ++particle.age > particle.lifetime);
        this.buttonParticles.removeIf(particle -> ++particle.age > particle.lifetime);
    }

    private void refreshPreviewEntity() {
        if (this.minecraft == null || this.minecraft.level == null || this.menu.relicLevel() <= 0) {
            this.previewEntity = null;
            this.previewHero = -1;
            return;
        }
        int hero = this.menu.heroType();
        if (this.previewEntity != null && this.previewHero == hero) return;
        this.previewHero = hero;
        this.previewEntity = switch (heroType()) {
            case ROMAN_LEGIONARY -> ModContent1211.ROMAN_LEGIONARY_ECHO.create(this.minecraft.level);
            case AZTEC_WARRIOR -> ModContent1211.AZTEC_WARRIOR_ECHO.create(this.minecraft.level);
            case EGYPTIAN_ARCHER -> ModContent1211.EGYPTIAN_ARCHER_ECHO.create(this.minecraft.level);
            case GUANDAO_WARRIOR -> ModContent1211.GUANDAO_WARRIOR_ECHO.create(this.minecraft.level);
            case JAPANESE_SAMURAI -> ModContent1211.JAPANESE_SAMURAI_ECHO.create(this.minecraft.level);
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFeedbackToast(graphics);
        renderControlTooltip(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderFeedbackToast(GuiGraphics graphics) {
        Component message = feedbackText();
        if (message == null) return;
        int left = this.leftPos + 178;
        int top = this.topPos + 4;
        int width = 63;
        boolean error = this.feedbackCode == SummonerMenu1211.ACTION_NO_RELIC
                || this.feedbackCode == SummonerMenu1211.ACTION_INVALID_SUMMONER
                || this.feedbackCode == SummonerMenu1211.ACTION_CREATE_FAILED
                || this.feedbackCode == SummonerMenu1211.ACTION_NOT_ENOUGH_FUEL
                || this.feedbackCode == SummonerMenu1211.ACTION_NO_SAFE_POSITION
                || this.feedbackCode == SummonerMenu1211.ACTION_LIMIT_REACHED
                || this.feedbackCode == SummonerMenu1211.ACTION_STALE_STATE;
        List<FormattedCharSequence> lines = this.font.split(message, width - 12);
        int visibleLines = Math.min(lines.size(), 6);
        int height = 8 + visibleLines * 10;
        graphics.fill(left, top, left + width, top + height, error ? 0xDD3B2025 : 0xDD26382D);
        graphics.fill(left, top, left + 2, top + height, error ? 0xFFE47777 : 0xFF7DCE91);
        for (int index = 0; index < visibleLines; index++) {
            graphics.drawString(this.font, lines.get(index), left + 7, top + 5 + index * 10,
                    0xFFF4F0E8, false);
        }
    }

    private Component feedbackText() {
        if (this.feedbackTicks <= 0) return null;
        return switch (this.feedbackCode) {
            case SummonerMenu1211.ACTION_SUMMONED -> Component.translatable(
                    "gui.echo_warrior.summoner.feedback.summoned");
            case SummonerMenu1211.ACTION_DISMISSED -> Component.translatable(
                    "gui.echo_warrior.summoner.feedback.dismissed");
            case SummonerMenu1211.ACTION_NO_RELIC -> Component.translatable(
                    "gui.echo_warrior.summoner.feedback.no_relic");
            case SummonerMenu1211.ACTION_INVALID_SUMMONER -> Component.translatable(
                    "gui.echo_warrior.summoner.feedback.invalid_summoner");
            case SummonerMenu1211.ACTION_CREATE_FAILED -> Component.translatable(
                    "gui.echo_warrior.summoner.feedback.create_failed");
            case SummonerMenu1211.ACTION_NOT_ENOUGH_FUEL -> Component.translatable(
                    "gui.echo_warrior.summoner.feedback.not_enough_fuel");
            case SummonerMenu1211.ACTION_NO_SAFE_POSITION -> Component.translatable(
                    "gui.echo_warrior.summoner.feedback.no_safe_position");
            case SummonerMenu1211.ACTION_MODE_CHANGED -> Component.translatable(
                    "gui.echo_warrior.summoner.feedback.mode_changed");
            case SummonerMenu1211.ACTION_SKILL_CHANGED -> Component.translatable(
                    "gui.echo_warrior.summoner.feedback.skill_changed");
            case SummonerMenu1211.ACTION_LIMIT_REACHED -> Component.translatable(
                    "gui.echo_warrior.summoner.feedback.limit_reached");
            case SummonerMenu1211.ACTION_STALE_STATE -> Component.translatable(
                    "gui.echo_warrior.summoner.feedback.stale_state");
            default -> null;
        };
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0.0F, 0.0F,
                IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);
        boolean active = this.menu.relicLevel() > 0;
        if (active && this.previewEntity != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                    this.leftPos + 8, this.topPos + 20, this.leftPos + 57, this.topPos + 90,
                    25, 0.0625F, mouseX, mouseY, this.previewEntity);
        }
        renderAttributes(graphics, active);
        renderSkills(graphics, active);
        renderTraits(graphics, active);
        renderModeRow(graphics, ACTIVITY_ICONS, ACTIVITY_Y, this.menu.activityMode(), active, mouseX, mouseY);
        renderModeRow(graphics, ALERT_ICONS, ALERT_Y, this.menu.alertMode(), active, mouseX, mouseY);
        renderSummonButton(graphics, active, mouseX, mouseY);
        renderProgressBars(graphics, active);
        renderParticles(graphics, this.fuelParticles, partialTick);
        renderParticles(graphics, this.buttonParticles, partialTick);
        if (this.menu.getSlot(SummonerMenu1211.FUEL_SLOT).getItem().isEmpty()) {
            graphics.blit(FUEL_HINT, this.leftPos + 179, this.topPos + 172, 0.0F, 0.0F, 16, 16, 16, 16);
        }
        if (this.menu.getSlot(SummonerMenu1211.RELIC_SLOT).getItem().isEmpty()) {
            graphics.blit(RELIC_HINT, this.leftPos + 217, this.topPos + 172, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }

    private void renderAttributes(GuiGraphics graphics, boolean active) {
        if (!active) return;
        for (int index = 0; index < ATTRIBUTE_ICONS.length; index++) {
            graphics.blit(ATTRIBUTE_ICONS[index], this.leftPos + ATTRIBUTE_ICON_X[index],
                    this.topPos + ATTRIBUTE_ICON_Y[index], 0.0F, 0.0F, 11, 11, 11, 11);
        }
    }

    private void renderSkills(GuiGraphics graphics, boolean active) {
        ResourceLocation[] icons = SKILL_ICONS[heroType().ordinal()].clone();
        if (heroType() == EchoHeroType1211.EGYPTIAN_ARCHER) {
            icons[1] = this.menu.egyptianArrowMode() == 2
                    ? EGYPTIAN_CONE_ARROW_ICON
                    : SKILL_ICONS[EchoHeroType1211.EGYPTIAN_ARCHER.ordinal()][1];
        }
        int count = active ? Math.min(this.menu.skillCount(), icons.length) : 0;
        for (int index = 0; index < 5; index++) {
            int frameX = this.leftPos + SKILL_X + index * SKILL_STEP;
            int frameY = this.topPos + SKILL_Y;
            ResourceLocation frame = active && index < count ? SKILL_OCCUPIED : SKILL_EMPTY;
            graphics.blit(frame, frameX, frameY, 0.0F, 0.0F, 20, 20, 20, 20);
        }
        if (!active) return;

        EchoHeroType1211 hero = heroType();
        for (int index = 0; index < count; index++) {
            int iconX = this.leftPos + SKILL_X + 2 + index * SKILL_STEP;
            int iconY = this.topPos + SKILL_Y + 2;
            graphics.blit(icons[index], iconX, iconY, 0.0F, 0.0F, 16, 16, 16, 16);
            boolean chargeSkill = hero == EchoHeroType1211.ROMAN_LEGIONARY && index == 2
                    || (hero == EchoHeroType1211.AZTEC_WARRIOR
                    || hero == EchoHeroType1211.EGYPTIAN_ARCHER) && index == 3
                    || hero == EchoHeroType1211.JAPANESE_SAMURAI && index == 1;
            boolean cooldownSkill = hero == EchoHeroType1211.GUANDAO_WARRIOR && index == 3;
            int maximumCharges = switch (hero) {
                case ROMAN_LEGIONARY, JAPANESE_SAMURAI -> 3;
                case AZTEC_WARRIOR, EGYPTIAN_ARCHER -> 2;
                case GUANDAO_WARRIOR -> 0;
            };
            if (chargeSkill && this.menu.shieldCharges() < maximumCharges
                    || cooldownSkill && this.menu.shieldChargeProgress() < 1000) {
                renderRadialCooldown(graphics, iconX, iconY, this.menu.shieldChargeProgress() / 1000.0F);
            }
            if (hero == EchoHeroType1211.ROMAN_LEGIONARY && index == 3 && this.menu.legionCooldownTicks() > 0) {
                renderRadialCooldown(graphics, iconX, iconY,
                        1.0F - this.menu.legionCooldownTicks() / 400.0F);
            }
            if (hero == EchoHeroType1211.JAPANESE_SAMURAI && index == 3 && this.menu.legionCooldownTicks() > 0) {
                renderRadialCooldown(graphics, iconX, iconY,
                        1.0F - this.menu.legionCooldownTicks() / 200.0F);
            }
            if ((this.menu.enabledSkills() & 1 << index) == 0) {
                graphics.fill(iconX, iconY, iconX + 16, iconY + 16, 0x99000000);
                for (int pixel = 1; pixel < 15; pixel++) {
                    graphics.fill(iconX + pixel, iconY + pixel,
                            iconX + pixel + 1, iconY + pixel + 1, 0xFFE05050);
                }
            }
            if (hero == EchoHeroType1211.ROMAN_LEGIONARY && index == 3 && this.menu.legionActive()) {
                drawBorder(graphics, iconX - 1, iconY - 1, iconX + 17, iconY + 17, 0xFFD8B55A);
            }
            if (chargeSkill) {
                graphics.drawString(this.font, Integer.toString(this.menu.shieldCharges()),
                        iconX + 10, iconY + 8, 0xFFFFFF, true);
            }
        }
    }

    private static void renderRadialCooldown(GuiGraphics graphics, int left, int top, float progress) {
        float reveal = Math.max(0.0F, Math.min(1.0F, progress)) * (float)(Math.PI * 2.0);
        for (int py = 0; py < 16; py++) {
            for (int px = 0; px < 16; px++) {
                double angle = Math.atan2(px - 7.5, 7.5 - py);
                if (angle < 0.0) angle += Math.PI * 2.0;
                if (angle > reveal) {
                    graphics.fill(left + px, top + py, left + px + 1, top + py + 1, 0xA8000000);
                }
            }
        }
    }

    private void renderTraits(GuiGraphics graphics, boolean active) {
        if (!active) return;
        int count = Integer.bitCount(this.menu.traitMask());
        int displayed = 0;
        for (EchoTrait1211 trait : EchoTrait1211.values()) {
            if ((this.menu.traitMask() & trait.mask()) == 0) continue;
            int x = this.leftPos + 157 - (count - 1 - displayed) * 11;
            blitScaled16(graphics, traitIcon(trait), x, this.topPos + 6, 11);
            displayed++;
        }
    }

    private ResourceLocation traitIcon(EchoTrait1211 trait) {
        String name = trait == EchoTrait1211.BIOME_AFFINITY
                ? "biome_affinity_" + EchoBiomeAffinity1211.byOrdinal(this.menu.biomeAffinity()).id()
                : trait.id();
        return icon("traits/" + name + ".png");
    }

    private void renderModeRow(GuiGraphics graphics, ResourceLocation[] icons, int yOffset, int selected,
                               boolean active, int mouseX, int mouseY) {
        for (int index = 0; index < icons.length; index++) {
            int x = this.leftPos + MODE_X + index * 19;
            int y = this.topPos + yOffset;
            ResourceLocation frame = active && index == selected ? MODE_PRESSED
                    : active && inside(mouseX, mouseY, x, y, MODE_SIZE, MODE_SIZE) ? MODE_HOVER : MODE_DEFAULT;
            graphics.blit(frame, x, y, 0.0F, 0.0F, MODE_SIZE, MODE_SIZE, MODE_SIZE, MODE_SIZE);
            if (active) graphics.blit(icons[index], x + 1, y + 1, 0.0F, 0.0F, 16, 16, 16, 16);
        }
    }

    private void renderSummonButton(GuiGraphics graphics, boolean active, int mouseX, int mouseY) {
        int x = this.leftPos + SUMMON_BUTTON_X;
        int y = this.topPos + SUMMON_BUTTON_Y;
        ResourceLocation texture = active && inside(mouseX, mouseY, x, y,
                SUMMON_BUTTON_WIDTH, SUMMON_BUTTON_HEIGHT) ? SUMMON_HOVER : SUMMON_DEFAULT;
        graphics.blit(texture, x, y, 0.0F, 0.0F,
                SUMMON_BUTTON_WIDTH, SUMMON_BUTTON_HEIGHT, SUMMON_BUTTON_WIDTH, SUMMON_BUTTON_HEIGHT);
    }

    private void renderProgressBars(GuiGraphics graphics, boolean active) {
        graphics.blit(EXPERIENCE_BACKGROUND, this.leftPos + 8, this.topPos + 114,
                0.0F, 0.0F, 160, 2, 160, 2);
        if (active && this.menu.relicExperienceNeeded() > 0) {
            int width = Math.max(0, Math.min(160,
                    Math.round(this.menu.relicExperience() * 160.0F / this.menu.relicExperienceNeeded())));
            if (width > 0) graphics.blit(EXPERIENCE_FILL, this.leftPos + 8, this.topPos + 114,
                    0.0F, 0.0F, width, 2, 160, 2);
        }
        int fuelWidth = Math.max(0, Math.min(54,
                Math.round(this.menu.fuelAmount() * 54.0F / SummonerFuel1211.CAPACITY)));
        if (fuelWidth > 0) {
            graphics.blit(FUEL_FILL, this.leftPos + 179, this.topPos + 165,
                    0.0F, 0.0F, fuelWidth, 3, 54, 3);
            long time = this.minecraft == null || this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime();
            for (int index = 0; index < 8; index++) {
                int px = Math.floorMod(index * 7 + (int)(time / (3 + index % 3)), 54);
                if (px < fuelWidth) graphics.fill(this.leftPos + 179 + px, this.topPos + 165 + index % 3,
                        this.leftPos + 180 + px, this.topPos + 166 + index % 3,
                        index % 2 == 0 ? 0xFF91FFFF : 0xFF57CBCD);
            }
        }
    }

    private void renderParticles(GuiGraphics graphics, List<UiParticle> particles, float partialTick) {
        for (UiParticle particle : particles) {
            double progress = Math.min(1.0, (particle.age + partialTick) / particle.lifetime);
            double eased = progress * progress * (3.0 - 2.0 * progress);
            double x = particle.startX + (particle.endX - particle.startX) * eased;
            double y = particle.startY + (particle.endY - particle.startY) * eased
                    - Math.sin(progress * Math.PI) * particle.arc;
            int alpha = Math.max(0, Math.min(255, (int)Math.round(255.0 * (1.0 - progress))));
            int px = this.leftPos + (int)Math.round(x);
            int py = this.topPos + (int)Math.round(y);
            graphics.fill(px, py, px + particle.size, py + particle.size,
                    alpha << 24 | particle.color & 0xFFFFFF);
        }
    }

    private void spawnFuelTransferParticles(boolean inserting) {
        ItemStack fuel = this.menu.getSlot(SummonerMenu1211.FUEL_SLOT).getItem();
        spawnFuelTransferParticles(inserting, fuel.is(Items.SOUL_SAND) || fuel.is(Items.SOUL_SOIL));
    }

    private void spawnFuelTransferParticles(boolean inserting, boolean soul) {
        if (this.minecraft == null || this.minecraft.level == null) return;
        RandomSource random = this.minecraft.level.getRandom();
        for (int index = 0; index < 8; index++) {
            double slotX = 187 + random.nextInt(7) - 3;
            double slotY = 180 + random.nextInt(7) - 3;
            double barX = 179 + Math.max(1, Math.round(this.menu.fuelAmount() * 53.0F / SummonerFuel1211.CAPACITY));
            double barY = 166;
            int color = soul ? (index % 2 == 0 ? 0x77D6CE : 0xB7F4E9)
                    : (index % 2 == 0 ? 0x8B4A42 : 0xB66A55);
            this.fuelParticles.add(new UiParticle(
                    inserting ? slotX : barX, inserting ? slotY : barY,
                    inserting ? barX : 206, inserting ? barY : 152,
                    3 + random.nextInt(5), 9 + random.nextInt(5), color, 1));
        }
    }

    private void spawnButtonParticles(boolean dismissing) {
        if (this.minecraft == null || this.minecraft.level == null) return;
        RandomSource random = this.minecraft.level.getRandom();
        for (int index = 0; index < 28; index++) {
            double insideX = SUMMON_BUTTON_X + 4 + random.nextDouble() * (SUMMON_BUTTON_WIDTH - 8);
            double insideY = SUMMON_BUTTON_Y + 3 + random.nextDouble() * (SUMMON_BUTTON_HEIGHT - 6);
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 8 + random.nextDouble() * 15;
            double outsideX = SUMMON_BUTTON_X + SUMMON_BUTTON_WIDTH / 2.0 + Math.cos(angle) * radius;
            double outsideY = SUMMON_BUTTON_Y + SUMMON_BUTTON_HEIGHT / 2.0 + Math.sin(angle) * radius;
            int color = index % 3 == 0 ? 0x61F4EB : index % 3 == 1 ? 0x50BFE6 : 0x477DE1;
            this.buttonParticles.add(new UiParticle(
                    dismissing ? outsideX : insideX, dismissing ? outsideY : insideY,
                    dismissing ? insideX : outsideX, dismissing ? insideY : outsideY,
                    2 + random.nextInt(5), 14 + random.nextInt(8), color, index % 5 == 0 ? 2 : 1));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean active = this.menu.relicLevel() > 0;
        Component heroName = active ? Component.translatable(heroType().translationKey())
                : Component.translatable("gui.echo_warrior.summoner.hero.none");
        drawFittedText(graphics, heroName, 8, 7, 115, 0xFFFFFF, true);
        if (active) {
            graphics.drawString(this.font,
                    this.menu.spiritHealth() + "/" + this.menu.spiritMaximumHealth(),
                    72, 21, attributeColor(this.menu.accessoryMaximumHealthChange()), true);
            graphics.drawString(this.font, Integer.toString(this.menu.relicLevel()), 145, 21, 0xFFFFFF, true);
            graphics.drawString(this.font, formatTenths(this.menu.spiritAttackDamage()), 72, 34, 0xFFFFFF, true);
            graphics.drawString(this.font, this.menu.spiritAttackSpeed() + "%", 127, 34, 0xFFFFFF, true);
            graphics.drawString(this.font, formatTenths(this.menu.spiritArmor()), 72, 47,
                    attributeColor(this.menu.accessoryArmorChange()), true);
            graphics.drawString(this.font, this.menu.spiritMovement() + "%", 127, 47,
                    attributeColor(this.menu.accessoryMovementChange()), true);
            graphics.drawString(this.font, Integer.toString(this.menu.spiritAlertRange()), 72, 60,
                    attributeColor(this.menu.accessoryAlertRangeChange()), true);
            graphics.drawString(this.font, this.menu.summonCostPercent() + "%", 127, 60, 0xFFFFFF, true);
        }
        drawFittedText(graphics, Component.translatable("gui.echo_warrior.summoner.section.activity"),
                179, 78, 55, 0xFFFFFF, true);
        drawFittedText(graphics, Component.translatable("gui.echo_warrior.summoner.section.alert"),
                179, 111, 55, 0xFFFFFF, true);
        Component label = Component.translatable(this.menu.relicLevel() <= 0
                ? "gui.echo_warrior.summoner.button.no_relic"
                : this.menu.spiritPresent()
                        ? "gui.echo_warrior.summoner.button.dismiss"
                        : "gui.echo_warrior.summoner.button.summon");
        drawCenteredFittedText(graphics, label, 206, 148, SUMMON_BUTTON_WIDTH - 8, 0xE0E0E0);
    }

    private void drawFittedText(GuiGraphics graphics, Component text, int x, int y,
                                int maximumWidth, int color, boolean shadow) {
        FormattedCharSequence sequence = text.getVisualOrderText();
        int width = this.font.width(sequence);
        if (width <= maximumWidth) {
            graphics.drawString(this.font, sequence, x, y, color, shadow);
            return;
        }
        float scale = maximumWidth / (float)width;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + (9.0F - 9.0F * scale) / 2.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, sequence, 0, 0, color, shadow);
        graphics.pose().popPose();
    }

    private void drawCenteredFittedText(GuiGraphics graphics, Component text, int centerX, int y,
                                        int maximumWidth, int color) {
        FormattedCharSequence sequence = text.getVisualOrderText();
        int width = this.font.width(sequence);
        float scale = Math.min(1.0F, maximumWidth / (float)Math.max(1, width));
        float drawnWidth = width * scale;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX - drawnWidth / 2.0F,
                y + (9.0F - 9.0F * scale) / 2.0F, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(this.font, sequence, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private void renderControlTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean active = this.menu.relicLevel() > 0;
        if (renderAttributeTooltip(graphics, mouseX, mouseY, active)) return;

        int count = Integer.bitCount(this.menu.traitMask());
        int displayed = 0;
        for (EchoTrait1211 trait : EchoTrait1211.values()) {
            if ((this.menu.traitMask() & trait.mask()) == 0) continue;
            int x = 157 - (count - 1 - displayed) * 11;
            if (this.isHovering(x, 6, 11, 11, mouseX, mouseY)) {
                Component name = Component.translatable(trait.nameTranslationKey());
                if (trait == EchoTrait1211.BIOME_AFFINITY) {
                    name = Component.translatable(EchoBiomeAffinity1211.byOrdinal(
                            this.menu.biomeAffinity()).nameTranslationKey());
                }
                graphics.renderComponentTooltip(this.font, List.of(
                        name.copy().withStyle(ChatFormatting.GOLD),
                        Component.translatable(trait.descriptionTranslationKey()).withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
                return;
            }
            displayed++;
        }
        for (int index = 0; index < 3; index++) {
            if (this.isHovering(MODE_X + index * 19, ACTIVITY_Y, MODE_SIZE, MODE_SIZE, mouseX, mouseY)) {
                renderModeTooltip(graphics, mouseX, mouseY,
                        "gui.echo_warrior.summoner.activity." + ACTIVITY_KEYS[index],
                        index == this.menu.activityMode());
                return;
            }
            if (this.isHovering(MODE_X + index * 19, ALERT_Y, MODE_SIZE, MODE_SIZE, mouseX, mouseY)) {
                renderModeTooltip(graphics, mouseX, mouseY,
                        "gui.echo_warrior.summoner.alert." + ALERT_KEYS[index],
                        index == this.menu.alertMode());
                return;
            }
        }
        for (int index = 0; index < this.menu.skillCount(); index++) {
            if (this.isHovering(SKILL_X + index * SKILL_STEP, SKILL_Y, 20, 20, mouseX, mouseY)) {
                renderSkillTooltip(graphics, mouseX, mouseY, index);
                return;
            }
        }
        if (this.isHovering(7, 113, 162, 4, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.echo_warrior.summoner.experience.title")
                    .withStyle(ChatFormatting.GOLD));
            if (!active) {
                lines.add(Component.translatable("gui.echo_warrior.summoner.experience.no_relic")
                        .withStyle(ChatFormatting.GRAY));
            } else if (this.menu.relicLevel() >= 30) {
                lines.add(Component.translatable("gui.echo_warrior.summoner.experience.max")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable("gui.echo_warrior.summoner.experience.progress",
                        this.menu.relicLevel(), this.menu.relicExperience(), this.menu.relicExperienceNeeded())
                        .withStyle(ChatFormatting.GRAY));
            }
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }
        if (this.isHovering(179, 164, 54, 5, mouseX, mouseY)) {
            int summonCost = Math.max(1, (int)Math.ceil(
                    SummonerFuel1211.SUMMON_COST * this.menu.summonCostPercent() / 100.0));
            double healCost = 2.0 * this.menu.summonCostPercent() / 100.0;
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("gui.echo_warrior.summoner.fuel.title").withStyle(ChatFormatting.GOLD),
                    Component.literal(this.menu.fuelAmount() + "/" + SummonerFuel1211.CAPACITY)
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.echo_warrior.summoner.fuel.summon_cost", summonCost)
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.echo_warrior.summoner.fuel.heal_cost",
                            String.format(java.util.Locale.ROOT, "%.1f", healCost)).withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.echo_warrior.summoner.fuel.summons",
                            this.menu.fuelAmount() / summonCost).withStyle(ChatFormatting.GRAY)
            ), mouseX, mouseY);
            return;
        }
        if (this.menu.getSlot(SummonerMenu1211.FUEL_SLOT).getItem().isEmpty()
                && this.isHovering(179, 172, 16, 16, mouseX, mouseY)) {
            graphics.renderComponentTooltip(this.font, List.of(
                    Component.translatable("gui.echo_warrior.summoner.fuel_input.title")
                            .withStyle(ChatFormatting.GOLD),
                    Component.translatable("gui.echo_warrior.summoner.fuel_input.rotten_flesh")
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.echo_warrior.summoner.fuel_input.soul")
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.echo_warrior.summoner.fuel_input.full")
                            .withStyle(ChatFormatting.DARK_GRAY)
            ), mouseX, mouseY);
            return;
        }
        for (int index = 0; index < SummonerMenu1211.MODULE_SLOT_COUNT; index++) {
            if (this.menu.getSlot(index).getItem().isEmpty()
                    && this.isHovering(MODULE_SLOT_X[index], 94, 16, 16, mouseX, mouseY)) {
                graphics.renderComponentTooltip(this.font, List.of(
                        Component.translatable("gui.echo_warrior.summoner.accessory.title")
                                .withStyle(ChatFormatting.GOLD),
                        Component.translatable("gui.echo_warrior.summoner.accessory.install")
                                .withStyle(ChatFormatting.GRAY),
                        Component.translatable("gui.echo_warrior.summoner.accessory.unique")
                                .withStyle(ChatFormatting.DARK_GRAY)
                ), mouseX, mouseY);
                return;
            }
        }
        if (this.isHovering(SUMMON_BUTTON_X, SUMMON_BUTTON_Y,
                SUMMON_BUTTON_WIDTH, SUMMON_BUTTON_HEIGHT, mouseX, mouseY)) {
            String titleKey = this.menu.spiritPresent()
                    ? "gui.echo_warrior.summoner.button.dismiss"
                    : "gui.echo_warrior.summoner.button.summon";
            String descriptionKey = !active
                    ? "gui.echo_warrior.summoner.button.summon.tooltip.missing"
                    : this.menu.spiritPresent()
                            ? "gui.echo_warrior.summoner.button.dismiss.tooltip"
                            : "gui.echo_warrior.summoner.button.summon.tooltip";
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(titleKey).withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.GRAY));
            Component feedback = feedbackText();
            if (feedback != null) {
                lines.add(feedback.copy().withStyle(this.feedbackCode == SummonerMenu1211.ACTION_SUMMONED
                        || this.feedbackCode == SummonerMenu1211.ACTION_DISMISSED
                        || this.feedbackCode == SummonerMenu1211.ACTION_MODE_CHANGED
                        || this.feedbackCode == SummonerMenu1211.ACTION_SKILL_CHANGED
                        ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
    }

    private boolean renderAttributeTooltip(GuiGraphics graphics, int mouseX, int mouseY, boolean active) {
        if (!active) return false;
        int[][] boxes = {
                {61, 19, 71, 11}, {134, 19, 35, 11},
                {61, 32, 53, 11}, {116, 32, 53, 11},
                {61, 45, 53, 11}, {116, 45, 53, 11},
                {61, 58, 53, 11}, {116, 58, 53, 11}
        };
        Component[] descriptions = {
                Component.translatable(this.menu.spiritPresent()
                                ? "gui.echo_warrior.summoner.attribute.health"
                                : "gui.echo_warrior.summoner.attribute.health_absent",
                        this.menu.spiritHealth(), this.menu.spiritMaximumHealth()),
                Component.translatable("gui.echo_warrior.summoner.attribute.level", this.menu.relicLevel()),
                Component.translatable("gui.echo_warrior.summoner.attribute.attack",
                        formatTenths(this.menu.spiritAttackDamage())),
                Component.translatable("gui.echo_warrior.summoner.attribute.attack_speed",
                        this.menu.spiritAttackSpeed() + "%"),
                Component.translatable("gui.echo_warrior.summoner.attribute.armor",
                        formatTenths(this.menu.spiritArmor())),
                Component.translatable("gui.echo_warrior.summoner.attribute.movement",
                        this.menu.spiritMovement() + "%"),
                Component.translatable("gui.echo_warrior.summoner.attribute.alert_range",
                        this.menu.spiritAlertRange()),
                Component.translatable("gui.echo_warrior.summoner.attribute.fuel_cost",
                        this.menu.summonCostPercent() + "%")
        };
        for (int index = 0; index < boxes.length; index++) {
            int[] box = boxes[index];
            if (!this.isHovering(box[0], box[1], box[2], box[3], mouseX, mouseY)) continue;
            graphics.renderTooltip(this.font, descriptions[index], mouseX, mouseY);
            return true;
        }
        return false;
    }

    private void renderModeTooltip(GuiGraphics graphics, int mouseX, int mouseY,
                                   String translationKey, boolean selected) {
        Component state = Component.translatable(this.menu.relicLevel() <= 0
                ? "gui.echo_warrior.summoner.state.no_relic"
                : selected
                        ? "gui.echo_warrior.summoner.state.selected"
                        : "gui.echo_warrior.summoner.state.switch");
        graphics.renderComponentTooltip(this.font, List.of(
                Component.translatable(translationKey + ".name").withStyle(ChatFormatting.GOLD),
                Component.translatable(translationKey + ".description").withStyle(ChatFormatting.GRAY),
                state.copy().withStyle(ChatFormatting.DARK_GRAY)
        ), mouseX, mouseY);
    }

    private void renderSkillTooltip(GuiGraphics graphics, int mouseX, int mouseY, int skill) {
        int hero = heroType().ordinal();
        String suffix = SKILL_KEYS[hero][skill];
        if (heroType() == EchoHeroType1211.EGYPTIAN_ARCHER && skill == 1) {
            suffix = switch (this.menu.egyptianArrowMode()) {
                case 1 -> "egyptian.leaf_arrow";
                case 2 -> "egyptian.cone_arrow";
                default -> "egyptian.normal_arrow";
            };
        }
        String key = "gui.echo_warrior.summoner.skill." + suffix;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(key + ".name").withStyle(ChatFormatting.GOLD));
        for (int line = 1; line <= SKILL_DESCRIPTION_LINES[hero][skill]; line++) {
            lines.add(Component.translatable(key + ".description." + line).withStyle(ChatFormatting.GRAY));
        }
        if (!(heroType() == EchoHeroType1211.EGYPTIAN_ARCHER && skill == 1)) {
            boolean enabled = (this.menu.enabledSkills() & 1 << skill) != 0;
            lines.add(Component.translatable(enabled
                    ? "gui.echo_warrior.summoner.skill.enabled"
                    : "gui.echo_warrior.summoner.skill.disabled").withStyle(ChatFormatting.GRAY));
        }
        graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private static void blitScaled16(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size) {
        float scale = size / 16.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menu.relicLevel() > 0) {
            for (int index = 0; index < 3; index++) {
                if (this.isHovering(MODE_X + index * 19, ACTIVITY_Y, MODE_SIZE, MODE_SIZE, mouseX, mouseY)) {
                    sendButton(SummonerMenu1211.BUTTON_ACTIVITY_START + index);
                    return true;
                }
                if (this.isHovering(MODE_X + index * 19, ALERT_Y, MODE_SIZE, MODE_SIZE, mouseX, mouseY)) {
                    sendButton(SummonerMenu1211.BUTTON_ALERT_START + index);
                    return true;
                }
            }
            for (int index = 0; index < this.menu.skillCount(); index++) {
                if (this.isHovering(SKILL_X + index * SKILL_STEP, SKILL_Y, 20, 20, mouseX, mouseY)) {
                    sendButton(SummonerMenu1211.BUTTON_SKILL_START + index);
                    return true;
                }
            }
        }
        if (button == 0 && this.isHovering(SUMMON_BUTTON_X, SUMMON_BUTTON_Y,
                SUMMON_BUTTON_WIDTH, SUMMON_BUTTON_HEIGHT, mouseX, mouseY)) {
            sendButton(SummonerMenu1211.BUTTON_SUMMON_OR_DISMISS);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private EchoHeroType1211 heroType() {
        EchoHeroType1211[] values = EchoHeroType1211.values();
        return values[Math.max(0, Math.min(values.length - 1, this.menu.heroType()))];
    }

    private static int attributeColor(double change) {
        return change > 1.0E-6 ? 0x55FF55 : change < -1.0E-6 ? 0xFF5555 : 0xFFFFFF;
    }

    private static String formatTenths(int value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value / 10.0);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void drawBorder(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    private static final class UiParticle {
        private final double startX;
        private final double startY;
        private final double endX;
        private final double endY;
        private final double arc;
        private final int lifetime;
        private final int color;
        private final int size;
        private int age;

        private UiParticle(double startX, double startY, double endX, double endY,
                           double arc, int lifetime, int color, int size) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.arc = arc;
            this.lifetime = lifetime;
            this.color = color;
            this.size = size;
        }
    }
}
