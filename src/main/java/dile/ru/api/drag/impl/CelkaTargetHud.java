package dile.ru.api.drag.impl;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import dile.ru.api.module.impl.combat.AuraModule;
import dile.ru.utils.render.animation.modernfx.Easings;
import dile.ru.utils.render.animation.modernfx.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.item.RenderItem;
import dile.ru.utils.render.ui.Render2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CelkaTargetHud extends CelkaHudPanel {
    private static final float WIDTH = 140.0F;
    private static final float HEIGHT = 36.0F;

    private LivingEntity target;
    private final SmoothAnimation animation = new SmoothAnimation();
    private float smoothHealth = 1.0F;

    public CelkaTargetHud() {
        super("targethud", "TargetHud", 10.0F, 300.0F, WIDTH, HEIGHT);
    }

    @Override
    public void render() {
        LivingEntity resolved = resolveTarget();
        if (resolved == null) {
            target = null;
            animation.set(0.0);
            contentVisible(false);
            return;
        }

        if (target != resolved) {
            target = resolved;
            smoothHealth = health(resolved);
        }
        animation.update();
        if (animation.get() <= 0.0F) {
            animation.run(1.0, 0.4, Easings.BACK_OUT);
        }
        float scale = animation.get();
        contentVisible(true);

        smoothHealth = smoothHealth + (health(target) - smoothHealth) * 0.16F;
        float health = clamp(smoothHealth, 0.0F, 1.0F);

        float posX = drag.x();
        float posY = drag.y();
        float centerX = posX + WIDTH / 2.0F;
        float centerY = posY + HEIGHT / 2.0F;

        Render2D.rect(scaledX(posX, centerX, scale), scaledY(posY, centerY, scale), 126.0F * scale, HEIGHT * scale, ColorUtil.rgba(30, 30, 30, 220));

        renderFace(target, posX, posY, centerX, centerY, scale);

        String name = target.getName().getString();
        if (name.length() > 10) {
            name = name.substring(0, 10);
        }
        Render2D.text(CELKA_FONT, name,
                scaledX(posX + 38.0F, centerX, scale), scaledY(posY + 3.0F, centerY, scale),
                8.5F * scale, ColorUtil.WHITE);

        float barX = posX + 39.0F;
        float barY = posY + 11.0F;
        float barWidth = 80.0F;
        float barHeight = 10.0F;

        Render2D.rect(scaledX(barX, centerX, scale), scaledY(barY, centerY, scale),
                barWidth * scale, barHeight * scale, ColorUtil.rgba(0, 0, 0, 90));

        float fillWidth = health * barWidth;
        if (fillWidth > 0.5F) {
            gradientRound(scaledX(barX, centerX, scale), scaledY(barY, centerY, scale),
                    fillWidth * scale, barHeight * scale, 0.0F, style(0), style(90), style(180), style(270));
        }

        String healthText = String.format(Locale.US, "%.1f%%", health * 100.0F);
        float textSize = 7.5F * scale;
        float textWidth = Render2D.textWidth(CELKA_FONT, healthText, textSize);
        Render2D.text(CELKA_FONT, healthText,
                scaledX(barX + barWidth / 2.0F, centerX, scale) - textWidth / 2.0F,
                scaledY(barY + (barHeight - 7.5F) / 2.0F, centerY, scale),
                textSize, ColorUtil.WHITE);

        float equipX = posX + 37.0F;
        float equipY = posY + 21.0F;
        for (ItemStack stack : equipment(target)) {
            RenderItem.item(stack, scaledX(equipX, centerX, scale), scaledY(equipY, centerY, scale), 6.0F * scale);
            equipX += 14.0F;
        }

        size(WIDTH, HEIGHT);
    }

    private LivingEntity resolveTarget() {
        if (AuraModule.target != null && AuraModule.target.isAlive()) {
            return AuraModule.target;
        }
        if (mc.screen instanceof ChatScreen && mc.player != null) {
            return mc.player;
        }
        return null;
    }

    private void renderFace(LivingEntity entity, float posX, float posY, float centerX, float centerY, float scale) {
        float faceSize = 36.0F * scale;
        float faceX = scaledX(posX, centerX, scale);
        float faceY = scaledY(posY, centerY, scale);
        int faceColor = entity.hurtTime > 0 ? ColorUtil.rgba(255, 0, 0, 180) : ColorUtil.WHITE;

        if (entity instanceof AbstractClientPlayer player) {
            String texture = player.getSkin().body().texturePath().toString();
            Render2D.imageUvNearest(texture, faceX, faceY, faceSize, faceSize, 0.0F, 1.0F,
                    8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F, faceColor);
            Render2D.imageUvNearest(texture, faceX, faceY, faceSize, faceSize, 0.0F, 1.0F,
                    40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F, faceColor);
            return;
        }

        Render2D.rect(faceX, faceY, faceSize, faceSize, ColorUtil.rgba(128, 128, 128, 40));
    }

    private float health(LivingEntity entity) {
        if (mc.level != null && mc.level.getScoreboard() != null && entity instanceof Player) {
            Scoreboard scoreboard = mc.level.getScoreboard();
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
            if (objective != null) {
                ReadOnlyScoreInfo info = scoreboard.getPlayerScoreInfo(entity, objective);
                if (info != null) {
                    return info.value() / 20.0F;
                }
            }
        }
        return entity.getHealth() / Math.max(1.0F, entity.getMaxHealth());
    }

    private List<ItemStack> equipment(LivingEntity entity) {
        List<ItemStack> result = new ArrayList<>();
        if (!(entity instanceof Player player)) {
            return result;
        }
        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            result.add(mainHand);
        }
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty()) {
            result.add(offHand);
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.isEmpty()) {
            result.add(head);
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty()) {
            result.add(chest);
        }
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!legs.isEmpty()) {
            result.add(legs);
        }
        ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);
        if (!feet.isEmpty()) {
            result.add(feet);
        }
        return result;
    }

    private static float scaledX(float px, float centerX, float scale) {
        return centerX + (px - centerX) * scale;
    }

    private static float scaledY(float py, float centerY, float scale) {
        return centerY + (py - centerY) * scale;
    }
}
