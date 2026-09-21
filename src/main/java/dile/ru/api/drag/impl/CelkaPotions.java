package dile.ru.api.drag.impl;

import net.minecraft.world.effect.MobEffectInstance;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

import java.util.ArrayList;
import java.util.List;

public final class CelkaPotions extends CelkaHudPanel {
    private static final float WIDTH = 80.0F;
    private static final float ENTRY_HEIGHT = 20.0F;
    private static final float PADDING = 5.0F;
    private static final float ROW_GAP = 24.0F;

    public CelkaPotions() {
        super("potions", "Potions", 200.0F, 50.0F, WIDTH, 21.0F);
    }

    @Override
    public void render() {
        List<MobEffectInstance> active = activePotions();
        if (active.isEmpty()) {
            contentVisible(false);
            return;
        }

        float posX = drag.x();
        float posY = drag.y();
        float totalHeight = active.size() * ROW_GAP;

        int glowStart = style(0);
        int glowEnd = style(90);

        size(WIDTH, totalHeight);

        int index = 0;
        for (MobEffectInstance effect : active) {
            float entryY = posY + index * ROW_GAP;

            String durationText = formatDurationTicks(effect.getDuration());
            String effectName = effect.getEffect().value().getDisplayName().getString();
            String amplifierText = roman(effect.getAmplifier() + 1);
            String fullEffectName = effectName + " " + amplifierText;

            glow(posX, entryY, WIDTH, ENTRY_HEIGHT, 10.0F, glowStart, glowEnd);
            Render2D.rect(posX, entryY, WIDTH, ENTRY_HEIGHT, 1.0F, ColorUtil.rgba(30, 30, 30, 220));
            Render2D.rect(posX, entryY, WIDTH, 1.0F, ColorUtil.rgba(30, 30, 30, 220));

            Render2D.effectIcon(effect, posX + PADDING, entryY + (ENTRY_HEIGHT - 18.0F) / 2.0F, 18.0F, ColorUtil.WHITE);
            Render2D.text(CELKA_FONT, fullEffectName, posX + PADDING + 2.0F, entryY + 5.0F, 7.0F, ColorUtil.WHITE);
            Render2D.text(CELKA_FONT, durationText,
                    posX + 27.0F - Render2D.textWidth(CELKA_FONT, durationText, 6.5F) - PADDING,
                    entryY + 13.0F, 6.5F, CELKA_MUTED);

            index++;
        }
    }

    private List<MobEffectInstance> activePotions() {
        List<MobEffectInstance> result = new ArrayList<>();
        if (mc.player != null) {
            for (MobEffectInstance effect : mc.player.getActiveEffects()) {
                if (effect.showIcon()) {
                    result.add(effect);
                }
            }
        }
        return result;
    }

    private static String roman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(number);
        };
    }
}
