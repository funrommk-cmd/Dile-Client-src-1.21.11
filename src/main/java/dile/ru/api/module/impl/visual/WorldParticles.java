package dile.ru.api.module.impl.visual;

import net.minecraft.client.Minecraft;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.WorldRenderEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.Setting;
import dile.ru.api.module.impl.visual.particles.ParticleSettings;
import dile.ru.api.module.impl.visual.particles.ParticleSystem;

public class WorldParticles extends Module {
    private final ParticleSettings particleSettings;
    private final ParticleSystem particleSystem;

    public WorldParticles() {
        super("World Particles", "Displays world particles around you.", ModuleCategory.VISUAL);
        this.particleSettings = new ParticleSettings(setting -> registerSetting(setting));
        this.particleSettings.spawnIf.setVisible(false);
        this.particleSettings.spawnIf.setSelected("Attacks", false);
        this.particleSettings.spawnIf.setSelected("World", true);
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
        particleSystem.tick(client);
    }

    @SubscribeEvent
    private void onWorldRender(WorldRenderEvent event) {
        particleSystem.render(mc, event);
    }

    private <S extends Setting<?>> S registerSetting(S setting) {
        return register(setting);
    }
}
