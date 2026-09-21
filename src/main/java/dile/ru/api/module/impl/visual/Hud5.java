package dile.ru.api.module.impl.visual;

import dile.ru.api.drag.core.ElementManager;
import dile.ru.api.drag.core.ElementScreen;
import dile.ru.api.drag.core.HudElement;
import dile.ru.api.drag.impl.CelkaArrayList;
import dile.ru.api.drag.impl.CelkaKeyBinds;
import dile.ru.api.drag.impl.CelkaPotions;
import dile.ru.api.drag.impl.CelkaStaff;
import dile.ru.api.drag.impl.CelkaTargetHud;
import dile.ru.api.drag.impl.CelkaWatermark;
import dile.ru.api.drag.impl.HudPanel;
import dile.ru.api.events.impl.DrawEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.LoadingVisualGuard;
import dile.ru.utils.render.item.RenderItem;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;

public class Hud5 extends Module {
    private static final String[] ELEMENTS = {
            "Watermark",
            "KeyBinds",
            "Staff",
            "Potions",
            "TargetHud",
            "ModuleList"
    };

    private static Hud5 instance;

    private final MultiModeSetting elements = register(new MultiModeSetting("Elements", "HUD elements to render.",
            ELEMENTS, ELEMENTS));
    private final BooleanSetting blur = register(new BooleanSetting("Blur", "Use blurred HUD backgrounds.", true));
    private final BooleanSetting glow = register(new BooleanSetting("Glow", "Use glow effects on HUD elements.", true));
    private final NumberSetting offset = register(new NumberSetting("Сдвиг", "Offset of HUD elements from the screen edge.", 5.0, 1.0, 10.0, 1.0));

    private final List<HudElement> elementsList = List.of(
            new CelkaWatermark(),
            new CelkaKeyBinds(),
            new CelkaStaff(),
            new CelkaPotions(),
            new CelkaTargetHud(),
            new CelkaArrayList()
    );

    public Hud5() {
        super("HUD 5", "Renders draggable HUD elements (Celka style).", ModuleCategory.VISUAL);
        instance = this;
    }

    @Override
    protected void onEnable() {
        Hud hud = Hud.getInstance();
        if (hud != null && hud.isEnabled()) {
            hud.setEnabled(false);
        }
        Hud2 hud2 = Hud2.getInstance();
        if (hud2 != null && hud2.isEnabled()) {
            hud2.setEnabled(false);
        }
        Hud3 hud3 = Hud3.getInstance();
        if (hud3 != null && hud3.isEnabled()) {
            hud3.setEnabled(false);
        }
        Hud4 hud4 = Hud4.getInstance();
        if (hud4 != null && hud4.isEnabled()) {
            hud4.setEnabled(false);
        }
    }

    public static Hud5 getInstance() {
        return instance;
    }

    public static boolean isBlurEnabled() {
        Hud5 hud = instance;
        return hud == null || hud.blur.getValue();
    }

    public static boolean isGlowEnabled() {
        Hud5 hud = instance;
        return hud == null || hud.glow.getValue();
    }

    public static int getOffset() {
        Hud5 hud = instance;
        return hud != null ? (int) Math.round(hud.offset.getValue()) : 5;
    }

    public void renderHudLayer(DrawEvent event) {
        if (mc.player == null || mc.level == null || mc.getWindow() == null || LoadingVisualGuard.shouldSuppressHud(mc)) {
            return;
        }

        ElementScreen screen = ElementScreen.current();
        ElementManager elementManager = ElementManager.getInstance();
        elementManager.frame(screen);
        if (event.getLayer() == DrawEvent.Layer.CHAT_OVERLAY) {
            elementManager.updateActiveElementFromMouse();
        }
        RenderItem.beginFrame(event.getGraphics());
        try {
            for (HudElement element : elementsList) {
                boolean selected = elements.isSelected(elementName(element));
                setElementVisible(element, selected);
                if (!selected) continue;
                element.render();
            }
        } finally {
            RenderItem.flush();
        }

        boolean editLayer = event.getLayer() == DrawEvent.Layer.CHAT_OVERLAY && elementManager.canEditCurrentScreen();
        if (editLayer) {
            elementManager.renderEditorOverlay(event.getGraphics(), screen);
        }
    }

    public boolean handleMouseClicked(MouseButtonEvent event, boolean doubled) {
        if (!isEnabled() || event == null) {
            return false;
        }
        if (event.button() != 0 && event.button() != 1) {
            return false;
        }
        for (int i = elementsList.size() - 1; i >= 0; i--) {
            HudElement element = elementsList.get(i);
            if (!elements.isSelected(elementName(element))) continue;
            if (element.mouseClicked(event, doubled)) {
                return true;
            }
        }
        return false;
    }

    public boolean handleMouseReleased(MouseButtonEvent event) {
        if (!isEnabled() || event == null) {
            return false;
        }
        for (int i = elementsList.size() - 1; i >= 0; i--) {
            HudElement element = elementsList.get(i);
            if (!elements.isSelected(elementName(element))) continue;
            if (element.mouseReleased(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean handleMouseDragged(MouseButtonEvent event) {
        if (!isEnabled() || event == null) {
            return false;
        }
        for (int i = elementsList.size() - 1; i >= 0; i--) {
            HudElement element = elementsList.get(i);
            if (!elements.isSelected(elementName(element))) continue;
            if (element.mouseDragged(event)) {
                return true;
            }
        }
        return false;
    }

    private String elementName(HudElement element) {
        if (element instanceof HudPanel panel) {
            return panel.elementName();
        }
        return element.getClass().getSimpleName();
    }

    private void setElementVisible(HudElement element, boolean visible) {
        if (element instanceof HudPanel panel) {
            panel.setHudVisible(visible);
        }
    }
}
