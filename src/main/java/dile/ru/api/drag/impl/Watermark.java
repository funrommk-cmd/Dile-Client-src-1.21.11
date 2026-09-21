package dile.ru.api.drag.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import dile.ru.api.config.ConfigManager;
import dile.ru.api.drag.core.ElementComponent;
import dile.ru.api.drag.core.ElementManager;
import dile.ru.api.drag.core.ElementScreen;
import dile.ru.api.drag.core.HudElement;
import dile.ru.api.module.Module;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.manager.Manager;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static dile.ru.IMinecraft.mc;

public final class Watermark implements HudElement {
    private static final float ICON_BOX_SIZE = 20.0f;
    private static final float ROW_HEIGHT = 20.0f;
    private static final float ROW_GAP = 2.0f;
    private static final float TEXT_SIZE = 6.0f;
    private static final float TOP_TEXT_OFFSET = 25.0f;
    private static final float TOP_DOT_OFFSET = 19.0f;
    private static final float TOP_SEGMENT_GAP = 8.0f;
    private static final float BOTTOM_TEXT_OFFSET = 24.0f;
    private static final float BOTTOM_DOT_OFFSET = 18.0f;
    private static final float BOTTOM_SEGMENT_GAP = 10.0f;
    private static final float SIZE_ANIMATION_SECONDS = 0.22f;
    private static final FontType TEXT_FONT = FontType.BOLD;

    private static final int ELEMENT_COUNT = 5;
    private static final String[] ELEMENT_NAMES = {"Nickname", "Fps", "Ping", "Tps", "Coordinates"};
    private static final String[] ELEMENT_ICONS = {"t", "b", "y", "z", "x"};
    private static final float[] ICON_SIZES = {9.0f, 9.0f, 8.5f, 7.0f, 9.0f};
    private static final float[] ICON_Y_OFFSETS = {6.5f, 6.5f, 6.5f, 7.0f, 6.0f};
    private static final int DEFAULT_TOP_ROW_COUNT = 2;

    private static final float POPUP_WIDTH = 105.0F;
    private static final float POPUP_ROW_HEIGHT = 11.0F;
    private static final float POPUP_PADDING = 5.0F;
    private static final float TOGGLE_WIDTH = 20.0F;
    private static final float TOGGLE_HEIGHT = 10.0F;
    private static final float TOGGLE_CIRCLE_SIZE = 7.0F;
    private static final float TOGGLE_PADDING = 1.5F;
    private static final float TOGGLE_CORNER = TOGGLE_HEIGHT * 0.5F;
    private static final float SLIDER_WIDTH = 55.0F;
    private static final float SLIDER_HEIGHT = 4.0F;
    private static final float SEPARATOR_HEIGHT = 4.0F;
    private static final float RESET_HEIGHT = 10.0F;
    private static final float DRAG_START_THRESHOLD = 4.0F;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = ConfigManager.systemDirectory().resolve("watermark.dile");

    private final ElementComponent drag = ElementManager.getInstance()
            .register("hud.watermark", "Watermark", 10.0f, 10.0f)
            .minimumSize(112.0f, 28.0f);

    private final SmoothAnimation topWidthAnimation = new SmoothAnimation();
    private final SmoothAnimation bottomWidthAnimation = new SmoothAnimation();
    private final SmoothAnimation totalWidthAnimation = new SmoothAnimation();
    private final SmoothAnimation popupAnimation = new SmoothAnimation();
    private final SmoothAnimation[] elementYAnims;

    private boolean settingsOpen;
    private float sizeMultiplier = 1.0F;
    private int[] elementOrder = {0, 1, 2, 3, 4};
    private boolean[] elementEnabled = {true, true, true, true, true};
    private int topRowCount = DEFAULT_TOP_ROW_COUNT;

    private boolean draggingSlider;
    private int dragElementIndex = -1;
    private float dragOffsetY;
    private int dragTargetIndex = -1;
    private float currentMouseY;
    private float dragStartMouseY;
    private boolean dragConfirmed;

