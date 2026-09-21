package dile.ru.mixin.accessor;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemLayerRenderStateAccessor {
    @Accessor("foilType")
    ItemStackRenderState.FoilType dile$getFoilType();

    @Accessor("transform")
    ItemTransform dile$getItemTransform();

    @Accessor("usesBlockLight")
    boolean dile$getUsesBlockLight();

    @Accessor("specialRenderer")
    SpecialModelRenderer<Object> dile$getSpecialRenderer();

    @Accessor("tintLayers")
    int[] dile$getTintLayers();
}
