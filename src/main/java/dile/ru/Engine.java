package dile.ru;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import dile.ru.manager.Manager;
import dile.ru.utils.render.ui.Render2D;
import dile.ru.utils.sounds.SoundManager;

public class Engine implements ModInitializer, ClientModInitializer {

    private final Manager manager = new Manager();

    @Override
    public void onInitialize() {
        SoundManager.init();
        Render2D.init();
    }

    @Override
    public void onInitializeClient() {
        manager.initClient();
    }
}
