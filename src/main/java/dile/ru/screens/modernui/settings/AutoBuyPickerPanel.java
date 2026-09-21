package dile.ru.screens.modernui.settings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import dile.ru.api.module.impl.player.AHHelper;
import dile.ru.api.module.impl.player.autobuy.AutoBuyCatalog;
import dile.ru.utils.render.item.RenderItem;
import dile.ru.utils.render.item.RenderItemOptions;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.util.ArrayList;
import java.util.List;

final class AutoBuyPickerPanel {
    private static final float TILE_WIDTH = 72.0F;
    private static final float TILE_HEIGHT = 24.0F;
    private static final float TILE_GAP_X = 4.0F;
    private static final float TILE_GAP_Y = 2.0F;
    private static final float HEADER_HEIGHT = 14.0F;
    private static final float GROUP_GAP = 8.0F;
    private static final float ICON_SIZE = 16.0F;
    private static final float PADDING = 4.0F;
    private static final float ENABLE_BAR_HEIGHT = 22.0F;

    private final List<Tile> tiles = new ArrayList<>();
    private float enableX;
    private float enableY;
    private float enableW;
    private float enableH;

    float measure(float width) {
        float height = ENABLE_BAR_HEIGHT + GROUP_GAP;
        int total = 0;
        for (AutoBuyCatalog.Group group : AutoBuyCatalog.groups()) {
            if (total > 0) {
                height += GROUP_GAP;
            }
            height += HEADER_HEIGHT;
            int rows = (int) Math.ceil(group.items().size() / (double) itemsPerRow(width));
            height += rows * (TILE_HEIGHT + TILE_GAP_Y);
            total += group.items().size();
        }
        return height;
    }

    void render(GuiGraphics graphics, SettingCardContext context, float x, float startY, float width, int mouseX, int mouseY, AHHelper module) {
        tiles.clear();
        RenderItem.beginFrame(graphics);

        int itemsPerRow = itemsPerRow(width);
        float startX = x + PADDING;
        float cursorY = startY;

        renderEnableBar(graphics, context, x, startY, width, module, mouseX, mouseY);
        cursorY += ENABLE_BAR_HEIGHT + GROUP_GAP;

        int total = 0;
        for (AutoBuyCatalog.Group group : AutoBuyCatalog.groups()) {
            if (total > 0) {
                cursorY += GROUP_GAP;
            }
            renderHeader(graphics, context, x + PADDING, cursorY, group.header());
            cursorY += HEADER_HEIGHT;

            int column = 0;
            for (AutoBuyCatalog.Entry entry : group.items()) {
                if (column >= itemsPerRow) {
                    column = 0;
                    cursorY += TILE_HEIGHT + TILE_GAP_Y;
                }
                float tileX = startX + column * (TILE_WIDTH + TILE_GAP_X);
                float tileY = cursorY;
                if (tileY + TILE_HEIGHT >= context.moduleClipY - 6.0F && tileY <= context.moduleClipY + context.moduleClipHeight + 6.0F) {
                    boolean hovered = context.inside(mouseX, mouseY, tileX, tileY, TILE_WIDTH, TILE_HEIGHT);
                    renderTile(graphics, context, tileX, tileY, entry, module.isBuyItemSelected(entry.search()), hovered);
                }
                tiles.add(new Tile(tileX, tileY, TILE_WIDTH, TILE_HEIGHT, entry.search()));
                column++;
                total++;
            }
            cursorY += TILE_HEIGHT + TILE_GAP_Y;
        }

        RenderItem.flush();
    }

    boolean mouseClicked(MouseButtonEvent event, SettingCardContext context, AHHelper module) {
        if (module == null) {
            return false;
        }
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && context.inside(event.x(), event.y(), enableX, enableY, enableW, enableH)) {
            module.toggle();
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        for (Tile tile : tiles) {
            if (context.inside(event.x(), event.y(), tile.x, tile.y, tile.w, tile.h)) {
                module.toggleBuyItem(tile.itemId);
                return true;
            }
        }
        return false;
    }

    private int itemsPerRow(float width) {
        return Math.max(1, (int) ((width - PADDING * 2.0F) / (TILE_WIDTH + TILE_GAP_X)));
    }

