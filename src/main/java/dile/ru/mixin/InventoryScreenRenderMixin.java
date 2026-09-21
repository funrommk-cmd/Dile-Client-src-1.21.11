package dile.ru.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dile.ru.api.module.impl.visual.CustomInvsee;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenRenderMixin {
    @Redirect(method = "renderBg", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private void dile$customInvseeInventoryBg(GuiGraphics graphics, RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        if (!CustomInvsee.isActive()) {
            graphics.blit(pipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight);
            return;
        }
        CustomInvsee.render(graphics, (AbstractContainerScreen<?>) (Object) this, x, y, width, height);
    }
}
