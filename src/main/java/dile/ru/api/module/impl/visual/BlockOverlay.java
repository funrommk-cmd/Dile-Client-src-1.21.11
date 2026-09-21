package dile.ru.api.module.impl.visual;

import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.WorldRenderEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.utils.render.Render3D;

import java.awt.Color;

public class BlockOverlay extends Module {
    private static BlockOverlay instance;

    private final ModeSetting fillMode = register(new ModeSetting("Fill", "Block fill mode.", "Regular", "Regular", "Shader"));
    private final ColorSetting color = register(new ColorSetting("Color", "Block overlay color.", new Color(109, 252, 255, 230)));

    public BlockOverlay() {
        super("Block Overlay", "Highlights the block you are looking at.", ModuleCategory.VISUAL);
        instance = this;
    }

    public static BlockOverlay getInstance() {
        return instance;
    }

    @SubscribeEvent
    private void onWorldRender(WorldRenderEvent event) {
        if (!(mc.hitResult instanceof BlockHitResult result) || !result.getType().equals(HitResult.Type.BLOCK)) {
            return;
        }

        BlockPos pos = result.getBlockPos();
        VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos);

        if (fillMode.is("Shader")) {
            float time = (float) ((System.nanoTime() % 10_000_000_000L) / 1_000_000_000.0);
            Render3D.drawShapeAlternative(pos, shape, shaderColor(pos, time), 1.5f, true, true);
            Render3D.drawShapeOverlay(pos, shape, color.getValue().getRGB(), 1.5f);
        } else {
            Render3D.drawShapeAlternative(pos, shape, color.getValue().getRGB(), 1.5f, true, true);
        }
    }

    private int shaderColor(BlockPos pos, float t) {
        float x = pos.getX() * 0.3f;
        float y = pos.getY() * 0.3f;
        float z = pos.getZ() * 0.3f;

        float q1 = fbm(x + t * 0.0f, y);
        float q2 = fbm(x + 1.0f, y + 1.0f);
        float r1 = fbm(x + q1 + 1.7f + 0.15f * t, y + q2 + 9.2f + 0.126f * t);
        float r2 = fbm(x + q1 + 8.3f + 0.126f * t, y + q2 + 2.8f);
        float f = fbm(x + r1, y + r2);

        float cr = f * f * f + 0.6f * f * f + 0.5f * f;

        float r = lerp(lerp(0.102f, 0.667f, clamp4(f)), 0.0f, clamp(length(q1, q2)));
        r = lerp(r, 0.667f, clamp(Math.abs(r1)));

        float g = lerp(lerp(0.196f, 0.667f, clamp4(f)), 0.0f, clamp(length(q1, q2)));
        g = lerp(g, 1.0f, clamp(Math.abs(r1)));

        float b = lerp(lerp(0.667f, 0.480f, clamp4(f)), 0.165f, clamp(length(q1, q2)));
        b = lerp(b, 1.0f, clamp(Math.abs(r1)));

        int ri = clampByte(cr * r);
        int gi = clampByte(cr * g);
        int bi = clampByte(cr * b);
        int ai = color.getValue().getAlpha();

        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private float fbm(float x, float y) {
        float v = 0, a = 0.5f;
        float cs = (float) Math.cos(0.5), sn = (float) Math.sin(0.5);
        for (int i = 0; i < 8; i++) {
            v += a * noise(x, y);
            float nx = cs * x * 2 + sn * y * 2 + 100;
            float ny = -sn * x * 2 + cs * y * 2 + 100;
            x = nx;
            y = ny;
            a *= 0.63f;
        }
        return v;
    }

    private float noise(float x, float y) {
        float ix = (float) Math.floor(x), iy = (float) Math.floor(y);
        float fx = x - ix, fy = y - iy;
        float a = hash(ix, iy), b = hash(ix + 1, iy);
        float c = hash(ix, iy + 1), d = hash(ix + 1, iy + 1);
        float ux = fx * fx * (3 - 2 * fx), uy = fy * fy * (3 - 2 * fy);
        return lerp(lerp(a, b, ux), lerp(c, d, ux), uy);
    }

    private float hash(float x, float y) {
        float n = (float) Math.sin(x * 12.9898f + y * 78.233f) * 43758.5453f;
        return n - (float) Math.floor(n);
    }

    private float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private float clamp(float v) { return Math.max(0, Math.min(1, v)); }
    private float clamp4(float v) { return clamp(v * v * 4); }
    private float length(float a, float b) { return (float) Math.sqrt(a * a + b * b); }
    private int clampByte(float v) { return Math.max(0, Math.min(255, (int) (v * 255))); }
}
