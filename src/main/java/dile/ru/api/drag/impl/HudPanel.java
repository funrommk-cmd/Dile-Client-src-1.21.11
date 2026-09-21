package dile.ru.api.drag.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import dile.ru.api.drag.core.ElementComponent;
import dile.ru.api.drag.core.ElementManager;
import dile.ru.api.drag.core.ElementScreen;
import dile.ru.api.drag.core.HudElement;
import dile.ru.api.settings.bind.KeyBind;
import dile.ru.utils.render.animation.Easings;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class HudPanel implements HudElement {
    protected static final FontType TITLE_FONT = FontType.BOLD;
    protected static final FontType TEXT_FONT = FontType.BOLD;
    protected static final int TEXT_COLOR = ColorUtil.rgba(255, 255, 255, 245);
    protected static final int MUTED_COLOR = ColorUtil.rgba(183, 190, 202, 215);
    protected static int accentColor() {
        return ClickGuiModule.getInstance().getColor(0.902F);
    }
    private static final float CONTENT_ANIM = 0.24F;
    private static final String[] ASCII_CHARS = new String[128];

    static {
        for (int i = 0; i < ASCII_CHARS.length; i++) {
            ASCII_CHARS[i] = Character.toString((char) i);
        }
    }

    protected final Minecraft mc = Minecraft.getInstance();
    protected final ElementComponent drag;
    private final String elementName;
    private float animatedWidth;
    private float animatedHeight;
    private long lastFrameMs = System.currentTimeMillis();
    private final SmoothAnimation contentAnimation = new SmoothAnimation();
    private boolean enabled = true;

    protected HudPanel(String id, String title, float defaultX, float defaultY, float width, float height) {
        this.elementName = title;
        this.drag = ElementManager.getInstance()
                .register("hud." + id, title, defaultX, defaultY)
                .minimumSize(12.0F, 12.0F);
        this.animatedWidth = width;
        this.animatedHeight = height;
    }

    public String elementName() {
        return elementName;
    }

    public ElementComponent getDrag() {
        return drag;
    }

    public void setHudVisible(boolean visible) {
        enabled = visible;
        drag.visible(visible);
    }

    protected boolean selected() {
        return enabled;
    }

    protected void contentVisible(boolean visible) {
        drag.visible(enabled && visible);
    }

    protected float contentAlpha(boolean targetVisible) {
        contentAnimation.update();
        contentAnimation.run(targetVisible ? 1.0 : 0.0, CONTENT_ANIM, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);

        float alpha = contentAnimation.get();
        contentVisible(targetVisible || alpha > 0.01F || contentAnimation.isAlive());
        return alpha;
    }

    protected boolean editPreview() {
        return mc.screen instanceof ChatScreen;
    }

    protected void size(float targetWidth, float targetHeight) {
        float delta = deltaSeconds();
        animatedWidth = smooth(animatedWidth, targetWidth, delta, 8.0F);
        animatedHeight = smooth(animatedHeight, targetHeight, delta, 8.0F);
        if (Math.abs(animatedWidth - targetWidth) < 0.2F) {
            animatedWidth = targetWidth;
        }
        if (Math.abs(animatedHeight - targetHeight) < 0.2F) {
            animatedHeight = targetHeight;
        }
        drag.size((float) Math.ceil(animatedWidth), (float) Math.ceil(animatedHeight));
        drag.clamp(ElementScreen.current());
    }

    protected String trimToWidth(String text, FontType font, float size, float width) {
        if (text == null) {
            return "";
        }
        if (Render2D.textWidth(font, text, size) <= width) {
            return text;
        }
        String suffix = "..";
        if (Render2D.textWidth(font, suffix, size) > width) {
            return suffix;
        }

        int low = 0;
        int high = text.length();
        int best = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            String candidate = text.substring(0, mid) + suffix;
            if (Render2D.textWidth(font, candidate, size) <= width) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return best <= 0 ? suffix : text.substring(0, best) + suffix;
    }

    protected String formatDurationTicks(int ticks) {
        if (ticks < 0) {
            return "inf";
        }
        int totalSeconds = Math.max(0, ticks / 20);
        return totalSeconds / 60 + ":" + twoDigits(totalSeconds % 60);
    }

    protected static String twoDigits(int value) {
        int safe = Math.max(0, Math.min(99, value));
        return ASCII_CHARS['0' + safe / 10] + ASCII_CHARS['0' + safe % 10];
    }

    protected static String timerText(int ticks) {
        if (ticks < 0) {
            return "**:**";
        }
        int totalSeconds = Math.max(0, ticks / 20);
        int minutes = Math.min(99, totalSeconds / 60);
        int seconds = totalSeconds % 60;
        return twoDigits(minutes) + ":" + twoDigits(seconds);
    }

    protected static String charText(char c) {
        return c < ASCII_CHARS.length ? ASCII_CHARS[c] : Character.toString(c);
    }

    protected String shortBind(KeyBind bind) {
        if (bind == null || !bind.isBound()) {
            return "None";
        }
        return bind.getDisplayName()
                .replace("MOUSE ", "M")
                .replace("L SHIFT", "LSH")
                .replace("R SHIFT", "RSH")
                .replace("L CTRL", "LCT")
                .replace("R CTRL", "RCT")
                .replace("SPACE", "SPC");
    }

    protected static float smooth(float current, float target, float deltaSeconds, float speed) {
        float factor = (float) (1.0D - Math.pow(0.001D, Math.max(0.0F, deltaSeconds) * speed));
        return current + (target - current) * factor;
    }

    protected static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float deltaSeconds() {
        long now = System.currentTimeMillis();
        float delta = Math.min(0.1F, Math.max(0.0F, (now - lastFrameMs) / 1000.0F));
        lastFrameMs = now;
        return delta;
    }

    protected record ColoredSegment(String text, int color) {}

    private static final int[] SECTION_COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    protected static List<ColoredSegment> parseComponent(Component component, int defaultColor) {
        List<ColoredSegment> result = new ArrayList<>();
        if (component == null) return result;
        String flatText = component.getString();
        if (flatText.isEmpty()) return result;
        if (flatText.indexOf('\u00a7') >= 0) {
            parseSectionCodes(flatText, defaultColor, result);
            return result;
        }
        component.visit((style, text) -> {
            if (text.isEmpty()) return Optional.empty();
            int color = defaultColor;
            if (style != null && style.getColor() != null) {
                color = style.getColor().getValue();
            }
            result.add(new ColoredSegment(text, color));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    private static void parseSectionCodes(String text, int baseColor, List<ColoredSegment> result) {
        int currentColor = baseColor;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                if (current.length() > 0) {
                    result.add(new ColoredSegment(current.toString(), currentColor));
                    current.setLength(0);
                }
                char code = Character.toLowerCase(text.charAt(i + 1));
                i++;
                int idx = "0123456789abcdef".indexOf(code);
                if (idx >= 0) {
                    currentColor = SECTION_COLORS[idx];
                } else if (code == 'r') {
                    currentColor = baseColor;
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(new ColoredSegment(current.toString(), currentColor));
        }
    }

    protected static float coloredTextWidth(FontType font, List<ColoredSegment> segments, float size) {
        float width = 0;
        for (ColoredSegment seg : segments) {
            if (!seg.text().isEmpty()) {
                width += Render2D.textWidth(font, seg.text(), size);
            }
        }
        return width;
    }

    protected static float renderColoredText(FontType font, List<ColoredSegment> segments, float x, float y, float size, float alpha) {
        float currentX = x;
        for (ColoredSegment seg : segments) {
            if (seg.text().isEmpty()) continue;
            int r = ColorUtil.getRed(seg.color());
            int g = ColorUtil.getGreen(seg.color());
            int b = ColorUtil.getBlue(seg.color());
            int color = ColorUtil.rgba(r, g, b, Math.round(255.0F * alpha));
            Render2D.text(font, seg.text(), currentX, y, size, color);
            currentX += Render2D.textWidth(font, seg.text(), size);
        }
        return currentX;
    }
}
