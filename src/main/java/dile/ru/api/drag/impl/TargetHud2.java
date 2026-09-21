package dile.ru.api.drag.impl;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
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
import dile.ru.utils.render.ui.font.FontType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class TargetHud2 extends HudPanel {
    private static final FontType FONT = FontType.SEMIBOLD;
    private static final float HEAD_SIZE = 20f;
    private static final float PADDING = 4.5f;
    private static final float GAP = 7f;
    private static final float RIGHT_PAD = 7f;
    private static final float BAR_HEIGHT = 4f;
    private static final float NAME_SIZE = 7f;
    private static final float HP_SIZE = 6f;
    private static final float SLOT_SIZE = 12f;
    private static final float SLOT_GAP = 2f;
    private static final float SLOT_PAD = 2.5f;
    private static final float SLOT_RAD = 2f;
    private static final float CONT_RAD = 3f;

    private final SmoothAnimation alphaAnim = new SmoothAnimation();
    private final SmoothAnimation hpAnim = new SmoothAnimation();
    private final SmoothAnimation hpTrailAnim = new SmoothAnimation();
    private final SmoothAnimation hpValueAnim = new SmoothAnimation();
    private final SmoothAnimation abValueAnim = new SmoothAnimation();
    private final SmoothAnimation goldenAlphaAnim = new SmoothAnimation();
    private final SmoothAnimation goldenHpAnim = new SmoothAnimation();

    private LivingEntity lastTarget;
    private float maxAbsorption = 20.0f;
    private int lastTargetHurtTime;
    private int animatedTargetId = Integer.MIN_VALUE;
    private final EspHealthTracker healthTracker = new EspHealthTracker();
    private final List<HeadParticle> headParticles = new ArrayList<>();
    private long lastParticleUpdateNs = System.nanoTime();
    private LivingEntity particleTarget;
    private int cachedBarThemeColor = ColorUtil.rgba(124, 91, 242, 255);

    public TargetHud2() {
        super("targethud2", "TargetHud", 160.0F, 130.0F, 120.0F, 31.0F);
    }

    @Override
    public void render() {
        if (mc.player == null) {
            headParticles.clear();
            lastTargetHurtTime = 0;
            drag.size(0, 0);
            return;
        }

        boolean chatOpen = mc.screen instanceof ChatScreen;
        LivingEntity auraTarget = AuraModule.target;
        boolean showTargetHud = chatOpen || auraTarget != null;

        alphaAnim.update();
        alphaAnim.run(showTargetHud ? 1.0 : 0.0, showTargetHud ? 0.12 : 0.2, showTargetHud ? Easings.QUAD_OUT : Easings.QUAD_OUT, true);
        float alpha = Mth.clamp(alphaAnim.get(), 0.0f, 1.0f);

        if (showTargetHud) {
            lastTarget = chatOpen ? mc.player : auraTarget;
        }

        LivingEntity target = showTargetHud ? (chatOpen ? mc.player : auraTarget) : lastTarget;
        if (target == null || alpha <= 0.01f) {
            headParticles.clear();
            lastTargetHurtTime = 0;
            drag.size(0, 0);
            goldenAlphaAnim.set(0.0);
            abValueAnim.set(0.0);
            goldenHpAnim.set(0.0);
            return;
        }

        float currentAbsorption = Math.max(0.0f, target.getAbsorptionAmount());
        if (currentAbsorption > maxAbsorption) maxAbsorption = currentAbsorption;

        float maxHealth = Math.max(1.0f, target.getMaxHealth());
        float rawHealth = displayHealth(target);

        hpValueAnim.update();
        hpValueAnim.run(showTargetHud ? rawHealth : 0.0, 0.15, Easings.QUAD_OUT, true);
        float animHealth = Mth.clamp(hpValueAnim.get(), 0.0f, maxHealth);

        float healthProgress = Mth.clamp(rawHealth / maxHealth, 0.0f, 1.0f);
        hpAnim.update();
        hpAnim.run(healthProgress, 0.11, Easings.QUAD_OUT, true);
        float hpFill = Mth.clamp(hpAnim.get(), 0.0f, 1.0f);

        if (hpFill > hpTrailAnim.get()) {
            hpTrailAnim.set(Mth.lerp(0.78f, hpTrailAnim.get(), hpFill));
        } else {
            hpTrailAnim.update();
            hpTrailAnim.run(hpFill, 0.14, Easings.QUAD_OUT, true);
        }
        float hpTrail = Mth.clamp(hpTrailAnim.get(), 0.0f, 1.0f);
        if (!showTargetHud) hpTrail = hpFill;

        int colorTheme = ClickGuiModule.getInstance().getColor();
        if (showTargetHud) cachedBarThemeColor = colorTheme;
        int barColor = colorTheme;

        String name = target.getName().getString();
        String hpText = "HP: " + (int) animHealth;

        float x = drag.x();
        float y = drag.y();
        float textW = Math.max(Render2D.textWidth(FONT, name, NAME_SIZE), Render2D.textWidth(FONT, hpText, HP_SIZE));
        float width = Math.max(90f, PADDING + HEAD_SIZE + GAP + textW + RIGHT_PAD);
        float height = HEAD_SIZE + PADDING * 2f;
        size(width, height);

        float headX = x + PADDING;
        float headY = y + PADDING;
        float textX = headX + HEAD_SIZE + GAP - 2f;
        float nameY = y + 2.7f;
        float hpTextY = nameY + 9f;
        float barY = headY + HEAD_SIZE - BAR_HEIGHT - 0.5f;
        float barW = width - (textX - x) - RIGHT_PAD;

        int drawAlpha = (int) (255 * alpha);
        int bgColor = ColorUtil.rgba(20, 20, 25, (int) (240 * alpha));
        int slotBg = ColorUtil.rgba(10, 10, 14, (int) (210 * alpha));
        int slotBorder = ColorUtil.rgba(255, 255, 255, (int) (38 * alpha));
        int containerBg = ColorUtil.rgba(20, 20, 25, (int) (230 * alpha));

        contentVisible(showTargetHud || alpha > 0.01f || alphaAnim.isAlive());

        HudRenderCompat.background(x, y, width, height, 6.0f, 15.0f, 1.2f, ColorUtil.rgba(0, 0, 0, (int) (255 * alpha)));
        Render2D.rect(x, y, width, height, 6.0f, bgColor);

        if (dile.ru.api.module.impl.visual.Hud2.accentEnabled()) {
            float lineW = 14f;
            float lineH = 3.5f;
            Render2D.rect(x + width - lineW - 5f, y - 0.5f, lineW, lineH, 1.5f, ColorUtil.multAlpha(barColor, alpha));
        }

        float hurtPercent = 0f;
        if (target.hurtTime > 0) {
            hurtPercent = Mth.clamp((target.hurtTime / 10.0f) * 0.55f, 0f, 0.55f);
        }
        renderHead(target, headX, headY, alpha, hurtPercent);

        Render2D.text(FONT, name, textX, nameY, NAME_SIZE, ColorUtil.rgba(255, 255, 255, drawAlpha));
        Render2D.text(FONT, hpText, textX, hpTextY, HP_SIZE, ColorUtil.rgba(160, 165, 175, drawAlpha));

        goldenAlphaAnim.update();
        goldenAlphaAnim.run(currentAbsorption > 0 ? 1.0 : 0.0, currentAbsorption > 0 ? 0.12 : 0.2, Easings.QUAD_OUT, true);
        float goldenAlpha = Mth.clamp(goldenAlphaAnim.get(), 0.0f, 1.0f);

        if (goldenAlpha > 0.01f) {
            int abVal = (int) currentAbsorption;
            String abText = "+" + abVal + "AB";
            float abTW = Render2D.textWidth(FONT, abText, HP_SIZE);
            float abX = textX + barW - abTW;
            int gAlpha = (int) (255 * goldenAlpha * alpha);
            int left = ColorUtil.rgba(236, 183, 39, gAlpha);
            int right = ColorUtil.rgba(200, 140, 20, gAlpha);
            Render2D.text(FONT, abText, abX, hpTextY, HP_SIZE, left, right, right, left);
        }

        int barBg = ColorUtil.rgba(40, 42, 55, (int) (180 * alpha));
        Render2D.rect(textX, barY, barW, BAR_HEIGHT, 1.5f, barBg);

        float goldenReservedW = 0f;
        if (goldenAlpha > 0.01f && currentAbsorption > 0) {
            abValueAnim.update();
            abValueAnim.run(showTargetHud ? currentAbsorption : 0f, 0.15, Easings.QUAD_OUT, true);
            float maxAB = Math.max(1.0f, maxAbsorption);
            goldenHpAnim.update();
            goldenHpAnim.run(Mth.clamp(currentAbsorption / maxAB, 0f, 1f), 0.11, Easings.QUAD_OUT, true);
            float goldenFill = Mth.clamp(goldenHpAnim.get(), 0f, 1f);
            goldenReservedW = barW * goldenFill;
            if (goldenReservedW > 1f) {
                float goldenX = textX + barW - goldenReservedW;
                int goldenL = ColorUtil.multAlpha(ColorUtil.rgba(147, 108, 16, 255), goldenAlpha * alpha);
                int goldenR = ColorUtil.multAlpha(ColorUtil.rgba(236, 183, 39, 255), goldenAlpha * alpha);
                Render2D.rect(goldenX, barY, goldenReservedW, BAR_HEIGHT, 1.5f, goldenL, goldenR, goldenR, goldenL);
            }
        }

        float hpZoneW = barW - goldenReservedW;
        float trailW = hpZoneW * hpTrail;
        if (showTargetHud && trailW > 1f) {
            int trailL = ColorUtil.multAlpha(barColor, alpha * 0.5f);
            int trailR = ColorUtil.multAlpha(darken(barColor, 0.5f), alpha * 0.5f);
            Render2D.rect(textX, barY, trailW, BAR_HEIGHT, 1.5f, trailL, trailR, trailR, trailL);
        }

        float fillW = hpZoneW * hpFill;
        if (fillW > 1f) {
            int fillL = ColorUtil.multAlpha(darken(barColor, 0.5f), alpha);
            int fillR = ColorUtil.multAlpha(barColor, alpha);
            Render2D.rect(textX, barY, fillW, BAR_HEIGHT, 1.5f, fillL, fillR, fillR, fillL);
        }

        float itemY = y + height + 3f;
        float itemScale = 0.52f * alpha;
        if (itemScale > 0.01f) {
            ItemStack mainHand = target.getMainHandItem();
            ItemStack offHand = target.getOffhandItem();

            float armorContW = SLOT_PAD * 2 + 4 * SLOT_SIZE + 3 * SLOT_GAP;
            float armorContH = SLOT_PAD * 2 + SLOT_SIZE;
            float armorContX = x - 4f;

            HudRenderCompat.background(armorContX, itemY, armorContW, armorContH, CONT_RAD, 10f, 1.2f, ColorUtil.rgba(0, 0, 0, (int) (200 * alpha)));
            Render2D.rect(armorContX, itemY, armorContW, armorContH, CONT_RAD, containerBg);

            int idx = 0;
            EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            for (EquipmentSlot slot : armorSlots) {
                ItemStack stack = target.getItemBySlot(slot);
                if (idx >= 4) break;
                float sx = armorContX + SLOT_PAD + idx * (SLOT_SIZE + SLOT_GAP);
                float sy = itemY + SLOT_PAD;
                Render2D.rect(sx, sy, SLOT_SIZE, SLOT_SIZE, SLOT_RAD, slotBg);
                Render2D.outline(sx, sy, SLOT_SIZE, SLOT_SIZE, SLOT_RAD, SLOT_RAD, SLOT_RAD, SLOT_RAD, 0.5f, slotBorder, slotBorder, slotBorder, slotBorder);
                drawItem(stack, sx, sy, SLOT_SIZE, itemScale);
                idx++;
            }

            float handContW = SLOT_PAD * 2 + 2 * SLOT_SIZE + SLOT_GAP;
            float handContX = armorContX + armorContW + 8f;

            HudRenderCompat.background(handContX, itemY, handContW, armorContH, CONT_RAD, 10f, 1.2f, ColorUtil.rgba(0, 0, 0, (int) (200 * alpha)));
            Render2D.rect(handContX, itemY, handContW, armorContH, CONT_RAD, containerBg);

            ItemStack[] hands = {mainHand, offHand};
            for (int i = 0; i < 2; i++) {
                float sx = handContX + SLOT_PAD + i * (SLOT_SIZE + SLOT_GAP);
                float sy = itemY + SLOT_PAD;
                Render2D.rect(sx, sy, SLOT_SIZE, SLOT_SIZE, SLOT_RAD, slotBg);
                Render2D.outline(sx, sy, SLOT_SIZE, SLOT_SIZE, SLOT_RAD, SLOT_RAD, SLOT_RAD, SLOT_RAD, 0.5f, slotBorder, slotBorder, slotBorder, slotBorder);
                if (!hands[i].isEmpty()) {
                    drawItem(hands[i], sx, sy, SLOT_SIZE, itemScale);
                }
            }
        }

        updateAndRenderHeadParticles(target, headX, headY, HEAD_SIZE, alpha, barColor);
    }

    private void renderHead(LivingEntity target, float headX, float headY, float alpha, float hurtPercent) {
        if (target instanceof AbstractClientPlayer player) {
            String texture = player.getSkin().body().texturePath().toString();
            int imageAlpha = Math.round(255.0f * alpha);
            if (imageAlpha > 3) {
                int color = ColorUtil.rgba(255, 255, 255, imageAlpha);
                boolean base = renderSkinPart(texture, headX, headY, HEAD_SIZE, 8.0f / 64.0f, 8.0f / 64.0f, 16.0f / 64.0f, 16.0f / 64.0f, color);
                boolean overlay = renderSkinPart(texture, headX, headY, HEAD_SIZE, 40.0f / 64.0f, 8.0f / 64.0f, 48.0f / 64.0f, 16.0f / 64.0f, color);
                if (!base && !overlay) {
                    Render2D.image(texture, headX, headY, HEAD_SIZE, 6.0f, color);
                }
                if (hurtPercent > 0.01f) {
                    int hurtAlpha = Math.round(hurtPercent * 180.0f * alpha);
                    Render2D.rect(headX, headY, HEAD_SIZE, HEAD_SIZE, 5.0f, ColorUtil.rgba(255, 60, 60, hurtAlpha));
                }
            }
        } else {
            String targetName = target.getName().getString();
            String letter = !targetName.isEmpty() ? targetName.substring(0, 1).toUpperCase() : "?";
            float tw = Render2D.textWidth(FONT, letter, 10.0f);
            Render2D.rect(headX, headY, HEAD_SIZE, HEAD_SIZE, 4.0f, ColorUtil.rgba(128, 128, 128, Math.round(24.0f * alpha)));
            Render2D.text(FONT, letter, headX + (HEAD_SIZE - tw) * 0.5f + 0.5f, headY + 4.5f, 10.0f, ColorUtil.multAlpha(TEXT_COLOR, alpha));
        }
    }

    private boolean renderSkinPart(String texture, float x, float y, float size, float u0, float v0, float u1, float v1, int color) {
        if (texture == null || texture.isBlank() || color >>> 24 == 0) return false;
        Render2D.imageUvNearest(texture, x, y, size, size, 4.0f, 1.0f, u0, v0, u1, v1, color);
        return true;
    }

    private void drawItem(ItemStack stack, float slotX, float slotY, float slotSize, float itemScale) {
        if (stack.isEmpty()) return;
        float itemScreenSize = 16f * itemScale;
        float offsetX = slotX + (slotSize - itemScreenSize) / 2f;
        float offsetY = slotY + (slotSize - itemScreenSize) / 2f;
        RenderItem.item(stack, offsetX, offsetY, itemScreenSize, RenderItemOptions.noDecorations(itemScale));
    }

    private float displayHealth(LivingEntity entity) {
        Float espHealth = ESP.resolveHudHealth(entity, false);
        if (espHealth != null) return Math.max(0.0f, espHealth);
        return healthTracker.resolveDisplayHealth(entity, false);
    }

    private void updateAndRenderHeadParticles(LivingEntity target, float headX, float headY, float headSize, float alpha, int themeColor) {
        if (target == null || alpha <= 0.02f) {
            headParticles.clear();
            particleTarget = target;
            lastTargetHurtTime = 0;
            return;
        }

        long now = System.nanoTime();
        float deltaTicks = Mth.clamp((now - lastParticleUpdateNs) / 1_000_000_000.0f * 60.0f, 0.2f, 3.0f);
        lastParticleUpdateNs = now;

        if (particleTarget != target) {
            headParticles.clear();
            particleTarget = target;
            lastTargetHurtTime = Math.max(0, target.hurtTime);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        float centerX = headX + headSize * 0.5f;
        float centerY = headY + headSize * 0.5f;
        int hurtTime = Math.max(0, target.hurtTime);
        boolean spawnBurst = hurtTime > 0 && (hurtTime > lastTargetHurtTime || hurtTime % 3 == 0);
        lastTargetHurtTime = hurtTime;

        if (spawnBurst) {
            int burstCount = 1 + random.nextInt(2);
            for (int n = 0; n < burstCount && headParticles.size() < 14; n++) {
                float angle = (float) (random.nextDouble() * Math.PI * 2.0);
                float radius = random.nextFloat() * headSize * 0.24f;
                float spreadAngle = (float) (random.nextDouble() * Math.PI * 2.0);
                float speed = 0.58f + random.nextFloat() * 0.9f;

                HeadParticle p = new HeadParticle();
                p.x = centerX + (float) Math.cos(angle) * radius;
                p.y = centerY + (float) Math.sin(angle) * radius;
                p.vx = (float) Math.cos(spreadAngle) * speed + (p.x - centerX) * 0.025f;
                p.vy = (float) Math.sin(spreadAngle) * speed + (p.y - centerY) * 0.025f;
                p.size = 3.8f + random.nextFloat() * 1.4f;
                p.age = 0.0f;
                p.maxAge = 74.0f + random.nextFloat() * 42.0f;
                headParticles.add(p);
            }
        }

        for (int i = headParticles.size() - 1; i >= 0; i--) {
            HeadParticle p = headParticles.get(i);
            p.age += deltaTicks;
            if (p.age >= p.maxAge) {
                headParticles.remove(i);
                continue;
            }
            p.x += p.vx * deltaTicks;
            p.y += p.vy * deltaTicks;
            p.vx *= (float) Math.pow(0.975f, deltaTicks);
            p.vy *= (float) Math.pow(0.975f, deltaTicks);
            p.vy += 0.0012f * deltaTicks;

            float life = 1.0f - (p.age / p.maxAge);
            float smoothLife = life * life * (3.0f - 2.0f * life);
            float particleAlpha = alpha * smoothLife;
            if (particleAlpha <= 0.02f) continue;

            float drawX = p.x - p.size * 0.5f;
            float drawY = p.y - p.size * 0.5f;
            int coreColor = ColorUtil.multAlpha(themeColor, particleAlpha * 0.58f);
            Render2D.rect(drawX, drawY, p.size, p.size, p.size * 0.45f, coreColor);
        }
    }

    private static int darken(int color, float factor) {
        int r = Math.round(ColorUtil.getRed(color) * factor);
        int g = Math.round(ColorUtil.getGreen(color) * factor);
        int b = Math.round(ColorUtil.getBlue(color) * factor);
        return ColorUtil.rgba(r, g, b, ColorUtil.getAlpha(color));
    }

    private static final class HeadParticle {
        float x, y, vx, vy, size, age, maxAge;
    }
}
