package dile.ru.utils.render.ui.menubackground;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dile.ru.mixin.accessor.GuiGraphicsExtractorAccessor;
import dile.ru.utils.render.ui.Render2DCoordinateSpace;
import dile.ru.utils.render.ScissorUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

public final class MenuBackgroundRenderer implements AutoCloseable {
    private static volatile MenuBackgroundRenderer instance;

    public static final RenderPipeline MENU_BACKGROUND_PIPELINE = RenderPipeline.builder()
            .withLocation(id("pipeline/menu_background"))
            .withVertexShader(id("ui/menu_background/menu_background"))
            .withFragmentShader(id("ui/menu_background/menu_background"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(false)
            .withUniform("DynamicTransforms", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .withUniform("MenuBackgroundParams", com.mojang.blaze3d.shaders.UniformType.UNIFORM_BUFFER)
            .build();

    private float time;
    private float mouseX;
    private float mouseY;
    private float resolutionX;
    private float resolutionY;
    private boolean active;
    private GuiGraphics activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;
    private boolean prepared;

    private MenuBackgroundRenderer() {
    }

    public static MenuBackgroundRenderer getInstance() {
        MenuBackgroundRenderer local = instance;
        if (local == null) {
            synchronized (MenuBackgroundRenderer.class) {
                local = instance;
                if (local == null) {
                    local = new MenuBackgroundRenderer();
                    instance = local;
                }
            }
        }
        return local;
    }

    public static void closeInstance() {
        MenuBackgroundRenderer local = instance;
        if (local != null) {
            local.close();
            instance = null;
        }
    }

    public void setParams(float time, float mouseX, float mouseY, float resolutionX, float resolutionY) {
        this.time = time;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.resolutionX = resolutionX;
        this.resolutionY = resolutionY;
        this.active = true;
        this.paramsDirty = true;
    }

    public void beginFrame(GuiGraphics graphics) {
        activeGraphics = graphics;
    }

    public void enqueue(float x, float y, float width, float height) {
        if (!active || activeGraphics == null) {
            return;
        }

        try {
            Matrix3x2f pose = Render2DCoordinateSpace.pose(activeGraphics);
            ((GuiGraphicsExtractorAccessor) activeGraphics)
                    .dile$getGuiRenderState()
                    .submitGuiElement(new MenuBackgroundRenderState(pose, x, y, width, height, ScissorUtil.current()));
        } catch (RuntimeException ignored) {
        }
    }

    public void flush() {
        activeGraphics = null;
        active = false;
    }

    public void beginGuiFrame() {
        prepared = false;
        paramsDirty = false;
        active = false;
    }

    public boolean isMenuBackgroundPipeline(RenderPipeline pipeline) {
        return pipeline == MENU_BACKGROUND_PIPELINE;
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || !prepared) {
            return;
        }

        GpuBuffer buffer = ensureParamsBuffer();
        if (buffer != null) {
            renderPass.setUniform("MenuBackgroundParams", buffer);
        }
    }

    public void prepareBuffers() {
        if (!active || !paramsDirty) {
            return;
        }

        GpuBuffer buffer = ensureWritableParamsBuffer();
        if (buffer == null) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = buildUniformData(stack);
            RenderSystem.getDevice()
                    .createCommandEncoder()
                    .writeToBuffer(buffer.slice(0, data.remaining()), data);
            paramsDirty = false;
            prepared = true;
        } catch (RuntimeException ignored) {
            paramsDirty = true;
        }
    }

    private GpuBuffer ensureParamsBuffer() {
        if (!paramsDirty && paramsBuffer != null) {
            return paramsBuffer;
        }

        prepareBuffers();
        if (!paramsDirty && paramsBuffer != null) {
            return paramsBuffer;
        }

        closeParamsBuffer();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = buildUniformData(stack);
            paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "RELEON_menu_bg_params", GpuBuffer.USAGE_UNIFORM, data);
            paramsDirty = false;
            prepared = true;
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (paramsBuffer != null && !paramsBuffer.isClosed() && paramsBuffer.size() >= 32) {
            return paramsBuffer;
        }

        closeParamsBuffer();

        try {
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "RELEON_menu_bg_params",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    32
            );
            return paramsBuffer;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private ByteBuffer buildUniformData(MemoryStack stack) {
        ByteBuffer data = stack.calloc(32);

        data.putFloat(0, time);
        data.putFloat(4, mouseX);
        data.putFloat(8, mouseY);
        data.putFloat(12, 0.0f);

        data.putFloat(16, resolutionX);
        data.putFloat(20, resolutionY);
        data.putFloat(24, 0.0f);
        data.putFloat(28, 0.0f);

        data.position(0);
        return data;
    }

    private void closeParamsBuffer() {
        if (paramsBuffer != null) {
            paramsBuffer.close();
            paramsBuffer = null;
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("dile", path);
    }

    @Override
    public void close() {
        active = false;
        activeGraphics = null;
        closeParamsBuffer();
    }
}
