package dile.ru.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.blur.BuiltBlur;
import dile.ru.utils.render.ui.font.FontType;
import dile.ru.utils.render.ui.menubackground.MenuBackgroundRenderer;

public final class MainMenuScreen extends Screen {
    private int hoveredButton = -1;

    private static final String[] BUTTON_LABELS = {"Singleplayer", "Multiplayer", "Alt Manager", "Options"};
    private static final float BTN_WIDTH = 200;
    private static final float BTN_HEIGHT = 46;
    private static final float BTN_GAP = 6;
    private static final float BTN_RADIUS = 6;

    public MainMenuScreen() {
        super(Component.literal(""));
        AltManagerScreen.applySavedAlt();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        Render2D.beginFrame(graphics);
        Render2D.image("dile:textures/menu/background.png", -10, -10, width + 20, height + 20, 0, 0xFFFFFFFF);

        MenuBackgroundRenderer renderer = MenuBackgroundRenderer.getInstance();
        float time = (float) (System.nanoTime() / 1_000_000_000.0);
        renderer.setParams(time, (float) mouseX, (float) mouseY,
                (float) Minecraft.getInstance().getWindow().getWidth(),
                (float) Minecraft.getInstance().getWindow().getHeight());
        renderer.enqueue(-10, -10, width + 20, height + 20);

        float centerX = width / 2.0f;
        float startY = height / 2.0f - (BUTTON_LABELS.length * (BTN_HEIGHT + BTN_GAP) - BTN_GAP) / 2.0f;

        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            float bx = centerX - BTN_WIDTH / 2.0f;
            float by = startY + i * (BTN_HEIGHT + BTN_GAP);
            boolean hovered = hoveredButton == i;
            int bg = hovered ? 0xE630304A : 0xCC1A1A2E;

            float radiusTop, radiusBottom;
            switch (i) {
                case 0 -> { radiusTop = 9; radiusBottom = 1; }
                case 1 -> { radiusTop = 1; radiusBottom = 1; }
                case 2 -> { radiusTop = 1; radiusBottom = 1; }
                case 3 -> { radiusTop = 1; radiusBottom = 9; }
                default -> { radiusTop = BTN_RADIUS; radiusBottom = BTN_RADIUS; }
            }

            Render2D.blur(new BuiltBlur(bx, by, BTN_WIDTH, BTN_HEIGHT, radiusTop, radiusTop, radiusBottom, radiusBottom, 1.0f, 12.0f, 0xFFFFFFFF));
            Render2D.rect(bx, by, BTN_WIDTH, BTN_HEIGHT, radiusTop, radiusTop, radiusBottom, radiusBottom, bg);
            Render2D.rect(bx, by, BTN_WIDTH, BTN_HEIGHT, radiusTop, radiusTop, radiusBottom, radiusBottom, 0x22FFFFFF);

            float tw = Render2D.textWidth(FontType.SEMIBOLD, BUTTON_LABELS[i], 20.0f);
            Render2D.text(FontType.SEMIBOLD, BUTTON_LABELS[i], centerX - tw / 2.0f, by + (BTN_HEIGHT - 20.0f) / 2.0f, 20.0f, 0xFFFFFFFF);
        }

        Render2D.flush();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != 0) return super.mouseClicked(event, doubled);

        float centerX = width / 2.0f;
        float startY = height / 2.0f - (BUTTON_LABELS.length * (BTN_HEIGHT + BTN_GAP) - BTN_GAP) / 2.0f;

        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            float bx = centerX - BTN_WIDTH / 2.0f;
            float by = startY + i * (BTN_HEIGHT + BTN_GAP);
            if (event.x() >= bx && event.x() <= bx + BTN_WIDTH && event.y() >= by && event.y() <= by + BTN_HEIGHT) {
                onButtonClick(i);
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
        float centerX = width / 2.0f;
        float startY = height / 2.0f - (BUTTON_LABELS.length * (BTN_HEIGHT + BTN_GAP) - BTN_GAP) / 2.0f;
        hoveredButton = -1;
        for (int i = 0; i < BUTTON_LABELS.length; i++) {
            float bx = centerX - BTN_WIDTH / 2.0f;
            float by = startY + i * (BTN_HEIGHT + BTN_GAP);
            if (mouseX >= bx && mouseX <= bx + BTN_WIDTH && mouseY >= by && mouseY <= by + BTN_HEIGHT) {
                hoveredButton = i;
                break;
            }
        }
    }

    private void onButtonClick(int index) {
        switch (index) {
            case 0 -> minecraft.setScreen(new SelectWorldScreen(this));
            case 1 -> minecraft.setScreen(new JoinMultiplayerScreen(this));
            case 2 -> minecraft.setScreen(new AltManagerScreen(this));
            case 3 -> minecraft.setScreen(new OptionsScreen(this, minecraft.options));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) return false;
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
