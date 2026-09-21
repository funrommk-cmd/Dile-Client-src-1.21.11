package dile.ru.screens.clickgui.impl.panel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.module.ModuleManager;
import dile.ru.api.module.impl.player.AHHelper;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.api.settings.Setting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.manager.Manager;
import dile.ru.screens.clickgui.impl.module.ModuleOption;
import dile.ru.screens.clickgui.impl.module.ModuleOptionFactory;
import dile.ru.screens.clickgui.impl.search.ClickGuiSearchBar;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ClickGuiPanel {
    private static final float HEADER_HEIGHT = 34.0f;
    private static final float ROW_HEIGHT = 30.0f;
    private static final float BOTTOM_PADDING = 7.0f;
    private static final float FIXED_HEIGHT = 280.0f;
    private static final float SCROLL_STEP = 30.0f;

    private final ModuleCategory category;
    private final Map<Module, ModuleOption> options = new IdentityHashMap<>();
    private final SmoothAnimation xAnimation = new SmoothAnimation();
    private final SmoothAnimation scrollAnimation = new SmoothAnimation();
    private float scrollTarget;
    private float scrollLimit;
    private float lastX;
    private float lastY;
    private float lastWidth;
    private float lastHeight;
    private float lastScale = 1.0f;

    public ClickGuiPanel(ModuleCategory category) {
        this.category = category;
    }

    public ModuleCategory category() {
        return category;
    }

    public void snapX(float x) {
        xAnimation.set(x);
    }

    public float animatedX(float targetX) {
        xAnimation.run(targetX, 0.18, Easings.CUBIC_OUT, true);
        xAnimation.update();
        return xAnimation.get();
    }

    public float currentX() {
        return xAnimation.get();
    }

    public void updateAnimations() {
        List<Module> modules = modules();
        for (Module module : modules) {
            ModuleOption option = optionFor(module);
            option.updateSearchMatch();
            option.updateAnimation();
        }
        updateScroll();
    }

    public float height() {
        return FIXED_HEIGHT;
    }

    public float contentHeight() {
        if (category == ModuleCategory.AUTO_BUY) {
            List<Module> modules = modules();
            if (modules.isEmpty()) {
                return 0.0f;
            }
            return optionFor(modules.get(0)).getAnimatedExtraHeight();
        }
        float rowsHeight = 0.0f;
        List<Module> modules = modules();
        for (Module module : modules) {
            ModuleOption option = optionFor(module);
            rowsHeight += ROW_HEIGHT + option.getAnimatedExtraHeight();
        }
        return rowsHeight;
    }

    public void renderShell(GuiGraphics graphics, float x, float y, float width, float height, float scale, float alpha) {
        rememberBounds(x, y, width, height, scale);
        width *= scale;
        height *= scale;

        Render2D.blur(x, y, width, height, 12 * scale, 1, 1.0f, color(0, 0, 0, 255, alpha));

        float contentTop = y + HEADER_HEIGHT * scale;
        float bottom = y + height - BOTTOM_PADDING * scale;
        float rowY = contentTop - scrollAnimation.get() * scale;
        float rowHeight = ROW_HEIGHT * scale;
        List<Module> modules = modules();
        Render2D.pushScissor(graphics, x, contentTop, width, Math.max(0.0f, bottom - contentTop));
        try {
            if (category == ModuleCategory.AUTO_BUY) {
                if (!modules.isEmpty()) {
                    float settingsTop = contentTop - scrollAnimation.get() * scale;
                    optionFor(modules.get(0)).renderSettingsBackground(x, settingsTop, width, scale, textAlpha(alpha), bottom);
                }
            } else {
                for (Module module : modules) {
                    ModuleOption option = optionFor(module);
                    float extraHeight = option.getAnimatedExtraHeight() * scale;
                    float blockBottom = rowY + rowHeight + extraHeight;
                    if (blockBottom > contentTop && rowY < bottom) {
                        option.renderSettingsBackground(x, rowY + rowHeight, width, scale, textAlpha(alpha), bottom);
                    }
                    rowY = blockBottom;
                }
            }
        } finally {
            Render2D.popScissor(graphics);
        }
    }

    public void renderContent(GuiGraphics graphics, float x, float y, float width, float height, float scale, float alpha) {
        rememberBounds(x, y, width, height, scale);
        width *= scale;
        height *= scale;
        List<Module> modules = modules();
        for (Module module : modules) {
            optionFor(module).beginInteractionFrame();
        }

        float textAlpha = textAlpha(alpha);
        if (textAlpha <= 0.0f) {
            return;
        }

        Render2D.rect(x, y, width, HEADER_HEIGHT * scale, 12.0f * scale, 12.0f * scale, 0.0f, 0.0f, color(255, 255, 255, 20, textAlpha));
        int themeColor = getThemeColor();
        Render2D.text(FontType.SEMIBOLD, category.getDisplayName(), x + 11.0f * scale, y + 10.0f * scale, 9.0f, ColorUtil.withAlpha(themeColor, (int) (200 * textAlpha)));
        String categoryIcon = categoryIcon();
        if (!categoryIcon.isEmpty()) {
            float iconSize = 12.0f;
            float iconWidth = Render2D.textWidth(FontType.WILD, categoryIcon, iconSize);
            Render2D.text(FontType.WILD, categoryIcon, x + width - iconWidth - 11.0f * scale, y + 8.0f * scale, iconSize, color(255, 255, 255, 230, textAlpha));
        }

        float contentTop = y + HEADER_HEIGHT * scale;
        float bottom = y + height - BOTTOM_PADDING * scale;
        Render2D.pushScissor(graphics, x, contentTop, width, Math.max(0.0f, bottom - contentTop));
        try {
            if (category == ModuleCategory.AUTO_BUY) {
                if (!modules.isEmpty()) {
                    float settingsTop = contentTop - scrollAnimation.get() * scale;
                    optionFor(modules.get(0)).renderSettingsContent(graphics, x, settingsTop, width, scale, textAlpha, bottom);
                }
            } else {
                float rowY = contentTop - scrollAnimation.get() * scale;
                float rowHeight = ROW_HEIGHT * scale;
                for (Module module : modules) {
                    ModuleOption option = optionFor(module);
                    float extraHeight = option.getAnimatedExtraHeight() * scale;
                    if (rowY + rowHeight > contentTop && rowY < bottom) {
                        option.renderRow(x, rowY, width, ROW_HEIGHT, scale, textAlpha);
                    }
                    if (rowY + rowHeight + extraHeight > contentTop && rowY + rowHeight < bottom) {
                        option.renderSettingsContent(graphics, x, rowY + rowHeight, width, scale, textAlpha, bottom);
                    }
                    rowY += rowHeight + extraHeight;
                }
            }
        } finally {
            Render2D.popScissor(graphics);
        }
    }

    public void renderOverlays(GuiGraphics graphics) {
        List<Module> modules = modules();
        for (Module module : modules) {
            optionFor(module).renderOverlay(graphics);
        }
    }

    public boolean reveal(Module target) {
        if (target == null || target.getCategory() != category) {
            return false;
        }
        optionFor(target).revealSettings();
        return true;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (mouseClickedPopup(event, doubled)) {
            return true;
        }
        return mouseClickedControls(event, doubled);
    }

    public boolean mouseClickedPopup(MouseButtonEvent event, boolean doubled) {
        List<Module> modules = modules();
        for (int i = modules.size() - 1; i >= 0; i--) {
            if (optionFor(modules.get(i)).mouseClickedPopup(event, doubled)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseClickedControls(MouseButtonEvent event, boolean doubled) {
        if (!containsContent(event.x(), event.y())) {
            return false;
        }
        List<Module> modules = modules();
        for (int i = modules.size() - 1; i >= 0; i--) {
            if (optionFor(modules.get(i)).mouseClickedSettings(event, doubled)) {
                return true;
            }
        }
        for (int i = modules.size() - 1; i >= 0; i--) {
            if (optionFor(modules.get(i)).mouseClickedRow(event, doubled)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!containsPanel(mouseX, mouseY)) {
            return false;
        }
        scrollTarget = Math.max(0.0f, scrollTarget - (float) scrollY * SCROLL_STEP);
        scrollAnimation.run(scrollTarget, 0.22, Easings.CUBIC_OUT, true);
        return true;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        List<Module> modules = modules();
        for (int i = modules.size() - 1; i >= 0; i--) {
            if (optionFor(modules.get(i)).mouseReleased(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        List<Module> modules = modules();
        for (int i = modules.size() - 1; i >= 0; i--) {
            if (optionFor(modules.get(i)).mouseDragged(event, dragX, dragY)) {
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        List<Module> modules = modules();
        for (int i = modules.size() - 1; i >= 0; i--) {
            if (optionFor(modules.get(i)).keyPressed(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        List<Module> modules = modules();
        for (int i = modules.size() - 1; i >= 0; i--) {
            if (optionFor(modules.get(i)).charTyped(event)) {
                return true;
            }
        }
        return false;
    }

    public void resetScroll() {
        scrollTarget = 0.0f;
        scrollAnimation.set(0.0);
    }

    public void warmupText() {
        Render2D.warmupText(FontType.SEMIBOLD, category.getDisplayName(), 9.0f);
        String categoryIcon = categoryIcon();
        if (!categoryIcon.isEmpty()) {
            Render2D.warmupText(FontType.WILD, categoryIcon, 12.0f);
        }
        for (Module module : modules()) {
            Render2D.warmupText(FontType.SEMIBOLD, module.getName(), 9.0f);
            for (Setting<?> setting : module.getSettings()) {
                Render2D.warmupText(FontType.SEMIBOLD, setting.getName(), 7.2f);
                warmupSettingValue(setting);
            }
        }
    }

    private List<Module> modules() {
        ModuleManager moduleManager = Manager.getModules();
        if (moduleManager == null) {
            return List.of();
        }
        if (category == ModuleCategory.AUTO_BUY) {
            Module module = moduleManager.getByType(AHHelper.class).orElse(null);
            return module == null ? List.of() : List.of(module);
        }
        List<Module> all = moduleManager.getByCategory(category);
        if (!ClickGuiSearchBar.isFiltering()) {
            return all;
        }
        List<Module> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.comparingInt(m -> optionFor(m).getSearchProgress() > 0.5f ? 0 : 1));
        return sorted;
    }

    private ModuleOption optionFor(Module module) {
        if (category == ModuleCategory.AUTO_BUY) {
            return options.computeIfAbsent(module, ModuleOptionFactory::createSettingsOnly);
        }
        return options.computeIfAbsent(module, ModuleOptionFactory::create);
    }

    private void updateScroll() {
        scrollLimit = Math.max(0.0f, contentHeight() - contentViewportHeight());
        scrollTarget = Math.max(0.0f, scrollTarget);
        scrollAnimation.run(scrollTarget, 0.22, Easings.CUBIC_OUT, true);
        scrollAnimation.update();
    }

    private float contentViewportHeight() {
        return Math.max(1.0f, FIXED_HEIGHT - HEADER_HEIGHT - BOTTOM_PADDING);
    }

    private void rememberBounds(float x, float y, float width, float height, float scale) {
        lastX = x;
        lastY = y;
        lastWidth = width * scale;
        lastHeight = height * scale;
        lastScale = scale;
    }

    private boolean containsPanel(double mouseX, double mouseY) {
        return mouseX >= lastX
                && mouseX <= lastX + lastWidth
                && mouseY >= lastY
                && mouseY <= lastY + lastHeight;
    }

    private boolean containsContent(double mouseX, double mouseY) {
        float top = lastY + HEADER_HEIGHT * lastScale;
        float bottom = lastY + lastHeight - BOTTOM_PADDING * lastScale;
        return mouseX >= lastX
                && mouseX <= lastX + lastWidth
                && mouseY >= top
                && mouseY <= bottom;
    }

    private String categoryIcon() {
        return switch (category) {
            case COMBAT -> "N";
            case MOVEMENT -> "M";
            case VISUAL -> "O";
            case PLAYER -> "P";
            case MISC -> "Q";
            case AUTO_BUY -> "B";
            default -> "";
        };
    }

    private static void warmupSettingValue(Setting<?> setting) {
        if (setting instanceof ModeSetting modeSetting) {
            Render2D.warmupText(FontType.SEMIBOLD, modeSetting.getValue(), 6.5f);
            for (String mode : modeSetting.getModes()) {
                Render2D.warmupText(FontType.SEMIBOLD, mode, 6.4f);
            }
            return;
        }
        if (setting instanceof MultiModeSetting multiModeSetting) {
            Render2D.warmupText(FontType.SEMIBOLD, multiModeSetting.selectedCount() + " selected", 6.5f);
            for (String mode : multiModeSetting.getModes()) {
                Render2D.warmupText(FontType.SEMIBOLD, mode, 6.4f);
            }
            return;
        }
        Object value = setting.getValue();
        if (value != null) {
            Render2D.warmupText(FontType.SEMIBOLD, String.valueOf(value), 6.5f);
        }
    }

    private float outlineShinePhase() {
        int index = category.ordinal();
        long cycle = 2900L + index * 240L;
        float basePhase = (System.currentTimeMillis() % cycle) / (float) cycle;
        float stagger = (index * 0.173f) % 1.0f;
        float phase = (basePhase + stagger) % 1.0f;
        return phase * 0.998f;
    }

    private static float textAlpha(float alpha) {
        if (alpha <= 0.001f) {
            return 0.0f;
        }
        return alpha * alpha * (3.0f - 2.0f * alpha);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int color(int red, int green, int blue, int alpha, float alphaMultiplier) {
        return ColorUtil.rgba(red, green, blue, (int) (alpha * alphaMultiplier));
    }

    private static int getThemeColor() {
        ClickGuiModule mod = ClickGuiModule.getInstance();
        return mod != null ? mod.getThemeColor() : ColorUtil.rgba(100, 200, 100, 255);
    }
}
