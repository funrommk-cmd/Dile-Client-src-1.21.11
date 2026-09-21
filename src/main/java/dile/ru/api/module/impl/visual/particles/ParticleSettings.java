package dile.ru.api.module.impl.visual.particles;

import dile.ru.api.settings.Setting;
import dile.ru.api.settings.impl.BooleanSetting;
import dile.ru.api.settings.impl.ColorSetting;
import dile.ru.api.settings.impl.ModeSetting;
import dile.ru.api.settings.impl.MultiModeSetting;
import dile.ru.api.settings.impl.NumberSetting;

import java.awt.Color;

import static dile.ru.api.module.impl.visual.particles.ParticleConstants.*;

public final class ParticleSettings {
    public final MultiModeSetting spawnIf;
    public final MultiModeSetting texturesEnabled;
    public final ModeSetting renderModeAs;
    public final NumberSetting maxBaseScale;
    public final BooleanSetting scaleAsAlphaEnabled;
    public final BooleanSetting smoothTextureEnabled;
    public final BooleanSetting bloomTextureEnabled;
    public final BooleanSetting rainbowColorEnabled;
    public final ColorSetting color;

    public final NumberSetting spawnCountPerTickInWorld;
    public final NumberSetting maxSpawnDistanceInWorld;
    public final ModeSetting spawnPulseMotionSideRuleInWorld;
    public final ModeSetting motionSideChangeRuleInWorld;
    public final NumberSetting aliveTimeBaseInWorld;

    public final NumberSetting spawnCountOnAttack;
    public final NumberSetting maxSpawnDistanceOnAttack;
    public final ModeSetting spawnPulseMotionSideRuleOnAttack;
    public final ModeSetting motionSideChangeRuleOnAttack;
    public final NumberSetting aliveTimeBaseOnAttack;

    public final NumberSetting spawnCountOnTotem;
    public final NumberSetting maxSpawnDistanceOnTotem;
    public final ModeSetting spawnPulseMotionSideRuleOnTotem;
    public final ModeSetting motionSideChangeRuleOnTotem;
    public final NumberSetting aliveTimeBaseOnTotem;

    public final NumberSetting spawnCountOnWalk;
    public final NumberSetting maxSpawnDistanceOnWalk;
    public final ModeSetting spawnPulseMotionSideRuleOnWalk;
    public final ModeSetting motionSideChangeRuleOnWalk;
    public final NumberSetting aliveTimeBaseOnWalk;

    public final NumberSetting speedBase;
    public final NumberSetting gravityPower;
    public final NumberSetting motionInfluenceMul;
    public final NumberSetting magnetismPowerBase;
    public final ModeSetting jumpFromCollideMode;
    public final BooleanSetting jumpFromAnotherParticle;

    private final SettingRegistrar registrar;

