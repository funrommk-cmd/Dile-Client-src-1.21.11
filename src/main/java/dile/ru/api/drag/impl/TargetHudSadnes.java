package dile.ru.api.drag.impl;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dile.ru.api.module.impl.combat.AuraModule;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.api.module.impl.visual.ESP;
import dile.ru.api.module.impl.visual.esp.EspHealthTracker;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.item.RenderItem;
import dile.ru.utils.render.item.RenderItemOptions;
import dile.ru.utils.render.ui.Render2D;

public final class TargetHudSadnes extends HudPanel {
    private static final float HEAD_X = 5.0F;
    private static final float HEAD_Y = 5.0F;
    private static final float HEAD_SIZE = 21.0F;
    private static final float NAME_SIZE = 7.0F;
    private static final float HP_SIZE = 6.0F;
    private static final float NAME_X = 31.0F;
    private static final float TEXT_Y = 4.8F;
    private static final float BAR_Y = 22.1F;
    private static final float BAR_HEIGHT = 6.7F;
    private static final float ARMOR_Y = 16.1F;
    private static final float ARMOR_STEP = 8.0F;
    private static final float HAND_OFFSET = 22.0F;
    private static final float HAND_STEP = 9.0F;
    private static final float PANEL_HEIGHT = 31.0F;
    private static final float NAME_CLIP_WIDTH = 50.0F;
    private static final float ITEM_SCALE = 0.4F;
    private static final float USE_BOX_SIZE = 24.0F;
    private static final float USE_BOX_Y = 4.0F;

    private final SmoothAnimation panelAnim = new SmoothAnimation();
    private final SmoothAnimation hpAnim = new SmoothAnimation();
    private final SmoothAnimation usingAnim = new SmoothAnimation();
    private final EspHealthTracker healthTracker = new EspHealthTracker();

    private LivingEntity lastTarget;
    private int animatedTargetId = Integer.MIN_VALUE;

    public TargetHudSadnes() {
        super("targethudsadnes", "TargetHud", 140.0F, 130.0F, 100.0F, PANEL_HEIGHT);
    }

