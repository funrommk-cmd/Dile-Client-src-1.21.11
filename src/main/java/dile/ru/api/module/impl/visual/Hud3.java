package dile.ru.api.module.impl.visual;

import dile.ru.api.drag.core.ElementManager;
import dile.ru.api.drag.core.ElementScreen;
import dile.ru.api.drag.core.HudElement;
import dile.ru.api.drag.impl.ArmorHud3;
import dile.ru.api.drag.impl.ArrowsHud3;
import dile.ru.api.drag.impl.CooldownsHud3;
import dile.ru.api.drag.impl.EventsScheduleHud;
import dile.ru.api.drag.impl.HotKeysHud3;
import dile.ru.api.drag.impl.HudPanel;
import dile.ru.api.drag.impl.InventoryHud3;
import dile.ru.api.drag.impl.MediaPlayerHud3;
import dile.ru.api.drag.impl.ModuleListHud;
import dile.ru.api.drag.impl.NotificationsHud3;
import dile.ru.api.drag.impl.PotionsHud3;
import dile.ru.api.drag.impl.StaffHud3;
import dile.ru.api.drag.impl.TargetHud3;
import dile.ru.api.drag.impl.TargetHudQuick;
import dile.ru.api.drag.impl.TargetHudSadnes;
import dile.ru.api.drag.impl.TotemCounterHud;
import dile.ru.api.drag.impl.WatermarkHud3;
import dile.ru.api.events.impl.DrawEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.utils.render.LoadingVisualGuard;
import dile.ru.utils.render.item.RenderItem;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;

public class Hud3 extends Module {
    private static final String[] ELEMENTS = {
            "Armor",
            "TotemCounter",
            "TargetHud",
            "Watermark3",
            "Staff",
            "Notifications",
            "Cooldowns",
            "Potions",
            "Inventory",
            "HotKeys",
            "ModuleList",
            "MediaPlayer",
            "EventsSchedule",
            "Arrows"
    };

    private static Hud3 instance;

    private final MultiModeSetting elements = register(new MultiModeSetting("Elements", "HUD elements to render.",
            ELEMENTS, ELEMENTS));

    private final ModeSetting targetStyle = register(new ModeSetting("Target Style", "Стиль таргет худа.", "Обычный", "Обычный", "Quick", "Sadnes"));

    private final TargetHudQuick targetHudQuick = new TargetHudQuick();
    private final TargetHud3 targetHud3 = new TargetHud3();
    private final TargetHudSadnes targetHudSadnes = new TargetHudSadnes();

    private final List<HudElement> elementsList = List.of(
            new ArmorHud3(),
            new TotemCounterHud(),
            targetHud3,
            new WatermarkHud3(),
            new StaffHud3(),
            new NotificationsHud3(),
            new CooldownsHud3(),
            new PotionsHud3(),
            new InventoryHud3(),
            new HotKeysHud3(),
            new ModuleListHud(),
            new MediaPlayerHud3(),
            new EventsScheduleHud(),
            new ArrowsHud3()
    );

    public Hud3() {
        super("HUD 3", "Renders draggable HUD elements (HUD Pouch Old style).", ModuleCategory.VISUAL);
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
        Hud4 hud4 = Hud4.getInstance();
        if (hud4 != null && hud4.isEnabled()) {
            hud4.setEnabled(false);
        }
        Hud5 hud5 = Hud5.getInstance();
        if (hud5 != null && hud5.isEnabled()) {
            hud5.setEnabled(false);
        }
    }

    public static Hud3 getInstance() {
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
                if (!selected) continue;
                if (element == targetHud3) {
                    targetStyleElement().render();
                } else {
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
            HudElement active = (element == targetHud3) ? targetStyleElement() : element;
            if (active.mouseClicked(event, doubled)) {
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
            HudElement active = (element == targetHud3) ? targetStyleElement() : element;
            if (active.mouseReleased(event)) {
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
            HudElement active = (element == targetHud3) ? targetStyleElement() : element;
            if (active.mouseDragged(event)) {
                return true;
            }
        }
        return false;
    }

    private String elementName(HudElement element) {
        if (element instanceof HudPanel panel) {
            return panel.elementName();
        }
        if (element instanceof WatermarkHud3 watermark) {
            return watermark.elementName();
        }
        return element.getClass().getSimpleName();
    }

    private HudElement targetStyleElement() {
        if (targetStyle.is("Quick")) {
            return targetHudQuick;
        }
        if (targetStyle.is("Sadnes")) {
            return targetHudSadnes;
        }
        return targetHud3;
    }

    private void setElementVisible(HudElement element, boolean visible) {
        if (element instanceof HudPanel panel) {
            panel.setHudVisible(visible);
            return;
        }
        if (element instanceof WatermarkHud3 watermark) {
            watermark.setHudVisible(visible);
        }
    }
}
