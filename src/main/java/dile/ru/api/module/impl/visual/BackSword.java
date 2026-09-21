package dile.ru.api.module.impl.visual;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.WorldRenderEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.Render3D;
import dile.ru.utils.render.RenderLayerFactory;

import static net.minecraft.client.renderer.RenderPipelines.MATRICES_PROJECTION_SNIPPET;

public final class BackSword extends Module {
    private static BackSword instance;

    private static final RenderPipeline FILL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/backsword_fill")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .build()
    );
    private static final RenderType FILL_TYPE = RenderLayerFactory.create("backsword_fill", 8192, FILL_PIPELINE);

    private static final RenderPipeline GLOW_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/backsword_glow")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .build()
    );
    private static final RenderType GLOW_TYPE = RenderLayerFactory.create("backsword_glow", 8192, GLOW_PIPELINE);

    private final ModeSetting target = register(new ModeSetting("Target", "Who can see the back sword.", "Self",
            "Self", "All Players", "Self and Others"));
    private final BooleanSetting glow = register(new BooleanSetting("Glow", "Enable glow effect.", true));
    private final NumberSetting glowLevel = register(new NumberSetting("Glow Level", "Glow intensity.", 50.0, 0.0, 100.0, 1.0));
    private final NumberSetting fillAlpha = register(new NumberSetting("Fill Alpha", "Fill transparency.", 20.0, 0.0, 100.0, 1.0));
    private final NumberSetting outlineAlpha = register(new NumberSetting("Outline Alpha", "Outline transparency.", 85.0, 0.0, 100.0, 1.0));
    private final NumberSetting posY = register(new NumberSetting("Offset Y", "Vertical position on the back.", 0.1, -0.5, 0.7, 0.01));
    private final NumberSetting posZ = register(new NumberSetting("Offset Z", "Depth position on the back.", 0.28, 0.0, 0.6, 0.01));

    private float selfBodyYaw;
    private boolean selfBodyYawInitialized;

    public BackSword() {
        super("BackSword", "Renders a 3D sword on the player's back.", ModuleCategory.VISUAL);
        instance = this;
        glowLevel.visibleWhen(() -> glow.getValue());
    }

    public static BackSword getInstance() {
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

        boolean firstPerson = mc.options.getCameraType().isFirstPerson();

        if (!firstPerson && mc.player.isAlive()) {
            boolean showSelf = target.is("Self") || target.is("Self and Others");
            if (showSelf) {
                try {
                    renderSword(stack, mc.player, tickDelta, camera, mc);
                } catch (Exception ignored) {
                }
            }
        }

        if (target.is("All Players") || target.is("Self and Others")) {
            for (Player player : mc.level.players()) {
                if (player == mc.player) continue;
                if (!player.isAlive()) continue;
                try {
                    renderSword(stack, player, tickDelta, camera, mc);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void renderSword(PoseStack stack, Player player, float tickDelta, Vec3 camera, Minecraft mc) {
        MultiBufferSource.BufferSource provider = mc.renderBuffers().bufferSource();

        double x = Mth.lerp(tickDelta, player.xo, player.getX()) - camera.x;
        double y = Mth.lerp(tickDelta, player.yo, player.getY()) - camera.y;
        double z = Mth.lerp(tickDelta, player.zo, player.getZ()) - camera.z;

        float bodyYaw = resolveBodyYaw(player, tickDelta);

        ClickGuiModule gui = ClickGuiModule.getInstance();
        int themeColor = gui != null ? gui.getThemeColor() : 0xFFFFFFFF;
        int r = (themeColor >> 16) & 0xFF;
        int g = (themeColor >> 8) & 0xFF;
        int b = themeColor & 0xFF;

        int fillA = (int) (fillAlpha.getFloat() / 100f * 255f);
        int outA = (int) (outlineAlpha.getFloat() / 100f * 255f);

        stack.pushPose();
        stack.translate(x, y, z);
        stack.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        stack.translate(-0.15f, posY.getFloat(), posZ.getFloat());
        stack.mulPose(Axis.ZP.rotationDegrees(-45f));
        stack.mulPose(Axis.XP.rotationDegrees(-3f));
        stack.scale(1.15f, 1.15f, 1.15f);

        PoseStack.Pose entry = stack.last();

        VertexConsumer fill = provider.getBuffer(FILL_TYPE);
        drawKatana(fill, entry, r, g, b, fillA);

        int br = Math.min(r + 40, 255);
        int bg = Math.min(g + 40, 255);
        int bb = Math.min(b + 40, 255);
        drawKatanaEdge(fill, entry, br, bg, bb, outA, 0.004f);

        if (glow.getValue()) {
            VertexConsumer glowCon = provider.getBuffer(GLOW_TYPE);
            float glowBase = glowLevel.getFloat() / 100f;
            float pulse = (float) (Math.sin(System.currentTimeMillis() / 700.0) * 0.12 + 0.88);
            glowBase *= pulse;
            int layers = 12;
            float maxEx = 0.045f;
            for (int i = 0; i < layers; i++) {
                float t = (float) i / (layers - 1);
                float falloff = (1f - t) * (1f - t);
                int ga = (int) (glowBase * falloff * 180f);
                if (ga <= 1) continue;
                float ex = t * maxEx;
                drawKatanaGlow(glowCon, entry, r, g, b, ga, ex);
            }
        }

        stack.popPose();

        provider.endBatch(FILL_TYPE);
        provider.endBatch(GLOW_TYPE);
    }

    private static void drawKatana(VertexConsumer consumer, PoseStack.Pose entry,
                                    int r, int g, int b, int a) {
        if (a <= 0) return;
        int color = argb(r, g, b, a);

        drawBox(consumer, entry, -0.028f, -0.42f, -0.016f, 0.028f, 0.0f, 0.016f, color);
        drawBox(consumer, entry, -0.032f, -0.46f, -0.018f, 0.032f, -0.42f, 0.018f, color);
        drawBox(consumer, entry, -0.085f, -0.005f, -0.034f, 0.085f, 0.005f, 0.034f, color);
        drawBlade(consumer, entry, color);
    }

    private static void drawBlade(VertexConsumer consumer, PoseStack.Pose entry, int color) {
        float bw = 0.022f, bBack = 0.006f, bEdge = 0.004f;
        float bx0 = -bw, bx1 = bw, by = 0.005f;
        float bzBack = bBack, bzEdge = -bEdge;

        float mw = 0.015f, mBack = 0.004f, mEdge = 0.002f;
        float mx0 = -mw, mx1 = mw, my = 0.50f;
        float mzBack = mBack, mzEdge = -mEdge;

        float tx = 0f, ty = 1.02f, tz = 0f;

        quad(consumer, entry, color,
                bx0, by, bzBack, bx1, by, bzBack,
                mx1, my, mzBack, mx0, my, mzBack);
        quad(consumer, entry, color,
                bx1, by, bzBack, bx1, by, bzEdge,
                mx1, my, mzEdge, mx1, my, mzBack);
        quad(consumer, entry, color,
                bx0, by, bzBack, bx0, by, bzEdge,
                mx0, my, mzEdge, mx0, my, mzBack);
        quad(consumer, entry, color,
                bx0, by, bzEdge, bx1, by, bzEdge,
                mx1, my, mzEdge, mx0, my, mzEdge);

        quad(consumer, entry, color,
                mx0, my, mzBack, mx1, my, mzBack,
                tx, ty, tz, tx, ty, tz);
        quad(consumer, entry, color,
                mx1, my, mzBack, mx1, my, mzEdge,
                tx, ty, tz, tx, ty, tz);
        quad(consumer, entry, color,
                mx0, my, mzBack, mx0, my, mzEdge,
                tx, ty, tz, tx, ty, tz);
        quad(consumer, entry, color,
                mx0, my, mzEdge, mx1, my, mzEdge,
                tx, ty, tz, tx, ty, tz);
    }

    private static void drawKatanaEdge(VertexConsumer consumer, PoseStack.Pose entry,
                                        int r, int g, int b, int a, float e) {
        if (a <= 0) return;
        int color = argb(r, g, b, a);

        drawBox(consumer, entry,
                -0.028f - e, -0.42f - e, -0.016f - e,
                0.028f + e, 0.0f + e, 0.016f + e, color);
        drawBox(consumer, entry,
                -0.032f - e, -0.46f - e, -0.018f - e,
                0.032f + e, -0.42f + e, 0.018f + e, color);
        drawBox(consumer, entry,
                -0.085f - e, -0.005f - e, -0.034f - e,
                0.085f + e, 0.005f + e, 0.034f + e, color);

        drawBlade(consumer, entry, color);
    }

    private static void drawKatanaGlow(VertexConsumer consumer, PoseStack.Pose entry,
                                        int r, int g, int b, int a, float ex) {
        if (a <= 0) return;
        int color = argb(r, g, b, a);

        drawBox(consumer, entry,
                -0.028f - ex, -0.42f - ex, -0.016f - ex,
                0.028f + ex, 0.0f + ex, 0.016f + ex, color);
        drawBox(consumer, entry,
                -0.032f - ex, -0.46f - ex, -0.018f - ex,
                0.032f + ex, -0.42f + ex, 0.018f + ex, color);
        drawBox(consumer, entry,
                -0.085f - ex, -0.005f - ex, -0.034f - ex,
                0.085f + ex, 0.005f + ex, 0.034f + ex, color);

        float bw = 0.022f + ex, bBack = 0.006f + ex, bEdge = 0.004f + ex;
        float mw = 0.015f + ex, mBack = 0.004f + ex, mEdge = 0.002f + ex;
        float bx0 = -bw, bx1 = bw, by = 0.005f - ex;
        float mx0 = -mw, mx1 = mw, my = 0.50f;
        float mzBack = mBack, mzEdge = -mEdge;
        float bzBack = bBack, bzEdge = -bEdge;
        float tx = 0f, ty = 1.02f + ex, tz = 0f;

        quad(consumer, entry, color,
                bx0, by, bzBack, bx1, by, bzBack,
                mx1, my, mzBack, mx0, my, mzBack);
        quad(consumer, entry, color,
                bx1, by, bzBack, bx1, by, bzEdge,
                mx1, my, mzEdge, mx1, my, mzBack);
        quad(consumer, entry, color,
                bx0, by, bzBack, bx0, by, bzEdge,
                mx0, my, mzEdge, mx0, my, mzBack);
        quad(consumer, entry, color,
                bx0, by, bzEdge, bx1, by, bzEdge,
                mx1, my, mzEdge, mx0, my, mzEdge);
        quad(consumer, entry, color,
                mx0, my, mzBack, mx1, my, mzBack,
                tx, ty, tz, tx, ty, tz);
        quad(consumer, entry, color,
                mx1, my, mzBack, mx1, my, mzEdge,
                tx, ty, tz, tx, ty, tz);
        quad(consumer, entry, color,
                mx0, my, mzBack, mx0, my, mzEdge,
                tx, ty, tz, tx, ty, tz);
        quad(consumer, entry, color,
                mx0, my, mzEdge, mx1, my, mzEdge,
                tx, ty, tz, tx, ty, tz);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose entry, int color,
                              float x0, float y0, float z0,
                              float x1, float y1, float z1,
                              float x2, float y2, float z2,
                              float x3, float y3, float z3) {
        consumer.addVertex(entry, x0, y0, z0).setColor(color);
        consumer.addVertex(entry, x1, y1, z1).setColor(color);
        consumer.addVertex(entry, x2, y2, z2).setColor(color);
        consumer.addVertex(entry, x3, y3, z3).setColor(color);
    }

    private static int argb(int r, int g, int b, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (Mth.clamp(r, 0, 255) << 16) | (Mth.clamp(g, 0, 255) << 8) | Mth.clamp(b, 0, 255);
    }

    private static void drawBox(VertexConsumer consumer, PoseStack.Pose entry,
                                 float x0, float y0, float z0, float x1, float y1, float z1,
                                 int color) {
        quad(consumer, entry, color, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(consumer, entry, color, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(consumer, entry, color, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(consumer, entry, color, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(consumer, entry, color, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(consumer, entry, color, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private float resolveBodyYaw(Player player, float tickDelta) {
        float target = Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot);
        if (player != Minecraft.getInstance().player) return target;
        if (!selfBodyYawInitialized || player.tickCount < 2) {
            selfBodyYaw = target;
            selfBodyYawInitialized = true;
            return selfBodyYaw;
        }
        selfBodyYaw = approachDegrees(selfBodyYaw, target, 14f);
        return selfBodyYaw;
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }

    @Override
    public void onDisable() {
        selfBodyYawInitialized = false;
        super.onDisable();
    }
}
