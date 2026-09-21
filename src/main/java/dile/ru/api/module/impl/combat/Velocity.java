package dile.ru.api.module.impl.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.PacketEvent;
import dile.ru.api.events.impl.TickEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ModeSetting;

public class Velocity extends Module {
    private final ModeSetting mode = register(new ModeSetting("Mode", "Knockback reduce mode.", "New Grim", "New Grim", "Old Grim", "Matrix", "Normal"));
    private final BooleanSetting ignoreCrystals = register(new BooleanSetting("Игнор кристаллов", "Замораживает игрока на месте при взрыве кристалла.", false));
    private boolean flag;
    private int grimTicks;
    private int ccCooldown;
    private Vec3 pendingVelocity;
    private int crystalFreezeTicks;
    private Vec3 frozenPos;

    public Velocity() {
        super("Velocity", "Reduces or cancels knockback.", ModuleCategory.COMBAT);
    }

    @SubscribeEvent
    private void onPacket(PacketEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (!event.isReceive() || client.player == null || client.player.isInWater() || client.player.isUnderWater() || client.player.isInLava()) {
            return;
        }
        if (ccCooldown > 0) {
            ccCooldown--;
            return;
        }
        if (ignoreCrystals.getValue() && event.getPacket() instanceof ClientboundExplodePacket) {
            event.cancel();
            crystalFreezeTicks = 20;
            frozenPos = client.player.position();
        }
        if (crystalFreezeTicks > 0) {
            if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.getId() == client.player.getId()) {
                event.cancel();
                return;
            }
            if (event.getPacket() instanceof ClientboundPlayerPositionPacket) {
                event.cancel();
                return;
            }
        }
        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket packet && packet.getId() == client.player.getId()) {
            handleVelocityPacket(event, packet);
        }
        if (mode.is("Old Grim") && event.getPacket() instanceof ClientboundPingPacket && grimTicks > 0) {
            event.cancel();
            grimTicks--;
        }
        if (event.getPacket() instanceof ClientboundPlayerPositionPacket && mode.is("New Grim")) {
            ccCooldown = 5;
        }
    }

    @SubscribeEvent
    private void onPreTick(TickEvent.Pre event) {
        if (crystalFreezeTicks > 0 && mc.player != null) {
            mc.player.setDeltaMovement(Vec3.ZERO);
            if (frozenPos != null) {
                mc.player.setPos(frozenPos.x, frozenPos.y, frozenPos.z);
            }
            mc.player.setSprinting(false);
            mc.player.fallDistance = 0.0F;
        }
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.player.isInWater() || client.player.isUnderWater()) {
            return;
        }
        if (crystalFreezeTicks > 0) {
            client.player.setDeltaMovement(Vec3.ZERO);
            if (frozenPos != null) {
                client.player.setPos(frozenPos.x, frozenPos.y, frozenPos.z);
            }
            client.player.setSprinting(false);
            client.player.fallDistance = 0.0F;
            crystalFreezeTicks--;
        }
        if (mode.is("Matrix")) {
            handleMatrixTick(client);
        }
        if (mode.is("New Grim") && flag) {
            handleNewGrimTick(client);
        }
        if (grimTicks > 0) {
            grimTicks--;
        }
    }

    @Override
    protected void onEnable() {
        grimTicks = 0;
        flag = false;
        ccCooldown = 0;
        pendingVelocity = null;
        crystalFreezeTicks = 0;
        frozenPos = null;
    }

    @Override
    protected void onDisable() {
        pendingVelocity = null;
        crystalFreezeTicks = 0;
        frozenPos = null;
    }

    private void handleVelocityPacket(PacketEvent event, ClientboundSetEntityMotionPacket packet) {
        Vec3 velocity = packet.getMovement();
        if (mode.is("Matrix")) {
            if (!flag) {
                event.cancel();
                flag = true;
            } else {
                flag = false;
                event.cancel();
                pendingVelocity = new Vec3(velocity.x * -0.1D, velocity.y, velocity.z * -0.1D);
            }
        } else if (mode.is("Normal")) {
            event.cancel();
        } else if (mode.is("Old Grim")) {
            event.cancel();
            grimTicks = 6;
        } else if (mode.is("New Grim")) {
            event.cancel();
            flag = true;
        }
    }

    private void handleMatrixTick(Minecraft client) {
        if (pendingVelocity != null) {
            client.player.setDeltaMovement(pendingVelocity);
            pendingVelocity = null;
        }
        if (client.player.hurtTime > 0 && !client.player.onGround()) {
            double yaw = client.player.getYRot() * 0.017453292F;
            double speed = Math.hypot(client.player.getDeltaMovement().x, client.player.getDeltaMovement().z);
            client.player.setDeltaMovement(-Math.sin(yaw) * speed, client.player.getDeltaMovement().y, Math.cos(yaw) * speed);
            client.player.setSprinting(client.player.tickCount % 2 != 0);
        }
    }

    private void handleNewGrimTick(Minecraft client) {
        if (ccCooldown <= 0) {
            client.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ(),
                    client.player.getYRot(),
                    client.player.getXRot(),
                    client.player.onGround(),
                    false
            ));
            client.player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    BlockPos.containing(client.player.position()),
                    Direction.DOWN
            ));
        }
        flag = false;
    }
}
