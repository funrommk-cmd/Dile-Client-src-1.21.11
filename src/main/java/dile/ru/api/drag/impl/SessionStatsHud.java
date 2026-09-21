package dile.ru.api.drag.impl;

import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

public final class SessionStatsHud extends HudPanel {
    private static final float HEADER_HEIGHT = 15.0F;
    private static final float ROW_HEIGHT = 11.0F;
    private static final float PAD_X = 4.5F;
    private static final float PANEL_RADIUS = 5.0F;
    private static final float BORDER_THICKNESS = 1.0F;
    private static final float GLOW_ALPHA_SCALE = 0.55F;
    private static final float GLOW_SIZE = 10.0F;

    private final long sessionStartMs = System.currentTimeMillis();

    public SessionStatsHud() {
        super("sessionstats", "SessionStats", 150.0F, 300.0F, 90.0F, 50.0F);
    }

    @Override
    public void render() {
        boolean visible = mc.player != null;
        float alpha = contentAlpha(visible);
        if (alpha <= 0.0F) return;

        String server = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "singleplayer";
        int colonIdx = server.indexOf(':');
        if (colonIdx > 0) server = server.substring(0, colonIdx);

        long elapsed = System.currentTimeMillis() - sessionStartMs;
        long totalSec = elapsed / 1000;
        long h = totalSec / 3600;
        long m = totalSec % 3600 / 60;
        long s = totalSec % 60;
        String playTime = h + "h " + m + "m " + s + "s";

        String playerName = mc.player != null ? mc.player.getName().getString() : "Unknown";

        String[][] rows = {{"Server Name", server}, {"Play time", playTime}, {"Name", playerName}};

        float width = calculateWidth(rows);
        float targetHeight = HEADER_HEIGHT + (float) rows.length * ROW_HEIGHT + 2.0F;
        size(width, targetHeight);

        float x = drag.x();
        float y = drag.y();
        int themeColor = ClickGuiModule.getInstance().getColor();
        int bgColor = ColorUtil.rgba(30, 25, 40, Math.round(255.0F * alpha));
        int headerBg = ColorUtil.rgba(30, 25, 40, Math.round(240.0F * alpha));
        int outlineColor = ColorUtil.rgba(30, 25, 40, Math.round(120.0F * alpha));
        int glowColor = ColorUtil.multAlpha(themeColor, GLOW_ALPHA_SCALE * alpha);
        int textColor = ColorUtil.rgba(255, 255, 255, Math.round(220.0F * alpha));
        int headerGradientEnd = ColorUtil.lerpColor(themeColor, ColorUtil.rgba(255, 255, 255, 255), 0.3F);
        headerGradientEnd = ColorUtil.multAlpha(headerGradientEnd, alpha);

        HudRenderCompat.glow("dile:textures/particles/ghost-glow.png", x - GLOW_SIZE, y - GLOW_SIZE, width + GLOW_SIZE * 2, targetHeight + GLOW_SIZE * 2, 0.0F, glowColor);
        HudRenderCompat.background(x, y, width, targetHeight, PANEL_RADIUS, 15.0F, 1.2F, bgColor);
        Render2D.rect(x, y, width, HEADER_HEIGHT, PANEL_RADIUS, headerBg);
        Render2D.outline(x, y, width, targetHeight, PANEL_RADIUS, BORDER_THICKNESS, outlineColor);

        Render2D.text(TEXT_FONT, "Session", x + PAD_X, y + 5.5F, 6.0F, ColorUtil.multAlpha(themeColor, alpha));
        Render2D.text(FontType.ICONS_NURIK, "D", x + width - PAD_X - 10.0F, y + 4.5F, 10.0F, headerGradientEnd);

        float baseItemY = y + HEADER_HEIGHT + 3.0F;
        int separatorColor = ColorUtil.multAlpha(themeColor, 0.3F * alpha);
        Render2D.rect(x + PAD_X, baseItemY - 1.5F, width - PAD_X * 2, 0.5F, 0.5F, separatorColor);

        for (int i = 0; i < rows.length; i++) {
            float itemY = baseItemY + (float) i * ROW_HEIGHT;
            Render2D.text(TEXT_FONT, rows[i][0], x + PAD_X, itemY + 1.5F, 5.5F, textColor);
            float valueW = Render2D.textWidth(TEXT_FONT, rows[i][1], 5.5F);
            Render2D.text(TEXT_FONT, rows[i][1], x + width - PAD_X - valueW, itemY + 1.5F, 5.5F, textColor);
        }
    }

    private float calculateWidth(String[][] rows) {
        float maxLeft = 0.0F;
        float maxRight = 0.0F;
        for (String[] row : rows) {
            maxLeft = Math.max(maxLeft, Render2D.textWidth(TEXT_FONT, row[0], 5.5F));
            maxRight = Math.max(maxRight, Render2D.textWidth(TEXT_FONT, row[1], 5.5F));
        }
        return Math.max(75.0F, maxLeft + maxRight + 26.0F);
    }
}
