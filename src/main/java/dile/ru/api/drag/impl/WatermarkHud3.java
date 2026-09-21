package dile.ru.api.drag.impl;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import dile.ru.api.drag.core.ElementComponent;
import dile.ru.api.drag.core.ElementManager;
import dile.ru.api.drag.core.ElementScreen;
import dile.ru.api.drag.core.HudElement;
import dile.ru.api.module.Module;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.manager.Manager;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import static dile.ru.IMinecraft.mc;

public final class WatermarkHud3 implements HudElement {
    private static final float BLOCK_H = 14.0F;
    private static final float BLOCK_RADIUS = 3.0F;
    private static final float SEPARATOR_W = 0.5F;
    private static final float SEPARATOR_H = 7.0F;
    private static final float ICON_GAP = 3.0F;
    private static final float TEXT_GAP = 5.0F;
    private static final float INNER_PAD = 5.0F;
    private static final float GAP_BETWEEN = 2.0F;
    private static final float BOTTOM_OFFSET = 21.0F;
    private static final float FPS_SMOOTH = 0.1F;

    private static final int DEF_BG = ColorUtil.rgba(0, 0, 0, 255);
    private static final int DEF_TEXT = ColorUtil.rgba(180, 140, 255, 255);
    private static final int DEF_BORDER = ColorUtil.rgba(120, 80, 160, 255);
    private static final int DEF_GLOW = ColorUtil.rgba(120, 80, 160, 100);

    private static final float ANIM_S = 0.35F;

    private boolean showName = true;
    private boolean showFps = true;
    private boolean showCoords = true;
    private boolean showNetherCoords = true;
    private boolean showPing = true;
    private boolean showTps = true;
    private boolean showBps = true;

    private float smoothedFps;
    private boolean fpsInit;

    private final SmoothAnimation widthAnimation = new SmoothAnimation();
    private final SmoothAnimation heightAnimation = new SmoothAnimation();
    private final SmoothAnimation alphaAnimation = new SmoothAnimation();

    private final ElementComponent drag = ElementManager.getInstance()
            .register("hud.watermark3", "Watermark3", 10.0F, 10.0F);

    public WatermarkHud3() {
        widthAnimation.set(180.0F);
        heightAnimation.set(BLOCK_H);
    }

    public String elementName() {
        return "Watermark3";
    }

    public void setHudVisible(boolean visible) {
        drag.visible(visible);
    }

    @Override
    public void render() {
        renderTopBar();
        renderBottomBar();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event) {
        return false;
    }

