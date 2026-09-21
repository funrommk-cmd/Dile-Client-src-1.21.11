package dile.ru.screens.clickgui.impl.module;

import dile.ru.api.module.Module;
import dile.ru.api.settings.Setting;
import dile.ru.api.settings.impl.BindSetting;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ButtonSetting;
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.api.settings.impl.StringSetting;
import dile.ru.screens.clickgui.impl.options.BindOption;
import dile.ru.screens.clickgui.impl.options.BooleanOption;
import dile.ru.screens.clickgui.impl.options.ButtonOption;
import dile.ru.screens.clickgui.impl.options.ClickGuiOption;
import dile.ru.screens.clickgui.impl.options.ColorOption;
import dile.ru.screens.clickgui.impl.options.MultiOption;
import dile.ru.screens.clickgui.impl.options.SingleOption;
import dile.ru.screens.clickgui.impl.options.SliderOption;
import dile.ru.screens.clickgui.impl.options.TextOption;

import java.util.ArrayList;
import java.util.List;

public final class ModuleOptionFactory {
    private ModuleOptionFactory() {
    }

    public static ModuleOption create(Module module) {
        return new ModuleOption(module, createSettings(module));
    }

    public static ModuleOption createSettingsOnly(Module module) {
        return new SettingsOnlyModuleOption(module, createSettings(module));
    }

    public static List<ClickGuiOption> createSettings(Module module) {
        List<ClickGuiOption> options = new ArrayList<>();
        List<ClickGuiOption> buttonOptions = new ArrayList<>();

        for (Setting<?> setting : module.getSettings()) {
            ClickGuiOption option = createSettingOption(setting);
            if (option == null) {
                continue;
            }
            option.setSetting(setting);
            if (setting instanceof ButtonSetting) {
                buttonOptions.add(option);
            } else {
                options.add(option);
            }
        }

        options.addAll(buttonOptions);
        return List.copyOf(options);
    }

    private static ClickGuiOption createSettingOption(Setting<?> setting) {
        if (setting instanceof ModeSetting modeSetting) {
            return new SingleOption(modeSetting);
        }
        if (setting instanceof MultiModeSetting multiModeSetting) {
            return new MultiOption(multiModeSetting);
        }
        if (setting instanceof NumberSetting numberSetting) {
            return new SliderOption(numberSetting, "");
        }
        if (setting instanceof BooleanSetting booleanSetting) {
            return new BooleanOption(booleanSetting);
        }
        if (setting instanceof StringSetting stringSetting) {
            return new TextOption(stringSetting);
        }
        if (setting instanceof ColorSetting colorSetting) {
            return new ColorOption(colorSetting);
        }
        if (setting instanceof BindSetting bindSetting) {
            return new BindOption(bindSetting);
        }
        if (setting instanceof ButtonSetting buttonSetting) {
            return new ButtonOption(buttonSetting, "Apply");
        }
        return null;
    }
}