    @Override
    public void render() {
        LivingEntity target = resolveTarget();
        boolean preview = target == null && editPreview() && mc.player != null;
        boolean visible = target != null || preview;

        panelAnim.update();
        panelAnim.run(visible ? 1.0 : 0.0, 0.24F, visible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
        float alpha = panelAnim.get();
        contentVisible(visible || alpha > 0.01F || panelAnim.isAlive());
        if (alpha <= 0.0F && !panelAnim.isAlive()) {
            return;
        }

        if (preview) {
            target = mc.player;
        } else if (target != null) {
            lastTarget = target;
        }
        if (target == null) {
            return;
        }

        drawMain(target, alpha, visible);
    }

    private void drawMain(LivingEntity target, float alpha, boolean realTarget) {
        String name = target.getName().getString();
        float health = animatedHealth(target);
        String hpText = hpText(target, health);
        float hpTextWidth = Render2D.textWidth(TEXT_FONT, hpText, HP_SIZE);
        float nameWidth = Render2D.textWidth(TEXT_FONT, name, NAME_SIZE);

        float width = Math.max(31.0F + nameWidth + hpTextWidth + 10.0F, 100.0F);
        size(width, PANEL_HEIGHT);

        float x = drag.x();
        float y = drag.y();
        int bgAlpha = Math.round(255.0F * alpha);

        HudRenderCompat.background(x, y, width, PANEL_HEIGHT, 6.0F, 12.0F, 1.2F, ColorUtil.rgba(0, 0, 0, bgAlpha));

        Render2D.rect(
                x,
                y,
                width,
                PANEL_HEIGHT,
                6.0F,
                ColorUtil.rgba(15, 18, 17, Math.round(180.0F * alpha)),
                ColorUtil.rgba(10, 12, 11, Math.round(180.0F * alpha)),
                ColorUtil.rgba(10, 12, 11, Math.round(180.0F * alpha)),
                ColorUtil.rgba(15, 18, 17, Math.round(180.0F * alpha))
        );
        Render2D.outline(x, y, width, PANEL_HEIGHT, 6.0F, 0.5F, ColorUtil.rgba(33, 33, 33, bgAlpha));

        renderHead(target, x + HEAD_X, y + HEAD_Y, alpha);

        String clippedName = name;
        if (nameWidth > NAME_CLIP_WIDTH) {
            clippedName = trimToWidth(name, TEXT_FONT, NAME_SIZE, width - hpTextWidth - 12.0F);
        }
        Render2D.text(TEXT_FONT, clippedName, x + NAME_X, y + TEXT_Y, NAME_SIZE, ColorUtil.rgba(255, 255, 255, bgAlpha));

        float hpX = x + width - hpTextWidth - 5.0F;
        Render2D.text(TEXT_FONT, hpText, hpX, y + TEXT_Y, HP_SIZE, ColorUtil.rgba(110, 235, 130, bgAlpha));

        float maxHealth = Math.max(1.0F, Math.max(target.getMaxHealth(), health));
        float healthProgress = Mth.clamp(health / maxHealth, 0.0F, 1.0F);
        float barX = x + NAME_X;
        float barWidth = width - 36.0F;
        if (realTarget) {
            Render2D.rect(barX, y + BAR_Y, barWidth, BAR_HEIGHT, 2.5F, ColorUtil.rgba(255, 255, 255, 20));
            if (healthProgress > 0.0F) {
                int theme = ClickGuiModule.getInstance().getColor();
                int themeLight = ColorUtil.lerpColor(theme, ColorUtil.rgba(255, 255, 255, 255), 0.35F);
                int themeDark = ColorUtil.lerpColor(theme, ColorUtil.rgba(0, 0, 0, 255), 0.35F);
                Render2D.rect(
                        barX,
                        y + BAR_Y,
                        barWidth * healthProgress,
                        BAR_HEIGHT,
                        2.5F,
                        ColorUtil.withAlpha(themeDark, bgAlpha),
                        ColorUtil.withAlpha(themeLight, bgAlpha),
                        ColorUtil.withAlpha(themeLight, bgAlpha),
                        ColorUtil.withAlpha(themeDark, bgAlpha)
                );
            }
        }

        renderArmor(target, x, y, width, alpha);
        renderUsingItem(target, x, y, alpha);
    }

    private void renderArmor(LivingEntity target, float x, float y, float width, float alpha) {
        float startX = x + NAME_X;
        float startY = y + ARMOR_Y;
        float itemSize = 16.0F * ITEM_SCALE;
        int dotColor = ColorUtil.rgba(90, 110, 115, Math.round(200.0F * alpha));

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            float currentX = startX + i * ARMOR_STEP;
            ItemStack stack = target.getItemBySlot(slots[i]);
            if (!stack.isEmpty()) {
                RenderItem.item(stack, currentX - 1.0F, startY - 2.0F, itemSize, RenderItemOptions.noDecorations(Math.max(alpha, 0.1F)));
            } else {
                Render2D.rect(currentX + 1.0F, startY + 1.0F, 3.5F, 3.5F, 1.5F, dotColor);
            }
        }

        float handX = x + width - HAND_OFFSET;
        drawHandItem(target.getMainHandItem(), handX, startY, itemSize, alpha);
        drawHandItem(target.getOffhandItem(), handX + HAND_STEP, startY, itemSize, alpha);
    }

    private void drawHandItem(ItemStack stack, float x, float y, float itemSize, float alpha) {
        if (stack.isEmpty()) {
            return;
        }
        RenderItem.item(stack, x, y - 2.0F, itemSize, RenderItemOptions.noDecorations(Math.max(alpha, 0.1F)));
    }

    private void renderUsingItem(LivingEntity target, float x, float y, float alpha) {
        boolean using = target.isUsingItem() && !target.getUseItem().isEmpty();
        usingAnim.update();
        usingAnim.run(using ? 1.0 : 0.0, 0.2F, Easings.EXPO_OUT, true);
        float anim = usingAnim.get();
        if (anim <= 0.01F) {
            return;
        }

        ItemStack active = target.getUseItem();
        if (active.isEmpty()) {
            return;
        }

        float boxX = x - (USE_BOX_SIZE + 5.0F) * anim;
        float boxY = y + USE_BOX_Y;

        Render2D.rect(boxX, boxY, USE_BOX_SIZE, USE_BOX_SIZE, 8.0F, ColorUtil.rgba(10, 12, 11, Math.round(220.0F * alpha)));

        int maxUse = Math.max(1, active.getUseDuration(target));
        int remaining = Math.max(0, target.getUseItemRemainingTicks());
        float progress = Mth.clamp((float) (maxUse - remaining) / (float) maxUse, 0.0F, 1.0F);

        int theme = ClickGuiModule.getInstance().getColor();
        int themeDark = ColorUtil.lerpColor(theme, ColorUtil.rgba(0, 0, 0, 255), 0.35F);
        int arcAlpha = Math.round(255.0F * alpha);
        Render2D.arc(
                boxX,
                boxY,
                USE_BOX_SIZE,
                3.5F,
                progress * 360.0F,
                -90.0F,
                ColorUtil.withAlpha(theme, arcAlpha),
                ColorUtil.withAlpha(themeDark, arcAlpha),
                ColorUtil.withAlpha(themeDark, arcAlpha),
                ColorUtil.withAlpha(theme, arcAlpha)
        );

        RenderItem.item(active, boxX + 4.0F, boxY + 4.0F, 16.0F, RenderItemOptions.noDecorations(Math.max(alpha, 0.1F)));
    }

