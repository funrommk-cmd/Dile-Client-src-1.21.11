package dile.ru.api.drag.impl;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PotionsHud3 extends HudPanel {
    private static final float HEADER_H = 15.0F;
    private static final float ITEM_SPACING = 11.0F;
    private static final float PANEL_RADIUS = 5.0F;
    private static final float MIN_WIDTH = 60.0F;
    private static final float ICON_SIZE = 8.0F;
    private static final float TITLE_SIZE = 7.0F;
    private static final float ITEM_TEXT_SIZE = 6.0F;
    private static final float ANIM_S = 0.24F;

    private static final int BG_COL = ColorUtil.rgba(30, 25, 40, 255);
    private static final int HEADER_COL = ColorUtil.rgba(30, 25, 40, 255);
    private static final int BORDER_COL = ColorUtil.rgba(120, 80, 160, 255);
    private static final int GLOW_COL = ColorUtil.rgba(120, 80, 160, 100);

    private final List<RowEntry> rowEntries = new ArrayList<>();
    private final List<MobEffectInstance> effects = new ArrayList<>();
    private final SmoothAnimation panelAlpha = new SmoothAnimation();
    private final SmoothAnimation heightAnim = new SmoothAnimation();
    private long previewIconSecond = -1L;
    private Holder<MobEffect> previewPositiveEffect = MobEffects.SPEED;
    private Holder<MobEffect> previewNegativeEffect = MobEffects.POISON;

    public PotionsHud3() {
        super("potions3", "Potions", 300.0F, 100.0F, MIN_WIDTH, HEADER_H + 11.0F);
    }

    @Override
    public void render() {
        effects.clear();
        if (mc.player != null) {
            for (MobEffectInstance effect : mc.player.getActiveEffects()) {
                if (effect.showIcon()) {
                    effects.add(effect);
                }
            }
        }

        boolean preview = effects.isEmpty() && editPreview();
        boolean targetVisible = !effects.isEmpty() || preview;

        panelAlpha.update();
        panelAlpha.run(targetVisible ? 1.0 : 0.0, ANIM_S, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
        float alpha = panelAlpha.get();

        for (RowEntry entry : rowEntries) {
            entry.active = false;
            entry.alpha.update();
            entry.y.update();
        }

        int targetRows = 0;
        if (preview) {
            updatePreviewEffects();
            RowEntry entry = row("__preview_pos", "Speed", "**:**", 0.0F, previewPositiveEffect);
            entry.active = true;
            entry.alpha.run(1.0, ANIM_S, Easings.EXPO_OUT, true);
            entry.y.run(0.0F, ANIM_S, Easings.EXPO_OUT, true);
            targetRows = 1;

            RowEntry entry2 = row("__preview_neg", "Poison", "**:**", targetRows * ITEM_SPACING, previewNegativeEffect);
            entry2.active = true;
            entry2.alpha.run(1.0, ANIM_S, Easings.EXPO_OUT, true);
            entry2.y.run(targetRows * ITEM_SPACING, ANIM_S, Easings.EXPO_OUT, true);
            targetRows++;
        } else {
            for (MobEffectInstance effect : effects) {
                String name = effect.getEffect().value().getDisplayName().getString();
                String level = levelText(effect);
                String duration = formatPotionDurationTicks(effect.getDuration());
                String display = level.isEmpty() ? name : name + " " + level;
                float targetY = targetRows * ITEM_SPACING;
                RowEntry entry = row("potion:" + name + ":" + effect.getAmplifier(), display, duration, targetY, effect.getEffect());
                entry.active = true;
                entry.alpha.run(1.0, ANIM_S, Easings.EXPO_OUT, true);
                entry.y.run(targetY, ANIM_S, Easings.EXPO_OUT, true);
                targetRows++;
            }
        }

        for (RowEntry entry : rowEntries) {
            if (!entry.active) {
                entry.alpha.run(0.0, ANIM_S, Easings.EXPO_IN, true);
            }
        }
        rowEntries.removeIf(e -> !e.active && e.alpha.get() <= 0.01F && !e.alpha.isAlive());

        float targetHeight = Math.max(20.0F, HEADER_H + Math.max(1, targetRows) * ITEM_SPACING);
        heightAnim.update();
        heightAnim.run(targetHeight, ANIM_S, Easings.CUBIC_OUT, true);
        float animatedH = heightAnim.get();

        float width = MIN_WIDTH;
        for (RowEntry entry : rowEntries) {
            if (entry.alpha.get() > 0.01F || entry.active) {
                float nameW = Render2D.textWidth(TEXT_FONT, entry.name, ITEM_TEXT_SIZE);
                float durW = Render2D.textWidth(TEXT_FONT, entry.duration, ITEM_TEXT_SIZE);
                width = Math.max(width, nameW + durW + 30.0F);
            }
        }

        size(width, animatedH + 2.3F);

        boolean visible = targetVisible || alpha > 0.01F || !rowEntries.isEmpty();
        contentVisible(visible);
        if (!visible) return;

        int themeCol = ClickGuiModule.getInstance().getColor();
        int bgA = Math.round(255.0F * alpha);
        int titleCol = ColorUtil.withAlpha(themeCol, bgA);
        int iconCol = ColorUtil.withAlpha(themeCol, bgA);

        float x = drag.x();
        float y = drag.y();
        float w = drag.width();
        float h = drag.height();

        int blurBg = ColorUtil.rgba(0, 0, 0, Math.round(bgA * 0.45F));
        int blurHeader = ColorUtil.rgba(0, 0, 0, bgA);

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y, w, h + 2.3F)
                .radius(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurBg)
                .build());

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y + HEADER_H, w, h - HEADER_H + 2.3F)
                .radius(0, 0, PANEL_RADIUS, PANEL_RADIUS)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurBg)
                .build());

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y, w, HEADER_H)
                .radius(PANEL_RADIUS, PANEL_RADIUS, 0.0F, 0.0F)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurHeader)
                .build());

        String headerText = "Potions";
        Render2D.text(TEXT_FONT, headerText, x + 4.5F, y + (HEADER_H - TITLE_SIZE) * 0.5F - 0.5F, TITLE_SIZE, titleCol);

        String iconChar = "E";
        float iconWidth = Render2D.textWidth(FontType.ICONS_NURIK, iconChar, ICON_SIZE);
        Render2D.text(FontType.ICONS_NURIK, iconChar,
                x + w - iconWidth - 4.5F,
                y + (HEADER_H - ICON_SIZE) * 0.5F + 0.1F,
                ICON_SIZE, iconCol);

        float rowY = y + HEADER_H;
        for (RowEntry entry : rowEntries) {
            float rowAlpha = alpha * entry.alpha.get();
            if (rowAlpha <= 0.01F) continue;

            float currentY = rowY + entry.y.get();
            float xOffset = entry.xOffset;
            float nameX = x + 2.5F + 9.0F + 3.0F + xOffset;
            float timeX = x + w - 2.5F + xOffset;
            String visibleName = trimToWidth(entry.name, TEXT_FONT, ITEM_TEXT_SIZE, timeX - nameX - 5.0F);

            boolean isHarmful = entry.effect != null && entry.effect.value().getCategory() == MobEffectCategory.HARMFUL;
            int baseTextCol = isHarmful ? ColorUtil.rgba(255, 85, 85, 255) : ColorUtil.rgba(255, 255, 255, 255);
            float pulseVal = 1.0F;
            if (entry.remainingTicks > 0 && entry.remainingTicks <= 120) {
                pulseVal = 0.6F + 0.4F * (float) Math.sin(System.currentTimeMillis() / 150.0);
            }
            int textCol = ColorUtil.withAlpha(baseTextCol, Math.round(255.0F * rowAlpha * pulseVal));

            if (entry.effect != null) {
                Render2D.effectIcon(entry.effect, x + 2.5F + xOffset, currentY + 1.0F, 9.0F, ColorUtil.rgba(255, 255, 255, Math.round(245.0F * rowAlpha)));
            }
            Render2D.text(TEXT_FONT, visibleName, nameX, currentY + 4.5F, ITEM_TEXT_SIZE, textCol);
            Render2D.text(TEXT_FONT, entry.duration,
                    timeX - Render2D.textWidth(TEXT_FONT, entry.duration, ITEM_TEXT_SIZE),
                    currentY + 4.5F, ITEM_TEXT_SIZE, textCol);
        }
    }

    private RowEntry row(String key, String name, String duration, float targetY, Holder<MobEffect> effect) {
        for (RowEntry entry : rowEntries) {
            if (entry.key.equals(key)) {
                entry.name = name;
                entry.duration = duration;
                entry.effect = effect;
                return entry;
            }
        }
        RowEntry entry = new RowEntry(key, name, duration, effect);
        entry.alpha.set(0.0);
        entry.y.set(targetY + 4.0F);
        rowEntries.add(entry);
        return entry;
    }

    private void updatePreviewEffects() {
        long second = System.currentTimeMillis() / 1000L;
        if (second != previewIconSecond) {
            previewIconSecond = second;
            previewPositiveEffect = MobEffects.SPEED;
            previewNegativeEffect = MobEffects.POISON;
        }
    }

    private String formatPotionDurationTicks(int ticks) {
        if (ticks < 0) return "**:**";
        if (ticks > 72000) return "**:**";
        int totalSeconds = Math.max(0, ticks / 20);
        int minutes = Math.min(99, totalSeconds / 60);
        int seconds = totalSeconds % 60;
        return twoDigits(minutes) + ":" + twoDigits(seconds);
    }

    private String levelText(MobEffectInstance effect) {
        int level = effect.getAmplifier() + 1;
        return level <= 1 ? "" : Integer.toString(level);
    }

    private static final class RowEntry {
        private final String key;
        private final SmoothAnimation alpha = new SmoothAnimation();
        private final SmoothAnimation y = new SmoothAnimation();
        private float xOffset = 0.0F;
        private String name;
        private String duration;
        private Holder<MobEffect> effect;
        private int remainingTicks;
        private boolean active;

        private RowEntry(String key, String name, String duration, Holder<MobEffect> effect) {
            this.key = key;
            this.name = name;
            this.duration = duration;
            this.effect = effect;
        }
    }
}
