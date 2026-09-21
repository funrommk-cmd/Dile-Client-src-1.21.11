package dile.ru.screens.modernui.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;
import dile.ru.utils.render.RenderSampler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class ClickGuiCloudsRenderer {
    private static final Identifier PIPELINE_ID = Identifier.fromNamespaceAndPath("dile", "pipeline/ui/clickgui_clouds");
    private static final Identifier VERTEX_SHADER = Identifier.fromNamespaceAndPath("dile", "ui/clickgui_clouds/clickgui_clouds");
    private static final Identifier FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("dile", "ui/clickgui_clouds/clickgui_clouds");
    private static final int MAX_CLICKS = 16;
    private static final int CLICK_STRIDE = 16;
    private static final int HEADER_SIZE = 32;
    private static final int UNIFORM_BYTES = HEADER_SIZE + MAX_CLICKS * CLICK_STRIDE;
    private static final long CLICK_DURATION_MS = 4000;

    private static RenderPipeline pipeline;

    private static RenderPipeline getPipeline() {
        if (pipeline == null) {
            pipeline = RenderPipeline.builder()
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                    .withUniform("ClickGuiCloudsData", UniformType.UNIFORM_BUFFER)
                    .withSampler("SceneTex")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build();
        }
        return pipeline;
    }

    private static final ByteBuffer DATA_BUFFER = MemoryUtil.memAlloc(UNIFORM_BYTES);
    private static final GpuBuffer DUMMY_VERTEX_BUFFER;
    private static final GpuBuffer UNIFORM_BUFFER;

    private static GpuTexture tempTexture;
    private static GpuTextureView tempTextureView;
    private static int lastWidth;
    private static int lastHeight;
    private static boolean disabledAfterError;

    private static final List<CloudClick> clicks = new ArrayList<>();

    static {
        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        DUMMY_VERTEX_BUFFER = RenderSystem.getDevice().createBuffer(
                () -> "dile:clouds_dummy_vertex",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                dummyData
        );
        MemoryUtil.memFree(dummyData);

        UNIFORM_BUFFER = RenderSystem.getDevice().createBuffer(
                () -> "dile:clouds_uniform",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_BYTES
        );
    }

    private ClickGuiCloudsRenderer() {
    }

    public static void addClick(float x, float y) {
        clicks.add(new CloudClick(x, y, System.currentTimeMillis()));
        if (clicks.size() > MAX_CLICKS) {
            clicks.remove(0);
        }
    }

    public static void clearClicks() {
        clicks.clear();
    }

    public static void render() {
        if (disabledAfterError) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getMainRenderTarget() == null) return;

        int width = mc.getMainRenderTarget().width;
        int height = mc.getMainRenderTarget().height;
        if (width <= 0 || height <= 0) return;

        ensureTextures(width, height);
        if (tempTexture == null || tempTextureView == null) return;

        try {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

            encoder.copyTextureToTexture(
                    mc.getMainRenderTarget().getColorTexture(),
                    tempTexture,
                    0, 0, 0, 0, 0,
                    width, height
            );

            updateClicks();
            writeUniforms(width, height);

            encoder.writeToBuffer(UNIFORM_BUFFER.slice(0, DATA_BUFFER.remaining()), DATA_BUFFER);

            try (RenderPass renderPass = encoder.createRenderPass(
                    () -> "dile:clouds_pass",
                    mc.getMainRenderTarget().getColorTextureView(),
                    OptionalInt.empty(),
                    null,
                    OptionalDouble.empty()
            )) {
                renderPass.setPipeline(getPipeline());
                renderPass.setVertexBuffer(0, DUMMY_VERTEX_BUFFER);
                renderPass.bindTexture("SceneTex", tempTextureView, RenderSampler.linear());
                renderPass.setUniform("ClickGuiCloudsData", UNIFORM_BUFFER.slice());
                renderPass.draw(0, 6);
            }
        } catch (Throwable ignored) {
            disabledAfterError = true;
        }
    }

    public static void shutdown() {
        if (tempTextureView != null) tempTextureView.close();
        if (tempTexture != null) tempTexture.close();
        tempTexture = null;
        tempTextureView = null;
        lastWidth = -1;
        lastHeight = -1;
    }

    private static void ensureTextures(int width, int height) {
        if (tempTexture != null && width == lastWidth && height == lastHeight) {
            return;
        }

        if (tempTextureView != null) tempTextureView.close();
        if (tempTexture != null) tempTexture.close();

        tempTexture = RenderSystem.getDevice().createTexture(
                () -> "dile:clouds_temp",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8,
                width,
                height,
                1,
                1
        );
        tempTextureView = RenderSystem.getDevice().createTextureView(tempTexture);
        lastWidth = width;
        lastHeight = height;
    }

    private static void updateClicks() {
        long now = System.currentTimeMillis();
        Iterator<CloudClick> it = clicks.iterator();
        while (it.hasNext()) {
            CloudClick c = it.next();
            if (now - c.startTime > CLICK_DURATION_MS) {
                it.remove();
            }
        }
    }

    private static void writeUniforms(int width, int height) {
        long now = System.currentTimeMillis();
        DATA_BUFFER.clear();

        DATA_BUFFER.putFloat((float) width);
        DATA_BUFFER.putFloat((float) height);
        DATA_BUFFER.putFloat(0.0f);
        DATA_BUFFER.putFloat(0.0f);

        float time = (System.nanoTime() % 360_000_000_000L) / 1_000_000_000.0f;
        int count = Math.min(clicks.size(), MAX_CLICKS);
        DATA_BUFFER.putFloat(time);
        DATA_BUFFER.putFloat((float) count);
        DATA_BUFFER.putFloat(0.0f);
        DATA_BUFFER.putFloat(0.0f);

        for (int i = 0; i < MAX_CLICKS; i++) {
            if (i < count) {
                CloudClick c = clicks.get(i);
                float age = (float) (now - c.startTime) / CLICK_DURATION_MS;
                float radius = age * Math.max(width, height) * 0.18f;
                float pushPhase = (1.0f - age) * (1.0f - age);
                float recoveryPhase = (float) Math.sin(age * Math.PI * 0.6) * -0.8f * (1.0f - age);
                float strength = pushPhase + recoveryPhase;
                DATA_BUFFER.putFloat(c.x);
                DATA_BUFFER.putFloat(c.y);
                DATA_BUFFER.putFloat(radius);
                DATA_BUFFER.putFloat(strength);
            } else {
                DATA_BUFFER.putFloat(0.0f);
                DATA_BUFFER.putFloat(0.0f);
                DATA_BUFFER.putFloat(0.0f);
                DATA_BUFFER.putFloat(0.0f);
            }
        }

        DATA_BUFFER.flip();
    }

    private static final class CloudClick {
        private final float x;
        private final float y;
        private final long startTime;

        private CloudClick(float x, float y, long startTime) {
            this.x = x;
            this.y = y;
            this.startTime = startTime;
        }
    }
}
