package dile.ru.screens.modernui.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalInt;

public final class ClickGuiBgShaderRenderer {
    private static final Identifier PIPELINE_ID = Identifier.fromNamespaceAndPath("dile", "pipeline/ui/clickgui_bg");
    private static final Identifier VERTEX_SHADER = Identifier.fromNamespaceAndPath("dile", "ui/clickgui_bg/clickgui_bg");
    private static final Identifier FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("dile", "ui/clickgui_bg/clickgui_bg");
    private static final int MAX_CLICKS = 16;
    private static final int CLICK_STRIDE = 16; // vec4 = 16 bytes
    private static final int HEADER_SIZE = 32; // vec2(8) + float(4) + float(4) + vec2(8) + int(4) + pad(4)
    private static final int UNIFORM_BYTES = HEADER_SIZE + MAX_CLICKS * CLICK_STRIDE;

    private static RenderPipeline pipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static ByteBuffer dataBuffer;

    private ClickGuiBgShaderRenderer() {
    }

    public static void render(float screenWidth, float screenHeight, float mouseX, float mouseY, List<ClickRevealData> reveals) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getMainRenderTarget() == null) return;

        if (pipeline == null) {
            initPipeline();
        }
        if (pipeline == null || uniformBuffer == null || dummyVertexBuffer == null || dataBuffer == null) return;

        try {
            float time = (System.nanoTime() % 180_000_000_000L) / 1_000_000_000.0f;
            writeUniforms(screenWidth, screenHeight, time, mouseX, mouseY, reveals);

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(uniformBuffer.slice(0, dataBuffer.remaining()), dataBuffer);

            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager._disableDepthTest();
            GlStateManager._disableCull();

            GpuTextureView colorView = mc.getMainRenderTarget().getColorTextureView();
            try (RenderPass renderPass = encoder.createRenderPass(
                    () -> "dile:clickgui_bg",
                    colorView,
                    OptionalInt.empty(),
                    null,
                    java.util.OptionalDouble.empty()
            )) {
                renderPass.setPipeline(pipeline);
                renderPass.setVertexBuffer(0, dummyVertexBuffer);
                renderPass.setUniform("ClickGuiBgData", uniformBuffer.slice());
                renderPass.draw(0, 6);
            }

            GlStateManager._enableDepthTest();
            GlStateManager._enableCull();
            GlStateManager._disableBlend();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public static void shutdown() {
        if (uniformBuffer != null) uniformBuffer.close();
        if (dummyVertexBuffer != null) dummyVertexBuffer.close();
        if (dataBuffer != null) MemoryUtil.memFree(dataBuffer);
        uniformBuffer = null;
        dummyVertexBuffer = null;
        dataBuffer = null;
        pipeline = null;
    }

    private static void initPipeline() {
        try {
            pipeline = RenderPipeline.builder()
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                    .withUniform("ClickGuiBgData", UniformType.UNIFORM_BUFFER)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build();

            dataBuffer = MemoryUtil.memAlloc(UNIFORM_BYTES);
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "dile:clickgui_bg_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    UNIFORM_BYTES
            );

            ByteBuffer dummyData = MemoryUtil.memAlloc(4);
            dummyData.putInt(0);
            dummyData.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "dile:clickgui_bg_dummy_vertex",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    dummyData
            );
            MemoryUtil.memFree(dummyData);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            pipeline = null;
        }
    }

    private static void writeUniforms(float screenWidth, float screenHeight, float time, float mouseX, float mouseY, List<ClickRevealData> reveals) {
        dataBuffer.clear();

        dataBuffer.putFloat(screenWidth);
        dataBuffer.putFloat(screenHeight);
        dataBuffer.putFloat(time);
        dataBuffer.putFloat(0.78f); // iOpacity
        dataBuffer.putFloat(mouseX);
        dataBuffer.putFloat(mouseY);

        int count = Math.min(reveals != null ? reveals.size() : 0, MAX_CLICKS);
        dataBuffer.putInt(count);
        dataBuffer.putInt(0); // padding

        for (int i = 0; i < MAX_CLICKS; i++) {
            if (i < count) {
                ClickRevealData r = reveals.get(i);
                dataBuffer.putFloat(r.x);
                dataBuffer.putFloat(r.y);
                dataBuffer.putFloat(r.radius);
                dataBuffer.putFloat(r.fade);
            } else {
                dataBuffer.putFloat(0);
                dataBuffer.putFloat(0);
                dataBuffer.putFloat(0);
                dataBuffer.putFloat(0);
            }
        }

        dataBuffer.flip();
    }

    public static final class ClickRevealData {
        public final float x;
        public final float y;
        public final float radius;
        public final float fade;

        public ClickRevealData(float x, float y, float radius, float fade) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.fade = fade;
        }
    }
}
