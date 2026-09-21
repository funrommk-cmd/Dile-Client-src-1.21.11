package dile.ru.mixin;

import dile.ru.access.GuiRenderStateLayerAccessor;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin implements GuiRenderStateLayerAccessor {
    @Unique
    private int dile$layerSerial;

    @Override
    public int dile$getLayerSerial() {
        return dile$layerSerial;
    }

    @Inject(method = "nextStratum", at = @At("RETURN"))
    private void dile$trackNextStratum(CallbackInfo ci) {
        dile$layerSerial++;
    }

    @Inject(method = "up", at = @At("RETURN"))
    private void dile$trackUpLayer(CallbackInfo ci) {
        dile$layerSerial++;
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void dile$resetLayerSerial(CallbackInfo ci) {
        dile$layerSerial = 0;
    }
}
