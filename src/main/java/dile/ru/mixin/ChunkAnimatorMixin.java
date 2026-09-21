package dile.ru.mixin;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dile.ru.api.module.impl.visual.ChunkAnimator;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class ChunkAnimatorMixin {
    @Shadow
    private long uploadedTime;

    @Shadow
    private long fadeDuration;

    @Shadow
    public int index;

    @Inject(method = "setFadeDuration", at = @At("HEAD"), cancellable = true)
    private void dile$chunkAnimatorSetFadeDuration(long fadeDuration, CallbackInfo ci) {
        ChunkAnimator animator = ChunkAnimator.getInstance();
        if (animator == null || !animator.isEnabled()) {
            return;
        }
        if (!animator.isFadeEmpty() && fadeDuration == 0L) {
            return;
        }
        ci.cancel();
        this.fadeDuration = animator.effectiveDuration(index);
    }

    @Inject(method = "getVisibility", at = @At("HEAD"), cancellable = true)
    private void dile$chunkAnimatorGetVisibility(long now, CallbackInfoReturnable<Float> cir) {
        ChunkAnimator animator = ChunkAnimator.getInstance();
        if (animator == null || !animator.isEnabled()) {
            return;
        }
        if (fadeDuration <= 0L) {
            return;
        }
        long elapsed = now - uploadedTime;
        if (elapsed <= 0L) {
            cir.setReturnValue(0.0F);
            return;
        }
        float progress = (float) elapsed / (float) fadeDuration;
        if (progress >= 1.0F) {
            cir.setReturnValue(1.0F);
            return;
        }
        cir.setReturnValue(animator.applyEasing(progress));
    }
}
