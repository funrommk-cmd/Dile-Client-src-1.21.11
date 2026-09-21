package dile.ru.api.drag.impl;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import dile.ru.api.module.impl.combat.AuraModule;
import dile.ru.api.module.impl.visual.ESP;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public final class TargetHudQuick extends HudPanel {
    private static final float RECT_W = 100.0F;
    private static final float RECT_H = 38.0F;
    private static final float PANEL_RADIUS = 5.5F;
    private static final float HEAD_SIZE = 32.0F;
    private static final float HEAD_PAD = 3.0F;
    private static final float NAME_X_OFFSET = 37.5F;
    private static final float NAME_Y_OFFSET = 4.3F;
    private static final float BAR_X_OFFSET = 37.0F;
    private static final float BAR_Y_OFFSET = 27.6F;
    private static final float BAR_WIDTH = 58.0F;
    private static final float BAR_HEIGHT = 4.0F;
    private static final float BAR_RADIUS = 2.0F;
    private static final float HP_TRI_GAP = 1.0F;
    private static final float HP_NUM_SIZE = 5.5F;
    private static final float NAME_TEXT_SIZE = 7.0F;
    private static final float ANIM_S = 0.24F;

    private static final int BG_COLOR = ColorUtil.rgba(30, 25, 40, 255);

    private final SmoothAnimation panelAnim = new SmoothAnimation();
    private final SmoothAnimation hpAnim = new SmoothAnimation();
    private final SmoothAnimation secondaryHpAnim = new SmoothAnimation();
    private final SmoothAnimation absorptionAnim = new SmoothAnimation();
    private final CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();

    private LivingEntity lastTarget;
    private float lastHurtTime = 0.0F;
    private int animatedTargetId = Integer.MIN_VALUE;

    public TargetHudQuick() {
        super("targethudquick", "TargetHud", 140.0F, 130.0F, RECT_W, RECT_H);
    }

    @Override
    public void render() {
        LivingEntity target = resolveTarget();
        boolean preview = target == null && editPreview() && mc.player != null;
        boolean visible = target != null || preview;

        panelAnim.update();
        panelAnim.run(visible ? 1.0F : 0.0F, ANIM_S, visible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
        float alpha = panelAnim.get();
        contentVisible(visible || alpha > 0.01F || panelAnim.isAlive());
        if (alpha <= 0.0F && !panelAnim.isAlive()) return;

        if (preview) {
            target = mc.player;
        } else if (target != null) {
            lastTarget = target;
        } else if (lastTarget != null && lastTarget.isAlive()) {
            target = lastTarget;
        } else if (mc.player != null) {
            target = mc.player;
        }
        if (target == null) return;

        updateAnimations(target, alpha);
        drawTarget(target, alpha);
        tickParticles(drag.x(), drag.y(), alpha);
    }

    private void updateAnimations(LivingEntity target, float alpha) {
        float currentHP = displayHealth(target);
        float absorption = Math.max(0.0F, target.getAbsorptionAmount());

        boolean newTarget = target.getId() != animatedTargetId;
        if (newTarget) {
            animatedTargetId = target.getId();
            hpAnim.set(currentHP);
            secondaryHpAnim.set(currentHP);
            absorptionAnim.set(absorption);
        }

        hpAnim.update();
        secondaryHpAnim.update();
        absorptionAnim.update();
        hpAnim.run(currentHP, 0.34F, Easings.EXPO_OUT, true);
        secondaryHpAnim.run(currentHP, 0.34F, Easings.EXPO_OUT, true);
        absorptionAnim.run(absorption, 0.34F, Easings.EXPO_OUT, true);

        float hurtTime = target.hurtTime > 0
                ? Math.min(0.5F, (float) target.hurtTime / 10.0F)
                : 0.0F;
        if (hurtTime > lastHurtTime) {
            for (int i = 0; i < 5; i++) {
                particles.add(new Particle(drag.x() + 14.0F, drag.y() + 16.5F));
            }
        }
        lastHurtTime = hurtTime;
    }

    private void drawTarget(LivingEntity target, float alpha) {
        float x = drag.x();
        float y = drag.y();
        float hp = hpAnim.get();
        float secHp = secondaryHpAnim.get();
        float absorption = absorptionAnim.get();
        float maxHP = Math.max(1.0F, target.getMaxHealth());
        String name = target.getName().getString();

        int bgA = Math.round(255.0F * alpha);
        int whiteCol = ColorUtil.rgba(255, 255, 255, bgA);
        int themeCol = ColorUtil.withAlpha(ClickGuiModule.getInstance().getColor(), bgA);

        int blurBg = ColorUtil.rgba(0, 0, 0, bgA);
        HudRenderCompat.background(Render2D.blurBuilder()
                .rectangle(x, y, RECT_W, RECT_H)
                .radius(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS)
                .blurRadius(15.0F)
                .smoothness(1.2F)
                .color(blurBg)
                .build());

        renderHead(target, x + HEAD_PAD, y + HEAD_PAD, alpha);

        Render2D.text(TEXT_FONT, name, x + NAME_X_OFFSET, y + NAME_Y_OFFSET, NAME_TEXT_SIZE, whiteCol);

        drawHpBar(x + BAR_X_OFFSET, y + BAR_Y_OFFSET, BAR_WIDTH, BAR_HEIGHT, BAR_RADIUS,
                hp, secHp, absorption, maxHP, alpha);
    }

    private void drawHpBar(float barX, float barY, float barW, float barH, float radius,
                            float hp, float secHp, float absorption, float maxHP, float alpha) {
        float hpPct = Math.min(hp / maxHP, 1.0F);
        float secPct = Math.min(secHp / maxHP, 1.0F);

        int themeCol = ClickGuiModule.getInstance().getColor();
        int activeColor = themeCol;
        int inactiveColor = ColorUtil.lerpColor(themeCol, ColorUtil.rgba(0, 0, 0, 255), 0.7F);

        int bgA = Math.round(255.0F * alpha);
        int bgBarCol = ColorUtil.withAlpha(inactiveColor, Math.round(255.0F * 0.3F * alpha));
        Render2D.rect(barX, barY, barW, barH, radius, bgBarCol);

        if (hpPct > 0.001F) {
            int mainCol = ColorUtil.withAlpha(activeColor, Math.round(255.0F * alpha));
            Render2D.rect(barX, barY, barW * hpPct, barH, radius, mainCol);
        }
        if (secPct > 0.001F && secPct > hpPct) {
            int secCol = ColorUtil.withAlpha(activeColor, Math.round(255.0F * alpha * 0.75F));
            Render2D.rect(barX, barY, barW * secPct, barH, radius, secCol);
        }

        if (absorption > 0.01F) {
            float absPct = Math.min(absorption / maxHP, 1.0F);
            int goldCol = ColorUtil.rgba(255, 210, 0, Math.round(255.0F * alpha));
            if (absPct > 0.001F) {
                Render2D.rect(barX, barY, barW * absPct, barH, radius, goldCol);
            }
        }

        float minFillEnd = barX + barW * Math.min(2.0F / maxHP, 1.0F);
        float fillEndX = Math.max(barX + barW * hpPct, minFillEnd) - 1.5F;
        float arrowSize = 12.0F;
        float arrowY = barY - arrowSize - 1.0F;
        float arrowW = Render2D.textWidth(FontType.TARGET, "f", arrowSize);
        Render2D.text(FontType.TARGET, "f", fillEndX - arrowW * 0.5F, arrowY, arrowSize, ColorUtil.withAlpha(themeCol, bgA));

        int hpInt = Math.round(hp);
        String hpStr = String.valueOf(hpInt);
        float hpNumW = Render2D.textWidth(TEXT_FONT, hpStr, HP_NUM_SIZE);
        int hpNumCol = ColorUtil.rgba(255, 255, 255, bgA);
        Render2D.text(TEXT_FONT, hpStr, fillEndX - hpNumW * 0.5F, arrowY - HP_NUM_SIZE - HP_TRI_GAP + 4.8F, HP_NUM_SIZE, hpNumCol);
    }

    private void renderHead(LivingEntity target, float headX, float headY, float alpha) {
        if (target instanceof AbstractClientPlayer player) {
            String texture = player.getSkin().body().texturePath().toString();
            int imageAlpha = Math.round(255.0F * alpha);
            if (imageAlpha > 3) {
                int color = ColorUtil.rgba(255, 255, 255, imageAlpha);
                boolean base = renderSkinPart(texture, headX, headY, HEAD_SIZE,
                        8.0F / 64.0F, 8.0F / 64.0F, 16.0F / 64.0F, 16.0F / 64.0F, color);
                boolean overlay = renderSkinPart(texture, headX, headY, HEAD_SIZE,
                        40.0F / 64.0F, 8.0F / 64.0F, 48.0F / 64.0F, 16.0F / 64.0F, color);
                if (!base && !overlay) {
                    Render2D.image(texture, headX, headY, HEAD_SIZE, 6.0F, color);
                }
            }
        } else {
            String targetName = target.getName().getString();
            String letter = target instanceof Player && !targetName.isEmpty()
                    ? targetName.substring(0, 1).toUpperCase() : "?";
            float tw = Render2D.textWidth(TITLE_FONT, letter, 10.0F);
            Render2D.rect(headX, headY, HEAD_SIZE, HEAD_SIZE, 4.0F,
                    ColorUtil.rgba(128, 128, 128, Math.round(24.0F * alpha)));
            Render2D.text(TITLE_FONT, letter,
                    headX + (HEAD_SIZE - tw) * 0.5F + 0.5F, headY + 12.0F, 10.0F,
                    ColorUtil.rgba(255, 255, 255, Math.round(255.0F * alpha)));
        }
    }

    private boolean renderSkinPart(String texture, float x, float y, float size,
                                   float u0, float v0, float u1, float v1, int color) {
        if (texture == null || texture.isBlank() || color >>> 24 == 0) return false;
        Render2D.imageUvNearest(texture, x, y, size, size, 4.0F, 1.0F, u0, v0, u1, v1, color);
        return true;
    }

    private void tickParticles(float hudX, float hudY, float alpha) {
        particles.removeIf(p -> System.currentTimeMillis() - p.startTime > p.lifetime);
        int themeCol = ClickGuiModule.getInstance().getColor();
        for (Particle p : particles) {
            p.update(hudX, hudY);
            float sz = 1.0F - (float) (System.currentTimeMillis() - p.startTime) / (float) p.lifetime;
            float rad = 2.3F;
            int col = ColorUtil.withAlpha(themeCol, Math.round(255.0F * p.progress * sz * alpha));
            Render2D.rect(p.originX - rad, p.originY - rad, rad * 2.0F, rad * 2.0F, rad - 1.0F, col);
        }
    }

    private LivingEntity resolveTarget() {
        if (AuraModule.target != null && AuraModule.target.isAlive()) return AuraModule.target;
        Entity crosshair = mc.crosshairPickEntity;
        if (crosshair instanceof LivingEntity living && living.isAlive()) return living;
        return null;
    }

    private float displayHealth(LivingEntity entity) {
        Float espHealth = ESP.resolveHudHealth(entity, false);
        if (espHealth != null) return Math.max(0.0F, espHealth);
        return Math.max(0.0F, entity.getHealth());
    }

    public static class Particle {
        private float posX, posY;
        private float originX, originY;
        private final float velX, velY;
        private final long startTime;
        private float progress;
        private final long lifetime;

        public Particle(float x, float y) {
            this.posX = 0.0F;
            this.posY = 0.0F;
            this.originX = x;
            this.originY = y;
            this.velX = ThreadLocalRandom.current().nextFloat(-2.0F, 2.0F);
            this.velY = ThreadLocalRandom.current().nextFloat(-2.0F, 2.0F);
            this.startTime = System.currentTimeMillis();
            this.lifetime = 1250L + ThreadLocalRandom.current().nextLong(750L);
        }

        public void update(float hudX, float hudY) {
            progress = Math.min(1.0F, progress + 0.1F);
            posX += velX * 0.75F;
            posY += velY * 0.75F;
            originX = hudX + 14.0F + posX;
            originY = hudY + 16.5F + posY;
        }
    }
}
