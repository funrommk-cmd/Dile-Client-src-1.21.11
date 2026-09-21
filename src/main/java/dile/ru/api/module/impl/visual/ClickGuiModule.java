package dile.ru.api.module.impl.visual;

import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.screens.clickgui.theme.ClickGuiTheme;

import java.awt.Color;

public class ClickGuiModule extends Module {
    private static ClickGuiModule instance;

    private final ModeSetting guiMode = register(new ModeSetting("GUI Mode", "ClickGui rendering style.", "CS GUI", "CS GUI"));
    private final ModeSetting theme = register(new ModeSetting("Theme", "RGB color theme for the ClickGui.", "Green", "Green", "Blue", "Purple", "Red", "Orange", "Cyan", "Pink", "Yellow", "Rainbow"));
    private final NumberSetting rgbSpeed = register(new NumberSetting("RGB Speed", "Color cycling speed multiplier.", 1.0, 0.1, 5.0, 0.1));
    private final BooleanSetting customizationEnabled = register(new BooleanSetting("Customization", "Enable GUI customization panel.", false));
    private final NumberSetting transparency = register(new NumberSetting("Transparency", "Main GUI background transparency 1-100.", 100.0, 1.0, 100.0, 1.0));
    private final BooleanSetting moduleAnimation = register(new BooleanSetting("Module Animation", "Animate module entry in GUI.", true));
    private final BooleanSetting customBackground = register(new BooleanSetting("Custom Background", "Enable custom background glow effect.", false));

    public ClickGuiModule() {
        super("ClickGui", "ClickGui appearance and style settings.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static ClickGuiModule getInstance() {
        return instance;
    }

    public boolean isCsMode() {
        return guiMode.is("CS GUI");
    }

    public boolean isCustomizationEnabled() {
        return customizationEnabled.getValue();
    }

    public void setCustomizationEnabled(boolean value) {
        customizationEnabled.setValue(value);
    }

    public int getTransparency() {
        return (int) Math.round(transparency.getValue());
    }

    public void setTransparency(int value) {
        transparency.setValue((double) value);
    }

    public boolean isModuleAnimationEnabled() {
        return moduleAnimation.getValue();
    }

    public void setModuleAnimationEnabled(boolean value) {
        moduleAnimation.setValue(value);
    }

    public boolean isCustomBackgroundEnabled() {
        return customBackground.getValue();
    }

    public void setCustomBackgroundEnabled(boolean value) {
        customBackground.setValue(value);
    }

    public String getThemeName() {
        return theme.getValue();
    }

    public float getRgbSpeed() {
        return rgbSpeed.getFloat();
    }

    public int getThemeColor() {
        return ClickGuiTheme.getCycleColor(theme.getValue(), rgbSpeed.getFloat());
    }

    public int getThemeColor(float alpha) {
        int rgb = getThemeColor();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int a = Math.round(Math.max(0, Math.min(255, alpha * 255)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public int getDimmedColor(float brightnessFactor) {
        return ClickGuiTheme.getDimmedColor(theme.getValue(), rgbSpeed.getFloat(), brightnessFactor);
    }

    public int getDimmedColor(float brightnessFactor, float alpha) {
        int rgb = getDimmedColor(brightnessFactor);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int a = Math.round(Math.max(0, Math.min(255, alpha * 255)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public Color getThemeAwtColor() {
        int rgb = getThemeColor();
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255);
    }

    public void setTheme(String name) {
        theme.setValue(name);
    }

    public ModeSetting getThemeSetting() {
        return theme;
    }

    public float getHue() {
        return ClickGuiTheme.getBaseHue(theme.getValue());
    }

    public float getSaturation() {
        return 0.75f;
    }

    public float getBrightness() {
        return 0.85f;
    }

    public int getColor() {
        return getThemeColor();
    }

    public int getColor(float alpha) {
        return getThemeColor(alpha);
    }

    public int getRed() {
        return (getThemeColor() >> 16) & 0xFF;
    }

    public int getGreen() {
        return (getThemeColor() >> 8) & 0xFF;
    }

    public int getBlue() {
        return getThemeColor() & 0xFF;
    }
}
