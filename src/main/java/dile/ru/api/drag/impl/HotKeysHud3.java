package dile.ru.api.drag.impl;

import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleManager;
import dile.ru.manager.Manager;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ScissorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.Render2DCoordinateSpace;
import dile.ru.utils.render.ui.font.FontType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HotKeysHud3 extends HudPanel {
    private static final float HEADER_H = 15.0F;
    private static final float ITEM_SPACING = 11.0F;
    private static final float PANEL_RADIUS = 5.0F;
    private static final float MIN_WIDTH = 60.0F;
    private static final float NAME_X = 4.5F;
    private static final float NAME_Y_OFFSET = 4.5F;
    private static final float BIND_PAD_X = 6.0F;
    private static final float BIND_PAD_Y = 2.0F;
    private static final float BIND_RADIUS = 2.0F;
    private static final float ICON_SIZE = 10.0F;
    private static final float TITLE_SIZE = 7.0F;
    private static final float ITEM_TEXT_SIZE = 6.0F;
    private static final float BIND_TEXT_SIZE = 6.0F;
    private static final float ANIM_S = 0.3F;

    private static final int BG_COL = ColorUtil.rgba(30, 25, 40, 255);

    private static final Comparator<Module> MODULE_COMPARATOR = Comparator.comparing(Module::getName);

    private final List<RowEntry> rowEntries = new ArrayList<>();
    private final List<Module> activeModules = new ArrayList<>();
    private final SmoothAnimation panelAlpha = new SmoothAnimation();
    private final SmoothAnimation heightAnim = new SmoothAnimation();

    public HotKeysHud3() {
        super("hotkeys3", "HotKeys", 300.0F, 40.0F, 80.0F, 23.0F);
    }

    @Override
    public void render() {
        List<Module> currentActive = getActiveModules();
        activeModules.clear();
        activeModules.addAll(currentActive);

        boolean preview = activeModules.isEmpty() && editPreview();
        boolean targetVisible = !activeModules.isEmpty() || preview;

        panelAlpha.update();
        panelAlpha.run(targetVisible ? 1.0F : 0.0F, ANIM_S, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
        float alpha = panelAlpha.get();

        for (RowEntry entry : rowEntries) {
            entry.active = false;
            entry.alpha.update();
            entry.y.update();
        }

        int targetRows = 0;
        if (preview) {
            RowEntry entry = row("__preview", "Example", "R", targetRows * ITEM_SPACING);
            entry.active = true;
            entry.alpha.run(1.0F, ANIM_S, Easings.EXPO_OUT, true);
            entry.y.run(0.0F, ANIM_S, Easings.EXPO_OUT, true);
            targetRows = 1;
        } else {
            for (Module module : activeModules) {
                String bind = shortBind(module.getBind());
                float targetY = targetRows * ITEM_SPACING;
                RowEntry entry = row(module.getName(), module.getName(), bind, targetY);
                entry.active = true;
                entry.alpha.run(1.0F, ANIM_S, Easings.EXPO_OUT, true);
                entry.y.run(targetY, ANIM_S, Easings.EXPO_OUT, true);
                targetRows++;
            }
        }

        for (RowEntry entry : rowEntries) {
            if (!entry.active) {
                entry.alpha.run(0.0F, ANIM_S, Easings.EXPO_IN, true);
            }
        }
        rowEntries.removeIf(e -> !e.active && e.alpha.get() <= 0.01F && !e.alpha.isAlive());

        float targetHeight = Math.max(20.0F, HEADER_H + Math.max(1, targetRows) * ITEM_SPACING);
        heightAnim.update();
        heightAnim.run(targetHeight, ANIM_S, Easings.CUBIC_OUT, true);
        float animatedH = heightAnim.get();

        float width = MIN_WIDTH;
        for (RowEntry entry : rowEntries) {
            if (entry.alpha.get() > 0.01F || entry.active) {
                float nameW = Render2D.textWidth(TEXT_FONT, entry.name, ITEM_TEXT_SIZE);
                float bindW = Render2D.textWidth(TEXT_FONT, entry.bind, BIND_TEXT_SIZE);
                width = Math.max(width, nameW + bindW + 18.0F + 8.5F);
            }
        }

        size(width, animatedH + 2.3F);

        boolean visible = targetVisible || alpha > 0.01F || !rowEntries.isEmpty();
        contentVisible(visible);
        if (!visible) return;

        int themeCol = ClickGuiModule.getInstance().getColor();
        int titleCol = ColorUtil.withAlpha(themeCol, Math.round(255.0F * alpha));
        int iconCol = ColorUtil.withAlpha(themeCol, Math.round(255.0F * alpha));

        float x = drag.x();
        float y = drag.y();
        float w = drag.width();
        float h = drag.height();

        int blurBg = ColorUtil.rgba(0, 0, 0, Math.round(255.0F * alpha * 0.45F));
        int blurHeader = ColorUtil.rgba(0, 0, 0, Math.round(255.0F * alpha));

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y, w, h + 2.3F)
                .radius(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurBg)
                .build());

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y + HEADER_H, w, h - HEADER_H + 2.3F)
                .radius(0, 0, PANEL_RADIUS, PANEL_RADIUS)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurBg)
                .build());

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y, w, HEADER_H)
                .radius(PANEL_RADIUS, PANEL_RADIUS, 0.0F, 0.0F)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurHeader)
                .build());

        String headerText = "Hotkeys";
        float headerTextW = Render2D.textWidth(TEXT_FONT, headerText, TITLE_SIZE);
        Render2D.text(TEXT_FONT, headerText, x + NAME_X, y + (HEADER_H - TITLE_SIZE) * 0.5F - 0.5F, TITLE_SIZE, titleCol);

        String iconChar = "C";
        float iconWidth = Render2D.textWidth(FontType.ICONS_NURIK, iconChar, ICON_SIZE);
        float iconXPos = x + w - iconWidth - 4.5F;
        float iconYPos = y + (HEADER_H - ICON_SIZE) * 0.5F + 0.1F;
        Render2D.text(FontType.ICONS_NURIK, iconChar, iconXPos, iconYPos, ICON_SIZE, iconCol);

        float rowY = y + HEADER_H;
        ScissorUtil.push(
                Render2DCoordinateSpace.toGuiInt(x),
                Render2DCoordinateSpace.toGuiInt(rowY),
                Render2DCoordinateSpace.toGuiInt(x + w),
                Render2DCoordinateSpace.toGuiInt(y + h)
        );

        for (RowEntry entry : rowEntries) {
            float rowAlpha = alpha * entry.alpha.get();
            if (rowAlpha <= 0.01F) continue;

            float currentY = rowY + entry.y.get();
            float bindW = Render2D.textWidth(TEXT_FONT, entry.bind, BIND_TEXT_SIZE);
            float nameXPos = x + NAME_X;
            float bindXPos = x + w - bindW - 7.5F;
            String visibleName = trimToWidth(entry.name, TEXT_FONT, ITEM_TEXT_SIZE, bindXPos - nameXPos - 5.0F);

            int itemTextCol = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * rowAlpha));
            int bindTextCol = ColorUtil.rgba(255, 255, 255, Math.round(245.0F * rowAlpha));

            Render2D.text(TEXT_FONT, visibleName, nameXPos, currentY + NAME_Y_OFFSET, ITEM_TEXT_SIZE, itemTextCol);
            Render2D.text(TEXT_FONT, entry.bind, bindXPos + BIND_PAD_X, currentY + NAME_Y_OFFSET, BIND_TEXT_SIZE, bindTextCol);
        }

        ScissorUtil.pop();
    }

    private List<Module> getActiveModules() {
        ArrayList<Module> result = new ArrayList<>();
        ModuleManager manager = Manager.getModules();
        if (manager != null) {
            for (Module module : manager.getModules()) {
                if (module.isEnabled() && !module.isHidden() && module.getBind() != null && module.getBind().isBound()) {
                    result.add(module);
                }
            }
        }
        if (result.size() > 1) {
            result.sort(MODULE_COMPARATOR);
        }
        return result;
    }

    private RowEntry row(String key, String name, String bind, float targetY) {
        for (RowEntry entry : rowEntries) {
            if (entry.key.equals(key)) {
                entry.name = name;
                entry.bind = bind;
                return entry;
            }
        }
        RowEntry entry = new RowEntry(key, name, bind);
        entry.alpha.set(0.0);
        entry.y.set(targetY + 4.0F);
        rowEntries.add(entry);
        return entry;
    }

    private static final class RowEntry {
        private final String key;
        private final SmoothAnimation alpha = new SmoothAnimation();
        private final SmoothAnimation y = new SmoothAnimation();
        private String name;
        private String bind;
        private boolean active;

        private RowEntry(String key, String name, String bind) {
            this.key = key;
            this.name = name;
            this.bind = bind;
        }
    }
}
