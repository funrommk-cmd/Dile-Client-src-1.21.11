package dile.ru.api.module.impl.visual;

import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.NumberSetting;

public final class ArmorDurability extends Module {
    private static ArmorDurability instance;

    private final NumberSetting greenStrength = register(new NumberSetting("Green Strength", "How strongly armor turns green as its durability drops.", 1.0, 0.0, 1.0, 0.05));

    public ArmorDurability() {
        super("Armor Durability", "Tints armor worn by entities greener as its durability decreases.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static ArmorDurability getInstance() {
        return instance;
    }

    public float getGreenStrength() {
        return greenStrength.getFloat();
    }
}
