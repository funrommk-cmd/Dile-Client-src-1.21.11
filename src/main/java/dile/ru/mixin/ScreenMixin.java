package dile.ru.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dile.ru.api.command.CommandManager;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
    private static void dile$handleClientClickEvent(ClickEvent clickEvent, Minecraft client, Screen screen, CallbackInfo ci) {
        if (clickEvent instanceof ClickEvent.RunCommand runCommand) {
            String command = runCommand.command();
            CommandManager manager = CommandManager.getInstance();
            if (manager != null && command != null && command.startsWith(manager.getPrefix())) {
                manager.execute(command.substring(manager.getPrefix().length()));
                ci.cancel();
            }
        }
    }
}
