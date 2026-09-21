package dile.ru.api.module.impl.visual;

import net.minecraft.client.Minecraft;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.TickEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.world.skyshader.SkyShaderRenderer;

import java.awt.Color;

public final class SkyShader extends Module {
    private static SkyShader instance;

    private final ModeSetting mode = register(new ModeSetting("Mode", "Sky shader mode.", "Water", "Water", "Caustic", "Stars"));
    private final NumberSetting speed = register(new NumberSetting("Speed", "Animation speed.", 1.0, 0.1, 5.0, 0.1));
    private final NumberSetting scale = register(new NumberSetting("Scale", "Pattern scale.", 5.0, 1.0, 20.0, 0.5));
    private final NumberSetting intensity = register(new NumberSetting("Intensity", "Effect intensity.", 0.01, 0.001, 0.05, 0.001));
    private final NumberSetting skyAlpha = register(new NumberSetting("Alpha", "Sky transparency.", 1.0, 0.3, 1.0, 0.05));
    private final ColorSetting skyColor = register(new ColorSetting("Color", "Sky theme color.", new Color(80, 130, 255, 255)));
    private final BooleanSetting removeClouds = register(new BooleanSetting("Remove Clouds", "Remove vanilla clouds.", false));

    public SkyShader() {
        super("Sky Shader", "Custom sky shader effect.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static SkyShader getInstance() {
        return instance;
    }

    public boolean shouldRemoveClouds() {
        return isEnabled() && removeClouds.getValue();
    }

    @Override
    protected void onEnable() {
        syncRenderer();
    }

    @Override
    protected void onDisable() {
        SkyShaderRenderer.shutdown();
    }

    @SubscribeEvent
    private void onTick(TickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        syncRenderer();
    }

    private void syncRenderer() {
        Color c = skyColor.getValue();
        SkyShaderRenderer.configure(
                c.getRed() / 255.0f,
                c.getGreen() / 255.0f,
                c.getBlue() / 255.0f,
                speed.getFloat(),
                scale.getFloat(),
                intensity.getFloat(),
                skyAlpha.getFloat(),
                mode.is("Caustic") ? 1.0f : mode.is("Stars") ? 2.0f : 0.0f
        );
    }
}
