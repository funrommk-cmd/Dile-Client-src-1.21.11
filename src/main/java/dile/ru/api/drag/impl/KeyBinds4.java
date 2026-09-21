package dile.ru.api.drag.impl;

import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleManager;
import dile.ru.manager.Manager;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

import java.util.ArrayList;
import java.util.List;

public final class KeyBinds4 extends HudPanel {
    private static final float WIDTH = 75.0F;
    private static final float OFFSET = 9.5F;

    private float heightAnimation = 0.0F;
    private int activeBind;

    public KeyBinds4() {
        super("keybinds4", "Active Binds", 140.0F, 200.0F, WIDTH, 21.0F);
    }

    @Override
    public void render() {
        float x = drag.x();
        float y = drag.y();

        float height2 = 21.0F;
        height2 += OFFSET * activeBind - 5.0F;
        if (activeBind == 0) {
            height2 -= 1.0F;
        }

        Render2D.rect(x, y, WIDTH, heightAnimation, 3.0F, ColorUtil.rgba(0, 0, 0, 195));
        Render2D.text(TEXT_FONT, "Key Binds", x + (WIDTH - Render2D.textWidth(TEXT_FONT, "Key Binds", 6.5F)) / 2.0F, y + 4.0F, 6.5F, ColorUtil.rgba(255, 255, 255, 255));

        int count = 0;
        List<Module> binds = binds();
        for (Module module : binds) {
            String bind = shortBind(module.getBind());
            float bindWidth = Render2D.textWidth(TEXT_FONT, bind, 6.0F);
            float yText = y + 16.7F + count * OFFSET;
            Render2D.text(TEXT_FONT, module.getName(), x + 4.0F, yText, 6.0F, ColorUtil.rgba(255, 255, 255, 255));
            Render2D.text(TEXT_FONT, bind, x + 71.0F - bindWidth, yText, 6.0F, ColorUtil.rgba(255, 255, 255, 255));
            count++;
        }
        activeBind = count;

        heightAnimation += (height2 - heightAnimation) * 0.14F;
        if (Math.abs(heightAnimation - height2) < 0.1F) {
            heightAnimation = height2;
        }
        size(WIDTH, height2);
    }

    private List<Module> binds() {
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
        return result;
    }
}
