package dile.ru.api.drag.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.module.ModuleManager;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.manager.Manager;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ScissorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.Render2DCoordinateSpace;
import dile.ru.utils.render.ui.font.FontType;

public final class ModuleListHud extends HudPanel {
    private static final float ROW_HEIGHT = 12.0F;
    private static final float PADDING = 4.0F;
    private static final float PANEL_ANIM = 0.24F;
    private static final float ROW_ANIM = 0.22F;
    private static final float BORDER_THICKNESS = 1.0F;

    private final Map<Module, SmoothAnimation[]> moduleAnims = new HashMap<>();
    private final Map<Module, Float> moduleWidths = new HashMap<>();
    private final List<Module> visibleModules = new ArrayList<>();
    private final List<Module> sortedModules = new ArrayList<>();
    private final List<Module> modulesToRemove = new ArrayList<>();
    private final Map<Module, Boolean> moduleVisibility = new HashMap<>();
    private final SmoothAnimation panelAnimation = new SmoothAnimation();
    private int visibilityHash;
    private boolean visibilityDirty = true;
    private boolean lastLeftSide = true;

    private final boolean[] categoryEnabled = {true, true, true, true, true, true};
    private static final ModuleCategory[] CATEGORIES = {
            ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.VISUAL,
            ModuleCategory.PLAYER, ModuleCategory.MISC, ModuleCategory.AUTO_BUY
    };

    public ModuleListHud() {
        super("modulelist", "ModuleList", 10.0F, 70.0F, 90.0F, 50.0F);
    }

    @Override
    public void render() {
        ModuleListState state = logics();
        if (state == null) {
            return;
        }
        renderList(state);
    }

    private ModuleListState logics() {
        updateVisibility();

        if (sortedModules.isEmpty()) {
            sizeInstant(90.0F, ROW_HEIGHT);
            contentVisible(false);
            return null;
        }

        float dragX = drag.x();
        float dragY = drag.y();
        float screenWidth = mc.getWindow().getGuiScaledWidth();
        boolean leftSide = dragX < screenWidth / 2.0F;

        float targetY = 0.0F;
        for (Module module : visibleModules) {
            SmoothAnimation[] anims = moduleAnims.computeIfAbsent(module, k -> new SmoothAnimation[]{
                    new SmoothAnimation(), new SmoothAnimation()
            });
            anims[0].run(0.0F, ROW_ANIM, Easings.EXPO_OUT, true);
            anims[1].run(targetY, ROW_ANIM, Easings.EXPO_OUT, true);
            targetY += ROW_HEIGHT;
        }

        modulesToRemove.clear();
        for (Map.Entry<Module, SmoothAnimation[]> entry : moduleAnims.entrySet()) {
            if (moduleVisibility.getOrDefault(entry.getKey(), false)) continue;
            entry.getValue()[0].run(-15.0F, ROW_ANIM, Easings.EXPO_IN, true);
            if (entry.getValue()[0].get() > -14.0F) continue;
            modulesToRemove.add(entry.getKey());
        }
        for (Module m : modulesToRemove) {
            moduleAnims.remove(m);
        }

        sortedModules.clear();
        sortedModules.addAll(moduleAnims.keySet());
        sortedModules.sort(Comparator.comparingDouble(m -> moduleAnims.get(m)[1].get()));

        float maxWidth = 0.0F;
        for (Module module : sortedModules) {
            float w = getModuleWidth(module);
            if (w > maxWidth) maxWidth = w;
        }

        dragY = Math.max(3.0F, dragY);
        float totalHeight = sortedModules.size() * ROW_HEIGHT;
        sizeInstant(maxWidth, totalHeight);

        boolean visible = !sortedModules.isEmpty();
        contentVisible(visible);
        panelAnimation.update();
        panelAnimation.run(visible ? 1.0 : 0.0, PANEL_ANIM, visible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
        float alpha = panelAnimation.get();
        if (alpha <= 0.01F && !panelAnimation.isAlive()) {
            return null;
        }

        lastLeftSide = leftSide;
        return new ModuleListState(sortedModules, leftSide, maxWidth, alpha, drag.x(), drag.y());
    }

    private void renderList(ModuleListState state) {
        int themeColor = ClickGuiModule.getInstance().getColor();
        int textColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * state.alpha));
        int bgColor = ColorUtil.rgba(30, 25, 40, Math.round(220.0F * state.alpha));
        int outlineColor = ColorUtil.rgba(30, 25, 40, Math.round(120.0F * state.alpha));
        int last = state.modules.size() - 1;

