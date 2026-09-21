package dile.ru.api.module.impl.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.WorldRenderEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.api.settings.impl.NumberSetting;
import dile.ru.utils.render.Render3D;
import dile.ru.utils.render.color.ColorUtil;
import dile.ru.utils.render.pipeline.ClientPipelines;
import dile.ru.utils.repository.friend.FriendUtils;

import java.awt.Color;

public final class GhostHat extends Module {
    private static GhostHat instance;
    private static final Identifier GHOST_TEXTURE = Identifier.fromNamespaceAndPath("dile", "textures/particles/ghost-glow.png");

    private final MultiModeSetting targets = register(new MultiModeSetting("Targets", "Who to display ghosts on.",
            new String[]{"Self", "Friends", "Players"}, "Players"));
    private final NumberSetting count = register(new NumberSetting("Count", "Number of ghosts per spiral.", 10.0, 1.0, 30.0, 1.0));
    private final NumberSetting height = register(new NumberSetting("Height", "Height above the head.", 0.0, -0.5, 2.0, 0.05));
    private final NumberSetting ghostSize = register(new NumberSetting("Size", "Size of each ghost particle.", 1.0, 0.1, 3.0, 0.1));
    private final NumberSetting orbitRadius = register(new NumberSetting("Radius", "Orbit radius around the head.", 0.8, 0.1, 3.0, 0.1));
    private final NumberSetting spiralCount = register(new NumberSetting("Spirals", "Number of spirals.", 3.0, 1.0, 5.0, 1.0));
    private final NumberSetting animSpeed = register(new NumberSetting("Speed", "Orbit animation speed.", 1.0, 0.1, 3.0, 0.1));
    private final ColorSetting color1 = register(new ColorSetting("Color 1", "Primary ghost color.", new Color(127, 242, 255, 136)));
    private final ColorSetting color2 = register(new ColorSetting("Color 2", "Secondary ghost color.", new Color(255, 50, 150, 255)));

    public GhostHat() {
        super("Ghost Hat", "Displays ghost particles orbiting around player heads.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static GhostHat getInstance() {
        return instance;
    }

    @SubscribeEvent
    private void onRender3D(WorldRenderEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) return;

        float tickDelta = event.getTickDelta();
        Vec3 camera = Render3D.lastCameraPos;
        PoseStack stack = event.getStack();
        if (camera == null) return;

        int ghostCount = (int) count.getFloat();
        double h = height.getValue();
        float ps = ghostSize.getFloat();
        double r = orbitRadius.getValue();
        int spirals = (int) spiralCount.getFloat();
        double sp = animSpeed.getValue();
        long frameTime = System.currentTimeMillis();

        int c1 = color1.getValue().getRGB();
        int c2 = color2.getValue().getRGB();

        for (Player player : mc.level.players()) {
            if (!shouldRender(player)) continue;

            double x = Mth.lerp(tickDelta, player.xo, player.getX()) - camera.x;
            double y = Mth.lerp(tickDelta, player.yo, player.getY()) - camera.y + player.getBbHeight() * 0.60 + h;
            double z = Mth.lerp(tickDelta, player.zo, player.getZ()) - camera.z;

            stack.pushPose();
            stack.translate(x, y, z);

            VertexConsumer consumer = provider().getBuffer(ClientPipelines.GHOSTS_ESP.apply(GHOST_TEXTURE));

            double distance = 20.0;
            for (int s = 0; s < spirals; s++) {
                for (int i = 0; i < ghostCount; i++) {
                    int idx = s * ghostCount + i;
                    stack.pushPose();

                    double angle = sp * ((frameTime * 0.55) - (i * distance)) / 40.0;
                    double sin = Math.sin(angle) * r;
                    double cos = Math.cos(angle) * r;

                    Vec3 trans = spiralOffset(s, sin, cos);
                    stack.translate(trans.x, trans.y, trans.z);
                    stack.mulPose(mc.gameRenderer.getMainCamera().rotation());

                    float spinRotation = (float) ((frameTime * 0.1) - (idx * 10.0));
                    stack.mulPose(Axis.ZP.rotationDegrees(spinRotation));
                    stack.translate(ps / 2.0f, ps / 2.0f, 0.0f);

                    int alpha = Math.max(4, 255 - idx);
                    int startColor = ColorUtil.withAlpha(c1, alpha);
                    int endColor = ColorUtil.withAlpha(c2, alpha);

                    PoseStack.Pose entry = stack.last();
                    consumer.addVertex(entry, 0.0f, -ps, 0.0f).setUv(0.0f, 0.0f).setColor(endColor);
                    consumer.addVertex(entry, -ps, -ps, 0.0f).setUv(0.0f, 1.0f).setColor(endColor);
                    consumer.addVertex(entry, -ps, 0.0f, 0.0f).setUv(1.0f, 1.0f).setColor(startColor);
                    consumer.addVertex(entry, 0.0f, 0.0f, 0.0f).setUv(1.0f, 0.0f).setColor(startColor);

                    stack.popPose();
                }
            }

            stack.popPose();
            provider().endBatch(ClientPipelines.GHOSTS_ESP.apply(GHOST_TEXTURE));
        }
    }

    private Vec3 spiralOffset(int spiral, double sin, double cos) {
        return switch (spiral % 3) {
            case 0 -> new Vec3(sin, cos, -cos);
            case 1 -> new Vec3(-sin, sin, -cos);
            default -> new Vec3(-sin, -sin, cos);
        };
    }

    private MultiBufferSource.BufferSource provider() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    private boolean shouldRender(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && !targets.isSelected("Self")) return false;
        if (player != mc.player && mc.player != null && mc.player.distanceTo(player) > 256) return false;
        if (FriendUtils.isFriend(player)) return targets.isSelected("Friends");
        return targets.isSelected("Players");
    }
}
