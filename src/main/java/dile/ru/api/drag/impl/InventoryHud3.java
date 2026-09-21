package dile.ru.api.drag.impl;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.item.RenderItem;
import dile.ru.utils.render.item.RenderItemOptions;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

public final class InventoryHud3 extends HudPanel {
    private static final ItemStack[] PREVIEW = {
            Items.ENDER_PEARL.getDefaultInstance(),
            Items.GOLDEN_APPLE.getDefaultInstance(),
            Items.TOTEM_OF_UNDYING.getDefaultInstance(),
            Items.SUGAR.getDefaultInstance()
    };
    private final ItemStack[] inventoryStacks = new ItemStack[27];

    public InventoryHud3() {
        super("inventory", "Inventory", 10.0F, 260.0F, 144.0F, 64.5F);
        for (int i = 0; i < inventoryStacks.length; i++) {
            inventoryStacks[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public void render() {
        InventoryState state = logics();
        if (state == null) {
            return;
        }
        renderInventory(state);
    }

    private InventoryState logics() {
        boolean hasItems = inventoryItems();
        boolean preview = !hasItems && editPreview();
        boolean visible = hasItems || preview;
        float alpha = contentAlpha(visible);
        if (alpha <= 0.0F) {
            return null;
        }

        size(144.0F, 64.5F);

        return new InventoryState(preview ? PREVIEW : inventoryStacks, preview ? PREVIEW.length : inventoryStacks.length, alpha, drag.x(), drag.y(), drag.width(), drag.height());
    }

    private void renderInventory(InventoryState state) {
        float a = state.alpha;
        float headerHeight = 15.0F;
        float headerY = state.y;

        int themeCol = ClickGuiModule.getInstance().getColor();
        int headerTextColor = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * a));
        int bgColor = ColorUtil.rgba(0, 0, 0, Math.round(255.0F * a));

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(state.x, headerY, state.width, state.height)
                .radius(5.0F, 5.0F, 5.0F, 5.0F)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(bgColor)
                .build());

        Render2D.rect(state.x, headerY, state.width, headerHeight, 5.0F, 5.0F, 0.0F, 0.0F, ColorUtil.rgba(0, 0, 0, Math.round(255.0F * a)));

        float headerTextSize = 7.0F;
        float headerTextX = state.x + 4.5F;
        float headerTextY = headerY + (headerHeight - headerTextSize) * 0.5F - 0.5F;
        String title = "Inventory";
        float charX = headerTextX;
        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            int col = ColorUtil.withAlpha(themeCol, Math.round(255.0F * a));
            Render2D.text(TEXT_FONT, ch, charX, headerTextY, headerTextSize, col);
            charX += Render2D.textWidth(TEXT_FONT, ch, headerTextSize);
        }

        float iconSize = 10.0F;
        float iconWidth = Render2D.textWidth(FontType.ICONS_NURIK, "A", iconSize);
        float iconX = state.x + state.width - iconWidth - 4.5F;
        float iconY = headerY + (headerHeight - iconSize) * 0.5F + 0.1F;
        Render2D.text(FontType.ICONS_NURIK, "A", iconX, iconY, iconSize, ColorUtil.withAlpha(themeCol, Math.round(255.0F * a)));

        float slotY = state.y + headerHeight + 1.5F;
        float slotHeight = 16.0F;
        int itemIndex = 0;
        float itemSize = 8.0F;
        int separatorColor = ColorUtil.rgba(255, 255, 255, Math.round(245.0F * a));

        for (int col = 1; col < 9; col++) {
            float lineX = state.x + col * 16.0F - 0.5F;
            Render2D.rect(lineX, slotY + 2.0F, 0.5F, slotHeight - 4.0F, separatorColor);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9 && itemIndex < state.count; col++, itemIndex++) {
                ItemStack stack = state.items[itemIndex];
                float itemX = state.x + col * 16.0F + 8.0F - 4.0F;
                float itemY = slotY + 8.0F - 4.0F;
                RenderItem.item(stack, itemX, itemY, itemSize, RenderItemOptions.countNoDurability(a));
            }
            slotY += slotHeight;
        }
    }

    private boolean inventoryItems() {
        if (mc.player == null) {
            clearInventoryCache();
            return false;
        }
        boolean hasItems = false;
        for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            ItemStack safeStack = stack == null ? ItemStack.EMPTY : stack;
            inventoryStacks[slot - 9] = safeStack;
            if (!safeStack.isEmpty()) {
                hasItems = true;
            }
        }
        return hasItems;
    }

    private void clearInventoryCache() {
        for (int i = 0; i < inventoryStacks.length; i++) {
            inventoryStacks[i] = ItemStack.EMPTY;
        }
    }

    private record InventoryState(ItemStack[] items, int count, float alpha, float x, float y, float width, float height) {
    }
}
