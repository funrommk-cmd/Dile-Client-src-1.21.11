package dile.ru.mixin.accessor;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
    @Accessor("activeLayerCount")
    int dile$getActiveLayerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] dile$getLayers();
}
