package dile.ru.screens.modernui.impl;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.modernfx.Decelerate;
import dile.ru.utils.render.animation.modernfx.Direction;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;
import dile.ru.utils.math.MathUtils;

import java.awt.Color;

public final class LeftPanel {
    private static final float PANEL_WIDTH = 130;
    private static final float PANEL_HEIGHT = 240;
    private static final float PAD = 12;

    private final Decelerate customizationAnim = MathUtils.createAnimation(200, Direction.FORWARDS);

    private boolean draggingSlider;

    public void render(GuiGraphics graphics, float x, float y, int mouseX, int mouseY) {
        ClickGuiModule mod = ClickGuiModule.getInstance();
        float transp = mod != null ? mod.getTransparency() / 100.0f : 1.0f;

        renderBackground(x, y, transp);

        Render2D.text(FontType.SEMIBOLD, "Настройки", x + PAD, y + 14, 7, new Color(238, 241, 247).getRGB());

        float cy = y + 38;
        renderToggle(x + PAD, cy, "Включено", mod != null && mod.isCustomizationEnabled(), mouseX, mouseY);
        cy += 30;

        boolean customizationOn = mod != null && mod.isCustomizationEnabled();
        customizationAnim.setDirection(customizationOn ? Direction.FORWARDS : Direction.BACKWARDS);
        float custProgress = MathUtils.clamp(customizationAnim.getOutput().floatValue(), 0, 1);

        if (custProgress > 0.01f) {
            float alpha = custProgress;

            Render2D.rect(x + PAD, cy, PANEL_WIDTH - PAD * 2, 0.5f, 1, new Color(255, 255, 255, Math.round(30 * alpha)).getRGB());
            cy += 10;

            renderToggle(x + PAD, cy, "Кастом фон", mod != null && mod.isCustomBackgroundEnabled(), mouseX, mouseY);
            cy += 24;

            renderToggle(x + PAD, cy, "Анимация модулей", mod != null && mod.isModuleAnimationEnabled(), mouseX, mouseY);
            cy += 24;

            Render2D.text(FontType.SEMIBOLD, "Прозрачность", x + PAD, cy, 6, new Color(255, 255, 255, Math.round(200 * alpha)).getRGB());
            cy += 16;

            float sliderX = x + PAD;
            float sliderY = cy;
            float sliderWidth = PANEL_WIDTH - PAD * 2;
            float sliderHeight = 4;
            int trans = mod != null ? mod.getTransparency() : 100;
            float prog = (trans - 1) / 99.0f;

            Render2D.rect(sliderX, sliderY, sliderWidth, sliderHeight, 999, new Color(36, 39, 49, Math.round(alpha * 255)).getRGB());
            if (prog > 0.01f) {
                int themeColor = mod != null ? mod.getThemeColor() : new Color(130, 100, 200).getRGB();
                Render2D.rect(sliderX, sliderY, sliderWidth * prog, sliderHeight, 999, multiplyAlpha(themeColor, alpha));
            }
            float knobX = sliderX + sliderWidth * prog;
            float knobY = sliderY + sliderHeight / 2;
            Render2D.rect(knobX - 5, knobY - 5, 10, 10, 999, new Color(246, 247, 255, Math.round(alpha * 255)).getRGB());
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, float x, float y) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        ClickGuiModule mod = ClickGuiModule.getInstance();
        if (mod == null) return false;

        float mx = (float) event.x();
        float my = (float) event.y();

        float cy = y + 38;
        if (toggleHit(mx, my, x + PAD, cy)) {
            mod.setCustomizationEnabled(!mod.isCustomizationEnabled());
            return true;
        }
        cy += 30;

        if (!mod.isCustomizationEnabled()) return false;
        float custProgress = MathUtils.clamp(customizationAnim.getOutput().floatValue(), 0, 1);
        if (custProgress <= 0.01f) return false;

        cy += 10;

        if (toggleHit(mx, my, x + PAD, cy)) {
            mod.setCustomBackgroundEnabled(!mod.isCustomBackgroundEnabled());
            return true;
        }
        cy += 24;

        if (toggleHit(mx, my, x + PAD, cy)) {
            mod.setModuleAnimationEnabled(!mod.isModuleAnimationEnabled());
            return true;
        }
        cy += 24;

        cy += 16;
        float sliderX = x + PAD;
        float sliderY = cy;
        float sliderWidth = PANEL_WIDTH - PAD * 2;
        if (mx >= sliderX && mx <= sliderX + sliderWidth && my >= sliderY - 8 && my <= sliderY + 12) {
            draggingSlider = true;
            updateSlider(mx, sliderX, sliderWidth, mod);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingSlider) {
            draggingSlider = false;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY, float x, float y) {
        if (!draggingSlider) return false;
        ClickGuiModule mod = ClickGuiModule.getInstance();
        if (mod == null) return false;
        float sliderX = x + PAD;
        float sliderWidth = PANEL_WIDTH - PAD * 2;
        updateSlider((float) event.x(), sliderX, sliderWidth, mod);
        return true;
    }

    private void updateSlider(float mouseX, float sliderX, float sliderWidth, ClickGuiModule mod) {
        float prog = MathUtils.clamp((mouseX - sliderX) / sliderWidth, 0, 1);
        int value = Math.round(1 + prog * 99);
        mod.setTransparency(value);
    }

    private void renderBackground(float x, float y, float transp) {
        Render2D.blur(x, y - 0.5f, PANEL_WIDTH, PANEL_HEIGHT, 11, 50.0F, 1.5F, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF);
        Render2D.liquidGlass(x, y - 0.5f, PANEL_WIDTH, PANEL_HEIGHT, 2.0f, 0.075f, 11, new Color(255, 255, 255, Math.round(255 * transp)).getRGB());
        Render2D.rect(x, y - 0.5f, PANEL_WIDTH, PANEL_HEIGHT, 11, new Color(0, 0, 0, Math.round(75 * transp)).getRGB());
        Render2D.outline(x, y - 0.5f, PANEL_WIDTH, PANEL_HEIGHT, 11, 0.25f, new Color(255, 255, 255, Math.round(50 * transp)).getRGB());
    }

    private void renderToggle(float tx, float ty, String label, boolean enabled, int mouseX, int mouseY) {
        ClickGuiModule mod = ClickGuiModule.getInstance();
        float toggleX = tx + 86.2f;
        float toggleY = ty + 2;
        float toggleWidth = 28;
        float toggleHeight = 14;

        boolean hovered = mouseX >= toggleX && mouseX <= toggleX + toggleWidth && mouseY >= toggleY && mouseY <= toggleY + toggleHeight;

        int bg = enabled ? new Color(130, 100, 200, 200).getRGB() : new Color(185, 185, 185, 50).getRGB();
        if (hovered && !enabled) bg = new Color(185, 185, 185, 80).getRGB();

        Render2D.rect(toggleX, toggleY, toggleWidth, toggleHeight, 7, bg);
        Render2D.outline(toggleX, toggleY, toggleWidth, toggleHeight, 7, 0.25f, new Color(255, 255, 255, 100).getRGB());

        float knobX = enabled ? toggleX + toggleWidth - 12 : toggleX + 2;
        Render2D.rect(knobX, toggleY + 2, 10, 10, 5, new Color(255, 255, 255, 255).getRGB());

        Render2D.text(FontType.SEMIBOLD, label, tx, ty + 3, 6, new Color(216, 220, 228).getRGB());
    }

    private boolean toggleHit(double mx, double my, float tx, float ty) {
        float toggleX = tx + 86.2f;
        float toggleY = ty + 2;
        return mx >= toggleX && mx <= toggleX + 28 && my >= toggleY && my <= toggleY + 14;
    }

    private int multiplyAlpha(int color, float alpha) {
        int a = Math.round(((color >>> 24) & 0xFF) * MathUtils.clamp(alpha, 0, 1));
        return (a << 24) | (color & 0x00FFFFFF);
    }
}
