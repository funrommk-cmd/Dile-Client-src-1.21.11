package dile.ru.api.drag.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.item.RenderItem;
import dile.ru.utils.render.item.RenderItemOptions;
import dile.ru.utils.render.ui.Render2D;

public final class Armor4 extends HudPanel {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final float ITEM_SIZE = 16.0F;
    private static final float GAP = 2.0F;
    private static final float PAD_X = 5.0F;
    private static final float PAD_Y = 4.0F;
    private static final float TITLE_H = 10.0F;
    private static final float TOTEM_H = 13.0F;
    private static final float WIDTH = PAD_X * 2.0F + ITEM_SIZE * 4.0F + GAP * 3.0F;
    private static final float HEIGHT = PAD_Y * 2.0F + TITLE_H + ITEM_SIZE + TOTEM_H;

    public Armor4() {
        super("armor4", "Armor", defaultX(), defaultY(), WIDTH, HEIGHT);
    }

    private static float defaultX() {
        var window = Minecraft.getInstance().getWindow();
        if (window == null) {
            return 3.0F;
        }
        return (window.getGuiScaledWidth() - WIDTH) / 2.0F;
    }

    private static float defaultY() {
        var window = Minecraft.getInstance().getWindow();
        if (window == null) {
            return 540.0F;
        }
        return window.getGuiScaledHeight() - HEIGHT - 8.0F;
    }

    @Override
    public void render() {
        if (mc.player == null) {
            return;
        }
        float x = drag.x();
        float y = drag.y();

        Render2D.rect(x, y, WIDTH, HEIGHT, 3.0F, ColorUtil.rgba(0, 0, 0, 195));
        String title = "Armor";
        float titleWidth = Render2D.textWidth(TEXT_FONT, title, 6.0F);
        Render2D.text(TEXT_FONT, title, x + (WIDTH - titleWidth) / 2.0F, y + PAD_Y + 1.0F, 6.0F, ColorUtil.rgba(255, 255, 255, 255));

        float itemX = x + PAD_X;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                RenderItem.item(stack, itemX, y + PAD_Y + TITLE_H, ITEM_SIZE, RenderItemOptions.decorated(1.0F));
            }
            itemX += ITEM_SIZE + GAP;
        }

        int totemCount = totemCount();
        RenderItem.item(Items.TOTEM_OF_UNDYING.getDefaultInstance(), x + PAD_X, y + PAD_Y + TITLE_H + ITEM_SIZE + 1.0F, 10.0F, RenderItemOptions.noDecorations(1.0F));
        String totemText = "Totems: " + totemCount;
        Render2D.text(TEXT_FONT, totemText, x + PAD_X + 12.0F, y + PAD_Y + TITLE_H + ITEM_SIZE + 3.5F, 6.0F, ColorUtil.rgba(255, 255, 255, 255));

        size(WIDTH, HEIGHT);
    }

    private int totemCount() {
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
