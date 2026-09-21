package dile.ru.api.module.impl.misc;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.minecraft.sounds.SoundEvent;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.KeyEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.sounds.SoundManager;

public final class ClickSounds extends Module {
    private final MultiModeSetting soundOn = register(new MultiModeSetting("Звук при", "На каких нажатиях играть звук.", new String[]{"Мышка", "Клава"}, "Мышка", "Клава"));
    private final ModeSetting mouseSound = register(new ModeSetting("Звук мыши", "Звук при клике кнопкой мыши.", "Клавиша", "Клавиша", "Слайм", "Крем", "Капелька"));
    private final ModeSetting keyboardSound = register(new ModeSetting("Звук клавиатуры", "Звук при нажатии клавиши.", "Клавиша", "Клавиша", "Слайм", "Крем", "Капелька"));
    private final NumberSetting mouseVolume = register(new NumberSetting("Громкость мыши", "Громкость звука мыши.", 0.8, 0.0, 1.0, 0.05));
    private final NumberSetting keyboardVolume = register(new NumberSetting("Громкость клавиатуры", "Громкость звука клавиатуры.", 0.8, 0.0, 1.0, 0.05));

    public ClickSounds() {
        super("Click Sounds", "Plays sounds on mouse and keyboard clicks.", ModuleCategory.MISC);
    }

    @SubscribeEvent
    private void onKey(KeyEvent event) {
        if (event.action() != GLFW.GLFW_PRESS) {
            return;
        }
        if (event.type() == InputConstants.Type.MOUSE) {
            if (soundOn.isSelected("Мышка")) {
                SoundManager.playSoundDirect(resolveSound(mouseSound), mouseVolume.getFloat(), 1.0F);
            }
        } else if (event.type() == InputConstants.Type.KEYSYM) {
            if (soundOn.isSelected("Клава")) {
                SoundManager.playSoundDirect(resolveSound(keyboardSound), keyboardVolume.getFloat(), 1.0F);
            }
        }
    }

    private SoundEvent resolveSound(ModeSetting setting) {
        if (setting.is("Слайм")) {
            return SoundManager.CLICK_SLIME;
        }
        if (setting.is("Крем")) {
            return SoundManager.CLICK_CREAM;
        }
        if (setting.is("Капелька")) {
            return SoundManager.CLICK_WATER;
        }
        return SoundManager.CLICK_KEY;
    }
}
