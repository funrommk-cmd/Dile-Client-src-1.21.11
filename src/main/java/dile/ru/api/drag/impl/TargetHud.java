package dile.ru.api.drag.impl;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import dile.ru.api.module.impl.combat.AuraModule;
import dile.ru.api.module.impl.visual.ESP;
import dile.ru.api.module.impl.visual.esp.EspHealthTracker;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;

public final class TargetHud extends HudPanel {
    private static final float HEAD_X_OFFSET = 5.0F;
    private static final float HEAD_Y_OFFSET = 5.0F;
    private static final float HEAD_SIZE = 21.0F;
    private static final float NAME_SIZE = 7.0F;
    private static final float NAME_GAP = 3.6F;
    private static final float RING_GAP = 3.6F;
    private static final float RING_SIZE = 18.0F;
    private static final float RING_THICKNESS = 2.5F;
    private static final float HP_TEXT_SIZE = 5.5F;
    private static final float RIGHT_PADDING = 3.9F;
    private static final float NAME_Y_LOWER = 3.7F;
    private static final float ORIGINAL_NAME_Y = 6.0F;

    private LivingEntity lastTarget;
    private final SmoothAnimation panelAnimation = new SmoothAnimation();
    private final SmoothAnimation healthAnimation = new SmoothAnimation();
    private final SmoothAnimation healthBarAnimation = new SmoothAnimation();
    private final SmoothAnimation absorptionAnimation = new SmoothAnimation();
    private final EspHealthTracker localHealthTracker = new EspHealthTracker();
    private int animatedTargetId = Integer.MIN_VALUE;
    private int animatedAbsorptionTargetId = Integer.MIN_VALUE;

    public TargetHud() {
        super("targethud", "TargetHud", 140.0F, 130.0F, 110.0F, 31.0F);
    }

    @Override
    public void render() {
        TargetHudState state = logics();
        if (state == null) {
            return;
        }
        renderTargetHud(state);
    }

