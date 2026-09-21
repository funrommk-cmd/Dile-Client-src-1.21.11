package dile.ru.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dile.ru.api.module.impl.visual.GlassHands;
import dile.ru.utils.render.shader.GlassHandsRenderer;

@Mixin(GameRenderer.class)
public abstract class GameRendererGlassHandsMixin {
    @Shadow
    @Final
    private FeatureRenderDispatcher featureRenderDispatcher;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Inject(
            method = "renderItemInHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"
            ),
            require = 0
    )
    private void dile$glassHandsCaptureBefore(float partialTicks, boolean renderBlockOutline, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (GlassHands.getInstance() != null && GlassHands.getInstance().isEnabled()) {
            GlassHandsRenderer.getInstance().captureSceneBeforeHands();
        }
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;renderScreenEffect(ZFLnet/minecraft/client/renderer/SubmitNodeCollector;)V"
            ),
            require = 0
    )
    private void dile$glassHandsRender(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (GlassHands.getInstance() == null || !GlassHands.getInstance().isEnabled()) {
            return;
        }

        GlassHandsRenderer renderer = GlassHandsRenderer.getInstance();
        if (!renderer.isEnabled()) {
            return;
        }

        renderer.captureSceneAfterHands();
        renderer.renderGlassEffect();
        featureRenderDispatcher.renderAllFeatures();
        renderBuffers.bufferSource().endBatch();
    }
}
