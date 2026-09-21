package dile.ru.utils.render.post.worldglow;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import dile.ru.utils.render.RenderSampler;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

public final class WorldGlowRenderer {
    private static final int UNIFORM_SIZE = 176;
    private static final int MATRIX_OFFSET = 48;
    private static final int CAMERA_POS_OFFSET = 32;

    private static final Identifier PIPELINE_ID = Identifier.fromNamespaceAndPath("dile", "pipeline/post/world_glow");
    private static final Identifier VERTEX_SHADER = Identifier.fromNamespaceAndPath("dile", "post/world_glow/world_glow");
    private static final Identifier FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("dile", "post/world_glow/world_glow");

    private static RenderPipeline pipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static GpuTexture sceneCopyTexture;
    private static GpuTextureView sceneCopyTextureView;
    private static GpuTexture depthCopyTexture;
    private static GpuTextureView depthCopyTextureView;
    private static int lastWidth = -1;
    private static int lastHeight = -1;
    private static boolean disabledAfterError;

    private WorldGlowRenderer() {
    }

    public static boolean isDisabledAfterError() {
        return disabledAfterError;
    }

    public static void clear() {
        closeTextures();
    }

    public static void apply(RenderTarget renderTarget, int glowColor, float strength, float speed, float timeSeconds, Matrix4f projection, Matrix4f view, Vec3 cameraPos) {
        if (disabledAfterError || renderTarget == null) {
            return;
        }
        if (renderTarget.getColorTexture() == null || renderTarget.getColorTextureView() == null || renderTarget.getDepthTexture() == null || renderTarget.getDepthTextureView() == null) {
            return;
        }
        if (renderTarget.width <= 0 || renderTarget.height <= 0) {
            return;
        }

        init();
        if (pipeline == null || uniformBuffer == null || dummyVertexBuffer == null) {
            return;
        }
        if (!ensureTextures(renderTarget.width, renderTarget.height)) {
            return;
        }

        try {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.copyTextureToTexture(renderTarget.getColorTexture(), sceneCopyTexture, 0, 0, 0, 0, 0, renderTarget.width, renderTarget.height);
            encoder.copyTextureToTexture(renderTarget.getDepthTexture(), depthCopyTexture, 0, 0, 0, 0, 0, renderTarget.width, renderTarget.height);

            writeUniforms(encoder, glowColor, strength, speed, timeSeconds, projection, view, cameraPos);

            try (RenderPass renderPass = encoder.createRenderPass(
                    () -> "dile:world_glow",
                    renderTarget.getColorTextureView(),
                    OptionalInt.empty()
            )) {
                renderPass.setPipeline(pipeline);
                renderPass.setVertexBuffer(0, dummyVertexBuffer);
                renderPass.bindTexture("SceneSampler", sceneCopyTextureView, RenderSampler.linear());
                renderPass.bindTexture("DepthSampler", depthCopyTextureView, RenderSampler.nearest());
                renderPass.setUniform("WorldGlowData", uniformBuffer.slice());
                renderPass.draw(0, 6);
            }
        } catch (Throwable throwable) {
            disabledAfterError = true;
            throwable.printStackTrace();
            closeTextures();
            closeBuffers();
        }
    }

