package dile.ru.api.drag.impl;

import net.minecraft.sounds.SoundEvent;
import dile.ru.api.drag.core.ElementScreen;
import dile.ru.utils.sounds.SoundManager;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Notifications extends HudPanel {

    private static final float HEIGHT = 16f;
    private static final float PAD_X = 8f;
    private static final float CORNER = 5f;
    private static final float BAR_W = 3f;
    private static final float BAR_PAD = 5f;
    private static final float BAR_VERT_PAD = 4f;
    private static final float GAP_Y = 3f;
    private static final float SLIDE_DIST = 20f;

    private static final float TOGGLE_WIDTH = 20f;
    private static final float TOGGLE_HEIGHT = 10f;
    private static final float TOGGLE_CIRCLE_SIZE = 7f;
    private static final float TOGGLE_PADDING = 1.5f;
    private static final float TOGGLE_CORNER = TOGGLE_HEIGHT * 0.5f;

    private static final long DURATION_MS = 3000L;
    private static final float ANIM_S = 0.35F;
    private static final int MAX_NOTIFICATIONS = 8;

    private static final int BG_COLOR = ColorUtil.rgba(15, 15, 15, 220);
    private static final int TEXT_COLOR = ColorUtil.rgba(230, 230, 230, 255);
    private static final int ENABLED_COL = ColorUtil.rgba(80, 220, 100, 255);
    private static final int DISABLED_COL = ColorUtil.rgba(220, 65, 65, 255);
    private static final int TOGGLE_OFF_BG = ColorUtil.rgba(50, 50, 55, 255);
    private static final int TOGGLE_CIRCLE = ColorUtil.rgba(255, 255, 255, 255);

    private static final List<NotifEntry> NOTIFICATIONS = new ArrayList<>();

    private final Map<NotifEntry, SmoothAnimation> appearAnims = new HashMap<>();
    private final Map<NotifEntry, SmoothAnimation> toggleAnims = new HashMap<>();
    private final Map<NotifEntry, Float> currentYPos = new HashMap<>();
    private long lastRenderTime = System.currentTimeMillis();

    public Notifications() {
        super("notifications", "Notifications", 10.0F, 330.0F, 118.0F, 24.0F);
        drag.locked(true);
    }

    public static void push(String title, String text) {
        push(title, text, DURATION_MS, null);
    }

    public static void push(String title, String text, long stayMs) {
        push(title, text, stayMs, null);
    }

    public static void push(String title, String text, long stayMs, SoundEvent sound) {
        if (NOTIFICATIONS.size() >= MAX_NOTIFICATIONS) {
            NOTIFICATIONS.subList(0, NOTIFICATIONS.size() - MAX_NOTIFICATIONS + 1).clear();
        }
        NOTIFICATIONS.add(new NotifEntry(null, false, title, text, System.currentTimeMillis(), Math.max(1L, stayMs)));
        if (sound != null) {
            SoundManager.playSoundDirect(sound, 0.7F, 1.0F);
        }
    }

    public static void pushModule(String moduleName, boolean enabled) {
        pushModule(moduleName, enabled, DURATION_MS, null);
    }

    public static void pushModule(String moduleName, boolean enabled, long stayMs, SoundEvent sound) {
        if (NOTIFICATIONS.size() >= MAX_NOTIFICATIONS) {
            NOTIFICATIONS.subList(0, NOTIFICATIONS.size() - MAX_NOTIFICATIONS + 1).clear();
        }
        NOTIFICATIONS.add(new NotifEntry(moduleName, enabled, null, null, System.currentTimeMillis(), Math.max(1L, stayMs)));
        if (sound != null) {
            SoundManager.playSoundDirect(sound, 0.7F, 1.0F);
        }
    }

    @Override
    public void render() {
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastRenderTime) / 1000f;
        lastRenderTime = now;

        for (NotifEntry e : NOTIFICATIONS) {
            if (!e.exiting && now > e.startTime + e.duration) {
                e.exiting = true;
            }
        }
        NOTIFICATIONS.removeIf(e -> e.exiting && !appearAnims.containsKey(e) || e.exiting && appearAnims.containsKey(e) && appearAnims.get(e).get() < 0.01F);

        if (NOTIFICATIONS.isEmpty()) {
            appearAnims.clear();
            toggleAnims.clear();
            currentYPos.clear();
            ElementScreen es = ElementScreen.current();
            float sw = es != null && es.valid() ? es.width() : 118f;
            float sh = es != null && es.valid() ? es.height() : HEIGHT;
            drag.position((sw - 118f) * 0.5f, (sh - HEIGHT) * 0.5f);
            drag.size(118f, HEIGHT);
            contentVisible(true);
            return;
        }

        contentVisible(true);

        ElementScreen screen = ElementScreen.current();
        float screenW = screen != null && screen.valid() ? screen.width() : mc.getWindow().getGuiScaledWidth();
        float screenH = screen != null && screen.valid() ? screen.height() : mc.getWindow().getGuiScaledHeight();

        float maxWidth = 0;
        for (NotifEntry entry : NOTIFICATIONS) {
            maxWidth = Math.max(maxWidth, calcWidth(entry));
        }

        float centerX = (screenW - maxWidth) * 0.5f;
        float topY = (screenH - (NOTIFICATIONS.size() * (HEIGHT + GAP_Y) - GAP_Y)) * 0.5f;

        float lerpSpeed = 12f;
        float targetY = topY;

        for (NotifEntry entry : NOTIFICATIONS) {
            SmoothAnimation appearAnim = appearAnims.computeIfAbsent(entry, e -> {
                SmoothAnimation a = new SmoothAnimation();
                a.set(0.0F);
                a.run(1.0, ANIM_S, Easings.EXPO_OUT, true);
                return a;
            });
            appearAnim.update();
            float appear = clamp(appearAnim.get(), 0.0F, 1.0F);

            SmoothAnimation toggleAnim = toggleAnims.computeIfAbsent(entry, e -> {
                SmoothAnimation a = new SmoothAnimation();
                a.set(entry.enabled ? 1.0F : 0.0F);
                return a;
            });
            toggleAnim.run(entry.enabled ? 1.0F : 0.0F, 0.2F, Easings.CUBIC_OUT, true);
            toggleAnim.update();
            float toggleProgress = clamp(toggleAnim.get(), 0.0F, 1.0F);

            long age = now - entry.startTime;
            float timerProgress = Math.max(0f, 1f - (float) age / entry.duration);

            float alpha = appear;
            if (age > entry.duration - 200) {
                alpha = Math.max(0f, (1f - (age - (entry.duration - 200)) / 200f)) * appear;
            }
            alpha = clamp(alpha, 0f, 1f);

            if (entry.exiting) {
                SmoothAnimation exitAnim = appearAnims.get(entry);
                if (exitAnim != null) {
                    exitAnim.update();
                    alpha *= clamp(exitAnim.get(), 0f, 1f);
                }
            }

            if (alpha <= 0.01f) {
                targetY += HEIGHT + GAP_Y;
                continue;
            }

            Float cy = currentYPos.get(entry);
            if (cy == null) cy = targetY;
            float diff = targetY - cy;
            if (Math.abs(diff) > 0.01f) {
                cy = cy + diff * Math.min(1f, lerpSpeed * deltaTime);
            } else {
                cy = targetY;
            }
            currentYPos.put(entry, cy);

            float entryW = calcWidth(entry);
            float x = centerX;
            float slideX = SLIDE_DIST * (1f - appear);
            float dx = x + slideX;
            float dy = cy;

            int barColor = entry.moduleName != null ? (entry.enabled ? ENABLED_COL : DISABLED_COL) : ENABLED_COL;

            int bgCol = ColorUtil.withAlpha(BG_COLOR, (int) (255 * alpha));
            int txtCol = ColorUtil.withAlpha(TEXT_COLOR, (int) (255 * alpha));
            int stateCol = ColorUtil.withAlpha(barColor, (int) (255 * alpha));

            Render2D.rect(dx - 2, dy - 2, entryW + 4, HEIGHT + 4, CORNER + 2, ColorUtil.withAlpha(ColorUtil.rgba(0, 0, 0, 120), (int) (100 * alpha)));
            Render2D.rect(dx, dy, entryW, HEIGHT, CORNER, bgCol);

            float textStartX = dx + PAD_X;

            if (entry.moduleName != null) {
                float toggleX = dx + PAD_X;
                float toggleY = dy + (HEIGHT - TOGGLE_HEIGHT) * 0.5f;

                int themeColor = accentColor();
                int toggleBgColor = ColorUtil.interpolateColor(
                        ColorUtil.withAlpha(TOGGLE_OFF_BG, (int) (255 * alpha)),
                        ColorUtil.withAlpha(themeColor, (int) (255 * alpha)),
                        toggleProgress
                );

                if (entry.enabled) {
                    float glowIntensity = toggleProgress;
                    Render2D.rect(toggleX - 1, toggleY - 1, TOGGLE_WIDTH + 2, TOGGLE_HEIGHT + 2,
                            TOGGLE_CORNER + 1, ColorUtil.withAlpha(themeColor, (int) (80 * glowIntensity * alpha)));
                }

                Render2D.rect(toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_CORNER, toggleBgColor);

                float circleX = toggleX + TOGGLE_PADDING
                        + (TOGGLE_WIDTH - TOGGLE_PADDING * 2 - TOGGLE_CIRCLE_SIZE) * toggleProgress;
                float circleY = toggleY + (TOGGLE_HEIGHT - TOGGLE_CIRCLE_SIZE) * 0.5f;
                int circleColor = ColorUtil.withAlpha(TOGGLE_CIRCLE, (int) (255 * alpha));

                Render2D.rect(circleX, circleY, TOGGLE_CIRCLE_SIZE, TOGGLE_CIRCLE_SIZE,
                        TOGGLE_CIRCLE_SIZE * 0.5f, circleColor);

                textStartX += TOGGLE_WIDTH + 5f;
            }

            float textY = dy + (HEIGHT - 5.5f) * 0.5f + 0.5f;

            if (entry.moduleName != null) {
                String basePart = "Module \u00AB" + entry.moduleName + "\u00BB  ";
                String statePart = entry.enabled ? "Enabled" : "Disabled";
                float baseW = Render2D.textWidth(TEXT_FONT, basePart, 5.5f);
                Render2D.text(TEXT_FONT, basePart, textStartX, textY, 5.5f, txtCol);
                Render2D.text(TEXT_FONT, statePart, textStartX + baseW, textY, 5.5f, stateCol);
            } else {
                String fullText = entry.title + "  " + entry.text;
                float available = entryW - PAD_X * 2 - BAR_PAD - BAR_W - BAR_PAD;
                String trimmed = trimToWidth(fullText, TEXT_FONT, 5.5f, available);
                Render2D.text(TEXT_FONT, trimmed, textStartX, textY, 5.5f, txtCol);
            }

            float barX = dx + entryW - BAR_PAD - BAR_W;
            float barInnerH = HEIGHT - BAR_VERT_PAD * 2f;
            float barCurH = barInnerH * timerProgress;
            float barBottom = dy + BAR_VERT_PAD + barInnerH;
            float barStartY = barBottom - barCurH;
            if (barCurH > 0.5f) {
                int barCol = ColorUtil.withAlpha(barColor, (int) (255 * alpha));
                Render2D.rect(barX, barStartY, BAR_W, barCurH, BAR_W * 0.5f, barCol);
            }

            targetY += HEIGHT + GAP_Y;
        }

        for (NotifEntry e : new ArrayList<>(appearAnims.keySet())) {
            if (!NOTIFICATIONS.contains(e)) {
                appearAnims.remove(e);
                toggleAnims.remove(e);
                currentYPos.remove(e);
            }
        }

        drag.position(centerX, topY);
        drag.size((float) Math.ceil(maxWidth), Math.max(HEIGHT, targetY - topY));
        drag.clamp(ElementScreen.current());
    }

    private float calcWidth(NotifEntry entry) {
        FontType font = TEXT_FONT;
        float textWidth;
        if (entry.moduleName != null) {
            textWidth = Render2D.textWidth(font, "Module \u00AB" + entry.moduleName + "\u00BB  " + (entry.enabled ? "Enabled" : "Disabled"), 5.5f);
        } else {
            String fullText = entry.title + "  " + entry.text;
            float available = 118f - PAD_X * 2 - BAR_PAD - BAR_W - BAR_PAD;
            String trimmed = trimToWidth(fullText, font, 5.5f, available);
            textWidth = Render2D.textWidth(font, trimmed, 5.5f);
        }
        float toggleSpace = entry.moduleName != null ? (TOGGLE_WIDTH + 6f) : 0f;
        return toggleSpace + textWidth + PAD_X * 2 + BAR_PAD + BAR_W + BAR_PAD;
    }

    private static final class NotifEntry {
        final String moduleName;
        boolean enabled;
        final String title;
        final String text;
        final long startTime;
        final long duration;
        boolean exiting = false;

        NotifEntry(String moduleName, boolean enabled, String title, String text, long startTime, long duration) {
            this.moduleName = moduleName;
            this.enabled = enabled;
            this.title = title;
            this.text = text;
            this.startTime = startTime;
            this.duration = duration;
        }
    }
}
