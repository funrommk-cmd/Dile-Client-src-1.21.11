package dile.ru.api.drag.impl;

import dile.ru.api.module.Module;
import dile.ru.api.module.impl.visual.Hud5;
import dile.ru.manager.Manager;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

public final class CelkaWatermark extends CelkaHudPanel {
    private static final float HEIGHT = 22.0F;

    public CelkaWatermark() {
        super("watermark", "Watermark", 10.0F, 10.0F, 120.0F, HEIGHT);
        drag.locked(true);
    }

    @Override
    public void render() {
        int offset = Hud5.getOffset();
        float x = 5.0F + offset;
        float y = 10.0F;

        int firstColor = style(0);
        int secondColor = style(90);
        int thirdColor = style(180);
        int fourthColor = style(270);

        String title = "Dile";
        String uidLabel = "UID:";
        String uidNumber = resolveUid();

        float titleWidth = Render2D.textWidth(CELKA_FONT, title, 9.0F);
        float uidWidth = Render2D.textWidth(CELKA_FONT, uidLabel + " " + uidNumber, 8.0F);
        float width = Math.max(titleWidth, uidWidth) + 38.0F;

        glow(x, y, width, HEIGHT, 12.0F, firstColor, secondColor);
        gradientRound(x, y, width, HEIGHT, 3.0F, firstColor, secondColor, thirdColor, fourthColor);

        float circleX = x + 8.0F;
        float circleY = y + HEIGHT / 2.0F;
        float circleRadius = 26.0F;
        glow(circleX - circleRadius / 2.0F, circleY - circleRadius / 2.0F, circleRadius, circleRadius, 10.0F, firstColor, secondColor);
        circle(circleX, circleY, circleRadius / 2.0F, secondColor, fourthColor);

        centeredText(title, x - 8.3F + width / 2.0F, y + 1.7F, 9.0F, ColorUtil.WHITE);
        Render2D.text(CELKA_FONT, uidLabel, x + width / 2.0F - 26.3F, y + 11.7F, 8.0F, ColorUtil.WHITE);
        Render2D.text(CELKA_FONT, uidNumber, x + width / 2.0F - 6.3F, y + 11.7F, 8.0F, CELKA_MUTED);

        cloud(x - 4.0F, y - 1.0F, 24.0F);

        drag.position(x, y);
        size(width, HEIGHT);
    }

    private String resolveUid() {
        Module nameProtect = Manager.getModules().getByName("Name Protect").orElse(null);
        if (nameProtect != null && nameProtect.isEnabled()) {
            return "Protected";
        }
        if (mc.getUser() != null && mc.getUser().getName() != null && !mc.getUser().getName().isBlank()) {
            return mc.getUser().getName();
        }
        if (mc.player != null && mc.player.getGameProfile() != null && mc.player.getGameProfile().name() != null) {
            return mc.player.getGameProfile().name();
        }
        return "User";
    }
}
