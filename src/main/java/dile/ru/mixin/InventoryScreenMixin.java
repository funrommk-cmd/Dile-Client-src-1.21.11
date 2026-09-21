package dile.ru.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dile.ru.api.module.impl.visual.CustomInvsee;

@Mixin(AbstractContainerScreen.class)
public abstract class InventoryScreenMixin {
    @Shadow
    @Final
    protected int imageWidth;

    @Shadow
    @Final
    protected int imageHeight;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"), cancellable = true)
    private void dile$customInvseeBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!CustomInvsee.isActive()) {
            return;
        }
        if ((Object) this instanceof InventoryScreen || (Object) this instanceof CreativeModeInventoryScreen) {
            return;
        }
        ci.cancel();
        CustomInvsee.render(graphics, (AbstractContainerScreen<?>) (Object) this, leftPos, topPos, imageWidth, imageHeight);
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void dile$customInvseeLabels(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (CustomInvsee.isActive()) {
            ci.cancel();
        }
    }
}
