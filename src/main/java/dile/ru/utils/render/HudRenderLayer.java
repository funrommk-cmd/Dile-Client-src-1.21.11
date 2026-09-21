package dile.ru.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import dile.ru.api.events.impl.DrawEvent;
import dile.ru.api.module.impl.visual.Hud;
import dile.ru.api.module.impl.visual.Hud2;
import dile.ru.api.module.impl.visual.Hud3;
import dile.ru.api.module.impl.visual.Hud4;
import dile.ru.api.module.impl.visual.Hud5;
import dile.ru.manager.Manager;
import dile.ru.utils.render.ui.Render2D;

public final class HudRenderLayer {
    private HudRenderLayer() {
    }

    public static boolean shouldRender(Minecraft client) {
        return client != null
                && client.player != null
                && client.level != null
                && client.getWindow() != null
                && !LoadingVisualGuard.shouldSuppressHud(client);
    }

    public static void renderGame(GuiGraphics graphics, float partialTick) {
        renderGame(Minecraft.getInstance(), graphics, partialTick);
    }

    public static void renderGame(Minecraft client, GuiGraphics graphics, float partialTick) {
        renderComposite(client, graphics, partialTick, DrawEvent.Layer.GAME);
    }

    public static void renderGameDrawEvents(GuiGraphics graphics, float partialTick) {
        renderDrawEvent(graphics, partialTick, DrawEvent.Layer.GAME);
    }

    public static void renderGameHud(Minecraft client, GuiGraphics graphics, float partialTick) {
        renderHudOnly(client, graphics, partialTick, DrawEvent.Layer.GAME);
    }

    public static void renderScreenBackground(Minecraft client, GuiGraphics graphics, float partialTick) {
        if (!shouldRender(client) || client.screen == null) {
            return;
        }

        renderHudOnly(client, graphics, partialTick, DrawEvent.Layer.SCREEN_BACKGROUND);
    }

    public static void renderChatOverlay(Minecraft client, GuiGraphics graphics, float partialTick) {
        renderHudOnly(client, graphics, partialTick, DrawEvent.Layer.CHAT_OVERLAY);
    }

    private static void renderComposite(Minecraft client, GuiGraphics graphics, float partialTick, DrawEvent.Layer hudLayer) {
        if (!shouldRender(client)) {
            return;
        }

        renderDrawEvent(graphics, partialTick, DrawEvent.Layer.GAME);
        renderHudOnly(client, graphics, partialTick, hudLayer);
    }

    private static void renderHudOnly(Minecraft client, GuiGraphics graphics, float partialTick, DrawEvent.Layer layer) {
        if (!shouldRender(client)) {
            return;
        }

        Hud hud = Hud.getInstance();
        Hud2 hud2 = Hud2.getInstance();
        Hud3 hud3 = Hud3.getInstance();
        Hud4 hud4 = Hud4.getInstance();
        Hud5 hud5 = Hud5.getInstance();
        if ((hud == null || !hud.isEnabled()) && (hud2 == null || !hud2.isEnabled()) && (hud3 == null || !hud3.isEnabled()) && (hud4 == null || !hud4.isEnabled()) && (hud5 == null || !hud5.isEnabled())) {
            return;
        }

        Render2D.beginFrame(graphics);
        DrawEvent drawEvent = new DrawEvent(graphics, partialTick, layer);
        if (hud != null && hud.isEnabled()) {
            hud.renderHudLayer(drawEvent);
        }
        if (hud2 != null && hud2.isEnabled()) {
            hud2.renderHudLayer(drawEvent);
        }
        if (hud3 != null && hud3.isEnabled()) {
            hud3.renderHudLayer(drawEvent);
        }
        if (hud4 != null && hud4.isEnabled()) {
            hud4.renderHudLayer(drawEvent);
        }
        if (hud5 != null && hud5.isEnabled()) {
            hud5.renderHudLayer(drawEvent);
        }
        Render2D.flush();
        graphics.nextStratum();
    }

    private static void renderDrawEvent(GuiGraphics graphics, float partialTick, DrawEvent.Layer layer) {
        Render2D.beginFrame(graphics);
        Manager.postEvent(new DrawEvent(graphics, partialTick, layer));
        Render2D.flush();
        graphics.nextStratum();
    }
}
