package dile.ru.utils.render.world.skyshader;

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

import static java.lang.Math.toRadians;

public final class SkyShaderRenderer {
    private static final Identifier PIPELINE_ID = Identifier.fromNamespaceAndPath("dile", "pipeline/effects/sky_shader");
    private static final Identifier VERTEX_SHADER = Identifier.fromNamespaceAndPath("dile", "effects/sky_shader/sky");
    private static final Identifier FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("dile", "effects/sky_shader/sky");
    private static final int UNIFORM_BYTES = 56;

    private static RenderPipeline pipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static ByteBuffer dataBuffer;

    private static float skyR = 0.3f;
    private static float skyG = 0.5f;
    private static float skyB = 1.0f;
    private static float speed = 1.0f;
    private static float scale = 5.0f;
    private static float intensity = 0.01f;
    private static float alpha = 1.0f;
    private static float mode = 0.0f;

    private SkyShaderRenderer() {
    }

    public static void configure(float r, float g, float b, float spd, float scl, float inten, float a, float m) {
        skyR = r;
        skyG = g;
        skyB = b;
        speed = spd;
        scale = scl;
        intensity = inten;
        alpha = a;
        mode = m;
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
            float yaw = (float) toRadians(mc.player.getYRot());
            float pitch = (float) toRadians(mc.player.getXRot());
            float fov = mc.options.fov().get();

            writeUniforms(time, yaw, pitch, fov);

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(uniformBuffer.slice(0, dataBuffer.remaining()), dataBuffer);

            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GL11.GL_EQUAL);
            GlStateManager._depthMask(false);
            GlStateManager._disableCull();
            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GpuTextureView colorView = mc.getMainRenderTarget().getColorTextureView();
            GpuTextureView depthView = mc.getMainRenderTarget().getDepthTextureView();
            try (RenderPass renderPass = encoder.createRenderPass(
                    () -> "dile:sky_shader",
                    colorView,
                    OptionalInt.empty(),
                    depthView,
                    java.util.OptionalDouble.empty()
            )) {
                renderPass.setPipeline(pipeline);
                renderPass.setVertexBuffer(0, dummyVertexBuffer);
                renderPass.setUniform("SkyData", uniformBuffer.slice());
                renderPass.draw(0, 6);
            }

            GlStateManager._depthMask(true);
            GlStateManager._depthFunc(GL11.GL_LEQUAL);
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
                    .withUniform("SkyData", UniformType.UNIFORM_BUFFER)
                    .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build();

            dataBuffer = MemoryUtil.memAlloc(UNIFORM_BYTES);
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "dile:sky_shader_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    UNIFORM_BYTES
            );

            ByteBuffer dummyData = MemoryUtil.memAlloc(4);
            dummyData.putInt(0);
            dummyData.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "dile:sky_shader_dummy_vertex",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    dummyData
            );
            MemoryUtil.memFree(dummyData);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            pipeline = null;
        }
    }

    private static void writeUniforms(float time, float yaw, float pitch, float fov) {
        Minecraft mc = Minecraft.getInstance();
        float fw = mc.getWindow().getWidth();
        float fh = mc.getWindow().getHeight();

        dataBuffer.clear();
        dataBuffer.putFloat(skyR);
        dataBuffer.putFloat(skyG);
        dataBuffer.putFloat(skyB);
        dataBuffer.putFloat(alpha);
        dataBuffer.putFloat(speed);
        dataBuffer.putFloat(scale);
        dataBuffer.putFloat(intensity);
        dataBuffer.putFloat(mode);
        dataBuffer.putFloat(yaw);
        dataBuffer.putFloat(pitch);
        dataBuffer.putFloat(fov);
        dataBuffer.putFloat(time);
        dataBuffer.putFloat(fw);
        dataBuffer.putFloat(fh);
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
