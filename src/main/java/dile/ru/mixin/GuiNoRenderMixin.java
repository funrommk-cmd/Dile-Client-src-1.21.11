package dile.ru.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dile.ru.api.module.impl.visual.Hud;
import dile.ru.api.module.impl.visual.Hud2;
import dile.ru.api.module.impl.visual.NoRender;

@Mixin(Gui.class)
public abstract class GuiNoRenderMixin {
    @Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void dile$confusion(GuiGraphics graphics, float nauseaStrength, CallbackInfo ci) {
        if (NoRender.isActive("Nausea")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void dile$scoreboard(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (NoRender.isActive("Scoreboard") || Hud.shouldRenderCustomScoreboard()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBossOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void dile$bossOverlay(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (NoRender.isActive("Bossbar")) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void dile$hotbar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!Hud.shouldRenderCustomHotbar() && !Hud2.shouldRenderCustomHotbar()) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "renderChat", at = @At("HEAD"), cancellable = true, require = 0)
    private void dile$chat(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (Hud.shouldRenderCustomChat()) {
            ci.cancel();
        }
    }
}

