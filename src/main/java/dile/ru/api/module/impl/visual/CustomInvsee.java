package dile.ru.api.module.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

public final class CustomInvsee extends Module {
    private static CustomInvsee instance;
    private static final Minecraft mc = Minecraft.getInstance();

    public CustomInvsee() {
        super("Custom Invsee", "Кастомный и красивый вид открытых инвентарей.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static CustomInvsee getInstance() {
        return instance;
    }

    public static boolean isActive() {
        return instance != null && instance.isEnabled();
    }

    public static void render(GuiGraphics graphics, AbstractContainerScreen<?> screen, int leftPos, int topPos, int imageWidth, int imageHeight) {
        if (mc.player == null || mc.getWindow() == null) {
            return;
        }

        boolean creative = screen instanceof CreativeModeInventoryScreen;

        float padding = creative ? 0.0f : 12.0f;
        float header = creative ? 0.0f : 28.0f;
        float panelX = leftPos - padding;
        float panelY = topPos - header;
        float panelW = imageWidth + padding * 2.0f;
        float panelH = imageHeight + header + padding;

        Render2D.beginFrame(graphics);

        Render2D.blur(panelX, panelY, panelW, panelH, 14.0f, 22.0f, 9.0f, 0xFFFFFFFF);
        Render2D.rect(panelX, panelY, panelW, panelH, 14.0f, 0x5530343A);
        Render2D.rect(panelX, panelY, panelW, panelH, 14.0f, 0x2A000000);

        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) {
                continue;
            }
            float sx = leftPos + slot.x;
            float sy = topPos + slot.y;
            Render2D.rect(sx, sy, 16.0f, 16.0f, 3.0f, 0x4530343A);
        }

        Render2D.flush();
        graphics.nextStratum();

        if (creative) {
            return;
        }

        String title = screen.getTitle().getString();
        if (!title.isEmpty()) {
            float textSize = 11.0f;
            float textWidth = Render2D.textWidth(FontType.SEMIBOLD, title, textSize);
            float tx = leftPos + imageWidth / 2.0f - textWidth / 2.0f;
            float ty = panelY + 7.0f;

            Render2D.beginFrame(graphics);
            Render2D.text(FontType.SEMIBOLD, title, tx + 0.8f, ty + 0.8f, textSize, ColorUtil.multAlpha(0xAA000000, 1.0f));
            Render2D.text(FontType.SEMIBOLD, title, tx, ty, textSize, 0xFFFFFFFF);
            Render2D.flush();
            graphics.nextStratum();
        }
    }
}
