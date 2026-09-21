package dile.ru.api.module.impl.visual.esp;

import net.minecraft.world.phys.AABB;
import dile.ru.utils.render.world.glow.Glow3D;

public final class EspGlowRenderer {
    private static final float SPACING = 0.15f;
    private static final int MAX_POINTS = 480;

    private EspGlowRenderer() {
    }

    public static void render(AABB box, int color, float size, float alpha, float intensity) {
        if (box == null) {
            return;
        }

        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        int emitted = 0;
        for (double y = minY; y <= maxY + 1e-6D; y += SPACING) {
            double yy = Math.min(y, maxY);
            for (double x = minX; x <= maxX + 1e-6D; x += SPACING) {
                emitted = emit(x, yy, minZ, size, color, alpha, intensity, emitted);
                emitted = emit(x, yy, maxZ, size, color, alpha, intensity, emitted);
            }
            for (double z = minZ; z <= maxZ + 1e-6D; z += SPACING) {
                emitted = emit(minX, yy, z, size, color, alpha, intensity, emitted);
                emitted = emit(maxX, yy, z, size, color, alpha, intensity, emitted);
            }
        }
        for (double x = minX; x <= maxX + 1e-6D; x += SPACING) {
            for (double z = minZ; z <= maxZ + 1e-6D; z += SPACING) {
                emitted = emit(x, minY, z, size, color, alpha, intensity, emitted);
                emitted = emit(x, maxY, z, size, color, alpha, intensity, emitted);
            }
        }
    }

    private static int emit(double x, double y, double z, float size, int color, float alpha, float intensity, int emitted) {
        if (emitted >= MAX_POINTS) {
            return emitted;
        }
        Glow3D.glow(x, y, z, size, color, alpha, intensity);
        return emitted + 1;
    }
}