        ScissorUtil.push(
                Render2DCoordinateSpace.toGuiInt(state.x),
                Render2DCoordinateSpace.toGuiInt(state.y),
                Render2DCoordinateSpace.toGuiInt(state.x + state.width),
                Render2DCoordinateSpace.toGuiInt(state.y + state.modules.size() * ROW_HEIGHT)
        );

        for (int i = 0; i < state.modules.size(); i++) {
            Module module = state.modules.get(i);
            SmoothAnimation[] anims = moduleAnims.get(module);
            if (anims == null) continue;

            float slide = anims[0].get();
            float y = state.y + anims[1].get();
            float width = getModuleWidth(module);

            float x = state.leftSide ? state.x + slide : state.x + state.width - width - slide;

            float drawY = y - 1.0F;
            float drawH = ROW_HEIGHT + 2.0F;

            boolean isFirst = i == 0;
            boolean isLast = i == last;
            float topLeft = isFirst ? 3.0F : 0.0F;
            float topRight = isFirst ? 3.0F : 0.0F;
            float bottomLeft = isLast ? 3.0F : 0.0F;
            float bottomRight = isLast ? 3.0F : 3.0F;

            Render2D.rect(x, drawY, width, drawH, topLeft, topRight, bottomRight, bottomLeft, bgColor);

            Render2D.outline(x, drawY, width, drawH,
                    topLeft, topRight, bottomRight, bottomLeft,
                    BORDER_THICKNESS, outlineColor, outlineColor, outlineColor, outlineColor);

            int perRowText = ColorUtil.lerpColor(textColor, themeColor, 0.15F);
            Render2D.text(FontType.SEMIBOLD, module.getName(), x + PADDING, y + ROW_HEIGHT / 2.0F - 3.0F, 6.0F, perRowText);
        }

        ScissorUtil.pop();
    }

    private void updateVisibility() {
        int hash = 1;
        ModuleManager manager = Manager.getModules();
        if (manager == null) return;
        List<Module> modules = new ArrayList<>(manager.getModules());

        for (Module module : modules) {
            if (module == null) continue;
            boolean visible = module.isEnabled() && !module.isHidden() && isCategoryVisible(module.getCategory());
            hash = 31 * hash + System.identityHashCode(module);
            hash = 31 * hash + (visible ? 1 : 0);
            Boolean lastVisible = moduleVisibility.put(module, visible);
            if (lastVisible != null && lastVisible == visible) continue;
            visibilityDirty = true;
        }
        if (!visibilityDirty && hash == visibilityHash) return;

        visibleModules.clear();
        for (Module module : modules) {
            if (module == null || !moduleVisibility.getOrDefault(module, false)) continue;
            visibleModules.add(module);
            getModuleWidth(module);
        }
        visibleModules.sort(Comparator.comparingDouble(m -> -getModuleWidth(m)));
        visibilityHash = hash;
        visibilityDirty = false;
    }

    private boolean isCategoryVisible(ModuleCategory category) {
        if (category == null) return true;
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i] == category) return categoryEnabled[i];
        }
        return true;
    }

    private float getModuleWidth(Module module) {
        return moduleWidths.computeIfAbsent(module, m -> Render2D.textWidth(FontType.SEMIBOLD, m.getName(), 6.0F) + PADDING * 2.0F);
    }

    private void sizeInstant(float width, float height) {
        drag.size((float) Math.ceil(width), (float) Math.ceil(height));
        drag.clamp(dile.ru.api.drag.core.ElementScreen.current());
    }

    private record ModuleListState(List<Module> modules, boolean leftSide, float width, float alpha, float x, float y) {
    }
}
