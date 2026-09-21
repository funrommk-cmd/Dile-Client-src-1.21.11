package dile.ru.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import dile.ru.screens.modernui.impl.WorldAnimation;
import dile.ru.utils.render.ui.blur.BlurFramebuffer;
import dile.ru.utils.render.ui.glass.GlassRenderer;
import dile.ru.utils.render.ui.image.ImageRenderer;
import dile.ru.utils.render.ui.outline.outline360.Outline360Renderer;
import dile.ru.utils.render.ui.outline.outlinedefault.DefaultOutlineRenderer;
import dile.ru.utils.render.ui.outline.outlineglass.GlassOutlineRenderer;
import dile.ru.utils.render.ui.arc.ArcOutlineRenderer;
import dile.ru.utils.render.ui.arc.ArcRenderer;
import dile.ru.utils.render.ui.rectangle.rectdefault.DefaultRectangleRenderer;
import dile.ru.utils.render.ui.rectangle.recthalficon.HalfIconRectangleRenderer;
import dile.ru.utils.render.ui.rectangle.recthalftone.HalftoneRectangleRenderer;
import dile.ru.utils.render.ui.ripple.RippleRenderer;
import dile.ru.utils.render.ui.zippy.ZippyRenderer;
import dile.ru.utils.render.ui.menubackground.MenuBackgroundRenderer;
import dile.ru.utils.render.item.RenderItem;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    private RenderPass dile$currentRenderPass;
    private boolean dile$blurDrawActive;
    private boolean dile$glassDrawActive;
    private boolean dile$glassOutlineDrawActive;
    private boolean dile$rectangleDrawActive;
    private boolean dile$halfIconRectangleDrawActive;
    private boolean dile$halftoneRectangleDrawActive;
    private boolean dile$zippyDrawActive;
    private boolean dile$arcDrawActive;
    private boolean dile$arcOutlineDrawActive;
    private boolean dile$outlineDrawActive;
    private boolean dile$outline360DrawActive;
    private boolean dile$imageDrawActive;
    private boolean dile$itemDrawActive;
    private boolean dile$rippleDrawActive;
    private boolean dile$menuBackgroundDrawActive;

    @Redirect(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V", ordinal = 0))
    private void dile$useModernWorldProjection(GpuBufferSlice projection, ProjectionType projectionType) {
        GpuBufferSlice override = WorldAnimation.projectionOverride();
        if (override != null) {
            RenderSystem.setProjectionMatrix(override, ProjectionType.PERSPECTIVE);
            return;
        }
        RenderSystem.setProjectionMatrix(projection, projectionType);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void dile$beginBlurFrame(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        BlurFramebuffer.getInstance().beginGuiFrame();
        GlassRenderer.getInstance().beginGuiFrame();
        GlassOutlineRenderer.getInstance().beginGuiFrame();
        DefaultRectangleRenderer.getInstance().beginGuiFrame();
        HalfIconRectangleRenderer.getInstance().beginGuiFrame();
        HalftoneRectangleRenderer.getInstance().beginGuiFrame();
        ZippyRenderer.getInstance().beginGuiFrame();
        ArcRenderer.getInstance().beginGuiFrame();
        ArcOutlineRenderer.getInstance().beginGuiFrame();
        DefaultOutlineRenderer.getInstance().beginGuiFrame();
        Outline360Renderer.getInstance().beginGuiFrame();
        ImageRenderer.getInstance().beginGuiFrame();
        RippleRenderer.getInstance().beginGuiFrame();
        MenuBackgroundRenderer.getInstance().beginGuiFrame();
        RenderItem.beginGuiFrame();
    }

    @Inject(method = "prepare", at = @At("HEAD"))
    private void dile$preparePendingBlurResources(CallbackInfo ci) {
        BlurFramebuffer.getInstance().preparePending();
    }

    @Inject(method = "prepare", at = @At("RETURN"))
    private void dile$prepareRenderUniforms(CallbackInfo ci) {
        BlurFramebuffer.getInstance().prepareBuffers();
        GlassRenderer.getInstance().prepareBuffers();
        GlassOutlineRenderer.getInstance().prepareBuffers();
        DefaultRectangleRenderer.getInstance().prepareBuffers();
        HalfIconRectangleRenderer.getInstance().prepareBuffers();
        HalftoneRectangleRenderer.getInstance().prepareBuffers();
        ZippyRenderer.getInstance().prepareBuffers();
        ArcRenderer.getInstance().prepareBuffers();
        ArcOutlineRenderer.getInstance().prepareBuffers();
        DefaultOutlineRenderer.getInstance().prepareBuffers();
        Outline360Renderer.getInstance().prepareBuffers();
        ImageRenderer.getInstance().prepareBuffers();
        RippleRenderer.getInstance().prepareBuffers();
        MenuBackgroundRenderer.getInstance().prepareBuffers();
        RenderItem.prepareBuffers();
    }

    @Inject(method = "draw", at = @At("HEAD"))
    private void dile$prepareBlurCapture(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        BlurFramebuffer.getInstance().prepareGuiDraw();
    }

    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V", shift = At.Shift.BEFORE))
    private void dile$prepareBlurCaptureAfterBeforeBlur(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        BlurFramebuffer.getInstance().prepareGuiDraw();
    }

    @Redirect(method = "executeDraw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"))
    private void dile$trackPipeline(RenderPass renderPass, RenderPipeline pipeline) {
        dile$currentRenderPass = renderPass;
        dile$blurDrawActive = BlurFramebuffer.getInstance().isBlurPipeline(pipeline);
        dile$glassDrawActive = GlassRenderer.getInstance().isGlassPipeline(pipeline);
        dile$glassOutlineDrawActive = GlassOutlineRenderer.getInstance().isGlassOutlinePipeline(pipeline);
        dile$rectangleDrawActive = DefaultRectangleRenderer.getInstance().isRectanglePipeline(pipeline);
        dile$halfIconRectangleDrawActive = HalfIconRectangleRenderer.getInstance().isHalfIconRectanglePipeline(pipeline);
        dile$halftoneRectangleDrawActive = HalftoneRectangleRenderer.getInstance().isHalftoneRectanglePipeline(pipeline);
        dile$zippyDrawActive = ZippyRenderer.getInstance().isZippyPipeline(pipeline);
        dile$arcDrawActive = ArcRenderer.getInstance().isArcPipeline(pipeline);
        dile$arcOutlineDrawActive = ArcOutlineRenderer.getInstance().isArcOutlinePipeline(pipeline);
        dile$outlineDrawActive = DefaultOutlineRenderer.getInstance().isOutlinePipeline(pipeline);
        dile$outline360DrawActive = Outline360Renderer.getInstance().isOutline360Pipeline(pipeline);
        dile$imageDrawActive = ImageRenderer.getInstance().isImagePipeline(pipeline);
        dile$rippleDrawActive = RippleRenderer.getInstance().isRipplePipeline(pipeline);
        dile$menuBackgroundDrawActive = MenuBackgroundRenderer.getInstance().isMenuBackgroundPipeline(pipeline);
        dile$itemDrawActive = RenderItem.isItemPipeline(pipeline);
        renderPass.setPipeline(pipeline);
    }

    @Inject(method = "executeDraw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderPass;drawIndexed(IIII)V", shift = At.Shift.BEFORE))
    private void dile$bindBlurParams(@Coerce Object draw, RenderPass renderPass, GpuBuffer gpuBuffer, VertexFormat.IndexType indexType, CallbackInfo ci) {
        if (dile$blurDrawActive && dile$currentRenderPass != null) {
            BlurFramebuffer.getInstance().bindBlurParams(dile$currentRenderPass);
        }
        if (dile$glassDrawActive && dile$currentRenderPass != null) {
            GlassRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$glassOutlineDrawActive && dile$currentRenderPass != null) {
            GlassOutlineRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$rectangleDrawActive && dile$currentRenderPass != null) {
            DefaultRectangleRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$halfIconRectangleDrawActive && dile$currentRenderPass != null) {
            HalfIconRectangleRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$halftoneRectangleDrawActive && dile$currentRenderPass != null) {
            HalftoneRectangleRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$zippyDrawActive && dile$currentRenderPass != null) {
            ZippyRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$arcDrawActive && dile$currentRenderPass != null) {
            ArcRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$arcOutlineDrawActive && dile$currentRenderPass != null) {
            ArcOutlineRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$outlineDrawActive && dile$currentRenderPass != null) {
            DefaultOutlineRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$outline360DrawActive && dile$currentRenderPass != null) {
            Outline360Renderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$imageDrawActive && dile$currentRenderPass != null) {
            ImageRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$rippleDrawActive && dile$currentRenderPass != null) {
            RippleRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$menuBackgroundDrawActive && dile$currentRenderPass != null) {
            MenuBackgroundRenderer.getInstance().bindParams(dile$currentRenderPass);
        }
        if (dile$itemDrawActive && dile$currentRenderPass != null) {
            RenderItem.bindParams(dile$currentRenderPass);
        }
    }

    @Inject(method = "executeDraw", at = @At("RETURN"))
    private void dile$clearTrackedPipeline(@Coerce Object draw, RenderPass renderPass, GpuBuffer gpuBuffer, VertexFormat.IndexType indexType, CallbackInfo ci) {
        dile$currentRenderPass = null;
        dile$blurDrawActive = false;
        dile$glassDrawActive = false;
        dile$glassOutlineDrawActive = false;
        dile$rectangleDrawActive = false;
        dile$halfIconRectangleDrawActive = false;
        dile$halftoneRectangleDrawActive = false;
        dile$zippyDrawActive = false;
        dile$arcDrawActive = false;
        dile$arcOutlineDrawActive = false;
        dile$outlineDrawActive = false;
        dile$outline360DrawActive = false;
        dile$imageDrawActive = false;
        dile$itemDrawActive = false;
        dile$rippleDrawActive = false;
        dile$menuBackgroundDrawActive = false;
    }
}
