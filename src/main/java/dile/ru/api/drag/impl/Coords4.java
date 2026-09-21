package dile.ru.api.drag.impl;

import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

public final class Coords4 extends HudPanel {
    private static final float HEIGHT = 15.0F;

    public Coords4() {
        super("coords4", "Coordinates", 3.0F, 524.0F, 110.0F, HEIGHT);
    }

    @Override
    public void render() {
        float x = drag.x();
        float y = drag.y();

        if (mc.player == null) {
            return;
        }
        int posX = (int) mc.player.getX();
        int posY = (int) mc.player.getY();
        int posZ = (int) mc.player.getZ();
        String text = "Coords: X: " + posX + " Y: " + posY + " Z: " + posZ;

        float textWidth = Render2D.textWidth(TEXT_FONT, text, 6.0F) + 8.5F;
        Render2D.rect(x, y, textWidth, HEIGHT, 3.0F, ColorUtil.rgba(0, 0, 0, 195));
        Render2D.text(TEXT_FONT, text, x + 4.5F, y + 4.0F, 6.0F, ColorUtil.rgba(255, 255, 255, 255));

        size(textWidth, HEIGHT);
    }
}
