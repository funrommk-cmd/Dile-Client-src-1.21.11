package dile.ru.api.drag.impl;

import net.minecraft.client.input.MouseButtonEvent;
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

import static dile.ru.IMinecraft.mc;

public final class SimpleWatermark implements HudElement {
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

    private static final int ELEMENT_COUNT = 4;
    private static final String[] ELEMENT_ICONS = {"f", "h", "k", "g"};
    private static final FontType[] ELEMENT_FONTS = {FontType.HUD2, FontType.HUD2, FontType.HUD2, FontType.HUD2};
    private static final float[] ICON_SIZES = {11.0f, 9.0f, 10.0f, 8.5f};
    private static final float[] ICON_Y_OFFSETS = {4.7f, 5.6f, 3.9f, 4.2f};
    private static final float[] GLOW_X_OFFSETS = {2.9f, 3.4f, 0.0f, 3.8f};
    private static final float[] GLOW_Y_OFFSETS = {1.2f, 0.0f, 0.0f, 1.2f};
    private static final int DEFAULT_TOP_ROW_COUNT = 2;

    private final ElementComponent drag = ElementManager.getInstance()
            .register("hud2.watermark", "Watermark", 10.0f, 10.0f)
            .minimumSize(112.0f, 28.0f);

    private final SmoothAnimation topWidthAnimation = new SmoothAnimation();
    private final SmoothAnimation bottomWidthAnimation = new SmoothAnimation();
    private final SmoothAnimation totalWidthAnimation = new SmoothAnimation();

    private float sizeMultiplier = 1.0F;
    private int[] elementOrder = {0, 1, 2, 3};
    private boolean[] elementEnabled = {true, true, true, true};
    private int topRowCount = DEFAULT_TOP_ROW_COUNT;

    public SimpleWatermark() {
        topWidthAnimation.set(160.0f);
        bottomWidthAnimation.set(154.0f);
        totalWidthAnimation.set(182.0f);
    }

    public String elementName() {
        return "Watermark";
    }

    public void setHudVisible(boolean visible) {
        drag.visible(visible);
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

    @Override
    public void render() {
        WatermarkState state = logics();
        renderWatermark(state);
    }

    private WatermarkState logics() {
        int ping = getPing();
        String username = resolveDisplayUsername();
        String fpsText = "Fps " + mc.getFps();
        String pingText = "Ping " + ping;
        String bpsText = "Bps " + (int) getBps();

        String[] texts = {username, fpsText, bpsText, pingText};

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

            if (dile.ru.api.module.impl.visual.Hud2.accentEnabled()) {
                float lineW = 14f;
                float lineH = 3.5f;
                Render2D.rect(state.x + ICON_BOX_SIZE + ROW_GAP + state.topWidth - lineW - 5f, state.y - 0.5f, lineW, lineH, 1.5f, themeColor);
            }

            float topX = state.x + ICON_BOX_SIZE + ROW_GAP;
            for (int i = 0; i < topRowCount; i++) {
                int elIdx = elementOrder[i];
                if (!elementEnabled[elIdx]) continue;
                topX += renderTopItem(topX, state.y, ELEMENT_ICONS[elIdx], ELEMENT_FONTS[elIdx], ICON_SIZES[elIdx], ICON_Y_OFFSETS[elIdx],
                        state.texts[elIdx], purple, white, glow, elIdx);
            }
        }

        if (state.bottomCount > 0) {
            float bottomY = state.y + (state.topCount > 0 ? ROW_HEIGHT + ROW_GAP : 0);
            HudRenderCompat.background(state.x, bottomY, state.bottomWidth, ROW_HEIGHT, state.radius, 15.0f, 1.2f, background);
            float bottomX = state.x;
            for (int i = topRowCount; i < ELEMENT_COUNT; i++) {
                int elIdx = elementOrder[i];
                if (!elementEnabled[elIdx]) continue;
                renderBottomItem(bottomX, bottomY, ELEMENT_ICONS[elIdx], ELEMENT_FONTS[elIdx], ICON_SIZES[elIdx], ICON_Y_OFFSETS[elIdx],
                        state.texts[elIdx], purple, white, glow, elIdx);
                bottomX += bottomSegmentWidth(Render2D.textWidth(FontType.BOLD, state.texts[elIdx], TEXT_SIZE));
            }
        }
    }

    private float renderTopItem(float x, float y, String icon, FontType font, float iconSize, float iconYOffset, String text, int iconColor, int textColor, int glowColor, int elIdx) {
        HudRenderCompat.glow("dile:textures/particles/ghost-glow.png", x - 1.0f + GLOW_X_OFFSETS[elIdx], y + GLOW_Y_OFFSETS[elIdx], 22.0f, 22.0f, 0.0f, glowColor);
        Render2D.text(font, icon, x + 6.5f, y + iconYOffset, iconSize, iconColor);
        Render2D.rect(x + TOP_DOT_OFFSET, y + 9.0f, 4.0f, 4.0f, 4.0f, textColor);
        Render2D.text(FontType.BOLD, text, x + TOP_TEXT_OFFSET, y + 7.0f, TEXT_SIZE, textColor);
        return topSegmentWidth(text);
    }

    private void renderBottomItem(float x, float y, String icon, FontType font, float iconSize, float iconYOffset, String text, int iconColor, int textColor, int glowColor, int elIdx) {
        HudRenderCompat.glow("dile:textures/particles/ghost-glow.png", x - 1.5f + GLOW_X_OFFSETS[elIdx], y - 1.0f + GLOW_Y_OFFSETS[elIdx], 22.0f, 22.0f, 0.0f, glowColor);
        Render2D.text(font, icon, x + 6.5f, y + iconYOffset, iconSize, iconColor);
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

    private float getBps() {
        if (mc.player == null) return 0;
        double dx = mc.player.getDeltaMovement().x;
        double dz = mc.player.getDeltaMovement().z;
        return (float) (Math.sqrt(dx * dx + dz * dz) * 20.0);
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
}