    public Watermark() {
        topWidthAnimation.set(160.0f);
        bottomWidthAnimation.set(154.0f);
        totalWidthAnimation.set(182.0f);
        elementYAnims = new SmoothAnimation[ELEMENT_COUNT];
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            elementYAnims[i] = new SmoothAnimation();
        }
        loadSettings();
    }

    public String elementName() {
        return "Watermark";
    }

    public void setHudVisible(boolean visible) {
        drag.visible(visible);
    }

    private boolean editPreview() {
        return mc.screen instanceof ChatScreen;
    }

    private boolean isOnWatermark(float mx, float my) {
        return mx >= drag.x() && mx <= drag.x() + drag.width()
                && my >= drag.y() && my <= drag.y() + drag.height();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event == null || !editPreview()) return false;
        float mx = (float) event.x();
        float my = (float) event.y();
        currentMouseY = my;

        if (event.button() == 1) {
            if (settingsOpen) {
                if (handlePopupClick(mx, my)) return true;
                settingsOpen = false;
                draggingSlider = false;
                dragElementIndex = -1;
                dragConfirmed = false;
                return false;
            }
            if (isOnWatermark(mx, my)) {
                settingsOpen = true;
                popupAnimation.set(1.0F);
                initElementAnimations();
                return true;
            }
        }

        if (event.button() == 0 && settingsOpen) {
            if (handlePopupClick(mx, my)) return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event == null) return false;
        if (event.button() == 0) {
            if (dragConfirmed && dragElementIndex >= 0 && dragTargetIndex >= 0 && dragElementIndex != dragTargetIndex) {
                moveElement(dragElementIndex, dragTargetIndex);
            }
            draggingSlider = false;
            dragElementIndex = -1;
            dragTargetIndex = -1;
            dragConfirmed = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event) {
        if (event == null || !settingsOpen) return false;
        float mx = (float) event.x();
        float my = (float) event.y();
        currentMouseY = my;

        if (draggingSlider) {
            float popupX = getPopupX();
            float sliderX = popupX + 48.0F;
            float sliderEndX = sliderX + SLIDER_WIDTH;
            float t = clamp((mx - sliderX) / (sliderEndX - sliderX), 0.0F, 1.0F);
            sizeMultiplier = 0.5F + t * 1.5F;
            saveSettings();
            return true;
        }

        if (dragElementIndex >= 0) {
            if (!dragConfirmed) {
                if (Math.abs(my - dragStartMouseY) >= DRAG_START_THRESHOLD) {
                    dragConfirmed = true;
                } else {
                    return false;
                }
            }
            dragTargetIndex = getPopupTargetIndex(my);
            return true;
        }

        return false;
    }

    @Override
    public void render() {
        if (settingsOpen && !editPreview()) {
            settingsOpen = false;
            draggingSlider = false;
            dragElementIndex = -1;
            dragConfirmed = false;
        }
        WatermarkState state = logics();
        renderWatermark(state);
        if (settingsOpen && editPreview()) {
            renderPopup();
        }
    }

    private WatermarkState logics() {
        int ping = getPing();
        String username = resolveDisplayUsername();
        String fpsText = "Fps " + mc.getFps();
        String pingText = "Ping " + ping;
        String tpsText = "Tps 20";
        String coordsText = getCoordinates();

        String[] texts = {username, fpsText, pingText, tpsText, coordsText};

        float topWidth = 0;
        int topCount = 0;
        for (int i = 0; i < topRowCount; i++) {
            int elIdx = elementOrder[i];
            if (!elementEnabled[elIdx]) continue;
            topWidth += topSegmentWidth(texts[elIdx]);
            topCount++;
        }
        if (topCount > 1) topWidth -= TOP_SEGMENT_GAP;
        if (topCount > 0) topWidth += 4.0f;

        float bottomWidth = 0;
        int bottomCount = 0;
        for (int i = topRowCount; i < ELEMENT_COUNT; i++) {
            int elIdx = elementOrder[i];
            if (!elementEnabled[elIdx]) continue;
            bottomWidth += bottomSegmentWidth(Render2D.textWidth(FontType.BOLD, texts[elIdx], TEXT_SIZE));
            bottomCount++;
        }
        if (bottomCount > 1) bottomWidth -= BOTTOM_SEGMENT_GAP;
        if (bottomCount > 0) bottomWidth += 4.0f;

        float animatedTopWidth = animate(topWidthAnimation, topCount > 0 ? topWidth : 0);
        float animatedBottomWidth = animate(bottomWidthAnimation, bottomCount > 0 ? bottomWidth : 0);
        float totalWidth = Math.max(
                topCount > 0 ? ICON_BOX_SIZE + ROW_GAP + animatedTopWidth : 0,
                bottomCount > 0 ? animatedBottomWidth : 0
        );
        if (topCount == 0 && bottomCount == 0) totalWidth = ICON_BOX_SIZE;
        float animatedTotalWidth = animate(totalWidthAnimation, totalWidth);
        float height = ROW_HEIGHT;
        if (topCount > 0 && bottomCount > 0) {
            height = ROW_HEIGHT * 2.0f + ROW_GAP;
        }

        float scaledTotalWidth = animatedTotalWidth * sizeMultiplier;
        float scaledHeight = height * sizeMultiplier;

        drag.size(Math.max(scaledTotalWidth, ICON_BOX_SIZE), Math.max(scaledHeight, ROW_HEIGHT));
        drag.clamp(ElementScreen.current());

        float x = drag.x();
        float y = drag.y();
        float radius = 4;

        return new WatermarkState(x, y, radius, animatedTopWidth, animatedBottomWidth,
                topCount, bottomCount, texts);
    }

    private void renderWatermark(WatermarkState state) {
        if (state == null) return;
        int background = ColorUtil.rgba(0, 0, 0, 255);
        int themeColor = getThemeColor();
        int glow = ColorUtil.multAlpha(themeColor, 130.0f / 255.0f);
        int purple = themeColor;
        int white = ColorUtil.rgba(255, 255, 255, 255);

        HudRenderCompat.background(state.x, state.y, ICON_BOX_SIZE, ROW_HEIGHT, state.radius, 15.0f, 1.2f, background);
        HudRenderCompat.glow("dile:textures/particles/ghost-glow.png", state.x - 1.0f, state.y - 1.0f, 22.0f, 22.0f, 0.0f, glow);
        Render2D.text(FontType.FONT, "f", state.x + 3.6f, state.y + 2.6f, 13.0f, purple);

        if (state.topCount > 0) {
            HudRenderCompat.background(state.x + ICON_BOX_SIZE + ROW_GAP, state.y, state.topWidth, ROW_HEIGHT, state.radius, 15.0f, 1.2f, background);
            float topX = state.x + ICON_BOX_SIZE + ROW_GAP;
            for (int i = 0; i < topRowCount; i++) {
                int elIdx = elementOrder[i];
                if (!elementEnabled[elIdx]) continue;
                topX += renderTopItem(topX, state.y, ELEMENT_ICONS[elIdx], ICON_SIZES[elIdx], ICON_Y_OFFSETS[elIdx],
                        state.texts[elIdx], purple, white, glow);
            }
        }

        if (state.bottomCount > 0) {
            float bottomY = state.y + (state.topCount > 0 ? ROW_HEIGHT + ROW_GAP : 0);
            HudRenderCompat.background(state.x, bottomY, state.bottomWidth, ROW_HEIGHT, state.radius, 15.0f, 1.2f, background);
            float bottomX = state.x;
            for (int i = topRowCount; i < ELEMENT_COUNT; i++) {
                int elIdx = elementOrder[i];
                if (!elementEnabled[elIdx]) continue;
                renderBottomItem(bottomX, bottomY, ELEMENT_ICONS[elIdx], ICON_SIZES[elIdx], ICON_Y_OFFSETS[elIdx],
                        state.texts[elIdx], purple, white, glow);
                bottomX += bottomSegmentWidth(Render2D.textWidth(FontType.BOLD, state.texts[elIdx], TEXT_SIZE));
            }
        }
    }

    private float renderTopItem(float x, float y, String icon, float iconSize, float iconYOffset, String text, int iconColor, int textColor, int glowColor) {
        HudRenderCompat.glow("dile:textures/particles/ghost-glow.png", x - 1.0f, y, 22.0f, 22.0f, 0.0f, glowColor);
        Render2D.text(FontType.MAINMENUSCREEN, icon, x + 6.5f, y + iconYOffset, iconSize, iconColor);
        Render2D.rect(x + TOP_DOT_OFFSET, y + 9.0f, 4.0f, 4.0f, 4.0f, textColor);
        Render2D.text(FontType.BOLD, text, x + TOP_TEXT_OFFSET, y + 7.0f, TEXT_SIZE, textColor);
        return topSegmentWidth(text);
    }

    private void renderBottomItem(float x, float y, String icon, float iconSize, float iconYOffset, String text, int iconColor, int textColor, int glowColor) {
        HudRenderCompat.glow("dile:textures/particles/ghost-glow.png", x - 1.5f, y - 1.0f, 22.0f, 22.0f, 0.0f, glowColor);
        Render2D.text(FontType.MAINMENUSCREEN, icon, x + 6.5f, y + iconYOffset, iconSize, iconColor);
        Render2D.rect(x + BOTTOM_DOT_OFFSET, y + 8.5f, 4.0f, 4.0f, 4.0f, textColor);
        Render2D.text(FontType.BOLD, text, x + BOTTOM_TEXT_OFFSET, y + 6.0f, TEXT_SIZE, textColor);
    }

    private float topSegmentWidth(String text) {
        return TOP_TEXT_OFFSET + Render2D.textWidth(FontType.BOLD, text, TEXT_SIZE) + TOP_SEGMENT_GAP;
    }

    private float bottomSegmentWidth(float textWidth) {
        return BOTTOM_TEXT_OFFSET + textWidth + BOTTOM_SEGMENT_GAP;
    }

    private float animate(SmoothAnimation animation, float target) {
        animation.run(target, SIZE_ANIMATION_SECONDS, Easings.CUBIC_OUT, true);
        animation.update();
        return animation.get();
    }

    private String getCoordinates() {
        if (mc.player == null) return "X 0 Y 0 Z 0";
        int x = (int) Math.floor(mc.player.getX());
        int y = (int) Math.floor(mc.player.getY());
        int z = (int) Math.floor(mc.player.getZ());
        return "X " + x + " Y " + y + " Z " + z;
    }

    private int getPing() {
        if (mc.player != null && mc.getConnection() != null && mc.getConnection().getPlayerInfo(mc.player.getUUID()) != null) {
            return Math.max(0, mc.getConnection().getPlayerInfo(mc.player.getUUID()).getLatency());
        }
        return 0;
    }

    private String resolveDisplayUsername() {
        Module nameProtect = Manager.getModules().getByName("Name Protect").orElse(null);
        if (nameProtect != null && nameProtect.isEnabled()) {
            return "Protected";
        }
        String username = "";
        if (mc.getUser() != null) {
            username = mc.getUser().getName();
        }
        if (isBlank(username) && mc.player != null && mc.player.getGameProfile() != null) {
            username = mc.player.getGameProfile().name();
        }
        return isBlank(username) ? "User" : username.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // ==================== POPUP ====================

    private void initElementAnimations() {
        float popupY = getPopupY();
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            int elIdx = elementOrder[i];
            elementYAnims[elIdx].set(getPopupElementY(i, popupY));
        }
    }

    private void renderPopup() {
        popupAnimation.update();
        popupAnimation.run(1.0, 0.2, Easings.CUBIC_OUT, true);
        float pa = popupAnimation.get();
        if (pa <= 0.01F) return;

        float popupX = getPopupX();
        float popupY = getPopupY();
        float popupH = getPopupHeight();

        int bgColor = ColorUtil.rgba(15, 15, 15, Math.round(230.0F * pa));
        int themeCol = getThemeColor();
        int borderColor = ColorUtil.multAlpha(themeCol, 40.0F / 255.0F * pa);
        int textColor = ColorUtil.rgba(255, 255, 255, Math.round(220.0F * pa));
        int valueColor = ColorUtil.multAlpha(themeCol, 220.0F / 255.0F * pa);
        int mutedColor = ColorUtil.rgba(180, 180, 180, Math.round(150.0F * pa));
        int circleColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * pa));

        Render2D.rect(popupX - 1, popupY - 1, POPUP_WIDTH + 2, popupH + 2, 6.0F, borderColor);
        Render2D.rect(popupX, popupY, POPUP_WIDTH, popupH, 5.0F, bgColor);

        float rowY = popupY + POPUP_PADDING;

        // Size slider (always visible, no toggle)
        Render2D.text(TEXT_FONT, "Размер:", popupX + POPUP_PADDING, rowY + 1.0F, 5.5F, textColor);
        float sliderX = popupX + 48.0F;
        float sliderY = rowY + (POPUP_ROW_HEIGHT - SLIDER_HEIGHT) * 0.5F;
        int trackBg = ColorUtil.rgba(50, 50, 55, Math.round(255.0F * pa));
        Render2D.rect(sliderX, sliderY, SLIDER_WIDTH, SLIDER_HEIGHT, SLIDER_HEIGHT * 0.5F, trackBg);

        float t = clamp((sizeMultiplier - 0.5F) / 1.5F, 0.0F, 1.0F);
        float fillW = SLIDER_WIDTH * t;
        int fillColor = ColorUtil.multAlpha(themeCol, 200.0F / 255.0F * pa);
        if (fillW > 1.0F) {
            Render2D.rect(sliderX, sliderY, fillW, SLIDER_HEIGHT, SLIDER_HEIGHT * 0.5F, fillColor);
        }
        float thumbX = sliderX + fillW - 4.0F;
        float thumbY = sliderY - 2.0F;
        int thumbColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * pa));
        Render2D.rect(thumbX, thumbY, 8.0F, SLIDER_HEIGHT + 4.0F, 4.0F, thumbColor);

        String sizeText = String.format("%.1fx", sizeMultiplier);
        float sizeTextW = Render2D.textWidth(TEXT_FONT, sizeText, 5.0F);
        Render2D.text(TEXT_FONT, sizeText, popupX + POPUP_WIDTH - POPUP_PADDING - sizeTextW, rowY + 2.0F, 5.0F, valueColor);

        rowY += POPUP_ROW_HEIGHT + SEPARATOR_HEIGHT;

        // Top section
        for (int i = 0; i < topRowCount; i++) {
            int elIdx = elementOrder[i];
            elementYAnims[elIdx].run(getPopupElementY(i, popupY), 0.2, Easings.CUBIC_OUT, true);
            elementYAnims[elIdx].update();
            float elY = elementYAnims[elIdx].get();
            if (dragElementIndex == i && dragConfirmed) continue;
            renderPopupElementRow(popupX, elY, elIdx, pa, textColor);
        }

        // Divider
        if (topRowCount > 0 && topRowCount < ELEMENT_COUNT) {
            float lastTopY = getPopupElementY(topRowCount - 1, popupY) + POPUP_ROW_HEIGHT;
            Render2D.rect(popupX + POPUP_PADDING, lastTopY + 1.0F, POPUP_WIDTH - POPUP_PADDING * 2, 0.5F,
                    ColorUtil.rgba(100, 100, 100, Math.round(100.0F * pa)));
        }

        // Bottom section
        for (int i = topRowCount; i < ELEMENT_COUNT; i++) {
            int elIdx = elementOrder[i];
            elementYAnims[elIdx].run(getPopupElementY(i, popupY), 0.2, Easings.CUBIC_OUT, true);
            elementYAnims[elIdx].update();
            float elY = elementYAnims[elIdx].get();
            if (dragElementIndex == i && dragConfirmed) continue;
            renderPopupElementRow(popupX, elY, elIdx, pa, textColor);
        }

        // Dragged element follows mouse
        if (dragElementIndex >= 0 && dragConfirmed) {
            int elIdx = elementOrder[dragElementIndex];
            float dragY = currentMouseY - dragOffsetY;
            int dragBg = ColorUtil.multAlpha(themeCol, 120.0F / 255.0F * pa);
            float elX = popupX + POPUP_PADDING;
            float elW = POPUP_WIDTH - POPUP_PADDING * 2;
            Render2D.rect(elX, dragY, elW, POPUP_ROW_HEIGHT, 3.0F, dragBg);
            Render2D.text(TEXT_FONT, ELEMENT_NAMES[elIdx], elX + 3.0F, dragY + 2.0F, 5.5F,
                    ColorUtil.rgba(255, 255, 255, Math.round(220.0F * pa)));

            int dragToggleBg = elementEnabled[elIdx] ?
                    ColorUtil.multAlpha(themeCol, 200.0F / 255.0F * pa) :
                    ColorUtil.rgba(50, 50, 55, Math.round(200.0F * pa));
            float dtX = elX + elW - TOGGLE_WIDTH - 2.0F;
            float dtY = dragY + (POPUP_ROW_HEIGHT - TOGGLE_HEIGHT) * 0.5F;
            Render2D.rect(dtX, dtY, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_CORNER, dragToggleBg);
            float dProg = elementEnabled[elIdx] ? 1.0F : 0.0F;
            float dcX = dtX + TOGGLE_PADDING + (TOGGLE_WIDTH - TOGGLE_PADDING * 2 - TOGGLE_CIRCLE_SIZE) * dProg;
            float dcY = dtY + (TOGGLE_HEIGHT - TOGGLE_CIRCLE_SIZE) * 0.5F;
            Render2D.rect(dcX, dcY, TOGGLE_CIRCLE_SIZE, TOGGLE_CIRCLE_SIZE,
                    TOGGLE_CIRCLE_SIZE * 0.5F, circleColor);
        }

        // Reset button
        float resetY = popupY + popupH - POPUP_PADDING - RESET_HEIGHT;
        float resetW = POPUP_WIDTH - POPUP_PADDING * 2;
        int resetBg = ColorUtil.rgba(40, 40, 45, Math.round(255.0F * pa));
        Render2D.rect(popupX + POPUP_PADDING, resetY, resetW, RESET_HEIGHT, 3.0F, resetBg);
        String resetText = "Сброс";
        float resetTextW = Render2D.textWidth(TEXT_FONT, resetText, 5.5F);
        Render2D.text(TEXT_FONT, resetText, popupX + POPUP_PADDING + (resetW - resetTextW) * 0.5F,
                resetY + 1.5F, 5.5F, textColor);
    }

    private void renderPopupElementRow(float popupX, float elY, int elIdx, float pa, int textColor) {
        float elX = popupX + POPUP_PADDING;
        float elW = POPUP_WIDTH - POPUP_PADDING * 2;

        Render2D.rect(elX, elY, elW, POPUP_ROW_HEIGHT, 3.0F,
                ColorUtil.rgba(30, 30, 35, Math.round(120.0F * pa)));

        Render2D.text(TEXT_FONT, ELEMENT_NAMES[elIdx], elX + 3.0F, elY + 2.0F, 5.5F, textColor);

        int toggleBg = elementEnabled[elIdx] ?
                ColorUtil.multAlpha(getThemeColor(), 200.0F / 255.0F * pa) :
                ColorUtil.rgba(50, 50, 55, Math.round(200.0F * pa));
        float tX = elX + elW - TOGGLE_WIDTH - 2.0F;
        float tY = elY + (POPUP_ROW_HEIGHT - TOGGLE_HEIGHT) * 0.5F;
        Render2D.rect(tX, tY, TOGGLE_WIDTH, TOGGLE_HEIGHT, TOGGLE_CORNER, toggleBg);

        float prog = elementEnabled[elIdx] ? 1.0F : 0.0F;
        float cX = tX + TOGGLE_PADDING + (TOGGLE_WIDTH - TOGGLE_PADDING * 2 - TOGGLE_CIRCLE_SIZE) * prog;
        float cY = tY + (TOGGLE_HEIGHT - TOGGLE_CIRCLE_SIZE) * 0.5F;
        int cColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * pa));
        Render2D.rect(cX, cY, TOGGLE_CIRCLE_SIZE, TOGGLE_CIRCLE_SIZE,
                TOGGLE_CIRCLE_SIZE * 0.5F, cColor);
    }

    // ==================== POPUP INTERACTION ====================

    private boolean handlePopupClick(float mx, float my) {
        float popupX = getPopupX();
        float popupY = getPopupY();
        float popupH = getPopupHeight();

        if (mx < popupX || mx > popupX + POPUP_WIDTH || my < popupY || my > popupY + popupH) {
            return false;
        }

        // Check element rows first (they take priority)
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            float elY = getPopupElementY(i, popupY);
            float elX = popupX + POPUP_PADDING;
            float elW = POPUP_WIDTH - POPUP_PADDING * 2;

            if (my < elY || my > elY + POPUP_ROW_HEIGHT) continue;
            if (mx < elX || mx > elX + elW) continue;

            int elIdx = elementOrder[i];

            // Toggle area (right side)
            float tX = elX + elW - TOGGLE_WIDTH - 2.0F;
            if (mx >= tX) {
                elementEnabled[elIdx] = !elementEnabled[elIdx];
                saveSettings();
                return true;
            }

            // Name area (left side) -> start drag
            dragElementIndex = i;
            dragOffsetY = my - elY;
            dragTargetIndex = i;
            dragStartMouseY = my;
            dragConfirmed = false;
            return true;
        }

        // Check slider
        float sliderX = popupX + 48.0F;
        float sliderRowY = popupY + POPUP_PADDING;
        float sliderY = sliderRowY + (POPUP_ROW_HEIGHT - SLIDER_HEIGHT) * 0.5F;
        if (mx >= sliderX - 4.0F && mx <= sliderX + SLIDER_WIDTH + 4.0F
                && my >= sliderY - 4.0F && my <= sliderY + SLIDER_HEIGHT + 4.0F) {
            draggingSlider = true;
            float t = clamp((mx - sliderX) / SLIDER_WIDTH, 0.0F, 1.0F);
            sizeMultiplier = 0.5F + t * 1.5F;
            saveSettings();
            return true;
        }

        // Check reset button
        float resetY = popupY + popupH - POPUP_PADDING - RESET_HEIGHT;
        float resetW = POPUP_WIDTH - POPUP_PADDING * 2;
        if (mx >= popupX + POPUP_PADDING && mx <= popupX + POPUP_PADDING + resetW
                && my >= resetY && my <= resetY + RESET_HEIGHT) {
            resetSettings();
            initElementAnimations();
            return true;
        }

        return true;
    }

    private void moveElement(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) return;
        int el = elementOrder[fromIndex];
        if (fromIndex < toIndex) {
            for (int i = fromIndex; i < toIndex; i++) {
                elementOrder[i] = elementOrder[i + 1];
            }
        } else {
            for (int i = fromIndex; i > toIndex; i--) {
                elementOrder[i] = elementOrder[i - 1];
            }
        }
        elementOrder[toIndex] = el;
        saveSettings();
    }

    private int getPopupTargetIndex(float mouseY) {
        float popupY = getPopupY();
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            float elY = getPopupElementY(i, popupY);
            float elMidY = elY + POPUP_ROW_HEIGHT * 0.5F;
            if (mouseY < elMidY) return i;
        }
        return ELEMENT_COUNT - 1;
    }

    // ==================== POPUP LAYOUT ====================

    private float getPopupX() {
        float x = drag.x() - POPUP_WIDTH - 5.0F;
        if (x < 0) x = drag.x() + drag.width() + 5.0F;
        return x;
    }

    private float getPopupY() {
        float popupH = getPopupHeight();
        float y = drag.y() + (drag.height() - popupH) * 0.5F;
        y = Math.max(0, y);
        float screenH = mc.getWindow().getGuiScaledHeight();
        if (y + popupH > screenH) y = screenH - popupH;
        return y;
    }

    private float getPopupHeight() {
        float h = POPUP_PADDING * 2;
        h += POPUP_ROW_HEIGHT; // size slider row (always)
        h += SEPARATOR_HEIGHT;
        h += topRowCount * POPUP_ROW_HEIGHT;
        if (topRowCount > 0 && topRowCount < ELEMENT_COUNT) h += 4.0F; // divider
        int bottomCount = ELEMENT_COUNT - topRowCount;
        h += bottomCount * POPUP_ROW_HEIGHT;
        h += SEPARATOR_HEIGHT;
        h += RESET_HEIGHT;
        return h;
    }

    private float getPopupElementY(int displayIndex, float popupY) {
        float y = popupY + POPUP_PADDING;
        y += POPUP_ROW_HEIGHT; // size slider
        y += SEPARATOR_HEIGHT;

        if (displayIndex < topRowCount) {
            y += displayIndex * POPUP_ROW_HEIGHT;
        } else {
            y += topRowCount * POPUP_ROW_HEIGHT;
            if (topRowCount > 0 && topRowCount < ELEMENT_COUNT) y += 4.0F; // divider
            y += (displayIndex - topRowCount) * POPUP_ROW_HEIGHT;
        }
        return y;
    }

    // ==================== SETTINGS PERSISTENCE ====================

    private void saveSettings() {
        try {
            SavedSettings saved = new SavedSettings();
            saved.sizeMultiplier = sizeMultiplier;
            saved.elementOrder = elementOrder.clone();
            saved.elementEnabled = elementEnabled.clone();
            saved.topRowCount = topRowCount;
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(saved, w);
            }
        } catch (IOException e) {
            System.err.println("Failed to save watermark config: " + e.getMessage());
        }
    }

    private void loadSettings() {
        if (!Files.exists(CONFIG_FILE)) return;
        try (Reader r = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            SavedSettings saved = GSON.fromJson(r, SavedSettings.class);
            if (saved != null) {
                sizeMultiplier = clamp(saved.sizeMultiplier, 0.5F, 2.0F);
                if (saved.elementOrder != null && saved.elementOrder.length == ELEMENT_COUNT
                        && validateElementOrder(saved.elementOrder)) {
                    elementOrder = saved.elementOrder.clone();
                }
                if (saved.elementEnabled != null && saved.elementEnabled.length == ELEMENT_COUNT) {
                    elementEnabled = saved.elementEnabled.clone();
                }
                topRowCount = Math.max(0, Math.min(saved.topRowCount, ELEMENT_COUNT));
            }
        } catch (Exception e) {
            System.err.println("Failed to load watermark config: " + e.getMessage());
        }
    }

    private void resetSettings() {
        sizeMultiplier = 1.0F;
        elementOrder = new int[]{0, 1, 2, 3, 4};
        elementEnabled = new boolean[]{true, true, true, true, true};
        topRowCount = DEFAULT_TOP_ROW_COUNT;
        saveSettings();
    }

    private static boolean validateElementOrder(int[] order) {
        if (order == null || order.length != ELEMENT_COUNT) return false;
        boolean[] seen = new boolean[ELEMENT_COUNT];
        for (int i : order) {
            if (i < 0 || i >= ELEMENT_COUNT || seen[i]) return false;
            seen[i] = true;
        }
        return true;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int getThemeColor() {
        return ClickGuiModule.getInstance().getColor();
    }

    private record WatermarkState(
            float x,
            float y,
            float radius,
            float topWidth,
            float bottomWidth,
            int topCount,
            int bottomCount,
            String[] texts
    ) {
    }

    private static final class SavedSettings {
        float sizeMultiplier = 1.0f;
        int[] elementOrder = {0, 1, 2, 3, 4};
        boolean[] elementEnabled = {true, true, true, true, true};
        int topRowCount = 2;
    }
}