    private void renderHead(LivingEntity target, float headX, float headY, float alpha) {
        if (target instanceof AbstractClientPlayer player) {
            String texture = player.getSkin().body().texturePath().toString();
            int imageAlpha = Math.round(255.0F * alpha);
            if (imageAlpha > 3) {
                int color = ColorUtil.rgba(255, 255, 255, imageAlpha);
                boolean base = renderSkinPart(texture, headX, headY, HEAD_SIZE, 8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F, color);
                boolean overlay = renderSkinPart(texture, headX, headY, HEAD_SIZE, 40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F, color);
                if (!base && !overlay) {
                    Render2D.image(texture, headX, headY, HEAD_SIZE, 6.0F, color);
                }
                if (target.hurtTime > 0) {
                    float hurtPercent = Mth.clamp((target.hurtTime / 10.0F) * 0.55F, 0.0F, 0.55F);
                    int hurtAlpha = Math.round(hurtPercent * 180.0F * alpha);
                    Render2D.rect(headX, headY, HEAD_SIZE, HEAD_SIZE, 5.0F, ColorUtil.rgba(255, 60, 60, hurtAlpha));
                }
            }
        } else {
            String targetName = target.getName().getString();
            String letter = target instanceof Player && !targetName.isEmpty() ? targetName.substring(0, 1).toUpperCase() : "?";
            float tw = Render2D.textWidth(TITLE_FONT, letter, 10.0F);
            Render2D.rect(headX, headY, HEAD_SIZE, HEAD_SIZE, 4.0F, ColorUtil.rgba(128, 128, 128, Math.round(24.0F * alpha)));
            Render2D.text(TITLE_FONT, letter, headX + (HEAD_SIZE - tw) * 0.5F + 0.5F, headY + 4.5F, 10.0F, ColorUtil.multAlpha(TEXT_COLOR, alpha));
        }
    }

    private boolean renderSkinPart(String texture, float x, float y, float size, float u0, float v0, float u1, float v1, int color) {
        if (texture == null || texture.isBlank() || color >>> 24 == 0) {
            return false;
        }
        Render2D.imageUvNearest(texture, x, y, size, size, 4.0F, 1.0F, u0, v0, u1, v1, color);
        return true;
    }

    private float animatedHealth(LivingEntity target) {
        float currentHealth = displayHealth(target);
        boolean newTarget = target.getId() != animatedTargetId;
        if (newTarget) {
            animatedTargetId = target.getId();
            hpAnim.set(currentHealth);
            return currentHealth;
        }
        hpAnim.update();
        hpAnim.run(currentHealth, 0.34F, Easings.EXPO_OUT, true);
        return Math.max(0.0F, hpAnim.get());
    }

    private String hpText(LivingEntity target, float health) {
        if (target.isInvisible()) {
            return "?? HP";
        }
        return Math.round(Math.max(0.0F, health)) + " HP";
    }

    private float displayHealth(LivingEntity entity) {
        Float espHealth = ESP.resolveHudHealth(entity, false);
        if (espHealth != null) {
            return Math.max(0.0F, espHealth);
        }
        return healthTracker.resolveDisplayHealth(entity, false);
    }

    private LivingEntity resolveTarget() {
        if (AuraModule.target != null && AuraModule.target.isAlive()) {
            return AuraModule.target;
        }
        Entity crosshair = mc.crosshairPickEntity;
        if (crosshair instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        return null;
    }
}
