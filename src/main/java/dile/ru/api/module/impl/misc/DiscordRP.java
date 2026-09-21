package dile.ru.api.module.impl.misc;

import dev.firstdark.rpc.DiscordRpc;
import dev.firstdark.rpc.enums.ActivityType;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.handlers.RPCEventHandler;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import dile.ru.api.module.Module;
import dile.ru.api.module.ModuleCategory;

public final class DiscordRP extends Module {
    private static DiscordRP instance;
    private static final String APPLICATION_ID = "1526095770065829908";
    private static final String DISCORD_URL = "https://discord.gg/NsdBkjJ5FS";
    private static final String BUILD_VERSION = "Build -> 1.4";

    private DiscordRpc rpc;

    public DiscordRP() {
        super("Discord RPC", "Discord integration with Rich Presence.", ModuleCategory.MISC);
        instance = this;
    }

    public static DiscordRP getInstance() {
        return instance;
    }

    @Override
    protected void onEnable() {
        try {
            rpc = new DiscordRpc();

            RPCEventHandler handler = new RPCEventHandler() {
                @Override
                public void ready(User user) {
                    DiscordRichPresence presence = DiscordRichPresence.builder()
                            .details(BUILD_VERSION)
                            .largeImageKey("dile")
                            .largeImageText("Dile Client")
                            .activityType(ActivityType.PLAYING)
                            .button(DiscordRichPresence.RPCButton.of("Discord", DISCORD_URL))
                            .build();
                    rpc.updatePresence(presence);
                }

                @Override
                public void disconnected(ErrorCode errorCode, String message) {
                }

                @Override
                public void errored(ErrorCode errorCode, String message) {
                }
            };

            rpc.init(APPLICATION_ID, handler, false);
        } catch (Exception e) {
            e.printStackTrace();
            setEnabled(false);
        }
    }

    @Override
    protected void onDisable() {
        if (rpc != null) {
            try {
                rpc.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
            rpc = null;
        }
    }
}
