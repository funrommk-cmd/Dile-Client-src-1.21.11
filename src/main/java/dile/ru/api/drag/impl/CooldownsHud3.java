package dile.ru.api.drag.impl;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.item.RenderItem;
import dile.ru.utils.render.item.RenderItemOptions;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CooldownsHud3 extends HudPanel {
    private static final float HEADER_H = 15.0F;
    private static final float ITEM_SPACING = 11.0F;
    private static final float PANEL_RADIUS = 5.0F;
    private static final float MIN_WIDTH = 60.0F;
    private static final float ICON_SIZE = 8.0F;
    private static final float TITLE_SIZE = 7.0F;
    private static final float ITEM_TEXT_SIZE = 6.0F;
    private static final float ANIM_S = 0.24F;

    private static final int BG_COL = ColorUtil.rgba(30, 25, 40, 255);
    private static final int HEADER_COL = ColorUtil.rgba(30, 25, 40, 255);
    private static final int BORDER_COL = ColorUtil.rgba(120, 80, 160, 255);
    private static final int GLOW_COL = ColorUtil.rgba(120, 80, 160, 100);

    private static final ItemStack[] PREVIEW_STACKS = {
            Items.SUGAR.getDefaultInstance(),
            Items.MACE.getDefaultInstance(),
            Items.GOLDEN_APPLE.getDefaultInstance()
    };

    private final Map<CooldownKey, CooldownInfo> infoByItem = new LinkedHashMap<>();
    private final List<RowEntry> rowEntries = new ArrayList<>();
    private final List<CooldownInfo> activeCooldowns = new ArrayList<>();
    private final SmoothAnimation panelAlpha = new SmoothAnimation();
    private final SmoothAnimation heightAnim = new SmoothAnimation();
    private boolean iconAlphaForward = true;

    public CooldownsHud3() {
        super("cooldowns3", "Cooldowns", 10.0F, 40.0F, MIN_WIDTH, HEADER_H + 11.0F);
    }

    @Override
    public void render() {
        List<CooldownInfo> active = collectActive();
        boolean preview = active.isEmpty() && editPreview();
        boolean targetVisible = !active.isEmpty() || preview;

        panelAlpha.update();
        panelAlpha.run(targetVisible ? 1.0 : 0.0, ANIM_S, targetVisible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
        float alpha = panelAlpha.get();

        for (RowEntry entry : rowEntries) {
            entry.active = false;
            entry.alpha.update();
            entry.y.update();
        }

        int targetRows = 0;
        if (preview) {
            RowEntry entry = row("__preview", PREVIEW_STACKS[0], "Sugar", "**:**", 0.0F);
            entry.active = true;
            entry.alpha.run(1.0, ANIM_S, Easings.EXPO_OUT, true);
            entry.y.run(0.0F, ANIM_S, Easings.EXPO_OUT, true);
            targetRows = 1;
        } else {
            for (CooldownInfo info : active) {
                float targetY = targetRows * ITEM_SPACING;
                RowEntry entry = row(info.key.rowKey(), info.stack, info.displayName, info.remainingText(), targetY);
                entry.active = true;
                entry.alpha.run(1.0, ANIM_S, Easings.EXPO_OUT, true);
                entry.y.run(targetY, ANIM_S, Easings.EXPO_OUT, true);
                targetRows++;
            }
        }

        for (RowEntry entry : rowEntries) {
            if (!entry.active) {
                entry.alpha.run(0.0, ANIM_S, Easings.EXPO_IN, true);
            }
        }
        rowEntries.removeIf(e -> !e.active && e.alpha.get() <= 0.01F && !e.alpha.isAlive());

        float targetHeight = Math.max(20.0F, HEADER_H + Math.max(1, targetRows) * ITEM_SPACING);
        heightAnim.update();
        heightAnim.run(targetHeight, ANIM_S, Easings.CUBIC_OUT, true);
        float animatedH = heightAnim.get();

        float width = MIN_WIDTH;
        for (RowEntry entry : rowEntries) {
            if (entry.alpha.get() > 0.01F || entry.active) {
                float nameW = Render2D.textWidth(TEXT_FONT, entry.name, ITEM_TEXT_SIZE);
                float timeW = Render2D.textWidth(TEXT_FONT, entry.time, ITEM_TEXT_SIZE);
                width = Math.max(width, nameW + timeW + 30.0F);
            }
        }

        size(width, animatedH + 2.3F);

        boolean visible = targetVisible || alpha > 0.01F || !rowEntries.isEmpty();
        contentVisible(visible);
        if (!visible) return;

        int themeCol = ClickGuiModule.getInstance().getColor();
        int bgA = Math.round(255.0F * alpha);
        int titleCol = ColorUtil.withAlpha(themeCol, bgA);
        int iconCol = ColorUtil.withAlpha(themeCol, bgA);
        int textCol = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * alpha));

        float x = drag.x();
        float y = drag.y();
        float w = drag.width();
        float h = drag.height();

        int blurBg = ColorUtil.rgba(0, 0, 0, Math.round(bgA * 0.45F));
        int blurHeader = ColorUtil.rgba(0, 0, 0, bgA);

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y, w, h + 2.3F)
                .radius(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurBg)
                .build());

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y + HEADER_H, w, h - HEADER_H + 2.3F)
                .radius(0, 0, PANEL_RADIUS, PANEL_RADIUS)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurBg)
                .build());

        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y, w, HEADER_H)
                .radius(PANEL_RADIUS, PANEL_RADIUS, 0.0F, 0.0F)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurHeader)
                .build());

        String headerText = "Cooldowns";
        Render2D.text(TEXT_FONT, headerText, x + 4.5F, y + (HEADER_H - TITLE_SIZE) * 0.5F - 0.5F, TITLE_SIZE, titleCol);

        String iconChar = "T";
        float iconWidth = Render2D.textWidth(FontType.ICONS_NURIK, iconChar, ICON_SIZE);
        Render2D.text(FontType.ICONS_NURIK, iconChar,
                x + w - iconWidth - 4.5F,
                y + (HEADER_H - ICON_SIZE) * 0.5F + 0.1F,
                ICON_SIZE, iconCol);

        float rowY = y + HEADER_H;
        for (RowEntry entry : rowEntries) {
            float rowAlpha = alpha * entry.alpha.get();
            if (rowAlpha <= 0.01F) continue;

            float currentY = rowY + entry.y.get();
            float itemX = x + 2.5F + entry.xOffset;
            float itemY = currentY + 1.0F;
            float itemSize = 8.0F;
            float nameX = itemX + itemSize + 3.0F;
            float timeX = x + w - 2.5F + entry.xOffset;
            String visibleName = trimToWidth(entry.name, TEXT_FONT, ITEM_TEXT_SIZE, timeX - nameX - 5.0F);

            int itemAlpha = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * rowAlpha));
            RenderItem.item(entry.stack, itemX, itemY, itemSize, RenderItemOptions.noDecorations(rowAlpha));
            Render2D.text(TEXT_FONT, visibleName, nameX, currentY + 4.5F, ITEM_TEXT_SIZE, itemAlpha);
            Render2D.text(TEXT_FONT, entry.time,
                    x + w - 2.5F - Render2D.textWidth(TEXT_FONT, entry.time, ITEM_TEXT_SIZE) + entry.xOffset,
                    currentY + 4.5F, ITEM_TEXT_SIZE, itemAlpha);
        }
    }

    private RowEntry row(String key, ItemStack stack, String name, String time, float targetY) {
        for (RowEntry entry : rowEntries) {
            if (entry.key.equals(key)) {
                entry.stack = stack == null ? ItemStack.EMPTY : stack.copy();
                entry.name = name;
                entry.time = time;
                return entry;
            }
        }
        RowEntry entry = new RowEntry(key, stack, name, time);
        entry.alpha.set(0.0);
        entry.y.set(targetY + 4.0F);
        rowEntries.add(entry);
        return entry;
    }

    private List<CooldownInfo> collectActive() {
        activeCooldowns.clear();
        if (mc.player == null) {
            infoByItem.clear();
            return activeCooldowns;
        }

        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !mc.player.getCooldowns().isOnCooldown(stack)) continue;

            CooldownKey key = CooldownKey.of(stack);
            float progress = mc.player.getCooldowns().getCooldownPercent(stack, 0.0F);
            CooldownInfo info = infoByItem.computeIfAbsent(key, k -> new CooldownInfo());
            info.key = key;
            info.stack = stack.copy();
            info.displayName = displayName(stack);
            info.update(progress, mc.player.tickCount);
            activeCooldowns.add(info);
        }
        infoByItem.entrySet().removeIf(e -> !containsActiveKey(e.getKey()));
        return activeCooldowns;
    }

    private boolean containsActiveKey(CooldownKey key) {
        for (CooldownInfo info : activeCooldowns) {
            if (info.key.equals(key)) return true;
        }
        return false;
    }

    private String displayName(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return switch (id) {
            case "minecraft:ender_eye" -> "Disorientation";
            case "minecraft:sugar" -> "Sugar";
            case "minecraft:netherite_scrap" -> "Trap";
            case "minecraft:dried_kelp" -> "Plast";
            case "minecraft:trident" -> "Trident";
            case "minecraft:mace" -> "Mace";
            case "minecraft:wind_charge" -> "Wind Charge";
            case "minecraft:enchanted_golden_apple" -> "Ench. Gap";
            case "minecraft:golden_apple" -> "Golden Apple";
            default -> stack.getHoverName().getString();
        };
    }

    private String formatCooldownDurationTicks(int ticks) {
        if (ticks < 0) return "**:**";
        int totalSeconds = Math.max(0, ticks / 20);
        int minutes = Math.min(99, totalSeconds / 60);
        int seconds = totalSeconds % 60;
        return twoDigits(minutes) + ":" + twoDigits(seconds);
    }

    private static final class RowEntry {
        private final String key;
        private final SmoothAnimation alpha = new SmoothAnimation();
        private final SmoothAnimation y = new SmoothAnimation();
        private float xOffset = 0.0F;
        private ItemStack stack;
        private String name;
        private String time;
        private boolean active;

        private RowEntry(String key, ItemStack stack, String name, String time) {
            this.key = key;
            this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
            this.name = name;
            this.time = time;
        }
    }

    private final class CooldownInfo {
        private CooldownKey key;
        private ItemStack stack = ItemStack.EMPTY;
        private String displayName = "";
        private float progress;
        private float lastProgress = -1.0F;
        private long lastTick = -1L;
        private int totalTicks = -1;
        private int remainingTicks;

        private void update(float nextProgress, long tick) {
            progress = Math.max(0.0F, Math.min(1.0F, nextProgress));
            if (lastTick >= 0L && tick > lastTick && lastProgress > progress) {
                float diff = lastProgress - progress;
                if (diff > 0.00001F) {
                    int estimate = Math.round((tick - lastTick) / diff);
                    if (estimate > 0 && estimate < 12000) {
                        totalTicks = totalTicks <= 0 ? estimate : Math.round(totalTicks * 0.75F + estimate * 0.25F);
                    }
                }
            }
            lastProgress = progress;
            lastTick = tick;
            remainingTicks = totalTicks <= 0
                    ? Math.max(1, Math.round(progress * 20.0F))
                    : Math.max(1, Math.round(progress * totalTicks));
        }

        private String remainingText() {
            return formatCooldownDurationTicks(remainingTicks);
        }
    }

    private record CooldownKey(Item item, DataComponentMap components, String name) {
        private static CooldownKey of(ItemStack stack) {
            return new CooldownKey(stack.getItem(), stack.immutableComponents(), stack.getHoverName().getString());
        }

        private String rowKey() {
            return BuiltInRegistries.ITEM.getKey(item) + "|" + name + "|" + components.hashCode();
        }
    }
}
