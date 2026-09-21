package dile.ru.api.module.impl.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.WorldRenderEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.Render3D;
import dile.ru.utils.render.pipeline.ClientPipelines;

import java.awt.Color;

public final class Crown extends Module {
    private static Crown instance;

    private static final int POINTS = 8;

    private final BooleanSetting renderSelf = register(new BooleanSetting("Self", "Render crown on yourself.", false));
    private final BooleanSetting renderOthers = register(new BooleanSetting("Others", "Render crown on other players.", true));

    private final NumberSetting radius = register(new NumberSetting("Radius", "Crown radius.", 0.35, 0.15, 0.8, 0.05));
    private final NumberSetting height = register(new NumberSetting("Height", "Crown height.", 0.25, 0.05, 0.6, 0.05));
    private final NumberSetting spikeHeight = register(new NumberSetting("Spike Height", "Height of crown spikes.", 0.15, 0.02, 0.4, 0.02));
    private final NumberSetting floatOffset = register(new NumberSetting("Float", "Floating offset above head.", 0.1, 0.0, 0.4, 0.02));
    private final ColorSetting color = register(new ColorSetting("Color", "Crown color.", new Color(255, 215, 0, 255)));
    private final NumberSetting alpha = register(new NumberSetting("Alpha", "Crown opacity.", 0.85, 0.0, 1.0, 0.05));
    private final NumberSetting rotationSpeed = register(new NumberSetting("Rotation", "Rotation speed.", 20.0, 0.0, 100.0, 5.0));
    private final NumberSetting bobAmount = register(new NumberSetting("Bob", "Bobbing amount.", 0.03, 0.0, 0.1, 0.01));

    private final NumberSetting radiusOthers = register(new NumberSetting("Radius (Others)", "Crown radius for others.", 0.35, 0.15, 0.8, 0.05));
    private final NumberSetting heightOthers = register(new NumberSetting("Height (Others)", "Crown height for others.", 0.25, 0.05, 0.6, 0.05));
    private final NumberSetting spikeHeightOthers = register(new NumberSetting("Spike Height (Others)", "Spike height for others.", 0.15, 0.02, 0.4, 0.02));
    private final ColorSetting colorOthers = register(new ColorSetting("Color (Others)", "Crown color for others.", new Color(255, 215, 0, 255)));
    private final NumberSetting alphaOthers = register(new NumberSetting("Alpha (Others)", "Crown opacity for others.", 0.85, 0.0, 1.0, 0.05));

    {
        radius.visibleWhen(() -> renderSelf.getValue());
        height.visibleWhen(() -> renderSelf.getValue());
        spikeHeight.visibleWhen(() -> renderSelf.getValue());
        floatOffset.visibleWhen(() -> renderSelf.getValue());
        color.visibleWhen(() -> renderSelf.getValue());
        alpha.visibleWhen(() -> renderSelf.getValue());
        rotationSpeed.visibleWhen(() -> renderSelf.getValue());
        bobAmount.visibleWhen(() -> renderSelf.getValue());
        radiusOthers.visibleWhen(() -> renderOthers.getValue());
        heightOthers.visibleWhen(() -> renderOthers.getValue());
        spikeHeightOthers.visibleWhen(() -> renderOthers.getValue());
        colorOthers.visibleWhen(() -> renderOthers.getValue());
        alphaOthers.visibleWhen(() -> renderOthers.getValue());
    }

    public Crown() {
        super("Crown", "Renders a 3D crown above the player's head.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static Crown getInstance() {
        return instance;
    }

    @SubscribeEvent
    private void onWorldRender(WorldRenderEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) return;

        float tickDelta = event.getTickDelta();
        Vec3 camera = Render3D.lastCameraPos;
        PoseStack stack = event.getStack();
        if (camera == null) return;

        for (Player player : mc.level.players()) {
            if (player == mc.player && !renderSelf.getValue()) continue;
            if (player != mc.player && !renderOthers.getValue()) continue;
            if (player == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            if (!player.isAlive()) continue;

            boolean isSelf = player == mc.player;
            float r = isSelf ? radius.getFloat() : radiusOthers.getFloat();
            float h = isSelf ? height.getFloat() : heightOthers.getFloat();
            float sh = isSelf ? spikeHeight.getFloat() : spikeHeightOthers.getFloat();
            float fo = isSelf ? floatOffset.getFloat() : 0.1f;
            Color c = isSelf ? color.getValue() : colorOthers.getValue();
            float a = isSelf ? alpha.getFloat() : alphaOthers.getFloat();
            float rs = isSelf ? rotationSpeed.getFloat() : 20f;
            float bob = isSelf ? bobAmount.getFloat() : 0.03f;

            try {
                renderCrown(stack, player, tickDelta, camera, mc, r, h, sh, fo, c, a, rs, bob);
            } catch (Exception ignored) {
            }
        }
    }

    private void renderCrown(PoseStack stack, Player player, float tickDelta, Vec3 camera,
                             Minecraft mc, float r, float h, float sh, float fo,
                             Color hatColor, float alphaSetting, float rs, float bob) {
        MultiBufferSource.BufferSource provider = mc.renderBuffers().bufferSource();

        double x = Mth.lerp(tickDelta, player.xo, player.getX()) - camera.x;
        double y = Mth.lerp(tickDelta, player.yo, player.getY()) - camera.y + player.getBbHeight() + fo;
        double z = Mth.lerp(tickDelta, player.zo, player.getZ()) - camera.z;

        float bobY = (float) (Math.sin(System.currentTimeMillis() / 600.0) * bob);
        float rotation = (player.tickCount + tickDelta) * rs;

        float cr = hatColor.getRed() / 255.0f;
        float cg = hatColor.getGreen() / 255.0f;
        float cb = hatColor.getBlue() / 255.0f;
        float ca = 0.3f + alphaSetting * 0.7f;

        stack.pushPose();
        stack.translate(x, y + bobY, z);
        stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));

