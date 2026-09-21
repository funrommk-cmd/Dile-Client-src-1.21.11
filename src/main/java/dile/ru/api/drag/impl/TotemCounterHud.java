package dile.ru.api.drag.impl;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.item.RenderItem;
import dile.ru.utils.render.item.RenderItemOptions;
import dile.ru.utils.render.ui.Render2D;

public final class TotemCounterHud extends HudPanel {
    private static final float PAD_X = 5.0F;
    private static final float ITEM_SIZE = 10.0F;
    private static final float PANEL_RADIUS = 4.0F;
    private final SmoothAnimation panelAnimation = new SmoothAnimation();

    public TotemCounterHud() {
        super("totemcounter", "TotemCounter", 100.0F, 400.0F, 50.0F, 18.0F);
    }

    @Override
    public void render() {
        boolean visible = mc.player != null;
        float alpha = contentAlpha(visible);
        if (alpha <= 0.0F) return;

        int totemCount = 0;
        if (mc.player != null) {
            for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.is(Items.TOTEM_OF_UNDYING)) {
                    totemCount += stack.getCount();
                }
            }
        }

        if (totemCount <= 0) {
            contentVisible(false);
            return;
        }

        String countText = "x" + totemCount;
        float textW = Render2D.textWidth(TEXT_FONT, countText, 6.0F);
        float width = Math.max(40.0F, PAD_X + ITEM_SIZE + 4.0F + textW + PAD_X);
        float height = 18.0F;
        size(width, height);

        float x = drag.x();
        float y = drag.y();
        int bgColor = ColorUtil.rgba(30, 25, 40, Math.round(255.0F * alpha));
        int textColor = ColorUtil.rgba(255, 255, 255, Math.round(220.0F * alpha));

        HudRenderCompat.background(x, y, width, height, PANEL_RADIUS, 15.0F, 1.2F, bgColor);

        RenderItem.item(Items.TOTEM_OF_UNDYING.getDefaultInstance(), x + PAD_X, y + (height - ITEM_SIZE) / 2.0F, ITEM_SIZE, RenderItemOptions.noDecorations(alpha));
        Render2D.text(TEXT_FONT, countText, x + PAD_X + ITEM_SIZE + 4.0F, y + (height - 6.0F) / 2.0F + 0.5F, 6.0F, textColor);
    }
}
