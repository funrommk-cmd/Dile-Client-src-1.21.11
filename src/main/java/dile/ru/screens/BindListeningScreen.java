package dile.ru.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import dile.ru.api.module.Module;
import dile.ru.api.settings.bind.KeyBind;
import dile.ru.manager.Manager;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.util.function.Consumer;

public final class BindListeningScreen extends Screen {
    private static final float TEXT_SIZE = 7.0f;
    private final Module module;
    private final Consumer<KeyBind> callback;

    public BindListeningScreen(Module module, Consumer<KeyBind> callback) {
        super(Component.literal("Bind Listening"));
        this.module = module;
        this.callback = callback;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Render2D.beginFrame(graphics);
        Render2D.rect(-5.0F, -5.0F, width + 10.0F, height + 10.0F, 0.0F, 0xAA000000);
        String text = "Нажмите клавишу";
        float textWidth = Render2D.textWidth(FontType.BOLD, text, TEXT_SIZE);
        float textX = (width - textWidth) / 2.0f;
        float textY = (height - TEXT_SIZE) / 2.0f;
        Render2D.text(FontType.BOLD, text, textX, textY, TEXT_SIZE, 0xFFFFFFFF);
        Render2D.flush();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        callback.accept(KeyBind.keyboard(event.key()));
        close();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        callback.accept(KeyBind.mouse(event.button()));
        close();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void close() {
        minecraft.setScreen(null);
    }
}
