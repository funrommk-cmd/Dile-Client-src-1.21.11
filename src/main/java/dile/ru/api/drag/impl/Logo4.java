package dile.ru.api.drag.impl;

import dile.ru.api.module.impl.combat.AuraModule;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

public final class Logo4 extends HudPanel {
    private static final float HEIGHT = 17.0F;

    public Logo4() {
        super("logo4", "Logo", 3.0F, 3.0F, 90.0F, HEIGHT);
    }

    @Override
    public void render() {
        float x = drag.x();
        float y = drag.y();

        String text = "Dile Client";
        String separator = "|";
        String fpsText = mc.getFps() + " fps";
        float textSize = 7.0F;
        float textWidth = Render2D.textWidth(TEXT_FONT, text, textSize);
        float separatorWidth = Render2D.textWidth(TEXT_FONT, separator, textSize);
        float fpsWidth = Render2D.textWidth(TEXT_FONT, fpsText, textSize);
        float panelWidth = textWidth + separatorWidth + fpsWidth + 14.2F;

        Render2D.rect(x, y, panelWidth, HEIGHT, 3.0F, ColorUtil.rgba(0, 0, 0, 195));
        Render2D.text(TEXT_FONT, text, x + 4.5F, y + 4.5F, textSize, ColorUtil.rgba(255, 255, 255, 255));
        Render2D.text(TEXT_FONT, separator, x + 4.5F + textWidth + 3.5F, y + 4.5F, textSize, ColorUtil.rgba(128, 128, 128, 255));
        Render2D.text(TEXT_FONT, fpsText, x + 4.5F + textWidth + separatorWidth + 6.5F, y + 4.5F, textSize, ColorUtil.rgba(255, 255, 255, 255));

        int tps = Math.max(0, Math.round(AuraModule.activeTps()));
        String ticksText = "Ticks: " + tps;
        float ticksWidth = Render2D.textWidth(TEXT_FONT, ticksText, 6.5F);
        Render2D.rect(x, y + 20.0F, ticksWidth + 6.0F, HEIGHT - 2.0F, 3.0F, ColorUtil.rgba(0, 0, 0, 195));
        Render2D.text(TEXT_FONT, ticksText, x + 3.0F, y + 24.4F, 6.5F, ColorUtil.rgba(255, 255, 255, 255));

        size(Math.max(panelWidth, ticksWidth + 6.0F), HEIGHT + 23.0F);
    }
}
