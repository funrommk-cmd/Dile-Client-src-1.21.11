package dile.ru.utils.render.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
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
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class GlassHandsRenderer {
    public static final int VIEW_DEFAULT = 0;
    public static final int VIEW_NONE = 1;
    public static final int VIEW_SHADER1 = 2;
    public static final int VIEW_SHADER2 = 3;
    public static final int VIEW_SHADER3 = 4;
    public static final int VIEW_SHADER4 = 5;

    private static GlassHandsRenderer instance;

    private static final Identifier GLASS_PIPELINE_ID = Identifier.fromNamespaceAndPath("dile", "pipeline/effects/glass_hands");
    private static final Identifier BLUR_PIPELINE_ID = Identifier.fromNamespaceAndPath("dile", "pipeline/effects/glass_hands_blur");
    private static final Identifier SHADER_PIPELINE_ID = Identifier.fromNamespaceAndPath("dile", "pipeline/effects/glass_hands_shader");
    private static final Identifier SHADER3_PIPELINE_ID = Identifier.fromNamespaceAndPath("dile", "pipeline/effects/glass_hands_shader3");
    private static final Identifier SHADER4_PIPELINE_ID = Identifier.fromNamespaceAndPath("dile", "pipeline/effects/glass_hands_shader4");
    private static final Identifier FULLSCREEN_VERTEX_SHADER = Identifier.fromNamespaceAndPath("dile", "effects/glass_hands/fullscreen");
    private static final Identifier GLASS_FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("dile", "effects/glass_hands/glass");
    private static final Identifier BLUR_FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("dile", "effects/glass_hands/blur");
    private static final Identifier SHADER_FILL_FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("dile", "effects/glass_hands/shader_fill");
    private static final Identifier SHADER_FILL3_FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("dile", "effects/glass_hands/shader_fill3");
    private static final Identifier SHADER_FILL4_FRAGMENT_SHADER = Identifier.fromNamespaceAndPath("dile", "effects/glass_hands/shader_fill4");
    private static final int UNIFORM_BYTES = 64;

    private boolean enabled;
    private int viewMode = VIEW_DEFAULT;
    private boolean removeHand;
    private float shaderTransparency;
    private float blurRadius = 2.5f;
    private int blurIterations = 3;
    private float saturation = 0.0f;
    private boolean reflect = true;
    private int tintColor = 0x00000000;
    private float tintIntensity = 0.0f;
    private float edgeGlowIntensity = 0.2f;

    private RenderPipeline glassPipeline;
    private RenderPipeline blurPipeline;
    private RenderPipeline shaderPipeline;
    private RenderPipeline shaderPipeline3;
    private RenderPipeline shaderPipeline4;
    private GpuBuffer uniformBuffer;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;
    private GpuTexture captureTexture;
    private GpuTexture sceneTexture;
    private GpuTexture blurTextureA;
    private GpuTexture blurTextureB;
    private GpuTextureView captureTextureView;
    private GpuTextureView sceneTextureView;
    private GpuTextureView blurTextureViewA;
    private GpuTextureView blurTextureViewB;
    private boolean capturedBefore;
    private boolean capturedAfter;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private boolean disabledAfterError;

    private GlassHandsRenderer() {
    }

    public static GlassHandsRenderer getInstance() {
        if (instance == null) {
            instance = new GlassHandsRenderer();
        }
        return instance;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            invalidate();
        }
    }

    public void invalidate() {
        capturedBefore = false;
        capturedAfter = false;
        closeTextures();
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
        glassPipeline = null;
        blurPipeline = null;
        shaderPipeline = null;
        shaderPipeline3 = null;
        shaderPipeline4 = null;
        lastWidth = -1;
        lastHeight = -1;
        disabledAfterError = false;
        invalid = true;
    }

    private boolean invalid = true;

    public void setViewMode(int viewMode) {
        this.viewMode = viewMode;
    }

    public void setRemoveHand(boolean removeHand) {
        this.removeHand = removeHand;
    }

    public void setShaderTransparency(float shaderTransparency) {
        this.shaderTransparency = shaderTransparency;
    }

    public void setBlurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
    }

    public void setBlurIterations(int blurIterations) {
        this.blurIterations = blurIterations;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public void setReflect(boolean reflect) {
        this.reflect = reflect;
    }

    public void setTintColor(int tintColor) {
        this.tintColor = tintColor;
    }

    public void setTintIntensity(float tintIntensity) {
        this.tintIntensity = tintIntensity;
    }

    public void setEdgeGlowIntensity(float edgeGlowIntensity) {
        this.edgeGlowIntensity = edgeGlowIntensity;
    }

    public void captureSceneBeforeHands() {
        capturedBefore = false;
        if (!enabled || disabledAfterError || viewMode == VIEW_NONE) {
            return;
        }

        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (!isUsable(target)) {
            return;
        }

        if (!ensureReady(target.width, target.height)) {
            return;
        }

        try {
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .copyTextureToTexture(target.getColorTexture(), captureTexture, 0, 0, 0, 0, 0, target.width, target.height);
            capturedBefore = true;
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }

    public void captureSceneAfterHands() {
        capturedAfter = false;
        if (!capturedBefore || !enabled || disabledAfterError || viewMode == VIEW_NONE) {
            return;
        }

        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (!isUsable(target) || !ensureReady(target.width, target.height)) {
            return;
        }

        try {
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .copyTextureToTexture(target.getColorTexture(), sceneTexture, 0, 0, 0, 0, 0, target.width, target.height);
            capturedAfter = true;
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }

    public void renderGlassEffect() {
        if (!capturedBefore || !capturedAfter || disabledAfterError || viewMode == VIEW_NONE) {
            return;
        }
        capturedBefore = false;
        capturedAfter = false;

        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (!isUsable(target) || !ensureReady(target.width, target.height)) {
            return;
        }

        try {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            writeUniforms(target.width, target.height);
            encoder.writeToBuffer(uniformBuffer.slice(0, dataBuffer.remaining()), dataBuffer);

            if (viewMode == VIEW_SHADER1 || viewMode == VIEW_SHADER2) {
                renderShaderFill(encoder, target);
                return;
            }

            if (viewMode == VIEW_SHADER3) {
                renderShaderFill3(encoder, target);
                return;
            }

            if (viewMode == VIEW_SHADER4) {
                renderShaderFill4(encoder, target);
                return;
            }

            for (int i = 0; i < blurIterations; i++) {
                renderBlur(encoder, target, i == 0);
            }

            renderGlass(encoder, target);
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
    }

    private boolean ensureReady(int width, int height) {
        if (invalid || glassPipeline == null || blurPipeline == null || uniformBuffer == null || dummyVertexBuffer == null || dataBuffer == null) {
            initPipelines();
        }
        ensureTextures(width, height);
        boolean needsShader = viewMode == VIEW_SHADER1 || viewMode == VIEW_SHADER2 || viewMode == VIEW_SHADER3 || viewMode == VIEW_SHADER4;
        boolean shaderReady = viewMode == VIEW_SHADER3 ? shaderPipeline3 != null
                : (viewMode == VIEW_SHADER4 ? shaderPipeline4 != null : shaderPipeline != null);
        return !disabledAfterError
                && glassPipeline != null
                && blurPipeline != null
                && (!needsShader || shaderReady)
                && uniformBuffer != null
                && dummyVertexBuffer != null
                && dataBuffer != null
                && captureTexture != null
                && sceneTexture != null
                && blurTextureA != null
                && blurTextureB != null
                && captureTextureView != null
                && sceneTextureView != null
                && blurTextureViewA != null
                && blurTextureViewB != null;
    }

    private void initPipelines() {
        try {
            glassPipeline = RenderPipeline.builder()
                    .withLocation(GLASS_PIPELINE_ID)
                    .withVertexShader(FULLSCREEN_VERTEX_SHADER)
                    .withFragmentShader(GLASS_FRAGMENT_SHADER)
                    .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                    .withUniform("GlassHandsData", UniformType.UNIFORM_BUFFER)
                    .withSampler("CaptureSampler")
                    .withSampler("SceneSampler")
                    .withSampler("BlurSampler")
                    .withSampler("DepthSampler")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build();
            blurPipeline = RenderPipeline.builder()
                    .withLocation(BLUR_PIPELINE_ID)
                    .withVertexShader(FULLSCREEN_VERTEX_SHADER)
                    .withFragmentShader(BLUR_FRAGMENT_SHADER)
                    .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                    .withUniform("GlassHandsData", UniformType.UNIFORM_BUFFER)
                    .withSampler("InputSampler")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build();

            dataBuffer = MemoryUtil.memAlloc(UNIFORM_BYTES);
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "dile:glass_hands_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    UNIFORM_BYTES
            );
            ByteBuffer dummyData = MemoryUtil.memAlloc(4);
            dummyData.putInt(0);
            dummyData.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "dile:glass_hands_dummy_vertex",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    dummyData
            );
            MemoryUtil.memFree(dummyData);
            invalid = false;
        } catch (Throwable throwable) {
            disableAfterError(throwable);
        }
        buildShaderPipeline();
        buildShaderPipeline3();
        buildShaderPipeline4();
    }

    private void buildShaderPipeline() {
        if (shaderPipeline != null) {
            return;
        }
        try {
            shaderPipeline = RenderPipeline.builder()
                    .withLocation(SHADER_PIPELINE_ID)
                    .withVertexShader(FULLSCREEN_VERTEX_SHADER)
                    .withFragmentShader(SHADER_FILL_FRAGMENT_SHADER)
                    .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                    .withUniform("GlassHandsData", UniformType.UNIFORM_BUFFER)
                    .withSampler("CaptureSampler")
                    .withSampler("SceneSampler")
                    .withSampler("DepthSampler")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build();
        } catch (Throwable throwable) {
            shaderPipeline = null;
        }
    }

    private void buildShaderPipeline3() {
        if (shaderPipeline3 != null) {
            return;
        }
        try {
            shaderPipeline3 = RenderPipeline.builder()
                    .withLocation(SHADER3_PIPELINE_ID)
                    .withVertexShader(FULLSCREEN_VERTEX_SHADER)
                    .withFragmentShader(SHADER_FILL3_FRAGMENT_SHADER)
                    .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                    .withUniform("GlassHandsData", UniformType.UNIFORM_BUFFER)
                    .withSampler("CaptureSampler")
                    .withSampler("SceneSampler")
                    .withSampler("DepthSampler")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build();
        } catch (Throwable throwable) {
            shaderPipeline3 = null;
        }
    }

    private void buildShaderPipeline4() {
        if (shaderPipeline4 != null) {
            return;
        }
        try {
            shaderPipeline4 = RenderPipeline.builder()
                    .withLocation(SHADER4_PIPELINE_ID)
                    .withVertexShader(FULLSCREEN_VERTEX_SHADER)
                    .withFragmentShader(SHADER_FILL4_FRAGMENT_SHADER)
                    .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                    .withUniform("GlassHandsData", UniformType.UNIFORM_BUFFER)
                    .withSampler("CaptureSampler")
                    .withSampler("SceneSampler")
                    .withSampler("DepthSampler")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build();
        } catch (Throwable throwable) {
            shaderPipeline4 = null;
        }
    }

    private void ensureTextures(int width, int height) {
        if (captureTexture != null && width == lastWidth && height == lastHeight) {
            return;
        }

        closeTextures();
        captureTexture = createTexture("dile:glass_hands_capture", width, height);
        sceneTexture = createTexture("dile:glass_hands_scene", width, height);
        blurTextureA = createTexture("dile:glass_hands_blur_a", width, height);
        blurTextureB = createTexture("dile:glass_hands_blur_b", width, height);
        captureTextureView = RenderSystem.getDevice().createTextureView(captureTexture);
        sceneTextureView = RenderSystem.getDevice().createTextureView(sceneTexture);
        blurTextureViewA = RenderSystem.getDevice().createTextureView(blurTextureA);
        blurTextureViewB = RenderSystem.getDevice().createTextureView(blurTextureB);
        lastWidth = width;
        lastHeight = height;
    }

    private GpuTexture createTexture(String label, int width, int height) {
        return RenderSystem.getDevice().createTexture(
                () -> label,
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                TextureFormat.RGBA8,
                width,
                height,
                1,
                1
        );
    }

    private void writeUniforms(int width, int height) {
        int tintRed = (tintColor >> 16) & 0xFF;
        int tintGreen = (tintColor >> 8) & 0xFF;
        int tintBlue = tintColor & 0xFF;
        dataBuffer.clear();
        // params0: blurRadius, blurIterations, saturation, reflectEnabled
        dataBuffer.putFloat(blurRadius);
        dataBuffer.putFloat((float) blurIterations);
        dataBuffer.putFloat(saturation);
        dataBuffer.putFloat(reflect ? 1.0f : 0.0f);
        // params1: tintColor.r, tintColor.g, tintColor.b, tintIntensity
        dataBuffer.putFloat(tintRed / 255.0f);
        dataBuffer.putFloat(tintGreen / 255.0f);
        dataBuffer.putFloat(tintBlue / 255.0f);
        dataBuffer.putFloat(tintIntensity);
        // params2: edgeGlowIntensity, shaderTransparency, screenWidth, screenHeight
        dataBuffer.putFloat(edgeGlowIntensity);
        dataBuffer.putFloat(shaderTransparency);
        dataBuffer.putFloat((float) width);
        dataBuffer.putFloat((float) height);
        // params3: viewMode, removeHand, time, unused
        dataBuffer.putFloat((float) viewMode);
        dataBuffer.putFloat(removeHand ? 1.0f : 0.0f);
        dataBuffer.putFloat(System.nanoTime() / 1_000_000_000.0f);
        dataBuffer.putFloat(0.0f);
        dataBuffer.flip();
    }

    private void renderBlur(CommandEncoder encoder, RenderTarget target, boolean firstPass) {
        GpuTextureView inputView = firstPass ? sceneTextureView : blurTextureViewA;
        GpuTextureView outputView = blurTextureViewB;

        try (var renderPass = encoder.createRenderPass(
                () -> "dile:glass_hands_blur",
                outputView,
                OptionalInt.empty(),
                null,
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(blurPipeline);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("InputSampler", inputView, RenderSampler.linear());
            renderPass.setUniform("GlassHandsData", uniformBuffer.slice());
            renderPass.draw(0, 6);
        }

        GpuTexture temp = blurTextureA;
        blurTextureA = blurTextureB;
        blurTextureB = temp;
        GpuTextureView tempView = blurTextureViewA;
        blurTextureViewA = blurTextureViewB;
        blurTextureViewB = tempView;
    }

    private void renderShaderFill(CommandEncoder encoder, RenderTarget target) {
        renderShaderFillPipeline(encoder, target, shaderPipeline, "dile:glass_hands_shader");
    }

    private void renderShaderFill3(CommandEncoder encoder, RenderTarget target) {
        renderShaderFillPipeline(encoder, target, shaderPipeline3, "dile:glass_hands_shader3");
    }

    private void renderShaderFill4(CommandEncoder encoder, RenderTarget target) {
        renderShaderFillPipeline(encoder, target, shaderPipeline4, "dile:glass_hands_shader4");
    }

    private void renderShaderFillPipeline(CommandEncoder encoder, RenderTarget target, RenderPipeline pipeline, String label) {
        try (var renderPass = encoder.createRenderPass(
                () -> label,
                target.getColorTextureView(),
                OptionalInt.empty(),
                null,
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(pipeline);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("CaptureSampler", captureTextureView, RenderSampler.linear());
            renderPass.bindTexture("SceneSampler", sceneTextureView, RenderSampler.linear());
            renderPass.bindTexture("DepthSampler", target.getDepthTextureView(), RenderSampler.nearest());
            renderPass.setUniform("GlassHandsData", uniformBuffer.slice());
            renderPass.draw(0, 6);
        }
    }

    private void renderGlass(CommandEncoder encoder, RenderTarget target) {
        try (var renderPass = encoder.createRenderPass(
                () -> "dile:glass_hands_glass",
                target.getColorTextureView(),
                OptionalInt.empty(),
                null,
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(glassPipeline);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("CaptureSampler", captureTextureView, RenderSampler.linear());
            renderPass.bindTexture("SceneSampler", sceneTextureView, RenderSampler.linear());
            renderPass.bindTexture("BlurSampler", blurTextureViewA, RenderSampler.linear());
            renderPass.bindTexture("DepthSampler", target.getDepthTextureView(), RenderSampler.nearest());
            renderPass.setUniform("GlassHandsData", uniformBuffer.slice());
            renderPass.draw(0, 6);
        }
    }

    private boolean isUsable(RenderTarget target) {
        return target != null
                && target.getColorTexture() != null
                && target.getColorTextureView() != null
                && target.getDepthTextureView() != null
                && target.width > 0
                && target.height > 0;
    }

    private void disableAfterError(Throwable throwable) {
        disabledAfterError = true;
        throwable.printStackTrace();
        invalidate();
    }

    private void closeTextures() {
        if (captureTextureView != null) {
            captureTextureView.close();
        }
        if (sceneTextureView != null) {
            sceneTextureView.close();
        }
        if (blurTextureViewA != null) {
            blurTextureViewA.close();
        }
        if (blurTextureViewB != null) {
            blurTextureViewB.close();
        }
        if (captureTexture != null) {
            captureTexture.close();
        }
        if (sceneTexture != null) {
            sceneTexture.close();
        }
        if (blurTextureA != null) {
            blurTextureA.close();
        }
        if (blurTextureB != null) {
            blurTextureB.close();
        }
        captureTextureView = null;
        sceneTextureView = null;
        blurTextureViewA = null;
        blurTextureViewB = null;
        captureTexture = null;
        sceneTexture = null;
        blurTextureA = null;
        blurTextureB = null;
        lastWidth = -1;
        lastHeight = -1;
    }
}
