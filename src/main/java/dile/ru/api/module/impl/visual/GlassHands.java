package dile.ru.api.module.impl.visual;

import net.minecraft.client.Minecraft;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.shader.GlassHandsRenderer;

import java.awt.Color;

public class GlassHands extends Module {

    private static GlassHands instance;

    private final ModeSetting viewMode = register(new ModeSetting("Вид", "Свечение на руке: Default - стекло, None - нет эффекта, Shader/Shader2/Shader3/Shader4 - заливка шейдером.", "Default", "Default", "None", "Shader", "Shader2", "Shader3", "Shader4"));
    private final BooleanSetting removeHand = register(new BooleanSetting("Убрать руку/предмет", "Скрыть руку/предмет, оставив только свечение.", false));
    private final NumberSetting shaderTransparency = register(new NumberSetting("Прозрачность", "Прозрачность заливки шейдера (Shader/Shader2).", 0.0, 0.0, 1.0, 0.05));

    private final NumberSetting blurRadius = register(new NumberSetting("Blur Radius", "Glass blur strength.", 2.5, 1.0, 5.0, 0.1));
    private final NumberSetting blurIterations = register(new NumberSetting("Quality", "Number of blur iterations.", 3, 1, 5, 1));
    private final NumberSetting saturation = register(new NumberSetting("Saturation", "Color saturation.", 0.0, 0.0, 2.0, 0.05));

    private final BooleanSetting enableTint = register(new BooleanSetting("Tint", "Enable glass color tint.", false));
    private final NumberSetting tintIntensity = register(new NumberSetting("Tint Intensity", "Tint color intensity.", 0.2, 0.0, 0.5, 0.01));
    private final ColorSetting tintColor = register(new ColorSetting("Tint Color", "Glass tint color.", new Color(0, 255, 255)));

    private final BooleanSetting enableEdgeGlow = register(new BooleanSetting("Edge Glow", "Enable edge glow effect.", true));
    private final NumberSetting edgeGlowIntensity = register(new NumberSetting("Glow Intensity", "Edge glow intensity.", 0.2, 0.0, 1.0, 0.01));

    public GlassHands() {
        super("GlassHands", "Makes hands and items appear glass-like.", ModuleCategory.VISUAL);
        tintIntensity.visibleWhen(() -> enableTint.getValue());
        tintColor.visibleWhen(() -> enableTint.getValue());
        edgeGlowIntensity.visibleWhen(() -> enableEdgeGlow.getValue());
        shaderTransparency.visibleWhen(() -> viewMode.is("Shader") || viewMode.is("Shader2") || viewMode.is("Shader3") || viewMode.is("Shader4"));
        instance = this;
    }

    public static GlassHands getInstance() {
        return instance;
    }

    @Override
    protected void onEnable() {
        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        renderer.invalidate();
        renderer.setEnabled(true);
        syncRenderer();
    }

    @Override
    protected void onDisable() {
        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        renderer.setEnabled(false);
    }

    @Override
    public void onTick(Minecraft client) {
        syncRenderer();
    }

    private void syncRenderer() {
        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();

        renderer.setViewMode(viewModeIndex());
        renderer.setRemoveHand(removeHand.getValue());
        renderer.setShaderTransparency(shaderTransparency.getFloat());

        renderer.setBlurRadius(blurRadius.getFloat());
        renderer.setBlurIterations((int) blurIterations.getValue().doubleValue());
        renderer.setSaturation(saturation.getFloat());
        renderer.setReflect(true);

        if (enableTint.getValue()) {
            renderer.setTintColor(tintColor.getValue().getRGB());
            renderer.setTintIntensity(tintIntensity.getFloat());
        } else {
            renderer.setTintColor(0x00000000);
            renderer.setTintIntensity(0.0f);
        }

        if (enableEdgeGlow.getValue()) {
            renderer.setEdgeGlowIntensity(edgeGlowIntensity.getFloat());
        } else {
            renderer.setEdgeGlowIntensity(0.0f);
        }
    }

    private int viewModeIndex() {
        return switch (viewMode.getValue()) {
            case "None" -> GlassHandsRenderer.VIEW_NONE;
            case "Shader" -> GlassHandsRenderer.VIEW_SHADER1;
            case "Shader2" -> GlassHandsRenderer.VIEW_SHADER2;
            case "Shader3" -> GlassHandsRenderer.VIEW_SHADER3;
            case "Shader4" -> GlassHandsRenderer.VIEW_SHADER4;
            default -> GlassHandsRenderer.VIEW_DEFAULT;
        };
    }
}
