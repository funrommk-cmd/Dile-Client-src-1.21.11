package dile.ru.api.drag.impl;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import dile.ru.api.module.impl.combat.AuraModule;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.item.RenderItem;
import dile.ru.utils.render.item.RenderItemOptions;
import dile.ru.utils.render.ui.Render2D;

public final class TargetHud4 extends HudPanel {
    private static final float WIDTH = 95.0F;
    private static final float HEIGHT = 34.0F;
    private static final float FACE_SIZE = 34.0F;
    private static final float HP_BAR_WIDTH = 52.0F;

    private final SmoothAnimation scaleAnimation = new SmoothAnimation();
    private final SmoothAnimation healthAnimation = new SmoothAnimation();
    private final SmoothAnimation absorptionAnimation = new SmoothAnimation();
    private LivingEntity target;
    private boolean forward;

    public TargetHud4() {
        super("targethud4", "Active Target", 140.0F, 130.0F, WIDTH, HEIGHT);
        scaleAnimation.set(0.0);
    }

    @Override
    public void render() {
        LivingEntity activeTarget = activeTarget();
        if (activeTarget == null) {
            scaleAnimation.update();
            scaleAnimation.run(0.0, 0.27, Easings.BACK_OUT, true);
            if (scaleAnimation.get() <= 0.01F && !scaleAnimation.isAlive()) {
                target = null;
                contentVisible(false);
                return;
            }
            contentVisible(scaleAnimation.get() > 0.01F);
            if (target == null) {
                return;
            }
            renderTarget(target);
            return;
        }
        if (!forward) {
            forward = true;
            scaleAnimation.set(0.0);
        }
        scaleAnimation.update();
        scaleAnimation.run(1.0, 0.27, Easings.BACK_OUT, true);
        target = activeTarget;
        contentVisible(true);
        renderTarget(target);
    }

    private void renderTarget(LivingEntity entity) {
        float scale = scaleAnimation.get();
        if (scale <= 0.001F) {
            return;
        }
        float x = drag.x() + (WIDTH - WIDTH * scale) * 0.5F;
        float y = drag.y() + (HEIGHT - HEIGHT * scale) * 0.5F;

        Render2D.rect(x, y, WIDTH - 5.0F, HEIGHT, 3.0F, ColorUtil.rgba(0, 0, 0, 195));

        if (entity instanceof AbstractClientPlayer player) {
            String texture = player.getSkin().body().texturePath().toString();
            int color = ColorUtil.rgba(255, 255, 255, 255);
            Render2D.imageUvNearest(texture, x + 3.0F, y + 4.0F, FACE_SIZE - 7.0F, HEIGHT - 7.0F, 4.0F, 1.0F, 8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F, color);
            Render2D.imageUvNearest(texture, x + 3.0F, y + 4.0F, FACE_SIZE - 7.0F, HEIGHT - 7.0F, 4.0F, 1.0F, 40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F, color);
        }

        float xHP = x + FACE_SIZE + 3.8F;
        float yHP = y + 21.3F;

        healthAnimation.update();
        absorptionAnimation.update();
        float healthProgress = clamp(entity.getHealth() / Math.max(1.0F, entity.getMaxHealth()), 0.0F, 1.0F);
        float absorptionProgress = clamp(entity.getAbsorptionAmount() / Math.max(1.0F, entity.getMaxHealth()), 0.0F, 1.0F);
        healthAnimation.run(healthProgress, 0.25, Easings.EXPO_OUT, true);
        absorptionAnimation.run(absorptionProgress, 0.25, Easings.EXPO_OUT, true);
        float hp = healthAnimation.get();
        float absorption = absorptionAnimation.get();

        int themeColor = ClickGuiModule.getInstance().getColor();
        Render2D.rect(xHP - 5.0F, yHP, HP_BAR_WIDTH * hp, 9.0F, 2.5F, themeColor);
        if (absorption > 0.001F) {
            Render2D.rect(xHP - 5.0F, yHP, HP_BAR_WIDTH * absorption, 9.0F, 2.5F, ColorUtil.rgba(222, 211, 14, 255));
        }

        String targetName = entity.getName().getString();
        Render2D.text(TEXT_FONT, targetName, xHP - 5.0F, yHP - 17.3F, 7.0F, ColorUtil.rgba(255, 255, 255, 255));

        float totalHp = entity.getHealth() + entity.getAbsorptionAmount();
        String hpText = "HP: " + String.valueOf(totalHp);
        if (hpText.length() > 7) {
            hpText = hpText.substring(0, 7);
        }
        Render2D.text(TEXT_FONT, hpText, xHP - 5.0F, yHP - 7.7F, 6.0F, ColorUtil.rgba(255, 255, 255, 255));

        drawItems(x, y - 12.0F);

        size(WIDTH, HEIGHT);
    }

    private void drawItems(float x, float y) {
        if (target == null) {
            return;
        }
        float offset = 10.5F;
        float currentX = x;

        ItemStack mainHand = target.getMainHandItem();
        if (!mainHand.isEmpty()) {
            RenderItem.item(mainHand, currentX, y, 10.5F, RenderItemOptions.decorated(1.0F));
            currentX += offset;
        }
        ItemStack offHand = target.getOffhandItem();
        if (!offHand.isEmpty()) {
            RenderItem.item(offHand, currentX, y, 10.5F, RenderItemOptions.decorated(1.0F));
            currentX += offset;
        }
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                RenderItem.item(stack, currentX, y, 10.5F, RenderItemOptions.decorated(1.0F));
                currentX += offset;
            }
        }
    }

    private LivingEntity activeTarget() {
        if (AuraModule.target != null && AuraModule.target.isAlive()) {
            return AuraModule.target;
        }
        if (mc.screen instanceof ChatScreen && mc.player != null) {
            return mc.player;
        }
        return null;
    }
}
