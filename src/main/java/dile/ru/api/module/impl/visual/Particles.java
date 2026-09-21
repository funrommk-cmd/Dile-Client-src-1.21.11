package dile.ru.api.module.impl.visual;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.AttackEvent;
import dile.ru.api.events.impl.PacketEvent;
import dile.ru.api.events.impl.WorldRenderEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.Setting;
import dile.ru.api.module.impl.visual.particles.ParticleSettings;
import dile.ru.api.module.impl.visual.particles.ParticleSystem;

public class Particles extends Module {
    private final ParticleSettings particleSettings;
    private final ParticleSystem particleSystem;

    public Particles() {
        super("Particles", "Displays Excellent-style attack, totem and walk particles.", ModuleCategory.VISUAL);
        this.particleSettings = new ParticleSettings(setting -> registerSetting(setting));
        this.particleSettings.spawnIf.setSelected("World", false);
        this.particleSystem = new ParticleSystem(this.particleSettings);
    }

    @Override
    protected void onEnable() {
        particleSystem.clear();
    }

    @Override
    protected void onDisable() {
        particleSystem.clear();
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player != null && client.player.onGround()
                && isMovingOnGround(client) && particleSettings.spawnWalk()) {
            particleSystem.spawnWalk(client);
        }
        particleSystem.tick(client);
    }

    @SubscribeEvent
    private void onWorldRender(WorldRenderEvent event) {
        particleSystem.render(mc, event);
    }

    @SubscribeEvent
    private void onAttack(AttackEvent event) {
        particleSystem.spawnAttack(mc, event.getTarget());
    }

    @SubscribeEvent
    private void onPacket(PacketEvent event) {
        if (!event.isReceive() || mc.level == null || mc.player == null) {
            return;
        }
        if (!particleSettings.spawnTotem()) {
            return;
        }
        if (!(event.getPacket() instanceof ClientboundEntityEventPacket packet)
                || packet.getEventId() != 35) {
            return;
        }
        Entity entity = packet.getEntity(mc.level);
        if (entity instanceof Player player && player.getUUID().equals(mc.player.getUUID())) {
            particleSystem.spawnTotem(mc, player);
        }
    }

    private boolean isMovingOnGround(Minecraft client) {
        if (client.player == null) {
            return false;
        }
        Vec3 velocity = client.player.getDeltaMovement();
        return velocity.x * velocity.x + velocity.z * velocity.z > 1.0E-6D;
    }

    private <S extends Setting<?>> S registerSetting(S setting) {
        return register(setting);
    }
}
