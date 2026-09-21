package dile.ru.api.module.impl.visual;

import dile.ru.api.drag.core.ElementManager;
import dile.ru.api.drag.core.ElementScreen;
import dile.ru.api.drag.core.HudElement;
import dile.ru.api.drag.impl.Armor4;
import dile.ru.api.drag.impl.Bps4;
import dile.ru.api.drag.impl.Coords4;
import dile.ru.api.drag.impl.Effects4;
import dile.ru.api.drag.impl.HudPanel;
import dile.ru.api.drag.impl.KeyBinds4;
import dile.ru.api.drag.impl.Logo4;
import dile.ru.api.drag.impl.StaffActive4;
import dile.ru.api.drag.impl.TargetHud4;
import dile.ru.api.drag.impl.TimerHud4;
import dile.ru.api.events.impl.DrawEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.utils.render.LoadingVisualGuard;
import dile.ru.utils.render.item.RenderItem;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;

public class Hud4 extends Module {
    private static final String[] ELEMENTS = {
            "Active Target",
            "Timer Indicator",
            "Logo",
            "Coordinates",
            "Speed",
            "Effects",
            "Active Binds",
            "Active Staff",
            "Armor"
    };

    private static Hud4 instance;

    private final MultiModeSetting elements = register(new MultiModeSetting("Elements", "HUD elements to render.",
            ELEMENTS, ELEMENTS));

    private final List<HudElement> elementsList = List.of(
            new TargetHud4(),
            new TimerHud4(),
            new Logo4(),
            new Coords4(),
            new Bps4(),
            new Effects4(),
            new KeyBinds4(),
            new StaffActive4(),
            new Armor4()
    );

    public Hud4() {
        super("HUD 4", "Renders draggable HUD elements (old client style).", ModuleCategory.VISUAL);
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
        Hud5 hud5 = Hud5.getInstance();
        if (hud5 != null && hud5.isEnabled()) {
            hud5.setEnabled(false);
        }
    }

    public static Hud4 getInstance() {
        return instance;
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
                if (selected) {
                    element.render();
                }
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