    private TargetHudState logics() {
        LivingEntity target = target();
        boolean preview = target == null && editPreview() && mc.player != null;
        boolean visible = target != null || preview;

        panelAnimation.update();
        panelAnimation.run(visible ? 1.0 : 0.0, 0.24F, visible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
        float alpha = panelAnimation.get();
        contentVisible(visible || alpha > 0.01F || panelAnimation.isAlive());
        if (alpha <= 0.0F && !panelAnimation.isAlive()) {
            return null;
        }
        if (preview) {
            target = mc.player;
        } else if (target != null) {
            lastTarget = target;
        } else if (lastTarget != null && lastTarget.isAlive()) {
            target = lastTarget;
        } else if (mc.player != null) {
            target = mc.player;
        }
        if (target == null) {
            return null;
        }

        String name = target.getName().getString();
        float nameWidth = Render2D.textWidth(TEXT_FONT, name, NAME_SIZE);
        float contentWidth = HEAD_X_OFFSET + HEAD_SIZE + NAME_GAP + nameWidth + RING_GAP + 6.9F + RING_SIZE + RIGHT_PADDING;
        size(contentWidth, 31.0F);
        return new TargetHudState(target, alpha, drag.x(), drag.y(), drag.width(), drag.height());
    }

    private void renderTargetHud(TargetHudState state) {
        String name = state.target.getName().getString();
        float targetHealth = displayHealth(state.target);
        float targetAbsorption = Math.max(0.0F, state.target.getAbsorptionAmount());
        float maxHealth = Math.max(1.0F, state.target.getMaxHealth());
        maxHealth = Math.max(maxHealth, targetHealth);

        float health = animatedHealth(state.target, targetHealth);
        float healthProgress = clamp(health / maxHealth, 0.0F, 1.0F);
        float absorption = animatedAbsorption(state.target, targetAbsorption);
        float absorptionProgress = clamp(absorption / maxHealth, 0.0F, 1.0F);
        String hp = formatWholeHealth(health);

        float headX = state.x + HEAD_X_OFFSET;
        float headY = state.y + HEAD_Y_OFFSET;
        float nameX = headX + HEAD_SIZE + NAME_GAP;
        float nameY = state.y + ORIGINAL_NAME_Y + NAME_Y_LOWER;
        float nameWidth = Render2D.textWidth(TEXT_FONT, name, NAME_SIZE);
        float nameRight = nameX + nameWidth;

        float ringX = nameRight + RING_GAP + 6.9F;
        float ringCenterX = ringX + RING_SIZE * 0.5F;
        float ringCenterY = state.y + state.height * 0.5F;
        float ringDrawY = ringCenterY - RING_SIZE * 0.5F;

        float hpTextWidth = Render2D.textWidth(TEXT_FONT, hp, HP_TEXT_SIZE);
        float hpTextX = ringCenterX - hpTextWidth * 0.5F;
        float hpTextY = ringCenterY - HP_TEXT_SIZE * 0.5F;

        int bgAlpha = Math.round(255.0F * state.alpha);
        int nameColor = ColorUtil.multAlpha(TEXT_COLOR, state.alpha);
        int hpColor = ColorUtil.rgba(255, 255, 255, bgAlpha);
        int themeColor = ColorUtil.withAlpha(ClickGuiModule.getInstance().getColor(), bgAlpha);
        int ringBgColor = ColorUtil.rgba(60, 60, 60, Math.round(140.0F * state.alpha));
        int absorptionColor = ColorUtil.rgba(255, 220, 60, bgAlpha);

        HudRenderCompat.background(state.x, state.y, state.width, state.height, 6.0F, 15.0F, 1.2F, ColorUtil.rgba(0, 0, 0, bgAlpha));

        renderHead(state, headX, headY);

        Render2D.text(TEXT_FONT, name, nameX, nameY, NAME_SIZE, nameColor);

        Render2D.arc(ringX, ringDrawY, RING_SIZE, RING_THICKNESS, 360.0F, 0.0F, ringBgColor);

        if (healthProgress > 0.001F) {
            float healthDeg = healthProgress * 360.0F;
            float healthRot = -90.0F + healthProgress * 180.0F;
            Render2D.arc(ringX, ringDrawY, RING_SIZE, RING_THICKNESS, healthDeg, healthRot, themeColor);
        }

        if (absorptionProgress > 0.001F) {
            float absDeg = absorptionProgress * 360.0F;
            float absRot = -90.0F + absorptionProgress * 180.0F;
            Render2D.arc(ringX, ringDrawY, RING_SIZE, RING_THICKNESS, absDeg, absRot, absorptionColor);
        }

        Render2D.text(TEXT_FONT, hp, hpTextX, hpTextY, HP_TEXT_SIZE, hpColor);
    }

    private void renderHead(TargetHudState state, float headX, float headY) {
        if (state.target instanceof AbstractClientPlayer player) {
            String texture = player.getSkin().body().texturePath().toString();
            int imageAlpha = Math.round(255.0F * state.alpha);

            if (imageAlpha > 3) {
                int color = ColorUtil.rgba(255, 255, 255, imageAlpha);
                boolean base = renderSkinPart(texture, headX, headY, HEAD_SIZE, 8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F, color);
                boolean overlay = renderSkinPart(texture, headX, headY, HEAD_SIZE, 40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F, color);
                if (!base && !overlay) {
                    Render2D.image(texture, headX, headY, HEAD_SIZE, 6.0F, color);
                }
            }
        } else {
            String targetName = state.target.getName().getString();
            String letter = state.target instanceof Player && !targetName.isEmpty() ? targetName.substring(0, 1).toUpperCase() : "?";
            float tw = Render2D.textWidth(TITLE_FONT, letter, 10.0F);

            Render2D.rect(headX, headY, HEAD_SIZE, HEAD_SIZE, 4.0F, ColorUtil.rgba(128, 128, 128, Math.round(24.0F * state.alpha)));
            Render2D.text(TITLE_FONT, letter, headX + (HEAD_SIZE - tw) * 0.5F + 0.5F, headY + 4.5F, 10.0F, ColorUtil.multAlpha(TEXT_COLOR, state.alpha));
        }
    }

    private float animatedHealth(LivingEntity target, float health) {
        boolean newTarget = target.getId() != animatedTargetId;
        boolean fullHealthCorrection = (health / Math.max(1.0F, target.getMaxHealth())) >= 0.995F && healthBarAnimation.get() < 0.95F;
        if (newTarget || fullHealthCorrection) {
            animatedTargetId = target.getId();
            healthAnimation.set(health);
            healthBarAnimation.set(health / Math.max(1.0F, target.getMaxHealth()));
            return health;
        }

        healthAnimation.update();
        healthBarAnimation.update();
        healthAnimation.run(health, 0.34F, Easings.EXPO_OUT, true);
        healthBarAnimation.run(health / Math.max(1.0F, target.getMaxHealth()), 0.34F, Easings.EXPO_OUT, true);
        return Math.max(0.0F, healthAnimation.get());
    }

    private float animatedAbsorption(LivingEntity target, float absorption) {
        boolean newTarget = target.getId() != animatedAbsorptionTargetId;
        if (newTarget) {
            animatedAbsorptionTargetId = target.getId();
            absorptionAnimation.set(absorption);
            return absorption;
        }

        absorptionAnimation.update();
        boolean hasAbsorption = absorption > 0.05F;
        absorptionAnimation.run(absorption, hasAbsorption ? 0.28F : 0.34F, Easings.EXPO_OUT, true);
        return Math.max(0.0F, absorptionAnimation.get());
    }

    private boolean renderSkinPart(String texture, float x, float y, float size, float u0, float v0, float u1, float v1, int color) {
        if (texture == null || texture.isBlank() || color >>> 24 == 0) {
            return false;
        }
        Render2D.imageUvNearest(texture, x, y, size, size, 4.0F, 1.0F, u0, v0, u1, v1, color);
        return true;
    }

    private float displayHealth(LivingEntity entity) {
        Float espHealth = resolveEspHealth(entity, false);
        if (espHealth != null) {
            return Math.max(0.0F, espHealth);
        }
        return localHealthTracker.resolveDisplayHealth(entity, false);
    }

    private String formatWholeHealth(float health) {
        if (!Float.isFinite(health)) {
            return "0";
        }
        return Integer.toString(Math.max(0, Math.round(health)));
    }

    private Float resolveEspHealth(LivingEntity entity, boolean includeAbsorption) {
        return ESP.resolveHudHealth(entity, includeAbsorption);
    }

    private LivingEntity target() {
        if (AuraModule.target != null && AuraModule.target.isAlive()) {
            return AuraModule.target;
        }
        Entity crosshair = mc.crosshairPickEntity;
        if (crosshair instanceof LivingEntity living && living.isAlive()) {
            return living;
        }
        return null;
    }

    private record TargetHudState(LivingEntity target, float alpha, float x, float y, float width, float height) {
    }
}
