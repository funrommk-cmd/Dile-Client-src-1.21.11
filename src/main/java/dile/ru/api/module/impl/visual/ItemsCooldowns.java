package dile.ru.api.module.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.DrawEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.Render2DCoordinateSpace;
import dile.ru.utils.render.ui.font.FontType;

import java.util.HashMap;
import java.util.Map;

public final class ItemsCooldowns extends Module {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final float HOTBAR_WIDTH = 182.0f;
    private static final float HOTBAR_HEIGHT = 22.0f;
    private static final float SLOT_WIDTH = 20.0f;
    private static final float ITEM_SIZE = 16.0f;
    private static final float ITEM_PADDING = 3.0f;

    private final NumberSetting alpha = register(new NumberSetting("Alpha", "Cooldown overlay opacity.", 145.0, 50.0, 255.0, 5.0));
    private final BooleanSetting showTimer = register(new BooleanSetting("Timer", "Show remaining cooldown time.", true));
    private final NumberSetting timerSize = register(new NumberSetting("Timer Size", "Cooldown timer text size.", 5.0, 3.0, 8.0, 0.5));

    private final Map<Integer, CooldownInfo> cooldowns = new HashMap<>();

    public ItemsCooldowns() {
        super("Items Cooldowns", "Displays cooldown overlays on hotbar items.", ModuleCategory.VISUAL);
    }

    @Override
    protected void onDisable() {
        cooldowns.clear();
    }

    @SubscribeEvent
    private void onDraw(DrawEvent event) {
        if (mc.player == null || mc.getWindow() == null || mc.options.hideGui) {
            return;
        }

        if (CustomHotbar.isActive() || Hud.shouldRenderCustomHotbar() || Hud2.shouldRenderCustomHotbar()) {
            return;
        }

        GuiGraphics graphics = event.getGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        float hotbarX = (sw - HOTBAR_WIDTH) * 0.5f;
        float hotbarY = sh - HOTBAR_HEIGHT;

        long now = mc.player.tickCount;

        Render2D.beginFrame(graphics);

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                cooldowns.remove(i);
                continue;
            }

            float cooldownPercent = mc.player.getCooldowns().getCooldownPercent(stack, 0.0f);
            if (cooldownPercent <= 0.0f) {
                cooldowns.remove(i);
                continue;
            }

            CooldownInfo info = cooldowns.computeIfAbsent(i, k -> new CooldownInfo());
            info.update(cooldownPercent, now);

            float slotX = hotbarX + i * SLOT_WIDTH;
            float itemX = slotX + ITEM_PADDING;
            float itemY = hotbarY + ITEM_PADDING;

            float overlayHeight = ITEM_SIZE * Math.max(0.0f, Math.min(1.0f, cooldownPercent));
            int overlayAlpha = Math.round(alpha.getFloat());
            Render2D.rect(
                    itemX,
                    itemY + ITEM_SIZE - overlayHeight,
                    ITEM_SIZE,
                    overlayHeight,
                    1.0f,
                    ColorUtil.rgba(0, 0, 0, overlayAlpha)
            );

            if (showTimer.getValue()) {
                String text = info.remainingText();
                float textSize = timerSize.getFloat();
                float textWidth = Render2D.textWidth(FontType.SEMIBOLD, text, textSize);
                float textX = slotX + (SLOT_WIDTH - textWidth) * 0.5f;
                float textY = hotbarY - textSize - 2.0f;

                int bgAlpha = Math.round(160.0f * (overlayAlpha / 255.0f));
                Render2D.rect(
                        textX - 2.0f,
                        textY - 1.0f,
                        textWidth + 4.0f,
                        textSize + 2.0f,
                        2.0f,
                        ColorUtil.rgba(0, 0, 0, bgAlpha)
                );
                Render2D.text(
                        FontType.SEMIBOLD,
                        text,
                        textX,
                        textY,
                        textSize,
                        ColorUtil.rgba(255, 255, 255, Math.round(245.0f * (overlayAlpha / 255.0f)))
                );
            }
        }

        Render2D.flush();
        graphics.nextStratum();
    }

    private static final class CooldownInfo {
        private float lastProgress = -1.0f;
        private long lastTick = -1L;
        private int totalTicks = -1;
        private int remainingTicks;

        private void update(float progress, long tick) {
            progress = Math.max(0.0f, Math.min(1.0f, progress));
            if (lastTick >= 0L && tick > lastTick && lastProgress > progress) {
                float diff = lastProgress - progress;
                if (diff > 0.00001f) {
                    int estimate = Math.round((tick - lastTick) / diff);
                    if (estimate > 0 && estimate < 12000) {
                        totalTicks = totalTicks <= 0 ? estimate : Math.round(totalTicks * 0.75f + estimate * 0.25f);
                    }
                }
            }
            lastProgress = progress;
            lastTick = tick;
            remainingTicks = totalTicks <= 0
                    ? Math.max(1, Math.round(progress * 20.0f))
                    : Math.max(1, Math.round(progress * totalTicks));
        }

        private String remainingText() {
            if (remainingTicks < 0) {
                return "**:**";
            }
            int totalSeconds = Math.max(0, remainingTicks / 20);
            int minutes = Math.min(99, totalSeconds / 60);
            int seconds = totalSeconds % 60;
            return twoDigits(minutes) + ":" + twoDigits(seconds);
        }

        private static String twoDigits(int value) {
            return value < 10 ? "0" + value : String.valueOf(value);
        }
    }
}
