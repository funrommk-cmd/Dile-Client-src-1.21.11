package dile.ru.manager;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import dile.ru.api.command.CommandManager;
import dile.ru.api.config.ConfigManager;
import dile.ru.api.drag.core.ElementManager;
import dile.ru.api.events.Event;
import dile.ru.api.events.bus.EventBus;
import dile.ru.api.events.impl.TickEvent;
import dile.ru.api.module.ModuleManager;
import dile.ru.api.module.impl.combat.aura.AngleConnection;
import dile.ru.screens.clickgui.ClickGui;
import dile.ru.screens.modernui.ClickGuiScreen;
import dile.ru.screens.modernui.impl.WorldAnimation;
import dile.ru.utils.repository.blockesp.BlockESPConfig;
import dile.ru.utils.inventory.item.ItemToolkit;
import dile.ru.utils.repository.friend.FriendUtils;
import dile.ru.utils.repository.macro.MacroRepository;
import dile.ru.utils.repository.staff.StaffUtils;
import dile.ru.utils.repository.way.WayRepository;

public class Manager {
    private static Manager instance;

    private EventBus eventBus;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private boolean clickGuiTextWarmed;

    public void initClient() {
        instance = this;
        eventBus = new EventBus();
        FriendUtils.load();
        StaffUtils.load();
        MacroRepository.getInstance().load();
        WayRepository.getInstance().load();
        BlockESPConfig.getInstance().load();
        moduleManager = new ModuleManager(eventBus);
        moduleManager.init();
        commandManager = new CommandManager();
        commandManager.init();
        eventBus.register(AngleConnection.INSTANCE);
        configManager = new ConfigManager(moduleManager);
        moduleManager.setDirtyListener(configManager::markDirty);
        configManager.init();
        ElementManager.getInstance().load();
        configManager.loadAll();
        eventBus.register(configManager);
        eventBus.register(commandManager);
        eventBus.register(MacroRepository.getInstance());
        eventBus.register(WayRepository.getInstance());
        eventBus.register(ItemToolkit.INSTANCE);

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            warmClickGuiText();
            WorldAnimation.tick();
            ClickGui.tickMovementKeys();
            postEvent(new TickEvent.Pre(client));
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> postEvent(new TickEvent.Post(client)));
    }

    public static Manager getInstance() {
        return instance;
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public static ModuleManager getModules() {
        return instance == null ? null : instance.moduleManager;
    }

    public static <T extends Event> T postEvent(T event) {
        if (instance == null || instance.eventBus == null) {
            return event;
        }
        return instance.eventBus.post(event);
    }

    public static void toggleClickGui(Minecraft client) {
        if (client.screen instanceof ClickGuiScreen || client.screen instanceof ClickGui) {
            client.screen.onClose();
            return;
        }
        if (WorldAnimation.isActive()) {
            return;
        }
        if (client.player == null || client.level == null) {
            return;
        }
        if (client.screen != null) {
            return;
        }
        client.setScreen(new ClickGuiScreen());
    }

    private void warmClickGuiText() {
        if (clickGuiTextWarmed) {
            return;
        }
        clickGuiTextWarmed = true;
        ClickGui.warmupText();
    }
}
