package dile.ru.mixin;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;
import dile.ru.api.events.impl.ChunkOcclusionEvent;
import dile.ru.manager.Manager;

@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {
    @ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean dile$disableAdvancedCulling(boolean advancedCulling) {
        ChunkOcclusionEvent event = Manager.postEvent(new ChunkOcclusionEvent());
        return event.isCancelled() ? false : advancedCulling;
    }
}
