package dile.ru.api.module.impl.combat.aura.rotations;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.module.impl.combat.aura.Angle;
import dile.ru.api.module.impl.combat.aura.AngleConnection;
import dile.ru.api.module.impl.combat.aura.MathAngle;
import dile.ru.api.module.impl.combat.aura.impl.RotateConstructor;
import dile.ru.api.module.impl.combat.aura.util.MathUtils;

public final class CakeAngle extends RotateConstructor {
    private int ticksOnTarget;
    private float prevYawDelta;
    private float prevPitchDelta;
    private float prevTotalDiff;

    public CakeAngle() {
        super("Cake");
    }

    @Override
    public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return currentAngle;
        }

        Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();
        float totalDiff = (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));

        if (totalDiff < 0.05F) {
            ticksOnTarget++;
        } else {
            ticksOnTarget = 0;
        }

        float progress = Mth.clamp((float) ticksOnTarget / 8.0F, 0.0F, 1.0F);

        float baseYawSpeed = 4.0F + progress * 4.0F;
        float basePitchSpeed = 1.2F + progress * 1.4F;

        float distanceFactor = 0.55F + Mth.clamp(totalDiff / 40.0F, 0.0F, 1.0F) * 0.85F;
        float settle = Mth.clamp(totalDiff / 4.0F, 0.25F, 1.0F);
        baseYawSpeed = Mth.clamp(baseYawSpeed * distanceFactor * settle, 1.5F, 9.0F);
        basePitchSpeed = Mth.clamp(basePitchSpeed * distanceFactor * settle, 0.5F, 2.5F);

        float inconsistency = MathUtils.getRandom(0.9F, 1.15F);
        baseYawSpeed *= inconsistency;
        basePitchSpeed *= inconsistency;

        float microJitterYaw = MathUtils.getRandom(-0.3F, 0.3F);
        float microJitterPitch = MathUtils.getRandom(-0.15F, 0.15F);

        if (totalDiff < 5.0F) {
            microJitterYaw *= 1.5F;
            microJitterPitch *= 1.5F;
        }

        float tracking = Mth.clamp(totalDiff / 6.0F, 0.0F, 1.0F);
        float yawSmoothing = 0.45F - tracking * 0.35F;
        float pitchSmoothing = 0.5F - tracking * 0.4F;

        float instantYaw = totalDiff > 0.0001F ? (yawDelta / totalDiff) * baseYawSpeed : 0.0F;
        float instantPitch = totalDiff > 0.0001F ? (pitchDelta / totalDiff) * basePitchSpeed : 0.0F;

        float smoothYaw = prevYawDelta * yawSmoothing + instantYaw * (1.0F - yawSmoothing);
        float smoothPitch = prevPitchDelta * pitchSmoothing + instantPitch * (1.0F - pitchSmoothing);

        prevYawDelta = smoothYaw;
        prevPitchDelta = smoothPitch;

        float finalYaw = Mth.clamp(smoothYaw + microJitterYaw, -baseYawSpeed * 1.6F, baseYawSpeed * 1.6F);
        float finalPitch = Mth.clamp(smoothPitch + microJitterPitch, -basePitchSpeed * 1.6F, basePitchSpeed * 1.6F);

        if (totalDiff < 1.5F) {
            float converge = Math.max(totalDiff / 1.5F, 0.35F);
            finalYaw *= converge;
            finalPitch *= converge;
        }

        if (prevTotalDiff > 1.5F && totalDiff <= 1.5F) {
            finalYaw += (float) Math.signum(yawDelta) * MathUtils.getRandom(0.2F, 0.5F);
            finalPitch += (float) Math.signum(pitchDelta) * MathUtils.getRandom(0.1F, 0.3F);
        }
        prevTotalDiff = totalDiff;

        Angle serverAngle = AngleConnection.INSTANCE.getServerAngle();
        float pitch = client.player.getXRot();
        if (serverAngle != null) {
            pitch = serverAngle.getPitch();
        }

        return new Angle(
                currentAngle.getYaw() + finalYaw,
                pitch + finalPitch
        );
    }

    @Override
    public Vec3 randomValue() {
        return new Vec3(
                MathUtils.getRandom(0.08F, 0.16F),
                MathUtils.getRandom(0.10F, 0.20F),
                MathUtils.getRandom(0.08F, 0.16F)
        );
    }
}