    private static void init() {
        if (disabledAfterError) {
            return;
        }

        try {
            if (pipeline == null) {
                pipeline = RenderPipelines.register(
                        RenderPipeline.builder()
                                .withLocation(PIPELINE_ID)
                                .withVertexShader(VERTEX_SHADER)
                                .withFragmentShader(FRAGMENT_SHADER)
                                .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                                .withUniform("WorldGlowData", UniformType.UNIFORM_BUFFER)
                                .withSampler("SceneSampler")
                                .withSampler("DepthSampler")
                                .withBlend(BlendFunction.TRANSLUCENT)
                                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                                .withDepthWrite(false)
                                .withCull(false)
                                .build()
                );
            }
            if (uniformBuffer == null || uniformBuffer.isClosed() || uniformBuffer.size() < UNIFORM_SIZE) {
                if (uniformBuffer != null) {
                    uniformBuffer.close();
                }
                uniformBuffer = RenderSystem.getDevice().createBuffer(
                        () -> "dile:world_glow_uniforms",
                        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                        UNIFORM_SIZE
                );
            }
            if (dummyVertexBuffer == null || dummyVertexBuffer.isClosed()) {
                if (dummyVertexBuffer != null) {
                    dummyVertexBuffer.close();
                }
                ByteBuffer dummyData = MemoryUtil.memAlloc(4);
                dummyData.putInt(0);
                dummyData.flip();
                dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                        () -> "dile:world_glow_dummy_vertex",
                        GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                        dummyData
                );
                MemoryUtil.memFree(dummyData);
            }
        } catch (Throwable throwable) {
            disabledAfterError = true;
            throwable.printStackTrace();
            pipeline = null;
            closeBuffers();
        }
    }

    private static boolean ensureTextures(int width, int height) {
        if (sceneCopyTexture != null && depthCopyTexture != null && lastWidth == width && lastHeight == height) {
            return true;
        }

        closeTextures();

        try {
            sceneCopyTexture = RenderSystem.getDevice().createTexture(
                    () -> "dile:world_glow_scene_copy",
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    TextureFormat.RGBA8,
                    width,
                    height,
                    1,
                    1
            );
            sceneCopyTextureView = RenderSystem.getDevice().createTextureView(sceneCopyTexture);
            depthCopyTexture = RenderSystem.getDevice().createTexture(
                    () -> "dile:world_glow_depth_copy",
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                    TextureFormat.DEPTH32,
                    width,
                    height,
                    1,
                    1
            );
            depthCopyTextureView = RenderSystem.getDevice().createTextureView(depthCopyTexture);
            lastWidth = width;
            lastHeight = height;
            return true;
        } catch (Throwable throwable) {
            disabledAfterError = true;
            throwable.printStackTrace();
            closeTextures();
            return false;
        }
    }

    private static void writeUniforms(CommandEncoder encoder, int glowColor, float strength, float speed, float timeSeconds, Matrix4f projection, Matrix4f view, Vec3 cameraPos) {
        Matrix4f inverseProjection = projection != null ? new Matrix4f(projection).invert() : null;
        Matrix4f inverseView = view != null ? new Matrix4f(view).invert() : null;
        if (inverseProjection == null || inverseView == null) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = stack.calloc(UNIFORM_SIZE);
            data.putFloat(0, strength);
            data.putFloat(4, speed);
            data.putFloat(8, timeSeconds);
            data.putFloat(12, 0.0f);
            data.putFloat(16, ((glowColor >> 16) & 0xFF) / 255.0f);
            data.putFloat(20, ((glowColor >> 8) & 0xFF) / 255.0f);
            data.putFloat(24, (glowColor & 0xFF) / 255.0f);
            data.putFloat(28, ((glowColor >>> 24) & 0xFF) / 255.0f);
            data.putFloat(CAMERA_POS_OFFSET, cameraPos != null ? (float) cameraPos.x : 0.0f);
            data.putFloat(CAMERA_POS_OFFSET + 4, cameraPos != null ? (float) cameraPos.y : 0.0f);
            data.putFloat(CAMERA_POS_OFFSET + 8, cameraPos != null ? (float) cameraPos.z : 0.0f);
            data.putFloat(CAMERA_POS_OFFSET + 12, 1.0f);
            putMatrix(data, MATRIX_OFFSET, inverseProjection);
            putMatrix(data, MATRIX_OFFSET + 64, inverseView);
            data.position(0);
            encoder.writeToBuffer(uniformBuffer.slice(0, UNIFORM_SIZE), data);
        }
    }

    private static void putMatrix(ByteBuffer buffer, int offset, Matrix4f matrix) {
        buffer.putFloat(offset, matrix.m00());
        buffer.putFloat(offset + 4, matrix.m01());
        buffer.putFloat(offset + 8, matrix.m02());
        buffer.putFloat(offset + 12, matrix.m03());
        buffer.putFloat(offset + 16, matrix.m10());
        buffer.putFloat(offset + 20, matrix.m11());
        buffer.putFloat(offset + 24, matrix.m12());
        buffer.putFloat(offset + 28, matrix.m13());
        buffer.putFloat(offset + 32, matrix.m20());
        buffer.putFloat(offset + 36, matrix.m21());
        buffer.putFloat(offset + 40, matrix.m22());
        buffer.putFloat(offset + 44, matrix.m23());
        buffer.putFloat(offset + 48, matrix.m30());
        buffer.putFloat(offset + 52, matrix.m31());
        buffer.putFloat(offset + 56, matrix.m32());
        buffer.putFloat(offset + 60, matrix.m33());
    }

    private static void closeTextures() {
        if (sceneCopyTextureView != null) {
            sceneCopyTextureView.close();
            sceneCopyTextureView = null;
        }
        if (sceneCopyTexture != null) {
            sceneCopyTexture.close();
            sceneCopyTexture = null;
        }
        if (depthCopyTextureView != null) {
            depthCopyTextureView.close();
            depthCopyTextureView = null;
        }
        if (depthCopyTexture != null) {
            depthCopyTexture.close();
            depthCopyTexture = null;
        }
        lastWidth = -1;
        lastHeight = -1;
    }

    private static void closeBuffers() {
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
            dummyVertexBuffer = null;
        }
    }
}