    public ParticleSettings(SettingRegistrar registrar) {
        this.registrar = registrar;
        this.spawnIf = register(new MultiModeSetting("Events", "Particle spawn events.", EVENT_MODES, EVENT_WORLD, EVENT_ATTACKS));
        this.texturesEnabled = register(new MultiModeSetting("Textures", "Particle textures.", TEXTURE_MODES, TEXTURE_MODES));
        this.renderModeAs = register(new ModeSetting("Render As", "Particle render mode.", RENDER_TEXTURES, RENDER_MODES));
        this.maxBaseScale = register(new NumberSetting("Max Scale", "Maximum particle scale.", 0.5, 0.1, 1.0, 0.05));
        this.scaleAsAlphaEnabled = register(new BooleanSetting("Dynamic Scale", "Scales particles by current alpha.", true));
        this.smoothTextureEnabled = register(new BooleanSetting("Smooth Textures", "Keeps parity with the source texture smoothing option.", false));
        this.bloomTextureEnabled = register(new BooleanSetting("Bright Mode", "Uses a glowing blend for texture particles.", true));
        this.rainbowColorEnabled = register(new BooleanSetting("Rainbow Colors", "Uses random rainbow particle colors.", false));
        this.color = register(new ColorSetting("Color", "Custom particle color.", new Color(127, 242, 255, 255)));

        this.spawnCountPerTickInWorld = register(new NumberSetting("World Count", "Particles added each tick around you.", 4.0, 1.0, 15.0, 1.0));
        this.maxSpawnDistanceInWorld = register(new NumberSetting("World Spread", "Maximum world particle spread.", 10.0, 1.0, 24.0, 1.0));
        this.spawnPulseMotionSideRuleInWorld = register(new ModeSetting("World Spawn Direction", "Initial world particle direction.", RULE_RANDOM, SPAWN_RULES));
        this.motionSideChangeRuleInWorld = register(new ModeSetting("World Force", "Continuous world particle force.", RULE_NONE, FORCE_RULES));
        this.aliveTimeBaseInWorld = register(new NumberSetting("World Lifetime", "World particle lifetime in milliseconds.", 2500.0, 500.0, 4000.0, 50.0));

        this.spawnCountOnAttack = register(new NumberSetting("Attack Count", "Particles added per hit.", 40.0, 5.0, 150.0, 5.0));
        this.maxSpawnDistanceOnAttack = register(new NumberSetting("Attack Spread", "Maximum hit particle spread.", 2.0, 0.5, 12.0, 0.5));
        this.spawnPulseMotionSideRuleOnAttack = register(new ModeSetting("Attack Spawn Direction", "Initial hit particle direction.", RULE_FROM_SOURCE, SPAWN_RULES));
        this.motionSideChangeRuleOnAttack = register(new ModeSetting("Attack Force", "Continuous hit particle force.", RULE_NONE, FORCE_RULES));
        this.aliveTimeBaseOnAttack = register(new NumberSetting("Attack Lifetime", "Hit particle lifetime in milliseconds.", 1250.0, 500.0, 4000.0, 50.0));

        this.spawnCountOnTotem = register(new NumberSetting("Totem Count", "Particles added on totem pop.", 80.0, 5.0, 200.0, 5.0));
        this.maxSpawnDistanceOnTotem = register(new NumberSetting("Totem Spread", "Maximum totem particle spread.", 3.0, 0.5, 12.0, 0.5));
        this.spawnPulseMotionSideRuleOnTotem = register(new ModeSetting("Totem Spawn Direction", "Initial totem particle direction.", RULE_FROM_SOURCE, SPAWN_RULES));
        this.motionSideChangeRuleOnTotem = register(new ModeSetting("Totem Force", "Continuous totem particle force.", RULE_NONE, FORCE_RULES));
        this.aliveTimeBaseOnTotem = register(new NumberSetting("Totem Lifetime", "Totem particle lifetime in milliseconds.", 2000.0, 500.0, 4000.0, 50.0));

        this.spawnCountOnWalk = register(new NumberSetting("Walk Count", "Particles added each step.", 2.0, 1.0, 10.0, 1.0));
        this.maxSpawnDistanceOnWalk = register(new NumberSetting("Walk Spread", "Maximum walk particle spread.", 2.0, 0.5, 8.0, 0.5));
        this.spawnPulseMotionSideRuleOnWalk = register(new ModeSetting("Walk Spawn Direction", "Initial walk particle direction.", RULE_RANDOM, SPAWN_RULES));
        this.motionSideChangeRuleOnWalk = register(new ModeSetting("Walk Force", "Continuous walk particle force.", RULE_NONE, FORCE_RULES));
        this.aliveTimeBaseOnWalk = register(new NumberSetting("Walk Lifetime", "Walk particle lifetime in milliseconds.", 1500.0, 500.0, 4000.0, 50.0));

        this.speedBase = register(new NumberSetting("Start Speed", "Initial particle speed.", 0.3, 0.05, 1.0, 0.05));
        this.gravityPower = register(new NumberSetting("Gravity", "Particle gravity factor.", 0.2, -0.5, 1.0, 0.05));
        this.motionInfluenceMul = register(new NumberSetting("Motion Influence", "Particle slowdown or acceleration.", 0.0, -1.0, 1.0, 0.05));
        this.magnetismPowerBase = register(new NumberSetting("Turn Sharpness", "Particle movement turn sharpness.", 1.0, 0.1, 2.0, 0.05));
        this.jumpFromCollideMode = register(new ModeSetting("On Collision", "Block collision behavior.", COLLIDE_RICOCHET, COLLISION_MODES));
        this.jumpFromAnotherParticle = register(new BooleanSetting("Particle Collisions", "Particles steer away from each other.", false));

        configureVisibility();
    }

    public boolean spawnWorld() {
        return spawnIf.isSelected(EVENT_WORLD);
    }

    public boolean spawnAttacks() {
        return spawnIf.isSelected(EVENT_ATTACKS);
    }

    public boolean spawnTotem() {
        return spawnIf.isSelected(EVENT_TOTEM);
    }

    public boolean spawnWalk() {
        return spawnIf.isSelected(EVENT_WALK);
    }

