package dile.ru.api.drag.impl;

import net.minecraft.world.effect.MobEffectInstance;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

public final class Effects4 extends HudPanel {
    private static final float HEIGHT = 21.3F;
    private static final float OFFSET = 24.0F;

    public Effects4() {
        super("effects4", "Effects", 3.0F, 40.0F, 90.0F, HEIGHT);
    }

    @Override
    public void render() {
        float x = drag.x();
        float y = drag.y();

        if (mc.player == null) {
            return;
        }

        int counter = 0;
        float maxWidth = 0.0F;
        for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            String level = String.valueOf(effect.getAmplifier() + 1);
            if ("1".equals(level)) {
                level = "";
            }
            String duration = formatDuration(effect);
            String name = effect.getEffect().value().getDisplayName().getString();
            String title = name + (level.isEmpty() ? "" : " " + level);

            float textWidth = Render2D.textWidth(TEXT_FONT, title, 6.0F) + Render2D.textWidth(TEXT_FONT, duration, 5.5F) + 27.3F - 0.6F;
            maxWidth = Math.max(maxWidth, textWidth);

            Render2D.rect(x, y, textWidth, HEIGHT, 3.0F, ColorUtil.rgba(0, 0, 0, 195));
            Render2D.text(TEXT_FONT, title, x + 22.3F, y + 3.7F, 6.0F, ColorUtil.rgba(255, 255, 255, 255));
            Render2D.text(TEXT_FONT, duration, x + 22.3F, y + 11.7F, 5.5F, ColorUtil.rgba(255, 255, 255, 255));
            Render2D.effectIcon(effect, x + 2.0F, y + 4.0F, 16.0F);
            y += OFFSET;
            counter++;
        }

        if (counter == 0) {
            maxWidth = 90.0F;
        }
        size(maxWidth, HEIGHT * counter + OFFSET / 8.0F * counter);
    }

    private String formatDuration(MobEffectInstance effect) {
        if (effect.getDuration() < 0) {
            return "inf";
        }
        int totalSeconds = Math.max(0, effect.getDuration() / 20);
        return totalSeconds / 60 + ":" + twoDigits(totalSeconds % 60);
    }
}
