package dile.ru.screens.clickgui.impl.search;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.util.Locale;

public final class ClickGuiSearchBar {
    private static final float WIDTH = 200.0f;
    private static final float HEIGHT = 21.0f;
    private static final float TOP = 12.0f;
    private static final float OPEN_OFFSET = 18.0f;

    private static String searchQuery = "";

    private final SmoothAnimation focusAnimation = new SmoothAnimation();
    private boolean focused;
    private boolean selectedAll;
    private float x;
    private float y;
    private float width;
    private float height;
    private float scale = 1.0f;

    public ClickGuiSearchBar() {
        focusAnimation.set(0.0);
    }

    public static String getQuery() {
        return searchQuery;
    }

    public static boolean isFiltering() {
        return searchQuery != null && !searchQuery.isBlank();
    }

    public static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }

    public void render(GuiGraphics graphics, int screenWidth, float openProgress, float offsetX, float targetY) {
        focusAnimation.run(focused ? 1.0 : 0.0, 0.18, Easings.CUBIC_OUT, true);
        focusAnimation.update();

        float alpha = clamp(openProgress);
        if (alpha <= 0.01f) {
            return;
        }

        scale = 1.08f - 0.08f * alpha;
        width = WIDTH * scale;
        height = HEIGHT * scale;
        x = (screenWidth - width) / 2.0f + offsetX;
        y = targetY - (1.0f - alpha) * OPEN_OFFSET;

        float radius = 5.5f * scale;
        Render2D.blur(x, y, width, height, radius, 1.0f, 1.0f, color(255, 255, 255, 255, alpha));
        Render2D.blur(x, y, width, height, radius, 1, 1.0f, color(0, 0, 0, 255, alpha));

        boolean placeholderVisible = searchQuery.isEmpty() && !focused;
        String display = placeholderVisible ? "Search..." : searchQuery;
        int textColor = placeholderVisible ? color(255, 255, 255, 255, alpha) : color(255, 255, 255, 245, alpha);
        Render2D.pushScissor(graphics, x + 5.0f * scale, y, width - 25.0f * scale, height);
        try {
            if (selectedAll && focused && !searchQuery.isEmpty()) {
                float selectionX = x + 8.0f * scale;
                float textX = x + 10.0f * scale;
                float textWidth = Render2D.textWidth(FontType.SEMIBOLD, searchQuery, 8.0f * scale);
                float selectionRight = Math.min(textX + textWidth + 4.0f * scale, x + width - 25.0f * scale);
                Render2D.rect(selectionX, y + 3.0f * scale, Math.max(0.0f, selectionRight - selectionX), 13.0f * scale, 2.0f * scale, color(155, 162, 176, 80, alpha));
            }
            Render2D.text(FontType.SEMIBOLD, display, x + 10.0f * scale, y + 6.0f * scale, 8.0f * scale, textColor);
            renderCaret(display, alpha);
        } finally {
            Render2D.popScissor(graphics);
        }

        Render2D.text(FontType.ICONS, "U", x + width - 20.0f * scale, y + 1.5f * scale, 16.0f * scale, color(255, 255, 255, 255, alpha));
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (hovered(mouseX, mouseY, x, y, width, height)) {
            if (event.button() == 0) {
                focused = true;
            }
            return true;
        }

        if (event.button() != 0) {
            return false;
        }

        focused = false;
        selectedAll = false;
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        if (!focused) {
            return false;
        }

        int key = event.key();
        boolean ctrl = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && key == GLFW.GLFW_KEY_A) {
            selectedAll = !searchQuery.isEmpty();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            focused = false;
            selectedAll = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (selectedAll) {
                searchQuery = "";
                selectedAll = false;
            } else if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            searchQuery = "";
            selectedAll = false;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            focused = false;
            selectedAll = false;
            return true;
        }
        return true;
    }

    public boolean charTyped(CharacterEvent event) {
        if (!focused || !event.isAllowedChatCharacter()) {
            return false;
        }
        String text = event.codepointAsString();
        if (text != null && !text.isBlank() && searchQuery.length() < 32) {
            if (selectedAll) {
                searchQuery = "";
                selectedAll = false;
            }
            if (searchQuery.length() + text.length() <= 32) {
                searchQuery += text;
            }
        }
        return true;
    }

    public boolean isFocused() {
        return focused;
    }

    private void renderCaret(String display, float alpha) {
        float focus = (float) focusAnimation.get();
        if (focus <= 0.01f || selectedAll) {
            return;
        }

        long now = System.currentTimeMillis();
        float blink = (float) (0.35 + 0.65 * Math.abs(Math.sin(now / 240.0)));
        float textWidth = searchQuery.isEmpty() ? 0.0f : Render2D.textWidth(FontType.SEMIBOLD, display, 8.0f * scale);
        float maxCaretX = x + width - 25.0f * scale;
        float caretX = Math.min(x + 7.0f * scale + textWidth + 1.5f * scale, maxCaretX);
        Render2D.rect(
                caretX,
                y + 5.5f * scale,
                1.0f * scale,
                11.0f * scale,
                0.5f * scale,
                color(255, 255, 255, 235, alpha * focus * blink)
        );
    }

    private boolean hovered(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private int color(int red, int green, int blue, int alpha, float alphaMultiplier) {
        return ColorUtil.rgba(red, green, blue, (int) (alpha * alphaMultiplier));
    }

    private float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
