package dile.ru.api.module.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.module.impl.combat.AuraModule;

public final class ElytraMotion extends Module {
    private boolean waitTarget;

    public ElytraMotion() {
        super("Elytra Motion", "Hovers in the air near the target while gliding.", ModuleCategory.MOVEMENT);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }

        LivingEntity target = AuraModule.target;

        if (target == null || !target.isAlive()) {
            if (!waitTarget) {
                client.player.setNoGravity(false);
                waitTarget = true;
            }
            return;
        }
        waitTarget = false;

        double dist = client.player.getEyePosition().distanceTo(target.getBoundingBox().getCenter());
        boolean shouldChase = target.isFallFlying() && target.getDeltaMovement().length() * 20 >= 13;

        float pon = target.isFallFlying() ? 1.5F : 3.0F;

        if (client.player.isFallFlying() && dist < pon && !shouldChase) {
            client.player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            client.player.setNoGravity(true);
        } else {
            client.player.setNoGravity(false);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) {
            mc.player.setNoGravity(false);
        }
        waitTarget = false;
    }
}
