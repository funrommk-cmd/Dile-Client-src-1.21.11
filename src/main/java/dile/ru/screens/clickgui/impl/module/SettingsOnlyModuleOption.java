package dile.ru.screens.clickgui.impl.module;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import dile.ru.api.module.Module;
import dile.ru.screens.clickgui.impl.options.ClickGuiOption;

import java.util.List;

public final class SettingsOnlyModuleOption extends ModuleOption {
    public SettingsOnlyModuleOption(Module module, List<ClickGuiOption> settings) {
        super(module, settings);
        revealSettings();
    }

    @Override
    public void renderRow(float panelX, float rowY, float panelWidth, float rowHeight, float scale, float alpha) {
    }

    @Override
    public boolean mouseClickedRow(MouseButtonEvent event, boolean doubled) {
        return false;
    }
}
