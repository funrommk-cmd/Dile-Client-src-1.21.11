package dile.ru.api.module.impl.player;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import dile.ru.api.events.annotation.SubscribeEvent;
import dile.ru.api.events.impl.TickEvent;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;
import dile.ru.api.settings.bind.KeyBind;
import dile.ru.api.settings.impl.BindSetting;

public final class CoordsDropper extends Module {
    private static final Minecraft mc = Minecraft.getInstance();

    private final BindSetting bind = register(new BindSetting("Bind", "Key to drop coordinates.", KeyBind.keyboard(GLFW.GLFW_KEY_UNKNOWN)));
    private boolean lastBindDown;

    public CoordsDropper() {
        super("Coords Dropper", "Sends your coordinates in chat on key press.", ModuleCategory.PLAYER);
    }

    @Override
    protected void onDisable() {
        lastBindDown = false;
    }

    @SubscribeEvent
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null || mc.getWindow() == null || mc.screen != null) {
            lastBindDown = false;
            return;
        }

        boolean bindDown = bind.getValue().isDown(mc.getWindow().handle());
        if (bindDown && !lastBindDown) {
            dropCoords();
        }
        lastBindDown = bindDown;
    }

    private void dropCoords() {
        int x = (int) mc.player.getX();
        int y = (int) mc.player.getY();
        int z = (int) mc.player.getZ();
        String message = "! \u044f \u043d\u0430 \u043a\u043e\u0440\u0434\u0430\u0445 " + x + " " + y + " " + z;
        mc.player.connection.sendChat(message);
    }
}
