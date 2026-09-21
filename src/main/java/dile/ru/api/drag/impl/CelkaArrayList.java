package dile.ru.api.drag.impl;

import dile.ru.api.drag.core.ElementScreen;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.module.ModuleManager;
import dile.ru.api.module.impl.visual.Hud5;
import dile.ru.manager.Manager;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class CelkaArrayList extends CelkaHudPanel {
    private static final float PADDING = 4.0F;
    private static final float EDGE_BAR = 2.0F;
    private static final float ELEMENT_HEIGHT = 8.0F;
    private static final float C_WIDTH = 4.0F;

    private final Map<Module, Float> animations = new IdentityHashMap<>();

    public CelkaArrayList() {
        super("modulelist", "ModuleList", 10.0F, 10.0F, 80.0F, 20.0F);
        drag.locked(true);
    }

    @Override
    public void render() {
        List<Module> modules = activeModules();
        if (modules.isEmpty()) {
            contentVisible(false);
            return;
        }
        contentVisible(true);

        float offset = Hud5.getOffset();
        int topColor = style(0);
        int bottomColor = style(90);

        float screenWidth = ElementScreen.current().width();
        float y = offset;
        float maxWidth = 0.0F;
        for (Module module : modules) {
            maxWidth = Math.max(maxWidth, Render2D.textWidth(CELKA_FONT, module.getName(), 7.0F));
        }

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            float target = module.isEnabled() ? 1.0F : 0.0F;
            float animation = animations.getOrDefault(module, 0.0F);
            animation = animation + (target - animation) * 0.16F;
            animations.put(module, animation);
            if (animation < 0.1F) {
                continue;
            }

            String name = module.getName();
            float elementWidth = Render2D.textWidth(CELKA_FONT, name, 7.0F) + PADDING * 2.0F;
            float elementRenderHeight = ELEMENT_HEIGHT * animation;
            float elementX = screenWidth - offset - elementWidth;
            float elementY = y;

            float progress = (float) i / Math.max(1, modules.size());
            int elementColor = ColorUtil.interpolateColor(topColor, bottomColor, progress);

            glow(elementX - C_WIDTH, elementY, elementWidth, elementRenderHeight, 3.0F, elementColor, elementColor);
            Render2D.rect(elementX - C_WIDTH, elementY, elementWidth, elementRenderHeight, elementColor);
            Render2D.rect(elementX - 2.0F + elementWidth - C_WIDTH, elementY, EDGE_BAR, elementRenderHeight, ColorUtil.WHITE);
            Render2D.text(CELKA_FONT, name,
                    elementX + PADDING - C_WIDTH - 1.0F,
                    elementY + (ELEMENT_HEIGHT - 7.0F) / 2.0F,
                    7.0F, ColorUtil.WHITE);

            y += elementRenderHeight;
        }

        float totalHeight = Math.max(0.0F, y - offset);
        float width = maxWidth + PADDING * 2.0F + C_WIDTH;
        drag.position(screenWidth - offset - maxWidth - PADDING * 2.0F - C_WIDTH, offset);
        size(width, totalHeight);
    }

    private List<Module> activeModules() {
        List<Module> result = new ArrayList<>();
        ModuleManager manager = Manager.getModules();
        if (manager == null) {
            return result;
        }
        for (Module module : manager.getModules()) {
            if (module == null || module.isHidden() || module.getCategory() == ModuleCategory.VISUAL) {
                continue;
            }
            float animation = animations.getOrDefault(module, 0.0F);
            if (!module.isEnabled() && animation < 0.1F) {
                continue;
            }
            result.add(module);
        }
        result.sort(Comparator.comparingDouble(m -> -Render2D.textWidth(CELKA_FONT, m.getName(), 7.0F)));
        return result;
    }
}
