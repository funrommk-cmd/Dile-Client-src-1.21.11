package dile.ru.api.module.impl.visual;

import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.NumberSetting;

public final class ChunkAnimator extends Module {
    private static ChunkAnimator instance;

    private final NumberSetting duration = register(new NumberSetting("Duration", "Chunk fade-in duration in seconds.", 1.5, 0.2, 5.0, 0.1));
    private final ModeSetting easing = register(new ModeSetting("Easing", "Animation curve of the chunk fade.", "Ease In Out", "Ease In Out", "Ease Out", "Ease In", "Smoothstep", "Linear"));
    private final BooleanSetting fadeEmpty = register(new BooleanSetting("Fade Empty", "Also animate chunks that vanilla would show instantly.", false));
    private final BooleanSetting randomize = register(new BooleanSetting("Randomize", "Randomize the fade duration per chunk for a more organic look.", true));

    public ChunkAnimator() {
        super("Chunk Animator", "Animates the appearance of blocks in newly loaded chunks.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static ChunkAnimator getInstance() {
        return instance;
    }

    public long getDurationMs() {
        return (long) (duration.getFloat() * 1000.0F);
    }

    public boolean isFadeEmpty() {
        return fadeEmpty.getValue();
    }

    public long effectiveDuration(int sectionIndex) {
        long base = Math.max(1L, getDurationMs());
        if (!randomize.getValue()) {
            return base;
        }
        long r = sectionIndex * 2654435761L + 0x9E3779B9L;
        r ^= r >>> 16;
        r *= 1274126177L;
        r ^= r >>> 16;
        float factor = 0.8F + (r & 0xFFFF) / 65535.0F * 0.5F;
        return Math.max(1L, (long) (base * factor));
    }

    public float applyEasing(float progress) {
        if (easing.is("Ease Out")) {
            return 1.0F - (float) Math.pow(1.0F - progress, 3.0F);
        }
        if (easing.is("Ease In")) {
            return progress * progress * progress;
        }
        if (easing.is("Smoothstep")) {
            return progress * progress * (3.0F - 2.0F * progress);
        }
        if (easing.is("Linear")) {
            return progress;
        }
        return progress < 0.5F ? 4.0F * progress * progress * progress : 1.0F - (float) Math.pow(-2.0F * progress + 2.0F, 3.0F) / 2.0F;
    }
}