    private void renderEnableBar(GuiGraphics graphics, SettingCardContext context, float x, float y, float width, AHHelper module, int mouseX, int mouseY) {
        boolean enabled = module.isEnabled();
        boolean hovered = context.inside(mouseX, mouseY, x, y, width, ENABLE_BAR_HEIGHT);
        enableX = x;
        enableY = y;
        enableW = width;
        enableH = ENABLE_BAR_HEIGHT;

        Render2D.rect(x, y, width, ENABLE_BAR_HEIGHT, 6,
                context.withAlpha(SettingCardContext.themeColor(), enabled ? 0.35f : 0.0f),
                context.withAlpha(SettingCardContext.themeLighter(), enabled ? 0.35f : 0.0f),
                context.withAlpha(SettingCardContext.themeColor(), enabled ? 0.35f : 0.0f),
                context.withAlpha(SettingCardContext.themeDarker(), enabled ? 0.35f : 0.0f));
        Render2D.rect(x, y, width, ENABLE_BAR_HEIGHT, 6,
                context.color(125, 125, 125, enabled ? 60 : (hovered ? 80 : 50)),
                context.color(125, 125, 125, enabled ? 60 : (hovered ? 80 : 50)),
                context.color(85, 85, 85, enabled ? 60 : (hovered ? 80 : 50)),
                context.color(85, 85, 85, enabled ? 60 : (hovered ? 80 : 50)));
        Render2D.outline(x, y, width, ENABLE_BAR_HEIGHT, 6, 0.25f,
                context.withAlpha(SettingCardContext.themeLighter(), enabled ? 0.8f : 0.2f),
                context.withAlpha(SettingCardContext.themeLighter(), enabled ? 0.8f : 0.2f),
                context.withAlpha(SettingCardContext.themeColor(), enabled ? 0.8f : 0.2f),
                context.withAlpha(SettingCardContext.themeDarker(), enabled ? 0.8f : 0.2f));

        String label = enabled ? "AUTO BUY - ON" : "AUTO BUY - OFF";
        Render2D.text(FontType.SEMIBOLD, label, x + 10.0F, y + 8.0F, 5.5F, context.color(255, 255, 255, enabled ? 255 : 170));

        float boxX = x + width - 26.0F;
        float boxY = y + (ENABLE_BAR_HEIGHT - 11.0F) * 0.5F;
        if (enabled) {
            Render2D.rect(boxX, boxY, 17.0F, 11.0F, 4,
                    context.withAlpha(SettingCardContext.themeColor(), 1.0f),
                    context.withAlpha(SettingCardContext.themeLighter(), 1.0f),
                    context.withAlpha(SettingCardContext.themeColor(), 1.0f),
                    context.withAlpha(SettingCardContext.themeDarker(), 1.0f));
            Render2D.rect(boxX + 5.0F, boxY + 1.5F, 8.0F, 8.0F, 2.5f, context.color(255, 255, 255, 255));
        } else {
            Render2D.rect(boxX, boxY, 17.0F, 11.0F, 4,
                    context.color(185, 185, 185, 50),
                    context.color(185, 185, 185, 50),
                    context.color(185, 185, 185, 50),
                    context.color(185, 185, 185, 50));
        }
        Render2D.outline(boxX, boxY, 17.0F, 11.0F, 4, 0.25f,
                context.color(255, 255, 255, 100),
                context.color(255, 255, 255, 100),
                context.color(255, 255, 255, 100),
                context.color(255, 255, 255, 100));
    }

    private void renderHeader(GuiGraphics graphics, SettingCardContext context, float x, float y, String text) {
        String label = text.toUpperCase();
        Render2D.text(FontType.SEMIBOLD, label, x, y + 3.0F, 5.0F, context.color(255, 255, 255, 255));
        float width = Render2D.textWidth(FontType.SEMIBOLD, label, 5.0F);
        Render2D.rect(x, y + 10.5F, width + 6.0F, 1.0F, 0.0F,
                context.withAlpha(SettingCardContext.themeColor(), 0.75f),
                context.withAlpha(SettingCardContext.themeLighter(), 0.75f),
                context.withAlpha(SettingCardContext.themeColor(), 0.75f),
                context.withAlpha(SettingCardContext.themeDarker(), 0.75f));
    }

    private void renderTile(GuiGraphics graphics, SettingCardContext context, float x, float y, AutoBuyCatalog.Entry entry, boolean selected, boolean hovered) {
        if (selected) {
            Render2D.rect(x, y, TILE_WIDTH, TILE_HEIGHT, 5,
                    context.withAlpha(SettingCardContext.themeColor(), 0.42f),
                    context.withAlpha(SettingCardContext.themeLighter(), 0.42f),
                    context.withAlpha(SettingCardContext.themeColor(), 0.42f),
                    context.withAlpha(SettingCardContext.themeDarker(), 0.42f));
            Render2D.outline(x, y, TILE_WIDTH, TILE_HEIGHT, 5, 0.3f,
                    context.withAlpha(SettingCardContext.themeLighter(), 0.9f),
                    context.withAlpha(SettingCardContext.themeLighter(), 0.9f),
                    context.withAlpha(SettingCardContext.themeColor(), 0.9f),
                    context.withAlpha(SettingCardContext.themeDarker(), 0.9f));
        } else {
            int alpha = hovered ? 80 : 50;
            Render2D.rect(x, y, TILE_WIDTH, TILE_HEIGHT, 5,
                    context.color(125, 125, 125, alpha),
                    context.color(125, 125, 125, alpha),
                    context.color(85, 85, 85, alpha),
                    context.color(85, 85, 85, alpha));
            Render2D.outline(x, y, TILE_WIDTH, TILE_HEIGHT, 5, 0.2f,
                    context.color(255, 255, 255, hovered ? 90 : 50),
                    context.color(255, 255, 255, hovered ? 90 : 50),
                    context.color(85, 85, 85, hovered ? 110 : 60),
                    context.color(85, 85, 85, hovered ? 110 : 60));
        }

        float iconX = x + 3.0F;
        float iconY = y + (TILE_HEIGHT - ICON_SIZE) * 0.5F;
        RenderItem.item(entry.stack(), iconX, iconY, ICON_SIZE, RenderItemOptions.noDecorations(selected ? 1.0F : 0.9F));

        float nameX = iconX + ICON_SIZE + 4.0F;
        float nameY = y + (TILE_HEIGHT - 4.5F) * 0.5F;
        Render2D.pushScissor(graphics, nameX, nameY - 1.0F, TILE_WIDTH - (nameX - x) - 2.0F, 8.0F);
        Render2D.text(FontType.SEMIBOLD, entry.displayName(), nameX, nameY, 4.5F,
                context.color(225, 225, 225, selected ? 255 : 165));
        Render2D.popScissor(graphics);

        if (selected) {
            Render2D.text(FontType.MAINMENUSCREEN, "Z", x + TILE_WIDTH - 12.0F, y + TILE_HEIGHT - 11.0F, 8, context.color(255, 255, 255, 255));
        }
    }

    private record Tile(float x, float y, float w, float h, String itemId) {
    }
}
