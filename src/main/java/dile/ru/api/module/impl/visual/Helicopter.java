package dile.ru.api.module.impl.visual;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
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
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.Render3D;
import dile.ru.utils.render.RenderLayerFactory;

import java.awt.Color;

import static net.minecraft.client.renderer.RenderPipelines.MATRICES_PROJECTION_SNIPPET;

public final class Helicopter extends Module {
    private static Helicopter instance;

    private static final RenderPipeline BLADE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/helicopter_blade")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
                    .build()
    );
    private static final RenderType BLADE_TYPE = RenderLayerFactory.create("helicopter_blade", 4096, BLADE_PIPELINE);

    private final BooleanSetting renderSelf = register(new BooleanSetting("Self", "Render helicopter on yourself.", false));
    private final BooleanSetting renderOthers = register(new BooleanSetting("Others", "Render helicopter on other players.", true));

    private final NumberSetting bladeLength = register(new NumberSetting("Length", "Blade length.", 0.6, 0.2, 1.5, 0.05));
    private final NumberSetting bladeWidth = register(new NumberSetting("Width", "Blade width.", 0.12, 0.03, 0.3, 0.01));
    private final NumberSetting speed = register(new NumberSetting("Speed", "Rotation speed.", 40.0, 5.0, 120.0, 5.0));
    private final NumberSetting heightOff = register(new NumberSetting("Height", "Height above head.", 0.1, 0.0, 0.5, 0.02));
    private final ColorSetting color = register(new ColorSetting("Color", "Blade color.", new Color(180, 180, 180, 255)));
    private final NumberSetting alpha = register(new NumberSetting("Alpha", "Blade opacity.", 0.75, 0.0, 1.0, 0.05));

    private final NumberSetting bladeLengthOthers = register(new NumberSetting("Length (Others)", "Blade length for others.", 0.6, 0.2, 1.5, 0.05));
    private final NumberSetting bladeWidthOthers = register(new NumberSetting("Width (Others)", "Blade width for others.", 0.12, 0.03, 0.3, 0.01));
    private final ColorSetting colorOthers = register(new ColorSetting("Color (Others)", "Blade color for others.", new Color(180, 180, 180, 255)));
    private final NumberSetting alphaOthers = register(new NumberSetting("Alpha (Others)", "Blade opacity for others.", 0.75, 0.0, 1.0, 0.05));

    {
        bladeLength.visibleWhen(() -> renderSelf.getValue());
        bladeWidth.visibleWhen(() -> renderSelf.getValue());
        speed.visibleWhen(() -> renderSelf.getValue());
        heightOff.visibleWhen(() -> renderSelf.getValue());
        color.visibleWhen(() -> renderSelf.getValue());
        alpha.visibleWhen(() -> renderSelf.getValue());
        bladeLengthOthers.visibleWhen(() -> renderOthers.getValue());
        bladeWidthOthers.visibleWhen(() -> renderOthers.getValue());
        colorOthers.visibleWhen(() -> renderOthers.getValue());
        alphaOthers.visibleWhen(() -> renderOthers.getValue());
    }

    public Helicopter() {
        super("Helicopter", "Renders spinning helicopter blades above the player's head.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static Helicopter getInstance() {
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
            float bl = isSelf ? bladeLength.getFloat() : bladeLengthOthers.getFloat();
            float bw = isSelf ? bladeWidth.getFloat() : bladeWidthOthers.getFloat();
            float sp = isSelf ? speed.getFloat() : 40f;
            float ho = isSelf ? heightOff.getFloat() : 0.1f;
            Color c = isSelf ? color.getValue() : colorOthers.getValue();
            float a = isSelf ? alpha.getFloat() : alphaOthers.getFloat();

            try {
                renderBlades(stack, player, tickDelta, camera, mc, bl, bw, sp, ho, c, a);
            } catch (Exception ignored) {
            }
        }
    }

    private void renderBlades(PoseStack stack, Player player, float tickDelta, Vec3 camera,
                              Minecraft mc, float bl, float bw, float sp, float ho,
                              Color bladeColor, float alphaSetting) {
        MultiBufferSource.BufferSource provider = mc.renderBuffers().bufferSource();

        double x = Mth.lerp(tickDelta, player.xo, player.getX()) - camera.x;
        double y = Mth.lerp(tickDelta, player.yo, player.getY()) - camera.y + player.getBbHeight() + ho;
        double z = Mth.lerp(tickDelta, player.zo, player.getZ()) - camera.z;

        float rotation = (player.tickCount + tickDelta) * sp;

        float cr = bladeColor.getRed() / 255.0f;
        float cg = bladeColor.getGreen() / 255.0f;
        float cb = bladeColor.getBlue() / 255.0f;
        float ca = 0.3f + alphaSetting * 0.7f;

        stack.pushPose();
        stack.translate(x, y, z);

        PoseStack.Pose entry = stack.last();
        int bladeColorInt = argb(cr, cg, cb, ca);

        VertexConsumer blade = provider.getBuffer(BLADE_TYPE);

        for (int b = 0; b < 2; b++) {
            float angle = (b == 0 ? rotation : rotation + 90f);
            float rad = (float) Math.toRadians(angle);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            float hw = bw / 2f;

            float ax = cos * bl;
            float az = sin * bl;
            float nx = -sin * hw;
            float nz = cos * hw;

            blade.addVertex(entry, -ax + nx, 0, -az + nz).setColor(bladeColorInt);
            blade.addVertex(entry, ax + nx, 0, az + nz).setColor(bladeColorInt);
            blade.addVertex(entry, ax - nx, 0, az - nz).setColor(bladeColorInt);
            blade.addVertex(entry, -ax - nx, 0, -az - nz).setColor(bladeColorInt);
        }

        float hubR = bw * 1.2f;
        int hubColor = argb(cr, cg, cb, Math.min(1.0f, ca + 0.2f));
        blade.addVertex(entry, -hubR, -0.02f, -hubR).setColor(hubColor);
        blade.addVertex(entry, hubR, -0.02f, -hubR).setColor(hubColor);
        blade.addVertex(entry, hubR, -0.02f, hubR).setColor(hubColor);
        blade.addVertex(entry, -hubR, -0.02f, hubR).setColor(hubColor);

        stack.popPose();

        provider.endBatch(BLADE_TYPE);
    }

    private static int argb(float r, float g, float b, float a) {
        int ri = Mth.clamp((int) (r * 255), 0, 255);
        int gi = Mth.clamp((int) (g * 255), 0, 255);
        int bi = Mth.clamp((int) (b * 255), 0, 255);
        int ai = Mth.clamp((int) (a * 255), 0, 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }
}