    private void renderTopBar() {
        String nameText = resolveUsername();
        int currentFps = mc.getFps();
        if (!fpsInit) {
            smoothedFps = currentFps;
            fpsInit = true;
        }
        smoothedFps += ((float) currentFps - smoothedFps) * FPS_SMOOTH;
        String fpsText = Math.round(smoothedFps) + " Fps";
        String pingText = getPing() + " Ping";
        String tpsText = "20 Ticks";

        float nameW = Render2D.textWidth(FontType.BOLD, nameText, 7.0F);
        float fpsW = Render2D.textWidth(FontType.BOLD, fpsText, 7.0F);
        float pingW = Render2D.textWidth(FontType.BOLD, pingText, 7.0F);
        float tpsW = Render2D.textWidth(FontType.BOLD, tpsText, 7.0F);

        float iconW = Render2D.textWidth(FontType.ICONS_NURIK, "W", 9.0F);
        float iconXW = Render2D.textWidth(FontType.ICONS_NURIK, "X", 9.0F);
        float iconQW = Render2D.textWidth(FontType.ICONS_NURIK, "Q", 9.0F);
        float iconGW = Render2D.textWidth(FontType.ICONS_NURIK, "$", 9.0F);
        float iconPW = Render2D.textWidth(FontType.ICONS_NURIK, "P", 9.0F);

        float nameBlockW = INNER_PAD + iconW + ICON_GAP + nameW + TEXT_GAP;
        float fpsBlockW = INNER_PAD + iconXW + ICON_GAP + fpsW + TEXT_GAP;
        float pingBlockW = INNER_PAD + iconQW + ICON_GAP + pingW + TEXT_GAP;
        float tpsBlockW = INNER_PAD + iconGW + ICON_GAP + tpsW + TEXT_GAP;

        float buildTextW = Render2D.textWidth(FontType.BOLD, "Beta", 8.0F);
        float buildBlockW = INNER_PAD + iconPW + 8.0F + buildTextW + 12.0F + INNER_PAD;

        int infoCount = 0;
        float combinedW = 0;
        if (showName) { combinedW += nameBlockW; infoCount++; }
        if (showFps) { combinedW += fpsBlockW; infoCount++; }
        if (showPing) { combinedW += pingBlockW; infoCount++; }
        if (showTps) { combinedW += tpsBlockW; infoCount++; }
        if (infoCount == 0) combinedW = 0;

        float totalW = buildBlockW + (infoCount > 0 ? GAP_BETWEEN + combinedW : 0);
        if (totalW < buildBlockW) totalW = buildBlockW;

        float animatedW = widthAnimation.get();
        widthAnimation.run(totalW, ANIM_S, Easings.CUBIC_OUT, true);
        widthAnimation.update();

        ElementScreen screen = ElementScreen.current();
        float screenW = screen != null && screen.valid() ? screen.width() : mc.getWindow().getGuiScaledWidth();
        float posX = 5.0F;

        boolean mirror = false;
        float buildX, combinedX;
        if (!mirror) {
            buildX = posX;
            combinedX = posX + buildBlockW + GAP_BETWEEN;
        } else {
            combinedX = posX;
            buildX = posX + combinedW + GAP_BETWEEN;
        }

        int bgCol = DEF_BG;
        int textCol = DEF_TEXT;
        int borderCol = DEF_BORDER;
        int glowCol = DEF_GLOW;
        int themeCol = getThemeColor();
        int netherCol = ColorUtil.rgba(255, 80, 80, 255);

        alphaAnimation.update();
        float alphaTarget = (mc.player != null) ? 1.0F : 0.0F;
        alphaAnimation.run(alphaTarget, ANIM_S, Easings.CUBIC_OUT, true);
        float alpha = alphaAnimation.get();
        if (alpha <= 0.01F) return;

        int bgA = ColorUtil.withAlpha(bgCol, Math.round(255 * alpha));
        int textA = ColorUtil.withAlpha(textCol, Math.round(255 * alpha));
        int borderA = ColorUtil.withAlpha(borderCol, Math.round(255 * alpha));
        int glowA = ColorUtil.multAlpha(glowCol, alpha);
        int iconA = ColorUtil.withAlpha(themeCol, Math.round(255 * alpha));

        float buildY = 5.0F;

        Render2D.rect(buildX, buildY, buildBlockW - 8.7F, BLOCK_H, BLOCK_RADIUS, bgA);
        Render2D.outline(buildX, buildY, buildBlockW - 8.7F, BLOCK_H, BLOCK_RADIUS, 0.5F, borderA);

        Render2D.text(FontType.FONT, "f", buildX + 5.5F, buildY + 0.9F, 9.0F, iconA);
        float sepX1 = buildX + 8.0F + iconPW;
        Render2D.rect(sepX1, buildY + 3.0F, SEPARATOR_W, SEPARATOR_H, 0, ColorUtil.withAlpha(themeCol, Math.round(200 * alpha)));
        Render2D.text(FontType.BOLD, "Beta", sepX1 + 4.0F, buildY + 2.9F, 8.0F, textA);

        float curX = combinedX;
        if (showName || showFps || showPing || showTps) {
            float infoBgX = combinedX;
            float infoBgW = combinedW - 16.6F;

            Render2D.rect(infoBgX, buildY, infoBgW, BLOCK_H, BLOCK_RADIUS, bgA);
            Render2D.outline(infoBgX, buildY, infoBgW, BLOCK_H, BLOCK_RADIUS, 0.5F, borderA);

            float infoX = combinedX + INNER_PAD;
            if (showName) {
                float iconW2 = Render2D.textWidth(FontType.ICONS_NURIK, "W", 9.0F);
                Render2D.text(FontType.ICONS_NURIK, "W", infoX, buildY + (BLOCK_H - 9.0F) * 0.5F + 0.5F, 9.0F, iconA);
                float textX2 = infoX + iconW2 + ICON_GAP;
                Render2D.text(FontType.BOLD, nameText, textX2, buildY + (BLOCK_H - 7.0F) * 0.5F + 0.5F, 7.0F, textA);
                infoX = textX2 + nameW + TEXT_GAP;
            }
            if (showFps) {
                float iconW2 = Render2D.textWidth(FontType.ICONS_NURIK, "X", 9.0F);
                Render2D.text(FontType.ICONS_NURIK, "X", infoX, buildY + (BLOCK_H - 9.0F) * 0.5F + 0.5F, 9.0F, iconA);
                float textX2 = infoX + iconW2 + ICON_GAP;
                Render2D.text(FontType.BOLD, fpsText, textX2, buildY + (BLOCK_H - 7.0F) * 0.5F + 0.5F, 7.0F, textA);
                infoX = textX2 + fpsW + TEXT_GAP;
            }
            if (showPing) {
                float iconW2 = Render2D.textWidth(FontType.ICONS_NURIK, "Q", 9.0F);
                Render2D.text(FontType.ICONS_NURIK, "Q", infoX, buildY + (BLOCK_H - 9.0F) * 0.5F + 0.5F, 9.0F, iconA);
                float textX2 = infoX + iconW2 + ICON_GAP;
                Render2D.text(FontType.BOLD, pingText, textX2, buildY + (BLOCK_H - 7.0F) * 0.5F + 0.5F, 7.0F, textA);
                infoX = textX2 + pingW + TEXT_GAP;
            }
            if (showTps) {
                float iconW2 = Render2D.textWidth(FontType.ICONS_NURIK, "$", 9.0F);
                Render2D.text(FontType.ICONS_NURIK, "$", infoX, buildY + (BLOCK_H - 9.0F) * 0.5F + 0.5F, 9.0F, iconA);
                float textX2 = infoX + iconW2 + ICON_GAP;
                Render2D.text(FontType.BOLD, tpsText, textX2, buildY + (BLOCK_H - 7.0F) * 0.5F + 0.5F, 7.0F, textA);
            }
        }

        drag.position(posX, buildY);
        drag.size(totalW, BLOCK_H);
        drag.clamp(ElementScreen.current());
    }

