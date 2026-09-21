package dile.ru.api.module.impl.visual;

import dile.ru.api.drag.core.ElementManager;
import dile.ru.api.drag.core.ElementScreen;
import dile.ru.api.drag.core.HudElement;
import dile.ru.api.drag.impl.AnimationShowcase;
import dile.ru.api.drag.impl.Armor;
import dile.ru.api.drag.impl.Binds;
import dile.ru.api.drag.impl.CooldownsHud2;
import dile.ru.api.drag.impl.Hotbar;
import dile.ru.api.drag.impl.HotbarHud2;
import dile.ru.api.drag.impl.HotKeysHud2;
import dile.ru.api.drag.impl.HudPanel;
import dile.ru.api.drag.impl.Inventory;
import dile.ru.api.drag.impl.Notifications;
import dile.ru.api.drag.impl.PotionsHud2;
import dile.ru.api.drag.impl.SimpleWatermark;
import dile.ru.api.drag.impl.StaffHud2;
import dile.ru.api.drag.impl.TargetHud2;
import dile.ru.api.drag.impl.TargetHudQuick;
import dile.ru.api.drag.impl.TargetHudSadnes;
import dile.ru.api.events.impl.DrawEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.utils.render.LoadingVisualGuard;
import dile.ru.utils.render.item.RenderItem;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;

public class Hud2 extends Module {
    private static final String[] ELEMENTS = {
            "Watermark",
            "Hotbar",
            "Armor",
            "HotKeys",
            "Binds",
            "Notifications",
            "Potions",
            "Cooldowns",
            "TargetHud",
            "Staff",
            "Inventory",
    };

    private static Hud2 instance;

    private final MultiModeSetting elements = register(new MultiModeSetting("Elements", "HUD elements to render.",
            ELEMENTS, ELEMENTS));

    private final BooleanSetting accentLines = register(new BooleanSetting("Полоски", "Accent strips in element corners.", true));
    private final ModeSetting targetStyle = register(new ModeSetting("Target Style", "Стиль таргет худа.", "Обычный", "Обычный", "Quick", "Sadnes"));

    private final TargetHudQuick targetHudQuick = new TargetHudQuick();
    private final TargetHud2 targetHud2 = new TargetHud2();
    private final TargetHudSadnes targetHudSadnes = new TargetHudSadnes();

    private final List<HudElement> elementsList = List.of(
            new SimpleWatermark(),
            new HotbarHud2(),
            new Armor(),
            new HotKeysHud2(),
            new Binds(),
            new Notifications(),
            new PotionsHud2(),
            new CooldownsHud2(),
            targetHud2,
            new StaffHud2(),
            new Inventory(),
            new AnimationShowcase()
    );

    public Hud2() {
        super("HUD 2", "Renders draggable HUD elements (simplified).", ModuleCategory.VISUAL);
        instance = this;
    }

    @Override
    protected void onEnable() {
        Hud hud = Hud.getInstance();
        if (hud != null && hud.isEnabled()) {
            hud.setEnabled(false);
        }
        Hud3 hud3 = Hud3.getInstance();
        if (hud3 != null && hud3.isEnabled()) {
            hud3.setEnabled(false);
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

    public static Hud2 getInstance() {
        return instance;
    }

    public static boolean accentEnabled() {
        return instance != null && instance.accentLines.getValue();
    }

    public static boolean shouldRenderCustomHotbar() {
        Hud2 hud = instance;
        return hud != null && hud.isEnabled() && hud.elements.isSelected("Hotbar");
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
                if (element == targetHud2) {
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
            HudElement active = (element == targetHud2) ? targetStyleElement() : element;
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
            HudElement active = (element == targetHud2) ? targetStyleElement() : element;
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
            HudElement active = (element == targetHud2) ? targetStyleElement() : element;
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
        if (element instanceof SimpleWatermark watermark) {
            return watermark.elementName();
        }
        if (element instanceof Hotbar hotbar) {
            return hotbar.elementName();
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
        return targetHud2;
    }

    private void setElementVisible(HudElement element, boolean visible) {
        if (element instanceof HudPanel panel) {
            panel.setHudVisible(visible);
            return;
        }
        if (element instanceof SimpleWatermark watermark) {
            watermark.setHudVisible(visible);
            return;
        }
        if (element instanceof Hotbar hotbar) {
            hotbar.setHudVisible(visible);
        }
    }
}
