package dile.ru.api.drag.impl;

import dile.ru.api.module.impl.combat.AuraModule;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

public final class TimerHud4 extends HudPanel {
    private static final float WIDTH = 71.0F;
    private static final float HEIGHT = 25.0F;

    private float perc;

    public TimerHud4() {
        super("timerhud4", "Timer Indicator", 140.0F, 170.0F, WIDTH, HEIGHT);
    }

    @Override
    public void render() {
        float x = drag.x();
        float y = drag.y();

        float tps = AuraModule.activeTps();
        float target = clamp(tps / 20.0F, 0.0F, 1.0F);
        perc = lerp(perc, target, 0.12F);
        if (Math.abs(perc - target) < 0.001F) {
            perc = target;
        }

        Render2D.rect(x, y, WIDTH, HEIGHT, 3.0F, ColorUtil.rgba(0, 0, 0, 195));
        Render2D.rect(x + 5.0F, y + 14.0F, 61.0F, 8.0F, 0.0F, ColorUtil.rgba(23, 23, 23, 196));
        Render2D.rect(x + 5.0F, y + 14.0F, 61.0F * perc, 8.0F, 3.0F, ClickGuiModule.getInstance().getColor());
        Render2D.text(TEXT_FONT, "Timer", x + 23.5F, y + 3.6F, 6.5F, ColorUtil.rgba(255, 255, 255, 255));

        size(WIDTH, HEIGHT);
    }

    private static float lerp(float current, float target, float factor) {
        return current + (target - current) * factor;
    }
}
