package dile.ru.api.module.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.WorldRenderEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.Render3D;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.repository.friend.FriendUtils;

import java.awt.Color;

public final class Chams extends Module {
    private static Chams instance;

    private final ModeSetting targets = register(new ModeSetting("Targets", "Who to apply chams to.", "All", "All", "Enemies", "Friends"));
    private final ColorSetting color = register(new ColorSetting("Color", "Chams color.", new Color(255, 100, 100, 255)));
    private final NumberSetting alpha = register(new NumberSetting("Alpha", "Wireframe alpha.", 200.0, 10.0, 255.0, 5.0));
    private final NumberSetting fillAlpha = register(new NumberSetting("Fill Alpha", "Fill alpha.", 80.0, 5.0, 200.0, 5.0));
    private final NumberSetting lineWidth = register(new NumberSetting("Line Width", "Outline thickness.", 1.5, 0.5, 5.0, 0.5));
    private final NumberSetting layers = register(new NumberSetting("Layers", "Number of render layers.", 1.0, 1.0, 3.0, 1.0));
    private final BooleanSetting throughWalls = register(new BooleanSetting("Through Walls", "Render through walls.", true));

    public Chams() {
        super("Chams", "Renders players as colored boxes.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static Chams getInstance() {
        return instance;
    }

    public static boolean shouldRender(Player player) {
        if (instance == null || !instance.isEnabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        if (player == mc.player) return false;
        if (mc.player.distanceTo(player) > 256) return false;
        String mode = instance.targets.getValue();
        return switch (mode) {
            case "Enemies" -> !FriendUtils.isFriend(player.getName().getString());
            case "Friends" -> FriendUtils.isFriend(player.getName().getString());
            default -> true;
        };
    }

    @SubscribeEvent
    private void onRender3D(WorldRenderEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        boolean depth = !throughWalls.getValue();
        int numLayers = (int) layers.getFloat();
        float lw = lineWidth.getFloat();

        for (Player player : mc.level.players()) {
            if (!shouldRender(player)) continue;

            AABB box = player.getBoundingBox();

            for (int i = 0; i < numLayers; i++) {
                double expand = i * 0.02;
                AABB layerBox = expand > 0 ? box.inflate(expand) : box;

                int fillColor = getFillColor(i);
                int lineColor = getLineColor(i);

                double x1 = layerBox.minX, y1 = layerBox.minY, z1 = layerBox.minZ;
                double x2 = layerBox.maxX, y2 = layerBox.maxY, z2 = layerBox.maxZ;
                Render3D.drawQuad(new Vec3(x1, y1, z1), new Vec3(x2, y1, z1), new Vec3(x2, y1, z2), new Vec3(x1, y1, z2), fillColor, depth);
                Render3D.drawQuad(new Vec3(x1, y1, z1), new Vec3(x1, y2, z1), new Vec3(x2, y2, z1), new Vec3(x2, y1, z1), fillColor, depth);
                Render3D.drawQuad(new Vec3(x2, y1, z1), new Vec3(x2, y2, z1), new Vec3(x2, y2, z2), new Vec3(x2, y1, z2), fillColor, depth);
                Render3D.drawQuad(new Vec3(x1, y1, z2), new Vec3(x2, y1, z2), new Vec3(x2, y2, z2), new Vec3(x1, y2, z2), fillColor, depth);
                Render3D.drawQuad(new Vec3(x1, y1, z1), new Vec3(x1, y1, z2), new Vec3(x1, y2, z2), new Vec3(x1, y2, z1), fillColor, depth);
                Render3D.drawQuad(new Vec3(x1, y2, z1), new Vec3(x1, y2, z2), new Vec3(x2, y2, z2), new Vec3(x2, y2, z1), fillColor, depth);

                Render3D.drawBox(layerBox, lineColor, lw, true, false, depth);
            }
        }
    }

    private int getLineColor(int layer) {
        Color c = color.getValue();
        int a = Math.max(10, (int) (alpha.getFloat() / (1f + layer * 1.5f)));
        return ColorUtil.rgba(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private int getFillColor(int layer) {
        Color c = color.getValue();
        int a = Math.max(5, (int) (fillAlpha.getFloat() / (1f + layer * 1.5f)));
        return ColorUtil.rgba(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
