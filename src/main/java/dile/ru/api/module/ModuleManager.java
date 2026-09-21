package dile.ru.api.module;

import net.minecraft.client.Minecraft;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.bus.EventBus;
import dile.ru.api.events.impl.TickEvent;
import dile.ru.api.module.impl.combat.AuraModule;
import dile.ru.api.module.impl.combat.AntiBot;
import dile.ru.api.module.impl.combat.AimAssist;
import dile.ru.api.module.impl.combat.AutoCrystal;
import dile.ru.api.module.impl.combat.AutoExplosion;
import dile.ru.api.module.impl.combat.AutoGApple;
import dile.ru.api.module.impl.combat.AutoPotion;
import dile.ru.api.module.impl.combat.AutoSwap;
import dile.ru.api.module.impl.combat.AutoTotem;
import dile.ru.api.module.impl.combat.Criticals;
import dile.ru.api.module.impl.combat.HitBoxModule;
import dile.ru.api.module.impl.combat.HitSound;
import dile.ru.api.module.impl.combat.MaceTarget;
import dile.ru.api.module.impl.combat.NoFriendDamage;
import dile.ru.api.module.impl.combat.NoInteract;
import dile.ru.api.module.impl.combat.TargetPearl;
import dile.ru.api.module.impl.combat.TriggerBot;
import dile.ru.api.module.impl.combat.FastUse;
import dile.ru.api.module.impl.combat.Velocity;
import dile.ru.api.module.impl.movement.*;
import dile.ru.api.module.impl.misc.AutoAuth;
import dile.ru.api.module.impl.misc.AutoDuel;
import dile.ru.api.module.impl.misc.AutoLes;
import dile.ru.api.module.impl.misc.AutoLeave;
import dile.ru.api.module.impl.misc.AutoTpAccept;
import dile.ru.api.module.impl.misc.ClientSounds;
import dile.ru.api.module.impl.misc.ClickSounds;
import dile.ru.api.module.impl.misc.DeathCoords;
import dile.ru.api.module.impl.misc.DiscordRP;
import dile.ru.api.module.impl.misc.ElytraHelper;
import dile.ru.api.module.impl.misc.JoinerHelper;
import dile.ru.api.module.impl.misc.ServerHelper;
import dile.ru.api.module.impl.misc.UseTracker;
import dile.ru.api.module.impl.player.AHHelper;
import dile.ru.api.module.impl.player.FastExp;
import dile.ru.api.module.impl.player.AutoMessage;
import dile.ru.api.module.impl.player.AutoRespawn;
import dile.ru.api.module.impl.player.AutoTool;
import dile.ru.api.module.impl.player.ChestStealer;
import dile.ru.api.module.impl.player.ClickPearl;
import dile.ru.api.module.impl.player.CoordsDropper;
import dile.ru.api.module.impl.player.KitFarmer;
import dile.ru.api.module.impl.player.FakePing;
import dile.ru.api.module.impl.player.FastBreak;
import dile.ru.api.module.impl.misc.ServerRPSpoofer;
import dile.ru.api.module.impl.player.NoEntityTrace;
import dile.ru.api.module.impl.player.NoDelay;
import dile.ru.api.module.impl.player.NoPush;
import dile.ru.api.module.impl.player.NameProtect;
import dile.ru.api.module.impl.player.ItemScroller;
import dile.ru.api.module.impl.player.FreeLook;
import dile.ru.api.module.impl.player.FreeCam;
import dile.ru.api.module.impl.player.OpenWalls;
import dile.ru.api.module.impl.player.WindJump;
import dile.ru.api.module.impl.visual.Ambience;
import dile.ru.api.module.impl.visual.Breasts;
import dile.ru.api.module.impl.visual.ArmorDurability;
import dile.ru.api.module.impl.visual.Arrows;
import dile.ru.api.module.impl.visual.CustomInvsee;
import dile.ru.api.module.impl.visual.ChunkAnimator;
import dile.ru.api.module.impl.visual.AspectRatio;
import dile.ru.api.module.impl.visual.BlockESP;
import dile.ru.api.module.impl.visual.BlockOverlay;
import dile.ru.api.module.impl.visual.Chams;
import dile.ru.api.module.impl.visual.ClickGuiModule;
import dile.ru.api.module.impl.visual.DashTrail;
import dile.ru.api.module.impl.visual.ESP;
import dile.ru.api.module.impl.visual.FogBlur;
import dile.ru.api.module.impl.visual.GhostHat;
import dile.ru.api.module.impl.visual.GlassHands;
import dile.ru.api.module.impl.visual.Hands;
import dile.ru.api.module.impl.visual.HoldMyItems;
import dile.ru.api.module.impl.visual.Hud;
import dile.ru.api.module.impl.visual.Hud2;
import dile.ru.api.module.impl.visual.Hud3;
import dile.ru.api.module.impl.visual.Hud4;
import dile.ru.api.module.impl.visual.Hud5;
import dile.ru.api.module.impl.visual.ItemPhysics;
import dile.ru.api.module.impl.visual.KillEffect;
import dile.ru.api.module.impl.visual.ItemReplacer;
import dile.ru.api.module.impl.visual.ItemsCooldowns;
import dile.ru.api.module.impl.visual.NoRender;
import dile.ru.api.module.impl.visual.Particles;
import dile.ru.api.module.impl.visual.WorldParticles;
import dile.ru.api.module.impl.visual.Predictions;
import dile.ru.api.module.impl.visual.SeeInvisible;
import dile.ru.api.module.impl.visual.SkyShader;
import dile.ru.api.module.impl.visual.TargetESP;
import dile.ru.api.module.impl.visual.SwingAnimation;
import dile.ru.api.module.impl.visual.ViewModel;
import dile.ru.api.module.impl.visual.WorldGlow;
import dile.ru.api.module.impl.visual.BackSword;
import dile.ru.api.module.impl.visual.ChinaHat;
import dile.ru.api.module.impl.visual.Crown;
import dile.ru.api.module.impl.visual.GlowItem;
import dile.ru.api.module.impl.visual.Helicopter;
import dile.ru.api.module.impl.visual.Wings;
import dile.ru.api.module.impl.visual.CustomModels;
import dile.ru.api.settings.bind.KeyBind;
import dile.ru.utils.inventory.InventoryFlowManager;
import dile.ru.utils.network.Network;
import dile.ru.utils.player.PlayerPositionCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ModuleManager {
    private final EventBus eventBus;
    private final List<Module> modules = new ArrayList<>();
    private final Map<String, Module> byName = new HashMap<>();
    private final Map<Class<? extends Module>, Module> byType = new HashMap<>();
    private final Map<ModuleCategory, List<Module>> byCategory = new EnumMap<>(ModuleCategory.class);
    private final Map<ModuleCategory, List<Module>> byCategoryViews = new EnumMap<>(ModuleCategory.class);
    private Collection<Module> modulesView = List.of();
    private final Map<Module, Boolean> bindStates = new IdentityHashMap<>();
    private Runnable dirtyListener = () -> {};

    public ModuleManager(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void init() {
        registerDefaults();
        eventBus.register(this);
    }

    public void setDirtyListener(Runnable dirtyListener) {
        this.dirtyListener = dirtyListener == null ? () -> {} : dirtyListener;
    }

    public void register(Module module) {
        String key = normalize(module.getName());
        if (byName.containsKey(key)) {
            return;
        }
        modules.add(module);
        byName.put(key, module);
        byType.putIfAbsent(module.getClass(), module);
        byCategory.computeIfAbsent(module.getCategory(), ignored -> new ArrayList<>()).add(module);
        module.addStateListener(changedModule -> dirtyListener.run());
        module.initialize(eventBus);
        sortViews();
    }

    public Collection<Module> getModules() {
        return modulesView;
    }

    public List<Module> getByCategory(ModuleCategory category) {
        return byCategoryViews.getOrDefault(category, List.of());
    }

    public Optional<Module> getByName(String name) {
        return Optional.ofNullable(byName.get(normalize(name)));
    }

    public <T extends Module> Optional<T> getByType(Class<T> type) {
        Module module = byType.get(type);
        if (type.isInstance(module)) {
            return Optional.of(type.cast(module));
        }
        for (Module candidate : modules) {
            if (type.isInstance(candidate)) {
                return Optional.of(type.cast(candidate));
            }
        }
        return Optional.empty();
    }

    public boolean isEnabled(Class<? extends Module> type) {
        return getByType(type).filter(Module::isEnabled).isPresent();
    }

    public Optional<AuraModule> getAura() {
        return getByType(AuraModule.class);
    }

    public Optional<AuraModule> getKillAura() {
        return getAura();
    }

    public Optional<HitBoxModule> getHitBox() {
        return getByType(HitBoxModule.class);
    }

    public boolean isHitBoxEnabled() {
        return isEnabled(HitBoxModule.class);
    }

    public Optional<NoInteract> getNoInteract() {
        return getByType(NoInteract.class);
    }

    public boolean isNoInteractEnabled() {
        return isEnabled(NoInteract.class);
    }

    public Optional<ServerRPSpoofer> getServerRPSpoofer() {
        return getByType(ServerRPSpoofer.class);
    }

    public boolean isServerRPSpooferEnabled() {
        return isEnabled(ServerRPSpoofer.class);
    }

    @SubscribeEvent
    private void onPreTick(TickEvent.Pre event) {
        InventoryFlowManager.update();
    }

    @SubscribeEvent
    private void onTick(TickEvent.Post event) {
        Minecraft client = event.getClient();
        Network.tick();
        PlayerPositionCache.getInstance().tick();
        handleBinds(client);
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick(client);
            }
        }
    }

    private void registerDefaults() {
        register(new AntiBot());
        register(new AimAssist());
        register(new AuraModule());
        register(new AutoCrystal());
        register(new AutoExplosion());
        register(new AutoGApple());
        register(new AutoPotion());
        register(new AutoSwap());
        register(new AutoTotem());
        register(new Criticals());
        register(new HitBoxModule());
        register(new MaceTarget());
        register(new HitSound());
        register(new NoFriendDamage());
        register(new NoInteract());
        register(new TriggerBot());
        register(new TargetPearl());
        register(new Velocity());
        register(new FastUse());

        register(new AutoSprint());
        register(new Fly());
        register(new ElytraTarget());
        register(new NoWeb());
        register(new NoSlow());
        register(new InventoryMove());
        register(new Spider());
        register(new Speed());
        register(new Strafe());
        register(new SuperFireWork());
        register(new TargetStrafe());
        register(new AirStuck());
        register(new ElytraMotion());
        register(new ElytraFly());
        register(new BoatFly());
        register(new NoVelocityDesync());
        register(new Ambience());
        register(new Breasts());
        register(new Arrows());
        register(new AspectRatio());
        register(new DashTrail());
        register(new Chams());
        register(new GhostHat());
        register(new BlockESP());
        register(new BlockOverlay());
        register(new ESP());
        register(new ArmorDurability());
        register(new TargetESP());
        register(new FogBlur());
        register(new Hands());
        register(new GlassHands());
        register(new HoldMyItems());
        register(new Hud());
        register(new Hud2());
        register(new Hud3());
        register(new Hud4());
        register(new Hud5());
        register(new ItemPhysics());
        register(new KillEffect());
        register(new Particles());
        register(new WorldParticles());
        register(new Predictions());
        register(new SeeInvisible());
        register(new SkyShader());
        register(new WorldGlow());
        register(new SwingAnimation());
        register(new ViewModel());
        register(new Wings());
        register(new BackSword());
        register(new ChinaHat());
        register(new Crown());
        register(new Helicopter());
        register(new GlowItem());
        register(new ItemReplacer());
        register(new NoRender());
        register(new ItemsCooldowns());
        register(new CustomModels());
        register(new CustomInvsee());
        register(new ChunkAnimator());

        register(new WindJump());
        register(new OpenWalls());
        register(new NoPush());
        register(new NoEntityTrace());
        register(new NoDelay());
        register(new FastBreak());
        register(new FakePing());
        register(new NameProtect());
        register(new ItemScroller());
        register(new FreeLook());
        register(new FreeCam());
        register(new AHHelper());
        register(new AutoRespawn());
        register(new AutoMessage());
        register(new AutoTool());
        register(new ChestStealer());
        register(new KitFarmer());
        register(new FastExp());

        register(new JoinerHelper());
        register(new ServerRPSpoofer());
        register(new AutoAuth());
        register(new AutoDuel());
        register(new AutoLeave());
        register(new AutoTpAccept());
        register(new AutoLes());
        register(new ClientSounds());
        register(new ClickSounds());
        register(new ClickPearl());
        register(new CoordsDropper());
        register(new DeathCoords());
        register(new ElytraHelper());
        register(new ServerHelper());
        register(new UseTracker());
        register(new DiscordRP());
        register(new ClickGuiModule());
    }

    private void handleBinds(Minecraft client) {
        if (client == null || client.getWindow() == null || client.screen != null) {
            bindStates.clear();
            return;
        }

        long handle = client.getWindow().handle();
        for (Module module : modules) {
            KeyBind bind = module.getBind();
            boolean down = bind.isDown(handle);
            boolean wasDown = bindStates.getOrDefault(module, false);
            if (down && !wasDown) {
                module.toggle();
            }
            bindStates.put(module, down);
        }
    }

    private void register(Module module, String name, String description, ModuleCategory category) {
        module.configure(name, description, category);
        register(module);
    }

    private void sortViews() {
        modules.sort(Comparator.comparing(module -> module.getName().toLowerCase(Locale.ROOT)));
        for (List<Module> categoryModules : byCategory.values()) {
            categoryModules.sort(Comparator.comparing(module -> module.getName().toLowerCase(Locale.ROOT)));
        }
        modulesView = Collections.unmodifiableList(modules);
        byCategoryViews.clear();
        for (Map.Entry<ModuleCategory, List<Module>> entry : byCategory.entrySet()) {
            byCategoryViews.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
    }

    private String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replace(" ", "");
    }
}
