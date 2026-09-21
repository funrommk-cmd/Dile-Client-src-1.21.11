package dile.ru.api.module.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.NumberSetting;

public final class NoVelocityDesync extends Module {
    private final ModeSetting mode = register(new ModeSetting("Mode", "Режим работы.", "Packet", "Packet", "Motion", "Both"));
    private final NumberSetting wallOffset = register(new NumberSetting("Wall Offset", "Смещение назад для имитации стены.", 0.1, 0.01, 0.5, 0.01));
    private final BooleanSetting onlyGround = register(new BooleanSetting("Only Ground", "Работать только на земле.", false));

    private boolean wasHurt;

    public NoVelocityDesync() {
        super("NoVelocityDesync", "Пакетный анти-отскок. При ударе отправляет пакет позиции, имитируя столкновение со стеной позади. Грим думает что ты не отлетел из-за стены.", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        wasHurt = false;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) return;

        boolean isHurt = client.player.hurtTime > 0;

        if (isHurt && !wasHurt) {
            if (onlyGround.getValue() && !client.player.onGround()) return;

            double x = client.player.getX();
            double y = client.player.getY();
            double z = client.player.getZ();
            float yaw = client.player.getYRot();
            float pitch = client.player.getXRot();

            double rad = Math.toRadians(yaw);
            double behindX = -Math.sin(rad) * wallOffset.getValue();
            double behindZ = Math.cos(rad) * wallOffset.getValue();

            if (!mode.is("Motion")) {
                double px = x + behindX;
                double pz = z + behindZ;
                client.player.connection.send(new ServerboundMovePlayerPacket.PosRot(px, y, pz, yaw, pitch, client.player.onGround(), client.player.horizontalCollision));
            }

            if (!mode.is("Packet")) {
                client.player.setDeltaMovement(0, client.player.getDeltaMovement().y, 0);
            }
        }

        wasHurt = isHurt;
    }
}
