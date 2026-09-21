package dile.ru.api.module.impl.visual;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.post.worldglow.WorldGlowRenderer;

import java.awt.Color;

public final class WorldGlow extends Module {
    private static WorldGlow instance;

    private final NumberSetting strength = register(new NumberSetting("Strength", "Opacity of the glow effect.", 1.0, 0.0, 1.0, 0.05));
    private final NumberSetting speed = register(new NumberSetting("Speed", "Speed of the moving glow.", 1.0, 0.1, 5.0, 0.1));
    private final ColorSetting color = register(new ColorSetting("Glow Color", "Color of the moving glow.", new Color(127, 242, 255, 255)));

    private float time;
    private long lastNanos;

    public WorldGlow() {
        super("World Glow", "Makes the world glow and shine through from you to the horizon.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static WorldGlow getInstance() {
        return instance;
    }

    @Override
    protected void onEnable() {
        time = 0.0f;
        lastNanos = 0L;
    }

    @Override
    protected void onDisable() {
        WorldGlowRenderer.clear();
    }

    @Override
    public void onTick(Minecraft client) {
        long now = System.nanoTime();
        if (lastNanos != 0L) {
            time += (now - lastNanos) / 1_000_000_000.0f;
        }
        lastNanos = now;
    }

    public void onAfterTranslucent(RenderTarget renderTarget, Matrix4f projection, Matrix4f view, Vec3 cameraPos) {
        if (mc.player == null || mc.level == null || mc.gameRenderer == null) {
            return;
        }
        if (WorldGlowRenderer.isDisabledAfterError()) {
            return;
        }
        WorldGlowRenderer.apply(
                renderTarget,
                color.getValue().getRGB(),
                strength.getFloat(),
                speed.getFloat(),
                time,
                projection,
                view,
                cameraPos
        );
    }
}
