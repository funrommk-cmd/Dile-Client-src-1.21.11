package dile.ru.api.module.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.WorldRenderEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.Render3D;
import dile.ru.utils.render.world.glow.Glow3D;

import java.awt.Color;

public final class GlowItem extends Module {
    private static GlowItem instance;

    private final ColorSetting color = register(new ColorSetting("Color", "Glow color.", new Color(255, 210, 92, 255)));
    private final NumberSetting size = register(new NumberSetting("Size", "Glow size.", 0.45, 0.1, 1.5, 0.05));
    private final NumberSetting alpha = register(new NumberSetting("Alpha", "Glow opacity.", 0.6, 0.0, 1.0, 0.05));
    private final NumberSetting intensity = register(new NumberSetting("Intensity", "Glow intensity.", 1.0, 0.1, 2.0, 0.1));
    private final NumberSetting range = register(new NumberSetting("Range", "Max render distance.", 64.0, 8.0, 256.0, 8.0));
    private final BooleanSetting throughWalls = register(new BooleanSetting("Through Walls", "Render through blocks.", true));
    private final BooleanSetting coreGlow = register(new BooleanSetting("Core", "Render bright core glow.", true));
    private final NumberSetting yOffset = register(new NumberSetting("Y Offset", "Vertical offset.", 0.3, -0.5, 1.0, 0.05));

    public GlowItem() {
        super("GlowItem", "Renders glow effects on dropped items.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static GlowItem getInstance() {
        return instance;
    }

    @SubscribeEvent
    private void onWorldRender(WorldRenderEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        float tickDelta = event.getTickDelta();
        Vec3 camera = Render3D.lastCameraPos;
        if (camera == null) return;

        int colorInt = color.getValue().getRGB();
        float s = size.getFloat();
        float a = alpha.getFloat();
        float i = intensity.getFloat();
        float maxRange = range.getFloat();
        float yo = yOffset.getFloat();
        boolean through = throughWalls.getValue();
        boolean core = coreGlow.getValue();

        Glow3D.setMatrices(Render3D.lastProjMat, Render3D.lastModMat);
        Glow3D.begin(through);

        try {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!(entity instanceof ItemEntity item)) continue;
                if (item.getItem().isEmpty()) continue;

                double dist = mc.player.distanceTo(item);
                if (dist > maxRange) continue;

                double ix = Mth.lerp(tickDelta, item.xo, item.getX());
                double iy = Mth.lerp(tickDelta, item.yo, item.getY()) + yo;
                double iz = Mth.lerp(tickDelta, item.zo, item.getZ());

                float fade = 1.0f - (float) (dist / maxRange);
                fade = Math.max(0.2f, fade);

                Glow3D.glow(ix, iy, iz, s, colorInt, a * fade, i);
                if (core) {
                    Glow3D.core(ix, iy, iz, s * 0.3f, colorInt, a * fade);
                }
            }
        } finally {
            Glow3D.end();
        }
    }
}
