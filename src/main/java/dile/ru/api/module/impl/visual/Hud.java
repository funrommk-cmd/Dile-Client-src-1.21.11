package dile.ru.api.module.impl.visual;

import dile.ru.api.drag.core.ElementManager;
import dile.ru.api.drag.core.ElementScreen;
import dile.ru.api.drag.core.HudElement;
import dile.ru.api.drag.impl.AnimationShowcase;
import dile.ru.api.drag.impl.Armor;
import dile.ru.api.drag.impl.Binds;
import dile.ru.api.drag.impl.Cooldowns;
import dile.ru.api.drag.impl.Hotbar;
import dile.ru.api.drag.impl.HotKeys;
import dile.ru.api.drag.impl.HudPanel;
import dile.ru.api.drag.impl.Inventory;
import dile.ru.api.drag.impl.MediaPlayer;
import dile.ru.api.drag.impl.MoveScoreBoard;
import dile.ru.api.drag.impl.Notifications;
import dile.ru.api.drag.impl.Potions;
import dile.ru.api.drag.impl.ScoreBoardHud;
import dile.ru.api.drag.impl.Staff;
import dile.ru.api.drag.impl.TargetHud;
import dile.ru.api.drag.impl.TargetHudQuick;
import dile.ru.api.drag.impl.TargetHudSadnes;
import dile.ru.api.drag.impl.Watermark;
import dile.ru.api.drag.impl.WatermarkIcons;
import dile.ru.api.events.impl.DrawEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.utils.render.LoadingVisualGuard;
import dile.ru.utils.render.item.RenderItem;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.blur.BuiltBlur;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.List;

public class Hud extends Module {
    private static final float NO_BLUR_RECT_ALPHA_SCALE = 200.0F / 255.0F;

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
            "ScoreBoard",
            "Move ScoreBoard",
//            "MediaPlayer",
            "Watermark icons"
    };
    private static Hud instance;

    private final MultiModeSetting elements = register(new MultiModeSetting("Elements", "HUD elements to render.",
            ELEMENTS, ELEMENTS));
    private final BooleanSetting blur = register(new BooleanSetting("Blur", "Use blurred HUD backgrounds.", true));
    private final BooleanSetting glow = register(new BooleanSetting("Glow", "Use glow effects on HUD elements.", true));
    private final ModeSetting targetStyle = register(new ModeSetting("Target Style", "Стиль таргет худа.", "Обычный", "Обычный", "Quick", "Sadnes"));

    private final TargetHudQuick targetHudQuick = new TargetHudQuick();
    private final TargetHud targetHud = new TargetHud();
    private final TargetHudSadnes targetHudSadnes = new TargetHudSadnes();

    private final List<HudElement> elementsList = List.of(
            new Hotbar(),
            new Armor(),
            new HotKeys(),
            new Binds(),
            new Notifications(),
            new Potions(),
            new Cooldowns(),
            targetHud,
            new Staff(),
            new Inventory(),
            new MoveScoreBoard(),
            new ScoreBoardHud(),
            new MediaPlayer(),
            // new WatermarkIcons(), // Debug panel - disabled
            new AnimationShowcase(),
            new Watermark()
    );

    public Hud() {
        super("HUD", "Renders draggable HUD elements.", ModuleCategory.VISUAL);
        instance = this;
        setEnabled(true);
    }

    @Override
    protected void onEnable() {
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
        Hud5 hud5 = Hud5.getInstance();
        if (hud5 != null && hud5.isEnabled()) {
            hud5.setEnabled(false);
        }
    }

    public static Hud getInstance() {
        return instance;
    }

    public static boolean shouldRenderCustomHotbar() {
        Hud hud = instance;
        return hud != null && hud.isEnabled() && hud.elements.isSelected("Hotbar");
    }

    public static boolean shouldRenderCustomChat() {
        return false;
    }

    public static boolean shouldRenderCustomScoreboard() {
        Hud hud = instance;
        return hud != null && hud.isEnabled() && hud.elements.isSelected("ScoreBoard");
    }

    public static boolean isBlurEnabled() {
        Hud hud = instance;
        return hud == null || hud.blur.getValue();
    }

    public static boolean isGlowEnabled() {
        Hud hud = instance;
        return hud == null || hud.glow.getValue();
    }

    public static void renderHudBackground(float x, float y, float width, float height, float radius, float blurRadius, float smoothness, int color) {
        if (isBlurEnabled()) {
            Render2D.blur(x, y, width, height, radius, blurRadius, smoothness, color);
            return;
        }
        Render2D.rect(x, y, width, height, radius, noBlurRectColor(color));
    }

    public static void renderHudBackground(BuiltBlur blur) {
        if (blur == null) {
            return;
        }
        if (isBlurEnabled()) {
            Render2D.blur(blur);
            return;
        }
        Render2D.rect(
                blur.x(),
                blur.y(),
                blur.width(),
                blur.height(),
                blur.radiusTopLeft(),
                blur.radiusTopRight(),
                blur.radiusBottomRight(),
                blur.radiusBottomLeft(),
                noBlurRectColor(blur.color())
        );
    }

    public static void renderHudGlow(String texture, float x, float y, float width, float height, float radius, int color) {
        if (isGlowEnabled()) {
            Render2D.image(texture, x, y, width, height, radius, color);
        }
    }

    private static int noBlurRectColor(int color) {
        int alpha = (color >>> 24) & 0xFF;
        int scaledAlpha = Math.round(alpha * NO_BLUR_RECT_ALPHA_SCALE);
        return (color & 0x00FFFFFF) | (scaledAlpha << 24);
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
                if (element == targetHud) {
                    syncTargetStyleVisibility(true);
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
            HudElement active = (element == targetHud) ? targetStyleElement() : element;
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
            HudElement active = (element == targetHud) ? targetStyleElement() : element;
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
            HudElement active = (element == targetHud) ? targetStyleElement() : element;
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
        if (element instanceof Watermark watermark) {
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
        return targetHud;
    }

    private void syncTargetStyleVisibility(boolean selected) {
        targetHud.setHudVisible(selected && targetStyle.is("Обычный"));
        targetHudQuick.setHudVisible(selected && targetStyle.is("Quick"));
        targetHudSadnes.setHudVisible(selected && targetStyle.is("Sadnes"));
    }

    private void setElementVisible(HudElement element, boolean visible) {
        if (element instanceof HudPanel panel) {
            panel.setHudVisible(visible);
            return;
        }
        if (element instanceof Watermark watermark) {
            watermark.setHudVisible(visible);
            return;
        }
        if (element instanceof Hotbar hotbar) {
            hotbar.setHudVisible(visible);
        }
    }
}