    private void renderBottomBar() {
        if (!showCoords && !showBps) return;

        String coordsText = getBlockCoords();
        String netherText = getNetherCoords();
        String bpsText = getSpeed() + " Bps";

        float iconF = Render2D.textWidth(FontType.ICONS_NURIK, "F", 9.0F);
        float iconAt = Render2D.textWidth(FontType.ICONS_NURIK, "@", 9.0F);
        float coordsTextW = Render2D.textWidth(FontType.BOLD, coordsText, 7.0F);
        float netherTextW = Render2D.textWidth(FontType.BOLD, netherText, 7.0F);
        float bpsTextW = Render2D.textWidth(FontType.BOLD, bpsText, 7.0F);

        float coordsBlockW = showCoords ? INNER_PAD + iconF + ICON_GAP + coordsTextW + TEXT_GAP : 0;
        float netherBlockW = (showCoords && showNetherCoords) ? netherTextW + 6.0F : 0;
        float bpsBlockW = showBps ? INNER_PAD + iconAt + ICON_GAP + bpsTextW + TEXT_GAP : 0;

        int bottomCount = 0;
        float bottomW = 0;
        if (showCoords) { bottomW += coordsBlockW + netherBlockW; bottomCount++; }
        if (showBps) { bottomW += bpsBlockW; bottomCount++; }
        if (bottomCount > 1) bottomW += GAP_BETWEEN;
        bottomW += INNER_PAD;

        float animW = widthAnimation.get();
        float posX = drag.x();

        int bgCol = DEF_BG;
        int textCol = DEF_TEXT;
        int borderCol = DEF_BORDER;
        int glowCol = DEF_GLOW;
        int themeCol = getThemeColor();
        int netherCol = ColorUtil.rgba(255, 80, 80, 255);

        alphaAnimation.update();
        float alpha = alphaAnimation.get();
        if (alpha <= 0.01F) return;

        int bgA = ColorUtil.withAlpha(bgCol, Math.round(255 * alpha));
        int textA = ColorUtil.withAlpha(textCol, Math.round(255 * alpha));
        int borderA = ColorUtil.withAlpha(borderCol, Math.round(255 * alpha));
        int glowA = ColorUtil.multAlpha(glowCol, alpha);
        int iconA = ColorUtil.withAlpha(themeCol, Math.round(255 * alpha));

        float bottomY = 5.0F + BOTTOM_OFFSET;

        Render2D.rect(posX, bottomY, bottomW - 8.5F, BLOCK_H, BLOCK_RADIUS, bgA);
        Render2D.outline(posX, bottomY, bottomW - 8.5F, BLOCK_H, BLOCK_RADIUS, 0.5F, borderA);

        float contentX = posX + INNER_PAD;

        if (showCoords) {
            Render2D.text(FontType.ICONS_NURIK, "F", contentX, bottomY + (BLOCK_H - 9.0F) * 0.5F + 0.5F, 9.0F, iconA);
            contentX += iconF + ICON_GAP;
            Render2D.text(FontType.BOLD, coordsText, contentX, bottomY + (BLOCK_H - 7.0F) * 0.5F + 0.5F, 7.0F, textA);
            contentX += coordsTextW;

            if (showNetherCoords) {
                contentX += 2.5F;
                Render2D.rect(contentX, bottomY + 3.5F, SEPARATOR_W, SEPARATOR_H, 0, ColorUtil.withAlpha(themeCol, Math.round(200 * alpha)));
                contentX += 3.0F;
                Render2D.text(FontType.BOLD, netherText, contentX, bottomY + (BLOCK_H - 7.0F) * 0.5F + 0.5F, 7.0F, netherCol);
                contentX += netherTextW;
            }
        }

        if (showCoords && showBps) {
            contentX += 2.5F;
            Render2D.rect(contentX, bottomY + 3.5F, SEPARATOR_W, SEPARATOR_H, 0, ColorUtil.withAlpha(themeCol, Math.round(200 * alpha)));
            contentX += 3.0F;
        }

        if (showBps) {
            Render2D.text(FontType.ICONS_NURIK, "@", contentX, bottomY + (BLOCK_H - 9.0F) * 0.5F + 0.5F, 9.0F, iconA);
            contentX += iconAt + ICON_GAP;
            Render2D.text(FontType.BOLD, bpsText, contentX, bottomY + (BLOCK_H - 7.0F) * 0.5F + 0.5F, 7.0F, textA);
        }
    }

