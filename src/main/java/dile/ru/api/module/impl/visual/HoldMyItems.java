package dile.ru.api.module.impl.visual;

import net.minecraft.client.Minecraft;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.NumberSetting;

public class HoldMyItems extends Module {
    private static HoldMyItems INSTANCE;

    public final BooleanSetting swapHands = register(new BooleanSetting("Swap Hands", "Swap left and right hand animations.", false));
    public final NumberSetting smoothness = register(new NumberSetting("Smoothness", "Animation smoothness.", 1.0, 0.35, 2.5, 0.05));
    public final NumberSetting mainHandX = register(new NumberSetting("Main Hand X", "Main hand X offset.", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting mainHandY = register(new NumberSetting("Main Hand Y", "Main hand Y offset.", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting mainHandZ = register(new NumberSetting("Main Hand Z", "Main hand Z offset.", 0.0, -2.5, 2.5, 0.05));
    public final NumberSetting offHandX = register(new NumberSetting("Off Hand X", "Off hand X offset.", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting offHandY = register(new NumberSetting("Off Hand Y", "Off hand Y offset.", 0.0, -1.0, 1.0, 0.05));
    public final NumberSetting offHandZ = register(new NumberSetting("Off Hand Z", "Off hand Z offset.", 0.0, -2.5, 2.5, 0.05));
    public final BooleanSetting climbAndCrawl = register(new BooleanSetting("Climb & Crawl", "Enable climb and crawl animations.", true));
    public final BooleanSetting swimmingAnimation = register(new BooleanSetting("Swimming", "Enable swimming animation.", true));
    public final BooleanSetting mb3DCompat = register(new BooleanSetting("3D Compat", "Enable 3D item compatibility mode.", false));
    public final ModeSetting animationType = register(new ModeSetting("Animation Type", "Sword swing animation type.", "\u0428\u0430\u0440\u043f", "\u0428\u0430\u0440\u043f", "\u041e\u0431\u044b\u0447\u043d\u044b\u0439"));

    public HoldMyItems() {
        super("HoldMyItems", "Custom first-person hand animations.", ModuleCategory.VISUAL);
        INSTANCE = this;
    }

    public static HoldMyItems getInstance() {
        return INSTANCE;
    }
}
