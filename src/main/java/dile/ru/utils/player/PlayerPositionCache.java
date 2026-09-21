package dile.ru.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import dile.ru.IMinecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerPositionCache implements IMinecraft {
    private static final PlayerPositionCache INSTANCE = new PlayerPositionCache();
    private final Map<UUID, CachedPosition> cache = new HashMap<>();

    private PlayerPositionCache() {
    }

    public static PlayerPositionCache getInstance() {
        return INSTANCE;
    }

    public void tick() {
        if (mc.player == null || mc.level == null) {
            return;
        }

        for (Player player : mc.level.players()) {
            if (player == mc.player) {
                continue;
            }
            UUID uuid = player.getUUID();
            cache.put(uuid, new CachedPosition(
                    player.getX(), player.getY(), player.getZ(),
                    player.getName().getString(),
                    System.currentTimeMillis(),
                    true
            ));
        }
    }

    public CachedPosition get(UUID uuid) {
        return cache.get(uuid);
    }

    public CachedPosition getByName(String name) {
        String lower = name.toLowerCase();
        CachedPosition exact = null;
        CachedPosition prefix = null;

        for (CachedPosition pos : cache.values()) {
            String playerName = pos.name().toLowerCase();
            if (playerName.equals(lower)) {
                return pos;
            }
            if (prefix == null && playerName.startsWith(lower)) {
                prefix = pos;
            }
        }

        return exact != null ? exact : prefix;
    }

    public Set<UUID> getCachedUUIDs() {
        return cache.keySet();
    }

    public void clear() {
        cache.clear();
    }

    public record CachedPosition(double x, double y, double z, String name, long timestamp, boolean current) {
        public int blockX() {
            return (int) Math.floor(x);
        }

        public int blockY() {
            return (int) Math.floor(y);
        }

        public int blockZ() {
            return (int) Math.floor(z);
        }

        public long ageSeconds() {
            return (System.currentTimeMillis() - timestamp) / 1000;
        }

        public String ageString() {
            long seconds = ageSeconds();
            if (seconds < 5) return "сейчас";
            if (seconds < 60) return seconds + "с назад";
            if (seconds < 3600) return (seconds / 60) + "м назад";
            return (seconds / 3600) + "ч назад";
        }
    }
}