    private String getBlockCoords() {
        if (mc.player == null) return "X 0 Y 0 Z 0";
        int x = (int) Math.floor(mc.player.getX());
        int y = (int) Math.floor(mc.player.getY());
        int z = (int) Math.floor(mc.player.getZ());
        return "X " + x + " Y " + y + " Z " + z;
    }

    private String getNetherCoords() {
        if (mc.player == null) return "NX 0 NY 0 NZ 0";
        double mul = mc.level != null && mc.level.dimensionType().hasSkyLight() ? 8.0 : 1.0 / 8.0;
        int nx = (int) Math.floor(mc.player.getX() * mul);
        int ny = (int) Math.floor(mc.player.getY());
        int nz = (int) Math.floor(mc.player.getZ() * mul);
        return "NX " + nx + " NY " + ny + " NZ " + nz;
    }

    private String getSpeed() {
        if (mc.player == null) return "0.0";
        double dx = mc.player.getX() - mc.player.xOld;
        double dz = mc.player.getZ() - mc.player.zOld;
        double bps = Math.sqrt(dx * dx + dz * dz) * 20.0;
        return String.format("%.1f", bps);
    }

    private int getPing() {
        if (mc.player != null && mc.getConnection() != null
                && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null) {
            return Math.max(0, mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency());
        }
        return 0;
    }

    private String resolveUsername() {
        Module nameProtect = Manager.getModules().getByName("Name Protect").orElse(null);
        if (nameProtect != null && nameProtect.isEnabled()) {
            return "Protected";
        }
        String username = "";
        if (mc.getUser() != null) {
            username = mc.getUser().getName();
        }
        if ((username == null || username.isBlank()) && mc.player != null
                && mc.player.getGameProfile() != null) {
            username = mc.player.getGameProfile().name();
        }
        return (username == null || username.isBlank()) ? "User" : username.trim();
    }

    private static int getThemeColor() {
        return ClickGuiModule.getInstance().getColor();
    }
}