        PoseStack.Pose entry = stack.last();

        int baseColor = argb(cr, cg, cb, ca);
        int spikeColor = argb(cr, cg, cb, Math.min(1.0f, ca + 0.15f));
        int outlineColor = argb(cr, cg, cb, 1.0f);

        VertexConsumer body = provider.getBuffer(ClientPipelines.CHINA_HAT_SIDES);
        for (int i = 0; i < POINTS; i++) {
            float angle1 = (float) (2 * Math.PI * i / POINTS);
            float angle2 = (float) (2 * Math.PI * (i + 1) / POINTS);
            float x1 = r * (float) Math.cos(angle1);
            float z1 = r * (float) Math.sin(angle1);
            float x2 = r * (float) Math.cos(angle2);
            float z2 = r * (float) Math.sin(angle2);

            body.addVertex(entry, x1, 0, z1).setColor(baseColor);
            body.addVertex(entry, x2, 0, z2).setColor(baseColor);
            body.addVertex(entry, x1, h, z1).setColor(baseColor);

            body.addVertex(entry, x2, 0, z2).setColor(baseColor);
            body.addVertex(entry, x2, h, z2).setColor(baseColor);
            body.addVertex(entry, x1, h, z1).setColor(baseColor);
        }

        for (int i = 0; i < POINTS; i++) {
            float angle1 = (float) (2 * Math.PI * i / POINTS);
            float angle2 = (float) (2 * Math.PI * (i + 1) / POINTS);
            float midAngle = (angle1 + angle2) / 2f;

            float sx1 = r * (float) Math.cos(angle1);
            float sz1 = r * (float) Math.sin(angle1);
            float sx2 = r * (float) Math.cos(angle2);
            float sz2 = r * (float) Math.sin(angle2);
            float spikeTop = h + sh;

            body.addVertex(entry, sx1, h, sz1).setColor(spikeColor);
            body.addVertex(entry, sx2, h, sz2).setColor(spikeColor);
            body.addVertex(entry, r * 0.7f * (float) Math.cos(midAngle), spikeTop, r * 0.7f * (float) Math.sin(midAngle)).setColor(spikeColor);
        }

        VertexConsumer cap = provider.getBuffer(ClientPipelines.CHINA_HAT);
        cap.addVertex(entry, 0, 0, 0).setColor(baseColor);
        for (int i = 0; i <= POINTS; i++) {
            float angle = (float) (2 * Math.PI * i / POINTS);
            float cx = r * (float) Math.cos(angle);
            float cz = r * (float) Math.sin(angle);
            cap.addVertex(entry, cx, 0, cz).setColor(baseColor);
        }

        VertexConsumer outline = provider.getBuffer(ClientPipelines.CHINA_HAT_OUTLINE);
        for (int i = 0; i <= POINTS; i++) {
            float angle = (float) (2 * Math.PI * i / POINTS);
            float ox = r * (float) Math.cos(angle);
            float oz = r * (float) Math.sin(angle);
            outline.addVertex(entry, ox, 0, oz).setColor(outlineColor);
        }

        for (int i = 0; i <= POINTS; i++) {
            float angle = (float) (2 * Math.PI * i / POINTS);
            float ox = r * (float) Math.cos(angle);
            float oz = r * (float) Math.sin(angle);
            outline.addVertex(entry, ox, h, oz).setColor(outlineColor);
        }

        for (int i = 0; i < POINTS; i++) {
            float angle1 = (float) (2 * Math.PI * i / POINTS);
            float angle2 = (float) (2 * Math.PI * (i + 1) / POINTS);
            float midAngle = (angle1 + angle2) / 2f;
            float sx1 = r * (float) Math.cos(angle1);
            float sz1 = r * (float) Math.sin(angle1);
            float sx2 = r * (float) Math.cos(angle2);
            float sz2 = r * (float) Math.sin(angle2);
            float spikeTop = h + sh;

            outline.addVertex(entry, sx1, h, sz1).setColor(outlineColor);
            outline.addVertex(entry, r * 0.7f * (float) Math.cos(midAngle), spikeTop, r * 0.7f * (float) Math.sin(midAngle)).setColor(outlineColor);
            outline.addVertex(entry, r * 0.7f * (float) Math.cos(midAngle), spikeTop, r * 0.7f * (float) Math.sin(midAngle)).setColor(outlineColor);
            outline.addVertex(entry, sx2, h, sz2).setColor(outlineColor);
        }

        stack.popPose();

        provider.endBatch(ClientPipelines.CHINA_HAT_SIDES);
        provider.endBatch(ClientPipelines.CHINA_HAT);
        provider.endBatch(ClientPipelines.CHINA_HAT_OUTLINE);
    }

    private static int argb(float r, float g, float b, float a) {
        int ri = Mth.clamp((int) (r * 255), 0, 255);
        int gi = Mth.clamp((int) (g * 255), 0, 255);
        int bi = Mth.clamp((int) (b * 255), 0, 255);
        int ai = Mth.clamp((int) (a * 255), 0, 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }
}
