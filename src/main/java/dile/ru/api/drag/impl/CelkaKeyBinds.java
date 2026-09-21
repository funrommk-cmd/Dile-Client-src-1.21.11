package dile.ru.api.drag.impl;

import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleManager;
import dile.ru.manager.Manager;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CelkaKeyBinds extends CelkaHudPanel {
    private static final float WIDTH = 110.0F;
    private static final float HEADER_HEIGHT = 13.0F;
    private static final float ITEM_HEIGHT = 16.0F;
    private static final float PADDING = 6.0F;
    private static final float TOP_BAR_HEIGHT = 2.0F;

    public CelkaKeyBinds() {
        super("keybinds", "KeyBinds", 10.0F, 100.0F, WIDTH, 21.0F);
    }

    @Override
    public void render() {
        float posX = drag.x();
        float posY = drag.y();

        List<Module> activeBinds = activeBinds();
        float height = HEADER_HEIGHT + (activeBinds.isEmpty() ? 20.0F : activeBinds.size() * ITEM_HEIGHT);

        int barColor = style(0);
        int glowStart = style(0);
        int glowEnd = style(90);

        glow(posX, posY, WIDTH, height, 10.0F, glowStart, glowEnd);
        Render2D.rect(posX, posY, WIDTH, height, ColorUtil.rgba(0, 0, 0, 220));
        Render2D.rect(posX, posY, WIDTH, TOP_BAR_HEIGHT, barColor);

        centeredText("Keybinds", posX + WIDTH / 2.0F, posY + 6.0F, 9.0F, ColorUtil.WHITE);

        if (activeBinds.isEmpty()) {
            Render2D.image("dile:textures/hud/keyboard.png", posX + 3.0F, posY + 14.0F, 16.0F, 16.0F, 0.0F, -1);
            centeredText("Список пуст", posX - 4.0F + WIDTH / 2.0F, posY + HEADER_HEIGHT + 2.0F, 8.5F, ColorUtil.WHITE);
            centeredText("Нет активных биндов!", posX + 2.0F + WIDTH / 2.0F, posY + 11.0F + HEADER_HEIGHT, 5.5F, ColorUtil.WHITE);
        } else {
            float yOffset = posY + HEADER_HEIGHT;
            for (Module module : activeBinds) {
                String keyName = keyName(module);
                Render2D.text(CELKA_FONT, module.getName(), posX + PADDING, yOffset + 4.0F, 7.5F, ColorUtil.WHITE);
                if (keyName != null) {
                    String bracket = "[" + keyName.toUpperCase() + "]";
                    Render2D.text(CELKA_FONT, bracket,
                            posX + WIDTH - PADDING - Render2D.textWidth(CELKA_FONT, bracket, 6.5F),
                            yOffset + 4.0F, 7.0F, ColorUtil.WHITE);
                }
                yOffset += ITEM_HEIGHT;
            }
        }

        size(WIDTH, height);
    }

    private List<Module> activeBinds() {
        List<Module> result = new ArrayList<>();
        ModuleManager manager = Manager.getModules();
        if (manager == null) {
            return result;
        }
        for (Module module : manager.getModules()) {
            if (module.isEnabled() && module.getBind() != null && module.getBind().isBound()) {
                result.add(module);
            }
        }
        result.sort(Comparator.comparing(Module::getName));
        return result;
    }

    private String keyName(Module module) {
        String key = shortBind(module.getBind());
        if (key == null || key.isEmpty() || key.equals("None")) {
            return null;
        }
        return key;
    }
}