    public boolean hasEnabledTextures() {
        return texturesEnabled.selectedCount() > 0;
    }

    private boolean hasAnySpawnEvent() {
        return spawnIf.selectedCount() > 0;
    }

    private boolean canSpawnParticles() {
        return hasAnySpawnEvent() && hasEnabledTextures();
    }

    private boolean hasAnyForceRule() {
        return spawnWorld() && !motionSideChangeRuleInWorld.is(RULE_NONE)
                || spawnAttacks() && !motionSideChangeRuleOnAttack.is(RULE_NONE)
                || spawnTotem() && !motionSideChangeRuleOnTotem.is(RULE_NONE)
                || spawnWalk() && !motionSideChangeRuleOnWalk.is(RULE_NONE);
    }

    @SuppressWarnings("unchecked")
    private <S extends Setting<?>> S register(S setting) {
        return (S) registrar.register(setting);
    }

    private void configureVisibility() {
        texturesEnabled.visibleWhen(this::hasAnySpawnEvent);
        renderModeAs.visibleWhen(this::canSpawnParticles);
        maxBaseScale.visibleWhen(this::canSpawnParticles);
        scaleAsAlphaEnabled.visibleWhen(this::canSpawnParticles);
        smoothTextureEnabled.visibleWhen(this::canSpawnParticles);
        bloomTextureEnabled.visibleWhen(this::canSpawnParticles);
        rainbowColorEnabled.visibleWhen(this::canSpawnParticles);
        color.visibleWhen(() -> canSpawnParticles() && !rainbowColorEnabled.getValue());

        spawnCountPerTickInWorld.visibleWhen(() -> spawnWorld() && hasEnabledTextures());
        maxSpawnDistanceInWorld.visibleWhen(() -> spawnWorld() && hasEnabledTextures());
        spawnPulseMotionSideRuleInWorld.visibleWhen(() -> spawnWorld() && hasEnabledTextures());
        motionSideChangeRuleInWorld.visibleWhen(() -> spawnWorld() && hasEnabledTextures());
        aliveTimeBaseInWorld.visibleWhen(() -> spawnWorld() && hasEnabledTextures());

        spawnCountOnAttack.visibleWhen(() -> spawnAttacks() && hasEnabledTextures());
        maxSpawnDistanceOnAttack.visibleWhen(() -> spawnAttacks() && hasEnabledTextures());
        spawnPulseMotionSideRuleOnAttack.visibleWhen(() -> spawnAttacks() && hasEnabledTextures());
        motionSideChangeRuleOnAttack.visibleWhen(() -> spawnAttacks() && hasEnabledTextures());
        aliveTimeBaseOnAttack.visibleWhen(() -> spawnAttacks() && hasEnabledTextures());

        spawnCountOnTotem.visibleWhen(() -> spawnTotem() && hasEnabledTextures());
        maxSpawnDistanceOnTotem.visibleWhen(() -> spawnTotem() && hasEnabledTextures());
        spawnPulseMotionSideRuleOnTotem.visibleWhen(() -> spawnTotem() && hasEnabledTextures());
        motionSideChangeRuleOnTotem.visibleWhen(() -> spawnTotem() && hasEnabledTextures());
        aliveTimeBaseOnTotem.visibleWhen(() -> spawnTotem() && hasEnabledTextures());

        spawnCountOnWalk.visibleWhen(() -> spawnWalk() && hasEnabledTextures());
        maxSpawnDistanceOnWalk.visibleWhen(() -> spawnWalk() && hasEnabledTextures());
        spawnPulseMotionSideRuleOnWalk.visibleWhen(() -> spawnWalk() && hasEnabledTextures());
        motionSideChangeRuleOnWalk.visibleWhen(() -> spawnWalk() && hasEnabledTextures());
        aliveTimeBaseOnWalk.visibleWhen(() -> spawnWalk() && hasEnabledTextures());

        speedBase.visibleWhen(this::canSpawnParticles);
        gravityPower.visibleWhen(this::canSpawnParticles);
        motionInfluenceMul.visibleWhen(this::canSpawnParticles);
        magnetismPowerBase.visibleWhen(() -> canSpawnParticles() && hasAnyForceRule());
        jumpFromCollideMode.visibleWhen(this::canSpawnParticles);
        jumpFromAnotherParticle.visibleWhen(this::canSpawnParticles);
    }

    @FunctionalInterface
    public interface SettingRegistrar {
        Setting<?> register(Setting<?> setting);
    }
}
