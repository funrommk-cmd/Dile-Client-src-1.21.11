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
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.Render3D;
import dile.ru.utils.render.pipeline.ClientPipelines;

import java.awt.Color;

public final class ChinaHat extends Module {
    private static ChinaHat instance;

    private static final int SEGMENTS = 32;

    private final ModeSetting hatType = register(new ModeSetting("Hat Type", "Hat shape variant.", "Normal",
            "Normal", "New"));
    private final BooleanSetting renderOwnPlayer = register(new BooleanSetting("Self", "Render hat on yourself.", false));
    private final BooleanSetting renderPlayers = register(new BooleanSetting("Others", "Render hat on other players.", true));

    private final NumberSetting width = register(new NumberSetting("Width", "Hat radius.", 1.0, 0.2, 3.0, 0.1));
    private final NumberSetting height = register(new NumberSetting("Height", "Hat cone height.", 0.25, 0.05, 0.8, 0.05));
    private final ColorSetting color = register(new ColorSetting("Color", "Hat color.", new Color(100, 150, 200, 255)));
    private final NumberSetting alpha = register(new NumberSetting("Alpha", "Hat opacity.", 0.85, 0.0, 1.0, 0.05));

    private final NumberSetting widthOthers = register(new NumberSetting("Width (Others)", "Hat radius for others.", 1.0, 0.2, 3.0, 0.1));
    private final NumberSetting heightOthers = register(new NumberSetting("Height (Others)", "Hat cone height for others.", 0.25, 0.05, 0.8, 0.05));
    private final ColorSetting colorOthers = register(new ColorSetting("Color (Others)", "Hat color for others.", new Color(100, 150, 200, 255)));
    private final NumberSetting alphaOthers = register(new NumberSetting("Alpha (Others)", "Hat opacity for others.", 0.85, 0.0, 1.0, 0.05));

    {
        width.visibleWhen(() -> renderOwnPlayer.getValue());
        height.visibleWhen(() -> renderOwnPlayer.getValue());
        color.visibleWhen(() -> renderOwnPlayer.getValue());
        alpha.visibleWhen(() -> renderOwnPlayer.getValue());
        widthOthers.visibleWhen(() -> renderPlayers.getValue());
        heightOthers.visibleWhen(() -> renderPlayers.getValue());
        colorOthers.visibleWhen(() -> renderPlayers.getValue());
        alphaOthers.visibleWhen(() -> renderPlayers.getValue());
    }

