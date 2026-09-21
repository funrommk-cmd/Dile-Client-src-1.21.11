package dile.ru.screens.clickgui.theme;

import java.awt.Color;

public final class ClickGuiTheme {
    private static final float DEFAULT_CYCLE_SECONDS = 3.0f;
    private static final float SATURATION = 0.75f;
    private static final float BRIGHTNESS_MIN = 0.55f;
    private static final float BRIGHTNESS_MAX = 1.0f;

    private ClickGuiTheme() {
    }

    public static int getCycleColor(String themeName, float speedMultiplier) {
        float hue = getBaseHue(themeName);
        float cycleRange = getCycleRange(themeName);
        float speed = DEFAULT_CYCLE_SECONDS / Math.max(0.1f, speedMultiplier);
        float time = (System.currentTimeMillis() % (long) (speed * 1000L)) / (speed * 1000.0f);
        float wave = (float) (Math.sin(time * Math.PI * 2.0) * 0.5 + 0.5);
        float currentHue = hue + cycleRange * wave;
        currentHue = currentHue - (float) Math.floor(currentHue);
        float brightness = BRIGHTNESS_MIN + (BRIGHTNESS_MAX - BRIGHTNESS_MIN) * wave;
        return Color.HSBtoRGB(currentHue, SATURATION, brightness);
    }

    public static int getAccentColor(String themeName, float speedMultiplier, float alpha) {
        int rgb = getCycleColor(themeName, speedMultiplier);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int a = Math.round(Math.max(0, Math.min(255, alpha * 255)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int getDimmedColor(String themeName, float speedMultiplier, float brightnessFactor) {
        int rgb = getCycleColor(themeName, speedMultiplier);
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
        return Color.HSBtoRGB(hsb[0], hsb[1], Math.max(0, hsb[2] * brightnessFactor));
    }

    public static float getBaseHue(String themeName) {
        return switch (themeName.toLowerCase()) {
            case "green" -> 0.33f;
            case "blue" -> 0.58f;
            case "purple" -> 0.75f;
            case "red" -> 0.0f;
            case "orange" -> 0.08f;
            case "cyan" -> 0.50f;
            case "pink" -> 0.92f;
            case "yellow" -> 0.15f;
            default -> 0.33f;
        };
    }

    public static float getCycleRange(String themeName) {
        return switch (themeName.toLowerCase()) {
            case "rainbow" -> 1.0f;
            case "green", "blue", "purple", "red", "orange", "cyan", "pink", "yellow" -> 0.08f;
            default -> 0.08f;
        };
    }

    public static String[] getThemes() {
        return new String[]{"Green", "Blue", "Purple", "Red", "Orange", "Cyan", "Pink", "Yellow", "Rainbow"};
    }

    public static int getPreviewColor(String themeName) {
        float hue = getBaseHue(themeName);
        return Color.HSBtoRGB(hue, 0.75f, 0.85f);
    }
}
