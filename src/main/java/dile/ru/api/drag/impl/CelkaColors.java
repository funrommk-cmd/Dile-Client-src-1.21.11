package dile.ru.api.drag.impl;

import dile.ru.api.module.impl.visual.ClickGuiModule;

import java.awt.Color;

final class CelkaColors {
    private static final float SATURATION = 0.75F;
    private static final float BRIGHTNESS = 0.85F;

    private CelkaColors() {
    }

    static int getColorStyle(int offset) {
        float hue = ClickGuiModule.getInstance().getHue();
        float wrapped = ((hue + offset / 360.0F) % 1.0F + 1.0F) % 1.0F;
        return Color.HSBtoRGB(wrapped, SATURATION, BRIGHTNESS);
    }
}