    public ChinaHat() {
        super("ChinaHat", "Renders a Chinese hat above the player's head.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static ChinaHat getInstance() {
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
            if (player == mc.player && !renderOwnPlayer.getValue()) continue;
            if (player != mc.player && !renderPlayers.getValue()) continue;

            if (player == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            if (!player.isAlive()) continue;

            boolean isSelf = player == mc.player;
            float w = isSelf ? width.getFloat() : widthOthers.getFloat();
            float h = isSelf ? height.getFloat() : heightOthers.getFloat();
            Color c = isSelf ? color.getValue() : colorOthers.getValue();
            float a = isSelf ? alpha.getFloat() : alphaOthers.getFloat();

            try {
                if (hatType.is("New")) {
                    renderTopHat(stack, player, tickDelta, camera, mc, w, h, c, a);
                } else {
                    renderConeHat(stack, player, tickDelta, camera, mc, w, h, c, a);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void renderConeHat(PoseStack stack, Player player, float tickDelta, Vec3 camera,
                               Minecraft mc, float w, float h, Color hatColor, float alphaSetting) {
        MultiBufferSource.BufferSource provider = mc.renderBuffers().bufferSource();

        double x = Mth.lerp(tickDelta, player.xo, player.getX()) - camera.x;
        double y = Mth.lerp(tickDelta, player.yo, player.getY()) - camera.y + player.getBbHeight();
        double z = Mth.lerp(tickDelta, player.zo, player.getZ()) - camera.z;

        float r = hatColor.getRed() / 255.0f;
        float g = hatColor.getGreen() / 255.0f;
        float b = hatColor.getBlue() / 255.0f;
        float a = 0.3f + alphaSetting * 0.7f;

        stack.pushPose();
        stack.translate(x, y, z);

        PoseStack.Pose entry = stack.last();

        VertexConsumer cone = provider.getBuffer(ClientPipelines.CHINA_HAT_SIDES);
        int coneColor = argb(r, g, b, a);
        for (int i = 0; i < SEGMENTS; i++) {
            float angle1 = (float) (2 * Math.PI * i / SEGMENTS);
            float angle2 = (float) (2 * Math.PI * (i + 1) / SEGMENTS);
            float x1 = w * (float) Math.cos(angle1);
            float z1 = w * (float) Math.sin(angle1);
            float x2 = w * (float) Math.cos(angle2);
            float z2 = w * (float) Math.sin(angle2);
            cone.addVertex(entry, x1, 0, z1).setColor(coneColor);
            cone.addVertex(entry, x2, 0, z2).setColor(coneColor);
            cone.addVertex(entry, 0, h, 0).setColor(coneColor);
        }

        VertexConsumer cap = provider.getBuffer(ClientPipelines.CHINA_HAT);
        int capColor = argb(r, g, b, a);
        cap.addVertex(entry, 0, 0, 0).setColor(capColor);
        for (int i = 0; i <= SEGMENTS; i++) {
            float angle = (float) (2 * Math.PI * i / SEGMENTS);
            float cx = w * (float) Math.cos(angle);
            float cz = w * (float) Math.sin(angle);
            cap.addVertex(entry, cx, 0, cz).setColor(capColor);
        }

        VertexConsumer outline = provider.getBuffer(ClientPipelines.CHINA_HAT_OUTLINE);
        int outlineColor = argb(r, g, b, 1.0f);
        for (int i = 0; i <= SEGMENTS; i++) {
            float angle = (float) (2 * Math.PI * i / SEGMENTS);
            float ox = w * (float) Math.cos(angle);
            float oz = w * (float) Math.sin(angle);
            outline.addVertex(entry, ox, 0, oz).setColor(outlineColor);
        }

        stack.popPose();

        provider.endBatch(ClientPipelines.CHINA_HAT_SIDES);
        provider.endBatch(ClientPipelines.CHINA_HAT);
        provider.endBatch(ClientPipelines.CHINA_HAT_OUTLINE);
    }

    private void renderTopHat(PoseStack stack, Player player, float tickDelta, Vec3 camera,
                              Minecraft mc, float w, float h, Color hatColor, float alphaSetting) {
        MultiBufferSource.BufferSource provider = mc.renderBuffers().bufferSource();

        double x = Mth.lerp(tickDelta, player.xo, player.getX()) - camera.x;
        double y = Mth.lerp(tickDelta, player.yo, player.getY()) - camera.y + player.getBbHeight();
        double z = Mth.lerp(tickDelta, player.zo, player.getZ()) - camera.z;

        float r = hatColor.getRed() / 255.0f;
        float g = hatColor.getGreen() / 255.0f;
        float b = hatColor.getBlue() / 255.0f;
        float a = 0.3f + alphaSetting * 0.7f;

        float tipH = h * 2.2f;
        float tipX = w * 0.12f;
        float tipZ = w * 0.08f;
        float crownR = w * 0.5f;
        float brimR = w * 1.2f;

        stack.pushPose();
        stack.translate(x, y, z);

        PoseStack.Pose entry = stack.last();

        VertexConsumer sides = provider.getBuffer(ClientPipelines.CHINA_HAT_SIDES);
        int sideColor = argb(r, g, b, a);
        for (int i = 0; i < SEGMENTS; i++) {
            float a1 = (float) (2 * Math.PI * i / SEGMENTS);
            float a2 = (float) (2 * Math.PI * (i + 1) / SEGMENTS);
            float x1 = crownR * (float) Math.cos(a1);
            float z1 = crownR * (float) Math.sin(a1);
            float x2 = crownR * (float) Math.cos(a2);
            float z2 = crownR * (float) Math.sin(a2);

            sides.addVertex(entry, x1, 0, z1).setColor(sideColor);
            sides.addVertex(entry, x2, 0, z2).setColor(sideColor);
            sides.addVertex(entry, tipX, tipH, tipZ).setColor(sideColor);
        }

        VertexConsumer cap = provider.getBuffer(ClientPipelines.CHINA_HAT);
        int capColor = argb(r, g, b, a);
        cap.addVertex(entry, 0, 0, 0).setColor(capColor);
        for (int i = 0; i <= SEGMENTS; i++) {
            float angle = (float) (2 * Math.PI * i / SEGMENTS);
            float cx = brimR * (float) Math.cos(angle);
            float cz = brimR * (float) Math.sin(angle);
            cap.addVertex(entry, cx, 0, cz).setColor(capColor);
        }

        VertexConsumer outline = provider.getBuffer(ClientPipelines.CHINA_HAT_OUTLINE);
        int outlineColor = argb(r, g, b, 1.0f);
        for (int i = 0; i <= SEGMENTS; i++) {
            float angle = (float) (2 * Math.PI * i / SEGMENTS);
            float ox = brimR * (float) Math.cos(angle);
            float oz = brimR * (float) Math.sin(angle);
            outline.addVertex(entry, ox, 0, oz).setColor(outlineColor);
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
