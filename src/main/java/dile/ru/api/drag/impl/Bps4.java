package dile.ru.api.drag.impl;

import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

public final class Bps4 extends HudPanel {
    private static final float HEIGHT = 15.0F;

    public Bps4() {
        super("bps4", "Speed", 3.0F, 507.0F, 90.0F, HEIGHT);
    }

    @Override
    public void render() {
        float x = drag.x();
        float y = drag.y();

        if (mc.player == null) {
            return;
        }
        double dx = mc.player.getX() - mc.player.xOld;
        double dz = mc.player.getZ() - mc.player.zOld;
        double speed = Math.hypot(dx, dz) * 20.0;
        String text = "Speed: " + String.format("%.2f", speed);

        float textWidth = Render2D.textWidth(TEXT_FONT, text, 6.0F) + 8.5F;
        Render2D.rect(x, y, textWidth, HEIGHT, 3.0F, ColorUtil.rgba(0, 0, 0, 195));
        Render2D.text(TEXT_FONT, text, x + 4.5F, y + 4.3F, 6.0F, ColorUtil.rgba(255, 255, 255, 255));

        size(textWidth, HEIGHT);
    }
}
