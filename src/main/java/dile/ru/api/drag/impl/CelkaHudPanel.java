package dile.ru.api.drag.impl;

import dile.ru.api.module.impl.visual.Hud5;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

public abstract class CelkaHudPanel extends HudPanel {
    protected static final FontType CELKA_FONT = FontType.MONT;
    protected static final int CELKA_TEXT = ColorUtil.rgba(255, 255, 255, 255);
    protected static final int CELKA_MUTED = ColorUtil.rgba(169, 169, 169, 255);

    protected CelkaHudPanel(String id, String title, float defaultX, float defaultY, float width, float height) {
        super("celka." + id, title, defaultX, defaultY, width, height);
    }

    protected static int style(int offset) {
        return CelkaColors.getColorStyle(offset);
    }

    protected static void glow(float x, float y, float width, float height, float blurStrength, int top, int bottom) {
        if (Hud5.isGlowEnabled() && width > 0.0F && height > 0.0F) {
            Render2D.blur(x, y, width, height, 6.0f, blurStrength, 1.0f, top, top, bottom, bottom);
        }
    }

    protected static void gradientRound(float x, float y, float width, float height, float radius, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        Render2D.rect(x, y, width, height, radius, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
    }

    protected static void centeredText(String text, float centerX, float y, float size, int color) {
        Render2D.text(CELKA_FONT, text, centerX - Render2D.textWidth(CELKA_FONT, text, size) / 2.0F, y, size, color);
    }

    protected static void circle(float cx, float cy, float radius, int top, int bottom) {
        if (radius <= 0.0F) {
            return;
        }
        float radiusSquared = radius * radius;
        for (float dy = -radius; dy <= radius; dy += 1.0F) {
            float halfWidth = (float) Math.sqrt(Math.max(0.0F, radiusSquared - dy * dy));
            float progress = (dy + radius) / (radius * 2.0F);
            Render2D.rect(cx - halfWidth, cy + dy, halfWidth * 2.0F, 1.0F, ColorUtil.interpolateColor(top, bottom, progress));
        }
    }

    protected static void cloud(float x, float y, float size) {
        int white = ColorUtil.rgba(255, 255, 255, 235);
        float scale = size / 24.0F;
        circle(x + 8.0F * scale, y + 14.0F * scale, 7.0F * scale, white, white);
        circle(x + 14.0F * scale, y + 14.0F * scale, 7.0F * scale, white, white);
        circle(x + 11.0F * scale, y + 10.0F * scale, 6.0F * scale, white, white);
        circle(x + 6.0F * scale, y + 12.0F * scale, 4.5F * scale, white, white);
        circle(x + 17.0F * scale, y + 12.0F * scale, 4.5F * scale, white, white);
    }
}
