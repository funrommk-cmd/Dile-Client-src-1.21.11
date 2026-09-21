package dile.ru.api.command.impl;

import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import dile.ru.IMinecraft;
import dile.ru.api.command.Command;
import dile.ru.api.command.helpers.TabCompleteHelper;
import dile.ru.utils.player.PlayerPositionCache;
import dile.ru.utils.player.PlayerPositionCache.CachedPosition;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

public final class PlayerCoordsCommand extends Command implements IMinecraft {
    public PlayerCoordsCommand() {
        super("playercoords", "Показывает координаты игрока", "pc");
    }

    @Override
    public void execute(String label, String[] args) {
        if (mc.player == null || mc.level == null) {
            logDirect("Игрок недоступен.", ChatFormatting.RED);
            return;
        }

        if (args.length < 1) {
            logDirect("Использование: ." + label + " <ник>", ChatFormatting.RED);
            return;
        }

        String targetName = args[0];

        Player loaded = findLoadedPlayer(targetName);
        if (loaded != null) {
            int x = (int) Math.floor(loaded.getX());
            int y = (int) Math.floor(loaded.getY());
            int z = (int) Math.floor(loaded.getZ());
            logDirect(loaded.getName().getString() + ": " + x + " " + y + " " + z, ChatFormatting.GREEN);
            return;
        }

        CachedPosition cached = PlayerPositionCache.getInstance().getByName(targetName);
        if (cached != null) {
            logDirect(cached.name() + ": " + cached.blockX() + " " + cached.blockY() + " " + cached.blockZ()
                    + " §8(" + cached.ageString() + ")", ChatFormatting.GREEN);
            return;
        }

        UUID uuid = findUuidInTabList(targetName);
        if (uuid != null) {
            Player byUuid = mc.level.getPlayerByUUID(uuid);
            if (byUuid != null) {
                int x = (int) Math.floor(byUuid.getX());
                int y = (int) Math.floor(byUuid.getY());
                int z = (int) Math.floor(byUuid.getZ());
                logDirect(byUuid.getName().getString() + ": " + x + " " + y + " " + z, ChatFormatting.GREEN);
                return;
            }
        }

        if (isPlayerOnline(targetName)) {
            logDirect(targetName + " онлайн, но координаты неизвестны.", ChatFormatting.YELLOW);
        } else {
            logDirect(targetName + " не найден.", ChatFormatting.RED);
        }
    }

    private Player findLoadedPlayer(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        Player exact = null;
        Player prefix = null;

        for (Player player : mc.level.players()) {
            String playerName = player.getName().getString().toLowerCase(Locale.ROOT);
            if (playerName.equals(lower)) {
                exact = player;
                break;
            }
            if (prefix == null && playerName.startsWith(lower)) {
                prefix = player;
            }
        }

        return exact != null ? exact : prefix;
    }

    private UUID findUuidInTabList(String name) {
        if (mc.getConnection() == null) {
            return null;
        }

        String lower = name.toLowerCase(Locale.ROOT);
        UUID exact = null;
        UUID prefix = null;

        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            String playerName = info.getProfile().name().toLowerCase(Locale.ROOT);
            if (playerName.equals(lower)) {
                return info.getProfile().id();
            }
            if (prefix == null && playerName.startsWith(lower)) {
                prefix = info.getProfile().id();
            }
        }

        return exact != null ? exact : prefix;
    }

    private boolean isPlayerOnline(String name) {
        if (mc.getConnection() == null) {
            return false;
        }

        String lower = name.toLowerCase(Locale.ROOT);
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            if (info.getProfile().name().toLowerCase(Locale.ROOT).equals(lower)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1 && mc.getConnection() != null) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return mc.getConnection().getOnlinePlayers().stream()
                    .map(info -> info.getProfile().name())
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted();
        }
        return Stream.empty();
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Показывает координаты указанного игрока.",
                "Использование:",
                "> playercoords <ник> - координаты игрока"
        );
    }
}
