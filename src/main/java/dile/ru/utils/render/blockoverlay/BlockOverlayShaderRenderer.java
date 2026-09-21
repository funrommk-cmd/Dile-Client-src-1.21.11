package dile.ru.utils.render.blockoverlay;

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
import java.util.OptionalInt;

public final class BlockOverlayShaderRenderer {
    private static final Identifier PIPELINE_ID = Identifier.fromNamespaceAndPath("dile", "pipeline/effects/block_overlay");
    private static final Identifier VERTEX_SHADER = Identifier.fromNamespaceAndPath("dile", "effects/block_overlay/block_overlay");
    private static final Identifier FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("dile", "effects/block_overlay/block_overlay");
    private static final int UNIFORM_BYTES = 16;

    private static RenderPipeline pipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static ByteBuffer dataBuffer;

    private static float alpha = 0.9f;

    private BlockOverlayShaderRenderer() {
    }

    public static void setAlpha(float a) {
        alpha = a;
    }

    public static void render() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getMainRenderTarget() == null) {
            return;
        }

        if (pipeline == null) {
            initPipeline();
        }
        if (pipeline == null || uniformBuffer == null || dummyVertexBuffer == null || dataBuffer == null) {
            return;
        }

        try {
            float time = (System.nanoTime() % 180_000_000_000L) / 1_000_000_000.0f;
            float w = mc.getWindow().getWidth();
            float h = mc.getWindow().getHeight();

            writeUniforms(w, h, time);

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(uniformBuffer.slice(0, dataBuffer.remaining()), dataBuffer);

            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager._disableDepthTest();
            GlStateManager._disableCull();

            GpuTextureView colorView = mc.getMainRenderTarget().getColorTextureView();
            try (RenderPass renderPass = encoder.createRenderPass(
                    () -> "dile:block_overlay",
                    colorView,
                    OptionalInt.empty(),
                    null,
                    java.util.OptionalDouble.empty()
            )) {
                renderPass.setPipeline(pipeline);
                renderPass.setVertexBuffer(0, dummyVertexBuffer);
                renderPass.setUniform("BlockOverlayData", uniformBuffer.slice());
                renderPass.draw(0, 6);
            }

            GlStateManager._enableDepthTest();
            GlStateManager._enableCull();
            GlStateManager._disableBlend();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static void initPipeline() {
        try {
            pipeline = RenderPipeline.builder()
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                    .withUniform("BlockOverlayData", UniformType.UNIFORM_BUFFER)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build();

            dataBuffer = MemoryUtil.memAlloc(UNIFORM_BYTES);
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "dile:block_overlay_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    UNIFORM_BYTES
            );

            ByteBuffer dummyData = MemoryUtil.memAlloc(4);
            dummyData.putInt(0);
            dummyData.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "dile:block_overlay_dummy_vertex",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    dummyData
            );
            MemoryUtil.memFree(dummyData);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            pipeline = null;
        }
    }

    private static void writeUniforms(float w, float h, float time) {
        dataBuffer.clear();
        dataBuffer.putFloat(w);
        dataBuffer.putFloat(h);
        dataBuffer.putFloat(time);
        dataBuffer.putFloat(alpha);
        dataBuffer.flip();
    }

    public static void shutdown() {
        if (uniformBuffer != null) {
            uniformBuffer.close();
        }
        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
        }
        if (dataBuffer != null) {
            MemoryUtil.memFree(dataBuffer);
        }
        uniformBuffer = null;
        dummyVertexBuffer = null;
        dataBuffer = null;
        pipeline = null;
    }
}
