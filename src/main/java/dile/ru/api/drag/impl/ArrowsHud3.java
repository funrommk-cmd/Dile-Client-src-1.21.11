package dile.ru.api.drag.impl;

import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.utils.render.animation.Easings;
import dile.ru.utils.render.animation.SmoothAnimation;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.render.ui.font.FontType;
import dile.ru.utils.repository.friend.FriendUtils;

import java.util.ArrayList;
import java.util.List;

public final class ArrowsHud3 extends HudPanel {
    private static final String ARROW_TEXTURE = "dile:textures/particles/ghost-triangle.png";
    private static final float DEFAULT_DISTANCE = 20.0F;
    private static final float DEFAULT_SIZE = 15.0F;
    private static final float SIZE_ANIM_SPEED = 1.0F;

    private final SmoothAnimation distanceAnimation = new SmoothAnimation();
    private final SmoothAnimation yawAnimation = new SmoothAnimation();
    private final SmoothAnimation pitchAnimation = new SmoothAnimation();
    private final SmoothAnimation cameraYawAnimation = new SmoothAnimation();
    private final List<ArrowEntry> arrows = new ArrayList<>();

    public ArrowsHud3() {
        super("arrows", "Arrows", 10.0F, 150.0F, 20.0F, 20.0F);
    }

    @Override
    public void render() {
        boolean visible = mc.player != null && mc.level != null && mc.options.getCameraType() == CameraType.FIRST_PERSON;
        float alpha = contentAlpha(visible);
        if (alpha <= 0.0F) {
            return;
        }

        distanceAnimation.update();
        yawAnimation.update();
        pitchAnimation.update();
        cameraYawAnimation.update();

        float targetSize = DEFAULT_DISTANCE + 45.0F;
        if (mc.player.isShiftKeyDown()) {
            targetSize -= 20.0F;
        }
        if (isMoving()) {
            targetSize += 10.0F;
        }

        float strafeInput = mc.player.input.getMoveVector().x;
        float forwardInput = mc.player.input.getMoveVector().y;
        yawAnimation.run(strafeInput * 5.0F, 0.75, Easings.EXPO_OUT, true);
        pitchAnimation.run(forwardInput * 5.0F, 0.75, Easings.EXPO_OUT, true);
        cameraYawAnimation.run(mc.gameRenderer.getMainCamera().yRot(), 0.75, Easings.EXPO_OUT, true);
        distanceAnimation.run(targetSize, SIZE_ANIM_SPEED, Easings.EXPO_OUT, true);

        for (ArrowEntry arrow : arrows) {
            arrow.active = false;
        }

        for (Player player : mc.level.players()) {
            if (player == mc.player || player.isRemoved()) {
                continue;
            }
            ArrowEntry arrow = findArrow(player);
            if (arrow == null) {
                arrow = new ArrowEntry(player);
                arrows.add(arrow);
            }
            arrow.active = true;
            arrow.fade.run(1.0, 0.20, Easings.CUBIC_OUT, true);
        }

        for (int i = arrows.size() - 1; i >= 0; i--) {
            ArrowEntry arrow = arrows.get(i);
            if (!arrow.active) {
                arrow.fade.run(0.0, 0.20, Easings.CUBIC_OUT, true);
                if (arrow.fade.get() <= 0.01f && !arrow.fade.isAlive()) {
                    arrows.remove(i);
                }
            }
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        float centerX = screenWidth / 2.0F;
        float centerY = screenHeight / 2.0F;
        double cameraYaw = cameraYawAnimation.getValue();
        double cos = Mth.cos((float) (cameraYaw * (Math.PI * 2.0 / 360.0)));
        double sin = Mth.sin((float) (cameraYaw * (Math.PI * 2.0 / 360.0)));

        float partialTick = 1.0F;

        for (ArrowEntry arrow : arrows) {
            arrow.fade.update();
            float arrowAlpha = arrow.fade.get();
            if (arrowAlpha <= 0.01f) {
                continue;
            }

            Player player = arrow.player;
            double playerX = Mth.lerp(partialTick, player.xOld, player.getX()) - mc.gameRenderer.getMainCamera().position().x;
            double playerZ = Mth.lerp(partialTick, player.zOld, player.getZ()) - mc.gameRenderer.getMainCamera().position().z;
            double rotY = -(playerZ * cos - playerX * sin);
            double rotX = -(playerX * cos + playerZ * sin);
            float angle = (float) (Math.atan2(rotY, rotX) * 180.0 / Math.PI);
            float x = (float) (distanceAnimation.getValue() * arrowAlpha * Mth.cos((float) Math.toRadians(angle)) + centerX + yawAnimation.getValue());
            float y = (float) (distanceAnimation.getValue() * arrowAlpha * Mth.sin((float) Math.toRadians(angle)) + centerY + pitchAnimation.getValue());

            int themeColor = ClickGuiModule.getInstance().getColor();
            boolean isFriend = FriendUtils.isFriend(player);
            int baseColor = isFriend ? ColorUtil.rgba(80, 255, 80, 255) : themeColor;
            int color = ColorUtil.multAlpha(baseColor, arrowAlpha * alpha);

            float size = DEFAULT_SIZE;
            float halfSize = size * 0.5F;
            Render2D.image(ARROW_TEXTURE, x - halfSize, y - halfSize, size, 0.0F, angle + 90.0F, x, y, color);
        }

        size(20.0F, 20.0F);
        drag.visible(false);
    }

    private ArrowEntry findArrow(Player player) {
        for (ArrowEntry arrow : arrows) {
            if (arrow.player == player) {
                return arrow;
            }
        }
        return null;
    }

    private boolean isMoving() {
        return mc.player.input.getMoveVector().y != 0.0F || mc.player.input.getMoveVector().x != 0.0F;
    }

    private static final class ArrowEntry {
        private final Player player;
        private final SmoothAnimation fade = new SmoothAnimation();
        private boolean active;

        private ArrowEntry(Player player) {
            this.player = player;
            this.fade.set(0.0);
        }
    }
}
